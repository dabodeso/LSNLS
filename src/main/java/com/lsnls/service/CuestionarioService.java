package com.lsnls.service;

import com.lsnls.entity.Cuestionario;
import com.lsnls.entity.Cuestionario.EstadoCuestionario;
import com.lsnls.entity.Cuestionario.NivelCuestionario;
import com.lsnls.entity.Pregunta;
import com.lsnls.entity.PreguntaCuestionario;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.CuestionarioRepository;
import com.lsnls.repository.PreguntaRepository;
import com.lsnls.repository.PreguntaCuestionarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import javax.persistence.EntityManager;
import com.lsnls.dto.CrearCuestionarioDTO;
import com.lsnls.dto.PreguntaCuestionarioDTO;
import com.lsnls.dto.PreguntaDTO;
import java.util.Map;
import java.util.HashMap;
import com.lsnls.service.TematicaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
@Transactional
public class CuestionarioService {
    
    @Autowired
    private CuestionarioRepository cuestionarioRepository;
    
    @Autowired
    private PreguntaRepository preguntaRepository;

    @Autowired
    private PreguntaCuestionarioRepository preguntaCuestionarioRepository;

    @Autowired
    private EntityManager entityManager;

    public boolean estaAsignadoAJornada(Long cuestionarioId) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(j) FROM Jornada j JOIN j.cuestionarios c WHERE c.id = :id", Long.class)
            .setParameter("id", cuestionarioId)
            .getSingleResult();
        return count != null && count > 0;
    }

    @Autowired
    private TematicaService tematicaService;

    public Cuestionario crear(Cuestionario cuestionario) {
        cuestionario.setFechaCreacion(LocalDateTime.now());
        cuestionario.setEstado(EstadoCuestionario.borrador);
        return cuestionarioRepository.save(cuestionario);
    }

    public List<Cuestionario> obtenerTodos() {
        return cuestionarioRepository.findAllOrderByIdDesc();
    }
    
    public Map<String, Object> obtenerTodosPaginados(int page, int size) {
        // Sincronizar estados con asignaciones de jornadas
        try { sincronizarEstadosAsignaciones(); } catch (Exception ignored) {}
        // Crear objeto Pageable para paginación
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        
        // Obtener el total de cuestionarios
        long totalCuestionarios = cuestionarioRepository.count();
        
        // Obtener cuestionarios paginados
        List<Cuestionario> cuestionariosPaginados = cuestionarioRepository.findAllPaginados(pageable);
        
        // Convertir a DTOs
        List<Map<String, Object>> dtos = new ArrayList<>();
        for (Cuestionario c : cuestionariosPaginados) {
            Map<String, Object> dto = obtenerCuestionarioConSlots(c.getId());
            if (dto != null) dtos.add(dto);
        }
        
        // Construir respuesta
        Map<String, Object> response = new HashMap<>();
        response.put("cuestionarios", dtos);
        response.put("currentPage", page);
        response.put("totalItems", totalCuestionarios);
        response.put("totalPages", Math.ceil((double) totalCuestionarios / size));
        
        return response;
    }

    public Optional<Cuestionario> obtenerPorId(Long id) {
        return cuestionarioRepository.findById(id);
    }

    public Optional<Cuestionario> obtenerConPreguntas(Long id) {
        try {
            @SuppressWarnings("unchecked")
            List<Cuestionario> resultados = entityManager.createQuery(
                "SELECT DISTINCT c FROM Cuestionario c " +
                "LEFT JOIN FETCH c.preguntas pc " +
                "LEFT JOIN FETCH pc.pregunta p " +
                "LEFT JOIN FETCH p.creacionUsuario " +
                "WHERE c.id = :cuestionarioId"
            ).setParameter("cuestionarioId", id).getResultList();
            
            if (resultados.isEmpty()) {
                return Optional.empty();
            }
            
            return Optional.of(resultados.get(0));
            
        } catch (Exception e) {
            System.err.println("Error al obtener cuestionario con preguntas: " + e.getMessage());
            return Optional.empty();
        }
    }

    public List<Cuestionario> obtenerPorEstado(EstadoCuestionario estado) {
        return cuestionarioRepository.findByEstado(estado);
    }

    public List<Cuestionario> obtenerPorNivel(NivelCuestionario nivel) {
        return cuestionarioRepository.findByNivel(nivel);
    }

    public List<Cuestionario> obtenerPorUsuario(Usuario usuario) {
        return cuestionarioRepository.findByCreacionUsuario(usuario);
    }

    public Cuestionario actualizar(Long id, Cuestionario cuestionario) {
        if (cuestionarioRepository.existsById(id)) {
            cuestionario.setId(id);
            return cuestionarioRepository.save(cuestionario);
        }
        return null;
    }

    public Cuestionario cambiarEstado(Long id, EstadoCuestionario nuevoEstado) {
        return cuestionarioRepository.findById(id).map(cuestionario -> {
            cuestionario.setEstado(nuevoEstado);
            return cuestionarioRepository.save(cuestionario);
        }).orElse(null);
    }

    public Cuestionario cambiarTematica(Long id, String nuevaTematica) {
        return cuestionarioRepository.findById(id).map(cuestionario -> {
            cuestionario.setTematica(nuevaTematica);
            return cuestionarioRepository.save(cuestionario);
        }).orElse(null);
    }

    // Métodos para gestión de temáticas de cuestionarios
    public List<String> obtenerTematicasDisponibles() {
        return tematicaService.obtenerNombresTematicas();
    }

    public void añadirTematica(String tematica) {
        // Validar que la temática no esté vacía
        if (tematica == null || tematica.trim().isEmpty()) {
            throw new IllegalArgumentException("La temática no puede estar vacía");
        }
        
        // Obtener el usuario actual (esto debería venir del contexto de seguridad)
        // Por ahora, creamos una temática sin usuario específico
        throw new RuntimeException("Use el endpoint específico de temáticas para añadir nuevas temáticas");
    }

    public void eliminarTematica(String tematica) {
        tematicaService.eliminarTematica(tematica);
    }

    public Map<String, Object> obtenerEstadisticasTematicas() {
        return tematicaService.obtenerEstadisticas();
    }

    /**
     * Obtiene cuestionarios disponibles para asignar: solo 'aprobado'.
     */
    public List<Cuestionario> obtenerDisponiblesParaConcursantes() {
        // Obtener cuestionarios aprobados, adjudicados o grabados (disponibles para asignar)
        List<Cuestionario> aprobados = cuestionarioRepository.findByEstado(EstadoCuestionario.aprobado);
        List<Cuestionario> adjudicados = cuestionarioRepository.findByEstado(EstadoCuestionario.adjudicado);
        List<Cuestionario> grabados = cuestionarioRepository.findByEstado(EstadoCuestionario.grabado);
        
        // Combinar todas las listas - permitir cualquiera de estos estados
        List<Cuestionario> disponibles = new ArrayList<>();
        disponibles.addAll(aprobados);
        disponibles.addAll(adjudicados);
        disponibles.addAll(grabados);
        
        // Ordenar por ID descendente (más recientes primero)
        disponibles.sort((a, b) -> b.getId().compareTo(a.getId()));
        return disponibles;
    }

    public boolean agregarPregunta(Long cuestionarioId, Long preguntaId, Integer factorMultiplicacion) {
        Optional<Cuestionario> cuestionarioOpt = cuestionarioRepository.findById(cuestionarioId);
        Optional<Pregunta> preguntaOpt = preguntaRepository.findById(preguntaId);
        
        if (cuestionarioOpt.isEmpty()) {
            throw new RuntimeException("Cuestionario no encontrado: " + cuestionarioId);
        }
        
        if (preguntaOpt.isEmpty()) {
            throw new RuntimeException("Pregunta no encontrada: " + preguntaId);
        }
        
        Cuestionario cuestionario = cuestionarioOpt.get();
        Pregunta pregunta = preguntaOpt.get();
        
        // Verificar que la pregunta esté aprobada; si está verificada, promover a aprobada automáticamente
        if (pregunta.getEstado() != Pregunta.EstadoPregunta.aprobada) {
            if (pregunta.getEstado() == Pregunta.EstadoPregunta.verificada) {
                entityManager.createNativeQuery("UPDATE preguntas SET estado = 'aprobada' WHERE id = ? AND estado = 'verificada'")
                    .setParameter(1, preguntaId)
                    .executeUpdate();
                // Refrescar entidad
                pregunta = preguntaRepository.findById(preguntaId).orElse(pregunta);
            } else {
                throw new RuntimeException("La pregunta debe estar aprobada para ser agregada a un cuestionario");
            }
        }
        
        // Verificar que la pregunta esté disponible o liberada (puede reutilizarse)
        // Tratar null como disponible para compatibilidad con datos antiguos
        if (pregunta.getEstadoDisponibilidad() != null &&
            pregunta.getEstadoDisponibilidad() != Pregunta.EstadoDisponibilidad.disponible && 
            pregunta.getEstadoDisponibilidad() != Pregunta.EstadoDisponibilidad.liberada) {
            throw new RuntimeException("La pregunta no está disponible (estado: " + pregunta.getEstadoDisponibilidad() + ")");
        }
        
        // Verificar que la pregunta no esté ya en este cuestionario
        PreguntaCuestionario.PreguntaCuestionarioId checkId = new PreguntaCuestionario.PreguntaCuestionarioId();
        checkId.setPreguntaId(preguntaId);
        checkId.setCuestionarioId(cuestionarioId);
        
        boolean yaExiste = preguntaCuestionarioRepository.existsById(checkId);
        
        if (yaExiste) {
            throw new RuntimeException("La pregunta ya está agregada a este cuestionario");
        }
        
        try {
            // Crear la relación pregunta-cuestionario
            PreguntaCuestionario pc = new PreguntaCuestionario();
            PreguntaCuestionario.PreguntaCuestionarioId id = new PreguntaCuestionario.PreguntaCuestionarioId();
            id.setPreguntaId(preguntaId);
            id.setCuestionarioId(cuestionarioId);
            
            pc.setId(id);
            pc.setPregunta(pregunta);
            pc.setCuestionario(cuestionario);
            pc.setFactorMultiplicacion(factorMultiplicacion != null ? factorMultiplicacion : 1);
            
            // Guardar la relación en la base de datos
            preguntaCuestionarioRepository.save(pc);
            
            // Marcar pregunta como usada usando consulta SQL directa para evitar validaciones
            entityManager.createNativeQuery(
                "UPDATE preguntas SET estado = 'usada', estado_disponibilidad = 'usada' WHERE id = ?")
                .setParameter(1, preguntaId)
                .executeUpdate();
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Error al agregar pregunta: " + e.getMessage());
            throw new RuntimeException("Error al agregar pregunta: " + e.getMessage());
        }
    }

    public boolean quitarPregunta(Long cuestionarioId, Long preguntaId) {
        Optional<Cuestionario> cuestionarioOpt = cuestionarioRepository.findById(cuestionarioId);
        Optional<Pregunta> preguntaOpt = preguntaRepository.findById(preguntaId);
        
        if (cuestionarioOpt.isPresent() && preguntaOpt.isPresent()) {
            Cuestionario cuestionario = cuestionarioOpt.get();
            Pregunta pregunta = preguntaOpt.get();
            
            // Verificar si existe la relación antes de eliminar
            PreguntaCuestionario.PreguntaCuestionarioId checkId = new PreguntaCuestionario.PreguntaCuestionarioId();
            checkId.setPreguntaId(preguntaId);
            checkId.setCuestionarioId(cuestionarioId);
            
            boolean existeRelacion = preguntaCuestionarioRepository.existsById(checkId);
            
            if (!existeRelacion) {
                return false;
            }
            
            // Eliminar la relación usando SQL directo para asegurar que funcione
            int filasEliminadas = entityManager.createNativeQuery(
                "DELETE FROM cuestionarios_preguntas WHERE cuestionario_id = ? AND pregunta_id = ?")
                .setParameter(1, cuestionarioId)
                .setParameter(2, preguntaId)
                .executeUpdate();
            
            // Liberar la pregunta usando consulta SQL directa
            if (pregunta.getEstadoDisponibilidad() == Pregunta.EstadoDisponibilidad.usada) {
                entityManager.createNativeQuery(
                    "UPDATE preguntas SET estado = 'aprobada', estado_disponibilidad = 'liberada' WHERE id = ?")
                    .setParameter(1, preguntaId)
                    .executeUpdate();
            }
            
            return filasEliminadas > 0;
        }
        
        return false;
    }

    public void eliminar(Long id) {
        // Verificar que el cuestionario existe
        Optional<Cuestionario> cuestionarioOpt = cuestionarioRepository.findById(id);
        if (cuestionarioOpt.isEmpty()) {
            throw new IllegalArgumentException("Cuestionario con ID " + id + " no encontrado");
        }

        Cuestionario cuestionario = cuestionarioOpt.get();

        // Verificar dependencias - no se puede eliminar si está adjudicado o grabado
        if (cuestionario.getEstado() == Cuestionario.EstadoCuestionario.adjudicado) {
            throw new IllegalArgumentException("No se puede eliminar el cuestionario porque está adjudicado a una jornada. Desasígnalo primero.");
        }
        if (cuestionario.getEstado() == Cuestionario.EstadoCuestionario.grabado) {
            throw new IllegalArgumentException("No se puede eliminar el cuestionario porque está grabado (asignado a concursantes). Desasígnalo primero.");
        }

        // Verificar si hay concursantes usando este cuestionario
        Long concursantesCount = entityManager.createQuery(
            "SELECT COUNT(c) FROM Concursante c WHERE c.cuestionario.id = :cuestionarioId", Long.class)
            .setParameter("cuestionarioId", id)
            .getSingleResult();
        
        if (concursantesCount > 0) {
            throw new IllegalArgumentException("No se puede eliminar el cuestionario porque está siendo usado por " + 
                concursantesCount + " concursante(s). Desasígnalo primero.");
        }

        // Verificar si está en alguna jornada
        Long jornadasCount = entityManager.createQuery(
            "SELECT COUNT(j) FROM Jornada j JOIN j.cuestionarios c WHERE c.id = :cuestionarioId", Long.class)
            .setParameter("cuestionarioId", id)
            .getSingleResult();
            
        if (jornadasCount > 0) {
            throw new IllegalArgumentException("No se puede eliminar el cuestionario porque está asignado a " + 
                jornadasCount + " jornada(s). Desasígnalo primero.");
        }

        // Eliminar registros del historial que referencian este cuestionario (si la tabla existe)
        try {
            // Verificar si la tabla existe antes de intentar eliminar
            Object result = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'historial_jornadas'")
                .getSingleResult();
            Long tableExists = ((Number) result).longValue();
            
            if (tableExists > 0) {
                // Verificar si la columna cuestionario_id existe
                Object columnExists = entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'historial_jornadas' AND column_name = 'cuestionario_id'")
                    .getSingleResult();
                Long columnExistsCount = ((Number) columnExists).longValue();
                
                if (columnExistsCount > 0) {
                    entityManager.createNativeQuery(
                        "DELETE FROM historial_jornadas WHERE cuestionario_id = ?")
                        .setParameter(1, id)
                        .executeUpdate();
                }
            }
        } catch (Exception e) {
            // Si hay error al eliminar el historial, continuamos de todas formas
            System.err.println("Advertencia: No se pudieron eliminar algunos registros del historial para el cuestionario " + id + ": " + e.getMessage());
        }

        // Si llegamos aquí, es seguro eliminar - liberar las preguntas asociadas
        Set<PreguntaCuestionario> preguntas = cuestionario.getPreguntas();
        for (PreguntaCuestionario pc : preguntas) {
            // Devolver a aprobada y marcar como liberada para poder reutilizar
            entityManager.createNativeQuery(
                "UPDATE preguntas SET estado = 'aprobada', estado_disponibilidad = 'liberada' WHERE id = ?")
                .setParameter(1, pc.getPregunta().getId())
                .executeUpdate();
        }
        
        cuestionarioRepository.deleteById(id);
    }

    public void eliminarPorId(Long id) {
        cuestionarioRepository.deleteById(id);
    }

    public Cuestionario actualizarNotasDireccion(Long id, String notasDireccion) {
        Optional<Cuestionario> cuestionarioOpt = cuestionarioRepository.findById(id);
        if (cuestionarioOpt.isPresent()) {
            Cuestionario cuestionario = cuestionarioOpt.get();
            cuestionario.setNotasDireccion(notasDireccion);
            return cuestionarioRepository.save(cuestionario);
        }
        throw new RuntimeException("Cuestionario no encontrado con ID: " + id);
    }

    public Map<String, Object> filtrarCuestionarios(String estado, String tematica, String subtema, String texto, int page, int size) {
        try { sincronizarEstadosAsignaciones(); } catch (Exception ignored) {}
        // Crear objeto Pageable para paginación
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        
        Page<Cuestionario> paginaCuestionarios;
        
        // Si hay texto, buscar en preguntas y respuestas usando consulta nativa
        if (texto != null && !texto.trim().isEmpty()) {
            String textoBusqueda = "%" + texto.trim().toLowerCase() + "%";
            
            // Construir consulta nativa para buscar cuestionarios que tengan preguntas o respuestas con el texto
            String sql = "SELECT DISTINCT c.id FROM cuestionarios c " +
                        "INNER JOIN cuestionarios_preguntas cp ON c.id = cp.cuestionario_id " +
                        "INNER JOIN preguntas p ON cp.pregunta_id = p.id " +
                        "WHERE (LOWER(p.pregunta) LIKE :texto OR LOWER(p.respuesta) LIKE :texto)";
            
            if (estado != null && !estado.isEmpty()) {
                sql += " AND c.estado = :estado";
            }
            if (tematica != null && !tematica.isEmpty()) {
                sql += " AND LOWER(c.tematica) LIKE :tematica";
            }
            if (subtema != null && !subtema.isEmpty()) {
                sql += " AND LOWER(p.subtema) LIKE :subtema";
            }
            
            sql += " ORDER BY c.id DESC";
            
            // Ejecutar consulta nativa
            javax.persistence.Query query = entityManager.createNativeQuery(sql);
            query.setParameter("texto", textoBusqueda);
            if (estado != null && !estado.isEmpty()) {
                query.setParameter("estado", estado);
            }
            if (tematica != null && !tematica.isEmpty()) {
                query.setParameter("tematica", "%" + tematica.toLowerCase() + "%");
            }
            if (subtema != null && !subtema.isEmpty()) {
                query.setParameter("subtema", "%" + subtema.toLowerCase() + "%");
            }
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
            
            @SuppressWarnings("unchecked")
            List<Number> resultadoIds = query.getResultList();
            
            // Obtener el total de resultados
            String countSql = "SELECT COUNT(DISTINCT c.id) FROM cuestionarios c " +
                             "INNER JOIN cuestionarios_preguntas cp ON c.id = cp.cuestionario_id " +
                             "INNER JOIN preguntas p ON cp.pregunta_id = p.id " +
                             "WHERE (LOWER(p.pregunta) LIKE :texto OR LOWER(p.respuesta) LIKE :texto)";
            if (estado != null && !estado.isEmpty()) {
                countSql += " AND c.estado = :estado";
            }
            if (tematica != null && !tematica.isEmpty()) {
                countSql += " AND LOWER(c.tematica) LIKE :tematica";
            }
            if (subtema != null && !subtema.isEmpty()) {
                countSql += " AND LOWER(p.subtema) LIKE :subtema";
            }
            
            javax.persistence.Query countQuery = entityManager.createNativeQuery(countSql);
            countQuery.setParameter("texto", textoBusqueda);
            if (estado != null && !estado.isEmpty()) {
                countQuery.setParameter("estado", estado);
            }
            if (tematica != null && !tematica.isEmpty()) {
                countQuery.setParameter("tematica", "%" + tematica.toLowerCase() + "%");
            }
            if (subtema != null && !subtema.isEmpty()) {
                countQuery.setParameter("subtema", "%" + subtema.toLowerCase() + "%");
            }
            
            long total = ((Number) countQuery.getSingleResult()).longValue();
            
            // Convertir resultados a cuestionarios
            List<Cuestionario> cuestionarios = new ArrayList<>();
            for (Number id : resultadoIds) {
                Optional<Cuestionario> cuestionarioOpt = cuestionarioRepository.findById(id.longValue());
                if (cuestionarioOpt.isPresent()) {
                    cuestionarios.add(cuestionarioOpt.get());
                }
            }
            
            // Convertir a DTOs
            List<Map<String, Object>> dtos = new ArrayList<>();
            for (Cuestionario c : cuestionarios) {
                Map<String, Object> dto = obtenerCuestionarioConSlots(c.getId());
                if (dto != null) dtos.add(dto);
            }
            
            // Construir respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("cuestionarios", dtos);
            response.put("currentPage", page);
            response.put("totalItems", total);
            response.put("totalPages", (int) Math.ceil((double) total / size));
            
            return response;
        }
        
        // Aplicar filtros y paginación directamente en la consulta a la base de datos
        if (estado != null && !estado.isEmpty() && tematica != null && !tematica.isEmpty()) {
            // Filtrar por estado y temática
            EstadoCuestionario estadoEnum = EstadoCuestionario.valueOf(estado);
            paginaCuestionarios = cuestionarioRepository.findByEstadoAndTematicaContainingIgnoreCase(
                estadoEnum, tematica, pageable);
        } else if (estado != null && !estado.isEmpty()) {
            // Filtrar solo por estado
            EstadoCuestionario estadoEnum = EstadoCuestionario.valueOf(estado);
            paginaCuestionarios = cuestionarioRepository.findByEstado(estadoEnum, pageable);
        } else if (tematica != null && !tematica.isEmpty()) {
            // Filtrar solo por temática
            paginaCuestionarios = cuestionarioRepository.findByTematicaContainingIgnoreCase(tematica, pageable);
        } else {
            // Si no hay filtros, usar la paginación existente
            return obtenerTodosPaginados(page, size);
        }
        
        // Convertir a DTOs
        List<Map<String, Object>> dtos = new ArrayList<>();
        for (Cuestionario c : paginaCuestionarios.getContent()) {
            Map<String, Object> dto = obtenerCuestionarioConSlots(c.getId());
            if (dto != null) dtos.add(dto);
        }
        
        // Construir respuesta
        Map<String, Object> response = new HashMap<>();
        response.put("cuestionarios", dtos);
        response.put("currentPage", paginaCuestionarios.getNumber());
        response.put("totalItems", paginaCuestionarios.getTotalElements());
        response.put("totalPages", paginaCuestionarios.getTotalPages());
        
        System.out.println("Filtrado con paginación optimizada - Página: " + page + ", Tamaño: " + size + 
                          ", Total: " + paginaCuestionarios.getTotalElements() + 
                          ", Cuestionarios en esta página: " + dtos.size());
        
        return response;
    }

    /** Sincroniza estados adjudicado/aprobado con asignaciones de jornada (lote) */
    private void sincronizarEstadosAsignaciones() {
        // Cuestionarios adjudicados por estar en jornadas
        // IMPORTANTE: no tocar los que ya están en 'aprobado' porque pueden estar marcados como reutilizados/liberados
        entityManager.createNativeQuery(
            "UPDATE cuestionarios SET estado='adjudicado' " +
            "WHERE id IN (SELECT cuestionario_id FROM jornadas_cuestionarios) " +
            "AND estado NOT IN ('adjudicado','grabado','aprobado')")
            .executeUpdate();
        // Cuestionarios sin jornada → aprobado (solo si estaban adjudicados)
        entityManager.createNativeQuery(
            "UPDATE cuestionarios SET estado='aprobado' WHERE estado='adjudicado' AND id NOT IN (SELECT cuestionario_id FROM jornadas_cuestionarios)")
            .executeUpdate();
    }
    
    public Map<String, Object> filtrarCuestionariosPorId(String idStr, int page, int size) {
        // Crear objeto Pageable para paginación
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        
        try {
            // Intentar buscar por ID exacto
            Long id = Long.parseLong(idStr);
            Optional<Cuestionario> cuestionario = cuestionarioRepository.findById(id);
            
            if (cuestionario.isPresent()) {
                // Para búsqueda exacta por ID, creamos una respuesta con un solo elemento
                List<Map<String, Object>> dtos = new ArrayList<>();
                Map<String, Object> dto = obtenerCuestionarioConSlots(id);
                if (dto != null) {
                    dtos.add(dto);
                }
                
                // Construir respuesta para un solo elemento
                Map<String, Object> response = new HashMap<>();
                response.put("cuestionarios", dtos);
                response.put("currentPage", 0);
                response.put("totalItems", 1);
                response.put("totalPages", 1);
                
                System.out.println("Búsqueda exacta por ID - ID: " + id + ", Encontrado: " + (dto != null));
                
                return response;
            } else {
                // No se encontró el cuestionario con el ID exacto
                Map<String, Object> response = new HashMap<>();
                response.put("cuestionarios", new ArrayList<>());
                response.put("currentPage", 0);
                response.put("totalItems", 0);
                response.put("totalPages", 0);
                
                System.out.println("Búsqueda exacta por ID - ID: " + id + ", No encontrado");
                
                return response;
            }
        } catch (NumberFormatException e) {
            // Si no es un número, buscar por coincidencia parcial usando paginación nativa
            // Implementar método en el repositorio para buscar por ID con paginación
            Page<Cuestionario> paginaCuestionarios = cuestionarioRepository.findByIdContaining(idStr, pageable);
            
            // Convertir a DTOs
            List<Map<String, Object>> dtos = new ArrayList<>();
            for (Cuestionario c : paginaCuestionarios.getContent()) {
                Map<String, Object> dto = obtenerCuestionarioConSlots(c.getId());
                if (dto != null) dtos.add(dto);
            }
            
            // Construir respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("cuestionarios", dtos);
            response.put("currentPage", paginaCuestionarios.getNumber());
            response.put("totalItems", paginaCuestionarios.getTotalElements());
            response.put("totalPages", paginaCuestionarios.getTotalPages());
            
            System.out.println("Búsqueda parcial por ID - Término: " + idStr + 
                              ", Página: " + page + ", Tamaño: " + size + 
                              ", Total: " + paginaCuestionarios.getTotalElements() + 
                              ", Cuestionarios en esta página: " + dtos.size());
            
            return response;
        }
    }

    // Método auxiliar para debug
    public Optional<Pregunta> obtenerPreguntaPorId(Long id) {
        return preguntaRepository.findById(id);
    }

    // Método para verificar preguntas usando SQL directo
    @SuppressWarnings("unchecked")
    public List<Object[]> obtenerPreguntasPorCuestionarioSQL(Long cuestionarioId) {
        return entityManager.createNativeQuery(
            "SELECT cp.pregunta_id, cp.cuestionario_id, cp.factor_multiplicacion, p.pregunta, p.respuesta " +
            "FROM cuestionarios_preguntas cp " +
            "JOIN preguntas p ON cp.pregunta_id = p.id " +
            "WHERE cp.cuestionario_id = ?"
        ).setParameter(1, cuestionarioId).getResultList();
    }

    /**
     * Verifica y reserva múltiples preguntas atómicamente para evitar conflictos de concurrencia
     * @param preguntaIds Lista de IDs de preguntas a verificar y reservar
     * @return true si todas las preguntas fueron reservadas exitosamente
     * @throws IllegalArgumentException si alguna pregunta no está disponible
     */
    @Transactional
    public boolean verificarYReservarPreguntasAtomico(List<Long> preguntaIds) {
        // Verificar que la lista no sea nula
        if (preguntaIds == null || preguntaIds.isEmpty()) {
            return true; // No hay nada que reservar
        }

        // Normalizar IDs: quitar nulos y duplicados para evitar falsos positivos
        java.util.List<Long> idsNormalizados = new java.util.ArrayList<>();
        java.util.Set<Long> vistos = new java.util.LinkedHashSet<>();
        for (Long id : preguntaIds) {
            if (id == null) continue;
            if (vistos.add(id)) {
                idsNormalizados.add(id);
            }
        }

        if (idsNormalizados.isEmpty()) {
            return true;
        }

        // PASO 1: Verificar que todas las preguntas existen y están en estado correcto
        List<Pregunta> preguntas = preguntaRepository.findAllById(idsNormalizados);

        if (preguntas.size() != idsNormalizados.size()) {
            // Construir una lista explícita de IDs que faltan para facilitar el debug en el frontend
            java.util.Set<Long> encontrados = new java.util.HashSet<>();
            for (Pregunta p : preguntas) {
                if (p != null && p.getId() != null) {
                    encontrados.add(p.getId());
                }
            }
            java.util.List<Long> faltantes = new java.util.ArrayList<>();
            for (Long id : idsNormalizados) {
                if (id != null && !encontrados.contains(id)) {
                    faltantes.add(id);
                }
            }
            String detalle = faltantes.isEmpty()
                ? ""
                : " (IDs faltantes: " + faltantes + ")";
            throw new IllegalArgumentException("Una o más preguntas no fueron encontradas" + detalle);
        }
        
        // Verificar el estado de cada pregunta (promover 'verificada' -> 'aprobada' si aplica)
        for (Pregunta pregunta : preguntas) {
            if (pregunta.getEstado() != Pregunta.EstadoPregunta.aprobada) {
                if (pregunta.getEstado() == Pregunta.EstadoPregunta.verificada) {
                    entityManager.createNativeQuery("UPDATE preguntas SET estado = 'aprobada' WHERE id = ? AND estado = 'verificada'")
                        .setParameter(1, pregunta.getId())
                        .executeUpdate();
                    // Actualizar objeto en memoria
                    pregunta.setEstado(Pregunta.EstadoPregunta.aprobada);
                } else {
                    throw new IllegalArgumentException("La pregunta " + pregunta.getId() + " no está aprobada (estado: " + pregunta.getEstado() + ")");
                }
            }
            
            // Tratar null como disponible para compatibilidad con datos antiguos
            if (pregunta.getEstadoDisponibilidad() != null &&
                pregunta.getEstadoDisponibilidad() != Pregunta.EstadoDisponibilidad.disponible && 
                pregunta.getEstadoDisponibilidad() != Pregunta.EstadoDisponibilidad.liberada) {
                throw new IllegalArgumentException("La pregunta " + pregunta.getId() + " no está disponible (estado: " + pregunta.getEstadoDisponibilidad() + ")");
            }
            
            // Verificar que sea pregunta de nivel 1-4 para cuestionarios
            if (pregunta.getNivel().name().startsWith("_5")) {
                throw new IllegalArgumentException("La pregunta " + pregunta.getId() + " es de nivel 5 y debe ir en combos, no en cuestionarios");
            }
        }
        
        // PASO 2: Reservar todas las preguntas ATÓMICAMENTE con una sola query
        String preguntaIdsStr = idsNormalizados.stream()
            .map(String::valueOf)
            .reduce((a, b) -> a + "," + b)
            .orElse("");
            
        int preguntasReservadas = entityManager.createNativeQuery(
            "UPDATE preguntas SET estado = 'usada', estado_disponibilidad = 'usada' " +
            "WHERE id IN (" + preguntaIdsStr + ") " +
            "AND (estado_disponibilidad IN ('disponible', 'liberada') OR estado_disponibilidad IS NULL) " +
            "AND estado = 'aprobada'"
        ).executeUpdate();
        
        // PASO 3: Verificar que se reservaron TODAS las preguntas
        if (preguntasReservadas != idsNormalizados.size()) {
            // Rollback - alguna pregunta fue tomada por otro usuario
            throw new IllegalStateException("Conflicto de concurrencia: " + (idsNormalizados.size() - preguntasReservadas) + 
                " pregunta(s) fueron reservadas por otro usuario. Por favor, verifica la disponibilidad e intenta nuevamente.");
        }
        
        return true;
    }

    /**
     * Libera múltiples preguntas atómicamente (rollback en caso de error)
     */
    @Transactional
    public void liberarPreguntasAtomico(List<Long> preguntaIds) {
        // Normalizar también aquí para evitar duplicados innecesarios
        String preguntaIdsStr = preguntaIds.stream()
            .filter(java.util.Objects::nonNull)
            .distinct()
            .map(String::valueOf)
            .reduce((a, b) -> a + "," + b)
            .orElse("");
            
        entityManager.createNativeQuery(
            "UPDATE preguntas SET estado = 'aprobada', estado_disponibilidad = 'liberada' " +
            "WHERE id IN (" + preguntaIdsStr + ") AND estado_disponibilidad = 'usada'"
        ).executeUpdate();
    }

    public Cuestionario crearDesdeDTO(CrearCuestionarioDTO dto, Usuario usuario) {
        // PASO 1: VERIFICACIÓN Y RESERVA ATÓMICA de todas las preguntas
        try {
            verificarYReservarPreguntasAtomico(dto.getPreguntasNormales());
        } catch (IllegalStateException e) {
            // Error de concurrencia - mensaje específico
            throw new IllegalArgumentException("Error de concurrencia al reservar preguntas: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // Error de validación - reenviar tal como está
            throw e;
        }
        
        // PASO 2: Crear el cuestionario (las preguntas ya están reservadas)
        Cuestionario cuestionario = new Cuestionario();
        cuestionario.setCreacionUsuario(usuario);
        cuestionario.setEstado(Cuestionario.EstadoCuestionario.borrador);
        cuestionario.setFechaCreacion(LocalDateTime.now());
        cuestionario.setNivel(Cuestionario.NivelCuestionario.NORMAL);
        cuestionario.setTematica(dto.getTematica());
        cuestionario.setNotasDireccion(dto.getNotasDireccion());
        cuestionario = cuestionarioRepository.save(cuestionario);

        // PASO 3: Crear las relaciones pregunta-cuestionario (preguntas ya reservadas)
        try {
            for (Long idPregunta : dto.getPreguntasNormales()) {
                Pregunta pregunta = preguntaRepository.findById(idPregunta)
                    .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada: " + idPregunta));
                
                PreguntaCuestionario pc = new PreguntaCuestionario();
                PreguntaCuestionario.PreguntaCuestionarioId pcid = new PreguntaCuestionario.PreguntaCuestionarioId();
                pcid.setPreguntaId(idPregunta);
                pcid.setCuestionarioId(cuestionario.getId());
                pc.setId(pcid);
                pc.setPregunta(pregunta);
                pc.setCuestionario(cuestionario);
                pc.setFactorMultiplicacion(1);
                preguntaCuestionarioRepository.save(pc);
                // Nota: La pregunta ya fue marcada como 'usada' en verificarYReservarPreguntasAtomico()
            }
            return cuestionarioRepository.findById(cuestionario.getId()).orElse(cuestionario);
        } catch (Exception e) {
            // En caso de error, liberar las preguntas reservadas
            liberarPreguntasAtomico(dto.getPreguntasNormales());
            // Eliminar el cuestionario creado
            cuestionarioRepository.deleteById(cuestionario.getId());
            throw new RuntimeException("Error al crear relaciones pregunta-cuestionario: " + e.getMessage());
        }
    }

    public Cuestionario actualizarDesdeDTO(Long id, CrearCuestionarioDTO dto) {
        Optional<Cuestionario> optCuestionario = cuestionarioRepository.findById(id);
        if (optCuestionario.isEmpty()) {
            throw new IllegalArgumentException("Cuestionario no encontrado");
        }
        Cuestionario cuestionario = optCuestionario.get();

        if (dto.getVersion() != null) {
            cuestionario.setVersion(dto.getVersion());
        }

        // Conjuntos para calcular diferencias
        Set<Long> actualesIds = new HashSet<>();
        for (PreguntaCuestionario pc : cuestionario.getPreguntas()) {
            actualesIds.add(pc.getPregunta().getId());
        }
        Set<Long> nuevosIds = new HashSet<>(dto.getPreguntasNormales());

        // Calcular listas: reservar solo nuevas; liberar solo eliminadas; mantener intersección
        List<Long> aReservar = new ArrayList<>();
        for (Long pid : nuevosIds) {
            if (!actualesIds.contains(pid)) aReservar.add(pid);
        }
        List<Long> aLiberar = new ArrayList<>();
        for (Long pid : actualesIds) {
            if (!nuevosIds.contains(pid)) aLiberar.add(pid);
        }

        // PASO 1: Verificar y reservar SOLO las nuevas preguntas
        try {
            verificarYReservarPreguntasAtomico(aReservar);
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("Error de concurrencia al reservar preguntas: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw e;
        }

        // PASO 2: Eliminar relaciones solo de las preguntas a liberar
        if (!aLiberar.isEmpty()) {
            // Eliminar relaciones y liberar disponibilidad
            for (PreguntaCuestionario pc : new HashSet<>(cuestionario.getPreguntas())) {
                if (aLiberar.contains(pc.getPregunta().getId())) {
                    preguntaCuestionarioRepository.delete(pc);
                    cuestionario.getPreguntas().remove(pc);
                }
            }
            liberarPreguntasAtomico(aLiberar);
        }

        // PASO 3: Crear relaciones SOLO para las nuevas (aReservar)
        try {
            for (Long idPregunta : aReservar) {
                Pregunta pregunta = preguntaRepository.findById(idPregunta)
                    .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada: " + idPregunta));

                PreguntaCuestionario pc = new PreguntaCuestionario();
                PreguntaCuestionario.PreguntaCuestionarioId pcid = new PreguntaCuestionario.PreguntaCuestionarioId();
                pcid.setPreguntaId(idPregunta);
                pcid.setCuestionarioId(cuestionario.getId());
                pc.setId(pcid);
                pc.setPregunta(pregunta);
                pc.setCuestionario(cuestionario);
                pc.setFactorMultiplicacion(1);
                preguntaCuestionarioRepository.save(pc);
                cuestionario.getPreguntas().add(pc);
            }

            // Actualizar campos
            cuestionario.setTematica(dto.getTematica());
            cuestionario.setNotasDireccion(dto.getNotasDireccion());
            cuestionarioRepository.save(cuestionario);

            return cuestionarioRepository.findById(cuestionario.getId()).orElse(cuestionario);
        } catch (Exception e) {
            // Revertir reserva de nuevas si falla
            liberarPreguntasAtomico(aReservar);
            throw new RuntimeException("Error al actualizar relaciones pregunta-cuestionario: " + e.getMessage());
        }
    }

    /**
     * Devuelve un cuestionario con las preguntas mapeadas a DTOs con slot/hueco.
     */
    public Map<String, Object> obtenerCuestionarioConSlots(Long id) {
        Optional<Cuestionario> opt = obtenerConPreguntas(id);
        if (opt.isEmpty()) return null;
        Cuestionario c = opt.get();
        Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("id", c.getId());
        dto.put("version", c.getVersion());
        dto.put("estado", c.getEstado());
        dto.put("fechaCreacion", c.getFechaCreacion() != null ? c.getFechaCreacion().toString() : null);
        // Campos visibles en la tabla
        dto.put("tematica", c.getTematica());
        dto.put("notasDireccion", c.getNotasDireccion());
        // Jornada asignada (si existe)
        try {
            Long jornadaId = entityManager.createQuery(
                "SELECT j.id FROM Jornada j JOIN j.cuestionarios cu WHERE cu.id = :id", Long.class)
                .setParameter("id", id)
                .setMaxResults(1)
                .getResultList()
                .stream().findFirst().orElse(null);
            if (jornadaId != null) {
                dto.put("jornadaAsignada", jornadaId);
            }
        } catch (Exception ignored) {}
        
        // Verificar si fue reutilizado de alguna jornada
        try {
            @SuppressWarnings("unchecked")
            java.util.List<Object[]> historialReutilizado = entityManager.createNativeQuery(
                "SELECT h.jornada_id, j.nombre " +
                "FROM historial_jornadas h " +
                "JOIN jornadas j ON h.jornada_id = j.id " +
                "WHERE h.cuestionario_id = :cid AND h.estado_asignacion = 'reaprovechado' " +
                "ORDER BY h.fecha_asignacion DESC " +
                "LIMIT 1")
                .setParameter("cid", id)
                .getResultList();
            
            if (!historialReutilizado.isEmpty()) {
                Object[] registro = historialReutilizado.get(0);
                dto.put("reutilizadoDeJornadaId", ((Number) registro[0]).longValue());
                dto.put("reutilizadoDeJornadaNombre", (String) registro[1]);
                System.out.println("[DTO-CUEST] Cuest " + id + " | estado=" + c.getEstado() + " | jornada=" + dto.get("jornadaAsignada") + " | reutilizadoDe=" + dto.get("reutilizadoDeJornadaId"));
            }
        } catch (Exception e) {
            System.err.println("[DTO-CUEST] ERROR al buscar historial para cuest " + id);
        }
        
        // Mapear preguntas a slots según su nivel real
        java.util.Map<String, PreguntaCuestionarioDTO> mapPorSlot = new java.util.HashMap<>();
        
        // Mapear cada pregunta a su slot correspondiente según su nivel
        for (PreguntaCuestionario pc : c.getPreguntas()) {
            PreguntaCuestionarioDTO pcdto = new PreguntaCuestionarioDTO();
            Pregunta p = pc.getPregunta();
            pcdto.setPregunta(mapPreguntaToDTO(p));
            pcdto.setFactorMultiplicacion(pc.getFactorMultiplicacion());
            
            // Determinar slot basado en el nivel real de la pregunta
            String slot = null;
            if (pc.getFactorMultiplicacion() == null || pc.getFactorMultiplicacion() == 1) {
                // Mapear según el nivel real de la pregunta
                String nivelPregunta = p.getNivel().name();
                switch (nivelPregunta) {
                    case "_1LS":
                        slot = "1LS";
                        break;
                    case "_2NLS":
                        slot = "2NLS";
                        break;
                    case "_3LS":
                        slot = "3LS";
                        break;
                    case "_4NLS":
                        slot = "4NLS";
                        break;
                    default:
                        // Si no es un nivel válido para cuestionarios, no asignar slot
                        continue;
                }
            }
            
            pcdto.setSlot(slot);
            if (slot != null) {
                mapPorSlot.put(slot, pcdto);
            }
        }
        
        // Asegurar los 4 slots en orden correcto
        java.util.List<PreguntaCuestionarioDTO> preguntasDTO = new java.util.ArrayList<>();
        for (String slot : new String[]{"1LS","2NLS","3LS","4NLS"}) {
            if (mapPorSlot.containsKey(slot)) {
                preguntasDTO.add(mapPorSlot.get(slot));
            } else {
                // Slot vacío
                PreguntaCuestionarioDTO vacio = new PreguntaCuestionarioDTO();
                vacio.setSlot(slot);
                vacio.setPregunta(null);
                vacio.setFactorMultiplicacion(null);
                preguntasDTO.add(vacio);
            }
        }
        dto.put("preguntas", preguntasDTO);
        return dto;
    }

    private PreguntaDTO mapPreguntaToDTO(Pregunta p) {
        PreguntaDTO dto = new PreguntaDTO();
        dto.setId(p.getId());
        dto.setTematica(p.getTematica());
        dto.setPregunta(p.getPregunta());
        dto.setRespuesta(p.getRespuesta());
        dto.setDatosExtra(p.getDatosExtra());
        dto.setFuentes(p.getFuentes());
        dto.setNivel(p.getNivel());
        dto.setCreacionUsuarioId(p.getCreacionUsuario() != null ? p.getCreacionUsuario().getId() : null);
        dto.setSubtema(p.getSubtema());
        dto.setNotas(p.getNotas());
        dto.setFactor(p.getFactor());
        dto.setNotasVerificacion(p.getNotasVerificacion());
        dto.setNotasDireccion(p.getNotasDireccion());
        dto.setFechaCreacion(p.getFechaCreacion() != null ? p.getFechaCreacion().toString() : null);
        dto.setFechaVerificacion(p.getFechaVerificacion() != null ? p.getFechaVerificacion().toString() : null);
        return dto;
    }

    public boolean quitarPreguntaPorSlot(Long cuestionarioId, String slot) {
        Optional<Cuestionario> cuestionarioOpt = obtenerConPreguntas(cuestionarioId);
        if (cuestionarioOpt.isEmpty()) {
            return false;
        }
        
        Cuestionario cuestionario = cuestionarioOpt.get();
        
        // Buscar la pregunta en el slot especificado
        for (PreguntaCuestionario pc : cuestionario.getPreguntas()) {
            Pregunta p = pc.getPregunta();
            String nivelPregunta = p.getNivel().name();
            String slotPregunta = null;
            
            // Mapear nivel a slot
            switch (nivelPregunta) {
                case "_1LS":
                    slotPregunta = "1LS";
                    break;
                case "_2NLS":
                    slotPregunta = "2NLS";
                    break;
                case "_3LS":
                    slotPregunta = "3LS";
                    break;
                case "_4NLS":
                    slotPregunta = "4NLS";
                    break;
            }
            
            if (slot.equals(slotPregunta)) {
                // Encontramos la pregunta en el slot, la eliminamos
                return quitarPregunta(cuestionarioId, p.getId());
            }
        }
        
        return false; // No se encontró pregunta en ese slot
    }

    /**
     * Valida que un cuestionario esté en el estado esperado para prevenir conflictos de concurrencia
     */
    private void validarEstadoCuestionarioParaAsignacion(Long cuestionarioId, Cuestionario.EstadoCuestionario estadoEsperado) {
        Optional<Cuestionario> cuestionarioOpt = cuestionarioRepository.findById(cuestionarioId);
        if (cuestionarioOpt.isEmpty()) {
            throw new IllegalArgumentException("Cuestionario con ID " + cuestionarioId + " no encontrado");
        }
        
        Cuestionario cuestionario = cuestionarioOpt.get();
        if (cuestionario.getEstado() != estadoEsperado) {
            throw new IllegalStateException("El cuestionario " + cuestionarioId + " no está en estado '" + estadoEsperado + 
                "'. Estado actual: '" + cuestionario.getEstado() + "'. Otro usuario pudo haberlo modificado.");
        }
    }

    /**
     * Cambia el estado de un cuestionario de forma atómica con validación de concurrencia
     */
    @Transactional
    public boolean cambiarEstadoAtomico(Long cuestionarioId, Cuestionario.EstadoCuestionario estadoActualEsperado, 
                                       Cuestionario.EstadoCuestionario nuevoEstado) {
        // Usar query nativa para cambio atómico con verificación de estado
        int filasActualizadas = entityManager.createNativeQuery(
            "UPDATE cuestionarios SET estado = ? WHERE id = ? AND estado = ?")
            .setParameter(1, nuevoEstado.name())
            .setParameter(2, cuestionarioId)
            .setParameter(3, estadoActualEsperado.name())
            .executeUpdate();
            
        if (filasActualizadas == 0) {
            throw new IllegalStateException("No se pudo cambiar el estado del cuestionario " + cuestionarioId + 
                " porque otro usuario lo modificó simultáneamente. Estado esperado: " + estadoActualEsperado);
        }
        
        return true;
    }
} 