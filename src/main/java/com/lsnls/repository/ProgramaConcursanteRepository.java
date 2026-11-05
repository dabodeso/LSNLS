package com.lsnls.repository;

import com.lsnls.entity.ProgramaConcursante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProgramaConcursanteRepository extends JpaRepository<ProgramaConcursante, Long> {
    List<ProgramaConcursante> findByProgramaIdOrderByOrdenEscaletaAscIdAsc(Long programaId);

    List<ProgramaConcursante> findByProgramaIdIn(Collection<Long> programaIds);

    List<ProgramaConcursante> findByConcursanteId(Long concursanteId);

    List<ProgramaConcursante> findByConcursanteIdIn(Collection<Long> concursanteIds);
}
