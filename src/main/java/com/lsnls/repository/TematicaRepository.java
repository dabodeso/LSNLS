package com.lsnls.repository;

import com.lsnls.entity.Tematica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TematicaRepository extends JpaRepository<Tematica, Long> {
    
    List<Tematica> findAllByOrderByNombreAsc();
    
    Optional<Tematica> findByNombre(String nombre);
    
    boolean existsByNombre(String nombre);
} 