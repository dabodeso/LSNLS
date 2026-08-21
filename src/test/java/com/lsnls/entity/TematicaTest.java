package com.lsnls.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TematicaTest {

    @Test
    void constructorAsignaNombreUsuarioYFecha() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("ana");

        Tematica tematica = new Tematica("Historia", usuario);

        assertEquals("Historia", tematica.getNombre());
        assertEquals(usuario, tematica.getCreacionUsuario());
        assertNotNull(tematica.getFechaCreacion());
    }

    @Test
    void constructorVacioDejaCamposNulos() {
        Tematica tematica = new Tematica();

        assertNull(tematica.getNombre());
        assertNull(tematica.getCreacionUsuario());
        assertNull(tematica.getFechaCreacion());
    }
}
