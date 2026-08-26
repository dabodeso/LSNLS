package com.lsnls.controller;

import com.lsnls.dto.AuthResponse;
import com.lsnls.dto.ErrorResponse;
import com.lsnls.dto.LoginRequest;
import com.lsnls.entity.Usuario;
import com.lsnls.service.AuthService;
import com.lsnls.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private AuthController authController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private LoginRequest loginRequest(String nombre, String password) {
        LoginRequest request = new LoginRequest();
        request.setNombre(nombre);
        request.setPassword(password);
        return request;
    }

    private Usuario usuario(Long id, String nombre, Usuario.RolUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre(nombre);
        usuario.setRol(rol);
        usuario.setPassword("secret");
        return usuario;
    }

    @Test
    void login_nombreVacio_devuelve400() {
        ResponseEntity<?> response = authController.login(loginRequest("  ", "pass"));

        assertEquals(400, response.getStatusCodeValue());
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertNotNull(body);
        assertEquals("Error de validación", body.getError());
        assertTrue(body.getMensaje().contains("nombre de usuario"));
    }

    @Test
    void login_nombreNulo_devuelve400() {
        ResponseEntity<?> response = authController.login(loginRequest(null, "pass"));

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(((ErrorResponse) response.getBody()).getMensaje().contains("nombre de usuario"));
    }

    @Test
    void login_passwordVacia_devuelve400() {
        ResponseEntity<?> response = authController.login(loginRequest("admin", "   "));

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(((ErrorResponse) response.getBody()).getMensaje().contains("contraseña"));
    }

    @Test
    void login_passwordNula_devuelve400() {
        ResponseEntity<?> response = authController.login(loginRequest("admin", null));

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(((ErrorResponse) response.getBody()).getMensaje().contains("contraseña"));
    }

    @Test
    void login_ok_devuelve200() {
        Usuario usuario = usuario(1L, "admin", Usuario.RolUsuario.ROLE_ADMIN);
        AuthResponse authResponse = new AuthResponse("token-123", usuario);
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        ResponseEntity<?> response = authController.login(loginRequest("admin", "pass"));

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(authResponse, response.getBody());
    }

    @Test
    void login_badCredentials_devuelve401() {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Bad credentials"));

        ResponseEntity<?> response = authController.login(loginRequest("admin", "wrong"));

        assertEquals(401, response.getStatusCodeValue());
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertEquals("Error de autenticación", body.getError());
        assertTrue(body.getMensaje().contains("Usuario o contraseña incorrectos"));
    }

    @Test
    void login_otraExcepcion_devuelve401() {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("servicio caído"));

        ResponseEntity<?> response = authController.login(loginRequest("admin", "pass"));

        assertEquals(401, response.getStatusCodeValue());
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertTrue(body.getMensaje().contains("servicio caído"));
    }

    @Test
    void register_nombreVacio_devuelve400() {
        Usuario usuario = usuario(null, "  ", Usuario.RolUsuario.ROLE_GUION);
        usuario.setPassword("Pass123!");

        ResponseEntity<?> response = authController.register(usuario);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(((ErrorResponse) response.getBody()).getMensaje().contains("nombre de usuario"));
    }

    @Test
    void register_passwordVacia_devuelve400() {
        Usuario usuario = usuario(null, "nuevo", Usuario.RolUsuario.ROLE_GUION);
        usuario.setPassword("  ");

        ResponseEntity<?> response = authController.register(usuario);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(((ErrorResponse) response.getBody()).getMensaje().contains("contraseña"));
    }

    @Test
    void register_rolNulo_devuelve400() {
        Usuario usuario = usuario(null, "nuevo", null);
        usuario.setPassword("Pass123!");

        ResponseEntity<?> response = authController.register(usuario);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(((ErrorResponse) response.getBody()).getMensaje().contains("rol"));
    }

    @Test
    void register_ok_devuelve200() {
        Usuario usuario = usuario(null, "nuevo", Usuario.RolUsuario.ROLE_CONSULTA);
        usuario.setPassword("Pass123!");
        Usuario creado = usuario(10L, "nuevo", Usuario.RolUsuario.ROLE_CONSULTA);
        when(authService.register(usuario)).thenReturn(new AuthResponse("tok", creado));

        ResponseEntity<?> response = authController.register(usuario);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof AuthResponse);
    }

    @Test
    void register_excepcion_devuelve400() {
        Usuario usuario = usuario(null, "nuevo", Usuario.RolUsuario.ROLE_GUION);
        usuario.setPassword("Pass123!");
        when(authService.register(usuario)).thenThrow(new RuntimeException("ya existe"));

        ResponseEntity<?> response = authController.register(usuario);

        assertEquals(400, response.getStatusCodeValue());
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertEquals("Error de registro", body.getError());
        assertTrue(body.getMensaje().contains("ya existe"));
    }

    @Test
    void getCurrentUser_ok_devuelve200() {
        Usuario usuario = usuario(1L, "admin", Usuario.RolUsuario.ROLE_ADMIN);
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario));

        ResponseEntity<?> response = authController.getCurrentUser();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(usuario, response.getBody());
    }

    @Test
    void getCurrentUser_noAutenticado_devuelve401() {
        when(authService.getCurrentUser()).thenReturn(Optional.empty());

        ResponseEntity<?> response = authController.getCurrentUser();

        assertEquals(401, response.getStatusCodeValue());
        assertTrue(((ErrorResponse) response.getBody()).getMensaje().contains("no autenticado"));
    }

    @Test
    void getCurrentUser_excepcion_devuelve401() {
        when(authService.getCurrentUser()).thenThrow(new RuntimeException("token inválido"));

        ResponseEntity<?> response = authController.getCurrentUser();

        assertEquals(401, response.getStatusCodeValue());
        assertTrue(((ErrorResponse) response.getBody()).getMensaje().contains("sesión"));
    }

    @Test
    void logout_ok_devuelve200() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "n/a", Collections.emptyList()));

        ResponseEntity<?> response = authController.logout();

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Sesión cerrada exitosamente", body.get("message"));
        assertEquals(null, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void getProfile_sinAuthentication_devuelve401() {
        ResponseEntity<?> response = authController.getProfile();

        assertEquals(401, response.getStatusCodeValue());
        assertTrue(((ErrorResponse) response.getBody()).getMensaje().contains("no autenticado"));
    }

    @Test
    void getProfile_noAutenticado_devuelve401() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "pass"));

        ResponseEntity<?> response = authController.getProfile();

        assertEquals(401, response.getStatusCodeValue());
        assertTrue(((ErrorResponse) response.getBody()).getMensaje().contains("no autenticado"));
    }

    @Test
    void getProfile_ok_devuelve200() {
        Usuario usuario = usuario(1L, "admin", Usuario.RolUsuario.ROLE_DIRECCION);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "n/a", Collections.emptyList()));
        when(usuarioService.obtenerPorNombre("admin")).thenReturn(Optional.of(usuario));

        ResponseEntity<?> response = authController.getProfile();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(usuario, response.getBody());
    }

    @Test
    void getProfile_usuarioNoEncontrado_devuelve401() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("fantasma", "n/a", Collections.emptyList()));
        when(usuarioService.obtenerPorNombre("fantasma")).thenReturn(Optional.empty());

        ResponseEntity<?> response = authController.getProfile();

        assertEquals(401, response.getStatusCodeValue());
        assertTrue(((ErrorResponse) response.getBody()).getMensaje().contains("no encontrado"));
    }

    @Test
    void getProfile_excepcion_devuelve401() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "n/a", Collections.emptyList()));
        when(usuarioService.obtenerPorNombre("admin")).thenThrow(new RuntimeException("bd caída"));

        ResponseEntity<?> response = authController.getProfile();

        assertEquals(401, response.getStatusCodeValue());
        assertTrue(((ErrorResponse) response.getBody()).getMensaje().contains("sesión"));
    }
}
