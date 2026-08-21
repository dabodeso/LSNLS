package com.lsnls.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsuarioTest {

    @Test
    void rolUsuarioToStringDevuelveName() {
        assertEquals("ROLE_ADMIN", Usuario.RolUsuario.ROLE_ADMIN.toString());
        assertEquals("ROLE_CONSULTA", Usuario.RolUsuario.ROLE_CONSULTA.toString());
        assertEquals("ROLE_GUION", Usuario.RolUsuario.ROLE_GUION.toString());
        assertEquals("ROLE_VERIFICACION", Usuario.RolUsuario.ROLE_VERIFICACION.toString());
        assertEquals("ROLE_DIRECCION", Usuario.RolUsuario.ROLE_DIRECCION.toString());
    }
}
