package com.lsnls.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsnls.entity.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Sistema de deshacer respaldado por backend.
 *
 * Cada operación compleja (borrados, cambios de estado con cascada...) registra,
 * dentro de su misma transacción, la lista de acciones inversas necesarias para
 * revertirla: reinsertar filas borradas (conservando su id) y restaurar campos
 * a sus valores previos. El frontend recibe el id de la operación en la cabecera
 * X-Undo-Operacion-Id y puede revertirla con POST /api/undo/{id}.
 *
 * Reglas: el undo es por usuario (solo el autor puede deshacer), se conservan
 * como máximo 50 operaciones por usuario y caducan a la hora.
 */
@Service
@lombok.extern.slf4j.Slf4j
public class UndoService {

    public static final String HEADER_OPERACION = "X-Undo-Operacion-Id";

    private static final int MAX_OPERACIONES_POR_USUARIO = 50;
    private static final int MAX_ANTIGUEDAD_MINUTOS = 60;

    /** Tablas sobre las que se permite ejecutar acciones de undo. */
    private static final Set<String> TABLAS_PERMITIDAS = Set.of(
            "jornadas", "cuestionarios", "combos", "preguntas", "programas", "concursantes",
            "jornadas_cuestionarios", "jornadas_combos", "cuestionarios_preguntas",
            "combos_preguntas", "historial_jornadas",
            "tematicas", "tematicas_preguntas", "tematicas_combos", "subtemas_preguntas");

    private static final Pattern IDENTIFICADOR_VALIDO = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    /** Id de la última operación registrada en este hilo, para exponerla como cabecera HTTP. */
    private static final ThreadLocal<Long> ULTIMA_OPERACION = new ThreadLocal<>();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthorizationService authorizationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Devuelve y limpia el id de la última operación registrada en este hilo. */
    public static Long consumirUltimaOperacionId() {
        Long id = ULTIMA_OPERACION.get();
        ULTIMA_OPERACION.remove();
        return id;
    }

    // ==================== SNAPSHOTS ====================

    /** Copia completa de una fila por id, con valores serializables a JSON. */
    public Map<String, Object> snapshotFila(String tabla, Object id) {
        validarTabla(tabla);
        List<Map<String, Object>> filas = jdbcTemplate.queryForList(
                "SELECT * FROM " + tabla + " WHERE id = ?", id);
        return filas.isEmpty() ? null : normalizarFila(filas.get(0));
    }

