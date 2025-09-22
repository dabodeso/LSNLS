package com.lsnls.repository;

import com.lsnls.entity.Jornada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
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
    
    @Query("SELECT j FROM Jornada j ORDER BY j.id DESC")
    Page<Jornada> findAllOrderByIdDesc(Pageable pageable);
    
    @Query("SELECT j FROM Jornada j WHERE " +
           "(:estado IS NULL OR j.estado = :estado) AND " +
           "(:fechaDesde IS NULL OR j.fechaJornada >= :fechaDesde) AND " +
           "(:fechaHasta IS NULL OR j.fechaJornada <= :fechaHasta) AND " +
           "(:buscar IS NULL OR LOWER(j.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR " +
           "LOWER(j.lugar) LIKE LOWER(CONCAT('%', :buscar, '%'))) " +
           "ORDER BY j.id DESC")
    Page<Jornada> findAllWithFilters(Pageable pageable,
            @Param("estado") String estado,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("buscar") String buscar);
    
    boolean existsByNombre(String nombre);
} 