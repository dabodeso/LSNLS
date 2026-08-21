package com.lsnls.service;

import com.lsnls.entity.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UndoServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private UndoService undoService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNombre("ana");
        UndoService.consumirUltimaOperacionId();
    }

    @AfterEach
    void tearDown() {
        UndoService.consumirUltimaOperacionId();
    }

    @Test
    void snapshotFilaConIdNuloDevuelveNullSiNoHayFilas() {
        when(jdbcTemplate.queryForList(anyString(), nullable(Object.class)))
                .thenReturn(Collections.emptyList());

        Map<String, Object> resultado = undoService.snapshotFila("preguntas", null);

        assertNull(resultado);
        verify(jdbcTemplate).queryForList(contains("preguntas"), nullable(Object.class));
    }

    @Test
    void snapshotFilaNormalizaTiposNoSerializables() {
        Map<String, Object> fila = new LinkedHashMap<String, Object>();
        fila.put("id", 3L);
        fila.put("ts", Timestamp.valueOf("2024-01-15 10:30:00"));
        fila.put("fecha", java.sql.Date.valueOf("2024-01-15"));
        fila.put("hora", java.sql.Time.valueOf("10:30:00"));
        fila.put("ldt", LocalDateTime.of(2024, 1, 15, 10, 30));
        fila.put("bin", new byte[] {1, 2, 3});
        fila.put("texto", "ok");
        when(jdbcTemplate.queryForList(anyString(), eq(3L)))
                .thenReturn(Collections.singletonList(fila));

        Map<String, Object> resultado = undoService.snapshotFila("preguntas", 3L);

        assertNotNull(resultado);
        assertEquals(3L, resultado.get("id"));
        assertTrue(resultado.get("ts") instanceof String);
        assertTrue(resultado.get("fecha") instanceof String);
        assertTrue(resultado.get("hora") instanceof String);
        assertTrue(resultado.get("ldt") instanceof String);
        assertEquals(Base64.getEncoder().encodeToString(new byte[] {1, 2, 3}), resultado.get("bin"));
        assertEquals("ok", resultado.get("texto"));
    }

    @Test
    void accionEliminarFilaConstruyeMapa() {
        Map<String, Object> accion = UndoService.accionEliminarFila("tematicas", 4L);

        assertEquals("eliminar_fila", accion.get("tipo"));
        assertEquals("tematicas", accion.get("tabla"));
        assertEquals(4L, accion.get("id"));
    }

    @Test
    void accionEliminarFilasConstruyeMapa() {
        Map<String, Object> accion = UndoService.accionEliminarFilas("combos_preguntas", "combo_id", 9L);

        assertEquals("eliminar_filas", accion.get("tipo"));
        assertEquals("combos_preguntas", accion.get("tabla"));
        assertEquals("combo_id", accion.get("campo"));
        assertEquals(9L, accion.get("valor"));
    }

    @Test
    void extraerIdAceptaNumberYString() {
        Map<String, Object> fila = new HashMap<String, Object>();
        fila.put("id", 12);
        assertEquals(12L, UndoService.extraerId(fila));
        fila.put("id", "15");
        assertEquals(15L, UndoService.extraerId(fila));
        fila.put("id", "x");
        assertNull(UndoService.extraerId(fila));
        assertNull(UndoService.extraerId(null));
        assertNull(UndoService.extraerId(Collections.emptyMap()));
        assertEquals(Collections.emptySet(), UndoService.extraerIds(null));
        assertEquals(Collections.singleton(12L), UndoService.extraerIds(Collections.singletonList(
                Collections.singletonMap("id", 12L))));
    }

    @Test
    void snapshotFilasNuevasFiltraIdsPrevios() {
        Map<String, Object> vieja = new HashMap<String, Object>();
        vieja.put("id", 1L);
        Map<String, Object> nueva = new HashMap<String, Object>();
        nueva.put("id", 2L);
        List<Map<String, Object>> filas = new ArrayList<Map<String, Object>>();
        filas.add(vieja);
        filas.add(nueva);
        when(jdbcTemplate.queryForList(anyString(), eq(10L))).thenReturn(filas);

        List<Map<String, Object>> resultado = undoService.snapshotFilasNuevas(
                "historial_jornadas", "combo_id", 10L, Collections.singleton(1L));

        assertEquals(1, resultado.size());
        assertEquals(2L, resultado.get(0).get("id"));
    }

    @Test
    void deshacerEliminarFila() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario));
        String json = "{\"acciones\":[{\"tipo\":\"eliminar_fila\",\"tabla\":\"tematicas\",\"id\":5}]}";
        Map<String, Object> op = operacionBase(7L, 0, Instant.now(), json);
        op.put("descripcion", "Añadir temática");
        when(jdbcTemplate.queryForList(anyString(), eq(21L))).thenReturn(Collections.singletonList(op));
        List<String> sqls = capturarUpdates();

        String desc = undoService.deshacer(21L);

        assertEquals("Añadir temática", desc);
        assertTrue(sqls.stream().anyMatch(s -> s.contains("DELETE FROM tematicas WHERE id = ?")));
        assertTrue(sqls.stream().anyMatch(s -> s.contains("deshecha = 1")));
    }

    @Test
    void deshacerEliminarFilaComboLimpiaDependencias() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario));
        String json = "{\"acciones\":[{\"tipo\":\"eliminar_fila\",\"tabla\":\"combos\",\"id\":9}]}";
        when(jdbcTemplate.queryForList(anyString(), eq(22L)))
                .thenReturn(Collections.singletonList(operacionBase(7L, 0, Instant.now(), json)));
        List<String> sqls = capturarUpdates();

        undoService.deshacer(22L);

        assertTrue(sqls.stream().anyMatch(s -> s.contains("UPDATE concursantes SET combo_id = NULL")));
        assertTrue(sqls.stream().anyMatch(s -> s.contains("DELETE FROM combos_preguntas")));
        assertTrue(sqls.stream().anyMatch(s -> s.contains("DELETE FROM combos WHERE id = ?")));
    }

    @Test
    void deshacerEliminarFilas() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario));
        String json = "{\"acciones\":[{\"tipo\":\"eliminar_filas\",\"tabla\":\"combos_preguntas\","
                + "\"campo\":\"combo_id\",\"valor\":9}]}";
        when(jdbcTemplate.queryForList(anyString(), eq(23L)))
                .thenReturn(Collections.singletonList(operacionBase(7L, 0, Instant.now(), json)));
        List<String> sqls = capturarUpdates();

        undoService.deshacer(23L);

        assertTrue(sqls.stream().anyMatch(s -> s.contains("DELETE FROM combos_preguntas WHERE `combo_id` = ?")));
    }

    @Test
    void snapshotFilaTablaTematicasPermitida() {
        when(jdbcTemplate.queryForList(anyString(), eq(1L))).thenReturn(Collections.emptyList());
        assertNull(undoService.snapshotFila("tematicas_preguntas", 1L));
        assertNull(undoService.snapshotFila("subtemas_preguntas", 1L));
    }

    @Test
    void snapshotFilasDevuelveFilasNormalizadas() {
        Map<String, Object> fila1 = new HashMap<String, Object>();
        fila1.put("id", 1L);
        fila1.put("nombre", "a");
        Map<String, Object> fila2 = new HashMap<String, Object>();
        fila2.put("id", 2L);
        fila2.put("nombre", "b");
        List<Map<String, Object>> filas = new ArrayList<Map<String, Object>>();
        filas.add(fila1);
        filas.add(fila2);
        when(jdbcTemplate.queryForList(anyString(), eq(10L))).thenReturn(filas);

        List<Map<String, Object>> resultado = undoService.snapshotFilas("jornadas", "jornada_id", 10L);

        assertEquals(2, resultado.size());
        assertEquals(1L, resultado.get(0).get("id"));
        assertEquals("b", resultado.get(1).get("nombre"));
    }

    @Test
    void snapshotFilasCampoInvalidoLanzaExcepcion() {
        assertThrows(IllegalStateException.class,
                () -> undoService.snapshotFilas("preguntas", "id;drop", 1L));
    }

    @Test
    void accionInsertarFilaConstruyeMapa() {
        Map<String, Object> datos = new HashMap<String, Object>();
        datos.put("id", 1L);
        Map<String, Object> accion = UndoService.accionInsertarFila("preguntas", datos);

        assertEquals("insertar_fila", accion.get("tipo"));
        assertEquals("preguntas", accion.get("tabla"));
        assertEquals(datos, accion.get("datos"));
    }

    @Test
    void accionActualizarCamposConstruyeMapa() {
        Map<String, Object> campos = new HashMap<String, Object>();
        campos.put("estado", "borrador");
        Map<String, Object> accion = UndoService.accionActualizarCampos("preguntas", 8L, campos);

        assertEquals("actualizar_campos", accion.get("tipo"));
        assertEquals("preguntas", accion.get("tabla"));
        assertEquals(8L, accion.get("id"));
        assertEquals(campos, accion.get("campos"));
    }

    @Test
    void registrarConAccionesVaciasNoHaceNada() {
        undoService.registrar("borrar", "desc", Collections.emptyList());
        undoService.registrar("borrar", "desc", null);

        verify(authorizationService, never()).getCurrentUser();
        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any(), any());
    }

    @Test
    void registrarSinUsuarioNoInserta() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.empty());
        List<Map<String, Object>> acciones = Collections.singletonList(
                UndoService.accionInsertarFila("preguntas", Collections.singletonMap("id", 1L)));

        undoService.registrar("borrar_pregunta", "Borrar", acciones);

        verify(jdbcTemplate, never()).update(contains("operaciones_undo"), any(), any(), any(), any());
        assertNull(UndoService.consumirUltimaOperacionId());
    }

    @Test
    void registrarConUsuarioGuardaOperacionYThreadLocal() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario));
        when(jdbcTemplate.queryForObject(contains("LAST_INSERT_ID"), eq(Long.class))).thenReturn(42L);
        List<Map<String, Object>> acciones = Collections.singletonList(
                UndoService.accionInsertarFila("preguntas", Collections.singletonMap("id", 1L)));

        undoService.registrar("borrar_pregunta", "Borrar pregunta", acciones);

        verify(jdbcTemplate).update(contains("INSERT INTO operaciones_undo"),
                eq(7L), eq("borrar_pregunta"), eq("Borrar pregunta"), anyString());
        assertEquals(42L, UndoService.consumirUltimaOperacionId());
        assertNull(UndoService.consumirUltimaOperacionId());
    }

    @Test
    void registrarCapturaExcepcionSinPropagarla() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario));
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("fallo sql"));
        List<Map<String, Object>> acciones = Collections.singletonList(
                UndoService.accionInsertarFila("preguntas", Collections.singletonMap("id", 1L)));

        undoService.registrar("borrar", "desc", acciones);

        assertNull(UndoService.consumirUltimaOperacionId());
    }

    @Test
    void deshacerSinUsuarioAutenticado() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> undoService.deshacer(1L));
        assertEquals("Usuario no autenticado", ex.getMessage());
    }

    @Test
    void deshacerOperacionInexistente() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario));
        when(jdbcTemplate.queryForList(anyString(), eq(99L))).thenReturn(Collections.emptyList());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> undoService.deshacer(99L));
        assertEquals("La operación ya no está disponible para deshacer", ex.getMessage());
    }

    @Test
    void deshacerSiNoEsAutor() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario));
        when(jdbcTemplate.queryForList(anyString(), eq(1L)))
                .thenReturn(Collections.singletonList(operacionBase(99L, 0, Instant.now(), "{}")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> undoService.deshacer(1L));
        assertEquals("Solo puedes deshacer tus propias operaciones", ex.getMessage());
    }

    @Test
    void deshacerSiUsuarioIdNulo() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario));
        Map<String, Object> op = operacionBase(null, 0, Instant.now(), "{}");
        when(jdbcTemplate.queryForList(anyString(), eq(1L))).thenReturn(Collections.singletonList(op));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> undoService.deshacer(1L));
        assertEquals("Solo puedes deshacer tus propias operaciones", ex.getMessage());
    }

    @Test
    void deshacerYaDeshechaConNumero() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario));
        when(jdbcTemplate.queryForList(anyString(), eq(1L)))
                .thenReturn(Collections.singletonList(operacionBase(7L, 1, Instant.now(), "{}")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> undoService.deshacer(1L));
        assertEquals("Esta operación ya fue deshecha", ex.getMessage());
    }

    @Test
    void deshacerYaDeshechaConBoolean() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario));
        Map<String, Object> op = operacionBase(7L, true, Instant.now(), "{}");
        when(jdbcTemplate.queryForList(anyString(), eq(1L))).thenReturn(Collections.singletonList(op));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> undoService.deshacer(1L));
        assertEquals("Esta operación ya fue deshecha", ex.getMessage());
    }

    @Test
    void deshacerOperacionExpirada() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario));
        Instant antigua = Instant.now().minusSeconds(61L * 60L);
        when(jdbcTemplate.queryForList(anyString(), eq(1L)))
                .thenReturn(Collections.singletonList(operacionBase(7L, 0, antigua, "{}")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> undoService.deshacer(1L));
        assertEquals("La operación tiene más de 1 hora y ya no se puede deshacer", ex.getMessage());
    }

    @Test
    void deshacerJsonInvalido() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario));
        when(jdbcTemplate.queryForList(anyString(), eq(1L)))
                .thenReturn(Collections.singletonList(operacionBase(7L, 0, Instant.now(), "no-json")));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> undoService.deshacer(1L));
        assertTrue(ex.getMessage().contains("No se pudo interpretar"));
    }

    @Test
    void deshacerInsertarFila() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario));
        String json = "{\"acciones\":[{\"tipo\":\"insertar_fila\",\"tabla\":\"preguntas\","
                + "\"datos\":{\"id\":5,\"pregunta\":\"Hola\"}}]}";
        Map<String, Object> op = operacionBase(7L, 0, Instant.now(), json);
        op.put("descripcion", "Borrar pregunta");
        when(jdbcTemplate.queryForList(anyString(), eq(11L))).thenReturn(Collections.singletonList(op));
        List<String> sqls = capturarUpdates();

        String desc = undoService.deshacer(11L);

        assertEquals("Borrar pregunta", desc);
        assertTrue(sqls.stream().anyMatch(s -> s.contains("INSERT INTO preguntas")));
        assertTrue(sqls.stream().anyMatch(s -> s.contains("deshecha = 1")));
    }

    @Test
    void deshacerInsertarFilaSinDatosNoInserta() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario));
        String json = "{\"acciones\":[{\"tipo\":\"insertar_fila\",\"tabla\":\"preguntas\",\"datos\":{}}]}";
        when(jdbcTemplate.queryForList(anyString(), eq(12L)))
                .thenReturn(Collections.singletonList(operacionBase(7L, 0, Instant.now(), json)));

        undoService.deshacer(12L);

        verify(jdbcTemplate, never()).update(contains("INSERT INTO"), any(), any());
        verify(jdbcTemplate).update(contains("deshecha = 1"), eq(12L));
    }

    @Test
    void deshacerActualizarCampos() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario));
        String json = "{\"acciones\":[{\"tipo\":\"actualizar_campos\",\"tabla\":\"preguntas\","
                + "\"id\":5,\"campos\":{\"estado\":\"borrador\"}}]}";
        Map<String, Object> op = operacionBase(7L, 0, Instant.now(), json);
        op.put("descripcion", "Cambio de estado");
        when(jdbcTemplate.queryForList(anyString(), eq(13L))).thenReturn(Collections.singletonList(op));
        List<String> sqls = capturarUpdates();

        String desc = undoService.deshacer(13L);

        assertEquals("Cambio de estado", desc);
        assertTrue(sqls.stream().anyMatch(s -> s.contains("UPDATE preguntas SET")));
        assertTrue(sqls.stream().anyMatch(s -> s.contains("deshecha = 1")));
    }

    @Test
    void deshacerActualizarCamposSinIdNoActualiza() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario));
        String json = "{\"acciones\":[{\"tipo\":\"actualizar_campos\",\"tabla\":\"preguntas\","
                + "\"id\":null,\"campos\":{\"estado\":\"borrador\"}}]}";
        when(jdbcTemplate.queryForList(anyString(), eq(14L)))
                .thenReturn(Collections.singletonList(operacionBase(7L, 0, Instant.now(), json)));

        undoService.deshacer(14L);

        verify(jdbcTemplate, never()).update(contains("UPDATE preguntas SET"), any(), any());
        verify(jdbcTemplate).update(contains("deshecha = 1"), eq(14L));
    }

    @Test
    void deshacerTablaDesconocida() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario));
        String json = "{\"acciones\":[{\"tipo\":\"insertar_fila\",\"tabla\":\"usuarios\","
                + "\"datos\":{\"id\":1}}]}";
        when(jdbcTemplate.queryForList(anyString(), eq(15L)))
                .thenReturn(Collections.singletonList(operacionBase(7L, 0, Instant.now(), json)));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> undoService.deshacer(15L));
        assertTrue(ex.getMessage().contains("Tabla no permitida"));
    }

    @Test
    void deshacerAccionDesconocida() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario));
        String json = "{\"acciones\":[{\"tipo\":\"borrar_fila\",\"tabla\":\"preguntas\"}]}";
        when(jdbcTemplate.queryForList(anyString(), eq(16L)))
                .thenReturn(Collections.singletonList(operacionBase(7L, 0, Instant.now(), json)));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> undoService.deshacer(16L));
        assertTrue(ex.getMessage().contains("Acción de undo desconocida"));
    }

    @Test
    void consumirUltimaOperacionIdSinRegistroEsNull() {
        assertNull(UndoService.consumirUltimaOperacionId());
    }

    @Test
    void deshacerNoExpiraSiFechaNoEsTimestamp() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario));
        String json = "{\"acciones\":[]}";
        Map<String, Object> op = operacionBase(7L, 0, Instant.now(), json);
        op.put("fecha_creacion", "2020-01-01");
        op.put("descripcion", "antigua");
        when(jdbcTemplate.queryForList(anyString(), eq(17L))).thenReturn(Collections.singletonList(op));

        String desc = undoService.deshacer(17L);

        assertEquals("antigua", desc);
        verify(jdbcTemplate).update(contains("deshecha = 1"), eq(17L));
    }

    @Test
    void deshechaFalseConBooleanCeroNoBloquea() {
        when(authorizationService.getCurrentUser()).thenReturn(Optional.of(usuario));
        String json = "{\"acciones\":[]}";
        Map<String, Object> op = operacionBase(7L, false, Instant.now(), json);
        op.put("descripcion", "ok");
        when(jdbcTemplate.queryForList(anyString(), eq(18L))).thenReturn(Collections.singletonList(op));

        assertEquals("ok", undoService.deshacer(18L));
    }

    private List<String> capturarUpdates() {
        List<String> sqls = new ArrayList<String>();
        org.mockito.stubbing.Answer<Integer> recoger = inv -> {
            sqls.add(inv.getArgument(0));
            return 1;
        };
        org.mockito.Mockito.lenient().when(jdbcTemplate.update(anyString(), any(Object.class)))
                .thenAnswer(recoger);
        org.mockito.Mockito.lenient().when(jdbcTemplate.update(anyString(), any(Object.class), any(Object.class)))
                .thenAnswer(recoger);
        return sqls;
    }

    private Map<String, Object> operacionBase(Number usuarioId, Object deshecha, Instant fecha, String json) {
        Map<String, Object> op = new HashMap<String, Object>();
        op.put("usuario_id", usuarioId);
        op.put("deshecha", deshecha);
        op.put("fecha_creacion", Timestamp.from(fecha));
        op.put("datos_undo", json);
        op.put("descripcion", "op");
        return op;
    }
}
