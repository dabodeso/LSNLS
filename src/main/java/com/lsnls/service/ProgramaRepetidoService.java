package com.lsnls.service;

import com.lsnls.dto.ConcursanteDTO;
import com.lsnls.dto.ProgramaRepetidoDTO;
import com.lsnls.entity.Concursante;
import com.lsnls.entity.ProgramaRepetido;
import com.lsnls.entity.ProgramaRepetidoConcursante;
import com.lsnls.repository.ConcursanteRepository;
import com.lsnls.repository.ProgramaRepetidoConcursanteRepository;
import com.lsnls.repository.ProgramaRepetidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProgramaRepetidoService {

    private static final Set<String> ESTADOS_ASIGNABLES = Set.of("grabado", "editado", "emitido");

    @Autowired
    private ProgramaRepetidoRepository programaRepetidoRepository;

    @Autowired
    private ProgramaRepetidoConcursanteRepository enlaceRepository;

    @Autowired
    private ConcursanteRepository concursanteRepository;

    @Autowired
    private ConcursanteService concursanteService;

    public List<ProgramaRepetidoDTO> findAll() {
        return programaRepetidoRepository.findAllByOrderByFechaEmisionDescIdDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ConcursanteDTO> findConcursantes(Long repetidoId) {
        exigirRepetido(repetidoId);
        List<ProgramaRepetidoConcursante> enlaces = enlaceRepository
                .findByProgramaRepetidoIdOrderByOrdenEscaletaAsc(repetidoId);
        if (enlaces.isEmpty()) {
            return List.of();
        }
        Map<Long, Concursante> porId = concursanteRepository.findAllById(
                enlaces.stream().map(ProgramaRepetidoConcursante::getConcursanteId).collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(Concursante::getId, concursante -> concursante));
        List<ConcursanteDTO> resultado = new ArrayList<>();
        for (ProgramaRepetidoConcursante enlace : enlaces) {
            Concursante concursante = porId.get(enlace.getConcursanteId());
            if (concursante == null) {
                continue;
            }
            ConcursanteDTO dto = concursanteService.copiarDto(concursante);
            dto.setOrdenEscaleta(enlace.getOrdenEscaleta());
            resultado.add(dto);
        }
        resultado.sort(Comparator.comparing(dto -> dto.getOrdenEscaleta() == null ? Integer.MAX_VALUE : dto.getOrdenEscaleta()));
        return resultado;
    }

    public Page<ConcursanteDTO> findDisponibles(Long repetidoId, Pageable pageable, String busqueda, String estado) {
        exigirRepetido(repetidoId);
        return concursanteService.findDisponiblesParaRepetido(pageable, busqueda, estado, repetidoId);
    }

    @Transactional
    public ConcursanteDTO asignar(Long repetidoId, Long concursanteId, Integer posicion) {
        exigirRepetido(repetidoId);
        Concursante concursante = concursanteRepository.findById(concursanteId)
                .orElseThrow(() -> new IllegalArgumentException("Concursante no encontrado"));
        String estado = concursante.getEstado() == null ? "" : concursante.getEstado().trim().toLowerCase();
        if (!ESTADOS_ASIGNABLES.contains(estado)) {
            throw new IllegalArgumentException("Solo se pueden añadir concursantes en estado grabado, editado o emitido.");
        }
        if (enlaceRepository.existsByProgramaRepetidoIdAndConcursanteId(repetidoId, concursanteId)) {
            throw new IllegalArgumentException("Ese concursante ya está en este repetido.");
        }
        int slot = posicion != null ? posicion : primerHuecoLibre(repetidoId);
        if (slot < 1 || slot > 3) {
            throw new IllegalArgumentException("La posición debe estar entre 1 y 3");
        }
        if (enlaceRepository.existsByProgramaRepetidoIdAndOrdenEscaleta(repetidoId, slot)) {
            throw new IllegalArgumentException("La posición " + slot + " ya está ocupada en este repetido");
        }
        ProgramaRepetidoConcursante enlace = new ProgramaRepetidoConcursante();
        enlace.setProgramaRepetidoId(repetidoId);
        enlace.setConcursanteId(concursanteId);
        enlace.setOrdenEscaleta(slot);
        enlaceRepository.save(enlace);
        actualizarTotal(repetidoId);
        ConcursanteDTO dto = concursanteService.copiarDto(concursante);
        dto.setOrdenEscaleta(slot);
        return dto;
    }

    @Transactional
    public void quitar(Long repetidoId, Long concursanteId) {
        exigirRepetido(repetidoId);
        if (!enlaceRepository.existsByProgramaRepetidoIdAndConcursanteId(repetidoId, concursanteId)) {
            throw new IllegalArgumentException("Ese concursante no está en este repetido.");
        }
        enlaceRepository.deleteByProgramaRepetidoIdAndConcursanteId(repetidoId, concursanteId);
        actualizarTotal(repetidoId);
    }

    private int primerHuecoLibre(Long repetidoId) {
        Set<Integer> usadas = enlaceRepository.findByProgramaRepetidoIdOrderByOrdenEscaletaAsc(repetidoId).stream()
                .map(ProgramaRepetidoConcursante::getOrdenEscaleta)
                .collect(Collectors.toSet());
        for (int slot = 1; slot <= 3; slot++) {
            if (!usadas.contains(slot)) {
                return slot;
            }
        }
        throw new IllegalArgumentException("El repetido ya tiene ocupadas las 3 posiciones de concursante");
    }

    private void actualizarTotal(Long repetidoId) {
        ProgramaRepetido repetido = exigirRepetido(repetidoId);
        repetido.setTotalConcursantes((int) enlaceRepository.countByProgramaRepetidoId(repetidoId));
        programaRepetidoRepository.save(repetido);
    }

    private ProgramaRepetido exigirRepetido(Long repetidoId) {
        return programaRepetidoRepository.findById(repetidoId)
                .orElseThrow(() -> new IllegalArgumentException("Programa repetido no encontrado"));
    }

    private ProgramaRepetidoDTO toDto(ProgramaRepetido repetido) {
        ProgramaRepetidoDTO dto = new ProgramaRepetidoDTO();
        dto.setId(repetido.getId());
        dto.setCodigo(repetido.getCodigo());
        dto.setVersion(repetido.getVersion());
        dto.setTemporada(repetido.getTemporada());
        dto.setFechaEmision(repetido.getFechaEmision());
        dto.setEstado(repetido.getEstado() != null ? repetido.getEstado().name() : null);
        dto.setTotalPremios(repetido.getTotalPremios());
        dto.setGap(repetido.getGap());
        dto.setTotalConcursantes(repetido.getTotalConcursantes());
        dto.setCreditosEspeciales(repetido.getCreditosEspeciales());
        dto.setNotas(repetido.getNotas());
        dto.setDuracionObjetivo("45m");
        dto.setTipo(repetido.getTipo());
        dto.setProgramaOriginalCodigo(repetido.getProgramaOriginalCodigo());
        dto.setFechaOriginal(repetido.getFechaOriginal());
        dto.setRepetido(true);
        return dto;
    }
}
