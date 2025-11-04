package com.lsnls.repository;

import com.lsnls.entity.TematicaPregunta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TematicaPreguntaRepository extends JpaRepository<TematicaPregunta, Long> {
    Optional<TematicaPregunta> findByNombre(String nombre);
    boolean existsByNombre(String nombre);
}


