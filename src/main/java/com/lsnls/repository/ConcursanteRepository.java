package com.lsnls.repository;

import com.lsnls.entity.Concursante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT c FROM Concursante c WHERE " +
           "(:estado IS NULL OR c.estado = :estado) AND " +
           "(:programaId IS NULL OR c.numeroPrograma = :programaId) AND " +
           "(:jornadaId IS NULL OR c.jornada.id = :jornadaId) AND " +
           "(:valoracion IS NULL OR LOWER(c.valoracionGuionista) LIKE LOWER(CONCAT('%', :valoracion, '%')) OR " +
           "LOWER(c.valoracionFinal) LIKE LOWER(CONCAT('%', :valoracion, '%'))) AND " +
           "(:lugar IS NULL OR LOWER(c.lugar) LIKE LOWER(CONCAT('%', :lugar, '%'))) AND " +
           "(:busqueda IS NULL OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "CAST(c.numeroConcursante AS string) LIKE CONCAT('%', :busqueda, '%'))")
    Page<Concursante> findAllWithFilters(Pageable pageable, 
            @Param("estado") String estado,
            @Param("programaId") String programaId,
            @Param("jornadaId") String jornadaId,
            @Param("valoracion") String valoracion,
            @Param("lugar") String lugar,
            @Param("busqueda") String busqueda);
} 