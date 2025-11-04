package com.lsnls.repository;

import com.lsnls.entity.Cuestionario;
import com.lsnls.entity.Cuestionario.EstadoCuestionario;
import com.lsnls.entity.Cuestionario.NivelCuestionario;
import com.lsnls.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    
    // Obtener cuestionarios paginados usando JPQL con paginación manual
    @Query("SELECT c FROM Cuestionario c ORDER BY c.id DESC")
    List<Cuestionario> findAllPaginados(Pageable pageable);
    
    // Filtros por estado
    List<Cuestionario> findByEstadoOrderByIdDesc(EstadoCuestionario estado);
    
    // Filtros por estado con paginación
    Page<Cuestionario> findByEstado(EstadoCuestionario estado, Pageable pageable);
    
    // Filtros por temática
    List<Cuestionario> findByTematicaContainingIgnoreCaseOrderByIdDesc(String tematica);
    
    // Filtros por temática con paginación
    Page<Cuestionario> findByTematicaContainingIgnoreCase(String tematica, Pageable pageable);
    
    // Conteo por temática (exacto, case-insensitive)
    long countByTematicaIgnoreCase(String tematica);
    
    // Filtros combinados
    List<Cuestionario> findByEstadoAndTematicaContainingIgnoreCaseOrderByIdDesc(EstadoCuestionario estado, String tematica);
    
    // Filtros combinados con paginación
    Page<Cuestionario> findByEstadoAndTematicaContainingIgnoreCase(EstadoCuestionario estado, String tematica, Pageable pageable);
    
    // Métodos para gestión de temáticas
    @Query("SELECT DISTINCT c.tematica FROM Cuestionario c WHERE c.tematica IS NOT NULL AND c.tematica != '' ORDER BY c.tematica")
    List<String> findDistinctTematicas();
    
    List<Cuestionario> findByTematica(String tematica);
    
    long countByTematica(String tematica);
    
    // Método para buscar por ID que contenga una cadena
    @Query("SELECT c FROM Cuestionario c WHERE CAST(c.id AS string) LIKE %:idStr%")
    List<Cuestionario> findByIdContaining(String idStr);
    
    // Método para buscar por ID que contenga una cadena con paginación
    @Query("SELECT c FROM Cuestionario c WHERE CAST(c.id AS string) LIKE %:idStr%")
    Page<Cuestionario> findByIdContaining(String idStr, Pageable pageable);
} 