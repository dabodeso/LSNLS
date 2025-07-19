package com.lsnls.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de rate limiting para proteger endpoints críticos contra abuso
 */
@Service
public class RateLimitingService {

    // Configuración de límites por endpoint
    private static final Map<String, RateLimit> RATE_LIMITS = Map.of(
        "AUTH_LOGIN", new RateLimit(5, 15), // 5 intentos por 15 minutos
        "CREATE_PREGUNTA", new RateLimit(20, 1), // 20 preguntas por minuto
        "CREATE_CUESTIONARIO", new RateLimit(10, 5), // 10 cuestionarios por 5 minutos
        "CREATE_COMBO", new RateLimit(5, 5), // 5 combos por 5 minutos
        "VALIDATION_SISTEMA", new RateLimit(3, 10), // 3 validaciones por 10 minutos
        "EXPORT_DATA", new RateLimit(2, 60), // 2 exportaciones por hora
        "BULK_OPERATIONS", new RateLimit(5, 10) // 5 operaciones batch por 10 minutos
    );

    // Cache de contadores por IP y usuario
    private final Map<String, RequestCounter> requestCounters = new ConcurrentHashMap<>();

    /**
     * Configuración de un límite de rate
     */
    private static class RateLimit {
        final int maxRequests;
        final int windowMinutes;

        RateLimit(int maxRequests, int windowMinutes) {
            this.maxRequests = maxRequests;
            this.windowMinutes = windowMinutes;
        }
    }

    /**
     * Contador de requests con ventana deslizante
     */
    private static class RequestCounter {
        private int count;
        private LocalDateTime windowStart;
        private final int windowMinutes;

        RequestCounter(int windowMinutes) {
            this.count = 0;
            this.windowStart = LocalDateTime.now();
            this.windowMinutes = windowMinutes;
        }

        synchronized boolean canProceed(int maxRequests) {
            LocalDateTime now = LocalDateTime.now();
            
            // Verificar si la ventana ha expirado
            if (ChronoUnit.MINUTES.between(windowStart, now) >= windowMinutes) {
                // Resetear ventana
                count = 0;
                windowStart = now;
            }
            
            // Verificar límite
            if (count >= maxRequests) {
                return false;
            }
            
            count++;
            return true;
        }

        synchronized int getRemainingRequests(int maxRequests) {
            return Math.max(0, maxRequests - count);
        }

        synchronized LocalDateTime getResetTime() {
            return windowStart.plusMinutes(windowMinutes);
        }
    }

    /**
     * Resultado de verificación de rate limit
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final int remainingRequests;
        private final LocalDateTime resetTime;
        private final String reason;

        private RateLimitResult(boolean allowed, int remainingRequests, LocalDateTime resetTime, String reason) {
            this.allowed = allowed;
            this.remainingRequests = remainingRequests;
            this.resetTime = resetTime;
            this.reason = reason;
        }

        public static RateLimitResult allowed(int remainingRequests, LocalDateTime resetTime) {
            return new RateLimitResult(true, remainingRequests, resetTime, null);
        }

        public static RateLimitResult blocked(String reason, LocalDateTime resetTime) {
            return new RateLimitResult(false, 0, resetTime, reason);
        }

        // Getters
        public boolean isAllowed() { return allowed; }
        public int getRemainingRequests() { return remainingRequests; }
        public LocalDateTime getResetTime() { return resetTime; }
        public String getReason() { return reason; }
    }

    /**
     * Verifica si una request está permitida bajo el rate limit
     */
    public RateLimitResult checkRateLimit(String endpoint, String clientId) {
        RateLimit limit = RATE_LIMITS.get(endpoint);
        if (limit == null) {
            // Sin límite configurado, permitir
            return RateLimitResult.allowed(Integer.MAX_VALUE, LocalDateTime.now().plusHours(1));
        }

        String key = endpoint + ":" + clientId;
        RequestCounter counter = requestCounters.computeIfAbsent(key, 
            k -> new RequestCounter(limit.windowMinutes));

        if (counter.canProceed(limit.maxRequests)) {
            int remaining = counter.getRemainingRequests(limit.maxRequests);
            return RateLimitResult.allowed(remaining, counter.getResetTime());
        } else {
            String reason = String.format("Rate limit excedido para %s. Máximo %d requests por %d minutos", 
                endpoint, limit.maxRequests, limit.windowMinutes);
            return RateLimitResult.blocked(reason, counter.getResetTime());
        }
    }

    /**
     * Verifica rate limit por IP
     */
    public RateLimitResult checkByIP(String endpoint, String ipAddress) {
        return checkRateLimit(endpoint, "IP:" + ipAddress);
    }

    /**
     * Verifica rate limit por usuario
     */
    public RateLimitResult checkByUser(String endpoint, String username) {
        return checkRateLimit(endpoint, "USER:" + username);
    }

    /**
     * Verifica rate limit global para el endpoint
     */
    public RateLimitResult checkGlobal(String endpoint) {
        return checkRateLimit(endpoint, "GLOBAL");
    }

    /**
     * Métodos específicos para endpoints comunes
     */

    /**
     * Verifica límite para intentos de login
     */
    public RateLimitResult checkLoginAttempt(String ipAddress, String username) {
        // Verificar por IP primero (más restrictivo)
        RateLimitResult ipResult = checkByIP("AUTH_LOGIN", ipAddress);
        if (!ipResult.isAllowed()) {
            return ipResult;
        }

        // Verificar por usuario
        return checkByUser("AUTH_LOGIN", username);
    }

