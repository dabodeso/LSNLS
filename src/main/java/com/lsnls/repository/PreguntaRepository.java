package com.lsnls.repository;

import com.lsnls.entity.Pregunta;
import com.lsnls.entity.Pregunta.EstadoPregunta;
import com.lsnls.entity.Pregunta.EstadoDisponibilidad;
import com.lsnls.entity.Pregunta.NivelPregunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import com.lsnls.entity.Usuario;

@Repository
public interface PreguntaRepository extends JpaRepository<Pregunta, Long> {
    @Query("SELECT DISTINCT p.tematica FROM Pregunta p WHERE p.tematica IS NOT NULL AND p.tematica <> '' ORDER BY p.tematica ASC")
    List<String> findDistinctTematicas();
    List<Pregunta> findByEstado(EstadoPregunta estado);
    
    List<Pregunta> findByNivel(NivelPregunta nivel);
    
    List<Pregunta> findByTematicaContainingIgnoreCase(String tematica);
    
    List<Pregunta> findByEstadoAndEstadoDisponibilidad(EstadoPregunta estado, EstadoDisponibilidad estadoDisponibilidad);
    
    @Query("SELECT p FROM Pregunta p WHERE p.estado = :estado AND p.nivel = :nivel")
    List<Pregunta> findByEstadoAndNivel(EstadoPregunta estado, NivelPregunta nivel);

    @Query("SELECT p FROM Pregunta p " +
           "WHERE (:nivel IS NULL OR p.nivel = :nivel) " +
           "  AND (:factor IS NULL OR p.factor = :factor) " +
           "  AND (:id IS NULL OR CAST(p.id AS string) = :id) " +
           "  AND (:pregunta IS NULL OR LOWER(p.pregunta) LIKE LOWER(CONCAT('%', :pregunta, '%'))) " +
           "  AND (:respuesta IS NULL OR LOWER(p.respuesta) LIKE LOWER(CONCAT('%', :respuesta, '%'))) " +
           "  AND (:tematica IS NULL OR LOWER(p.tematica) LIKE LOWER(CONCAT('%', :tematica, '%'))) " +
           "  AND p.estado = :estado " +
           "  AND p.estadoDisponibilidad = :estadoDisponibilidad")
    Page<Pregunta> buscarPreguntas(
        @Param("nivel") com.lsnls.entity.Pregunta.NivelPregunta nivel,
        @Param("factor") com.lsnls.entity.Pregunta.FactorPregunta factor,
        @Param("id") String id,
        @Param("pregunta") String pregunta,
        @Param("respuesta") String respuesta,
        @Param("tematica") String tematica,
        @Param("estado") com.lsnls.entity.Pregunta.EstadoPregunta estado,
        @Param("estadoDisponibilidad") com.lsnls.entity.Pregunta.EstadoDisponibilidad estadoDisponibilidad,
        Pageable pageable
    );

    @Query("SELECT p FROM Pregunta p " +
           "WHERE (:nivel IS NULL OR p.nivel = :nivel) " +
           "  AND (:factor IS NULL OR p.factor = :factor) " +
           "  AND (:id IS NULL OR CAST(p.id AS string) = :id) " +
           "  AND (:pregunta IS NULL OR LOWER(p.pregunta) LIKE LOWER(CONCAT('%', :pregunta, '%'))) " +
           "  AND (:respuesta IS NULL OR LOWER(p.respuesta) LIKE LOWER(CONCAT('%', :respuesta, '%'))) " +
           "  AND (:tematica IS NULL OR LOWER(p.tematica) LIKE LOWER(CONCAT('%', :tematica, '%'))) " +
           "  AND p.estado = :estado")
    Page<Pregunta> buscarPreguntasSinFiltroDisponibilidad(
        @Param("nivel") com.lsnls.entity.Pregunta.NivelPregunta nivel,
        @Param("factor") com.lsnls.entity.Pregunta.FactorPregunta factor,
        @Param("id") String id,
        @Param("pregunta") String pregunta,
        @Param("respuesta") String respuesta,
        @Param("tematica") String tematica,
        @Param("estado") com.lsnls.entity.Pregunta.EstadoPregunta estado,
        Pageable pageable
    );

