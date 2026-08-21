package com.lsnls.service;

import com.lsnls.dto.EntityChangeDTO;
import com.lsnls.dto.VisibleEntityDTO;
import com.lsnls.entity.AuditLog;
import com.lsnls.entity.Combo;
import com.lsnls.entity.Concursante;
import com.lsnls.entity.Cuestionario;
import com.lsnls.entity.Jornada;
import com.lsnls.entity.Pregunta;
import com.lsnls.entity.Programa;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.AuditLogRepository;
import com.lsnls.repository.ComboRepository;
import com.lsnls.repository.ConcursanteRepository;
import com.lsnls.repository.CuestionarioRepository;
import com.lsnls.repository.JornadaRepository;
import com.lsnls.repository.PreguntaRepository;
import com.lsnls.repository.ProgramaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock
    private PreguntaRepository preguntaRepository;

    @Mock
    private ComboRepository comboRepository;

    @Mock
    private CuestionarioRepository cuestionarioRepository;

    @Mock
    private JornadaRepository jornadaRepository;

    @Mock
    private ProgramaRepository programaRepository;

    @Mock
    private ConcursanteRepository concursanteRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private SyncService syncService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("ana");
    }

    @Test
    void checkVisibleChangesConItemsVacios() {
        assertTrue(syncService.checkVisibleChanges(null).isEmpty());
        assertTrue(syncService.checkVisibleChanges(Collections.emptyList()).isEmpty());
    }

    @Test
    void checkVisibleChangesIgnoraItemsInvalidosYMismaVersion() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario));
        Pregunta pregunta = new Pregunta();
        pregunta.setId(1L);
        pregunta.setVersion(3L);
        pregunta.setPregunta("¿Capital?");
        when(preguntaRepository.findById(1L)).thenReturn(Optional.of(pregunta));

        VisibleEntityDTO nulo = null;
        VisibleEntityDTO sinTipo = new VisibleEntityDTO();
        sinTipo.setEntityId(1L);
        VisibleEntityDTO sinId = new VisibleEntityDTO();
        sinId.setEntityType("PREGUNTA");
        VisibleEntityDTO tipoInvalido = item("NOEXISTE", 1L, 0L);
        VisibleEntityDTO mismaVersion = item("PREGUNTA", 1L, 3L);
        VisibleEntityDTO noEncontrada = item("PREGUNTA", 99L, 0L);
        VisibleEntityDTO usuarioTipo = item("USUARIO", 1L, 0L);

        List<VisibleEntityDTO> items = Arrays.asList(
                nulo, sinTipo, sinId, tipoInvalido, mismaVersion, noEncontrada, usuarioTipo);

        List<EntityChangeDTO> cambios = syncService.checkVisibleChanges(items);

        assertTrue(cambios.isEmpty());
    }

    @Test
    void checkVisibleChangesConEntidadesMockeadas() {
        when(authService.getCurrentUser()).thenReturn(Optional.of(usuario));

        Pregunta pregunta = new Pregunta();
        pregunta.setId(1L);
        pregunta.setVersion(5L);
        pregunta.setPregunta("¿Capital de España?");
        when(preguntaRepository.findById(1L)).thenReturn(Optional.of(pregunta));

        Combo combo = new Combo();
        combo.setId(2L);
        combo.setVersion(4L);
        when(comboRepository.findById(2L)).thenReturn(Optional.of(combo));

        Cuestionario cuestionario = new Cuestionario();
        cuestionario.setId(3L);
        cuestionario.setVersion(2L);
        when(cuestionarioRepository.findById(3L)).thenReturn(Optional.of(cuestionario));

        Jornada jornada = new Jornada();
        jornada.setId(4L);
        jornada.setVersion(7L);
        jornada.setNombre("Jornada Norte");
        when(jornadaRepository.findById(4L)).thenReturn(Optional.of(jornada));

        Programa programa = new Programa();
        programa.setId(5L);
        programa.setVersion(9L);
        programa.setTemporada(2024);
        programa.setCodigo("P01");
        when(programaRepository.findById(5L)).thenReturn(Optional.of(programa));

        Concursante concursante = new Concursante();
        concursante.setId(6L);
        concursante.setVersion(1L);
        concursante.setNombre("Luis");
        when(concursanteRepository.findById(6L)).thenReturn(Optional.of(concursante));

        Usuario otro = new Usuario();
        otro.setId(99L);
        otro.setNombre("bruno");
        AuditLog logUpdate = new AuditLog(otro, AuditLog.OperationType.UPDATE, AuditLog.EntityType.PREGUNTA,
                1L, "upd", AuditLog.OperationResult.SUCCESS);
        when(auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(
                AuditLog.EntityType.PREGUNTA, 1L)).thenReturn(Collections.singletonList(logUpdate));

        AuditLog logLogin = new AuditLog(otro, AuditLog.OperationType.LOGIN, AuditLog.EntityType.COMBO,
                2L, "login", AuditLog.OperationResult.SUCCESS);
        AuditLog logPropio = new AuditLog(usuario, AuditLog.OperationType.STATE_CHANGE, AuditLog.EntityType.COMBO,
                2L, "estado", AuditLog.OperationResult.SUCCESS);
        when(auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(
                AuditLog.EntityType.COMBO, 2L)).thenReturn(Arrays.asList(logLogin, logPropio));

        List<VisibleEntityDTO> items = new ArrayList<VisibleEntityDTO>();
        items.add(item("PREGUNTA", 1L, 1L));
        items.add(item("COMBO", 2L, 1L));
        items.add(item("CUESTIONARIO", 3L, 0L));
        items.add(item("JORNADA", 4L, 1L));
        items.add(item("PROGRAMA", 5L, 1L));
        items.add(item("CONCURSANTE", 6L, null));

        List<EntityChangeDTO> cambios = syncService.checkVisibleChanges(items);

        assertEquals(6, cambios.size());
        assertEquals("PREGUNTA", cambios.get(0).getEntityType());
        assertEquals("¿Capital de España?", cambios.get(0).getEntityLabel());
        assertEquals("bruno", cambios.get(0).getUsuarioNombre());
        assertEquals(5L, cambios.get(0).getVersion());
        assertTrue(cambios.get(0).getMensaje().contains("bruno"));

        assertEquals("Combo 2", cambios.get(1).getEntityLabel());
        assertTrue(cambios.get(1).getMensaje().startsWith("Otro usuario"));

        assertEquals("Cuestionario 3", cambios.get(2).getEntityLabel());
        assertEquals("Jornada Norte", cambios.get(3).getEntityLabel());
        assertEquals("Programa T2024 #P01", cambios.get(4).getEntityLabel());
        assertEquals("Luis", cambios.get(5).getEntityLabel());
        assertEquals(1L, cambios.get(5).getVersion());
    }

    @Test
    void checkVisibleChangesProgramaSinCodigoUsaId() {
        when(authService.getCurrentUser()).thenReturn(Optional.empty());
        Programa programa = new Programa();
        programa.setId(8L);
        programa.setVersion(2L);
        programa.setTemporada(2025);
        when(programaRepository.findById(8L)).thenReturn(Optional.of(programa));

        List<EntityChangeDTO> cambios = syncService.checkVisibleChanges(
                Collections.singletonList(item("programa", 8L, 0L)));

        assertEquals(1, cambios.size());
        assertEquals("Programa T2025 #8", cambios.get(0).getEntityLabel());
    }

    @Test
    void checkVisibleChangesFallbacksSiEntidadDesapareceTrasVersion() {
        when(authService.getCurrentUser()).thenReturn(Optional.empty());
        Pregunta pregunta = new Pregunta();
        pregunta.setId(1L);
        pregunta.setVersion(2L);
        when(preguntaRepository.findById(1L)).thenReturn(Optional.of(pregunta), Optional.empty());
        Jornada jornada = new Jornada();
        jornada.setId(4L);
        jornada.setVersion(2L);
        when(jornadaRepository.findById(4L)).thenReturn(Optional.of(jornada), Optional.empty());
        Concursante concursante = new Concursante();
        concursante.setId(6L);
        concursante.setVersion(2L);
        when(concursanteRepository.findById(6L)).thenReturn(Optional.of(concursante), Optional.empty());
        Programa programa = new Programa();
        programa.setId(5L);
        programa.setVersion(2L);
        when(programaRepository.findById(5L)).thenReturn(Optional.of(programa), Optional.empty());

        List<VisibleEntityDTO> items = Arrays.asList(
                item("PREGUNTA", 1L, 0L),
                item("JORNADA", 4L, 0L),
                item("CONCURSANTE", 6L, 0L),
                item("PROGRAMA", 5L, 0L));

        List<EntityChangeDTO> cambios = syncService.checkVisibleChanges(items);

        assertEquals("Pregunta 1", cambios.get(0).getEntityLabel());
        assertEquals("Jornada 4", cambios.get(1).getEntityLabel());
        assertEquals("Concursante 6", cambios.get(2).getEntityLabel());
        assertEquals("Programa 5", cambios.get(3).getEntityLabel());
    }

    private VisibleEntityDTO item(String tipo, Long id, Long version) {
        VisibleEntityDTO dto = new VisibleEntityDTO();
        dto.setEntityType(tipo);
        dto.setEntityId(id);
        dto.setVersion(version);
        return dto;
    }
}
