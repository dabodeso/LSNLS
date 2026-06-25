package com.lsnls.service;

import com.lsnls.dto.JornadaDTO;
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
import javax.persistence.EntityManager;

@Service
@Transactional
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

        // Asignar cuestionarios (máximo 6) - CON PROTECCIÓN ATÓMICA
        if (jornadaDTO.getCuestionarioIds() != null) {
            if (jornadaDTO.getCuestionarioIds().size() > 6) {
                throw new IllegalArgumentException("Máximo 6 cuestionarios por jornada");
            }
            Set<Cuestionario> cuestionarios = new HashSet<>();
            for (Long cuestionarioId : jornadaDTO.getCuestionarioIds()) {
                // OPERACIÓN ATÓMICA: Cambiar estado de 'creado' a 'adjudicado'
                try {
                    boolean exito = cuestionarioService.cambiarEstadoAtomico(
                        cuestionarioId, 
                        Cuestionario.EstadoCuestionario.aprobado, 
                        Cuestionario.EstadoCuestionario.adjudicado
                    );
                    if (!exito) {
                        throw new IllegalStateException("El cuestionario " + cuestionarioId + " fue modificado por otro usuario. Por favor, recarga e intenta nuevamente.");
                    }
                } catch (IllegalStateException e) {
                    throw new IllegalArgumentException("Error de concurrencia al asignar cuestionario " + cuestionarioId + ": " + e.getMessage());
                }
                
                // Cargar el cuestionario actualizado
                Cuestionario cuestionario = cuestionarioRepository.findById(cuestionarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Cuestionario no encontrado: " + cuestionarioId));
                cuestionarios.add(cuestionario);
            }
            jornada.setCuestionarios(cuestionarios);
        }

        // Asignar combos (máximo 6) - CON PROTECCIÓN ATÓMICA
        if (jornadaDTO.getComboIds() != null) {
            if (jornadaDTO.getComboIds().size() > 6) {
                throw new IllegalArgumentException("Máximo 6 combos por jornada");
            }
            Set<Combo> combos = new HashSet<>();
            for (Long comboId : jornadaDTO.getComboIds()) {
                // OPERACIÓN ATÓMICA: Cambiar estado de 'creado' a 'adjudicado'
                try {
                    boolean exito = comboService.cambiarEstadoAtomico(
                        comboId, 
                        Combo.EstadoCombo.aprobado, 
                        Combo.EstadoCombo.adjudicado
                    );
                    if (!exito) {
                        throw new IllegalStateException("El combo " + comboId + " fue modificado por otro usuario. Por favor, recarga e intenta nuevamente.");
                    }
                } catch (IllegalStateException e) {
                    throw new IllegalArgumentException("Error de concurrencia al asignar combo " + comboId + ": " + e.getMessage());
                }
                
                // Cargar el combo actualizado
                Combo combo = comboRepository.findById(comboId)
                    .orElseThrow(() -> new IllegalArgumentException("Combo no encontrado: " + comboId));
                combos.add(combo);
            }
            jornada.setCombos(combos);
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

        // Actualizar cuestionarios
        if (jornadaDTO.getCuestionarioIds() != null) {
            if (jornadaDTO.getCuestionarioIds().size() > 6) {
                throw new IllegalArgumentException("Máximo 6 cuestionarios por jornada");
            }
            
            // Liberar cuestionarios que ya no están asignados a esta jornada
            Set<Cuestionario> cuestionariosActuales = jornada.getCuestionarios();
            if (cuestionariosActuales != null) {
                for (Cuestionario cuestionarioActual : cuestionariosActuales) {
                    if (!jornadaDTO.getCuestionarioIds().contains(cuestionarioActual.getId())) {
                        // Este cuestionario se está quitando de la jornada
                        if (cuestionarioActual.getEstado() == Cuestionario.EstadoCuestionario.adjudicado) {
                            cuestionarioActual.setEstado(Cuestionario.EstadoCuestionario.aprobado);
                            cuestionarioRepository.save(cuestionarioActual);
                        }
                    }
                }
            }
            
            Set<Cuestionario> cuestionarios = new HashSet<>();
            for (Long cuestionarioId : jornadaDTO.getCuestionarioIds()) {
                Cuestionario cuestionario = cuestionarioRepository.findById(cuestionarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Cuestionario no encontrado: " + cuestionarioId));
                
                // Si es un cuestionario nuevo (no estaba previamente asignado)
                if (cuestionariosActuales == null || !cuestionariosActuales.contains(cuestionario)) {
                    // OPERACIÓN ATÓMICA: Cambiar estado de 'creado' a 'adjudicado'
                    try {
                        boolean exito = cuestionarioService.cambiarEstadoAtomico(
                            cuestionarioId, 
                            Cuestionario.EstadoCuestionario.aprobado, 
                            Cuestionario.EstadoCuestionario.adjudicado
                        );
                        if (!exito) {
                            throw new IllegalStateException("El cuestionario " + cuestionarioId + " fue modificado por otro usuario. Por favor, recarga e intenta nuevamente.");
                        }
                    } catch (IllegalStateException e) {
                        throw new IllegalArgumentException("Error de concurrencia al asignar cuestionario " + cuestionarioId + ": " + e.getMessage());
                    }
                    
                    // Recargar el cuestionario actualizado
                    cuestionario = cuestionarioRepository.findById(cuestionarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Cuestionario no encontrado: " + cuestionarioId));
                }
                cuestionarios.add(cuestionario);
            }
            jornada.setCuestionarios(cuestionarios);
        }

        // Actualizar combos
        if (jornadaDTO.getComboIds() != null) {
            if (jornadaDTO.getComboIds().size() > 6) {
                throw new IllegalArgumentException("Máximo 6 combos por jornada");
            }
            
            // Liberar combos que ya no están asignados a esta jornada
            Set<Combo> combosActuales = jornada.getCombos();
            if (combosActuales != null) {
                for (Combo comboActual : combosActuales) {
                    if (!jornadaDTO.getComboIds().contains(comboActual.getId())) {
                        // Este combo se está quitando de la jornada
                        if (comboActual.getEstado() == Combo.EstadoCombo.adjudicado) {
                            comboActual.setEstado(Combo.EstadoCombo.aprobado);
                            comboRepository.save(comboActual);
                        }
                    }
                }
            }
            
            Set<Combo> combos = new HashSet<>();
            for (Long comboId : jornadaDTO.getComboIds()) {
                Combo combo = comboRepository.findById(comboId)
                    .orElseThrow(() -> new IllegalArgumentException("Combo no encontrado: " + comboId));
                
                // Si es un combo nuevo (no estaba previamente asignado)
                if (combosActuales == null || !combosActuales.contains(combo)) {
                    // OPERACIÓN ATÓMICA: Cambiar estado de 'creado' a 'adjudicado'
                    try {
                        boolean exito = comboService.cambiarEstadoAtomico(
                            comboId, 
                            Combo.EstadoCombo.aprobado, 
                            Combo.EstadoCombo.adjudicado
                        );
                        if (!exito) {
                            throw new IllegalStateException("El combo " + comboId + " fue modificado por otro usuario. Por favor, recarga e intenta nuevamente.");
                        }
                    } catch (IllegalStateException e) {
                        throw new IllegalArgumentException("Error de concurrencia al asignar combo " + comboId + ": " + e.getMessage());
                    }
                    
                    // Recargar el combo actualizado
                    combo = comboRepository.findById(comboId)
                        .orElseThrow(() -> new IllegalArgumentException("Combo no encontrado: " + comboId));
                }
                combos.add(combo);
            }
            jornada.setCombos(combos);
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
    }

    public JornadaDTO cambiarEstado(Long id, String nuevoEstado) {
        Jornada jornada = jornadaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Jornada no encontrada"));

        try {
            System.out.println("🔄 [JORNADA ESTADO] Solicitud de cambio de estado - Jornada " + id + " -> " + nuevoEstado);
            Jornada.EstadoJornada estado = Jornada.EstadoJornada.valueOf(nuevoEstado);
            jornada.setEstado(estado);
            jornada = jornadaRepository.save(jornada);
            System.out.println("✅ [JORNADA ESTADO] Jornada " + id + " guardada con estado " + estado);

            // Si jornada queda 'archivada' (grabada en front) → marcar elementos en 'grabado'.
            // Si jornada NO está 'archivada' (p.ej. 'preparacion' o 'completada') → marcar elementos en 'adjudicado'.
            if (estado == Jornada.EstadoJornada.archivada) {
                int totalC = 0, totalCmb = 0;
                if (jornada.getCuestionarios() != null) {
                    System.out.println("ℹ️ [JORNADA ESTADO] (GRABADA) Cuestionarios asignados a jornada " + id + ": " + jornada.getCuestionarios().size());
                    for (Cuestionario c : jornada.getCuestionarios()) {
                        System.out.println("   • Cuestionario " + c.getId() + " estado actual: " + c.getEstado());
                        if (c.getEstado() != Cuestionario.EstadoCuestionario.grabado) {
                            c.setEstado(Cuestionario.EstadoCuestionario.grabado);
                            totalC++;
                            System.out.println("   → Cuestionario " + c.getId() + " marcado como GRABADO");
                        }
                        cuestionarioRepository.save(c);
                    }
                }
                if (jornada.getCombos() != null) {
                    System.out.println("ℹ️ [JORNADA ESTADO] (GRABADA) Combos asignados a jornada " + id + ": " + jornada.getCombos().size());
                    for (Combo combo : jornada.getCombos()) {
                        System.out.println("   • Combo " + combo.getId() + " estado actual: " + combo.getEstado());
                        if (combo.getEstado() != Combo.EstadoCombo.grabado) {
                            combo.setEstado(Combo.EstadoCombo.grabado);
                            totalCmb++;
                            System.out.println("   → Combo " + combo.getId() + " marcado como GRABADO");
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
                    System.out.println("🟢 [JORNADA ESTADO] Batch grabado → cuestionarios: " + updatedC + ", combos: " + updatedCb);
                } catch (Exception e) {
                    System.err.println("⚠️ [JORNADA ESTADO] Error batch (grabado): " + e.getMessage());
                }
            } else {
                int totalC = 0, totalCmb = 0;
                if (jornada.getCuestionarios() != null) {
                    System.out.println("ℹ️ [JORNADA ESTADO] (NO GRABADA) Cuestionarios asignados a jornada " + id + ": " + jornada.getCuestionarios().size());
                    for (Cuestionario c : jornada.getCuestionarios()) {
                        if (c.getEstado() != Cuestionario.EstadoCuestionario.adjudicado) {
                            c.setEstado(Cuestionario.EstadoCuestionario.adjudicado);
                            totalC++;
                            System.out.println("   → Cuestionario " + c.getId() + " marcado como ADJUDICADO");
                        }
                        cuestionarioRepository.save(c);
                    }
                }
                if (jornada.getCombos() != null) {
                    System.out.println("ℹ️ [JORNADA ESTADO] (NO GRABADA) Combos asignados a jornada " + id + ": " + jornada.getCombos().size());
                    for (Combo combo : jornada.getCombos()) {
                        if (combo.getEstado() != Combo.EstadoCombo.adjudicado) {
                            combo.setEstado(Combo.EstadoCombo.adjudicado);
                            totalCmb++;
                            System.out.println("   → Combo " + combo.getId() + " marcado como ADJUDICADO");
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
                    System.out.println("🟡 [JORNADA ESTADO] Batch adjudicado → cuestionarios: " + updatedC + ", combos: " + updatedCb);
                } catch (Exception e) {
                    System.err.println("⚠️ [JORNADA ESTADO] Error batch (adjudicado): " + e.getMessage());
                }
            }

            return convertirADTO(jornada);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado no válido: " + nuevoEstado);
        }
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
            
            return excelExportService.exportarJornada(jornada, opciones);
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
            map.put("nivel", c.getNivel().name());
            map.put("estado", c.getEstado().name());
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

        // Convertir cuestionarios
        if (jornada.getCuestionarios() != null) {
            dto.setCuestionarioIds(jornada.getCuestionarios().stream()
                .map(Cuestionario::getId).collect(Collectors.toList()));
            
            dto.setCuestionarios(jornada.getCuestionarios().stream().map(c -> {
                JornadaDTO.CuestionarioResumenDTO resumen = new JornadaDTO.CuestionarioResumenDTO();
                resumen.setId(c.getId());
                resumen.setNivel(c.getNivel().name());
                resumen.setEstado(c.getEstado().name());
                resumen.setTematica(c.getTematica());
                resumen.setNotasDireccion(c.getNotasDireccion());
                resumen.setTotalPreguntas(c.getPreguntas() != null ? c.getPreguntas().size() : 0);
                // Marcado reutilizado si existe historial con estado 'reaprovechado' para esta jornada/cuestionario
                try {
                    Long reutilizadoCount = entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM historial_jornadas WHERE jornada_id = :jid AND cuestionario_id = :cid AND estado_asignacion = 'reaprovechado'")
                        .setParameter("jid", jornada.getId())
                        .setParameter("cid", c.getId())
                        .getSingleResult() instanceof Number ? ((Number) entityManager.createNativeQuery(
                            "SELECT COUNT(*) FROM historial_jornadas WHERE jornada_id = :jid AND cuestionario_id = :cid AND estado_asignacion = 'reaprovechado'")
                            .setParameter("jid", jornada.getId())
                            .setParameter("cid", c.getId())
                            .getSingleResult()).longValue() : 0L;
                    resumen.setReutilizado(reutilizadoCount != null && reutilizadoCount > 0);
                } catch (Exception ignored) {}
                return resumen;
            }).collect(Collectors.toList()));
        }

        // Convertir combos
        if (jornada.getCombos() != null) {
            dto.setComboIds(jornada.getCombos().stream()
                .map(Combo::getId).collect(Collectors.toList()));
            
            dto.setCombos(jornada.getCombos().stream().map(c -> {
                JornadaDTO.ComboResumenDTO resumen = new JornadaDTO.ComboResumenDTO();
                resumen.setId(c.getId());
                resumen.setNivel(c.getNivel().name());
                resumen.setEstado(c.getEstado().name());
                resumen.setTipo(c.getTipo() != null ? c.getTipo().name() : null);
                resumen.setNotasDireccion(c.getNotasDireccion());
                resumen.setTotalPreguntas(c.getPreguntas() != null ? c.getPreguntas().size() : 0);
                try {
                    Long reutilizadoCount = entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM historial_jornadas WHERE jornada_id = :jid AND combo_id = :cid AND estado_asignacion = 'reaprovechado'")
                        .setParameter("jid", jornada.getId())
                        .setParameter("cid", c.getId())
                        .getSingleResult() instanceof Number ? ((Number) entityManager.createNativeQuery(
                            "SELECT COUNT(*) FROM historial_jornadas WHERE jornada_id = :jid AND combo_id = :cid AND estado_asignacion = 'reaprovechado'")
                            .setParameter("jid", jornada.getId())
                            .setParameter("cid", c.getId())
                            .getSingleResult()).longValue() : 0L;
                    resumen.setReutilizado(reutilizadoCount != null && reutilizadoCount > 0);
                } catch (Exception ignored) {}
                return resumen;
            }).collect(Collectors.toList()));
        }

        return dto;
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
        
        // Ponerlo disponible para nuevas jornadas: pasar de adjudicado/grabado -> aprobado (sin quitar de esta jornada)
        System.out.println("========================================");
        System.out.println("[REUTILIZAR-CUEST] INICIO - Cuestionario " + cuestionarioId + " de jornada " + jornadaId);
        
        try {
            Cuestionario.EstadoCuestionario estadoActual = cuestionario.getEstado();
            System.out.println("[REUTILIZAR-CUEST] Estado actual: " + estadoActual);
            
            if (estadoActual == Cuestionario.EstadoCuestionario.adjudicado || estadoActual == Cuestionario.EstadoCuestionario.grabado) {
                System.out.println("[REUTILIZAR-CUEST] Cambiando estado: " + estadoActual + " -> aprobado");
                boolean exito = cuestionarioService.cambiarEstadoAtomico(
                    cuestionarioId,
                    estadoActual,
                    Cuestionario.EstadoCuestionario.aprobado
                );
                if (!exito) {
                    System.err.println("[REUTILIZAR-CUEST] ERROR: No se pudo cambiar el estado");
                    throw new IllegalStateException("El cuestionario " + cuestionarioId + " fue modificado por otro usuario. Recarga e intenta de nuevo.");
                }
                // refrescar entidad
                cuestionario = cuestionarioRepository.findById(cuestionarioId)
                    .orElse(cuestionario);
                System.out.println("[REUTILIZAR-CUEST] EXITO: Estado cambiado a " + cuestionario.getEstado());
            } else if (estadoActual == Cuestionario.EstadoCuestionario.aprobado) {
                System.out.println("[REUTILIZAR-CUEST] Ya estaba en estado aprobado");
            } else {
                System.out.println("[REUTILIZAR-CUEST] ERROR: Estado invalido " + estadoActual);
                throw new IllegalArgumentException("El cuestionario " + cuestionarioId + " está en estado " + estadoActual + ". Solo se pueden reutilizar cuestionarios en estado 'adjudicado' o 'grabado'.");
            }
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("Error de concurrencia al reutilizar cuestionario " + cuestionarioId + ": " + e.getMessage());
        }

        // Registrar reutilización (sin quitar de la jornada)
        System.out.println("[REUTILIZAR-CUEST] Registrando en historial...");
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
                System.out.println("[REUTILIZAR-CUEST] Registro de historial creado");
            } else {
                System.out.println("[REUTILIZAR-CUEST] Ya existia registro en historial");
            }
        } catch (Exception e) {
            registrarHistorialReutilizacion(jornada, cuestionario, "cuestionario", usuarioId);
            System.out.println("[REUTILIZAR-CUEST] Registro de historial creado (catch)");
        }
        
        System.out.println("[REUTILIZAR-CUEST] COMPLETADO - Cuestionario " + cuestionarioId);
        System.out.println("========================================");
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
            System.out.println("🔄🔄🔄 [REUTILIZAR COMBO] Combo " + comboId + " | Estado actual: " + estadoActual + " | Jornada: " + jornadaId);
            
            if (estadoActual == Combo.EstadoCombo.adjudicado || estadoActual == Combo.EstadoCombo.grabado) {
                System.out.println("🔄 [REUTILIZAR COMBO] Cambiando estado: " + estadoActual + " -> aprobado");
                boolean exito = comboService.cambiarEstadoAtomico(
                    comboId,
                    estadoActual,
                    Combo.EstadoCombo.aprobado
                );
                if (!exito) {
                    System.err.println("❌ [REUTILIZAR COMBO] No se pudo cambiar el estado del combo " + comboId);
                    throw new IllegalStateException("El combo " + comboId + " fue modificado por otro usuario. Recarga e intenta de nuevo.");
                }
                combo = comboRepository.findById(comboId).orElse(combo);
                System.out.println("✅✅✅ [REUTILIZAR COMBO] Combo " + comboId + " cambiado: " + estadoActual + " -> " + combo.getEstado());
            } else if (estadoActual == Combo.EstadoCombo.aprobado) {
                System.out.println("✅ [REUTILIZAR COMBO] Combo " + comboId + " ya está en estado aprobado");
            } else {
                System.out.println("⚠️ [REUTILIZAR COMBO] Combo " + comboId + " está en estado " + estadoActual + ", no se puede reutilizar");
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
        
        System.out.println("♻️♻️♻️ [REUTILIZAR COMBO] Combo " + comboId + " reutilizado de jornada " + jornadaId);
    }

    public void quitarReutilizacionCuestionario(Long jornadaId, Long cuestionarioId) {
        // Si el cuestionario está asignado a otra jornada distinta, impedirlo
        Long countOtras = entityManager.createQuery(
            "SELECT COUNT(j) FROM Jornada j JOIN j.cuestionarios c WHERE c.id = :cid AND j.id <> :jid", Long.class)
            .setParameter("cid", cuestionarioId)
            .setParameter("jid", jornadaId)
            .getSingleResult();
        if (countOtras != null && countOtras > 0) {
            // Obtener alguna jornada para informar
            Long otraId = entityManager.createQuery(
                "SELECT j.id FROM Jornada j JOIN j.cuestionarios c WHERE c.id = :cid AND j.id <> :jid", Long.class)
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
            "SELECT COUNT(j) FROM Jornada j JOIN j.combos c WHERE c.id = :cid AND j.id <> :jid", Long.class)
            .setParameter("cid", comboId)
            .setParameter("jid", jornadaId)
            .getSingleResult();
        if (countOtras != null && countOtras > 0) {
            Long otraId = entityManager.createQuery(
                "SELECT j.id FROM Jornada j JOIN j.combos c WHERE c.id = :cid AND j.id <> :jid", Long.class)
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
            System.err.println("⚠️ [JORNADA] Error al registrar historial de reutilización: " + e.getMessage());
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
        Jornada jornada = jornadaRepository.findById(jornadaId)
            .orElseThrow(() -> new IllegalArgumentException("Jornada no encontrada con ID: " + jornadaId));
        
        // Verificar que el combo existe y está asignado a esta jornada
        Combo combo = comboRepository.findById(comboId)
            .orElseThrow(() -> new IllegalArgumentException("Combo no encontrado con ID: " + comboId));
        
        if (jornada.getCombos() == null || !jornada.getCombos().contains(combo)) {
            throw new IllegalArgumentException("El combo " + comboId + " no está asignado a la jornada " + jornadaId);
        }
        
        // Verificar que el combo está en estado adjudicado o grabado
        Combo.EstadoCombo estadoActual = combo.getEstado();
        if (estadoActual != Combo.EstadoCombo.adjudicado && estadoActual != Combo.EstadoCombo.grabado) {
            throw new IllegalArgumentException("El combo " + comboId + " no está en estado adjudicado ni grabado. Estado actual: " + estadoActual);
        }

        // Mantener el combo en la jornada pero marcarlo como disponible para nuevas jornadas
        System.out.println("🔄🔄🔄 [RECICLAR ENTERO] Combo " + comboId + " | Estado actual: " + estadoActual + " | Jornada: " + jornadaId);
        combo.setEstado(Combo.EstadoCombo.aprobado);
        comboRepository.save(combo);
        System.out.println("✅✅✅ [RECICLAR ENTERO] Combo " + comboId + " cambiado: " + estadoActual + " -> aprobado");

        // Registrar en el historial como reaprovechado (para pintarlo en verde en la UI)
        registrarHistorialReutilizacion(jornada, combo, "combo", usuarioId);

        System.out.println("♻️♻️♻️ [RECICLAR ENTERO] Combo " + comboId + " reciclado completamente de jornada " + jornadaId);
    }

    /**
     * Recicla un combo parcialmente, creando un nuevo combo con las preguntas no usadas.
     * 
     * @param jornadaId ID de la jornada
     * @param comboId ID del combo a reciclar
     * @param preguntaUsadaId ID de la pregunta que se usó
     * @param usuarioId ID del usuario que realiza la acción
     */
    public void reciclarComboParcial(Long jornadaId, Long comboId, Long preguntaUsadaId, Long usuarioId) {
        // Verificar que la jornada existe
        Jornada jornada = jornadaRepository.findById(jornadaId)
            .orElseThrow(() -> new IllegalArgumentException("Jornada no encontrada con ID: " + jornadaId));
        
        // Verificar que el combo existe y está asignado a esta jornada
        Combo combo = comboRepository.findById(comboId)
            .orElseThrow(() -> new IllegalArgumentException("Combo no encontrado con ID: " + comboId));
        
        if (jornada.getCombos() == null || !jornada.getCombos().contains(combo)) {
            throw new IllegalArgumentException("El combo " + comboId + " no está asignado a la jornada " + jornadaId);
        }
        
        // Verificar que el combo está en estado adjudicado o grabado
        Combo.EstadoCombo estadoActual = combo.getEstado();
        if (estadoActual != Combo.EstadoCombo.adjudicado && estadoActual != Combo.EstadoCombo.grabado) {
            throw new IllegalArgumentException("El combo " + comboId + " no está en estado adjudicado ni grabado. Estado actual: " + estadoActual);
        }
        
        // Verificar que el combo no ha sido reciclado previamente para esta jornada
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
            if (count != null && count > 0) {
                throw new IllegalArgumentException("Este combo ya ha sido reciclado previamente para esta jornada. No se puede reciclar el mismo combo varias veces.");
            }
        } catch (IllegalArgumentException e) {
            throw e; // Re-lanzar la excepción de validación
        } catch (Exception e) {
            System.err.println("⚠️ [JORNADA] Error al verificar historial de reciclaje: " + e.getMessage());
            // Continuar si hay error en la verificación (no bloquear la operación)
        }
        
        System.out.println("🔍 [JORNADA] Reciclando combo " + comboId + " parcialmente. Estado actual: " + estadoActual);
        
        int totalPreguntas = combo.getPreguntas() == null ? 0 : combo.getPreguntas().size();
        if (totalPreguntas < 2) {
            throw new IllegalArgumentException("El combo debe tener al menos 2 preguntas para reciclaje parcial. Con 1 pregunta use reciclaje entero.");
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
        
        // 2) Crear un combo nuevo con las preguntas no usadas
        Combo comboNuevo = new Combo();
        comboNuevo.setNivel(combo.getNivel());
        comboNuevo.setTipo(combo.getTipo());
        comboNuevo.setTematica(combo.getTematica());
        comboNuevo.setNotasDireccion("Combo derivado del combo " + comboId + " (reciclaje parcial) - " + (combo.getNotasDireccion() != null ? combo.getNotasDireccion() : ""));
        // CORRECCIÓN: El combo nuevo con solo 2 preguntas debe estar en estado BORRADOR, no APROBADO
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
        
        // 4) Mantener el combo original intacto en la jornada con su estado actual
        System.out.println("🔄🔄🔄 [RECICLAR PARCIAL] Manteniendo estado del combo original " + comboId + ": " + estadoActual);
        comboRepository.save(combo);
        
        // 5) Mantener combo original en la jornada, marcado como reutilizado en historial
        registrarHistorialReutilizacion(jornada, combo, "combo", usuarioId);
        
        // 6) Registrar el combo nuevo en el historial como hijo
        registrarHistorialComboHijo(jornada, comboNuevo, comboId, usuarioId);

        System.out.println("✅✅✅ [RECICLAR PARCIAL] Combo " + comboId + " reciclado parcialmente:");
        System.out.println("   - Combo original " + comboId + ": estado=" + combo.getEstado() + ", preguntas=" + totalPreguntas + " (usada=" + preguntaUsadaId + ")");
        System.out.println("   - Combo nuevo " + comboNuevo.getId() + ": estado=borrador, preguntas=" + preguntasNoUsadas.size());
        System.out.println("♻️♻️♻️ [RECICLAR PARCIAL] Reciclaje parcial completado para combo " + comboId);
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
                .setParameter(5, "Combo hijo creado desde combo padre " + comboPadreId + " - contiene preguntas no usadas")
                .executeUpdate();
                
        } catch (Exception e) {
            System.err.println("⚠️ [JORNADA] Error al registrar historial de combo hijo: " + e.getMessage());
            // No lanzar excepción para no afectar la operación principal
        }
    }
} 