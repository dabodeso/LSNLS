package com.lsnls.repository;

import com.lsnls.entity.Programa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
 
@Repository
public interface ProgramaRepository extends JpaRepository<Programa, Long> {
    // Métodos personalizados si se necesitan
    Long countByTemporada(Integer temporada);
    Optional<Programa> findByCodigo(String codigo);
    boolean existsByCodigoAndIdNot(String codigo, Long id);
} 