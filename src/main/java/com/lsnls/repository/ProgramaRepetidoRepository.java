package com.lsnls.repository;

import com.lsnls.entity.ProgramaRepetido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramaRepetidoRepository extends JpaRepository<ProgramaRepetido, Long> {
    List<ProgramaRepetido> findAllByOrderByFechaEmisionDescIdDesc();

    @Query("SELECT r.codigo FROM ProgramaRepetido r WHERE r.tipo = :tipo AND r.temporada = :temporada")
    List<String> findCodigosByTipoAndTemporada(@Param("tipo") String tipo, @Param("temporada") Integer temporada);

    boolean existsByTipoAndCodigoAndTemporadaAndFechaEmision(
            String tipo, String codigo, Integer temporada, LocalDate fechaEmision);

    boolean existsByTipoAndCodigoAndTemporadaAndFechaEmisionIsNull(
            String tipo, String codigo, Integer temporada);

    Optional<ProgramaRepetido> findByTipoAndCodigoAndTemporadaAndFechaEmision(
            String tipo, String codigo, Integer temporada, LocalDate fechaEmision);

    Optional<ProgramaRepetido> findByTipoAndCodigoAndTemporadaAndFechaEmisionIsNull(
            String tipo, String codigo, Integer temporada);
}
