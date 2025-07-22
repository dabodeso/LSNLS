package com.lsnls.service;

import com.lsnls.dto.ProgramaDTO;
import com.lsnls.entity.Programa;
import com.lsnls.repository.ProgramaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

@Service
public class ProgramaService {

    @Autowired
    private ProgramaRepository programaRepository;

    @Autowired
    private ConfiguracionGlobalService configuracionService;

    @Autowired
    private EntityManager entityManager;

    public List<Programa> findAll() {
        return programaRepository.findAll();
    }

    public List<ProgramaDTO> findAllDTO() {
        return programaRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<Programa> findById(Long id) {
        return programaRepository.findById(id);
    }

    public Optional<ProgramaDTO> findByIdDTO(Long id) {
        return programaRepository.findById(id)
                .map(this::convertToDTO);
    }

    public Programa create(Programa programa) {
        programa.actualizarEstado();
        return programaRepository.save(programa);
    }

    public ProgramaDTO createFromDTO(ProgramaDTO programaDTO) {
        Programa programa = convertToEntity(programaDTO);
        programa.actualizarEstado();
        Programa saved = programaRepository.save(programa);
        return convertToDTO(saved);
    }

    public Programa update(Long id, Programa programa) {
        programa.setId(id);
        programa.actualizarEstado();
        return programaRepository.save(programa);
    }

    public ProgramaDTO updateFromDTO(Long id, ProgramaDTO programaDTO) {
        Programa programa = convertToEntity(programaDTO);
        programa.setId(id);
        programa.actualizarEstado();
        Programa saved = programaRepository.save(programa);
        return convertToDTO(saved);
    }

    public ProgramaDTO updateCampo(Long id, Map<String, Object> campo) {
        Optional<Programa> optionalPrograma = programaRepository.findById(id);
        if (optionalPrograma.isPresent()) {
            Programa programa = optionalPrograma.get();
            
            for (Map.Entry<String, Object> entry : campo.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                switch (key) {
                    case "temporada":
                        if (value != null) {
                            programa.setTemporada(Integer.parseInt(value.toString()));
                        }
                        break;
                    case "totalPremios":
                        programa.setTotalPremios(value != null ? new BigDecimal(value.toString()) : null);
                        break;
                    case "creditosEspeciales":
                        programa.setCreditosEspeciales((String) value);
                        break;
                    case "resultadoAcumulado":
                        programa.setResultadoAcumulado(value != null ? new BigDecimal(value.toString()) : null);
                        break;
                    case "duracionAcumulada":
                        if (value != null && !value.toString().isEmpty()) {
                            try {
                                programa.setDuracionAcumulada(java.time.LocalTime.parse(value.toString()));
                            } catch (Exception e) {
                                programa.setDuracionAcumulada(null);
                            }
                        } else {
                            programa.setDuracionAcumulada(null);
                        }
                        break;
                    case "fechaEmision":
                        if (value != null && !value.toString().isEmpty()) {
                            try {
                                programa.setFechaEmision(java.time.LocalDate.parse(value.toString()));
                            } catch (Exception e) {
                                programa.setFechaEmision(null);
                            }
                        } else {
                            programa.setFechaEmision(null);
                        }
                        break;
                    case "estado":
                        if (value != null && !value.toString().isEmpty()) {
                            try {
                                programa.setEstado(Programa.EstadoPrograma.valueOf(value.toString()));
                            } catch (Exception e) {
                                // Si el estado no es válido, no cambiar el estado actual
                            }
                        }
                        break;
                }
            }
            
            programa.actualizarEstado();
            
            Programa saved = programaRepository.save(programa);
            return convertToDTO(saved);
        }
        throw new RuntimeException("Programa no encontrado con id: " + id);
    }

    public void delete(Long id) {
        // Verificar que el programa existe
        Optional<Programa> programaOpt = programaRepository.findById(id);
        if (programaOpt.isEmpty()) {
            throw new IllegalArgumentException("Programa con ID " + id + " no encontrado");
        }

        Programa programa = programaOpt.get();

        // Verificar dependencias - no se puede eliminar si hay concursantes asignados
        Long concursantesCount = entityManager.createQuery(
            "SELECT COUNT(c) FROM Concursante c WHERE c.numeroPrograma = :programaId", Long.class)
            .setParameter("programaId", programa.getTemporada())
            .getSingleResult();
        
        if (concursantesCount > 0) {
            throw new IllegalArgumentException("No se puede eliminar el programa temporada " + 
                programa.getTemporada() + " porque tiene " + concursantesCount + 
                " concursante(s) asignado(s). Desasigna los concursantes primero.");
        }

        // Verificar estado del programa - no eliminar si está en grabación o finalizado
        if (programa.getEstado() == Programa.EstadoPrograma.programado) {
            throw new IllegalArgumentException("No se puede eliminar un programa que ya está programado. " +
                "Cambia su estado a 'borrador' primero.");
        }
        
        if (programa.getEstado() == Programa.EstadoPrograma.emitido) {
            throw new IllegalArgumentException("No se puede eliminar un programa que ya ha sido emitido. " +
                "Los programas emitidos no pueden ser eliminados.");
        }

        // Si llegamos aquí, es seguro eliminar
        programaRepository.deleteById(id);
    }

    public String getDuracionObjetivo() {
        return configuracionService.getDuracionObjetivo();
    }

    private ProgramaDTO convertToDTO(Programa programa) {
        ProgramaDTO dto = new ProgramaDTO();
        dto.setId(programa.getId());
        dto.setTemporada(programa.getTemporada());
        dto.setDuracionAcumulada(programa.getDuracionAcumulada());
        dto.setResultadoAcumulado(programa.getResultadoAcumulado());
        dto.setFechaEmision(programa.getFechaEmision());
        dto.setDatoAudienciaShare(programa.getDatoAudienciaShare());
        dto.setDatoAudienciaTarget(programa.getDatoAudienciaTarget());
        dto.setEstado(programa.getEstado() != null ? programa.getEstado().toString() : null);
        dto.setTotalPremios(programa.getTotalPremios());
        dto.setGap(programa.getGap());
        dto.setTotalConcursantes(programa.getTotalConcursantes());
        dto.setCreditosEspeciales(programa.getCreditosEspeciales());
        return dto;
    }

    private Programa convertToEntity(ProgramaDTO dto) {
        Programa programa = new Programa();
        programa.setId(dto.getId());
        programa.setTemporada(dto.getTemporada());
        programa.setDuracionAcumulada(dto.getDuracionAcumulada());
        programa.setResultadoAcumulado(dto.getResultadoAcumulado());
        programa.setFechaEmision(dto.getFechaEmision());
        programa.setDatoAudienciaShare(dto.getDatoAudienciaShare());
        programa.setDatoAudienciaTarget(dto.getDatoAudienciaTarget());
        if (dto.getEstado() != null) {
            try {
                programa.setEstado(Programa.EstadoPrograma.valueOf(dto.getEstado()));
            } catch (IllegalArgumentException e) {
                programa.setEstado(Programa.EstadoPrograma.borrador);
            }
        }
        programa.setTotalPremios(dto.getTotalPremios());
        programa.setGap(dto.getGap());
        programa.setTotalConcursantes(dto.getTotalConcursantes());
        programa.setCreditosEspeciales(dto.getCreditosEspeciales());
        return programa;
    }
} 