package com.lsnls.service;

import com.lsnls.entity.Usuario;
import com.lsnls.entity.Pregunta;
import com.lsnls.entity.Cuestionario;
import com.lsnls.entity.Cuestionario.EstadoCuestionario;
import com.lsnls.entity.Concursante;
import com.lsnls.entity.EstadoConcursante;
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
                    case creada:
                        return usuario.getRol() == Usuario.RolUsuario.ROLE_GUION ||
                               usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION ||
                               usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                        
                    case verificada:
                        return usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION ||
                               usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                        
                    case rechazada:
                    case aprobada:
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

                // Solo verificacion y direccion pueden verificar
                if (nuevoEstado == Pregunta.EstadoPregunta.verificada) {
                    return usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION ||
                           usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                }

                // Solo direccion puede aprobar o rechazar
                if (nuevoEstado == Pregunta.EstadoPregunta.aprobada || nuevoEstado == Pregunta.EstadoPregunta.rechazada) {
                    return usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                }

                // Cambios básicos (borrador, creada)
                return canEditPregunta(estadoActual);
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
                // El admin siempre puede editar
                if (usuario.getRol() == Usuario.RolUsuario.ROLE_ADMIN) {
                    return true;
                }

                // Solo se pueden editar cuestionarios en estado borrador o creado
                if (estado != EstadoCuestionario.borrador && 
                    estado != EstadoCuestionario.creado) {
                    return false;
                }

                return usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION ||
                       usuario.getRol() == Usuario.RolUsuario.ROLE_GUION;
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
    public boolean canEditConcursante(EstadoConcursante estado) {
        return getCurrentUser()
            .map(usuario -> {
                switch (estado) {
                    case BORRADOR:
                        return usuario.getRol() == Usuario.RolUsuario.ROLE_GUION ||
                               usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION ||
                               usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                        
                    case GRABADO:
                    case EDITADO:
                        return usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION ||
                               usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                        
                    case PROGRAMADO:
                        return usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                        
                    default:
                        return false;
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