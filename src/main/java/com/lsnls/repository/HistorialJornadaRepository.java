package com.lsnls.repository;

import com.lsnls.entity.HistorialJornada;
import com.lsnls.entity.HistorialJornada.EstadoAsignacion;
import com.lsnls.entity.HistorialJornada.TipoAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HistorialJornadaRepository extends JpaRepository<HistorialJornada, Long> {
    
    // Buscar historial por cuestionario
    @Query("SELECT h FROM HistorialJornada h WHERE h.cuestionario.id = :cuestionarioId ORDER BY h.fechaAsignacion DESC")
    List<HistorialJornada> findByCuestionarioId(@Param("cuestionarioId") Long cuestionarioId);
    
    // Buscar historial por combo
    @Query("SELECT h FROM HistorialJornada h WHERE h.combo.id = :comboId ORDER BY h.fechaAsignacion DESC")
    List<HistorialJornada> findByComboId(@Param("comboId") Long comboId);
    
    // Buscar historial por jornada
    @Query("SELECT h FROM HistorialJornada h WHERE h.jornada.id = :jornadaId ORDER BY h.tipoAsignacion, h.fechaAsignacion")
    List<HistorialJornada> findByJornadaId(@Param("jornadaId") Long jornadaId);
    
    // Buscar asignaciones no usadas por jornada
    @Query("SELECT h FROM HistorialJornada h WHERE h.jornada.id = :jornadaId AND h.estadoAsignacion = :estado ORDER BY h.tipoAsignacion, h.fechaAsignacion")
    List<HistorialJornada> findByJornadaIdAndEstado(@Param("jornadaId") Long jornadaId, @Param("estado") EstadoAsignacion estado);
    
    // Buscar por tipo de asignación y estado
    @Query("SELECT h FROM HistorialJornada h WHERE h.tipoAsignacion = :tipo AND h.estadoAsignacion = :estado ORDER BY h.fechaAsignacion DESC")
    List<HistorialJornada> findByTipoAsignacionAndEstado(@Param("tipo") TipoAsignacion tipo, @Param("estado") EstadoAsignacion estado);
    
    // Contar asignaciones por cuestionario
    @Query("SELECT COUNT(h) FROM HistorialJornada h WHERE h.cuestionario.id = :cuestionarioId")
    Long countByCuestionarioId(@Param("cuestionarioId") Long cuestionarioId);
    
    // Contar asignaciones por combo
    @Query("SELECT COUNT(h) FROM HistorialJornada h WHERE h.combo.id = :comboId")
    Long countByComboId(@Param("comboId") Long comboId);
}
