package com.lsnls.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CacheConfigTest {

    @Test
    void cacheManagerExponeCachesDeclarados() {
        CacheManager manager = new CacheConfig().cacheManager();
        assertNotNull(manager.getCache("preguntas-disponibles"));
        assertNotNull(manager.getCache("cuestionarios-activos"));
        assertNotNull(manager.getCache("combos-activos"));
        assertNotNull(manager.getCache("configuracion-global"));
        assertNotNull(manager.getCache("estadisticas-sistema"));
        assertNotNull(manager.getCache("usuarios-activos"));
        assertNotNull(manager.getCache("validaciones-integridad"));
        assertNotNull(manager.getCache("programas-vigentes"));
    }
}
