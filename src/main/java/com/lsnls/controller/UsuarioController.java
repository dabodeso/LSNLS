package com.lsnls.controller;

import com.lsnls.entity.Usuario;
import com.lsnls.service.UsuarioService;
import com.lsnls.service.AuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
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

    @PostMapping("/crear")
    @PreAuthorize("@authorizationService.canValidate()")
    public ResponseEntity<?> crear(@RequestBody Usuario usuario) {
        try {
            if (!authService.canValidate()) {
                return ResponseEntity.status(403).body("No tienes permisos para crear usuarios");
            }
            Usuario nuevoUsuario = usuarioService.crear(usuario);
            return ResponseEntity.ok(nuevoUsuario);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al crear usuario: " + e.getMessage());
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
            return authService.getCurrentUser()
                .map(currentUser -> {
                    // Solo ROLE_DIRECCION puede editar otros usuarios, o el propio usuario puede editarse a sí mismo
                    if (currentUser.getRol() != Usuario.RolUsuario.ROLE_DIRECCION && !currentUser.getId().equals(id)) {
                        return ResponseEntity.status(403).body("No tienes permisos para editar este usuario");
                    }

                    return usuarioService.obtenerPorId(id)
                        .map(usuarioExistente -> {
                            // Solo ROLE_DIRECCION puede cambiar roles
                            if (!usuario.getRol().equals(usuarioExistente.getRol()) && 
                                currentUser.getRol() != Usuario.RolUsuario.ROLE_DIRECCION) {
                                return ResponseEntity.status(403).body("No tienes permisos para cambiar roles");
                            }

                            try {
                                Usuario usuarioActualizado = usuarioService.actualizar(id, usuario);
                                return ResponseEntity.ok(usuarioActualizado);
                            } catch (Exception e) {
                                return ResponseEntity.badRequest().body("Error al actualizar usuario: " + e.getMessage());
                            }
                        })
                        .orElse(ResponseEntity.notFound().build());
                })
                .orElse(ResponseEntity.status(401).body("Usuario no autenticado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar usuario: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationService.canDelete()")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            if (!authService.canDelete()) {
                return ResponseEntity.status(403).body("No tienes permisos para eliminar usuarios");
            }
            usuarioService.eliminar(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar usuario: " + e.getMessage());
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