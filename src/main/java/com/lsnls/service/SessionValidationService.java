package com.lsnls.service;

import com.lsnls.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para validaciones avanzadas de sesión y seguridad JWT
 */
@Service
public class SessionValidationService {

    @Value("${application.security.jwt.secret-key}")
    private String jwtSecret;

    @Value("${application.security.jwt.expiration:86400000}") // 24 horas por defecto
    private int jwtExpirationInMs;

    @Autowired
    private AuditService auditService;

    // Cache de tokens invalidados/blacklist
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
    
    // Cache de sesiones activas por usuario
    private final Map<String, Set<String>> userActiveSessions = new ConcurrentHashMap<>();
    
    // Límite de sesiones concurrentes por usuario
    private static final int MAX_CONCURRENT_SESSIONS = 3;
    
    // Timeouts de sesión en minutos
    private static final int SESSION_TIMEOUT_MINUTES = 30;
    private static final int ADMIN_SESSION_TIMEOUT_MINUTES = 60;

    /**
     * Resultado de validación de sesión
     */
    public static class SessionValidationResult {
        private final boolean valid;
        private final String reason;
        private final SecurityLevel securityLevel;

        public SessionValidationResult(boolean valid, String reason, SecurityLevel securityLevel) {
            this.valid = valid;
            this.reason = reason;
            this.securityLevel = securityLevel;
        }

        public boolean isValid() { return valid; }
        public String getReason() { return reason; }
        public SecurityLevel getSecurityLevel() { return securityLevel; }

        public static SessionValidationResult valid(SecurityLevel level) {
            return new SessionValidationResult(true, null, level);
        }

        public static SessionValidationResult invalid(String reason) {
            return new SessionValidationResult(false, reason, SecurityLevel.BLOCKED);
        }
    }

    public enum SecurityLevel {
        HIGH,      // Sesión completamente válida
        MEDIUM,    // Sesión válida con advertencias
        LOW,       // Sesión válida pero sospechosa
        BLOCKED    // Sesión bloqueada
    }

    /**
     * Valida completamente una sesión JWT
     */
    public SessionValidationResult validateSession(String token, HttpServletRequest request) {
        try {
            // Verificar si el token está en blacklist
            if (isTokenBlacklisted(token)) {
                auditService.logSecurityEvent("Token blacklisted usado: " + getTokenSummary(token), 
                    getClientIP(request), request.getHeader("User-Agent"));
                return SessionValidationResult.invalid("Token en lista negra");
            }

            // Validar estructura y firma del JWT
            Claims claims = validateJwtToken(token);
            if (claims == null) {
                return SessionValidationResult.invalid("Token JWT inválido");
            }

            // Validar usuario y obtener datos
            String username = claims.getSubject();
            if (username == null || username.trim().isEmpty()) {
                return SessionValidationResult.invalid("Usuario no encontrado en token");
            }

            // Validar expiración
            Date expiration = claims.getExpiration();
            if (expiration.before(new Date())) {
                return SessionValidationResult.invalid("Token expirado");
            }

            // Validaciones de seguridad adicionales
            SecurityLevel securityLevel = performSecurityChecks(claims, request, username);
            
            if (securityLevel == SecurityLevel.BLOCKED) {
                return SessionValidationResult.invalid("Sesión bloqueada por seguridad");
            }

            // Validar límites de sesiones concurrentes
            if (!validateConcurrentSessions(username, token)) {
                auditService.logSecurityEvent("Exceso de sesiones concurrentes para usuario: " + username,
                    getClientIP(request), request.getHeader("User-Agent"));
                return SessionValidationResult.invalid("Demasiadas sesiones activas");
            }

            // Actualizar última actividad
            updateSessionActivity(username, token);

            return SessionValidationResult.valid(securityLevel);

        } catch (ExpiredJwtException e) {
            return SessionValidationResult.invalid("Token expirado");
        } catch (JwtException e) {
            auditService.logSecurityEvent("JWT inválido: " + e.getMessage(),
                getClientIP(request), request.getHeader("User-Agent"));
            return SessionValidationResult.invalid("Token malformado");
        } catch (Exception e) {
            auditService.logSecurityEvent("Error en validación de sesión: " + e.getMessage(),
                getClientIP(request), request.getHeader("User-Agent"));
            return SessionValidationResult.invalid("Error interno de validación");
        }
    }

    /**
     * Valida el token JWT y extrae claims
     */
    private Claims validateJwtToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Realiza verificaciones de seguridad adicionales
     */
    private SecurityLevel performSecurityChecks(Claims claims, HttpServletRequest request, String username) {
        SecurityLevel level = SecurityLevel.HIGH;

        // Verificar IP consistency (si está en el token)
        String tokenIP = claims.get("ip", String.class);
        String currentIP = getClientIP(request);
        if (tokenIP != null && !tokenIP.equals(currentIP)) {
            level = SecurityLevel.MEDIUM;
            auditService.logSecurityEvent("Cambio de IP detectado para usuario " + username + 
                ": de " + tokenIP + " a " + currentIP, currentIP, request.getHeader("User-Agent"));
        }

        // Verificar User-Agent consistency
        String tokenUA = claims.get("userAgent", String.class);
        String currentUA = request.getHeader("User-Agent");
        if (tokenUA != null && !tokenUA.equals(currentUA)) {
            level = SecurityLevel.LOW;
        }

        // Verificar hora de emisión del token
        Date issuedAt = claims.getIssuedAt();
        if (issuedAt != null) {
            long hoursAgo = (System.currentTimeMillis() - issuedAt.getTime()) / (1000 * 60 * 60);
            if (hoursAgo > 24) {
                level = SecurityLevel.LOW;
            }
        }

        // Verificar actividad sospechosa por IP
        if (isSuspiciousActivity(currentIP)) {
            auditService.logSecurityEvent("Actividad sospechosa detectada desde IP: " + currentIP,
                currentIP, request.getHeader("User-Agent"));
            return SecurityLevel.BLOCKED;
        }

        return level;
    }

