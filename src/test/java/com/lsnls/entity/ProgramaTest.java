package com.lsnls.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgramaTest {

    @Test
    void actualizarEstadoConFechaEmisionEsProgramado() {
        Programa programa = new Programa();
        programa.setFechaEmision(LocalDate.now());
        programa.setDuracionAcumulada(LocalTime.of(1, 0));
        programa.setResultadoAcumulado(BigDecimal.TEN);

        programa.actualizarEstado();

        assertEquals(Programa.EstadoPrograma.programado, programa.getEstado());
    }

    @Test
    void actualizarEstadoConDuracionAcumuladaEsEditado() {
        Programa programa = new Programa();
        programa.setDuracionAcumulada(LocalTime.of(0, 45));
        programa.setResultadoAcumulado(BigDecimal.ONE);

        programa.actualizarEstado();

        assertEquals(Programa.EstadoPrograma.editado, programa.getEstado());
    }

    @Test
    void actualizarEstadoConResultadoAcumuladoEsGrabado() {
        Programa programa = new Programa();
        programa.setResultadoAcumulado(new BigDecimal("100.50"));

        programa.actualizarEstado();

        assertEquals(Programa.EstadoPrograma.grabado, programa.getEstado());
    }

    @Test
    void actualizarEstadoSinDatosEsBorrador() {
        Programa programa = new Programa();

        programa.actualizarEstado();

        assertEquals(Programa.EstadoPrograma.borrador, programa.getEstado());
    }
}
