package com.lsnls.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeoutOperationServiceTest {

    private TimeoutOperationService service;

    @BeforeEach
    void setUp() {
        service = new TimeoutOperationService();
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Test
    void executeWithTimeout_exito() {
        TimeoutOperationService.TimeoutResult<String> result =
                service.executeWithTimeout(() -> "ok", 50, TimeUnit.MILLISECONDS);

        assertTrue(result.isSuccess());
        assertEquals("ok", result.getResult());
        assertFalse(result.isTimedOut());
        assertNull(result.getErrorMessage());
    }

    @Test
    void executeWithTimeout_falloPorExcepcion() {
        TimeoutOperationService.TimeoutResult<String> result =
                service.executeWithTimeout(() -> {
                    throw new RuntimeException("boom");
                }, 50, TimeUnit.MILLISECONDS);

        assertFalse(result.isSuccess());
        assertFalse(result.isTimedOut());
        assertNull(result.getResult());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Error en la operación"));
    }

    @Test
    void executeWithTimeout_timeoutConSleep() {
        TimeoutOperationService.TimeoutResult<String> result =
                service.executeWithTimeout(() -> {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return "tarde";
                }, 50, TimeUnit.MILLISECONDS);

        assertFalse(result.isSuccess());
        assertTrue(result.isTimedOut());
        assertEquals("Operación cancelada por timeout", result.getErrorMessage());
    }

    @Test
    void executeWithRetries_exitoAlPrimerIntento() {
        TimeoutOperationService.TimeoutResult<Integer> result =
                service.executeWithRetries(() -> 1, 3, 50, TimeUnit.MILLISECONDS);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getResult());
    }

    @Test
    void executeWithRetries_reintentaTrasFallo() {
        AtomicInteger intentos = new AtomicInteger();
        TimeoutOperationService.TimeoutResult<String> result = service.executeWithRetries(() -> {
            if (intentos.incrementAndGet() == 1) {
                throw new RuntimeException("fallo temporal");
            }
            return "recuperado";
        }, 2, 50, TimeUnit.MILLISECONDS);

        assertTrue(result.isSuccess());
        assertEquals("recuperado", result.getResult());
        assertEquals(2, intentos.get());
    }

    @Test
    void executeWithRetries_noReintentaSiTimeout() {
        AtomicInteger intentos = new AtomicInteger();
        TimeoutOperationService.TimeoutResult<String> result = service.executeWithRetries(() -> {
            intentos.incrementAndGet();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "x";
        }, 3, 50, TimeUnit.MILLISECONDS);

        assertTrue(result.isTimedOut());
        assertEquals(1, intentos.get());
    }

    @Test
    void executeQueryCreationUpdateValidationExport() {
        assertTrue(service.executeQuery(() -> 10).isSuccess());
        assertTrue(service.executeCreation(() -> "creado").isSuccess());
        assertTrue(service.executeUpdate(() -> true).isSuccess());
        assertTrue(service.executeValidation(() -> "valido").isSuccess());
        assertTrue(service.executeExport(() -> "excel").isSuccess());
    }

    @Test
    void executeConOperationTypeYBatchImport() {
        assertTrue(service.executeWithTimeout(TimeoutOperationService.OperationType.QUERY, () -> 1).isSuccess());
        assertTrue(service.executeWithTimeout(TimeoutOperationService.OperationType.CREATION, () -> 1).isSuccess());
        assertTrue(service.executeWithTimeout(TimeoutOperationService.OperationType.UPDATE, () -> 1).isSuccess());
        assertTrue(service.executeWithTimeout(TimeoutOperationService.OperationType.VALIDATION, () -> 1).isSuccess());
        assertTrue(service.executeWithTimeout(TimeoutOperationService.OperationType.EXPORT, () -> 1).isSuccess());
        assertTrue(service.executeWithTimeout(TimeoutOperationService.OperationType.BATCH, () -> 1).isSuccess());
        assertTrue(service.executeBatchOperation(() -> "batch").isSuccess());
        assertTrue(service.executeImport(() -> "import").isSuccess());
        assertEquals(30, TimeoutOperationService.OperationType.QUERY.getDefaultTimeoutSeconds());
    }

    @Test
    void getActiveOperationsYCompleted() {
        assertTrue(service.getActiveOperations() >= 0);
        service.executeQuery(() -> "x");
        assertTrue(service.getCompletedOperations() >= 0);
    }

    @Test
    void timeoutResultFactories() {
        TimeoutOperationService.TimeoutResult<String> ok = TimeoutOperationService.TimeoutResult.success("a");
        assertTrue(ok.isSuccess());
        TimeoutOperationService.TimeoutResult<String> fail = TimeoutOperationService.TimeoutResult.failure("e");
        assertEquals("e", fail.getErrorMessage());
        TimeoutOperationService.TimeoutResult<String> to = TimeoutOperationService.TimeoutResult.timeout();
        assertTrue(to.isTimedOut());
    }
}
