package com.lsnls.repository;

import com.lsnls.entity.Programa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
@Repository
public interface ProgramaRepository extends JpaRepository<Programa, Long> {
    // Métodos personalizados si se necesitan
} 