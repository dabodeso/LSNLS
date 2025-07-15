package com.lsnls.repository;

import com.lsnls.entity.PreguntaCombo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
 
@Repository
public interface PreguntaComboRepository extends JpaRepository<PreguntaCombo, PreguntaCombo.PreguntaComboId> {
    
    /**
     * Verifica si una pregunta está asignada a algún combo
     */
    @Query("SELECT COUNT(pc) > 0 FROM PreguntaCombo pc WHERE pc.pregunta.id = :preguntaId")
    boolean existsByPreguntaId(@Param("preguntaId") Long preguntaId);
} 