package com.lsnls.service;

import com.lsnls.entity.AuditLog;
import com.lsnls.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionValidationServiceTest {

    private static final String JWT_SECRET = "0123456789abcdef0123456789abcdef";

    @Mock
    private AuditService auditService;

    @InjectMocks
    private SessionValidationService sessionValidationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sessionValidationService, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(sessionValidationService, "jwtExpirationInMs", 86400000);
    }

    @Test
    void blacklistTokenEIsTokenBlacklisted() {
        String token = "abcdefghijXXXXXXXXXXklmnopqrst";

        assertFalse(sessionValidationService.isTokenBlacklisted(token));
        sessionValidationService.blacklistToken(token);
        assertTrue(sessionValidationService.isTokenBlacklisted(token));
    }

    @Test
    void blacklistTokenCortoUsaElTokenEntero() {
        sessionValidationService.blacklistToken("corto");
        assertTrue(sessionValidationService.isTokenBlacklisted("corto"));
    }

    @Test
    void addRemoveActiveSessionYCount() {
        String token = crearToken("ana", new Date(), new Date(System.currentTimeMillis() + 3600000));

        sessionValidationService.addActiveSession("ana", token);
        assertEquals(1, sessionValidationService.getActiveSessionsCount("ana"));
        assertEquals(0, sessionValidationService.getActiveSessionsCount("bruno"));

        sessionValidationService.removeActiveSession("ana", token);
        assertEquals(0, sessionValidationService.getActiveSessionsCount("ana"));
        sessionValidationService.removeActiveSession("nadie", token);
    }

    @Test
    void invalidateAllUserSessionsBlacklistea() {
        String token = crearToken("ana", new Date(), new Date(System.currentTimeMillis() + 3600000));
        sessionValidationService.addActiveSession("ana", token);

        sessionValidationService.invalidateAllUserSessions("ana");

        assertEquals(0, sessionValidationService.getActiveSessionsCount("ana"));
        assertTrue(sessionValidationService.isTokenBlacklisted(token));
        verify(auditService).logSecurityEvent(anyString(), any(), any());
    }

    @Test
    void forceLogoutInvalidaYAudita() {
        String token = crearToken("ana", new Date(), new Date(System.currentTimeMillis() + 3600000));
        sessionValidationService.addActiveSession("ana", token);

        sessionValidationService.forceLogout("ana", "sospecha");

        assertEquals(0, sessionValidationService.getActiveSessionsCount("ana"));
        assertTrue(sessionValidationService.isTokenBlacklisted(token));
    }

    @Test
    void isSessionTimedOutLastActivityNuloYIssuedAtNulo() {
        Claims claims = mock(Claims.class);
        when(claims.get("lastActivity", Date.class)).thenReturn(null);
        when(claims.getIssuedAt()).thenReturn(null);

        assertTrue(sessionValidationService.isSessionTimedOut(claims, Usuario.RolUsuario.ROLE_GUION));
    }

    @Test
    void isSessionTimedOutUsaIssuedAtSiNoHayLastActivity() {
        Claims claims = mock(Claims.class);
        when(claims.get("lastActivity", Date.class)).thenReturn(null);
        when(claims.getIssuedAt()).thenReturn(new Date());

        assertFalse(sessionValidationService.isSessionTimedOut(claims, Usuario.RolUsuario.ROLE_GUION));
    }

    @Test
    void isSessionTimedOutAdminVsGuion() {
        Date hace45Min = new Date(System.currentTimeMillis() - 45L * 60L * 1000L);
        Claims claims = mock(Claims.class);
        when(claims.get("lastActivity", Date.class)).thenReturn(hace45Min);

        assertFalse(sessionValidationService.isSessionTimedOut(claims, Usuario.RolUsuario.ROLE_ADMIN));
        assertFalse(sessionValidationService.isSessionTimedOut(claims, Usuario.RolUsuario.ROLE_DIRECCION));
        assertTrue(sessionValidationService.isSessionTimedOut(claims, Usuario.RolUsuario.ROLE_GUION));
    }

    @Test
    void getSessionStats() {
        String token = crearToken("ana", new Date(), new Date(System.currentTimeMillis() + 3600000));
        sessionValidationService.addActiveSession("ana", token);
        sessionValidationService.blacklistToken(token);

        Map<String, Object> stats = sessionValidationService.getSessionStats();

        assertEquals(1, stats.get("totalActiveUsers"));
        assertEquals(1, stats.get("totalActiveSessions"));
        assertEquals(1, stats.get("blacklistedTokens"));
        assertNotNull(stats.get("timestamp"));
    }

    @Test
    void cleanupExpiredTokensLimpiaBlacklist() {
        String token = crearToken("ana", new Date(), new Date(System.currentTimeMillis() + 3600000));
        sessionValidationService.blacklistToken(token);
        sessionValidationService.addActiveSession("ana", token);

        sessionValidationService.cleanupExpiredTokens();

        assertFalse(sessionValidationService.isTokenBlacklisted(token));
        assertEquals(1, sessionValidationService.getActiveSessionsCount("ana"));
    }

    @Test
    void validateSessionConTokenBlacklisted() {
        String token = crearToken("ana", new Date(), new Date(System.currentTimeMillis() + 3600000));
        sessionValidationService.blacklistToken(token);
        HttpServletRequest request = mockRequest("127.0.0.1", "JUnit");

        SessionValidationService.SessionValidationResult result =
                sessionValidationService.validateSession(token, request);

        assertFalse(result.isValid());
        assertEquals("Token en lista negra", result.getReason());
        assertEquals(SessionValidationService.SecurityLevel.BLOCKED, result.getSecurityLevel());
        verify(auditService).logSecurityEvent(anyString(), anyString(), anyString());
    }

    @Test
    void validateSessionTokenInvalido() {
        HttpServletRequest request = mockRequest("127.0.0.1", "JUnit");

        SessionValidationService.SessionValidationResult result =
                sessionValidationService.validateSession("no-es-un-jwt", request);

        assertFalse(result.isValid());
        assertEquals("Token JWT inválido", result.getReason());
    }

    @Test
    void validateSessionValido() {
        when(auditService.findSuspiciousActivity(anyString(), any(), any()))
                .thenReturn(new ArrayList<AuditLog>());
        String token = crearToken("ana", new Date(), new Date(System.currentTimeMillis() + 3600000));
        HttpServletRequest request = mockRequest("127.0.0.1", "JUnit");

        SessionValidationService.SessionValidationResult result =
                sessionValidationService.validateSession(token, request);

        assertTrue(result.isValid());
        assertEquals(SessionValidationService.SecurityLevel.HIGH, result.getSecurityLevel());
    }

    @Test
    void validateSessionDemasiadasSesiones() {
        when(auditService.findSuspiciousActivity(anyString(), any(), any()))
                .thenReturn(new ArrayList<AuditLog>());
        HttpServletRequest request = mockRequest("127.0.0.1", "JUnit");
        for (int i = 0; i < 3; i++) {
            String extra = crearTokenConSufijo("ana", "tokenseed" + i);
            sessionValidationService.addActiveSession("ana", extra);
        }
        String token = crearTokenConSufijo("ana", "tokenseedX");

        SessionValidationService.SessionValidationResult result =
                sessionValidationService.validateSession(token, request);

        assertFalse(result.isValid());
        assertEquals("Demasiadas sesiones activas", result.getReason());
    }

    @Test
    void validateSessionActividadSospechosa() {
        List<AuditLog> muchos = new ArrayList<AuditLog>();
        for (int i = 0; i < 11; i++) {
            muchos.add(new AuditLog());
        }
        when(auditService.findSuspiciousActivity(anyString(), any(), any())).thenReturn(muchos);
        String token = crearToken("ana", new Date(), new Date(System.currentTimeMillis() + 3600000));
        HttpServletRequest request = mockRequest("10.0.0.1", "JUnit");

        SessionValidationService.SessionValidationResult result =
                sessionValidationService.validateSession(token, request);

        assertFalse(result.isValid());
        assertEquals("Sesión bloqueada por seguridad", result.getReason());
    }

    private HttpServletRequest mockRequest(String ip, String ua) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        org.mockito.Mockito.lenient().when(request.getHeader("X-Forwarded-For")).thenReturn(ip);
        org.mockito.Mockito.lenient().when(request.getHeader("User-Agent")).thenReturn(ua);
        return request;
    }

    private String crearToken(String subject, Date issuedAt, Date expiration) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(key)
                .compact();
    }

    private String crearTokenConSufijo(String subject, String nonce) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .claim("nonce", nonce)
                .signWith(key)
                .compact();
    }
}
