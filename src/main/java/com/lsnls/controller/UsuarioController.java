package com.lsnls.controller;

import com.lsnls.entity.Usuario;
import com.lsnls.service.UsuarioService;
import com.lsnls.service.AuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AuthorizationService authService;

    @PostMapping
    @PreAuthorize("@authorizationService.canValidate()")
    public ResponseEntity<?> crear(@RequestBody Usuario usuario) {
        try {
            // Validaciones específicas de campos requeridos
            if (usuario.getNombre() == null || usuario.getNombre().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El campo 'nombre' es obligatorio");
            }
            if (usuario.getRol() == null) {
                return ResponseEntity.badRequest().body("El campo 'rol' es obligatorio");
            }

            // Validación de permisos específica
            if (!authService.canValidate()) {
                return ResponseEntity.status(403).body("Solo usuarios con rol ADMIN, VERIFICACION o DIRECCION pueden crear usuarios");
            }

            // Validar que el nombre no esté duplicado
            if (usuarioService.obtenerPorNombre(usuario.getNombre()).isPresent()) {
                return ResponseEntity.badRequest().body("Ya existe un usuario con el nombre '" + usuario.getNombre() + "'");
            }

            Usuario nuevoUsuario = usuarioService.crear(usuario);
            return ResponseEntity.ok(nuevoUsuario);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error interno al crear usuario: " + e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<List<Usuario>> obtenerTodos() {
        return ResponseEntity.ok(usuarioService.obtenerTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<Usuario> obtenerPorId(@PathVariable Long id) {
        return usuarioService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nombre/{nombre}")
    @PreAuthorize("@authorizationService.canRead()")
    public ResponseEntity<Usuario> obtenerPorNombre(@PathVariable String nombre) {
        return usuarioService.obtenerPorNombre(nombre)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Usuario usuario) {
        try {
            // Validaciones específicas de campos requeridos
            if (usuario.getNombre() == null || usuario.getNombre().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El campo 'nombre' es obligatorio");
            }
            if (usuario.getRol() == null) {
                return ResponseEntity.badRequest().body("El campo 'rol' es obligatorio");
            }

            // Verificar autenticación
            Optional<Usuario> currentUserOpt = authService.getCurrentUser();
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(401).body("Usuario no autenticado");
            }

            Usuario currentUser = currentUserOpt.get();

            // Verificar que el usuario a editar existe
            Optional<Usuario> usuarioExistenteOpt = usuarioService.obtenerPorId(id);
            if (usuarioExistenteOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Usuario con ID " + id + " no encontrado");
            }

            Usuario usuarioExistente = usuarioExistenteOpt.get();

            // Verificar permisos de edición
            if (currentUser.getRol() != Usuario.RolUsuario.ROLE_DIRECCION && !currentUser.getId().equals(id)) {
                return ResponseEntity.status(403).body("Solo el usuario DIRECCION puede editar otros usuarios, o puedes editar tu propio perfil");
            }

            // Validar duplicación de nombre (excepto si es el mismo usuario)
            if (!usuario.getNombre().equals(usuarioExistente.getNombre())) {
                if (usuarioService.obtenerPorNombre(usuario.getNombre()).isPresent()) {
                    return ResponseEntity.badRequest().body("Ya existe otro usuario con el nombre '" + usuario.getNombre() + "'");
                }
            }

            // Verificar permisos para cambiar roles
            if (!usuario.getRol().equals(usuarioExistente.getRol()) && 
                currentUser.getRol() != Usuario.RolUsuario.ROLE_DIRECCION) {
                return ResponseEntity.status(403).body("Solo el usuario DIRECCION puede cambiar roles de otros usuarios");
            }

            try {
                Usuario usuarioActualizado = usuarioService.actualizar(id, usuario);
                return ResponseEntity.ok(usuarioActualizado);
            } catch (ObjectOptimisticLockingFailureException e) {
                return ResponseEntity.status(409).body("El usuario ha sido modificado por otro usuario. Por favor, recarga e intenta nuevamente.");
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Error interno al actualizar usuario: " + e.getMessage());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar usuario: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationService.canDelete()")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            // Verificar permisos específicos
            if (!authService.canDelete()) {
                return ResponseEntity.status(403).body("Solo usuarios con rol ADMIN o DIRECCION pueden eliminar usuarios");
            }

            // Verificar que el usuario existe
            Optional<Usuario> usuarioOpt = usuarioService.obtenerPorId(id);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Usuario con ID " + id + " no encontrado");
            }

            Usuario usuario = usuarioOpt.get();

            // Impedir auto-eliminación
            Optional<Usuario> currentUserOpt = authService.getCurrentUser();
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(401).body("Usuario no autenticado");
            }

            Usuario currentUser = currentUserOpt.get();
            if (currentUser.getId().equals(id)) {
                return ResponseEntity.badRequest().body("No puedes eliminar tu propio usuario");
            }

            try {
                usuarioService.eliminar(id);
                return ResponseEntity.ok().build();
            } catch (Exception e) {
                String mensaje = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (mensaje.contains("foreign key") || mensaje.contains("constraint") || mensaje.contains("referenced")) {
                    return ResponseEntity.badRequest().body("No se puede eliminar el usuario '" + usuario.getNombre() + 
                        "' porque tiene preguntas, cuestionarios o combos asociados. Reasigna estos elementos a otro usuario primero.");
                }
                return ResponseEntity.badRequest().body("Error interno al eliminar usuario: " + e.getMessage());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error interno al eliminar usuario: " + e.getMessage());
        }
    }

    @GetMapping("/perfil")
    public ResponseEntity<Usuario> obtenerPerfilActual() {
        return authService.getCurrentUser()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.status(401).build());
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> resetearPassword(@PathVariable Long id) {
        try {
            Usuario usuario = usuarioService.resetearPassword(id);
            return ResponseEntity.ok(usuario);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al resetear contraseña: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/cambiar-password")
    public ResponseEntity<?> cambiarPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String actual = body.get("actual");
            String nueva = body.get("nueva");
            if (actual == null || nueva == null) {
                return ResponseEntity.badRequest().body("Faltan campos obligatorios");
            }
            // Validación de la nueva contraseña
            if (!nueva.matches("^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]\\{\\};':\"\\\\|,.<>/?]).{8,}$")) {
                return ResponseEntity.badRequest().body("La nueva contraseña no cumple los requisitos de seguridad");
            }
            return authService.getCurrentUser().map(currentUser -> {
                if (!currentUser.getId().equals(id)) {
                    return ResponseEntity.status(403).body("No puedes cambiar la contraseña de otro usuario");
                }
                boolean ok = usuarioService.cambiarPassword(id, actual, nueva);
                if (ok) {
                    return ResponseEntity.ok("Contraseña cambiada correctamente");
                } else {
                    return ResponseEntity.status(400).body("La contraseña actual no es correcta");
                }
            }).orElse(ResponseEntity.status(401).body("Usuario no autenticado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al cambiar contraseña: " + e.getMessage());
        }
    }
} 