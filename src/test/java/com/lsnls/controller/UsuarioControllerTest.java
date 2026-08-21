package com.lsnls.controller;

import com.lsnls.entity.Usuario;
import com.lsnls.service.AuthorizationService;
import com.lsnls.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private AuthorizationService authService;

    @InjectMocks
    private UsuarioController usuarioController;

    private Usuario usuario(Long id, String nombre, Usuario.RolUsuario rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setRol(rol);
        u.setPassword("secret");
        return u;
    }

    @Test
    void crear_nombreVacio_devuelve400() {
        ResponseEntity<?> response = usuarioController.crear(usuario(null, "  ", Usuario.RolUsuario.ROLE_GUION));

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("nombre"));
    }

    @Test
    void crear_rolNulo_devuelve400() {
        ResponseEntity<?> response = usuarioController.crear(usuario(null, "ana", null));

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("rol"));
    }

    @Test
    void crear_sinPermiso_devuelve403() {
        when(authService.canValidate()).thenReturn(false);

        ResponseEntity<?> response = usuarioController.crear(usuario(null, "ana", Usuario.RolUsuario.ROLE_GUION));

        assertEquals(403, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("No tienes permisos"));
    }

    @Test
    void crear_nombreDuplicado_devuelve400() {
        when(authService.canValidate()).thenReturn(true);
        when(usuarioService.obtenerPorNombre("ana")).thenReturn(Optional.of(usuario(2L, "ana", Usuario.RolUsuario.ROLE_GUION)));

        ResponseEntity<?> response = usuarioController.crear(usuario(null, "ana", Usuario.RolUsuario.ROLE_GUION));

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Ya existe"));
    }

    @Test
    void crear_ok_devuelve200() {
        Usuario nuevo = usuario(5L, "ana", Usuario.RolUsuario.ROLE_VERIFICACION);
        when(authService.canValidate()).thenReturn(true);
        when(usuarioService.obtenerPorNombre("ana")).thenReturn(Optional.empty());
        when(usuarioService.crear(nuevo)).thenReturn(nuevo);

        ResponseEntity<?> response = usuarioController.crear(nuevo);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(nuevo, response.getBody());
    }

    @Test
    void crear_excepcion_devuelve400() {
        Usuario nuevo = usuario(null, "ana", Usuario.RolUsuario.ROLE_GUION);
        when(authService.canValidate()).thenReturn(true);
        when(usuarioService.obtenerPorNombre("ana")).thenReturn(Optional.empty());
        when(usuarioService.crear(nuevo)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = usuarioController.crear(nuevo);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Error interno"));
    }

    @Test
    void obtenerTodos_ok_devuelve200() {
        List<Usuario> usuarios = Collections.singletonList(usuario(1L, "admin", Usuario.RolUsuario.ROLE_ADMIN));
        when(usuarioService.obtenerTodos()).thenReturn(usuarios);

        ResponseEntity<List<Usuario>> response = usuarioController.obtenerTodos();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void obtenerPorId_ok_devuelve200() {
        Usuario u = usuario(1L, "admin", Usuario.RolUsuario.ROLE_ADMIN);
        when(usuarioService.obtenerPorId(1L)).thenReturn(Optional.of(u));

        ResponseEntity<Usuario> response = usuarioController.obtenerPorId(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(u, response.getBody());
    }

    @Test
    void obtenerPorId_noEncontrado_devuelve404() {
        when(usuarioService.obtenerPorId(9L)).thenReturn(Optional.empty());

        ResponseEntity<Usuario> response = usuarioController.obtenerPorId(9L);

        assertEquals(404, response.getStatusCodeValue());
        assertNull(response.getBody());
    }

    @Test
    void obtenerPorNombre_ok_devuelve200() {
        Usuario u = usuario(1L, "admin", Usuario.RolUsuario.ROLE_ADMIN);
        when(usuarioService.obtenerPorNombre("admin")).thenReturn(Optional.of(u));

        ResponseEntity<Usuario> response = usuarioController.obtenerPorNombre("admin");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(u, response.getBody());
    }

    @Test
    void obtenerPorNombre_noEncontrado_devuelve404() {
        when(usuarioService.obtenerPorNombre("x")).thenReturn(Optional.empty());

        ResponseEntity<Usuario> response = usuarioController.obtenerPorNombre("x");

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void actualizar_nombreVacio_devuelve400() {
        ResponseEntity<?> response = usuarioController.actualizar(1L, usuario(1L, " ", Usuario.RolUsuario.ROLE_GUION));

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("nombre"));
    }

    @Test
    void actualizar_rolNulo_devuelve400() {
        ResponseEntity<?> response = usuarioController.actualizar(1L, usuario(1L, "ana", null));

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("rol"));
    }

    @Test
    void actualizar_noAutenticado_devuelve401() {
        when(authService.getCurrentUser()).thenReturn(Optional.empty());

        ResponseEntity<?> response = usuarioController.actualizar(1L, usuario(1L, "ana", Usuario.RolUsuario.ROLE_GUION));

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void actualizar_noEncontrado_devuelve404() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L, "admin", Usuario.RolUsuario.ROLE_ADMIN)));
        when(usuarioService.obtenerPorId(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = usuarioController.actualizar(9L, usuario(9L, "ana", Usuario.RolUsuario.ROLE_GUION));

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void actualizar_sinPermisoEditarOtros_devuelve403() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(2L, "guion", Usuario.RolUsuario.ROLE_GUION)));
        when(usuarioService.obtenerPorId(1L)).thenReturn(Optional.of(usuario(1L, "otro", Usuario.RolUsuario.ROLE_CONSULTA)));

        ResponseEntity<?> response = usuarioController.actualizar(1L, usuario(1L, "otro", Usuario.RolUsuario.ROLE_CONSULTA));

        assertEquals(403, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("editar otros usuarios"));
    }

    @Test
    void actualizar_nombreDuplicado_devuelve400() {
        Usuario actual = usuario(1L, "admin", Usuario.RolUsuario.ROLE_ADMIN);
        when(authService.getCurrentUser()).thenReturn(Optional.of(actual));
        when(usuarioService.obtenerPorId(2L)).thenReturn(Optional.of(usuario(2L, "viejo", Usuario.RolUsuario.ROLE_GUION)));
        when(usuarioService.obtenerPorNombre("nuevo")).thenReturn(Optional.of(usuario(3L, "nuevo", Usuario.RolUsuario.ROLE_CONSULTA)));

        ResponseEntity<?> response = usuarioController.actualizar(2L, usuario(2L, "nuevo", Usuario.RolUsuario.ROLE_GUION));

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Ya existe otro usuario"));
    }

    @Test
    void actualizar_cambiarRolSinPermiso_devuelve403() {
        Usuario current = usuario(1L, "yo", Usuario.RolUsuario.ROLE_GUION);
        when(authService.getCurrentUser()).thenReturn(Optional.of(current));
        when(usuarioService.obtenerPorId(1L)).thenReturn(Optional.of(usuario(1L, "yo", Usuario.RolUsuario.ROLE_GUION)));

        ResponseEntity<?> response = usuarioController.actualizar(1L, usuario(1L, "yo", Usuario.RolUsuario.ROLE_ADMIN));

        assertEquals(403, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("cambiar roles"));
    }

    @Test
    void actualizar_ok_devuelve200() {
        Usuario current = usuario(1L, "admin", Usuario.RolUsuario.ROLE_ADMIN);
        Usuario existente = usuario(2L, "ana", Usuario.RolUsuario.ROLE_GUION);
        Usuario actualizado = usuario(2L, "ana", Usuario.RolUsuario.ROLE_VERIFICACION);
        when(authService.getCurrentUser()).thenReturn(Optional.of(current));
        when(usuarioService.obtenerPorId(2L)).thenReturn(Optional.of(existente));
        when(usuarioService.actualizar(2L, actualizado)).thenReturn(actualizado);

        ResponseEntity<?> response = usuarioController.actualizar(2L, actualizado);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(actualizado, response.getBody());
    }

    @Test
    void actualizar_optimisticLock_devuelve409() {
        Usuario current = usuario(1L, "admin", Usuario.RolUsuario.ROLE_DIRECCION);
        Usuario existente = usuario(2L, "ana", Usuario.RolUsuario.ROLE_GUION);
        Usuario payload = usuario(2L, "ana", Usuario.RolUsuario.ROLE_GUION);
        when(authService.getCurrentUser()).thenReturn(Optional.of(current));
        when(usuarioService.obtenerPorId(2L)).thenReturn(Optional.of(existente));
        when(usuarioService.actualizar(2L, payload))
                .thenThrow(new ObjectOptimisticLockingFailureException("conflicto", new RuntimeException()));

        ResponseEntity<?> response = usuarioController.actualizar(2L, payload);

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void actualizar_errorInternoServicio_devuelve400() {
        Usuario current = usuario(1L, "admin", Usuario.RolUsuario.ROLE_ADMIN);
        Usuario existente = usuario(2L, "ana", Usuario.RolUsuario.ROLE_GUION);
        Usuario payload = usuario(2L, "ana", Usuario.RolUsuario.ROLE_GUION);
        when(authService.getCurrentUser()).thenReturn(Optional.of(current));
        when(usuarioService.obtenerPorId(2L)).thenReturn(Optional.of(existente));
        when(usuarioService.actualizar(2L, payload)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = usuarioController.actualizar(2L, payload);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Error interno al actualizar"));
    }

    @Test
    void eliminar_sinPermiso_devuelve403() {
        when(authService.canDelete()).thenReturn(false);

        ResponseEntity<?> response = usuarioController.eliminar(2L);

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void eliminar_noEncontrado_devuelve404() {
        when(authService.canDelete()).thenReturn(true);
        when(usuarioService.obtenerPorId(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = usuarioController.eliminar(9L);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void eliminar_noAutenticado_devuelve401() {
        when(authService.canDelete()).thenReturn(true);
        when(usuarioService.obtenerPorId(2L)).thenReturn(Optional.of(usuario(2L, "ana", Usuario.RolUsuario.ROLE_GUION)));
        when(authService.getCurrentUser()).thenReturn(Optional.empty());

        ResponseEntity<?> response = usuarioController.eliminar(2L);

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void eliminar_autoEliminacion_devuelve400() {
        Usuario current = usuario(2L, "ana", Usuario.RolUsuario.ROLE_ADMIN);
        when(authService.canDelete()).thenReturn(true);
        when(usuarioService.obtenerPorId(2L)).thenReturn(Optional.of(current));
        when(authService.getCurrentUser()).thenReturn(Optional.of(current));

        ResponseEntity<?> response = usuarioController.eliminar(2L);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("propio usuario"));
    }

    @Test
    void eliminar_ok_devuelve200() {
        when(authService.canDelete()).thenReturn(true);
        when(usuarioService.obtenerPorId(2L)).thenReturn(Optional.of(usuario(2L, "ana", Usuario.RolUsuario.ROLE_GUION)));
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L, "admin", Usuario.RolUsuario.ROLE_ADMIN)));
        doNothing().when(usuarioService).eliminar(2L);

        ResponseEntity<?> response = usuarioController.eliminar(2L);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void eliminar_foreignKey_devuelve400() {
        when(authService.canDelete()).thenReturn(true);
        when(usuarioService.obtenerPorId(2L)).thenReturn(Optional.of(usuario(2L, "ana", Usuario.RolUsuario.ROLE_GUION)));
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L, "admin", Usuario.RolUsuario.ROLE_ADMIN)));
        doThrow(new RuntimeException("foreign key constraint")).when(usuarioService).eliminar(2L);

        ResponseEntity<?> response = usuarioController.eliminar(2L);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("preguntas, cuestionarios o combos"));
    }

    @Test
    void eliminar_errorInterno_devuelve400() {
        when(authService.canDelete()).thenReturn(true);
        when(usuarioService.obtenerPorId(2L)).thenReturn(Optional.of(usuario(2L, "ana", Usuario.RolUsuario.ROLE_GUION)));
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L, "admin", Usuario.RolUsuario.ROLE_ADMIN)));
        doThrow(new RuntimeException("otro")).when(usuarioService).eliminar(2L);

        ResponseEntity<?> response = usuarioController.eliminar(2L);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Error interno al eliminar"));
    }

    @Test
    void obtenerPerfilActual_ok_devuelve200() {
        Usuario u = usuario(1L, "admin", Usuario.RolUsuario.ROLE_ADMIN);
        when(authService.getCurrentUser()).thenReturn(Optional.of(u));

        ResponseEntity<Usuario> response = usuarioController.obtenerPerfilActual();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(u, response.getBody());
    }

    @Test
    void obtenerPerfilActual_noAutenticado_devuelve401() {
        when(authService.getCurrentUser()).thenReturn(Optional.empty());

        ResponseEntity<Usuario> response = usuarioController.obtenerPerfilActual();

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void resetearPassword_ok_devuelve200() {
        Usuario u = usuario(2L, "ana", Usuario.RolUsuario.ROLE_GUION);
        when(usuarioService.resetearPassword(2L)).thenReturn(u);

        ResponseEntity<?> response = usuarioController.resetearPassword(2L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(u, response.getBody());
    }

    @Test
    void resetearPassword_excepcion_devuelve400() {
        when(usuarioService.resetearPassword(2L)).thenThrow(new RuntimeException("no existe"));

        ResponseEntity<?> response = usuarioController.resetearPassword(2L);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("no existe"));
    }

    @Test
    void cambiarPassword_camposFaltantes_devuelve400() {
        Map<String, String> body = new HashMap<>();
        body.put("actual", "old");

        ResponseEntity<?> response = usuarioController.cambiarPassword(1L, body);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Faltan campos obligatorios", response.getBody());
    }

    @Test
    void cambiarPassword_formatoInvalido_devuelve400() {
        Map<String, String> body = new HashMap<>();
        body.put("actual", "old");
        body.put("nueva", "corta");

        ResponseEntity<?> response = usuarioController.cambiarPassword(1L, body);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("requisitos de seguridad"));
    }

    @Test
    void cambiarPassword_noAutenticado_devuelve401() {
        when(authService.getCurrentUser()).thenReturn(Optional.empty());
        Map<String, String> body = new HashMap<>();
        body.put("actual", "OldPass1!");
        body.put("nueva", "Password!");

        ResponseEntity<?> response = usuarioController.cambiarPassword(1L, body);

        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void cambiarPassword_otroUsuario_devuelve403() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(2L, "otro", Usuario.RolUsuario.ROLE_GUION)));
        Map<String, String> body = new HashMap<>();
        body.put("actual", "OldPass1!");
        body.put("nueva", "Password!");

        ResponseEntity<?> response = usuarioController.cambiarPassword(1L, body);

        assertEquals(403, response.getStatusCodeValue());
    }

    @Test
    void cambiarPassword_ok_devuelve200() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L, "yo", Usuario.RolUsuario.ROLE_GUION)));
        when(usuarioService.cambiarPassword(1L, "OldPass1!", "Password!")).thenReturn(true);
        Map<String, String> body = new HashMap<>();
        body.put("actual", "OldPass1!");
        body.put("nueva", "Password!");

        ResponseEntity<?> response = usuarioController.cambiarPassword(1L, body);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Contraseña cambiada correctamente", response.getBody());
    }

    @Test
    void cambiarPassword_actualIncorrecta_devuelve400() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario(1L, "yo", Usuario.RolUsuario.ROLE_GUION)));
        when(usuarioService.cambiarPassword(1L, "OldPass1!", "Password!")).thenReturn(false);
        Map<String, String> body = new HashMap<>();
        body.put("actual", "OldPass1!");
        body.put("nueva", "Password!");

        ResponseEntity<?> response = usuarioController.cambiarPassword(1L, body);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("contraseña actual no es correcta"));
    }

    @Test
    void cambiarPassword_excepcion_devuelve400() {
        when(authService.getCurrentUser()).thenThrow(new RuntimeException("boom"));
        Map<String, String> body = new HashMap<>();
        body.put("actual", "OldPass1!");
        body.put("nueva", "Password!");

        ResponseEntity<?> response = usuarioController.cambiarPassword(1L, body);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("boom"));
    }
}
