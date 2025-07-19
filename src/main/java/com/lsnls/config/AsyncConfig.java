package com.lsnls.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuración para operaciones asíncronas con timeouts y pools de threads
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Pool de threads para operaciones de auditoría
     */
    @Bean(name = "auditTaskExecutor")
    public Executor auditTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("audit-");
        executor.setKeepAliveSeconds(60);
        
        // Política de rechazo: ejecutar en el hilo llamador si está lleno
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // Permitir que los threads core mueran si están inactivos
        executor.setAllowCoreThreadTimeOut(true);
        
        executor.initialize();
        return executor;
    }

    /**
     * Pool de threads para operaciones de validación
     */
    @Bean(name = "validationTaskExecutor")
    public Executor validationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("validation-");
        executor.setKeepAliveSeconds(30);
        
        // Política de rechazo: descartar tareas más antiguas si está lleno
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        
        executor.setAllowCoreThreadTimeOut(true);
        
        executor.initialize();
        return executor;
    }

    /**
     * Pool de threads para operaciones de exportación/importación
     */
    @Bean(name = "exportTaskExecutor")
    public Executor exportTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("export-");
        executor.setKeepAliveSeconds(120);
        
        // Política de rechazo: rechazar tareas nuevas si está lleno
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        
        executor.setAllowCoreThreadTimeOut(true);
        
        executor.initialize();
        return executor;
    }

    /**
     * Pool general para operaciones críticas con timeout
     */
    @Bean(name = "criticalTaskExecutor")
    public Executor criticalTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("critical-");
        executor.setKeepAliveSeconds(45);
        
        // Política de rechazo: ejecutar en el hilo llamador para operaciones críticas
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.setAllowCoreThreadTimeOut(false); // Mantener threads core vivos para operaciones críticas
        
        executor.initialize();
        return executor;
    }
} 