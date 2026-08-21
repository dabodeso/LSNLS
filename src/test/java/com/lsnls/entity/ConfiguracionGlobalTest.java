package com.lsnls.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfiguracionGlobalTest {

    @Test
    void constructorConParametrosAsignaCampos() {
        ConfiguracionGlobal config = new ConfiguracionGlobal("tema", "oscuro", "Tema de la UI");

        assertEquals("tema", config.getClave());
        assertEquals("oscuro", config.getValor());
        assertEquals("Tema de la UI", config.getDescripcion());
    }

    @Test
    void constructorVacioYSetters() {
        ConfiguracionGlobal config = new ConfiguracionGlobal();
        assertNull(config.getClave());

        config.setId(4L);
        config.setVersion(1L);
        config.setClave("max_sesiones");
        config.setValor("3");
        config.setDescripcion("Límite");

        assertEquals(4L, config.getId());
        assertEquals(1L, config.getVersion());
        assertEquals("max_sesiones", config.getClave());
        assertEquals("3", config.getValor());
        assertEquals("Límite", config.getDescripcion());
    }
}
