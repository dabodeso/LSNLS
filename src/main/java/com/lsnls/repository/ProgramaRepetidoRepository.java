package com.lsnls.repository;

import com.lsnls.entity.ProgramaRepetido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgramaRepetidoRepository extends JpaRepository<ProgramaRepetido, Long> {
    List<ProgramaRepetido> findAllByOrderByFechaEmisionDescIdDesc();
}