    /**
     * Verifica límite para creación de preguntas
     */
    public RateLimitResult checkCreatePregunta(String username) {
        return checkByUser("CREATE_PREGUNTA", username);
    }

    /**
     * Verifica límite para creación de cuestionarios
     */
    public RateLimitResult checkCreateCuestionario(String username) {
        return checkByUser("CREATE_CUESTIONARIO", username);
    }

    /**
     * Verifica límite para creación de combos
     */
    public RateLimitResult checkCreateCombo(String username) {
        return checkByUser("CREATE_COMBO", username);
    }

    /**
     * Verifica límite para validaciones del sistema
     */
    public RateLimitResult checkSystemValidation(String username) {
        return checkByUser("VALIDATION_SISTEMA", username);
    }

    /**
     * Verifica límite para exportaciones
     */
    public RateLimitResult checkExport(String username) {
        return checkByUser("EXPORT_DATA", username);
    }

    /**
     * Verifica límite para operaciones batch
     */
    public RateLimitResult checkBulkOperation(String username, String ipAddress) {
        // Verificar por usuario
        RateLimitResult userResult = checkByUser("BULK_OPERATIONS", username);
        if (!userResult.isAllowed()) {
            return userResult;
        }

        // Verificar por IP para prevenir abuso desde múltiples cuentas
        return checkByIP("BULK_OPERATIONS", ipAddress);
    }

    /**
     * Permite reset manual de límites (para admins)
     */
    public void resetRateLimit(String endpoint, String clientId) {
        String key = endpoint + ":" + clientId;
        requestCounters.remove(key);
    }

    /**
     * Reset por IP
     */
    public void resetByIP(String endpoint, String ipAddress) {
        resetRateLimit(endpoint, "IP:" + ipAddress);
    }

    /**
     * Reset por usuario
     */
    public void resetByUser(String endpoint, String username) {
        resetRateLimit(endpoint, "USER:" + username);
    }

    /**
     * Obtiene estadísticas de rate limiting
     */
    public Map<String, Object> getRateLimitStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalActiveCounters", requestCounters.size());
        stats.put("configuredEndpoints", RATE_LIMITS.keySet());
        stats.put("timestamp", LocalDateTime.now());
        
        // Contar contadores activos por endpoint
        Map<String, Integer> countersByEndpoint = new java.util.HashMap<>();
        for (String key : requestCounters.keySet()) {
            String endpoint = key.split(":")[0];
            countersByEndpoint.merge(endpoint, 1, Integer::sum);
        }
        stats.put("countersByEndpoint", countersByEndpoint);
        
        return stats;
    }

    /**
     * Limpieza periódica de contadores expirados
     */
    public void cleanupExpiredCounters() {
        LocalDateTime now = LocalDateTime.now();
        requestCounters.entrySet().removeIf(entry -> {
            RequestCounter counter = entry.getValue();
            // Remover contadores que no han sido usados en la última hora
            return ChronoUnit.MINUTES.between(counter.windowStart, now) > 60;
        });
    }

    /**
     * Configuración dinámica de límites (para casos especiales)
     */
    public void setTemporaryLimit(String endpoint, int maxRequests, int windowMinutes, int durationMinutes) {
        // En una implementación completa, esto podría almacenarse temporalmente
        // y aplicarse solo durante el período especificado
        String tempKey = "TEMP_" + endpoint;
        RateLimit tempLimit = new RateLimit(maxRequests, windowMinutes);
        // Almacenar temporalmente...
    }

    /**
     * Verificar si una IP está en lista negra temporal
     */
    public boolean isTemporarilyBlocked(String ipAddress) {
        // Verificar si la IP ha excedido múltiples límites recientemente
        String prefix = "IP:" + ipAddress;
        long blockedEndpoints = requestCounters.entrySet().stream()
            .filter(entry -> entry.getKey().contains(prefix))
            .map(entry -> {
                String endpoint = entry.getKey().split(":")[0];
                RateLimit limit = RATE_LIMITS.get(endpoint);
                if (limit != null) {
                    return !entry.getValue().canProceed(limit.maxRequests);
                }
                return false;
            })
            .mapToInt(blocked -> blocked ? 1 : 0)
            .sum();
            
        // Si está bloqueada en 3 o más endpoints, considerar como bloqueada temporalmente
        return blockedEndpoints >= 3;
    }

    /**
     * Obtener información de rate limit para un cliente específico
     */
    public Map<String, Object> getRateLimitInfo(String endpoint, String clientId) {
        RateLimit limit = RATE_LIMITS.get(endpoint);
        if (limit == null) {
            return Map.of("error", "Endpoint no configurado");
        }

        String key = endpoint + ":" + clientId;
        RequestCounter counter = requestCounters.get(key);
        
        Map<String, Object> info = new java.util.HashMap<>();
        info.put("endpoint", endpoint);
        info.put("maxRequests", limit.maxRequests);
        info.put("windowMinutes", limit.windowMinutes);
        
        if (counter != null) {
            info.put("remainingRequests", counter.getRemainingRequests(limit.maxRequests));
            info.put("resetTime", counter.getResetTime());
        } else {
            info.put("remainingRequests", limit.maxRequests);
            info.put("resetTime", LocalDateTime.now().plusMinutes(limit.windowMinutes));
        }
        
        return info;
    }
} 