package com.lsnls.repository;

import com.lsnls.entity.TematicaCombo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TematicaComboRepository extends JpaRepository<TematicaCombo, Long> {
    Optional<TematicaCombo> findByNombre(String nombre);
    boolean existsByNombre(String nombre);
}


