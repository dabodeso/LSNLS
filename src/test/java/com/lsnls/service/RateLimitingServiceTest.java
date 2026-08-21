package com.lsnls.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitingServiceTest {

    private RateLimitingService service;

    @BeforeEach
    void setUp() {
        service = new RateLimitingService();
    }

    @Test
    void checkLoginAttempt_permiteHastaLimiteYLuegoBloquea() {
        RateLimitingService.RateLimitResult ultimoPermitido = null;
        for (int i = 0; i < 5; i++) {
            ultimoPermitido = service.checkLoginAttempt("10.0.0.1", "user1");
            assertTrue(ultimoPermitido.isAllowed());
        }
        assertNotNull(ultimoPermitido);
        assertEquals(0, ultimoPermitido.getRemainingRequests());

        RateLimitingService.RateLimitResult bloqueado = service.checkLoginAttempt("10.0.0.1", "user1");
        assertFalse(bloqueado.isAllowed());
        assertEquals(0, bloqueado.getRemainingRequests());
        assertNotNull(bloqueado.getReason());
        assertTrue(bloqueado.getReason().contains("AUTH_LOGIN"));
        assertNotNull(bloqueado.getResetTime());
    }

    @Test
    void reset_permiteDeNuevo() {
        for (int i = 0; i < 5; i++) {
            service.checkLoginAttempt("10.0.0.2", "user2");
        }
        assertFalse(service.checkLoginAttempt("10.0.0.2", "user2").isAllowed());

        service.resetByIP("AUTH_LOGIN", "10.0.0.2");
        service.resetByUser("AUTH_LOGIN", "user2");

        assertTrue(service.checkLoginAttempt("10.0.0.2", "user2").isAllowed());
    }

    @Test
    void resetRateLimit_directo() {
        service.checkRateLimit("CREATE_PREGUNTA", "USER:ana");
        service.resetRateLimit("CREATE_PREGUNTA", "USER:ana");
        Map<String, Object> info = service.getRateLimitInfo("CREATE_PREGUNTA", "USER:ana");
        assertEquals(20, info.get("remainingRequests"));
    }

    @Test
    void checkCreatePreguntaCuestionarioComboExportBulkYValidacion() {
        assertTrue(service.checkCreatePregunta("ana").isAllowed());
        assertTrue(service.checkCreateCuestionario("ana").isAllowed());
        assertTrue(service.checkCreateCombo("ana").isAllowed());
        assertTrue(service.checkSystemValidation("ana").isAllowed());
        assertTrue(service.checkExport("ana").isAllowed());
        assertTrue(service.checkBulkOperation("ana", "1.1.1.1").isAllowed());
    }

    @Test
    void checkExport_bloqueaAlTercerIntento() {
        assertTrue(service.checkExport("exportador").isAllowed());
        assertTrue(service.checkExport("exportador").isAllowed());
        RateLimitingService.RateLimitResult tercero = service.checkExport("exportador");
        assertFalse(tercero.isAllowed());
    }

    @Test
    void checkBulkOperation_bloqueaPorUsuario() {
        for (int i = 0; i < 5; i++) {
            assertTrue(service.checkBulkOperation("bulkUser", "9.9.9.9").isAllowed());
        }
        assertFalse(service.checkBulkOperation("bulkUser", "9.9.9.9").isAllowed());
    }

    @Test
    void checkRateLimit_endpointDesconocido_permite() {
        RateLimitingService.RateLimitResult result = service.checkRateLimit("NO_EXISTE", "x");
        assertTrue(result.isAllowed());
        assertEquals(Integer.MAX_VALUE, result.getRemainingRequests());
    }

    @Test
    void checkGlobalYByIP() {
        assertTrue(service.checkGlobal("CREATE_COMBO").isAllowed());
        assertTrue(service.checkByIP("CREATE_COMBO", "8.8.8.8").isAllowed());
    }

    @Test
    void getRateLimitStats() {
        service.checkCreatePregunta("stats");
        Map<String, Object> stats = service.getRateLimitStats();
        assertTrue((Integer) stats.get("totalActiveCounters") >= 1);
        assertNotNull(stats.get("configuredEndpoints"));
        assertNotNull(stats.get("timestamp"));
        assertNotNull(stats.get("countersByEndpoint"));
    }

    @Test
    void getRateLimitInfo_endpointDesconocidoYSinContador() {
        Map<String, Object> error = service.getRateLimitInfo("UNKNOWN", "x");
        assertEquals("Endpoint no configurado", error.get("error"));

        Map<String, Object> info = service.getRateLimitInfo("CREATE_PREGUNTA", "USER:nuevo");
        assertEquals("CREATE_PREGUNTA", info.get("endpoint"));
        assertEquals(20, info.get("maxRequests"));
        assertEquals(1, info.get("windowMinutes"));
        assertEquals(20, info.get("remainingRequests"));
        assertNotNull(info.get("resetTime"));
    }

    @Test
    void getRateLimitInfo_conContadorExistente() {
        service.checkCreatePregunta("infoUser");
        Map<String, Object> info = service.getRateLimitInfo("CREATE_PREGUNTA", "USER:infoUser");
        assertEquals(19, info.get("remainingRequests"));
    }

    @Test
    void isTemporarilyBlocked() {
        assertFalse(service.isTemporarilyBlocked("3.3.3.3"));

        for (int i = 0; i < 6; i++) {
            service.checkRateLimit("AUTH_LOGIN", "IP:3.3.3.3");
        }
        for (int i = 0; i < 21; i++) {
            service.checkRateLimit("CREATE_PREGUNTA", "IP:3.3.3.3");
        }
        for (int i = 0; i < 3; i++) {
            service.checkRateLimit("EXPORT_DATA", "IP:3.3.3.3");
        }

        assertTrue(service.isTemporarilyBlocked("3.3.3.3"));
    }

    @Test
    void cleanupExpiredCounters_eliminaAntiguos() throws Exception {
        service.checkCreatePregunta("limpio");
        envejecerContadores(61);
        service.cleanupExpiredCounters();
        Map<String, Object> stats = service.getRateLimitStats();
        assertEquals(0, stats.get("totalActiveCounters"));
    }

    @Test
    void cleanupExpiredCounters_conservaRecientes() {
        service.checkCreatePregunta("reciente");
        service.cleanupExpiredCounters();
        Map<String, Object> stats = service.getRateLimitStats();
        assertEquals(1, stats.get("totalActiveCounters"));
    }

    @Test
    void setTemporaryLimit_noLanza() {
        service.setTemporaryLimit("AUTH_LOGIN", 1, 1, 5);
    }

    @Test
    void ventanaExpirada_reseteaContador() throws Exception {
        for (int i = 0; i < 5; i++) {
            service.checkCreateCombo("ventana");
        }
        assertFalse(service.checkCreateCombo("ventana").isAllowed());
        envejecerContadores(6);
        assertTrue(service.checkCreateCombo("ventana").isAllowed());
    }

    @SuppressWarnings("unchecked")
    private void envejecerContadores(int minutos) throws Exception {
        Field countersField = RateLimitingService.class.getDeclaredField("requestCounters");
        countersField.setAccessible(true);
        Map<String, Object> counters = (Map<String, Object>) countersField.get(service);
        for (Object counter : counters.values()) {
            Field windowStart = counter.getClass().getDeclaredField("windowStart");
            windowStart.setAccessible(true);
            windowStart.set(counter, LocalDateTime.now().minusMinutes(minutos));
        }
    }
}