    @Query("SELECT p FROM Pregunta p " +
           "WHERE (:nivel IS NULL OR p.nivel = :nivel) " +
           "  AND (:factor IS NULL OR p.factor = :factor) " +
           "  AND (:id IS NULL OR CAST(p.id AS string) = :id) " +
           "  AND (:pregunta IS NULL OR LOWER(p.pregunta) LIKE LOWER(CONCAT('%', :pregunta, '%'))) " +
           "  AND (:respuesta IS NULL OR LOWER(p.respuesta) LIKE LOWER(CONCAT('%', :respuesta, '%'))) " +
           "  AND (:tematica IS NULL OR LOWER(p.tematica) LIKE LOWER(CONCAT('%', :tematica, '%'))) " +
           "  AND p.estado = :estado " +
           "  AND (p.estadoDisponibilidad = 'disponible' OR p.estadoDisponibilidad = 'liberada')")
    Page<Pregunta> buscarPreguntasDisponibles(
        @Param("nivel") com.lsnls.entity.Pregunta.NivelPregunta nivel,
        @Param("factor") com.lsnls.entity.Pregunta.FactorPregunta factor,
        @Param("id") String id,
        @Param("pregunta") String pregunta,
        @Param("respuesta") String respuesta,
        @Param("tematica") String tematica,
        @Param("estado") com.lsnls.entity.Pregunta.EstadoPregunta estado,
        Pageable pageable
    );

    @Query("SELECT p FROM Pregunta p " +
           "WHERE (:nivel IS NULL OR p.nivel = :nivel) " +
           "  AND (:factor IS NULL OR p.factor = :factor) " +
           "  AND (:estado IS NULL OR p.estado = :estado) " +
           "  AND (:tematica IS NULL OR LOWER(p.tematica) LIKE LOWER(CONCAT('%', :tematica, '%'))) " +
           "  AND (:subtema IS NULL OR LOWER(p.subtema) LIKE LOWER(CONCAT('%', :subtema, '%'))) " +
           "  AND (:pregunta IS NULL OR LOWER(p.pregunta) LIKE LOWER(CONCAT('%', :pregunta, '%'))) " +
           "  AND (:respuesta IS NULL OR LOWER(p.respuesta) LIKE LOWER(CONCAT('%', :respuesta, '%'))) " +
           "  AND (:autoria IS NULL OR LOWER(COALESCE(p.autor, p.creacionUsuario.nombre, '')) LIKE LOWER(CONCAT('%', :autoria, '%'))) " +
           "  AND (:texto IS NULL OR (LOWER(p.pregunta) LIKE LOWER(CONCAT('%', :texto, '%')) OR LOWER(p.respuesta) LIKE LOWER(CONCAT('%', :texto, '%')))) " +
           "ORDER BY p.id DESC")
    List<Pregunta> filtrarTodas(
        @Param("nivel") com.lsnls.entity.Pregunta.NivelPregunta nivel,
        @Param("factor") com.lsnls.entity.Pregunta.FactorPregunta factor,
        @Param("estado") com.lsnls.entity.Pregunta.EstadoPregunta estado,
        @Param("tematica") String tematica,
        @Param("subtema") String subtema,
        @Param("pregunta") String pregunta,
        @Param("respuesta") String respuesta,
        @Param("autoria") String autoria,
        @Param("texto") String texto
    );

    @Query("SELECT p FROM Pregunta p " +
           "WHERE (:nivel IS NULL OR p.nivel = :nivel) " +
           "  AND (:factor IS NULL OR p.factor = :factor) " +
           "  AND (:tematica IS NULL OR LOWER(p.tematica) LIKE LOWER(CONCAT('%', :tematica, '%'))) " +
           "  AND (:subtema IS NULL OR LOWER(p.subtema) LIKE LOWER(CONCAT('%', :subtema, '%'))) " +
           "  AND (:pregunta IS NULL OR LOWER(p.pregunta) LIKE LOWER(CONCAT('%', :pregunta, '%'))) " +
           "  AND (:respuesta IS NULL OR LOWER(p.respuesta) LIKE LOWER(CONCAT('%', :respuesta, '%'))) " +
           "  AND (:autoria IS NULL OR LOWER(COALESCE(p.autor, p.creacionUsuario.nombre, '')) LIKE LOWER(CONCAT('%', :autoria, '%'))) " +
           "  AND (:texto IS NULL OR (LOWER(p.pregunta) LIKE LOWER(CONCAT('%', :texto, '%')) OR LOWER(p.respuesta) LIKE LOWER(CONCAT('%', :texto, '%')))) " +
           "  AND (COALESCE(:estados) IS NULL OR p.estado IN :estados) " +
           "ORDER BY p.id DESC")
    List<Pregunta> filtrarPorEstados(
        @Param("nivel") com.lsnls.entity.Pregunta.NivelPregunta nivel,
        @Param("factor") com.lsnls.entity.Pregunta.FactorPregunta factor,
        @Param("estados") java.util.List<com.lsnls.entity.Pregunta.EstadoPregunta> estados,
        @Param("tematica") String tematica,
        @Param("subtema") String subtema,
        @Param("pregunta") String pregunta,
        @Param("respuesta") String respuesta,
        @Param("autoria") String autoria,
        @Param("texto") String texto
    );

    Long countByCreacionUsuario(Usuario usuario);
} 