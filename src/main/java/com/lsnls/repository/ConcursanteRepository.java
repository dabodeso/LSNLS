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
           "(:jornada IS NULL OR LOWER(c.jornada.nombre) LIKE LOWER(CONCAT('%', :jornada, '%'))) AND " +
           "(:lugar IS NULL OR LOWER(c.lugar) LIKE LOWER(CONCAT('%', :lugar, '%'))) AND " +
           "(:numeroPrograma IS NULL OR CAST(c.numeroPrograma AS string) LIKE CONCAT('%', :numeroPrograma, '%')) AND " +
           "(:duracionFinal IS NULL OR LOWER(c.duracionFinal) LIKE LOWER(CONCAT('%', :duracionFinal, '%'))) AND " +
           "(:valoracionFinal IS NULL OR c.valoracionFinal = :valoracionFinal) AND " +
           "(:bonico IS NULL OR " +
           "((:bonico = 'vacio' AND (c.bonico IS NULL OR c.bonico = '')) OR " +
           "(:bonico = 'contenido' AND c.bonico IS NOT NULL AND c.bonico != '')))")
    Page<Concursante> findAllWithFilters(Pageable pageable, 
            @Param("estado") String estado,
            @Param("jornada") String jornada,
            @Param("lugar") String lugar,
            @Param("numeroPrograma") String numeroPrograma,
            @Param("duracionFinal") String duracionFinal,
            @Param("valoracionFinal") String valoracionFinal,
            @Param("bonico") String bonico);
} 