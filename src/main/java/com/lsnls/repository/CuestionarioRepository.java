package com.lsnls.repository;

import com.lsnls.entity.Cuestionario;
import com.lsnls.entity.Cuestionario.EstadoCuestionario;
import com.lsnls.entity.Cuestionario.NivelCuestionario;
import com.lsnls.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CuestionarioRepository extends JpaRepository<Cuestionario, Long> {
    List<Cuestionario> findByEstado(EstadoCuestionario estado);
    
    List<Cuestionario> findByNivel(NivelCuestionario nivel);
    
    List<Cuestionario> findByCreacionUsuario(Usuario usuario);
    
    @Query("SELECT c FROM Cuestionario c WHERE c.estado = :estado AND c.nivel = :nivel")
    List<Cuestionario> findByEstadoAndNivel(EstadoCuestionario estado, NivelCuestionario nivel);
    
    // Obtener todos los cuestionarios ordenados por ID descendente (más recientes primero)
    @Query("SELECT c FROM Cuestionario c ORDER BY c.id DESC")
    List<Cuestionario> findAllOrderByIdDesc();
} 