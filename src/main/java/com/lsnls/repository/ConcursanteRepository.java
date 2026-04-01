package com.lsnls.repository;

import com.lsnls.entity.Concursante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConcursanteRepository extends JpaRepository<Concursante, Long>, JpaSpecificationExecutor<Concursante> {
    List<Concursante> findByEstado(String estado); // Cambio de EstadoConcursante a String
    List<Concursante> findByNumeroPrograma(Integer numeroPrograma);
    @Query("SELECT c FROM Concursante c WHERE c.numeroPrograma = :numeroPrograma " +
           "ORDER BY CASE WHEN c.numeroConcursante IS NULL THEN 1 ELSE 0 END, c.numeroConcursante ASC, c.id ASC")
    List<Concursante> findByNumeroProgramaOrderByNumeroConcursanteAsc(@Param("numeroPrograma") Integer numeroPrograma);
    long countByNumeroProgramaAndNumeroConcursante(Integer numeroPrograma, Integer numeroConcursante);
    List<Concursante> findByNumeroProgramaIsNull();
    Page<Concursante> findByNumeroProgramaIsNull(Pageable pageable);
    
    @Query("SELECT c FROM Concursante c WHERE c.numeroPrograma IS NULL AND " +
           "(:busqueda IS NULL OR " +
           "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(c.ocupacion) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(c.lugar) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "CAST(c.numeroConcursante AS string) LIKE CONCAT('%', :busqueda, '%'))")
    Page<Concursante> findByNumeroProgramaIsNullWithSearch(Pageable pageable, @Param("busqueda") String busqueda);
    
    @Query("SELECT MAX(c.numeroConcursante) FROM Concursante c")
    Integer findMaxNumeroConcursante();

    Long countByNumeroConcursante(Integer numeroConcursante);

    @Query("SELECT c FROM Concursante c WHERE " +
           "(:estado IS NULL OR c.estado = :estado) AND " +
           // Jornada: por nombre o por id cuando el filtro es numérico
           "(:jornada IS NULL OR (LOWER(c.jornada.nombre) LIKE LOWER(CONCAT('%', :jornada, '%')) OR CAST(c.jornada.id AS string) = :jornada)) AND " +
           "(:lugar IS NULL OR LOWER(c.lugar) LIKE LOWER(CONCAT('%', :lugar, '%'))) AND " +
           "(:numeroPrograma IS NULL OR CAST(c.numeroPrograma AS string) LIKE CONCAT('%', :numeroPrograma, '%')) AND " +
           // Rango de duración usando fallback: final -> dirección -> general
           "(:duracionFinalMin IS NULL OR COALESCE(c.duracionFinal, c.duracionDireccion, c.duracion) >= :duracionFinalMin) AND " +
           "(:duracionFinalMax IS NULL OR COALESCE(c.duracionFinal, c.duracionDireccion, c.duracion) <= :duracionFinalMax) AND " +
           // Valoración final (1,2,3)
           "(:valoracionFinal IS NULL OR c.valoracionFinal = :valoracionFinal) AND " +
           // Bonico vacío o con contenido
           "(:bonico IS NULL OR ((:bonico = 'vacio' AND (c.bonico IS NULL OR c.bonico = '')) OR (:bonico = 'contenido' AND c.bonico IS NOT NULL AND c.bonico <> '')))"
    )
    Page<Concursante> findAllWithFilters(Pageable pageable, 
            @Param("estado") String estado,
            @Param("jornada") String jornada,
            @Param("lugar") String lugar,
            @Param("numeroPrograma") String numeroPrograma,
            @Param("duracionFinalMin") String duracionFinalMin,
            @Param("duracionFinalMax") String duracionFinalMax,
            @Param("valoracionFinal") String valoracionFinal,
            @Param("bonico") String bonico);
} 