    /** Copia completa de las filas que cumplen campo = valor. */
    public List<Map<String, Object>> snapshotFilas(String tabla, String campo, Object valor) {
        validarTabla(tabla);
        validarIdentificador(campo);
        List<Map<String, Object>> filas = jdbcTemplate.queryForList(
                "SELECT * FROM " + tabla + " WHERE `" + campo + "` = ?", valor);
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Map<String, Object> fila : filas) {
            resultado.add(normalizarFila(fila));
        }
        return resultado;
    }

    // ==================== CONSTRUCCIÓN DE ACCIONES ====================

    public static Map<String, Object> accionInsertarFila(String tabla, Map<String, Object> datos) {
        Map<String, Object> accion = new LinkedHashMap<>();
        accion.put("tipo", "insertar_fila");
        accion.put("tabla", tabla);
        accion.put("datos", datos);
        return accion;
    }

    public static Map<String, Object> accionActualizarCampos(String tabla, Object id, Map<String, Object> campos) {
        Map<String, Object> accion = new LinkedHashMap<>();
        accion.put("tipo", "actualizar_campos");
        accion.put("tabla", tabla);
        accion.put("id", id);
        accion.put("campos", campos);
        return accion;
    }

    public static Map<String, Object> accionEliminarFila(String tabla, Object id) {
        Map<String, Object> accion = new LinkedHashMap<>();
        accion.put("tipo", "eliminar_fila");
        accion.put("tabla", tabla);
        accion.put("id", id);
        return accion;
    }

    public static Map<String, Object> accionEliminarFilas(String tabla, String campo, Object valor) {
        Map<String, Object> accion = new LinkedHashMap<>();
        accion.put("tipo", "eliminar_filas");
        accion.put("tabla", tabla);
        accion.put("campo", campo);
        accion.put("valor", valor);
        return accion;
    }

    public static Long extraerId(Map<String, Object> fila) {
        if (fila == null) {
            return null;
        }
        Object id = fila.get("id");
        if (id instanceof Number) {
            return ((Number) id).longValue();
        }
        if (id instanceof String && !((String) id).isBlank()) {
            try {
                return Long.valueOf((String) id);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public static Set<Long> extraerIds(List<Map<String, Object>> filas) {
        Set<Long> ids = new HashSet<>();
        if (filas == null) {
            return ids;
        }
        for (Map<String, Object> fila : filas) {
            Long id = extraerId(fila);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    /** Filas actuales de `campo = valor` cuyo id no estaba en el conjunto previo. */
    public List<Map<String, Object>> snapshotFilasNuevas(String tabla, String campo, Object valor, Set<Long> idsPrevios) {
        List<Map<String, Object>> ahora = snapshotFilas(tabla, campo, valor);
        if (idsPrevios == null || idsPrevios.isEmpty()) {
            return ahora;
        }
        List<Map<String, Object>> nuevas = new ArrayList<>();
        for (Map<String, Object> fila : ahora) {
            Long id = extraerId(fila);
            if (id == null || !idsPrevios.contains(id)) {
                nuevas.add(fila);
            }
        }
        return nuevas;
    }

    // ==================== REGISTRO ====================

    /**
     * Registra una operación deshacible para el usuario actual. Participa en la
     * transacción del llamador: si la operación principal falla, el registro se
     * revierte con ella. Nunca lanza excepción para no romper la operación principal.
     */
    public void registrar(String tipoOperacion, String descripcion, List<Map<String, Object>> acciones) {
        try {
            if (acciones == null || acciones.isEmpty()) {
                return;
            }
            Optional<Usuario> usuario = authorizationService.getCurrentUser();
            if (usuario.isEmpty()) {
                return;
            }
            Long usuarioId = usuario.get().getId();
            String json = objectMapper.writeValueAsString(Collections.singletonMap("acciones", acciones));
            jdbcTemplate.update(
                    "INSERT INTO operaciones_undo (usuario_id, tipo_operacion, descripcion, datos_undo, fecha_creacion, deshecha) "
                            + "VALUES (?, ?, ?, ?, NOW(6), 0)",
                    usuarioId, tipoOperacion, descripcion, json);
            Long operacionId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            ULTIMA_OPERACION.set(operacionId);
            limpiarAntiguas(usuarioId);
        } catch (Exception e) {
            log.warn("[UNDO] No se pudo registrar la operación deshacible '" + tipoOperacion + "': " + e.getMessage());
        }
    }

    private void limpiarAntiguas(Long usuarioId) {
        jdbcTemplate.update(
                "DELETE FROM operaciones_undo WHERE usuario_id = ? AND fecha_creacion < NOW(6) - INTERVAL " + MAX_ANTIGUEDAD_MINUTOS + " MINUTE",
                usuarioId);
        jdbcTemplate.update(
                "DELETE FROM operaciones_undo WHERE usuario_id = ? AND id NOT IN ("
                        + "SELECT id FROM (SELECT id FROM operaciones_undo WHERE usuario_id = ? "
                        + "ORDER BY id DESC LIMIT " + MAX_OPERACIONES_POR_USUARIO + ") t)",
                usuarioId, usuarioId);
    }

    // ==================== DESHACER ====================

    /**
     * Revierte una operación registrada. Solo el autor puede deshacerla, una vez,
     * y dentro de la hora siguiente. Devuelve la descripción de lo deshecho.
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public String deshacer(Long operacionId) {
        Usuario usuario = authorizationService.getCurrentUser()
                .orElseThrow(() -> new IllegalArgumentException("Usuario no autenticado"));

        List<Map<String, Object>> filas = jdbcTemplate.queryForList(
                "SELECT * FROM operaciones_undo WHERE id = ?", operacionId);
        if (filas.isEmpty()) {
            throw new IllegalArgumentException("La operación ya no está disponible para deshacer");
        }
        Map<String, Object> operacion = filas.get(0);

        Number opUsuarioId = (Number) operacion.get("usuario_id");
        if (opUsuarioId == null || opUsuarioId.longValue() != usuario.getId()) {
            throw new IllegalArgumentException("Solo puedes deshacer tus propias operaciones");
        }

        Object deshechaObj = operacion.get("deshecha");
        boolean deshecha = (deshechaObj instanceof Boolean && (Boolean) deshechaObj)
                || (deshechaObj instanceof Number && ((Number) deshechaObj).intValue() != 0);
        if (deshecha) {
            throw new IllegalArgumentException("Esta operación ya fue deshecha");
        }

        Object fechaObj = operacion.get("fecha_creacion");
        if (fechaObj instanceof Timestamp) {
            Instant limite = Instant.now().minusSeconds(60L * MAX_ANTIGUEDAD_MINUTOS);
            if (((Timestamp) fechaObj).toInstant().isBefore(limite)) {
                throw new IllegalArgumentException("La operación tiene más de 1 hora y ya no se puede deshacer");
            }
        }

        List<Map<String, Object>> acciones;
        try {
            Map<String, Object> payload = objectMapper.readValue((String) operacion.get("datos_undo"), Map.class);
            acciones = (List<Map<String, Object>>) payload.get("acciones");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo interpretar la operación de undo: " + e.getMessage());
        }

        for (Map<String, Object> accion : acciones) {
            ejecutarAccion(accion);
        }

        jdbcTemplate.update("UPDATE operaciones_undo SET deshecha = 1 WHERE id = ?", operacionId);
        return (String) operacion.get("descripcion");
    }

    @SuppressWarnings("unchecked")
    private void ejecutarAccion(Map<String, Object> accion) {
        String tipo = (String) accion.get("tipo");
        String tabla = (String) accion.get("tabla");
        validarTabla(tabla);

        if ("insertar_fila".equals(tipo)) {
            insertarFila(tabla, (Map<String, Object>) accion.get("datos"));
        } else if ("actualizar_campos".equals(tipo)) {
            actualizarCampos(tabla, accion.get("id"), (Map<String, Object>) accion.get("campos"));
        } else if ("eliminar_fila".equals(tipo)) {
            eliminarFila(tabla, accion.get("id"));
        } else if ("eliminar_filas".equals(tipo)) {
            eliminarFilas(tabla, (String) accion.get("campo"), accion.get("valor"));
        } else {
            throw new IllegalStateException("Acción de undo desconocida: " + tipo);
        }
    }

    private void insertarFila(String tabla, Map<String, Object> datos) {
        if (datos == null || datos.isEmpty()) {
            return;
        }
        List<String> columnas = new ArrayList<>();
        List<Object> valores = new ArrayList<>();
        for (Map.Entry<String, Object> entrada : datos.entrySet()) {
            validarIdentificador(entrada.getKey());
            columnas.add("`" + entrada.getKey() + "`");
            valores.add(entrada.getValue());
        }
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < columnas.size(); i++) {
            placeholders.append(i > 0 ? ", ?" : "?");
        }
        String sql = "INSERT INTO " + tabla + " (" + String.join(", ", columnas) + ") VALUES (" + placeholders + ")";
        jdbcTemplate.update(sql, valores.toArray());
    }

    private void actualizarCampos(String tabla, Object id, Map<String, Object> campos) {
        if (id == null || campos == null || campos.isEmpty()) {
            return;
        }
        List<String> asignaciones = new ArrayList<>();
        List<Object> valores = new ArrayList<>();
        for (Map.Entry<String, Object> entrada : campos.entrySet()) {
            validarIdentificador(entrada.getKey());
            asignaciones.add("`" + entrada.getKey() + "` = ?");
            valores.add(entrada.getValue());
        }
        valores.add(id);
        String sql = "UPDATE " + tabla + " SET " + String.join(", ", asignaciones) + " WHERE id = ?";
        jdbcTemplate.update(sql, valores.toArray());
    }

    private void eliminarFila(String tabla, Object id) {
        if (id == null) {
            return;
        }
        if ("combos".equals(tabla)) {
            // Quitar FKs que impedirían borrar un combo creado por reciclaje/reaprovechar.
            jdbcTemplate.update("UPDATE concursantes SET combo_id = NULL WHERE combo_id = ?", id);
            jdbcTemplate.update("DELETE FROM combos_preguntas WHERE combo_id = ?", id);
            jdbcTemplate.update("DELETE FROM historial_jornadas WHERE combo_id = ?", id);
            jdbcTemplate.update("DELETE FROM jornadas_combos WHERE combo_id = ?", id);
        }
        jdbcTemplate.update("DELETE FROM " + tabla + " WHERE id = ?", id);
    }

    private void eliminarFilas(String tabla, String campo, Object valor) {
        if (valor == null) {
            return;
        }
        validarIdentificador(campo);
        jdbcTemplate.update("DELETE FROM " + tabla + " WHERE `" + campo + "` = ?", valor);
    }

    // ==================== UTILIDADES ====================

    /** Convierte tipos SQL no serializables (fechas, binarios) a representaciones aptas para JSON y reinserción. */
    private Map<String, Object> normalizarFila(Map<String, Object> fila) {
        Map<String, Object> normalizada = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entrada : fila.entrySet()) {
            Object valor = entrada.getValue();
            if (valor instanceof Timestamp || valor instanceof java.sql.Date || valor instanceof java.sql.Time
                    || valor instanceof java.time.temporal.Temporal) {
                valor = valor.toString();
            } else if (valor instanceof byte[]) {
                valor = Base64.getEncoder().encodeToString((byte[]) valor);
            }
            normalizada.put(entrada.getKey(), valor);
        }
        return normalizada;
    }

    private void validarTabla(String tabla) {
        if (tabla == null || !TABLAS_PERMITIDAS.contains(tabla)) {
            throw new IllegalStateException("Tabla no permitida en operaciones de undo: " + tabla);
        }
    }

    private void validarIdentificador(String nombre) {
        if (nombre == null || !IDENTIFICADOR_VALIDO.matcher(nombre).matches()) {
            throw new IllegalStateException("Identificador no válido en operación de undo: " + nombre);
        }
    }
}
