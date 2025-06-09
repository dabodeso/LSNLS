package com.lsnls.repository;

import com.lsnls.entity.Concursante;
import com.lsnls.entity.EstadoConcursante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConcursanteRepository extends JpaRepository<Concursante, Long> {
    List<Concursante> findByEstado(EstadoConcursante estado);
    List<Concursante> findByProgramaId(Long programaId);
} 