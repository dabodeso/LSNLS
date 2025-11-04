package com.lsnls.repository;

import com.lsnls.entity.SubtemaPregunta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubtemaPreguntaRepository extends JpaRepository<SubtemaPregunta, Long> {
    Optional<SubtemaPregunta> findByNombre(String nombre);
    boolean existsByNombre(String nombre);
}


