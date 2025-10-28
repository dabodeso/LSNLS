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
     * para que coincidan con los nuevos: borrador, completa, grabada.
     */
    private void normalizarEstadosLegacy() {
        // preparacion -> borrador
        entityManager.createNativeQuery("UPDATE jornadas SET estado='borrador' WHERE estado='preparacion'").executeUpdate();
        // lista y completada -> completa
        entityManager.createNativeQuery("UPDATE jornadas SET estado='completa' WHERE estado IN ('lista','completada')").executeUpdate();
        // en_grabacion y archivada -> grabada
        entityManager.createNativeQuery("UPDATE jornadas SET estado='grabada' WHERE estado IN ('en_grabacion','archivada')").executeUpdate();
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
        jornada.setEstado(Jornada.EstadoJornada.borrador);

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
        if (jornada.getEstado() == Jornada.EstadoJornada.grabada) {
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
        if (jornada.getEstado() == Jornada.EstadoJornada.grabada) {
            throw new IllegalArgumentException("No se puede eliminar una jornada que ya está grabada.");
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
            Jornada.EstadoJornada estado = Jornada.EstadoJornada.valueOf(nuevoEstado);
            jornada.setEstado(estado);
            jornada = jornadaRepository.save(jornada);
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
        
        // Ponerlo disponible para nuevas jornadas: pasar de adjudicado -> aprobado (sin quitar de esta jornada)
        try {
            if (cuestionario.getEstado() == Cuestionario.EstadoCuestionario.adjudicado) {
                boolean exito = cuestionarioService.cambiarEstadoAtomico(
                    cuestionarioId,
                    Cuestionario.EstadoCuestionario.adjudicado,
                    Cuestionario.EstadoCuestionario.aprobado
                );
                if (!exito) {
                    throw new IllegalStateException("El cuestionario " + cuestionarioId + " fue modificado por otro usuario. Recarga e intenta de nuevo.");
                }
                // refrescar entidad
                cuestionario = cuestionarioRepository.findById(cuestionarioId)
                    .orElse(cuestionario);
            }
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("Error de concurrencia al reutilizar cuestionario " + cuestionarioId + ": " + e.getMessage());
        }

        // Registrar reutilización (sin quitar de la jornada)
        // Evitar duplicados
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
            }
        } catch (Exception e) {
            // Continuar aunque falle el conteo; intentar registrar igualmente
            registrarHistorialReutilizacion(jornada, cuestionario, "cuestionario", usuarioId);
        }
        
        System.out.println("✅ [JORNADA] Cuestionario " + cuestionarioId + " reutilizado de jornada " + jornadaId);
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
        
        // Ponerlo disponible: pasar de adjudicado -> aprobado (sin quitar de esta jornada)
        try {
            if (combo.getEstado() == Combo.EstadoCombo.adjudicado) {
                boolean exito = comboService.cambiarEstadoAtomico(
                    comboId,
                    Combo.EstadoCombo.adjudicado,
                    Combo.EstadoCombo.aprobado
                );
                if (!exito) {
                    throw new IllegalStateException("El combo " + comboId + " fue modificado por otro usuario. Recarga e intenta de nuevo.");
                }
                combo = comboRepository.findById(comboId).orElse(combo);
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
        
        System.out.println("✅ [JORNADA] Combo " + comboId + " reutilizado de jornada " + jornadaId);
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
        
        // Verificar que el combo está en estado adjudicado
        if (combo.getEstado() != Combo.EstadoCombo.adjudicado) {
            throw new IllegalArgumentException("El combo " + comboId + " no está en estado adjudicado. Estado actual: " + combo.getEstado());
        }

        // Mantener el combo en la jornada pero marcarlo como disponible para nuevas jornadas
        combo.setEstado(Combo.EstadoCombo.aprobado);
        comboRepository.save(combo);

        // Registrar en el historial como reaprovechado (para pintarlo en verde en la UI)
        registrarHistorialReutilizacion(jornada, combo, "combo", usuarioId);

        System.out.println("✅ [JORNADA] Combo " + comboId + " reciclado completamente (marcado reutilizado y disponible) en jornada " + jornadaId);
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
        
        // Verificar que el combo está en estado adjudicado
        if (combo.getEstado() != Combo.EstadoCombo.adjudicado) {
            throw new IllegalArgumentException("El combo " + comboId + " no está en estado adjudicado. Estado actual: " + combo.getEstado());
        }
        
        // Verificar que el combo tiene exactamente 3 preguntas
        if (combo.getPreguntas() == null || combo.getPreguntas().size() != 3) {
            throw new IllegalArgumentException("El combo debe tener exactamente 3 preguntas para reciclaje parcial");
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
        
        // El combo se mantiene en la jornada; se libera(n) las 2 preguntas no usadas
        // 1) Eliminar del combo las preguntas no usadas y marcarlas como disponibles
        java.util.List<PreguntaCombo> aEliminar = new java.util.ArrayList<>();
        for (PreguntaCombo pc : combo.getPreguntas()) {
            if (!pc.getPregunta().getId().equals(preguntaUsadaId)) {
                aEliminar.add(pc);
            }
        }
        for (PreguntaCombo pc : aEliminar) {
            try {
                // Quitar relación del combo
                preguntaComboRepository.delete(pc);
            } catch (Exception ignored) {}
            try {
                // Marcar pregunta como disponible
                entityManager.createQuery("UPDATE Pregunta p SET p.estadoDisponibilidad = :disp WHERE p.id = :pid")
                    .setParameter("disp", com.lsnls.entity.Pregunta.EstadoDisponibilidad.disponible)
                    .setParameter("pid", pc.getPregunta().getId())
                    .executeUpdate();
                // Si estaba 'usada', devolver a 'aprobada' para que vuelva a aparecer en los selectores
                entityManager.createQuery("UPDATE Pregunta p SET p.estado = :aprobada WHERE p.id = :pid AND p.estado = :usada")
                    .setParameter("aprobada", com.lsnls.entity.Pregunta.EstadoPregunta.aprobada)
                    .setParameter("usada", com.lsnls.entity.Pregunta.EstadoPregunta.usada)
                    .setParameter("pid", pc.getPregunta().getId())
                    .executeUpdate();
            } catch (Exception e) {
                System.err.println("⚠️ [JORNADA] No se pudo marcar disponible la pregunta " + pc.getPregunta().getId() + ": " + e.getMessage());
            }
        }

        // 2) Mantener combo en la jornada, marcado como reutilizado en historial
        registrarHistorialReutilizacion(jornada, combo, "combo", usuarioId);

        System.out.println("✅ [JORNADA] Combo " + comboId + " reciclado parcialmente: liberadas " + aEliminar.size() + " preguntas (disponibles), combo permanece en jornada marcado como reutilizado");
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
                .setParameter(5, "Combo hijo creado desde combo padre " + comboPadreId + " - contiene 2 preguntas no usadas")
                .executeUpdate();
                
        } catch (Exception e) {
            System.err.println("⚠️ [JORNADA] Error al registrar historial de combo hijo: " + e.getMessage());
            // No lanzar excepción para no afectar la operación principal
        }
    }
} 