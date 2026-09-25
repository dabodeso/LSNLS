package com.lsnls.service;

import com.lsnls.config.SlotsJornada;
import com.lsnls.dto.JornadaDTO;
import com.lsnls.dto.ReciclajeComboDTO;
import com.lsnls.entity.*;
import com.lsnls.repository.*;
import com.lsnls.entity.PreguntaCombo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.time.LocalDate;
import java.util.Comparator;
import javax.persistence.EntityManager;

@Service
@Transactional
@lombok.extern.slf4j.Slf4j
public class JornadaService {

    @Autowired
    private JornadaRepository jornadaRepository;

    @Autowired
    private CuestionarioRepository cuestionarioRepository;

    @Autowired
    private ComboRepository comboRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ExcelExportService excelExportService;

    @Autowired
    private CuestionarioService cuestionarioService;

    @Autowired
    private ComboService comboService;

    @Autowired
    private PreguntaComboRepository preguntaComboRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UndoService undoService;

    @Autowired
    private ConcursanteRepository concursanteRepository;

    public List<JornadaDTO> obtenerTodas() {
        // Normalizar estados legacy en BD antes de leer
        try { normalizarEstadosLegacy(); } catch (Exception ignored) {}
        List<Jornada> jornadas = jornadaRepository.findAllOrderByFechaCreacionDesc();
        return jornadas.stream().map(this::convertirADTO).collect(Collectors.toList());
    }

    public Page<JornadaDTO> obtenerTodasPaginadas(Pageable pageable) {
        try { normalizarEstadosLegacy(); } catch (Exception ignored) {}
        Page<Jornada> jornadas = jornadaRepository.findAllOrderByIdDesc(pageable);
        return jornadas.map(this::convertirADTO);
    }

    public Page<JornadaDTO> obtenerTodasPaginadasConFiltros(Pageable pageable, 
            String estado, String fechaDesde, String fechaHasta, String buscar) {
        try { normalizarEstadosLegacy(); } catch (Exception ignored) {}
        // Convertir fechas de String a LocalDate
        LocalDate fechaDesdeLocal = null;
        LocalDate fechaHastaLocal = null;
        
        if (fechaDesde != null && !fechaDesde.isEmpty()) {
            fechaDesdeLocal = LocalDate.parse(fechaDesde);
        }
        if (fechaHasta != null && !fechaHasta.isEmpty()) {
            fechaHastaLocal = LocalDate.parse(fechaHasta);
        }
        
        // Convertir estado de String a Enum
        Jornada.EstadoJornada estadoEnum = null;
        if (estado != null && !estado.isEmpty()) {
            try {
                // Los valores del enum son en minúsculas, no necesitamos toUpperCase()
                estadoEnum = Jornada.EstadoJornada.valueOf(estado);
            } catch (IllegalArgumentException e) {
                // Si el estado no es válido, ignorar el filtro
                estadoEnum = null;
            }
        }
        
        Page<Jornada> jornadas = jornadaRepository.findAllWithFilters(pageable, estado, estadoEnum,
                fechaDesdeLocal, fechaHastaLocal, buscar);
        return jornadas.map(this::convertirADTO);
    }

    /**
     * Normaliza los valores legacy de estados de jornada en la base de datos
     * para que coincidan con los nuevos: preparacion, lista, en_grabacion, completada, archivada.
     */
    private void normalizarEstadosLegacy() {
        // Migrar estados antiguos a los nuevos del enum actual
        entityManager.createNativeQuery("UPDATE jornadas SET estado='preparacion' WHERE estado='borrador'").executeUpdate();
        entityManager.createNativeQuery("UPDATE jornadas SET estado='completada' WHERE estado='completa'").executeUpdate();
        entityManager.createNativeQuery("UPDATE jornadas SET estado='archivada' WHERE estado='grabada'").executeUpdate();
    }

    public Optional<JornadaDTO> obtenerPorId(Long id) {
        return jornadaRepository.findById(id).map(this::convertirADTO);
    }

