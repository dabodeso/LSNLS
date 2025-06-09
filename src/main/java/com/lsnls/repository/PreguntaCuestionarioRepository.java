package com.lsnls.repository;

import com.lsnls.entity.PreguntaCuestionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
@Repository
public interface PreguntaCuestionarioRepository extends JpaRepository<PreguntaCuestionario, PreguntaCuestionario.PreguntaCuestionarioId> {
} 