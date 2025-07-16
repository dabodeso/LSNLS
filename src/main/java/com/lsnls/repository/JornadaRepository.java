package com.lsnls.repository;

import com.lsnls.entity.Jornada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JornadaRepository extends JpaRepository<Jornada, Long> {
    
    List<Jornada> findByEstadoOrderByFechaCreacionDesc(Jornada.EstadoJornada estado);
    
    List<Jornada> findByFechaJornadaBetweenOrderByFechaJornada(LocalDate fechaInicio, LocalDate fechaFin);
    
    @Query("SELECT j FROM Jornada j WHERE j.fechaJornada = :fecha ORDER BY j.fechaCreacion DESC")
    List<Jornada> findByFechaJornada(@Param("fecha") LocalDate fecha);
    
    @Query("SELECT j FROM Jornada j ORDER BY j.fechaCreacion DESC")
    List<Jornada> findAllOrderByFechaCreacionDesc();
    
    boolean existsByNombre(String nombre);
} 