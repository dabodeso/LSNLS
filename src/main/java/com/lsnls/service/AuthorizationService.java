package com.lsnls.service;

import com.lsnls.entity.Usuario;
import com.lsnls.entity.Pregunta;
import com.lsnls.entity.Cuestionario;
import com.lsnls.entity.Cuestionario.EstadoCuestionario;
import com.lsnls.entity.Combo;
import com.lsnls.entity.Concursante;
import com.lsnls.entity.Programa;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final AuthService authService;

    /**
     * Obtiene el usuario autenticado actual
     */
    public Optional<Usuario> getCurrentUser() {
        return authService.getCurrentUser();
    }

    /**
     * Verifica si el usuario actual puede leer entidades
     */
    public boolean canRead() {
        return getCurrentUser().isPresent();
    }

    /**
     * Verifica si el usuario actual puede crear preguntas
     */
    public boolean canCreatePregunta() {
        return getCurrentUser()
            .map(usuario -> 
                usuario.getRol() == Usuario.RolUsuario.ROLE_ADMIN ||
                usuario.getRol() == Usuario.RolUsuario.ROLE_GUION ||
                usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION ||
                usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION)
            .orElse(false);
    }

    /**
     * Verifica si el usuario actual puede editar una pregunta según su estado
     */
    public boolean canEditPregunta(Pregunta.EstadoPregunta estado) {
        return getCurrentUser()
            .map(usuario -> {
                // El admin siempre puede editar
                if (usuario.getRol() == Usuario.RolUsuario.ROLE_ADMIN) {
                    return true;
                }

                switch (estado) {
                    case borrador:
                    case para_verificar:
                        // Niveles 2, 3 y 4 (GUION, VERIFICACION, DIRECCION)
                        return usuario.getRol() == Usuario.RolUsuario.ROLE_GUION ||
                               usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION ||
                               usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                        
                    case verificada:
                    case revisar:
                    case corregir:
                        // Niveles 3 y 4 (VERIFICACION, DIRECCION)
                        return usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION ||
                               usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                        
                    case rechazada:
                    case aprobada:
                        // Solo nivel 4 (DIRECCION)
                        return usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                        
                    default:
                        return false;
                }
            })
            .orElse(false);
    }

    /**
     * Verifica si el usuario actual puede cambiar el estado de una pregunta
     */
    public boolean canChangeEstadoPregunta(Pregunta.EstadoPregunta estadoActual, Pregunta.EstadoPregunta nuevoEstado) {
        return getCurrentUser()
            .map(usuario -> {
                // El admin siempre puede cambiar estados
                if (usuario.getRol() == Usuario.RolUsuario.ROLE_ADMIN) {
                    return true;
                }

                switch (nuevoEstado) {
                    case borrador:
                    case para_verificar:
                        // Niveles 2, 3 y 4 pueden establecer estos estados
                        return usuario.getRol() == Usuario.RolUsuario.ROLE_GUION ||
                               usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION ||
                               usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                        
                    case verificada:
                    case revisar:
                        // Solo niveles 3 y 4 pueden verificar o marcar para revisar
                        return usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION ||
                               usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                        
                    case corregir:
                    case rechazada:
                    case aprobada:
                        // Solo nivel 4 puede mandar a corregir, rechazar o aprobar
                        return usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                        
                    default:
                        return false;
                }
            })
            .orElse(false);
    }

    /**
     * Verifica si el usuario actual puede crear cuestionarios
     */
    public boolean canCreateCuestionario() {
        return getCurrentUser()
            .map(usuario -> 
                usuario.getRol() == Usuario.RolUsuario.ROLE_ADMIN ||
                usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION ||
                usuario.getRol() == Usuario.RolUsuario.ROLE_GUION)
            .orElse(false);
    }

    /**
     * Verifica si el usuario actual puede editar un cuestionario según su estado
     */
    public boolean canEditCuestionario(EstadoCuestionario estado) {
        return getCurrentUser()
            .map(usuario -> {
                // El admin siempre puede editar cualquier estado
                if (usuario.getRol() == Usuario.RolUsuario.ROLE_ADMIN) {
                    return true;
                }

                // Dirección puede editar en todos los estados
                if (usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION) {
                    return true;
                }

                // Verificación puede editar en todos los estados excepto grabado
                if (usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION) {
                    return estado != EstadoCuestionario.grabado;
                }

                // Guion solo puede editar en estados borrador y creado
                if (usuario.getRol() == Usuario.RolUsuario.ROLE_GUION) {
                    return estado == EstadoCuestionario.borrador || 
                           estado == EstadoCuestionario.creado;
                }

                return false;
            })
            .orElse(false);
    }

    /**
     * Verifica si el usuario actual puede editar un combo según su estado
     */
    public boolean canEditCombo(Combo.EstadoCombo estado) {
        return getCurrentUser()
            .map(usuario -> {
                // El admin siempre puede editar cualquier estado
                if (usuario.getRol() == Usuario.RolUsuario.ROLE_ADMIN) {
                    return true;
                }

                // Dirección puede editar en todos los estados
                if (usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION) {
                    return true;
                }

                // Verificación puede editar en todos los estados excepto grabado
                if (usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION) {
                    return estado != Combo.EstadoCombo.grabado;
                }

                // Guion solo puede editar en estados borrador y creado
                if (usuario.getRol() == Usuario.RolUsuario.ROLE_GUION) {
                    return estado == Combo.EstadoCombo.borrador || 
                           estado == Combo.EstadoCombo.grabado;
                }

                return false;
            })
            .orElse(false);
    }

    /**
     * Verifica si el usuario actual puede crear concursantes
     */
    public boolean canCreateConcursante() {
        return getCurrentUser()
            .map(usuario -> 
                usuario.getRol() == Usuario.RolUsuario.ROLE_GUION ||
                usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION ||
                usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION)
            .orElse(false);
    }

    /**
     * Verifica si el usuario actual puede editar un concursante según su estado
     */
    public boolean canEditConcursante(String estado) {
        return getCurrentUser()
            .map(usuario -> {
                if (estado == null) {
                    return usuario.getRol() == Usuario.RolUsuario.ROLE_GUION ||
                           usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION ||
                           usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                }
                
                switch (estado.toUpperCase()) {
                    case "BORRADOR":
                        return usuario.getRol() == Usuario.RolUsuario.ROLE_GUION ||
                               usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION ||
                               usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                        
                    case "GRABADO":
                    case "EDITADO":
                        return usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION ||
                               usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                        
                    case "PROGRAMADO":
                        return usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                        
                    default:
                        // Para estados personalizados, permitir edición según roles básicos
                        return usuario.getRol() == Usuario.RolUsuario.ROLE_GUION ||
                               usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION ||
                               usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                }
            })
            .orElse(false);
    }

    /**
     * Verifica si el usuario actual puede crear programas
     */
    public boolean canCreatePrograma() {
        return getCurrentUser()
            .map(usuario -> 
                usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION ||
                usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION)
            .orElse(false);
    }

    /**
     * Verifica si el usuario actual puede editar un programa según su estado
     */
    public boolean canEditPrograma(Programa.EstadoPrograma estado) {
        return getCurrentUser()
            .map(usuario -> {
                switch (estado) {
                    case borrador:
                        return usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION ||
                               usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                        
                    case programado:
                        return usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                        
                    default:
                        return false;
                }
            })
            .orElse(false);
    }

    /**
     * Verifica si el usuario actual puede eliminar entidades
     */
    public boolean canDelete() {
        return getCurrentUser()
            .map(usuario -> 
                usuario.getRol() == Usuario.RolUsuario.ROLE_ADMIN ||
                usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION)
            .orElse(false);
    }

    /**
     * Verifica si el usuario actual puede crear entidades generales
     */
    public boolean canCreate() {
        return getCurrentUser()
            .map(usuario -> 
                usuario.getRol() == Usuario.RolUsuario.ROLE_ADMIN ||
                usuario.getRol() == Usuario.RolUsuario.ROLE_GUION ||
                usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION ||
                usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION)
            .orElse(false);
    }

    /**
     * Verifica si el usuario actual puede editar entidades generales
     */
    public boolean canEdit() {
        return getCurrentUser()
            .map(usuario -> 
                usuario.getRol() == Usuario.RolUsuario.ROLE_ADMIN ||
                usuario.getRol() == Usuario.RolUsuario.ROLE_GUION ||
                usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION ||
                usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION)
            .orElse(false);
    }

    /**
     * Verifica si el usuario actual puede validar (aprobar/rechazar)
     */
    public boolean canValidate() {
        return getCurrentUser()
            .map(usuario -> 
                usuario.getRol() == Usuario.RolUsuario.ROLE_ADMIN ||
                usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION ||
                usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION)
            .orElse(false);
    }
} 