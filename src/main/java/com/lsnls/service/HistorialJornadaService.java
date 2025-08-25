package com.lsnls.service;

import com.lsnls.entity.HistorialJornada;
import com.lsnls.entity.HistorialJornada.EstadoAsignacion;
import com.lsnls.entity.HistorialJornada.TipoAsignacion;
import com.lsnls.entity.Cuestionario;
import com.lsnls.entity.Combo;
import com.lsnls.entity.Jornada;
import com.lsnls.entity.Pregunta;
import com.lsnls.repository.HistorialJornadaRepository;
import com.lsnls.repository.CuestionarioRepository;
import com.lsnls.repository.ComboRepository;
import com.lsnls.repository.JornadaRepository;
import com.lsnls.repository.PreguntaRepository;
import com.lsnls.dto.HistorialJornadaDTO;
import com.lsnls.dto.MarcarNoUsadoDTO;
import com.lsnls.dto.ReaprovecharComboDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class HistorialJornadaService {
    
    @Autowired
    private HistorialJornadaRepository historialRepository;
    
    @Autowired
    private CuestionarioRepository cuestionarioRepository;
    
    @Autowired
    private ComboRepository comboRepository;
    
    @Autowired
    private JornadaRepository jornadaRepository;
    
    @Autowired
    private PreguntaRepository preguntaRepository;

    /**
     * Registrar asignación de cuestionario a jornada
     */
    public HistorialJornada registrarAsignacionCuestionario(Long jornadaId, Long cuestionarioId) {
        Optional<Jornada> jornadaOpt = jornadaRepository.findById(jornadaId);
        Optional<Cuestionario> cuestionarioOpt = cuestionarioRepository.findById(cuestionarioId);
        
        if (jornadaOpt.isEmpty() || cuestionarioOpt.isEmpty()) {
            throw new IllegalArgumentException("Jornada o cuestionario no encontrado");
        }
        
        HistorialJornada historial = new HistorialJornada();
        historial.setJornada(jornadaOpt.get());
        historial.setCuestionario(cuestionarioOpt.get());
        historial.setTipoAsignacion(TipoAsignacion.CUESTIONARIO);
        historial.setEstadoAsignacion(EstadoAsignacion.asignado);
        
        return historialRepository.save(historial);
    }

    /**
     * Registrar asignación de combo a jornada
     */
    public HistorialJornada registrarAsignacionCombo(Long jornadaId, Long comboId) {
        Optional<Jornada> jornadaOpt = jornadaRepository.findById(jornadaId);
        Optional<Combo> comboOpt = comboRepository.findById(comboId);
        
        if (jornadaOpt.isEmpty() || comboOpt.isEmpty()) {
            throw new IllegalArgumentException("Jornada o combo no encontrado");
        }
        
        HistorialJornada historial = new HistorialJornada();
        historial.setJornada(jornadaOpt.get());
        historial.setCombo(comboOpt.get());
        historial.setTipoAsignacion(TipoAsignacion.COMBO);
        historial.setEstadoAsignacion(EstadoAsignacion.asignado);
        
        return historialRepository.save(historial);
    }

    /**
     * Marcar elementos como no usados en una jornada
     */
    public void marcarNoUsados(MarcarNoUsadoDTO dto) {
        // Marcar cuestionarios como no usados
        if (dto.getCuestionarioIds() != null) {
            for (Long cuestionarioId : dto.getCuestionarioIds()) {
                List<HistorialJornada> historiales = historialRepository.findByCuestionarioId(cuestionarioId);
                for (HistorialJornada historial : historiales) {
                    if (historial.getJornada().getId().equals(dto.getJornadaId())) {
                        historial.setEstadoAsignacion(EstadoAsignacion.no_usado);
                        historial.setNotas(dto.getMotivo());
                        historialRepository.save(historial);
                        
                        // Cambiar estado del cuestionario a aprobado
                        Cuestionario cuestionario = historial.getCuestionario();
                        cuestionario.setEstado(Cuestionario.EstadoCuestionario.aprobado);
                        cuestionarioRepository.save(cuestionario);
                    }
                }
            }
        }
        
        // Marcar combos como no usados
        if (dto.getComboIds() != null) {
            for (Long comboId : dto.getComboIds()) {
                List<HistorialJornada> historiales = historialRepository.findByComboId(comboId);
                for (HistorialJornada historial : historiales) {
                    if (historial.getJornada().getId().equals(dto.getJornadaId())) {
                        historial.setEstadoAsignacion(EstadoAsignacion.no_usado);
                        historial.setNotas(dto.getMotivo());
                        historialRepository.save(historial);
                        
                        // Cambiar estado del combo a aprobado
                        Combo combo = historial.getCombo();
                        combo.setEstado(Combo.EstadoCombo.aprobado);
                        comboRepository.save(combo);
                    }
                }
            }
        }
    }

    /**
     * Reaprovechar un combo
     */
    public Combo reaprovecharCombo(ReaprovecharComboDTO dto) {
        Optional<Combo> comboOriginalOpt = comboRepository.findById(dto.getComboOriginalId());
        if (comboOriginalOpt.isEmpty()) {
            throw new IllegalArgumentException("Combo original no encontrado");
        }
        
        Combo comboOriginal = comboOriginalOpt.get();
        
        // Marcar combo original como reaprovechado
        comboOriginal.setEstado(Combo.EstadoCombo.reaprovechado);
        comboRepository.save(comboOriginal);
        
        // Crear nuevo combo
        Combo nuevoCombo = new Combo();
        nuevoCombo.setCreacionUsuario(comboOriginal.getCreacionUsuario());
        nuevoCombo.setNivel(comboOriginal.getNivel());
        nuevoCombo.setTipo(comboOriginal.getTipo());
        nuevoCombo.setEstado(Combo.EstadoCombo.borrador);
        
        Combo comboGuardado = comboRepository.save(nuevoCombo);
        
        // Registrar pregunta usada en el historial
        List<HistorialJornada> historiales = historialRepository.findByComboId(dto.getComboOriginalId());
        for (HistorialJornada historial : historiales) {
            if (historial.getEstadoAsignacion() == EstadoAsignacion.asignado) {
                historial.setEstadoAsignacion(EstadoAsignacion.usado);
                historial.setPreguntaUsadaId(dto.getPreguntaUsadaId());
                historial.setFechaUso(LocalDateTime.now());
                historial.setNotas("Combo reaprovechado - Pregunta usada: " + dto.getPreguntaUsadaId());
                historialRepository.save(historial);
                break;
            }
        }
        
        // Liberar preguntas no usadas
        if (dto.getPreguntasNoUsadasIds() != null) {
            for (Long preguntaId : dto.getPreguntasNoUsadasIds()) {
                Optional<Pregunta> preguntaOpt = preguntaRepository.findById(preguntaId);
                if (preguntaOpt.isPresent()) {
                    Pregunta pregunta = preguntaOpt.get();
                    pregunta.setEstadoDisponibilidad(Pregunta.EstadoDisponibilidad.disponible);
                    preguntaRepository.save(pregunta);
                }
            }
        }
        
        return comboGuardado;
    }

    /**
     * Obtener historial de un cuestionario
     */
    public List<HistorialJornadaDTO> obtenerHistorialCuestionario(Long cuestionarioId) {
        List<HistorialJornada> historiales = historialRepository.findByCuestionarioId(cuestionarioId);
        return historiales.stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
    }

    /**
     * Obtener historial de un combo
     */
    public List<HistorialJornadaDTO> obtenerHistorialCombo(Long comboId) {
        List<HistorialJornada> historiales = historialRepository.findByComboId(comboId);
        return historiales.stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
    }

    /**
     * Obtener asignaciones no usadas de una jornada
     */
    public List<HistorialJornadaDTO> obtenerNoUsados(Long jornadaId) {
        List<HistorialJornada> historiales = historialRepository.findByJornadaIdAndEstado(jornadaId, EstadoAsignacion.no_usado);
        return historiales.stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
    }

    /**
     * Convertir entidad a DTO
     */
    private HistorialJornadaDTO convertirADTO(HistorialJornada historial) {
        HistorialJornadaDTO dto = new HistorialJornadaDTO();
        dto.setId(historial.getId());
        dto.setJornadaId(historial.getJornada().getId());
        dto.setJornadaNombre(historial.getJornada().getNombre());
        dto.setTipoAsignacion(historial.getTipoAsignacion().name());
        dto.setEstadoAsignacion(historial.getEstadoAsignacion().name());
        dto.setFechaAsignacion(historial.getFechaAsignacion());
        dto.setFechaUso(historial.getFechaUso());
        dto.setPreguntaUsadaId(historial.getPreguntaUsadaId());
        dto.setNotas(historial.getNotas());
        
        if (historial.getCuestionario() != null) {
            dto.setCuestionarioId(historial.getCuestionario().getId());
        }
        if (historial.getCombo() != null) {
            dto.setComboId(historial.getCombo().getId());
        }
        
        return dto;
    }
}
