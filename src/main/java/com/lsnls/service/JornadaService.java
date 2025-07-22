package com.lsnls.service;

import com.lsnls.dto.JornadaDTO;
import com.lsnls.entity.*;
import com.lsnls.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;

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
    private EntityManager entityManager;

    public List<JornadaDTO> obtenerTodas() {
        List<Jornada> jornadas = jornadaRepository.findAllOrderByFechaCreacionDesc();
        return jornadas.stream().map(this::convertirADTO).collect(Collectors.toList());
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

        // Asignar cuestionarios (máximo 5) - CON PROTECCIÓN ATÓMICA
        if (jornadaDTO.getCuestionarioIds() != null) {
            if (jornadaDTO.getCuestionarioIds().size() > 5) {
                throw new IllegalArgumentException("Máximo 5 cuestionarios por jornada");
            }
            Set<Cuestionario> cuestionarios = new HashSet<>();
            for (Long cuestionarioId : jornadaDTO.getCuestionarioIds()) {
                // OPERACIÓN ATÓMICA: Cambiar estado de 'creado' a 'adjudicado'
                try {
                    boolean exito = cuestionarioService.cambiarEstadoAtomico(
                        cuestionarioId, 
                        Cuestionario.EstadoCuestionario.creado, 
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

        // Asignar combos (máximo 5) - CON PROTECCIÓN ATÓMICA
        if (jornadaDTO.getComboIds() != null) {
            if (jornadaDTO.getComboIds().size() > 5) {
                throw new IllegalArgumentException("Máximo 5 combos por jornada");
            }
            Set<Combo> combos = new HashSet<>();
            for (Long comboId : jornadaDTO.getComboIds()) {
                // OPERACIÓN ATÓMICA: Cambiar estado de 'creado' a 'adjudicado'
                try {
                    boolean exito = comboService.cambiarEstadoAtomico(
                        comboId, 
                        Combo.EstadoCombo.creado, 
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
            if (jornadaDTO.getCuestionarioIds().size() > 5) {
                throw new IllegalArgumentException("Máximo 5 cuestionarios por jornada");
            }
            
            // Liberar cuestionarios que ya no están asignados a esta jornada
            Set<Cuestionario> cuestionariosActuales = jornada.getCuestionarios();
            if (cuestionariosActuales != null) {
                for (Cuestionario cuestionarioActual : cuestionariosActuales) {
                    if (!jornadaDTO.getCuestionarioIds().contains(cuestionarioActual.getId())) {
                        // Este cuestionario se está quitando de la jornada
                        if (cuestionarioActual.getEstado() == Cuestionario.EstadoCuestionario.adjudicado) {
                            cuestionarioActual.setEstado(Cuestionario.EstadoCuestionario.creado);
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
                            Cuestionario.EstadoCuestionario.creado, 
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
            if (jornadaDTO.getComboIds().size() > 5) {
                throw new IllegalArgumentException("Máximo 5 combos por jornada");
            }
            
            // Liberar combos que ya no están asignados a esta jornada
            Set<Combo> combosActuales = jornada.getCombos();
            if (combosActuales != null) {
                for (Combo comboActual : combosActuales) {
                    if (!jornadaDTO.getComboIds().contains(comboActual.getId())) {
                        // Este combo se está quitando de la jornada
                        if (comboActual.getEstado() == Combo.EstadoCombo.adjudicado) {
                            comboActual.setEstado(Combo.EstadoCombo.creado);
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
                            Combo.EstadoCombo.creado, 
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
        if (jornada.getEstado() == Jornada.EstadoJornada.en_grabacion) {
            throw new IllegalArgumentException("No se puede eliminar una jornada que está en grabación. " +
                "Finaliza la grabación antes de eliminar la jornada.");
        }
        
        if (jornada.getEstado() == Jornada.EstadoJornada.completada) {
            throw new IllegalArgumentException("No se puede eliminar una jornada que ya está completada. " +
                "Las jornadas completadas no pueden ser eliminadas.");
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
                    cuestionario.setEstado(Cuestionario.EstadoCuestionario.creado);
                    cuestionarioRepository.save(cuestionario);
                }
            }
        }

        // Liberar todos los combos asignados a esta jornada
        if (jornada.getCombos() != null) {
            for (Combo combo : jornada.getCombos()) {
                if (combo.getEstado() == Combo.EstadoCombo.adjudicado) {
                    combo.setEstado(Combo.EstadoCombo.creado);
                    comboRepository.save(combo);
                }
            }
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

    public byte[] exportarExcel(Long id) {
        try {
            Jornada jornada = jornadaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Jornada no encontrada"));
            
            return excelExportService.exportarJornada(jornada);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar Excel: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> obtenerCuestionariosDisponibles() {
        List<Cuestionario> cuestionarios = cuestionarioRepository.findByEstado(Cuestionario.EstadoCuestionario.creado);
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
        List<Combo> combos = comboRepository.findByEstado(Combo.EstadoCombo.creado);
        return combos.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("nivel", c.getNivel().name());
            map.put("estado", c.getEstado().name());
            map.put("tipo", c.getTipo() != null ? c.getTipo().name() : null);
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
                return resumen;
            }).collect(Collectors.toList()));
        }

        return dto;
    }
} 