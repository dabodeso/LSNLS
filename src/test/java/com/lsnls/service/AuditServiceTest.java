package com.lsnls.service;

import com.lsnls.entity.AuditLog;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuditService auditService;

    private Usuario usuario;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("ana");
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void logOperationGuardaLogSinRequest() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditService.logOperation(AuditLog.OperationType.CREATE, AuditLog.EntityType.PREGUNTA,
                5L, "creada", AuditLog.OperationResult.SUCCESS);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals(usuario, log.getUsuario());
        assertEquals(AuditLog.OperationType.CREATE, log.getOperationType());
        assertEquals(AuditLog.EntityType.PREGUNTA, log.getEntityType());
        assertEquals(5L, log.getEntityId());
        assertEquals("creada", log.getDescription());
        assertEquals(AuditLog.OperationResult.SUCCESS, log.getResult());
    }

    @Test
    void logOperationSinUsuarioUsaSistema() {
        when(authService.getCurrentUser()).thenReturn(Optional.empty());

        auditService.logOperation(AuditLog.OperationType.UPDATE, AuditLog.EntityType.SISTEMA,
                null, "auto", AuditLog.OperationResult.SUCCESS);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals("SISTEMA", captor.getValue().getUsuarioNombre());
    }

    @Test
    void logOperationCapturaExcepcion() {
        when(authService.getCurrentUser()).thenThrow(new RuntimeException("fallo auth"));

        assertDoesNotThrow(() -> auditService.logOperation(
                AuditLog.OperationType.DELETE, AuditLog.EntityType.PREGUNTA,
                1L, "x", AuditLog.OperationResult.FAILURE));
    }

    @Test
    void logOperationWithValuesSerializaObjetos() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario));

        auditService.logOperationWithValues(AuditLog.OperationType.UPDATE, AuditLog.EntityType.COMBO,
                2L, "cambio", Collections.singletonMap("a", 1), Collections.singletonMap("a", 2),
                AuditLog.OperationResult.SUCCESS);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertTrue(captor.getValue().getOldValues().contains("\"a\""));
        assertTrue(captor.getValue().getNewValues().contains("2"));
    }

    @Test
    void logOperationWithValuesAceptaNulos() {
        when(authService.getCurrentUser()).thenReturn(Optional.empty());

        auditService.logOperationWithValues(AuditLog.OperationType.UPDATE, AuditLog.EntityType.COMBO,
                2L, "cambio", null, null, AuditLog.OperationResult.PARTIAL_SUCCESS);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(null, captor.getValue().getOldValues());
        assertEquals(null, captor.getValue().getNewValues());
    }

    @Test
    void logOperationWithValuesCapturaExcepcion() {
        when(authService.getCurrentUser()).thenThrow(new RuntimeException("x"));

        assertDoesNotThrow(() -> auditService.logOperationWithValues(
                AuditLog.OperationType.UPDATE, AuditLog.EntityType.COMBO,
                1L, "d", "old", "new", AuditLog.OperationResult.FAILURE));
    }

    @Test
    void logSecurityEventGuardaEventoBloqueado() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario));

        auditService.logSecurityEvent("token inválido", "1.1.1.1", "JUnit");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals(AuditLog.OperationType.SECURITY_EVENT, log.getOperationType());
        assertEquals(AuditLog.EntityType.SISTEMA, log.getEntityType());
        assertEquals(AuditLog.OperationResult.BLOCKED, log.getResult());
        assertEquals("1.1.1.1", log.getIpAddress());
        assertEquals("JUnit", log.getUserAgent());
    }

    @Test
    void logSecurityEventCapturaExcepcion() {
        when(authService.getCurrentUser()).thenThrow(new RuntimeException("x"));

        assertDoesNotThrow(() -> auditService.logSecurityEvent("e", "ip", "ua"));
    }

    @Test
    void logSuccessfulLogin() {
        auditService.logSuccessfulLogin(usuario);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals(AuditLog.OperationType.LOGIN, log.getOperationType());
        assertEquals(AuditLog.EntityType.USUARIO, log.getEntityType());
        assertEquals(1L, log.getEntityId());
        assertEquals(AuditLog.OperationResult.SUCCESS, log.getResult());
        assertTrue(log.getDescription().contains("ana"));
    }

    @Test
    void logSuccessfulLoginCapturaExcepcion() {
        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("db"));

        assertDoesNotThrow(() -> auditService.logSuccessfulLogin(usuario));
    }

    @Test
    void logFailedLogin() {
        auditService.logFailedLogin("ana", "password incorrecta");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals(AuditLog.OperationType.LOGIN, log.getOperationType());
        assertEquals(AuditLog.OperationResult.FAILURE, log.getResult());
        assertEquals("password incorrecta", log.getErrorMessage());
        assertTrue(log.getDescription().contains("ana"));
    }

    @Test
    void logFailedLoginCapturaExcepcion() {
        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("db"));

        assertDoesNotThrow(() -> auditService.logFailedLogin("x", "y"));
    }

    @Test
    void consultasDelegadasAlRepositorio() {
        Page<AuditLog> pagina = new PageImpl<AuditLog>(Collections.emptyList());
        List<AuditLog> lista = Collections.emptyList();
        when(auditLogRepository.findByUsuarioOrderByTimestampDesc(usuario, pageable)).thenReturn(pagina);
        when(auditLogRepository.findByOperationTypeOrderByTimestampDesc(AuditLog.OperationType.CREATE, pageable))
                .thenReturn(pagina);
        when(auditLogRepository.findByEntityTypeOrderByTimestampDesc(AuditLog.EntityType.PREGUNTA, pageable))
                .thenReturn(pagina);
        when(auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(AuditLog.EntityType.PREGUNTA, 1L))
                .thenReturn(lista);
        LocalDateTime ini = LocalDateTime.now().minusDays(1);
        LocalDateTime fin = LocalDateTime.now();
        when(auditLogRepository.findByTimestampBetween(ini, fin, pageable)).thenReturn(pagina);
        when(auditLogRepository.findCriticalEvents(pageable)).thenReturn(pagina);
        when(auditLogRepository.findSecurityEvents(pageable)).thenReturn(pagina);
        when(auditLogRepository.getOperationStats(ini, fin)).thenReturn(Collections.emptyList());
        when(auditLogRepository.findByIpAddressAndTimestampBetween("1.1.1.1", ini, fin)).thenReturn(lista);

        assertEquals(pagina, auditService.findLogsByUser(usuario, pageable));
        assertEquals(pagina, auditService.findLogsByOperationType(AuditLog.OperationType.CREATE, pageable));
        assertEquals(pagina, auditService.findLogsByEntityType(AuditLog.EntityType.PREGUNTA, pageable));
        assertEquals(lista, auditService.findLogsByEntity(AuditLog.EntityType.PREGUNTA, 1L));
        assertEquals(pagina, auditService.findLogsByDateRange(ini, fin, pageable));
        assertEquals(pagina, auditService.findCriticalEvents(pageable));
        assertEquals(pagina, auditService.findSecurityEvents(pageable));
        assertEquals(Collections.emptyList(), auditService.getOperationStats(ini, fin));
        assertEquals(lista, auditService.findSuspiciousActivity("1.1.1.1", ini, fin));
    }

    @Test
    void cleanOldLogsInvocaDelete() {
        LocalDateTime corte = LocalDateTime.now().minusYears(1);

        auditService.cleanOldLogs(corte);

        verify(auditLogRepository).deleteLogsOlderThan(corte);
    }

    @Test
    void cleanOldLogsCapturaExcepcion() {
        doThrow(new RuntimeException("db")).when(auditLogRepository).deleteLogsOlderThan(any());

        assertDoesNotThrow(() -> auditService.cleanOldLogs(LocalDateTime.now()));
    }

    @Test
    void metodosDeConveniencia() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario));

        auditService.logPreguntaCreated(1L, "nueva");
        auditService.logPreguntaStateChange(1L, "borrador", "aprobada");
        auditService.logCuestionarioCreated(2L, 4);
        auditService.logComboCreated(3L, 3);
        auditService.logConcursanteAssigned(4L, 2L, 3L);
        auditService.logConcursanteAssigned(5L, null, null);
        auditService.logValidationError(6L, AuditLog.EntityType.PREGUNTA, "corta");
        auditService.logConcurrencyConflict(AuditLog.EntityType.COMBO, 7L, "update");

        verify(auditLogRepository, org.mockito.Mockito.times(8)).save(any(AuditLog.class));
    }
}
