package com.lsnls.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Configuración de cache para mejorar el rendimiento del sistema
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Gestor de cache simple para desarrollo
     * En producción se debería usar Redis o Hazelcast
     */
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // Configurar caches específicos
        cacheManager.setCacheNames(Arrays.asList(
            "preguntas-disponibles",      // Cache de preguntas disponibles por nivel
            "cuestionarios-activos",      // Cache de cuestionarios en estados activos
            "combos-activos",             // Cache de combos en estados activos
            "configuracion-global",       // Cache de configuración global
            "estadisticas-sistema",       // Cache de estadísticas del sistema
            "usuarios-activos",           // Cache de usuarios activos
            "validaciones-integridad",    // Cache de resultados de validación
            "programas-vigentes"          // Cache de programas vigentes
        ));
        
        // Permitir la creación dinámica de caches no declarados
        cacheManager.setAllowNullValues(false);
        
        return cacheManager;
    }
} 