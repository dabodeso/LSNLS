package com.lsnls.repository;

import com.lsnls.entity.ProgramaRepetidoConcursante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgramaRepetidoConcursanteRepository extends JpaRepository<ProgramaRepetidoConcursante, Long> {
    List<ProgramaRepetidoConcursante> findByProgramaRepetidoIdOrderByOrdenEscaletaAsc(Long programaRepetidoId);

    boolean existsByProgramaRepetidoIdAndConcursanteId(Long programaRepetidoId, Long concursanteId);

    boolean existsByProgramaRepetidoIdAndOrdenEscaleta(Long programaRepetidoId, Integer ordenEscaleta);

    long countByProgramaRepetidoId(Long programaRepetidoId);

    void deleteByProgramaRepetidoIdAndConcursanteId(Long programaRepetidoId, Long concursanteId);
}
