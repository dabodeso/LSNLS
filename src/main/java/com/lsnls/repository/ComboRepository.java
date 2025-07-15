package com.lsnls.repository;

import com.lsnls.entity.Combo;
import com.lsnls.entity.Combo.EstadoCombo;
import com.lsnls.entity.Combo.NivelCombo;
import com.lsnls.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ComboRepository extends JpaRepository<Combo, Long> {
    List<Combo> findByEstado(EstadoCombo estado);
    
    List<Combo> findByNivel(NivelCombo nivel);
    
    List<Combo> findByCreacionUsuario(Usuario usuario);
    
    @Query("SELECT c FROM Combo c WHERE c.estado = :estado AND c.nivel = :nivel")
    List<Combo> findByEstadoAndNivel(EstadoCombo estado, NivelCombo nivel);
} 