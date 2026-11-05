package com.lsnls.repository;

import com.lsnls.entity.Concursante;
import com.lsnls.entity.ProgramaRepetidoConcursante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConcursanteRepository extends JpaRepository<Concursante, Long>, JpaSpecificationExecutor<Concursante> {
    List<Concursante> findByEstado(String estado); // Cambio de EstadoConcursante a String
    List<Concursante> findByNumeroPrograma(Integer numeroPrograma);

    List<Concursante> findByNumeroProgramaIn(java.util.Collection<Integer> numerosPrograma);
    @Query("SELECT c FROM Concursante c WHERE c.numeroPrograma = :numeroPrograma " +
           "ORDER BY CASE WHEN c.ordenEscaleta IS NULL THEN 1 ELSE 0 END, c.ordenEscaleta ASC, c.id ASC")
    List<Concursante> findByNumeroProgramaOrderByNumeroConcursanteAsc(@Param("numeroPrograma") Integer numeroPrograma);
    long countByNumeroProgramaAndOrdenEscaletaAndIdNot(Integer numeroPrograma, Integer ordenEscaleta, Long id);
    List<Concursante> findByNumeroProgramaIsNull();
    Page<Concursante> findByNumeroProgramaIsNull(Pageable pageable);
    
    /**
     * Programas distintos en los que está asignado.
     * Normales: filas de programas_concursantes más numero_programa si no está ya en esa tabla.
     * Repetidos: cada programa de programas_repetidos_concursantes suma otra salida.
     * :salidas = 'una' (exactamente 1) o 'mas' (más de 1). Esos códigos son SalidasConcursante.UNA y MAS.
     * No usa la columna estado: grabado/editado siguen yendo por :estado.
     */
    String CUENTA_SALIDAS =
            "((SELECT COUNT(DISTINCT pc.programaId) FROM ProgramaConcursante pc " +
            "WHERE pc.concursanteId = c.id " +
            "AND (c.numeroPrograma IS NULL OR CAST(pc.programaId AS string) <> CAST(c.numeroPrograma AS string))) " +
            "+ (SELECT COUNT(ficha.id) FROM Concursante ficha WHERE ficha.id = c.id AND ficha.numeroPrograma IS NOT NULL) " +
            "+ (SELECT COUNT(DISTINCT pr.programaRepetidoId) FROM ProgramaRepetidoConcursante pr WHERE pr.concursanteId = c.id))";

    String FILTRO_ESTADO_O_SALIDAS =
            "((:salidas IS NULL AND (:estado IS NULL OR LOWER(c.estado) = LOWER(:estado))) OR " +
            "(:salidas = 'una' AND " + CUENTA_SALIDAS + " = 1) OR " +
            "(:salidas = 'mas' AND " + CUENTA_SALIDAS + " > 1))";

    // Grabado, editado o emitido, sin este programa (libres o en otro).
    // emitido/repetido del filtro cuentan salidas; no comparan la columna estado.
    @Query("SELECT c FROM Concursante c WHERE LOWER(c.estado) IN ('grabado', 'editado', 'emitido') AND " +
           "((:programaId IS NULL AND c.numeroPrograma IS NULL) OR " +
           " (:programaId IS NOT NULL AND (c.numeroPrograma IS NULL OR c.numeroPrograma <> :programaId))) AND " +
           FILTRO_ESTADO_O_SALIDAS + " AND " +
           "(:busqueda IS NULL OR " +
           "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(c.ocupacion) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(c.lugar) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(c.estado) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "CAST(c.numeroConcursante AS string) LIKE CONCAT('%', :busqueda, '%'))")
    Page<Concursante> findDisponiblesParaProgramaWithSearch(Pageable pageable,
            @Param("busqueda") String busqueda,
            @Param("estado") String estado,
            @Param("programaId") Integer programaId,
            @Param("salidas") String salidas);

    @Query("SELECT c FROM Concursante c WHERE LOWER(c.estado) IN ('grabado', 'editado', 'emitido') AND " +
           "NOT EXISTS (SELECT 1 FROM ProgramaRepetidoConcursante r " +
           "WHERE r.programaRepetidoId = :repetidoId AND r.concursanteId = c.id) AND " +
           FILTRO_ESTADO_O_SALIDAS + " AND " +
           "(:busqueda IS NULL OR " +
           "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(c.ocupacion) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(c.lugar) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(c.estado) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "CAST(c.numeroConcursante AS string) LIKE CONCAT('%', :busqueda, '%'))")
    Page<Concursante> findDisponiblesParaRepetido(Pageable pageable,
            @Param("busqueda") String busqueda,
            @Param("estado") String estado,
            @Param("repetidoId") Long repetidoId,
            @Param("salidas") String salidas);
    
    @Query("SELECT MAX(c.numeroConcursante) FROM Concursante c")
    Integer findMaxNumeroConcursante();

    Long countByNumeroConcursante(Integer numeroConcursante);

    boolean existsByCuestionario_Id(Long cuestionarioId);

    boolean existsByCombo_Id(Long comboId);

    Optional<Concursante> findFirstByCombo_Id(Long comboId);

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