package com.lsnls.repository;

import com.lsnls.entity.ConfiguracionGlobal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracionGlobalRepository extends JpaRepository<ConfiguracionGlobal, Long> {
    Optional<ConfiguracionGlobal> findByClave(String clave);
} 