package com.lsnls.service;

import com.lsnls.dto.JornadaDTO;
import com.lsnls.entity.*;
import com.lsnls.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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

        // Asignar cuestionarios (máximo 5)
        if (jornadaDTO.getCuestionarioIds() != null) {
            if (jornadaDTO.getCuestionarioIds().size() > 5) {
                throw new IllegalArgumentException("Máximo 5 cuestionarios por jornada");
            }
            Set<Cuestionario> cuestionarios = new HashSet<>();
            for (Long cuestionarioId : jornadaDTO.getCuestionarioIds()) {
                Cuestionario cuestionario = cuestionarioRepository.findById(cuestionarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Cuestionario no encontrado: " + cuestionarioId));
                cuestionarios.add(cuestionario);
            }
            jornada.setCuestionarios(cuestionarios);
        }

        // Asignar combos (máximo 5)
        if (jornadaDTO.getComboIds() != null) {
            if (jornadaDTO.getComboIds().size() > 5) {
                throw new IllegalArgumentException("Máximo 5 combos por jornada");
            }
            Set<Combo> combos = new HashSet<>();
            for (Long comboId : jornadaDTO.getComboIds()) {
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
            Set<Cuestionario> cuestionarios = new HashSet<>();
            for (Long cuestionarioId : jornadaDTO.getCuestionarioIds()) {
                Cuestionario cuestionario = cuestionarioRepository.findById(cuestionarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Cuestionario no encontrado: " + cuestionarioId));
                cuestionarios.add(cuestionario);
            }
            jornada.setCuestionarios(cuestionarios);
        }

        // Actualizar combos
        if (jornadaDTO.getComboIds() != null) {
            if (jornadaDTO.getComboIds().size() > 5) {
                throw new IllegalArgumentException("Máximo 5 combos por jornada");
            }
            Set<Combo> combos = new HashSet<>();
            for (Long comboId : jornadaDTO.getComboIds()) {
                Combo combo = comboRepository.findById(comboId)
                    .orElseThrow(() -> new IllegalArgumentException("Combo no encontrado: " + comboId));
                combos.add(combo);
            }
            jornada.setCombos(combos);
        }

        jornada = jornadaRepository.save(jornada);
        return convertirADTO(jornada);
    }

    public void eliminar(Long id) {
        Jornada jornada = jornadaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Jornada no encontrada"));

        if (jornada.getEstado() == Jornada.EstadoJornada.en_grabacion) {
            throw new IllegalArgumentException("No se puede eliminar una jornada en grabación");
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