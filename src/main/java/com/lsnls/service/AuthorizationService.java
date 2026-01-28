package com.lsnls.service;

import com.lsnls.entity.Usuario;
import com.lsnls.entity.Pregunta;
import com.lsnls.entity.Cuestionario.EstadoCuestionario;
import com.lsnls.entity.Combo;
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
                    case revisar:
                    case corregir:
                        // Niveles 2, 3 y 4 (GUION, VERIFICACION, DIRECCION)
                        return usuario.getRol() == Usuario.RolUsuario.ROLE_GUION ||
                               usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION ||
                               usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                        
                    case verificada:
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
                // El admin siempre puede cambiar estados, pero debe seguir el flujo correcto
                boolean isAdmin = usuario.getRol() == Usuario.RolUsuario.ROLE_ADMIN;
                boolean isGuion = usuario.getRol() == Usuario.RolUsuario.ROLE_GUION;
                boolean isVerificacion = usuario.getRol() == Usuario.RolUsuario.ROLE_VERIFICACION;
                boolean isDireccion = usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION;
                
                // Verificar la transición válida según el autómata
                boolean transicionValida = false;

                // Dirección puede hacer cualquier transición
                if (isDireccion) {
                    return true;
                }
                
                switch (estadoActual) {
                    case borrador:
                        // Borrador -> Para Verificar (por Guion)
                        transicionValida = nuevoEstado == Pregunta.EstadoPregunta.para_verificar && isGuion;
                        break;
                        
                    case para_verificar:
                        // Para Verificar -> Verificada o Revisar (por Verificación/Guion)
                        transicionValida = (nuevoEstado == Pregunta.EstadoPregunta.verificada && 
                                           (isVerificacion || isDireccion)) || 
                                          (nuevoEstado == Pregunta.EstadoPregunta.revisar && 
                                           (isVerificacion || isGuion));
                        break;
                        
                    case revisar:
                    // Revisar -> Para Verificar, Para Aprobar o Rechazada (por Guion)
                    transicionValida = (nuevoEstado == Pregunta.EstadoPregunta.para_verificar ||
                                       nuevoEstado == Pregunta.EstadoPregunta.para_aprobar ||
                                       nuevoEstado == Pregunta.EstadoPregunta.rechazada) &&
                                      isGuion;
                        break;
                        
                    case verificada:
                        // Verificada -> Corregir, Rechazada o Aprobada (por Dirección)
                        transicionValida = (nuevoEstado == Pregunta.EstadoPregunta.corregir || 
                                           nuevoEstado == Pregunta.EstadoPregunta.rechazada || 
                                           nuevoEstado == Pregunta.EstadoPregunta.aprobada) && 
                                          isDireccion;
                        break;
                        
                    case corregir:
                        // Corregir -> Para Aprobar o Para Verificar (por Guion)
                        transicionValida = (nuevoEstado == Pregunta.EstadoPregunta.para_aprobar || 
                                           nuevoEstado == Pregunta.EstadoPregunta.para_verificar) && 
                                          isGuion;
                        break;
                        
                    case para_aprobar:
                    // Para Aprobar -> Aprobada, Corregir, Rechazada o Para Verificar (por Dirección)
                    transicionValida = (nuevoEstado == Pregunta.EstadoPregunta.aprobada ||
                                       nuevoEstado == Pregunta.EstadoPregunta.corregir ||
                                       nuevoEstado == Pregunta.EstadoPregunta.rechazada ||
                                       nuevoEstado == Pregunta.EstadoPregunta.para_verificar) &&
                                      isDireccion;
                        break;
                        
                    case aprobada:
                        // Aprobada -> Usada (automático al asignar a cuestionario)
                        transicionValida = nuevoEstado == Pregunta.EstadoPregunta.usada;
                        break;
                        
                    case usada:
                        // Usada -> Aprobada (automático al quitar del cuestionario)
                        transicionValida = nuevoEstado == Pregunta.EstadoPregunta.aprobada;
                        break;
                        
                    default:
                        transicionValida = false;
                }
                
                // Si es admin, puede hacer cualquier transición válida
                return isAdmin || transicionValida;
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
                usuario.getRol() == Usuario.RolUsuario.ROLE_DIRECCION)
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