    /**
     * Valida límites de sesiones concurrentes
     */
    private boolean validateConcurrentSessions(String username, String token) {
        Set<String> userSessions = userActiveSessions.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet());
        
        // Limpiar tokens expirados
        userSessions.removeIf(this::isTokenExpired);
        
        // Si el token ya está en la lista, está bien
        if (userSessions.contains(getTokenSummary(token))) {
            return true;
        }
        
        // Si hay menos del límite, agregar y permitir
        if (userSessions.size() < MAX_CONCURRENT_SESSIONS) {
            userSessions.add(getTokenSummary(token));
            return true;
        }
        
        // Demasiadas sesiones
        return false;
    }

    /**
     * Detecta actividad sospechosa
     */
    private boolean isSuspiciousActivity(String ipAddress) {
        if (ipAddress == null) return false;
        
        // Verificar si la IP ha tenido muchos intentos fallidos recientemente
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<com.lsnls.entity.AuditLog> recentActivity = auditService.findSuspiciousActivity(ipAddress, oneHourAgo, LocalDateTime.now());
        
        // Si hay más de 10 eventos sospechosos en la última hora
        return recentActivity.size() > 10;
    }

    /**
     * Blacklist de tokens
     */
    public void blacklistToken(String token) {
        blacklistedTokens.add(getTokenSummary(token));
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(getTokenSummary(token));
    }

    /**
     * Gestión de sesiones activas
     */
    public void addActiveSession(String username, String token) {
        Set<String> userSessions = userActiveSessions.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet());
        userSessions.add(getTokenSummary(token));
    }

    public void removeActiveSession(String username, String token) {
        Set<String> userSessions = userActiveSessions.get(username);
        if (userSessions != null) {
            userSessions.remove(getTokenSummary(token));
            if (userSessions.isEmpty()) {
                userActiveSessions.remove(username);
            }
        }
    }

    public int getActiveSessionsCount(String username) {
        Set<String> userSessions = userActiveSessions.get(username);
        return userSessions != null ? userSessions.size() : 0;
    }

    /**
     * Invalidar todas las sesiones de un usuario
     */
    public void invalidateAllUserSessions(String username) {
        Set<String> userSessions = userActiveSessions.remove(username);
        if (userSessions != null) {
            blacklistedTokens.addAll(userSessions);
            auditService.logSecurityEvent("Todas las sesiones invalidadas para usuario: " + username,
                null, null);
        }
    }

    /**
     * Forzar cierre de sesión por seguridad
     */
    public void forceLogout(String username, String reason) {
        invalidateAllUserSessions(username);
        auditService.logSecurityEvent("Logout forzado para usuario " + username + ": " + reason,
            null, null);
    }

    /**
     * Validar timeout de sesión
     */
    public boolean isSessionTimedOut(Claims claims, Usuario.RolUsuario userRole) {
        Date lastActivity = claims.get("lastActivity", Date.class);
        if (lastActivity == null) {
            lastActivity = claims.getIssuedAt();
        }

        if (lastActivity == null) {
            return true; // Sin actividad registrada, considerar expirado
        }

        int timeoutMinutes = (userRole == Usuario.RolUsuario.ROLE_ADMIN || userRole == Usuario.RolUsuario.ROLE_DIRECCION) 
            ? ADMIN_SESSION_TIMEOUT_MINUTES 
            : SESSION_TIMEOUT_MINUTES;

        long minutesSinceActivity = (System.currentTimeMillis() - lastActivity.getTime()) / (1000 * 60);
        return minutesSinceActivity > timeoutMinutes;
    }

    /**
     * Métodos auxiliares
     */
    private void updateSessionActivity(String username, String token) {
        // En una implementación real, actualizarías el timestamp en el token o base de datos
        // Por simplicidad, solo registramos la actividad
    }

    private String getTokenSummary(String token) {
        if (token == null || token.length() < 20) {
            return token;
        }
        // Retornar solo una parte del token para identificación
        return token.substring(0, 10) + "..." + token.substring(token.length() - 10);
    }

    private boolean isTokenExpired(String tokenSummary) {
        // En una implementación real, validarías cada token
        // Por simplicidad, asumimos que si no está en blacklist, puede estar activo
        return blacklistedTokens.contains(tokenSummary);
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Obtener estadísticas de sesiones
     */
    public Map<String, Object> getSessionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalActiveUsers", userActiveSessions.size());
        stats.put("totalActiveSessions", userActiveSessions.values().stream().mapToInt(Set::size).sum());
        stats.put("blacklistedTokens", blacklistedTokens.size());
        stats.put("timestamp", LocalDateTime.now());
        return stats;
    }

    /**
     * Limpieza periódica de tokens expirados
     */
    public void cleanupExpiredTokens() {
        // Limpiar tokens blacklisted antiguos (más de 24 horas)
        blacklistedTokens.clear(); // En producción, filtrar por fecha
        
        // Limpiar sesiones inactivas
        userActiveSessions.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(this::isTokenExpired);
            return entry.getValue().isEmpty();
        });
    }
} 