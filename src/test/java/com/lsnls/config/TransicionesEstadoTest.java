package com.lsnls.config;

import com.lsnls.entity.Combo.EstadoCombo;
import com.lsnls.entity.Cuestionario.EstadoCuestionario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransicionesEstadoTest {

    @Test
    void nullOMismoEstadoNoLanza() {
        assertDoesNotThrow(() -> TransicionesEstado.validar(null, EstadoCombo.revisar, false));
        assertDoesNotThrow(() -> TransicionesEstado.validar(EstadoCombo.borrador, null, false));
        assertDoesNotThrow(() -> TransicionesEstado.validar(
                EstadoCuestionario.revisar, EstadoCuestionario.revisar, false));
    }

    @Test
    void mensajeDeTransicionProhibida() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> TransicionesEstado.validar(EstadoCombo.borrador, EstadoCombo.aprobado, false));
        assertEquals("Transición de estado no permitida: borrador -> aprobado", ex.getMessage());
    }
}