    public JornadaDTO crear(JornadaDTO jornadaDTO, Long usuarioId) {
        if (jornadaRepository.existsByNombre(jornadaDTO.getNombre())) {
            throw new IllegalArgumentException("Ya existe una jornada con ese nombre");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Jornada jornada = new Jornada();
        jornada.setNombre(jornadaDTO.getNombre());
        jornada.setFechaJornada(jornadaDTO.getFechaJornada());
        jornada.setLugar(jornadaDTO.getLugar());
        jornada.setNotas(jornadaDTO.getNotas());
        jornada.setCreacionUsuario(usuario);
        jornada.setEstado(Jornada.EstadoJornada.preparacion);

        if (jornadaDTO.getCuestionarioIds() != null) {
            jornada.reemplazarCuestionariosPorSlot(
                cargarCuestionariosEnSlots(jornadaDTO.getCuestionarioIds(), Collections.emptySet()));
        }
        if (jornadaDTO.getComboIds() != null) {
            jornada.reemplazarCombosPorSlot(
                cargarCombosEnSlots(jornadaDTO.getComboIds(), Collections.emptySet()));
        }

        jornada = jornadaRepository.save(jornada);
        return convertirADTO(jornada);
    }

    public JornadaDTO actualizar(Long id, JornadaDTO jornadaDTO) {
        Jornada jornada = jornadaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Jornada no encontrada"));

        // Verificar si se puede editar
        if (jornada.getEstado() == Jornada.EstadoJornada.completada ||
            jornada.getEstado() == Jornada.EstadoJornada.archivada) {
            throw new IllegalArgumentException("No se puede editar una jornada completada o archivada");
        }

        // Actualizar campos básicos
        jornada.setNombre(jornadaDTO.getNombre());
        jornada.setFechaJornada(jornadaDTO.getFechaJornada());
        jornada.setLugar(jornadaDTO.getLugar());
        jornada.setNotas(jornadaDTO.getNotas());

        if (jornadaDTO.getCuestionarioIds() != null) {
            Set<Long> actuales = idsDe(jornada.getCuestionarios());
            Set<Long> nuevos = SlotsJornada.idsAsignados(SlotsJornada.normalizarIds(jornadaDTO.getCuestionarioIds()));
            validarCuestionariosNoQuitables(jornada.getCuestionarios(), nuevos);
            liberarCuestionariosQuitados(jornada.getCuestionarios(), nuevos);
            jornada.reemplazarCuestionariosPorSlot(cargarCuestionariosEnSlots(jornadaDTO.getCuestionarioIds(), actuales));
        }
        if (jornadaDTO.getComboIds() != null) {
            Set<Long> actuales = idsDe(jornada.getCombos());
            Set<Long> nuevos = SlotsJornada.idsAsignados(SlotsJornada.normalizarIds(jornadaDTO.getComboIds()));
            validarCombosNoQuitables(jornada.getCombos(), nuevos);
            liberarCombosQuitados(jornada.getCombos(), nuevos);
            jornada.reemplazarCombosPorSlot(cargarCombosEnSlots(jornadaDTO.getComboIds(), actuales));
        }

        jornada = jornadaRepository.save(jornada);
        return convertirADTO(jornada);
    }

    public void eliminar(Long id) {
        Jornada jornada = jornadaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Jornada con ID " + id + " no encontrada"));

        // Verificar estado de la jornada
        if (jornada.getEstado() == Jornada.EstadoJornada.completada ||
            jornada.getEstado() == Jornada.EstadoJornada.archivada) {
            throw new IllegalArgumentException("No se puede eliminar una jornada que ya está completada o archivada.");
        }

        // Verificar si hay concursantes asignados a esta jornada
        Long concursantesCount = entityManager.createQuery(
            "SELECT COUNT(c) FROM Concursante c WHERE c.jornada.id = :jornadaId", Long.class)
            .setParameter("jornadaId", id)
            .getSingleResult();
        
        if (concursantesCount > 0) {
            throw new IllegalArgumentException("No se puede eliminar la jornada porque tiene " + 
                concursantesCount + " concursante(s) asignado(s). Desasigna los concursantes primero.");
        }

        // UNDO: capturar todo lo que este borrado destruye o modifica, ANTES de tocarlo:
        // la fila de la jornada (se reinsertará con el mismo id), sus relaciones con
        // cuestionarios/combos, su historial y los estados que se van a liberar
        List<Map<String, Object>> accionesUndo = new ArrayList<>();
        Map<String, Object> filaJornada = undoService.snapshotFila("jornadas", id);
        if (filaJornada != null) {
            accionesUndo.add(UndoService.accionInsertarFila("jornadas", filaJornada));
        }
        for (Map<String, Object> fila : undoService.snapshotFilas("jornadas_cuestionarios", "jornada_id", id)) {
            accionesUndo.add(UndoService.accionInsertarFila("jornadas_cuestionarios", fila));
        }
        for (Map<String, Object> fila : undoService.snapshotFilas("jornadas_combos", "jornada_id", id)) {
            accionesUndo.add(UndoService.accionInsertarFila("jornadas_combos", fila));
        }
        for (Map<String, Object> fila : undoService.snapshotFilas("historial_jornadas", "jornada_id", id)) {
            accionesUndo.add(UndoService.accionInsertarFila("historial_jornadas", fila));
        }
        if (jornada.getCuestionarios() != null) {
            for (Cuestionario c : jornada.getCuestionarios()) {
                accionesUndo.add(UndoService.accionActualizarCampos("cuestionarios", c.getId(),
                    Collections.singletonMap("estado", c.getEstado() != null ? c.getEstado().name() : null)));
            }
        }
        if (jornada.getCombos() != null) {
            for (Combo cb : jornada.getCombos()) {
                accionesUndo.add(UndoService.accionActualizarCampos("combos", cb.getId(),
                    Collections.singletonMap("estado", cb.getEstado() != null ? cb.getEstado().name() : null)));
            }
        }

        // Liberar todos los cuestionarios asignados a esta jornada
        if (jornada.getCuestionarios() != null) {
            for (Cuestionario cuestionario : jornada.getCuestionarios()) {
                if (cuestionario.getEstado() == Cuestionario.EstadoCuestionario.adjudicado) {
                    cuestionario.setEstado(Cuestionario.EstadoCuestionario.aprobado);
                    cuestionarioRepository.save(cuestionario);
                }
            }
        }

        // Liberar todos los combos asignados a esta jornada
        if (jornada.getCombos() != null) {
            for (Combo combo : jornada.getCombos()) {
                if (combo.getEstado() == Combo.EstadoCombo.adjudicado) {
                    combo.setEstado(Combo.EstadoCombo.aprobado);
                    comboRepository.save(combo);
                }
            }
        }

        // Eliminar historial asociado a la jornada para evitar violación de FK
        try {
            entityManager.createNativeQuery("DELETE FROM historial_jornadas WHERE jornada_id = :jornadaId")
                .setParameter("jornadaId", id)
                .executeUpdate();
        } catch (Exception e) {
            // No bloquear la eliminación por errores en limpieza de historial; se reportará abajo si falla el delete principal
        }

        jornadaRepository.delete(jornada);

        // UNDO: registrar el borrado como operación deshacible (reinserta con el mismo id)
        undoService.registrar("eliminar_jornada",
            "Eliminar jornada '" + jornada.getNombre() + "' (" + id + ")", accionesUndo);
    }

    public JornadaDTO cambiarEstado(Long id, String nuevoEstado) {
        Jornada jornada = jornadaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Jornada no encontrada"));

        log.debug("🔄 [JORNADA ESTADO] Solicitud de cambio de estado - Jornada " + id + " -> " + nuevoEstado);
        final Jornada.EstadoJornada estado;
        try {
            estado = Jornada.EstadoJornada.valueOf(nuevoEstado);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado no válido: " + nuevoEstado);
        }

        if (estado == Jornada.EstadoJornada.en_grabacion) {
            validarMultiplicadoresCombosParaGrabacion(jornada);
        } else if (estado == Jornada.EstadoJornada.completada) {
            validarGrabadosORecicladosParaCompletar(jornada);
        }

        // UNDO: capturar estados previos de la jornada y de todos sus elementos
        // ANTES de la cascada, para poder revertirla exactamente
        Jornada.EstadoJornada estadoAnterior = jornada.getEstado();
        List<Map<String, Object>> accionesUndo = new ArrayList<>();
        accionesUndo.add(UndoService.accionActualizarCampos("jornadas", id,
            Collections.singletonMap("estado", estadoAnterior != null ? estadoAnterior.name() : null)));
        if (jornada.getCuestionarios() != null) {
            for (Cuestionario c : jornada.getCuestionarios()) {
                accionesUndo.add(UndoService.accionActualizarCampos("cuestionarios", c.getId(),
                    Collections.singletonMap("estado", c.getEstado() != null ? c.getEstado().name() : null)));
            }
        }
        if (jornada.getCombos() != null) {
            for (Combo cb : jornada.getCombos()) {
                accionesUndo.add(UndoService.accionActualizarCampos("combos", cb.getId(),
                    Collections.singletonMap("estado", cb.getEstado() != null ? cb.getEstado().name() : null)));
            }
        }

        jornada.setEstado(estado);
        jornada = jornadaRepository.save(jornada);
        log.debug("✅ [JORNADA ESTADO] Jornada " + id + " guardada con estado " + estado);

        // Archivada → grabado. Lista/preparación → adjudicado.
        // En grabación y completada no pisan el estado de cuestionarios/combos.
        if (estado == Jornada.EstadoJornada.archivada) {
                int totalC = 0, totalCmb = 0;
                if (jornada.getCuestionarios() != null) {
                    log.debug("ℹ️ [JORNADA ESTADO] (GRABADA) Cuestionarios asignados a jornada " + id + ": " + jornada.getCuestionarios().size());
                    for (Cuestionario c : jornada.getCuestionarios()) {
                        log.debug("   • Cuestionario " + c.getId() + " estado actual: " + c.getEstado());
                        if (c.getEstado() != Cuestionario.EstadoCuestionario.grabado) {
                            c.setEstado(Cuestionario.EstadoCuestionario.grabado);
                            totalC++;
                            log.debug("   → Cuestionario " + c.getId() + " marcado como GRABADO");
                        }
                        cuestionarioRepository.save(c);
                    }
                }
                if (jornada.getCombos() != null) {
                    log.debug("ℹ️ [JORNADA ESTADO] (GRABADA) Combos asignados a jornada " + id + ": " + jornada.getCombos().size());
                    for (Combo combo : jornada.getCombos()) {
                        log.debug("   • Combo " + combo.getId() + " estado actual: " + combo.getEstado());
                        if (combo.getEstado() != Combo.EstadoCombo.grabado) {
                            combo.setEstado(Combo.EstadoCombo.grabado);
                            totalCmb++;
                            log.debug("   → Combo " + combo.getId() + " marcado como GRABADO");
                        }
                        comboRepository.save(combo);
                    }
                }
                try { entityManager.flush(); } catch (Exception ignored) {}
                try {
                    int updatedC = entityManager.createNativeQuery(
                        "UPDATE cuestionarios SET estado='grabado' WHERE id IN (SELECT cuestionario_id FROM jornadas_cuestionarios WHERE jornada_id = ?) AND estado <> 'grabado'")
                        .setParameter(1, jornada.getId())
                        .executeUpdate();
                    int updatedCb = entityManager.createNativeQuery(
                        "UPDATE combos SET estado='grabado' WHERE id IN (SELECT combo_id FROM jornadas_combos WHERE jornada_id = ?) AND estado <> 'grabado'")
                        .setParameter(1, jornada.getId())
                        .executeUpdate();
                    log.debug("🟢 [JORNADA ESTADO] Batch grabado → cuestionarios: " + updatedC + ", combos: " + updatedCb);
                } catch (Exception e) {
                    log.warn("⚠️ [JORNADA ESTADO] Error batch (grabado): " + e.getMessage());
                }
            } else if (estado == Jornada.EstadoJornada.lista
                    || estado == Jornada.EstadoJornada.preparacion) {
                int totalC = 0, totalCmb = 0;
                if (jornada.getCuestionarios() != null) {
                    log.debug("ℹ️ [JORNADA ESTADO] (NO GRABADA) Cuestionarios asignados a jornada " + id + ": " + jornada.getCuestionarios().size());
                    for (Cuestionario c : jornada.getCuestionarios()) {
                        if (c.getEstado() != Cuestionario.EstadoCuestionario.adjudicado) {
                            c.setEstado(Cuestionario.EstadoCuestionario.adjudicado);
                            totalC++;
                            log.debug("   → Cuestionario " + c.getId() + " marcado como ADJUDICADO");
                        }
                        cuestionarioRepository.save(c);
                    }
                }
                if (jornada.getCombos() != null) {
                    log.debug("ℹ️ [JORNADA ESTADO] (NO GRABADA) Combos asignados a jornada " + id + ": " + jornada.getCombos().size());
                    for (Combo combo : jornada.getCombos()) {
                        if (combo.getEstado() != Combo.EstadoCombo.adjudicado) {
                            combo.setEstado(Combo.EstadoCombo.adjudicado);
                            totalCmb++;
                            log.debug("   → Combo " + combo.getId() + " marcado como ADJUDICADO");
                        }
                        comboRepository.save(combo);
                    }
                }
                try { entityManager.flush(); } catch (Exception ignored) {}
                try {
                    int updatedC = entityManager.createNativeQuery(
                        "UPDATE cuestionarios SET estado='adjudicado' WHERE id IN (SELECT cuestionario_id FROM jornadas_cuestionarios WHERE jornada_id = ?) AND estado <> 'adjudicado'")
                        .setParameter(1, jornada.getId())
                        .executeUpdate();
                    int updatedCb = entityManager.createNativeQuery(
                        "UPDATE combos SET estado='adjudicado' WHERE id IN (SELECT combo_id FROM jornadas_combos WHERE jornada_id = ?) AND estado <> 'adjudicado'")
                        .setParameter(1, jornada.getId())
                        .executeUpdate();
                    log.debug("🟡 [JORNADA ESTADO] Batch adjudicado → cuestionarios: " + updatedC + ", combos: " + updatedCb);
                } catch (Exception e) {
                    log.warn("⚠️ [JORNADA ESTADO] Error batch (adjudicado): " + e.getMessage());
                }
            }

        if (estadoAnterior == null || !estadoAnterior.name().equals(nuevoEstado)) {
            undoService.registrar("cambiar_estado_jornada",
                "Estado de jornada '" + jornada.getNombre() + "' (" +
                    (estadoAnterior != null ? estadoAnterior.name() : "?") + " → " + nuevoEstado + ")",
                accionesUndo);
        }

        return convertirADTO(jornada);
    }

    /**
     * Exporta una jornada a formato Excel con opciones de personalización.
     *
     * @param id El ID de la jornada a exportar
     * @return Los bytes del archivo Excel generado
     */
    public byte[] exportarExcel(Long id) {
        return exportarExcel(id, null);
    }

    /**
     * Exporta una jornada a formato Excel con opciones de personalización.
     *
     * @param id El ID de la jornada a exportar
     * @param opciones Mapa con opciones de configuración para el Excel
     * @return Los bytes del archivo Excel generado
     */
    public byte[] exportarExcel(Long id, Map<String, Object> opciones) {
        try {
            Jornada jornada = jornadaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Jornada no encontrada"));

            Map<String, Object> opcionesExport = opciones == null ? new HashMap<>() : new HashMap<>(opciones);
            opcionesExport.put(ExcelExportService.OPCION_ANCESTROS_COMBO, ancestrosCombosDeJornada(jornada));
            return excelExportService.exportarJornada(jornada, opcionesExport);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar Excel: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> obtenerCuestionariosDisponibles() {
        List<Cuestionario> cuestionarios = cuestionarioRepository.findByEstado(Cuestionario.EstadoCuestionario.aprobado);
        return cuestionarios.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("nivel", c.getNivel().name());
            map.put("estado", c.getEstado().name());
            map.put("tematica", c.getTematica());
            map.put("notasDireccion", c.getNotasDireccion());
            map.put("totalPreguntas", c.getPreguntas() != null ? c.getPreguntas().size() : 0);
            map.put("fechaCreacion", c.getFechaCreacion());
            return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> obtenerCombosDisponibles() {
        List<Combo> combos = comboRepository.findByEstado(Combo.EstadoCombo.aprobado);
        return combos.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("nivel", c.getNivel() != null ? c.getNivel().name() : null);
            map.put("estado", c.getEstado() != null ? c.getEstado().name() : null);
            map.put("tipo", c.getTipo() != null ? c.getTipo().name() : null);
            map.put("tematica", c.getTematica());
            map.put("totalPreguntas", c.getPreguntas() != null ? c.getPreguntas().size() : 0);
            map.put("fechaCreacion", c.getFechaCreacion());
            return map;
        }).collect(Collectors.toList());
    }

    private JornadaDTO convertirADTO(Jornada jornada) {
        JornadaDTO dto = new JornadaDTO();
        dto.setId(jornada.getId());
        dto.setVersion(jornada.getVersion());
        dto.setNombre(jornada.getNombre());
        dto.setFechaJornada(jornada.getFechaJornada());
        dto.setLugar(jornada.getLugar());
        dto.setEstado(jornada.getEstado().name());
        dto.setCreacionUsuarioId(jornada.getCreacionUsuario().getId());
        dto.setCreacionUsuarioNombre(jornada.getCreacionUsuario().getNombre());
        dto.setFechaCreacion(jornada.getFechaCreacion());
        dto.setNotas(jornada.getNotas());

        List<Long> cuestionarioIds = new ArrayList<>(Collections.nCopies(SlotsJornada.TOTAL, null));
        List<JornadaDTO.CuestionarioResumenDTO> cuestionarios = new ArrayList<>(Collections.nCopies(SlotsJornada.TOTAL, null));
        List<Cuestionario> cuestionariosPorSlot = jornada.getCuestionariosPorSlot();
        for (int i = 0; i < SlotsJornada.TOTAL; i++) {
            Cuestionario c = cuestionariosPorSlot.get(i);
            if (c == null) {
                continue;
            }
            cuestionarioIds.set(i, c.getId());
            JornadaDTO.CuestionarioResumenDTO resumen = new JornadaDTO.CuestionarioResumenDTO();
            resumen.setId(c.getId());
            resumen.setNivel(c.getNivel() != null ? c.getNivel().name() : null);
            resumen.setEstado(c.getEstado() != null ? c.getEstado().name() : null);
            resumen.setTematica(c.getTematica());
            resumen.setNotasDireccion(c.getNotasDireccion());
            resumen.setTotalPreguntas(c.getPreguntas() != null ? c.getPreguntas().size() : 0);
            resumen.setReutilizado(esReutilizado(jornada.getId(), "cuestionario_id", c.getId()));
            resumen.setAsignadoAConcursante(estaAsignadoAConcursante(c));
            cuestionarios.set(i, resumen);
        }
        dto.setCuestionarioIds(cuestionarioIds);
        dto.setCuestionarios(cuestionarios);

        List<Long> comboIds = new ArrayList<>(Collections.nCopies(SlotsJornada.TOTAL, null));
        List<JornadaDTO.ComboResumenDTO> combos = new ArrayList<>(Collections.nCopies(SlotsJornada.TOTAL, null));
        List<Combo> combosPorSlot = jornada.getCombosPorSlot();
        for (int i = 0; i < SlotsJornada.TOTAL; i++) {
            Combo c = combosPorSlot.get(i);
            if (c == null) {
                continue;
            }
            comboIds.set(i, c.getId());
            JornadaDTO.ComboResumenDTO resumen = new JornadaDTO.ComboResumenDTO();
            resumen.setId(c.getId());
            resumen.setNivel(c.getNivel() != null ? c.getNivel().name() : null);
            resumen.setEstado(c.getEstado() != null ? c.getEstado().name() : null);
            resumen.setTipo(c.getTipo() != null ? c.getTipo().name() : null);
            resumen.setTematica(c.getTematica());
            resumen.setNotasDireccion(c.getNotasDireccion());
            resumen.setTotalPreguntas(c.getPreguntas() != null ? c.getPreguntas().size() : 0);
            resumen.setReutilizado(esReutilizado(jornada.getId(), "combo_id", c.getId()));
            resumen.setAsignadoAConcursante(estaAsignadoAConcursante(c));
            resumen.setMultiplicadorMaximo(multiplicadorMasAlto(c));
            resumen.setPreguntaUsadaId(c.getPreguntaUsadaId());
            combos.set(i, resumen);
        }
        dto.setComboIds(comboIds);
        dto.setCombos(combos);

        return dto;
    }

    private void validarMultiplicadoresCombosParaGrabacion(Jornada jornada) {
        List<String> pendientes = new ArrayList<>();
        for (Combo combo : jornada.getCombosPorSlot()) {
            if (combo == null) {
                continue;
            }
            Set<PreguntaCombo> preguntas = combo.getPreguntas();
            if (preguntas == null || preguntas.isEmpty()) {
                pendientes.add("combo " + combo.getId());
                continue;
            }
            boolean todosConNumero = true;
            for (PreguntaCombo pc : preguntas) {
                if (!factorTieneNumero(pc.getFactorMultiplicacion())) {
                    todosConNumero = false;
                    break;
                }
            }
            if (!todosConNumero) {
                pendientes.add("combo " + combo.getId());
            }
        }
        if (!pendientes.isEmpty()) {
            throw new IllegalArgumentException(
                "Para pasar a En Grabación todos los multiplicadores de cada combo deben tener un número. No vale solo X. Pendientes: "
                    + String.join(", ", pendientes) + ".");
        }
    }

    private void validarGrabadosORecicladosParaCompletar(Jornada jornada) {
        List<String> pendientes = new ArrayList<>();
        for (Cuestionario c : jornada.getCuestionariosPorSlot()) {
            if (c == null) {
                continue;
            }
            boolean grabado = c.getEstado() == Cuestionario.EstadoCuestionario.grabado;
            boolean reciclado = esReutilizado(jornada.getId(), "cuestionario_id", c.getId());
            if (!grabado && !reciclado) {
                pendientes.add("cuestionario " + c.getId());
            }
        }
        for (Combo c : jornada.getCombosPorSlot()) {
            if (c == null) {
                continue;
            }
            boolean grabado = c.getEstado() == Combo.EstadoCombo.grabado;
            boolean reciclado = c.getEstado() == Combo.EstadoCombo.reaprovechado
                || esReutilizado(jornada.getId(), "combo_id", c.getId());
            if (!grabado && !reciclado) {
                pendientes.add("combo " + c.getId());
            }
        }
        if (!pendientes.isEmpty()) {
            throw new IllegalArgumentException(
                "Para pasar a Completada todos los cuestionarios y combos deben estar grabados o reciclados. Pendientes: "
                    + String.join(", ", pendientes) + ".");
        }
    }

    private String multiplicadorMasAlto(Combo combo) {
        if (combo == null || combo.getPreguntas() == null || combo.getPreguntas().isEmpty()) {
            return null;
        }
        String mejor = null;
        int mejorRango = -1;
        for (PreguntaCombo pc : combo.getPreguntas()) {
            String factor = pc.getFactorMultiplicacion();
            if (!factorRelleno(factor)) {
                continue;
            }
            int rango = rangoFactor(factor);
            if (rango > mejorRango) {
                mejorRango = rango;
                mejor = normalizarFactor(factor);
            }
        }
        return mejor;
    }

    private static boolean factorRelleno(String factor) {
        return factor != null && !factor.trim().isEmpty();
    }

    private static boolean factorTieneNumero(String factor) {
        return factorRelleno(factor) && factor.replaceAll("\\D+", "").length() > 0;
    }

    private static int rangoFactor(String factor) {
        String digitos = factor.replaceAll("\\D+", "");
        if (digitos.isEmpty()) {
            return 100;
        }
        try {
            return Integer.parseInt(digitos);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String normalizarFactor(String factor) {
        String limpio = factor.trim().toUpperCase();
        String digitos = limpio.replaceAll("\\D+", "");
        if (digitos.isEmpty()) {
            return "X";
        }
        return "X" + digitos;
    }

    private boolean esReutilizado(Long jornadaId, String columna, Long elementoId) {
        if (jornadaId == null || elementoId == null) {
            return false;
        }
        try {
            Object count = entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM historial_jornadas WHERE jornada_id = :jid AND " + columna + " = :cid AND estado_asignacion = 'reaprovechado'")
                    .setParameter("jid", jornadaId)
                    .setParameter("cid", elementoId)
                    .getSingleResult();
            return count instanceof Number && ((Number) count).longValue() > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private Set<Long> idsDe(Set<?> entidades) {
        Set<Long> ids = new HashSet<>();
        if (entidades == null) {
            return ids;
        }
        for (Object e : entidades) {
            if (e instanceof Cuestionario) {
                ids.add(((Cuestionario) e).getId());
            } else if (e instanceof Combo) {
                ids.add(((Combo) e).getId());
            }
        }
        return ids;
    }

    private boolean estaAsignadoAConcursante(Cuestionario cuestionario) {
        if (cuestionario == null || cuestionario.getId() == null) {
            return false;
        }
        return cuestionario.getEstado() == Cuestionario.EstadoCuestionario.grabado
            || concursanteRepository.existsByCuestionario_Id(cuestionario.getId());
    }

    private boolean estaAsignadoAConcursante(Combo combo) {
        if (combo == null || combo.getId() == null) {
            return false;
        }
        return combo.getEstado() == Combo.EstadoCombo.grabado
            || concursanteRepository.existsByCombo_Id(combo.getId());
    }

    private void validarCuestionariosNoQuitables(Set<Cuestionario> actuales, Set<Long> nuevosIds) {
        if (actuales == null) {
            return;
        }
        for (Cuestionario cuestionario : actuales) {
            if (cuestionario == null || cuestionario.getId() == null || nuevosIds.contains(cuestionario.getId())) {
                continue;
            }
            if (estaAsignadoAConcursante(cuestionario)) {
                throw new IllegalArgumentException(
                    "No se puede quitar el cuestionario " + cuestionario.getId()
                        + " de la jornada porque está asignado a un concursante.");
            }
        }
    }

    private void validarCombosNoQuitables(Set<Combo> actuales, Set<Long> nuevosIds) {
        if (actuales == null) {
            return;
        }
        for (Combo combo : actuales) {
            if (combo == null || combo.getId() == null || nuevosIds.contains(combo.getId())) {
                continue;
            }
            if (estaAsignadoAConcursante(combo)) {
                throw new IllegalArgumentException(
                    "No se puede quitar el combo " + combo.getId()
                        + " de la jornada porque está asignado a un concursante.");
            }
        }
    }

    private void liberarCuestionariosQuitados(Set<Cuestionario> actuales, Set<Long> nuevosIds) {
        if (actuales == null) {
            return;
        }
        for (Cuestionario cuestionarioActual : actuales) {
            if (cuestionarioActual.getId() != null && !nuevosIds.contains(cuestionarioActual.getId())
                    && cuestionarioActual.getEstado() == Cuestionario.EstadoCuestionario.adjudicado) {
                cuestionarioActual.setEstado(Cuestionario.EstadoCuestionario.aprobado);
                cuestionarioRepository.save(cuestionarioActual);
            }
        }
    }

    private void liberarCombosQuitados(Set<Combo> actuales, Set<Long> nuevosIds) {
        if (actuales == null) {
            return;
        }
        for (Combo comboActual : actuales) {
            if (comboActual.getId() != null && !nuevosIds.contains(comboActual.getId())
                    && comboActual.getEstado() == Combo.EstadoCombo.adjudicado) {
                comboActual.setEstado(Combo.EstadoCombo.aprobado);
                comboRepository.save(comboActual);
            }
        }
    }

    private List<Cuestionario> cargarCuestionariosEnSlots(List<Long> ids, Set<Long> yaAsignados) {
        List<Long> slots = SlotsJornada.normalizarIds(ids);
        List<Cuestionario> porSlot = new ArrayList<>(Collections.nCopies(SlotsJornada.TOTAL, null));
        for (int i = 0; i < slots.size(); i++) {
            Long cuestionarioId = slots.get(i);
            if (cuestionarioId == null) {
                continue;
            }
            if (yaAsignados == null || !yaAsignados.contains(cuestionarioId)) {
                try {
                    boolean exito = cuestionarioService.cambiarEstadoAtomico(
                            cuestionarioId,
                            Cuestionario.EstadoCuestionario.aprobado,
                            Cuestionario.EstadoCuestionario.adjudicado);
                    if (!exito) {
                        throw new IllegalStateException("El cuestionario " + cuestionarioId + " fue modificado por otro usuario. Por favor, recarga e intenta nuevamente.");
                    }
                } catch (IllegalStateException e) {
                    throw new IllegalArgumentException("Error de concurrencia al asignar cuestionario " + cuestionarioId + ": " + e.getMessage());
                }
            }
            Cuestionario cuestionario = cuestionarioRepository.findById(cuestionarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Cuestionario no encontrado: " + cuestionarioId));
            porSlot.set(i, cuestionario);
        }
        return porSlot;
    }

    private List<Combo> cargarCombosEnSlots(List<Long> ids, Set<Long> yaAsignados) {
        List<Long> slots = SlotsJornada.normalizarIds(ids);
        List<Combo> porSlot = new ArrayList<>(Collections.nCopies(SlotsJornada.TOTAL, null));
        for (int i = 0; i < slots.size(); i++) {
            Long comboId = slots.get(i);
            if (comboId == null) {
                continue;
            }
            if (yaAsignados == null || !yaAsignados.contains(comboId)) {
                try {
                    boolean exito = comboService.cambiarEstadoAtomico(
                            comboId,
                            Combo.EstadoCombo.aprobado,
                            Combo.EstadoCombo.adjudicado);
                    if (!exito) {
                        throw new IllegalStateException("El combo " + comboId + " fue modificado por otro usuario. Por favor, recarga e intenta nuevamente.");
                    }
                } catch (IllegalStateException e) {
                    throw new IllegalArgumentException("Error de concurrencia al asignar combo " + comboId + ": " + e.getMessage());
                }
            }
            Combo combo = comboRepository.findById(comboId)
                    .orElseThrow(() -> new IllegalArgumentException("Combo no encontrado: " + comboId));
            porSlot.set(i, combo);
        }
        return porSlot;
    }

    /**
     * Reutiliza un cuestionario de una jornada, liberándolo para uso en otras jornadas.
     * 
     * @param jornadaId ID de la jornada
     * @param cuestionarioId ID del cuestionario a reutilizar
     * @param usuarioId ID del usuario que realiza la acción
     */
    public void reutilizarCuestionario(Long jornadaId, Long cuestionarioId, Long usuarioId) {
        // Verificar que la jornada existe
        Jornada jornada = jornadaRepository.findById(jornadaId)
            .orElseThrow(() -> new IllegalArgumentException("Jornada no encontrada con ID: " + jornadaId));
        
        // Verificar que el cuestionario existe y está asignado a esta jornada
        Cuestionario cuestionario = cuestionarioRepository.findById(cuestionarioId)
            .orElseThrow(() -> new IllegalArgumentException("Cuestionario no encontrado con ID: " + cuestionarioId));
        
        if (jornada.getCuestionarios() == null || !jornada.getCuestionarios().contains(cuestionario)) {
            throw new IllegalArgumentException("El cuestionario " + cuestionarioId + " no está asignado a la jornada " + jornadaId);
        }
        
        // Solo se reciclan cuestionarios adjudicados. Grabado = ya colocado en un concursante.
        log.debug("========================================");
        log.debug("[REUTILIZAR-CUEST] INICIO - Cuestionario " + cuestionarioId + " de jornada " + jornadaId);
        
        try {
            Cuestionario.EstadoCuestionario estadoActual = cuestionario.getEstado();
            log.debug("[REUTILIZAR-CUEST] Estado actual: " + estadoActual);

            if (estadoActual == Cuestionario.EstadoCuestionario.grabado
                    || concursanteRepository.existsByCuestionario_Id(cuestionarioId)) {
                throw new IllegalArgumentException(
                    "No se puede reciclar el cuestionario " + cuestionarioId
                        + " porque está grabado (asignado a un concursante).");
            }
            
            if (estadoActual == Cuestionario.EstadoCuestionario.adjudicado) {
                log.debug("[REUTILIZAR-CUEST] Cambiando estado: " + estadoActual + " -> aprobado");
                boolean exito = cuestionarioService.cambiarEstadoAtomico(
                    cuestionarioId,
                    estadoActual,
                    Cuestionario.EstadoCuestionario.aprobado
                );
                if (!exito) {
                    log.warn("[REUTILIZAR-CUEST] ERROR: No se pudo cambiar el estado");
                    throw new IllegalStateException("El cuestionario " + cuestionarioId + " fue modificado por otro usuario. Recarga e intenta de nuevo.");
                }
                // refrescar entidad
                cuestionario = cuestionarioRepository.findById(cuestionarioId)
                    .orElse(cuestionario);
                log.debug("[REUTILIZAR-CUEST] EXITO: Estado cambiado a " + cuestionario.getEstado());
            } else if (estadoActual == Cuestionario.EstadoCuestionario.aprobado) {
                log.debug("[REUTILIZAR-CUEST] Ya estaba en estado aprobado");
            } else {
                log.debug("[REUTILIZAR-CUEST] ERROR: Estado invalido " + estadoActual);
                throw new IllegalArgumentException("El cuestionario " + cuestionarioId + " está en estado " + estadoActual + ". Solo se pueden reutilizar cuestionarios en estado 'adjudicado'.");
            }
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("Error de concurrencia al reutilizar cuestionario " + cuestionarioId + ": " + e.getMessage());
        }

        // Registrar reutilización (sin quitar de la jornada)
        log.debug("[REUTILIZAR-CUEST] Registrando en historial...");
        try {
            Long count = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM historial_jornadas WHERE jornada_id = :jid AND cuestionario_id = :cid AND estado_asignacion = 'reaprovechado'")
                .setParameter("jid", jornadaId)
                .setParameter("cid", cuestionarioId)
                .getSingleResult() instanceof Number ? ((Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM historial_jornadas WHERE jornada_id = :jid AND cuestionario_id = :cid AND estado_asignacion = 'reaprovechado'")
                    .setParameter("jid", jornadaId)
                    .setParameter("cid", cuestionarioId)
                    .getSingleResult()).longValue() : 0L;
            if (count == null || count == 0L) {
                registrarHistorialReutilizacion(jornada, cuestionario, "cuestionario", usuarioId);
                log.debug("[REUTILIZAR-CUEST] Registro de historial creado");
            } else {
                log.debug("[REUTILIZAR-CUEST] Ya existia registro en historial");
            }
        } catch (Exception e) {
            registrarHistorialReutilizacion(jornada, cuestionario, "cuestionario", usuarioId);
            log.debug("[REUTILIZAR-CUEST] Registro de historial creado (catch)");
        }
        
        log.debug("[REUTILIZAR-CUEST] COMPLETADO - Cuestionario " + cuestionarioId);
        log.debug("========================================");
    }

    /**
     * Reutiliza un combo de una jornada, liberándolo para uso en otras jornadas.
     * 
     * @param jornadaId ID de la jornada
     * @param comboId ID del combo a reutilizar
     * @param usuarioId ID del usuario que realiza la acción
     */
    public void reutilizarCombo(Long jornadaId, Long comboId, Long usuarioId) {
        // Verificar que la jornada existe
        Jornada jornada = jornadaRepository.findById(jornadaId)
            .orElseThrow(() -> new IllegalArgumentException("Jornada no encontrada con ID: " + jornadaId));
        
        // Verificar que el combo existe y está asignado a esta jornada
        Combo combo = comboRepository.findById(comboId)
            .orElseThrow(() -> new IllegalArgumentException("Combo no encontrado con ID: " + comboId));
        
        if (jornada.getCombos() == null || !jornada.getCombos().contains(combo)) {
            throw new IllegalArgumentException("El combo " + comboId + " no está asignado a la jornada " + jornadaId);
        }
        
        // Ponerlo disponible: pasar de adjudicado/grabado -> aprobado (sin quitar de esta jornada)
        try {
            Combo.EstadoCombo estadoActual = combo.getEstado();
            log.debug("🔄🔄🔄 [REUTILIZAR COMBO] Combo " + comboId + " | Estado actual: " + estadoActual + " | Jornada: " + jornadaId);
            
            if (estadoActual == Combo.EstadoCombo.adjudicado || estadoActual == Combo.EstadoCombo.grabado) {
                log.debug("🔄 [REUTILIZAR COMBO] Cambiando estado: " + estadoActual + " -> aprobado");
                boolean exito = comboService.cambiarEstadoAtomico(
                    comboId,
                    estadoActual,
                    Combo.EstadoCombo.aprobado
                );
                if (!exito) {
                    log.warn("❌ [REUTILIZAR COMBO] No se pudo cambiar el estado del combo " + comboId);
                    throw new IllegalStateException("El combo " + comboId + " fue modificado por otro usuario. Recarga e intenta de nuevo.");
                }
                combo = comboRepository.findById(comboId).orElse(combo);
                log.debug("✅✅✅ [REUTILIZAR COMBO] Combo " + comboId + " cambiado: " + estadoActual + " -> " + combo.getEstado());
            } else if (estadoActual == Combo.EstadoCombo.aprobado) {
                log.debug("✅ [REUTILIZAR COMBO] Combo " + comboId + " ya está en estado aprobado");
            } else {
                log.debug("⚠️ [REUTILIZAR COMBO] Combo " + comboId + " está en estado " + estadoActual + ", no se puede reutilizar");
                throw new IllegalArgumentException("El combo " + comboId + " está en estado " + estadoActual + ". Solo se pueden reutilizar combos en estado 'adjudicado' o 'grabado'.");
            }
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("Error de concurrencia al reutilizar combo " + comboId + ": " + e.getMessage());
        }

        // Registrar reutilización (sin quitarlo de la jornada)
        try {
            Long count = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM historial_jornadas WHERE jornada_id = :jid AND combo_id = :cid AND estado_asignacion = 'reaprovechado'")
                .setParameter("jid", jornadaId)
                .setParameter("cid", comboId)
                .getSingleResult() instanceof Number ? ((Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM historial_jornadas WHERE jornada_id = :jid AND combo_id = :cid AND estado_asignacion = 'reaprovechado'")
                    .setParameter("jid", jornadaId)
                    .setParameter("cid", comboId)
                    .getSingleResult()).longValue() : 0L;
            if (count == null || count == 0L) {
                registrarHistorialReutilizacion(jornada, combo, "combo", usuarioId);
            }
        } catch (Exception e) {
            registrarHistorialReutilizacion(jornada, combo, "combo", usuarioId);
        }
        
        log.debug("♻️♻️♻️ [REUTILIZAR COMBO] Combo " + comboId + " reutilizado de jornada " + jornadaId);
    }

    public void quitarReutilizacionCuestionario(Long jornadaId, Long cuestionarioId) {
        // Si el cuestionario está asignado a otra jornada distinta, impedirlo
        Long countOtras = entityManager.createQuery(
            "SELECT COUNT(a) FROM JornadaCuestionarioAsignacion a WHERE a.cuestionario.id = :cid AND a.jornada.id <> :jid", Long.class)
            .setParameter("cid", cuestionarioId)
            .setParameter("jid", jornadaId)
            .getSingleResult();
        if (countOtras != null && countOtras > 0) {
            // Obtener alguna jornada para informar
            Long otraId = entityManager.createQuery(
                "SELECT a.jornada.id FROM JornadaCuestionarioAsignacion a WHERE a.cuestionario.id = :cid AND a.jornada.id <> :jid", Long.class)
                .setParameter("cid", cuestionarioId)
                .setParameter("jid", jornadaId)
                .setMaxResults(1)
                .getSingleResult();
            throw new IllegalArgumentException("No se puede quitar la reutilización: el cuestionario está asignado en la jornada " + otraId);
        }
        // Borrar marca de reutilizado en historial
        entityManager.createNativeQuery("DELETE FROM historial_jornadas WHERE jornada_id = :jid AND cuestionario_id = :cid AND estado_asignacion = 'reaprovechado'")
            .setParameter("jid", jornadaId)
            .setParameter("cid", cuestionarioId)
            .executeUpdate();
    }

    public void quitarReutilizacionCombo(Long jornadaId, Long comboId) {
        Long countOtras = entityManager.createQuery(
            "SELECT COUNT(a) FROM JornadaComboAsignacion a WHERE a.combo.id = :cid AND a.jornada.id <> :jid", Long.class)
            .setParameter("cid", comboId)
            .setParameter("jid", jornadaId)
            .getSingleResult();
        if (countOtras != null && countOtras > 0) {
            Long otraId = entityManager.createQuery(
                "SELECT a.jornada.id FROM JornadaComboAsignacion a WHERE a.combo.id = :cid AND a.jornada.id <> :jid", Long.class)
                .setParameter("cid", comboId)
                .setParameter("jid", jornadaId)
                .setMaxResults(1)
                .getSingleResult();
            throw new IllegalArgumentException("No se puede quitar la reutilización: el combo está asignado en la jornada " + otraId);
        }
        entityManager.createNativeQuery("DELETE FROM historial_jornadas WHERE jornada_id = :jid AND combo_id = :cid AND estado_asignacion = 'reaprovechado'")
            .setParameter("jid", jornadaId)
            .setParameter("cid", comboId)
            .executeUpdate();
    }

    /**
     * Registra la reutilización en el historial de jornadas.
     */
    private void registrarHistorialReutilizacion(Jornada jornada, Object elemento, String tipo, Long usuarioId) {
        try {
            // Crear entrada en el historial usando las columnas correctas del esquema
            String sql;
            if (tipo.equals("cuestionario")) {
                sql = "INSERT INTO historial_jornadas (jornada_id, cuestionario_id, tipo_asignacion, estado_asignacion, fecha_asignacion, notas) VALUES (?, ?, ?, ?, NOW(), ?)";
                entityManager.createNativeQuery(sql)
                    .setParameter(1, jornada.getId())
                    .setParameter(2, ((Cuestionario) elemento).getId())
                    .setParameter(3, "CUESTIONARIO")
                    .setParameter(4, "reaprovechado")
                    .setParameter(5, "Cuestionario reutilizado - ahora disponible para otras jornadas")
                    .executeUpdate();
            } else {
                sql = "INSERT INTO historial_jornadas (jornada_id, combo_id, tipo_asignacion, estado_asignacion, fecha_asignacion, notas) VALUES (?, ?, ?, ?, NOW(), ?)";
                entityManager.createNativeQuery(sql)
                    .setParameter(1, jornada.getId())
                    .setParameter(2, ((Combo) elemento).getId())
                    .setParameter(3, "COMBO")
                    .setParameter(4, "reaprovechado")
                    .setParameter(5, "Combo reutilizado - ahora disponible para otras jornadas")
                    .executeUpdate();
            }
                
        } catch (Exception e) {
            log.warn("⚠️ [JORNADA] Error al registrar historial de reutilización: " + e.getMessage());
            // No lanzar excepción para no afectar la operación principal
        }
    }

    /**
     * Recicla un combo completamente, marcándolo como liberado.
     * 
     * @param jornadaId ID de la jornada
     * @param comboId ID del combo a reciclar
     * @param usuarioId ID del usuario que realiza la acción
     */
    public void reciclarComboEntero(Long jornadaId, Long comboId, Long usuarioId) {
        // Verificar que la jornada existe
        Combo combo = comboRepository.findById(comboId)
            .orElseThrow(() -> new IllegalArgumentException("Combo no encontrado con ID: " + comboId));
        Jornada jornada = resolverJornadaDueñaDelCombo(jornadaId, combo);
        
        // Verificar que el combo está en estado adjudicado o grabado
        Combo.EstadoCombo estadoActual = combo.getEstado();
        if (estadoActual != Combo.EstadoCombo.adjudicado && estadoActual != Combo.EstadoCombo.grabado) {
            throw new IllegalArgumentException("El combo " + comboId + " no está en estado adjudicado ni grabado. Estado actual: " + estadoActual);
        }

        // Mantener el combo en la jornada pero marcarlo como disponible para nuevas jornadas
        int totalPreguntas = combo.getPreguntas() == null ? 0 : combo.getPreguntas().size();
        if (totalPreguntas != 3) {
            throw new IllegalArgumentException("Solo se pueden reciclar combos con exactamente 3 preguntas");
        }

        log.debug("🔄🔄🔄 [RECICLAR ENTERO] Combo " + comboId + " | Estado actual: " + estadoActual + " | Jornada: " + jornadaId);
        Combo.EstadoCombo estadoAnterior = estadoActual;
        Set<Long> idsHistorialAntes = UndoService.extraerIds(
                undoService.snapshotFilas("historial_jornadas", "combo_id", comboId));
        combo.setEstado(Combo.EstadoCombo.aprobado);
        comboRepository.save(combo);
        log.debug("✅✅✅ [RECICLAR ENTERO] Combo " + comboId + " cambiado: " + estadoActual + " -> aprobado");

        // Registrar en el historial como reaprovechado (para pintarlo en verde en la UI)
        registrarHistorialReutilizacion(jornada, combo, "combo", usuarioId);
        registrarUndoReciclajeEntero(comboId, estadoAnterior.name(), idsHistorialAntes);

        log.debug("♻️♻️♻️ [RECICLAR ENTERO] Combo " + comboId + " reciclado completamente de jornada " + jornadaId);
    }

    /**
     * Recicla un combo parcialmente, creando un nuevo combo con las preguntas no usadas.
     * 
     * @param jornadaId ID de la jornada
     * @param comboId ID del combo a reciclar
     * @param preguntaUsadaId ID de la pregunta que se usó
     * @param usuarioId ID del usuario que realiza la acción
     */
    public ReciclajeComboDTO reciclarComboParcial(Long jornadaId, Long comboId, Long preguntaUsadaId, Long usuarioId) {
        Combo combo = comboRepository.findById(comboId)
            .orElseThrow(() -> new IllegalArgumentException("Combo no encontrado con ID: " + comboId));
        Jornada jornada = resolverJornadaDueñaDelCombo(jornadaId, combo);
        Long jornadaEfectivaId = jornada.getId();
        
        // Verificar que el combo está en estado adjudicado o grabado
        Combo.EstadoCombo estadoActual = combo.getEstado();
        if (estadoActual != Combo.EstadoCombo.adjudicado && estadoActual != Combo.EstadoCombo.grabado) {
            throw new IllegalArgumentException("El combo " + comboId + " no está en estado adjudicado ni grabado. Estado actual: " + estadoActual);
        }
        
        // Verificar que el combo no ha sido reciclado previamente para esta jornada
        try {
            Long count = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM historial_jornadas WHERE jornada_id = :jid AND combo_id = :cid AND estado_asignacion = 'reaprovechado'")
                .setParameter("jid", jornadaEfectivaId)
                .setParameter("cid", comboId)
                .getSingleResult() instanceof Number ? ((Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM historial_jornadas WHERE jornada_id = :jid AND combo_id = :cid AND estado_asignacion = 'reaprovechado'")
                    .setParameter("jid", jornadaEfectivaId)
                    .setParameter("cid", comboId)
                    .getSingleResult()).longValue() : 0L;
            if (count != null && count > 0) {
                throw new IllegalArgumentException("Este combo ya ha sido reciclado previamente para esta jornada. No se puede reciclar el mismo combo varias veces.");
            }
        } catch (IllegalArgumentException e) {
            throw e; // Re-lanzar la excepción de validación
        } catch (Exception e) {
            log.warn("⚠️ [JORNADA] Error al verificar historial de reciclaje: " + e.getMessage());
            // Continuar si hay error en la verificación (no bloquear la operación)
        }
        
        log.debug("🔍 [JORNADA] Reciclando combo " + comboId + " parcialmente. Estado actual: " + estadoActual);
        
        int totalPreguntas = combo.getPreguntas() == null ? 0 : combo.getPreguntas().size();
        if (totalPreguntas != 3) {
            throw new IllegalArgumentException("Solo se pueden reciclar combos con exactamente 3 preguntas");
        }
        
        // Verificar que la pregunta usada existe en el combo
        boolean preguntaEncontrada = false;
        for (PreguntaCombo pc : combo.getPreguntas()) {
            if (pc.getPregunta().getId().equals(preguntaUsadaId)) {
                preguntaEncontrada = true;
                break;
            }
        }
        
        if (!preguntaEncontrada) {
            throw new IllegalArgumentException("La pregunta " + preguntaUsadaId + " no pertenece al combo " + comboId);
        }
        
        // El combo original se mantiene en la jornada con todas sus preguntas.
        // Se crea un combo nuevo con las preguntas no usadas para reutilizarlas en otras jornadas.
        
        // 1) Recopilar las preguntas no usadas y sus factores
        java.util.List<PreguntaCombo> preguntasNoUsadas = new java.util.ArrayList<>();
        for (PreguntaCombo pc : combo.getPreguntas()) {
            if (!pc.getPregunta().getId().equals(preguntaUsadaId)) {
                preguntasNoUsadas.add(pc);
            }
        }
        
        if (preguntasNoUsadas.isEmpty()) {
            throw new IllegalArgumentException("Debe quedar al menos una pregunta sin usar para crear el combo derivado");
        }

        Set<Long> idsHistorialPadreAntes = UndoService.extraerIds(
                undoService.snapshotFilas("historial_jornadas", "combo_id", comboId));
        
        // 2) Crear un combo nuevo con las preguntas no usadas
        Combo comboNuevo = new Combo();
        comboNuevo.setNivel(combo.getNivel());
        comboNuevo.setTipo(combo.getTipo());
        comboNuevo.setTematica(combo.getTematica());
        comboNuevo.setNotasDireccion(combo.getNotasDireccion());
        // Solo tiene las preguntas no usadas: queda incompleto hasta que se complete en Combos.
        comboNuevo.setEstado(Combo.EstadoCombo.borrador);
        comboNuevo.setFechaCreacion(java.time.LocalDateTime.now());
        comboNuevo.setCreacionUsuario(combo.getCreacionUsuario());
        comboNuevo = comboRepository.save(comboNuevo);
        
        // 3) Agregar las preguntas no usadas al combo nuevo
        for (PreguntaCombo pcOriginal : preguntasNoUsadas) {
            // Crear la clave compuesta primero
            PreguntaCombo.PreguntaComboId nuevoId = new PreguntaCombo.PreguntaComboId();
            nuevoId.setComboId(comboNuevo.getId());
            nuevoId.setPreguntaId(pcOriginal.getPregunta().getId());
            
            // Crear la relación PreguntaCombo preservando la posicion original (PM1/PM2/PM3)
            // Si posicion es null (combo legacy), inferirla desde el factor convencional
            Integer posicion = pcOriginal.getPosicion();
            if (posicion == null) {
                String f = pcOriginal.getFactorMultiplicacion();
                if ("2".equals(f)) posicion = 1;
                else if ("3".equals(f)) posicion = 2;
                else posicion = 3; // "0", "1", "X", etc. → PM3
            }
            PreguntaCombo pcNuevo = new PreguntaCombo();
            pcNuevo.setId(nuevoId);
            pcNuevo.setCombo(comboNuevo);
            pcNuevo.setPregunta(pcOriginal.getPregunta());
            pcNuevo.setFactorMultiplicacion(pcOriginal.getFactorMultiplicacion());
            pcNuevo.setPosicion(posicion);
            preguntaComboRepository.save(pcNuevo);
        }
        
        // 4) Guardar en el combo original qué pregunta se usó y mantenerlo en la jornada
        Long preguntaUsadaAnterior = combo.getPreguntaUsadaId();
        combo.setPreguntaUsadaId(preguntaUsadaId);
        log.debug("🔄🔄🔄 [RECICLAR PARCIAL] Manteniendo estado del combo original " + comboId + ": " + estadoActual);
        comboRepository.save(combo);
        
        // 5) Registrar el reciclaje con la pregunta usada para poder auditarlo.
        registrarHistorialReciclajeParcial(jornada, combo, preguntaUsadaId);
        
        // 6) Registrar el combo nuevo en el historial como hijo
        registrarHistorialComboHijo(jornada, comboNuevo, comboId, usuarioId);
        registrarUndoReciclajeParcial(comboId, comboNuevo.getId(), idsHistorialPadreAntes, preguntaUsadaAnterior);

        log.debug("✅✅✅ [RECICLAR PARCIAL] Combo " + comboId + " reciclado parcialmente:");
        log.debug("   - Combo original " + comboId + ": estado=" + combo.getEstado() + ", preguntas=" + totalPreguntas + " (usada=" + preguntaUsadaId + ")");
        log.debug("   - Combo nuevo " + comboNuevo.getId() + ": estado=borrador, preguntas=" + preguntasNoUsadas.size());
        log.debug("♻️♻️♻️ [RECICLAR PARCIAL] Reciclaje parcial completado para combo " + comboId);
        return new ReciclajeComboDTO(jornadaEfectivaId, comboId, comboNuevo.getId(), preguntaUsadaId);
    }

    Jornada resolverJornadaDueñaDelCombo(Long jornadaIdSolicitada, Combo combo) {
        Jornada solicitada = jornadaRepository.findById(jornadaIdSolicitada)
            .orElseThrow(() -> new IllegalArgumentException("Jornada no encontrada con ID: " + jornadaIdSolicitada));
        if (jornadaContieneCombo(solicitada, combo.getId())) {
            return solicitada;
        }
        Jornada jornadaConcursante = buscarJornadaDelConcursanteConCombo(combo.getId());
        if (jornadaConcursante != null && jornadaIdSolicitada.equals(jornadaConcursante.getId())) {
            return solicitada;
        }
        Jornada dueña = buscarJornadaQueContieneCombo(combo.getId());
        if (dueña != null) {
            return dueña;
        }
        if (jornadaConcursante != null) {
            return jornadaConcursante;
        }
        if (concursanteRepository.existsByCombo_Id(combo.getId())) {
            return solicitada;
        }
        throw new IllegalArgumentException("El combo " + combo.getId() + " no está asignado a ninguna jornada");
    }

    private Jornada buscarJornadaDelConcursanteConCombo(Long comboId) {
        return concursanteRepository.findFirstByCombo_Id(comboId)
            .map(Concursante::getJornada)
            .filter(jornada -> jornada != null && jornada.getId() != null)
            .orElse(null);
    }

    public boolean jornadaContieneCombo(Jornada jornada, Long comboId) {
        if (jornada == null || comboId == null) {
            return false;
        }
        for (Combo c : jornada.getCombosPorSlot()) {
            if (c != null && comboId.equals(c.getId())) {
                return true;
            }
        }
        if (jornada.getId() != null) {
            Number count = (Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM jornadas_combos WHERE jornada_id = ? AND combo_id = ?")
                .setParameter(1, jornada.getId())
                .setParameter(2, comboId)
                .getSingleResult();
            return count != null && count.longValue() > 0;
        }
        return false;
    }

    private Jornada buscarJornadaQueContieneCombo(Long comboId) {
        @SuppressWarnings("unchecked")
        List<Object> ids = entityManager.createNativeQuery(
                "SELECT jornada_id FROM jornadas_combos WHERE combo_id = ? ORDER BY jornada_id")
            .setParameter(1, comboId)
            .getResultList();
        for (Object raw : ids) {
            if (raw instanceof Number) {
                Optional<Jornada> encontrada = jornadaRepository.findById(((Number) raw).longValue());
                if (encontrada.isPresent()) {
                    return encontrada.get();
                }
            }
        }
        return null;
    }

    private void registrarHistorialReciclajeParcial(Jornada jornada, Combo comboPadre, Long preguntaUsadaId) {
        entityManager.createNativeQuery(
                "INSERT INTO historial_jornadas (jornada_id, combo_id, tipo_asignacion, estado_asignacion, fecha_asignacion, pregunta_usada_id, notas) "
                    + "VALUES (?, ?, 'COMBO', 'reaprovechado', NOW(), ?, ?)")
            .setParameter(1, jornada.getId())
            .setParameter(2, comboPadre.getId())
            .setParameter(3, preguntaUsadaId)
            .setParameter(4, "RECICLAJE_PARCIAL_COMBO_PADRE:" + comboPadre.getId())
            .executeUpdate();
    }

    /**
     * Registra un combo hijo en el historial.
     */
    private void registrarHistorialComboHijo(Jornada jornada, Combo comboHijo, Long comboPadreId, Long usuarioId) {
        try {
            String sql = "INSERT INTO historial_jornadas (jornada_id, combo_id, tipo_asignacion, estado_asignacion, fecha_asignacion, notas) VALUES (?, ?, ?, ?, NOW(), ?)";
            entityManager.createNativeQuery(sql)
                .setParameter(1, jornada.getId())
                .setParameter(2, comboHijo.getId())
                .setParameter(3, "COMBO")
                .setParameter(4, "asignado")
                .setParameter(5, "RECICLAJE_PARCIAL_COMBO_HIJO;PADRE:" + comboPadreId)
                .executeUpdate();
                
        } catch (Exception e) {
            log.warn("⚠️ [JORNADA] Error al registrar historial de combo hijo: " + e.getMessage());
            // No lanzar excepción para no afectar la operación principal
        }
    }

    /**
     * Elimina un combo derivado que no llegó a estar asignado a un concursante.
     */
    public void cancelarReciclajeCombo(Long jornadaId, Long comboHijoId) {
        Long asignaciones = ((Number) entityManager.createQuery(
                "SELECT COUNT(c) FROM Concursante c WHERE c.combo.id = :comboId")
            .setParameter("comboId", comboHijoId)
            .getSingleResult()).longValue();
        if (asignaciones > 0) {
            throw new IllegalStateException("No se puede cancelar el reciclaje: el combo derivado ya está asignado.");
        }

        List<Map<String, Object>> historialHijo = undoService.snapshotFilas("historial_jornadas", "combo_id", comboHijoId);
        List<Map<String, Object>> historialHijoReciclaje = new ArrayList<>();
        Long comboPadreId = null;
        for (Map<String, Object> fila : historialHijo) {
            if (!mismaJornada(fila, jornadaId)) {
                continue;
            }
            String notas = valorTexto(fila.get("notas"));
            if (notas.contains("RECICLAJE_PARCIAL_COMBO_HIJO")) {
                historialHijoReciclaje.add(fila);
                if (comboPadreId == null) {
                    comboPadreId = extraerComboPadreDesdeNotas(notas);
                }
            }
        }
        if (historialHijoReciclaje.isEmpty()) {
            throw new IllegalArgumentException("El combo no es un derivado reciclado de esta jornada.");
        }

        List<Map<String, Object>> historialPadre = new ArrayList<>();
        if (comboPadreId != null) {
            for (Map<String, Object> fila : undoService.snapshotFilas("historial_jornadas", "combo_id", comboPadreId)) {
                if (mismaJornada(fila, jornadaId)
                        && valorTexto(fila.get("notas")).contains("RECICLAJE_PARCIAL_COMBO_PADRE")) {
                    historialPadre.add(fila);
                }
            }
        }

        List<Map<String, Object>> accionesUndo = new ArrayList<>();
        Map<String, Object> filaCombo = undoService.snapshotFila("combos", comboHijoId);
        if (filaCombo != null) {
            accionesUndo.add(UndoService.accionInsertarFila("combos", filaCombo));
        }
        for (Map<String, Object> fila : undoService.snapshotFilas("combos_preguntas", "combo_id", comboHijoId)) {
            accionesUndo.add(UndoService.accionInsertarFila("combos_preguntas", fila));
        }
        for (Map<String, Object> fila : historialHijoReciclaje) {
            accionesUndo.add(UndoService.accionInsertarFila("historial_jornadas", fila));
        }
        for (Map<String, Object> fila : historialPadre) {
            accionesUndo.add(UndoService.accionInsertarFila("historial_jornadas", fila));
        }

        entityManager.createNativeQuery(
                "DELETE FROM historial_jornadas WHERE jornada_id = ? AND combo_id = ? "
                    + "AND notas LIKE 'RECICLAJE_PARCIAL_COMBO_HIJO;%'")
            .setParameter(1, jornadaId)
            .setParameter(2, comboHijoId)
            .executeUpdate();
        if (comboPadreId != null) {
            entityManager.createNativeQuery(
                    "DELETE FROM historial_jornadas WHERE jornada_id = ? AND combo_id = ? "
                        + "AND notas LIKE 'RECICLAJE_PARCIAL_COMBO_PADRE:%'")
                .setParameter(1, jornadaId)
                .setParameter(2, comboPadreId)
                .executeUpdate();
        }
        comboRepository.deleteById(comboHijoId);
        if (comboPadreId != null) {
            final Long padreId = comboPadreId;
            comboRepository.findById(padreId).ifPresent(padre -> {
                accionesUndo.add(UndoService.accionActualizarCampos("combos", padreId,
                    Collections.singletonMap("pregunta_usada_id", padre.getPreguntaUsadaId())));
                padre.setPreguntaUsadaId(null);
                comboRepository.save(padre);
            });
        }
        undoService.registrar("cancelar_reciclaje_combo",
                "Cancelar reciclaje combo " + comboHijoId, accionesUndo);
    }

    private void registrarUndoReciclajeEntero(Long comboId, String estadoAnterior, Set<Long> idsHistorialAntes) {
        entityManager.flush();
        List<Map<String, Object>> acciones = new ArrayList<>();
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("estado", estadoAnterior);
        acciones.add(UndoService.accionActualizarCampos("combos", comboId, campos));
        for (Map<String, Object> fila : undoService.snapshotFilasNuevas(
                "historial_jornadas", "combo_id", comboId, idsHistorialAntes)) {
            Long id = UndoService.extraerId(fila);
            if (id != null) {
                acciones.add(UndoService.accionEliminarFila("historial_jornadas", id));
            }
        }
        undoService.registrar("reciclar_combo_entero", "Reciclar combo " + comboId, acciones);
    }

    private void registrarUndoReciclajeParcial(Long comboPadreId, Long comboHijoId, Set<Long> idsHistorialPadreAntes, Long preguntaUsadaAnterior) {
        if (comboHijoId == null) {
            return;
        }
        entityManager.flush();
        List<Map<String, Object>> acciones = new ArrayList<>();
        acciones.add(UndoService.accionEliminarFilas("combos_preguntas", "combo_id", comboHijoId));
        for (Map<String, Object> fila : undoService.snapshotFilas("historial_jornadas", "combo_id", comboHijoId)) {
            Long id = UndoService.extraerId(fila);
            if (id != null) {
                acciones.add(UndoService.accionEliminarFila("historial_jornadas", id));
            }
        }
        for (Map<String, Object> fila : undoService.snapshotFilasNuevas(
                "historial_jornadas", "combo_id", comboPadreId, idsHistorialPadreAntes)) {
            Long id = UndoService.extraerId(fila);
            if (id != null) {
                acciones.add(UndoService.accionEliminarFila("historial_jornadas", id));
            }
        }
        acciones.add(UndoService.accionEliminarFila("combos", comboHijoId));
        Map<String, Object> camposPadre = new LinkedHashMap<>();
        camposPadre.put("pregunta_usada_id", preguntaUsadaAnterior);
        acciones.add(UndoService.accionActualizarCampos("combos", comboPadreId, camposPadre));
        undoService.registrar("reciclar_combo_parcial",
                "Reciclaje parcial combo " + comboPadreId + " → " + comboHijoId, acciones);
    }

    private static boolean mismaJornada(Map<String, Object> fila, Long jornadaId) {
        Object valor = fila.get("jornada_id");
        return valor instanceof Number && jornadaId != null && ((Number) valor).longValue() == jornadaId;
    }

    private static String valorTexto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    private static Long extraerComboPadreDesdeNotas(String notas) {
        if (notas == null) {
            return null;
        }
        int idx = notas.indexOf("PADRE:");
        if (idx < 0) {
            return null;
        }
        String resto = notas.substring(idx + 6);
        StringBuilder numero = new StringBuilder();
        for (int i = 0; i < resto.length(); i++) {
            char c = resto.charAt(i);
            if (Character.isDigit(c)) {
                numero.append(c);
            } else {
                break;
            }
        }
        return numero.length() == 0 ? null : Long.valueOf(numero.toString());
    }

    /**
     * Para cada combo reciclado de la jornada, la cadena de combos de los que procede,
     * del más reciente al original: "312/216". Los combos sin origen no aparecen.
     */
    Map<Long, String> ancestrosCombosDeJornada(Jornada jornada) {
        Map<Long, String> porCombo = new HashMap<>();
        for (Combo combo : jornada.getCombosPorSlot()) {
            if (combo == null || combo.getId() == null) {
                continue;
            }
            List<Long> ancestros = new ArrayList<>();
            Set<Long> visitados = new HashSet<>();
            Long actual = combo.getId();
            visitados.add(actual);
            Long padre;
            while ((padre = comboPadreDe(actual)) != null && visitados.add(padre)) {
                ancestros.add(padre);
                actual = padre;
            }
            if (!ancestros.isEmpty()) {
                porCombo.put(combo.getId(), ancestros.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining("/")));
            }
        }
        return porCombo;
    }

    private Long comboPadreDe(Long comboId) {
        @SuppressWarnings("unchecked")
        List<Object> notas = entityManager.createNativeQuery(
                "SELECT notas FROM historial_jornadas WHERE combo_id = ? "
                    + "AND notas LIKE 'RECICLAJE_PARCIAL_COMBO_HIJO;%'")
            .setParameter(1, comboId)
            .getResultList();
        for (Object nota : notas) {
            Long padre = extraerComboPadreDesdeNotas(valorTexto(nota));
            if (padre != null) {
                return padre;
            }
        }
        return null;
    }

    public boolean esComboDerivadoDeJornada(Long jornadaId, Long comboId) {
        Number count = (Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM historial_jornadas WHERE jornada_id = ? AND combo_id = ? "
                    + "AND notas LIKE 'RECICLAJE_PARCIAL_COMBO_HIJO;%'")
            .setParameter(1, jornadaId)
            .setParameter(2, comboId)
            .getSingleResult();
        return count.longValue() > 0;
    }

    public boolean esComboDerivado(Long comboId) {
        Number count = (Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM historial_jornadas WHERE combo_id = ? "
                    + "AND notas LIKE 'RECICLAJE_PARCIAL_COMBO_HIJO;%'")
            .setParameter(1, comboId)
            .getSingleResult();
        return count.longValue() > 0;
    }

    public List<Map<String, Object>> listarContenidoOtras(Long jornadaId, String tipo) {
        boolean listarCombos = tipo != null && tipo.toLowerCase().startsWith("combo");
        List<Map<String, Object>> resultado = new ArrayList<>();
        Set<Long> combosDerivados = listarCombos ? idsCombosDerivados() : Collections.emptySet();
        for (Jornada jornada : jornadaRepository.findAll()) {
            if (jornada.getId() == null || jornada.getId().equals(jornadaId)) {
                continue;
            }
            if (listarCombos) {
                for (Combo combo : jornada.getCombosPorSlot()) {
                    if (combo == null || combo.getId() == null) {
                        continue;
                    }
                    if (combosDerivados.contains(combo.getId()) && comboDerivadoNoAsignable(combo)) {
                        continue;
                    }
                    if (!estadoAsignableCombo(combo)) {
                        continue;
                    }
                    if (concursanteRepository.existsByCombo_Id(combo.getId())) {
                        continue;
                    }
                    resultado.add(mapearItemOtraJornadaCombo(combo, jornada));
                }
            } else {
                for (Cuestionario cuestionario : jornada.getCuestionariosPorSlot()) {
                    if (cuestionario == null || cuestionario.getId() == null) {
                        continue;
                    }
                    if (!estadoAsignableCuestionario(cuestionario)) {
                        continue;
                    }
                    if (concursanteRepository.existsByCuestionario_Id(cuestionario.getId())) {
                        continue;
                    }
                    resultado.add(mapearItemOtraJornadaCuestionario(cuestionario, jornada));
                }
            }
        }
        return resultado;
    }

    /** Estados que ConcursanteService admite al asignar un cuestionario. */
    private boolean estadoAsignableCuestionario(Cuestionario c) {
        Cuestionario.EstadoCuestionario e = c.getEstado();
        return e == Cuestionario.EstadoCuestionario.aprobado
            || e == Cuestionario.EstadoCuestionario.adjudicado
            || e == Cuestionario.EstadoCuestionario.grabado;
    }

    public boolean comboDerivadoNoAsignable(Combo combo) {
        if (combo.getEstado() == Combo.EstadoCombo.borrador) {
            return true;
        }
        if (!estadoAsignableCombo(combo)) {
            return true;
        }
        int preguntas = combo.getPreguntas() == null ? 0 : combo.getPreguntas().size();
        return preguntas > 0 && preguntas != 3;
    }

    /** Estados que ConcursanteService admite al asignar un combo. */
    private boolean estadoAsignableCombo(Combo c) {
        Combo.EstadoCombo e = c.getEstado();
        return e == Combo.EstadoCombo.aprobado
            || e == Combo.EstadoCombo.adjudicado
            || e == Combo.EstadoCombo.grabado;
    }

    private Set<Long> idsCombosDerivados() {
        @SuppressWarnings("unchecked")
        List<Object> filas = entityManager.createNativeQuery(
                "SELECT DISTINCT combo_id FROM historial_jornadas "
                    + "WHERE combo_id IS NOT NULL AND notas LIKE 'RECICLAJE_PARCIAL_COMBO_HIJO;%'")
            .getResultList();
        Set<Long> ids = new HashSet<>();
        for (Object fila : filas) {
            if (fila instanceof Number) {
                ids.add(((Number) fila).longValue());
            }
        }
        return ids;
    }

    @Transactional
    public void registrarArrastre(Long jornadaDestinoId, String tipo, Long itemId) {
        String marca = marcaArrastre(jornadaDestinoId);
        boolean esCombo = tipo != null && tipo.toLowerCase().startsWith("combo");
        if (esCombo) {
            Combo combo = comboRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Combo no encontrado"));
            if (esComboDerivado(itemId) && comboDerivadoNoAsignable(combo)) {
                throw new IllegalStateException("Un combo reciclado incompleto no se puede arrastrar a otra jornada.");
            }
            combo.setNotasDireccion(anexarNotaDireccion(combo.getNotasDireccion(), marca));
            comboRepository.save(combo);
        } else {
            Cuestionario cuestionario = cuestionarioRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Cuestionario no encontrado"));
            cuestionario.setNotasDireccion(anexarNotaDireccion(cuestionario.getNotasDireccion(), marca));
            cuestionarioRepository.save(cuestionario);
        }
    }

    @Transactional
    public void quitarArrastre(Long jornadaDestinoId, String tipo, Long itemId) {
        String marca = marcaArrastre(jornadaDestinoId);
        boolean esCombo = tipo != null && tipo.toLowerCase().startsWith("combo");
        if (esCombo) {
            Combo combo = comboRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Combo no encontrado"));
            combo.setNotasDireccion(quitarNotaDireccion(combo.getNotasDireccion(), marca));
            comboRepository.save(combo);
        } else {
            Cuestionario cuestionario = cuestionarioRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Cuestionario no encontrado"));
            cuestionario.setNotasDireccion(quitarNotaDireccion(cuestionario.getNotasDireccion(), marca));
            cuestionarioRepository.save(cuestionario);
        }
    }

    /** Los nombres de jornada ya suelen empezar por "Jornada", se evita duplicarlo. */
    private String marcaArrastre(Long jornadaDestinoId) {
        Jornada destino = jornadaRepository.findById(jornadaDestinoId)
            .orElseThrow(() -> new IllegalArgumentException("Jornada no encontrada"));
        String nombre = destino.getNombre();
        String etiqueta = (nombre != null && !nombre.isBlank())
            ? nombre.trim().replaceFirst("(?i)^jornada\\s+", "")
            : String.valueOf(destino.getId());
        if (etiqueta.isBlank()) {
            etiqueta = String.valueOf(destino.getId());
        }
        return "Arrastrado a Jornada " + etiqueta;
    }

    private String anexarNotaDireccion(String actual, String marca) {
        if (actual != null && actual.contains(marca)) {
            return actual;
        }
        if (actual == null || actual.isBlank()) {
            return marca;
        }
        return actual.trim() + "\n" + marca;
    }

    private String quitarNotaDireccion(String actual, String marca) {
        if (actual == null || !actual.contains(marca)) {
            return actual;
        }
        List<String> lineas = new ArrayList<>();
        for (String linea : actual.split("\\r?\\n", -1)) {
            if (!linea.trim().equals(marca)) {
                lineas.add(linea);
            }
        }
        String restante = String.join("\n", lineas).trim();
        return restante.isEmpty() ? null : restante;
    }

    private Map<String, Object> mapearItemOtraJornadaCuestionario(Cuestionario c, Jornada jornada) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.getId());
        map.put("tematica", c.getTematica());
        map.put("estado", c.getEstado() != null ? c.getEstado().name() : null);
        map.put("fechaCreacion", c.getFechaCreacion());
        map.put("jornadaId", jornada.getId());
        map.put("jornadaNombre", jornada.getNombre());
        map.put("preguntas", mapearPreguntasCuestionario(c));
        return map;
    }

    private Map<String, Object> mapearItemOtraJornadaCombo(Combo c, Jornada jornada) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.getId());
        map.put("tematica", c.getTematica());
        map.put("tipo", c.getTipo() != null ? c.getTipo().name() : null);
        map.put("estado", c.getEstado() != null ? c.getEstado().name() : null);
        map.put("fechaCreacion", c.getFechaCreacion());
        map.put("jornadaId", jornada.getId());
        map.put("jornadaNombre", jornada.getNombre());
        map.put("preguntas", mapearPreguntasCombo(c));
        return map;
    }

    private List<Map<String, Object>> mapearPreguntasCuestionario(Cuestionario c) {
        List<Map<String, Object>> preguntas = new ArrayList<>();
        if (c.getPreguntas() == null) {
            return preguntas;
        }
        for (PreguntaCuestionario pc : c.getPreguntas()) {
            Pregunta p = pc.getPregunta();
            if (p == null) {
                continue;
            }
            Map<String, Object> pm = new HashMap<>();
            pm.put("pregunta", p.getPregunta());
            pm.put("respuesta", p.getRespuesta());
            pm.put("nivel", p.getNivel() != null ? p.getNivel().name() : null);
            pm.put("datosExtra", p.getDatosExtra());
            preguntas.add(pm);
        }
        return preguntas;
    }

    private List<Map<String, Object>> mapearPreguntasCombo(Combo c) {
        List<Map<String, Object>> preguntas = new ArrayList<>();
        if (c.getPreguntas() == null) {
            return preguntas;
        }
        List<PreguntaCombo> ordenadas = new ArrayList<>(c.getPreguntas());
        ordenadas.sort(Comparator.comparing(pc -> pc.getPosicion() != null ? pc.getPosicion() : 999));
        for (PreguntaCombo pc : ordenadas) {
            Pregunta p = pc.getPregunta();
            if (p == null) {
                continue;
            }
            Map<String, Object> pm = new HashMap<>();
            pm.put("pregunta", p.getPregunta());
            pm.put("respuesta", p.getRespuesta());
            pm.put("nivel", p.getNivel() != null ? p.getNivel().name() : null);
            pm.put("datosExtra", p.getDatosExtra());
            pm.put("factorMultiplicacion", pc.getFactorMultiplicacion());
            pm.put("posicion", pc.getPosicion());
            preguntas.add(pm);
        }
        return preguntas;
    }
} 