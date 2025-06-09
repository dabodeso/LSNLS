package com.lsnls.service;

import com.lsnls.dto.ConcursanteDTO;
import com.lsnls.entity.Concursante;
import com.lsnls.entity.EstadoConcursante;
import com.lsnls.entity.Cuestionario;
import com.lsnls.repository.ConcursanteRepository;
import com.lsnls.repository.CuestionarioRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConcursanteService {

    @Autowired
    private ConcursanteRepository concursanteRepository;

    @Autowired
    private CuestionarioRepository cuestionarioRepository;

    public List<ConcursanteDTO> findAll() {
        return concursanteRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ConcursanteDTO findById(Long id) {
        return concursanteRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    @Transactional
    public ConcursanteDTO create(ConcursanteDTO concursanteDTO) {
        Concursante concursante = convertToEntity(concursanteDTO);
        concursante.actualizarEstado();
        concursante = concursanteRepository.save(concursante);
        return convertToDTO(concursante);
    }

    @Transactional
    public ConcursanteDTO update(Long id, ConcursanteDTO concursanteDTO) {
        Concursante concursante = concursanteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Concursante no encontrado"));

        BeanUtils.copyProperties(concursanteDTO, concursante, "id");
        concursante.actualizarEstado();
        concursante = concursanteRepository.save(concursante);
        return convertToDTO(concursante);
    }

    public void delete(Long id) {
        concursanteRepository.deleteById(id);
    }

    public List<ConcursanteDTO> findByEstado(EstadoConcursante estado) {
        return concursanteRepository.findByEstado(estado).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ConcursanteDTO> findByProgramaId(Long programaId) {
        return concursanteRepository.findByProgramaId(programaId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ConcursanteDTO convertToDTO(Concursante concursante) {
        ConcursanteDTO dto = new ConcursanteDTO();
        BeanUtils.copyProperties(concursante, dto);
        if (concursante.getFecha() != null) {
            dto.setFecha(concursante.getFecha().toString());
        } else {
            dto.setFecha(null);
        }
        if (concursante.getCuestionario() != null) {
            dto.setCuestionarioId(concursante.getCuestionario().getId());
        } else {
            dto.setCuestionarioId(null);
        }
        if (concursante.getPrograma() != null) {
            ConcursanteDTO.ProgramaResumen resumen = new ConcursanteDTO.ProgramaResumen();
            resumen.setId(concursante.getPrograma().getId());
            resumen.setFechaEmision(concursante.getPrograma().getFechaEmision());
            dto.setPrograma(resumen);
        } else {
            dto.setPrograma(null);
        }
        dto.setImagen(concursante.getImagen());
        return dto;
    }

    private Concursante convertToEntity(ConcursanteDTO dto) {
        Concursante concursante = new Concursante();
        BeanUtils.copyProperties(dto, concursante);
        if (dto.getCuestionarioId() != null) {
            cuestionarioRepository.findById(dto.getCuestionarioId()).ifPresent(concursante::setCuestionario);
        } else {
            concursante.setCuestionario(null);
        }
        concursante.setImagen(dto.getImagen());
        return concursante;
    }
} 