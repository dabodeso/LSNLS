package com.lsnls.repository;

import com.lsnls.entity.Tematica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TematicaRepository extends JpaRepository<Tematica, Long> {
    
    // Buscar por nombre (case insensitive)
    Optional<Tematica> findByNombreIgnoreCase(String nombre);
    
    // Verificar si existe por nombre (case insensitive)
    boolean existsByNombreIgnoreCase(String nombre);
    
    // Obtener todas las temáticas ordenadas por nombre
    List<Tematica> findAllByOrderByNombreAsc();
    
    // Buscar temáticas que contengan un texto específico
    @Query("SELECT t FROM Tematica t WHERE LOWER(t.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) ORDER BY t.nombre")
    List<Tematica> findByNombreContainingIgnoreCase(String texto);
}
