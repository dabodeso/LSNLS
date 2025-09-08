package com.lsnls.repository;

import com.lsnls.entity.Combo;
import com.lsnls.entity.Combo.EstadoCombo;
import com.lsnls.entity.Combo.NivelCombo;
import com.lsnls.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface ComboRepository extends JpaRepository<Combo, Long> {
    List<Combo> findByEstado(EstadoCombo estado);
    
    List<Combo> findByNivel(NivelCombo nivel);
    
    List<Combo> findByCreacionUsuario(Usuario usuario);
    
    @Query("SELECT c FROM Combo c WHERE c.estado = :estado AND c.nivel = :nivel")
    List<Combo> findByEstadoAndNivel(EstadoCombo estado, NivelCombo nivel);
    
    // Obtener combos paginados usando JPQL con paginación manual
    @Query("SELECT c FROM Combo c ORDER BY c.id DESC")
    List<Combo> findAllPaginados(Pageable pageable);
    
    // Método para buscar por ID que contenga una cadena
    @Query("SELECT c FROM Combo c WHERE CAST(c.id AS string) LIKE %:idStr%")
    List<Combo> findByIdContaining(String idStr);
    
    // Método para filtrar por estado y tipo
    List<Combo> findByEstadoAndTipo(EstadoCombo estado, Combo.TipoCombo tipo);
    
    // Método para filtrar por tipo
    List<Combo> findByTipo(Combo.TipoCombo tipo);
} 