package com.lsnls.controller;

import com.lsnls.config.MensajesUsuario;
import com.lsnls.dto.AuthResponse;
import com.lsnls.dto.ErrorResponse;
import com.lsnls.dto.LoginRequest;
import com.lsnls.entity.Usuario;
import com.lsnls.service.AuthService;
import com.lsnls.service.UsuarioService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        log.info("🔐 Intento de login para usuario: {}", request.getNombre());
        try {
            // Validación básica
            if (request.getNombre() == null || request.getNombre().trim().isEmpty()) {
                log.warn("❌ Login fallido: nombre de usuario vacío");
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Error de validación", "El nombre de usuario es obligatorio. Por favor, introduce tu nombre de usuario."));
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                log.warn("❌ Login fallido: contraseña vacía");
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Error de validación", "La contraseña es obligatoria. Por favor, introduce tu contraseña."));
            }

            log.debug("👉 Intentando autenticar usuario con AuthService");
            AuthResponse response = authService.login(request);
            log.info("✅ Login exitoso para usuario: {}", request.getNombre());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error en login: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            String mensaje = e.getMessage();
            if (mensaje != null && mensaje.toLowerCase().contains("bad credentials")) {
                return ResponseEntity.status(401)
                    .body(new ErrorResponse("Error de autenticación", "Usuario o contraseña incorrectos. Comprueba tus datos e inténtalo de nuevo."));
            }
            return ResponseEntity.status(401)
                .body(new ErrorResponse("Error de autenticación", MensajesUsuario.sanitizar(mensaje, "No se ha podido iniciar sesión. Inténtalo de nuevo.")));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody Usuario usuario) {
        try {
            // Validación básica
            if (usuario.getNombre() == null || usuario.getNombre().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Error de validación", "El nombre de usuario es requerido"));
            }
            if (usuario.getPassword() == null || usuario.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Error de validación", "La contraseña es requerida"));
            }
            if (usuario.getRol() == null) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Error de validación", "El rol es requerido"));
            }

            AuthResponse response = authService.register(usuario);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Error de registro", MensajesUsuario.sanitizar(e.getMessage(), MensajesUsuario.VALIDACION)));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        log.debug("👤 Solicitando usuario actual");
        try {
            Optional<Usuario> usuario = authService.getCurrentUser();
            if (usuario.isPresent()) {
                log.debug("✅ Usuario actual encontrado: {}", usuario.get().getNombre());
                return ResponseEntity.ok(usuario.get());
            } else {
                log.warn("❌ Usuario actual no encontrado");
                return ResponseEntity.status(401)
                    .body(new ErrorResponse("Error de autenticación", "Usuario no autenticado"));
            }
        } catch (Exception e) {
            log.error("Error al obtener usuario actual: {}", e.getMessage());
            return ResponseEntity.status(401)
                .body(new ErrorResponse("Error de autenticación", MensajesUsuario.SESION));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        try {
            SecurityContextHolder.clearContext();
            return ResponseEntity.ok(Map.of("message", "Sesión cerrada exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Error al cerrar sesión", MensajesUsuario.GENERICO));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                    .body(new ErrorResponse("Error de autenticación", "Usuario no autenticado"));
            }

            Optional<Usuario> usuario = usuarioService.obtenerPorNombre(authentication.getName());
            if (usuario.isPresent()) {
                return ResponseEntity.ok(usuario.get());
            } else {
                return ResponseEntity.status(401)
                    .body(new ErrorResponse("Error de autenticación", "Usuario no encontrado"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(401)
                .body(new ErrorResponse("Error de autenticación", MensajesUsuario.SESION));
        }
    }
} 