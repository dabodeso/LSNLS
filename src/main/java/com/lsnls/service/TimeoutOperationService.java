package com.lsnls.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Servicio para ejecutar operaciones con timeouts configurables
 * Previene que operaciones largas se cuelguen indefinidamente
 */
@Service
public class TimeoutOperationService {

    // Timeouts por defecto en segundos
    private static final int DEFAULT_QUERY_TIMEOUT = 30;
    private static final int DEFAULT_CREATION_TIMEOUT = 60;
    private static final int DEFAULT_UPDATE_TIMEOUT = 45;
    private static final int DEFAULT_VALIDATION_TIMEOUT = 15;
    private static final int DEFAULT_EXPORT_TIMEOUT = 300; // 5 minutos

    private final ExecutorService timeoutExecutor;

    public TimeoutOperationService() {
        this.timeoutExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "timeout-operation");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Resultado de una operación con timeout
     */
    public static class TimeoutResult<T> {
        private final boolean success;
        private final T result;
        private final String errorMessage;
        private final boolean timedOut;

        private TimeoutResult(boolean success, T result, String errorMessage, boolean timedOut) {
            this.success = success;
            this.result = result;
            this.errorMessage = errorMessage;
            this.timedOut = timedOut;
        }

        public static <T> TimeoutResult<T> success(T result) {
            return new TimeoutResult<>(true, result, null, false);
        }

        public static <T> TimeoutResult<T> failure(String errorMessage) {
            return new TimeoutResult<>(false, null, errorMessage, false);
        }

        public static <T> TimeoutResult<T> timeout() {
            return new TimeoutResult<>(false, null, "Operación cancelada por timeout", true);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public T getResult() { return result; }
        public String getErrorMessage() { return errorMessage; }
        public boolean isTimedOut() { return timedOut; }
    }

    /**
     * Ejecuta una operación con timeout por defecto según el tipo
     */
    public <T> TimeoutResult<T> executeWithTimeout(OperationType operationType, Supplier<T> operation) {
        int timeoutSeconds = getDefaultTimeout(operationType);
        return executeWithTimeout(operation, timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * Ejecuta una operación con timeout personalizado
     */
    public <T> TimeoutResult<T> executeWithTimeout(Supplier<T> operation, long timeout, TimeUnit timeUnit) {
        Future<T> future = timeoutExecutor.submit(() -> {
            try {
                return operation.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        try {
            T result = future.get(timeout, timeUnit);
            return TimeoutResult.success(result);
        } catch (TimeoutException e) {
            future.cancel(true);
            return TimeoutResult.timeout();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            return TimeoutResult.failure("Error en la operación: " + 
                (cause != null ? cause.getMessage() : e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            return TimeoutResult.failure("Operación interrumpida");
        }
    }

    /**
     * Ejecuta una operación crítica con timeout y reintentos
     */
    public <T> TimeoutResult<T> executeWithRetries(Supplier<T> operation, int maxRetries, long timeout, TimeUnit timeUnit) {
        TimeoutResult<T> lastResult = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            lastResult = executeWithTimeout(operation, timeout, timeUnit);
            
            if (lastResult.isSuccess()) {
                return lastResult;
            }
            
            // Si fue timeout, no reintentar (probablemente problema de recursos)
            if (lastResult.isTimedOut()) {
                break;
            }
            
            // Esperar antes del siguiente intento (backoff exponencial)
            if (attempt < maxRetries) {
                try {
                    Thread.sleep(Math.min(1000 * attempt, 5000)); // máximo 5 segundos
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        return lastResult;
    }

    /**
     * Métodos específicos para operaciones comunes
     */

    /**
     * Ejecuta una query de base de datos con timeout
     */
    public <T> TimeoutResult<T> executeQuery(Supplier<T> query) {
        return executeWithTimeout(query, DEFAULT_QUERY_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Ejecuta una operación de creación con timeout
     */
    public <T> TimeoutResult<T> executeCreation(Supplier<T> creation) {
        return executeWithTimeout(creation, DEFAULT_CREATION_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Ejecuta una operación de actualización con timeout
     */
    public <T> TimeoutResult<T> executeUpdate(Supplier<T> update) {
        return executeWithTimeout(update, DEFAULT_UPDATE_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Ejecuta una validación con timeout
     */
    public <T> TimeoutResult<T> executeValidation(Supplier<T> validation) {
        return executeWithTimeout(validation, DEFAULT_VALIDATION_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Ejecuta una exportación con timeout extendido
     */
    public <T> TimeoutResult<T> executeExport(Supplier<T> export) {
        return executeWithTimeout(export, DEFAULT_EXPORT_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Ejecuta una operación batch con timeout y reintentos
     */
    public <T> TimeoutResult<T> executeBatchOperation(Supplier<T> batchOperation) {
        return executeWithRetries(batchOperation, 3, DEFAULT_CREATION_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Ejecuta una operación de importación con timeout extendido y reintentos
     */
    public <T> TimeoutResult<T> executeImport(Supplier<T> importOperation) {
        return executeWithRetries(importOperation, 2, DEFAULT_EXPORT_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * Tipos de operaciones con timeouts por defecto
     */
    public enum OperationType {
        QUERY(DEFAULT_QUERY_TIMEOUT),
        CREATION(DEFAULT_CREATION_TIMEOUT),
        UPDATE(DEFAULT_UPDATE_TIMEOUT),
        VALIDATION(DEFAULT_VALIDATION_TIMEOUT),
        EXPORT(DEFAULT_EXPORT_TIMEOUT),
        BATCH(DEFAULT_CREATION_TIMEOUT);

        private final int defaultTimeoutSeconds;

        OperationType(int defaultTimeoutSeconds) {
            this.defaultTimeoutSeconds = defaultTimeoutSeconds;
        }

        public int getDefaultTimeoutSeconds() {
            return defaultTimeoutSeconds;
        }
    }

    private int getDefaultTimeout(OperationType operationType) {
        return operationType.getDefaultTimeoutSeconds();
    }

    /**
     * Monitoreo de operaciones en progreso
     */
    public int getActiveOperations() {
        if (timeoutExecutor instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) timeoutExecutor).getActiveCount();
        }
        return 0;
    }

    public long getCompletedOperations() {
        if (timeoutExecutor instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) timeoutExecutor).getCompletedTaskCount();
        }
        return 0;
    }

    /**
     * Limpieza de recursos
     */
    public void shutdown() {
        timeoutExecutor.shutdown();
        try {
            if (!timeoutExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                timeoutExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            timeoutExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
} 