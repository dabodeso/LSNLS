package com.lsnls.service;

import com.lsnls.entity.Combo;
import com.lsnls.entity.Cuestionario.EstadoCuestionario;
import com.lsnls.entity.Pregunta;
import com.lsnls.entity.Programa;
import com.lsnls.entity.Usuario;
import com.lsnls.entity.Usuario.RolUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthorizationService authorizationService;

    private Usuario conRol(RolUsuario rol) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNombre(rol.name());
        u.setRol(rol);
        return u;
    }

    private void autenticar(RolUsuario rol) {
        when(authService.getCurrentUser()).thenReturn(Optional.of(conRol(rol)));
    }

    private void sinUsuario() {
        when(authService.getCurrentUser()).thenReturn(Optional.empty());
    }

    @Test
    void getCurrentUser_delega() {
        autenticar(RolUsuario.ROLE_ADMIN);
        assertTrue(authorizationService.getCurrentUser().isPresent());
    }

    @Test
    void isAdmin() {
        autenticar(RolUsuario.ROLE_ADMIN);
        assertTrue(authorizationService.isAdmin());
        autenticar(RolUsuario.ROLE_GUION);
        assertFalse(authorizationService.isAdmin());
        sinUsuario();
        assertFalse(authorizationService.isAdmin());
    }

    @Test
    void canRead() {
        autenticar(RolUsuario.ROLE_CONSULTA);
        assertTrue(authorizationService.canRead());
        sinUsuario();
        assertFalse(authorizationService.canRead());
    }

    @Test
    void canCreatePregunta() {
        autenticar(RolUsuario.ROLE_ADMIN);
        assertTrue(authorizationService.canCreatePregunta());
        autenticar(RolUsuario.ROLE_GUION);
        assertTrue(authorizationService.canCreatePregunta());
        autenticar(RolUsuario.ROLE_DIRECCION);
        assertTrue(authorizationService.canCreatePregunta());
        autenticar(RolUsuario.ROLE_VERIFICACION);
        assertFalse(authorizationService.canCreatePregunta());
        autenticar(RolUsuario.ROLE_CONSULTA);
        assertFalse(authorizationService.canCreatePregunta());
        sinUsuario();
        assertFalse(authorizationService.canCreatePregunta());
    }

    @Test
    void canEditPregunta_porRolYEstado() {
        autenticar(RolUsuario.ROLE_ADMIN);
        for (Pregunta.EstadoPregunta estado : Pregunta.EstadoPregunta.values()) {
            assertTrue(authorizationService.canEditPregunta(estado));
        }

        autenticar(RolUsuario.ROLE_GUION);
        assertTrue(authorizationService.canEditPregunta(Pregunta.EstadoPregunta.borrador));
        assertTrue(authorizationService.canEditPregunta(Pregunta.EstadoPregunta.para_verificar));
        assertTrue(authorizationService.canEditPregunta(Pregunta.EstadoPregunta.revisar));
        assertTrue(authorizationService.canEditPregunta(Pregunta.EstadoPregunta.corregir));
        assertTrue(authorizationService.canEditPregunta(Pregunta.EstadoPregunta.verificada));
        assertFalse(authorizationService.canEditPregunta(Pregunta.EstadoPregunta.para_aprobar));
        assertFalse(authorizationService.canEditPregunta(Pregunta.EstadoPregunta.rechazada));
        assertFalse(authorizationService.canEditPregunta(Pregunta.EstadoPregunta.aprobada));
        assertFalse(authorizationService.canEditPregunta(Pregunta.EstadoPregunta.usada));

        autenticar(RolUsuario.ROLE_VERIFICACION);
        assertTrue(authorizationService.canEditPregunta(Pregunta.EstadoPregunta.verificada));
        assertFalse(authorizationService.canEditPregunta(Pregunta.EstadoPregunta.para_aprobar));

        autenticar(RolUsuario.ROLE_DIRECCION);
        assertTrue(authorizationService.canEditPregunta(Pregunta.EstadoPregunta.para_aprobar));
        assertTrue(authorizationService.canEditPregunta(Pregunta.EstadoPregunta.rechazada));
        assertTrue(authorizationService.canEditPregunta(Pregunta.EstadoPregunta.aprobada));

        autenticar(RolUsuario.ROLE_CONSULTA);
        assertFalse(authorizationService.canEditPregunta(Pregunta.EstadoPregunta.borrador));

        sinUsuario();
        assertFalse(authorizationService.canEditPregunta(Pregunta.EstadoPregunta.borrador));
    }

    @Test
    void canChangeEstadoPregunta_flujos() {
        autenticar(RolUsuario.ROLE_DIRECCION);
        assertTrue(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.borrador, Pregunta.EstadoPregunta.usada));

        autenticar(RolUsuario.ROLE_ADMIN);
        assertTrue(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.borrador, Pregunta.EstadoPregunta.para_verificar));
        assertTrue(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.rechazada, Pregunta.EstadoPregunta.borrador));

        autenticar(RolUsuario.ROLE_GUION);
        assertTrue(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.borrador, Pregunta.EstadoPregunta.para_verificar));
        assertFalse(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.borrador, Pregunta.EstadoPregunta.verificada));
        assertTrue(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.para_verificar, Pregunta.EstadoPregunta.verificada));
        assertTrue(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.para_verificar, Pregunta.EstadoPregunta.revisar));
        assertTrue(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.revisar, Pregunta.EstadoPregunta.para_verificar));
        assertTrue(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.revisar, Pregunta.EstadoPregunta.para_aprobar));
        assertTrue(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.revisar, Pregunta.EstadoPregunta.rechazada));
        assertFalse(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.verificada, Pregunta.EstadoPregunta.aprobada));
        assertTrue(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.corregir, Pregunta.EstadoPregunta.para_aprobar));
        assertTrue(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.corregir, Pregunta.EstadoPregunta.para_verificar));
        assertFalse(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.para_aprobar, Pregunta.EstadoPregunta.aprobada));
        assertTrue(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.aprobada, Pregunta.EstadoPregunta.usada));
        assertTrue(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.usada, Pregunta.EstadoPregunta.aprobada));
        assertFalse(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.rechazada, Pregunta.EstadoPregunta.borrador));

        autenticar(RolUsuario.ROLE_VERIFICACION);
        assertTrue(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.para_verificar, Pregunta.EstadoPregunta.verificada));
        assertTrue(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.para_verificar, Pregunta.EstadoPregunta.revisar));
        assertFalse(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.borrador, Pregunta.EstadoPregunta.para_verificar));

        autenticar(RolUsuario.ROLE_CONSULTA);
        assertFalse(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.borrador, Pregunta.EstadoPregunta.para_verificar));

        sinUsuario();
        assertFalse(authorizationService.canChangeEstadoPregunta(
                Pregunta.EstadoPregunta.borrador, Pregunta.EstadoPregunta.para_verificar));
    }

    @Test
    void canCreateYEditCuestionario() {
        autenticar(RolUsuario.ROLE_ADMIN);
        assertTrue(authorizationService.canCreateCuestionario());
        assertTrue(authorizationService.canEditCuestionario(EstadoCuestionario.borrador));
        autenticar(RolUsuario.ROLE_DIRECCION);
        assertTrue(authorizationService.canCreateCuestionario());
        assertTrue(authorizationService.canEditCuestionario(EstadoCuestionario.grabado));
        autenticar(RolUsuario.ROLE_GUION);
        assertFalse(authorizationService.canCreateCuestionario());
        assertFalse(authorizationService.canEditCuestionario(EstadoCuestionario.borrador));
        autenticar(RolUsuario.ROLE_CONSULTA);
        assertFalse(authorizationService.canCreateCuestionario());
        sinUsuario();
        assertFalse(authorizationService.canCreateCuestionario());
        assertFalse(authorizationService.canEditCuestionario(EstadoCuestionario.borrador));
    }

    @Test
    void canEditCombo() {
        autenticar(RolUsuario.ROLE_ADMIN);
        assertTrue(authorizationService.canEditCombo(Combo.EstadoCombo.borrador));
        autenticar(RolUsuario.ROLE_DIRECCION);
        assertTrue(authorizationService.canEditCombo(Combo.EstadoCombo.aprobado));
        autenticar(RolUsuario.ROLE_VERIFICACION);
        assertFalse(authorizationService.canEditCombo(Combo.EstadoCombo.borrador));
        sinUsuario();
        assertFalse(authorizationService.canEditCombo(Combo.EstadoCombo.borrador));
    }

    @Test
    void canCreateYEditConcursante() {
        autenticar(RolUsuario.ROLE_ADMIN);
        assertTrue(authorizationService.canCreateConcursante());
        autenticar(RolUsuario.ROLE_GUION);
        assertTrue(authorizationService.canCreateConcursante());
        assertTrue(authorizationService.canEditConcursante(null));
        assertTrue(authorizationService.canEditConcursante("GRABADO"));
        assertFalse(authorizationService.canEditConcursante("EDITADO"));
        assertFalse(authorizationService.canEditConcursante("PROGRAMADO"));
        assertTrue(authorizationService.canEditConcursante("otro"));

        autenticar(RolUsuario.ROLE_VERIFICACION);
        assertTrue(authorizationService.canEditConcursante("GRABADO"));
        assertTrue(authorizationService.canEditConcursante("EDITADO"));
        assertFalse(authorizationService.canEditConcursante("PROGRAMADO"));

        autenticar(RolUsuario.ROLE_DIRECCION);
        assertTrue(authorizationService.canEditConcursante("PROGRAMADO"));
        assertTrue(authorizationService.canEditConcursante("GRABADO"));

        autenticar(RolUsuario.ROLE_CONSULTA);
        assertFalse(authorizationService.canCreateConcursante());
        assertFalse(authorizationService.canEditConcursante("GRABADO"));

        sinUsuario();
        assertFalse(authorizationService.canCreateConcursante());
        assertFalse(authorizationService.canEditConcursante("GRABADO"));
    }

    @Test
    void canCreateYEditPrograma() {
        autenticar(RolUsuario.ROLE_VERIFICACION);
        assertTrue(authorizationService.canCreatePrograma());
        assertTrue(authorizationService.canEditPrograma(Programa.EstadoPrograma.borrador));
        assertFalse(authorizationService.canEditPrograma(Programa.EstadoPrograma.programado));

        autenticar(RolUsuario.ROLE_DIRECCION);
        assertTrue(authorizationService.canCreatePrograma());
        assertTrue(authorizationService.canEditPrograma(Programa.EstadoPrograma.programado));
        assertFalse(authorizationService.canEditPrograma(Programa.EstadoPrograma.emitido));

        autenticar(RolUsuario.ROLE_ADMIN);
        assertFalse(authorizationService.canCreatePrograma());
        autenticar(RolUsuario.ROLE_GUION);
        assertFalse(authorizationService.canCreatePrograma());
        sinUsuario();
        assertFalse(authorizationService.canCreatePrograma());
        assertFalse(authorizationService.canEditPrograma(Programa.EstadoPrograma.borrador));
    }

    @Test
    void canDeleteCreateEditValidate() {
        autenticar(RolUsuario.ROLE_ADMIN);
        assertTrue(authorizationService.canDelete());
        assertTrue(authorizationService.canCreate());
        assertTrue(authorizationService.canEdit());
        assertTrue(authorizationService.canValidate());

        autenticar(RolUsuario.ROLE_DIRECCION);
        assertTrue(authorizationService.canDelete());
        assertTrue(authorizationService.canValidate());

        autenticar(RolUsuario.ROLE_VERIFICACION);
        assertFalse(authorizationService.canDelete());
        assertTrue(authorizationService.canCreate());
        assertTrue(authorizationService.canEdit());
        assertTrue(authorizationService.canValidate());

        autenticar(RolUsuario.ROLE_GUION);
        assertFalse(authorizationService.canDelete());
        assertTrue(authorizationService.canCreate());
        assertFalse(authorizationService.canValidate());

        autenticar(RolUsuario.ROLE_CONSULTA);
        assertFalse(authorizationService.canDelete());
        assertFalse(authorizationService.canCreate());
        assertFalse(authorizationService.canEdit());
        assertFalse(authorizationService.canValidate());

        sinUsuario();
        assertFalse(authorizationService.canDelete());
        assertFalse(authorizationService.canCreate());
        assertFalse(authorizationService.canEdit());
        assertFalse(authorizationService.canValidate());
    }
}
