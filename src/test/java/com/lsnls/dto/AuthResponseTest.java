package com.lsnls.dto;

import com.lsnls.entity.Usuario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthResponseTest {

    @Test
    void constructor_copiaDatosDelUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNombre("admin");
        usuario.setRol(Usuario.RolUsuario.ROLE_ADMIN);

        AuthResponse response = new AuthResponse("jwt-token", usuario);

        assertEquals("jwt-token", response.getToken());
        assertEquals("Bearer", response.getTipo());
        assertEquals(7L, response.getId());
        assertEquals("admin", response.getNombre());
        assertEquals(Usuario.RolUsuario.ROLE_ADMIN, response.getRol());
    }

    @Test
    void setters() {
        AuthResponse response = new AuthResponse("t", new Usuario());
        response.setTipo("Custom");
        response.setToken("otro");
        assertEquals("Custom", response.getTipo());
        assertEquals("otro", response.getToken());
    }
}
