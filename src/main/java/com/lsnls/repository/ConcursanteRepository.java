package com.lsnls.repository;

import com.lsnls.entity.Concursante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConcursanteRepository extends JpaRepository<Concursante, Long> {
    List<Concursante> findByEstado(String estado); // Cambio de EstadoConcursante a String
    List<Concursante> findByNumeroPrograma(Integer numeroPrograma);
    List<Concursante> findByNumeroProgramaIsNull();
    
    @Query("SELECT MAX(c.numeroConcursante) FROM Concursante c")
    Integer findMaxNumeroConcursante();

    Long countByNumeroConcursante(Integer numeroConcursante);
} 