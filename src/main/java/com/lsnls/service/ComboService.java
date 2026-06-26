package com.lsnls.service;

import com.lsnls.entity.Combo;
import com.lsnls.entity.Combo.EstadoCombo;
import com.lsnls.entity.Combo.NivelCombo;
import com.lsnls.entity.Pregunta;
import com.lsnls.entity.PreguntaCombo;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.ComboRepository;
import com.lsnls.repository.PreguntaRepository;
import com.lsnls.repository.PreguntaComboRepository;
import com.lsnls.repository.TematicaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import javax.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.lsnls.dto.CrearComboDTO;

@Service
@Transactional
public class ComboService {
    
    @Autowired
    private ComboRepository comboRepository;
    
    @Autowired
    private PreguntaRepository preguntaRepository;

    @Autowired
    private PreguntaComboRepository preguntaComboRepository;
    
    @Autowired
    private TematicaRepository tematicaRepository;

    @Autowired
    private EntityManager entityManager;

    public Combo crear(Combo combo) {
        combo.setFechaCreacion(LocalDateTime.now());
        combo.setEstado(EstadoCombo.borrador);
        return comboRepository.save(combo);
    }

    public List<Combo> obtenerTodos() {
        return comboRepository.findAll();
    }
    
    public Map<String, Object> obtenerTodosPaginados(int page, int size) {
        // Sincronizar estados con asignaciones de jornadas
        try { sincronizarEstadosAsignaciones(); } catch (Exception ignored) {}
        // Crear objeto Pageable para paginaci?n
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        
        // Obtener el total de combos
        long totalCombos = comboRepository.count();
        
        // Obtener combos paginados
        List<Combo> combosPaginados = comboRepository.findAllPaginados(pageable);
        
        // Convertir a DTOs
        List<Map<String, Object>> dtos = new java.util.ArrayList<>();
        for (Combo c : combosPaginados) {
            Map<String, Object> dto = obtenerComboConSlots(c.getId());
            if (dto != null) dtos.add(dto);
        }
        
        // Construir respuesta
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("combos", dtos);
        response.put("currentPage", page);
        response.put("totalItems", totalCombos);
        response.put("totalPages", Math.ceil((double) totalCombos / size));
        
        return response;
    }

    public Optional<Combo> obtenerPorId(Long id) {
        return comboRepository.findById(id);
    }

    public Optional<Combo> obtenerConPreguntas(Long id) {
        try {
            @SuppressWarnings("unchecked")
            List<Combo> resultados = entityManager.createQuery(
                "SELECT DISTINCT c FROM Combo c " +
                "LEFT JOIN FETCH c.preguntas pc " +
                "LEFT JOIN FETCH pc.pregunta p " +
                "LEFT JOIN FETCH p.creacionUsuario " +
                "WHERE c.id = :comboId"
            ).setParameter("comboId", id).getResultList();
            
            if (resultados.isEmpty()) {
                return Optional.empty();
            }
            
            return Optional.of(resultados.get(0));
            
        } catch (Exception e) {
            System.err.println("Error al obtener combo con preguntas: " + e.getMessage());
            return Optional.empty();
        }
    }

    public List<Combo> obtenerPorEstado(EstadoCombo estado) {
        return comboRepository.findByEstado(estado);
    }

    public List<Combo> obtenerPorNivel(NivelCombo nivel) {
        return comboRepository.findByNivel(nivel);
    }
    
    
    public Map<String, Object> filtrarCombos(String estado, String tipo, String tematica, String subtema, String texto, int page, int size) {
        try { sincronizarEstadosAsignaciones(); } catch (Exception ignored) {}
        // Crear objeto Pageable para paginaci?n
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        Page<Combo> paginaCombos;

        // Si hay texto, buscar en preguntas y respuestas usando consulta nativa
        if (texto != null && !texto.trim().isEmpty()) {
            String textoBusqueda = "%" + texto.trim().toLowerCase() + "%";
            
            // Construir consulta nativa para buscar combos que tengan preguntas o respuestas con el texto
            String sql = "SELECT DISTINCT c.id FROM combos c " +
                        "INNER JOIN combos_preguntas cp ON c.id = cp.combo_id " +
                        "INNER JOIN preguntas p ON cp.pregunta_id = p.id " +
                        "WHERE (LOWER(p.pregunta) LIKE :texto OR LOWER(p.respuesta) LIKE :texto)";
            
            if (estado != null && !estado.isEmpty()) {
                sql += " AND c.estado = :estado";
            }
            if (tipo != null && !tipo.isEmpty()) {
                sql += " AND c.tipo = :tipo";
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
            if (tipo != null && !tipo.isEmpty()) {
                query.setParameter("tipo", tipo);
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
            String countSql = "SELECT COUNT(DISTINCT c.id) FROM combos c " +
                             "INNER JOIN combos_preguntas cp ON c.id = cp.combo_id " +
                             "INNER JOIN preguntas p ON cp.pregunta_id = p.id " +
                             "WHERE (LOWER(p.pregunta) LIKE :texto OR LOWER(p.respuesta) LIKE :texto)";
            if (estado != null && !estado.isEmpty()) {
                countSql += " AND c.estado = :estado";
            }
            if (tipo != null && !tipo.isEmpty()) {
                countSql += " AND c.tipo = :tipo";
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
            if (tipo != null && !tipo.isEmpty()) {
                countQuery.setParameter("tipo", tipo);
            }
            if (tematica != null && !tematica.isEmpty()) {
                countQuery.setParameter("tematica", "%" + tematica.toLowerCase() + "%");
            }
            if (subtema != null && !subtema.isEmpty()) {
                countQuery.setParameter("subtema", "%" + subtema.toLowerCase() + "%");
            }
            
            long total = ((Number) countQuery.getSingleResult()).longValue();
            
            // Convertir resultados a combos
            List<Combo> combos = new ArrayList<>();
            for (Number id : resultadoIds) {
                Optional<Combo> comboOpt = comboRepository.findById(id.longValue());
                if (comboOpt.isPresent()) {
                    combos.add(comboOpt.get());
                }
            }
            
            // Convertir a DTOs
            List<Map<String, Object>> dtos = new ArrayList<>();
            for (Combo c : combos) {
                Map<String, Object> dto = obtenerComboConSlots(c.getId());
                if (dto != null) dtos.add(dto);
            }
            
            // Construir respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("combos", dtos);
            response.put("currentPage", page);
            response.put("totalItems", total);
            response.put("totalPages", (int) Math.ceil((double) total / size));
            
            return response;
        }

        // Aplicar filtros y paginaci?n directamente en la consulta a la base de datos
        if (estado != null && !estado.isEmpty() && tipo != null && !tipo.isEmpty() && tematica != null && !tematica.isEmpty()) {
            // Filtrar por estado, tipo y tem?tica
            EstadoCombo estadoEnum = EstadoCombo.valueOf(estado);
            Combo.TipoCombo tipoEnum = Combo.TipoCombo.valueOf(tipo);
            paginaCombos = comboRepository.findByEstadoAndTipoAndTematica(estadoEnum, tipoEnum, tematica, pageable);
        } else if (estado != null && !estado.isEmpty() && tipo != null && !tipo.isEmpty()) {
            // Filtrar por estado y tipo
            EstadoCombo estadoEnum = EstadoCombo.valueOf(estado);
            Combo.TipoCombo tipoEnum = Combo.TipoCombo.valueOf(tipo);
            paginaCombos = comboRepository.findByEstadoAndTipo(estadoEnum, tipoEnum, pageable);
        } else if (estado != null && !estado.isEmpty() && tematica != null && !tematica.isEmpty()) {
            // Filtrar por estado y tem?tica
            EstadoCombo estadoEnum = EstadoCombo.valueOf(estado);
            paginaCombos = comboRepository.findByEstadoAndTematica(estadoEnum, tematica, pageable);
        } else if (tipo != null && !tipo.isEmpty() && tematica != null && !tematica.isEmpty()) {
            // Filtrar por tipo y tem?tica
            Combo.TipoCombo tipoEnum = Combo.TipoCombo.valueOf(tipo);
            paginaCombos = comboRepository.findByTipoAndTematica(tipoEnum, tematica, pageable);
        } else if (estado != null && !estado.isEmpty()) {
            // Filtrar solo por estado
            EstadoCombo estadoEnum = EstadoCombo.valueOf(estado);
            paginaCombos = comboRepository.findByEstado(estadoEnum, pageable);
        } else if (tipo != null && !tipo.isEmpty()) {
            // Filtrar solo por tipo
            Combo.TipoCombo tipoEnum = Combo.TipoCombo.valueOf(tipo);
            paginaCombos = comboRepository.findByTipo(tipoEnum, pageable);
        } else if (tematica != null && !tematica.isEmpty()) {
            // Filtrar solo por tem?tica
            paginaCombos = comboRepository.findByTematica(tematica, pageable);
        } else {
            // Si no hay filtros, usar la paginaci?n existente
            return obtenerTodosPaginados(page, size);
        }

        // Convertir a DTOs
        List<Map<String, Object>> dtos = new ArrayList<>();
        for (Combo c : paginaCombos.getContent()) {
            Map<String, Object> dto = obtenerComboConSlots(c.getId());
            if (dto != null) dtos.add(dto);
        }

        // Construir respuesta
        Map<String, Object> response = new HashMap<>();
        response.put("combos", dtos);
        response.put("currentPage", paginaCombos.getNumber());
        response.put("totalItems", paginaCombos.getTotalElements());
        response.put("totalPages", paginaCombos.getTotalPages());

        System.out.println("Filtrado de combos con paginaci?n optimizada - P?gina: " + page + ", Tama?o: " + size +
                          ", Total: " + paginaCombos.getTotalElements() +
                          ", Combos en esta p?gina: " + dtos.size());

        return response;
    }
    
    public Map<String, Object> filtrarCombosPorId(String idStr, int page, int size) {
        // Crear objeto Pageable para paginaci?n
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        
        try {
            // Intentar buscar por ID exacto
            Long id = Long.parseLong(idStr);
            Optional<Combo> combo = comboRepository.findById(id);
            if (combo.isPresent()) {
                // Si se encuentra el combo exacto, devolverlo
                Map<String, Object> dto = obtenerComboConSlots(combo.get().getId());
                if (dto != null) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("combos", List.of(dto));
                    response.put("currentPage", 0);
                    response.put("totalItems", 1);
                    response.put("totalPages", 1);
                    return response;
                }
            }
        } catch (NumberFormatException e) {
            // Si no es un n?mero, buscar por coincidencia parcial
        }
        
        // Buscar por coincidencia parcial en el ID
        Page<Combo> paginaCombos = comboRepository.findByIdContaining(idStr, pageable);
        
        // Convertir a DTOs
        List<Map<String, Object>> dtos = new ArrayList<>();
        for (Combo c : paginaCombos.getContent()) {
            Map<String, Object> dto = obtenerComboConSlots(c.getId());
            if (dto != null) dtos.add(dto);
        }

        // Construir respuesta
        Map<String, Object> response = new HashMap<>();
        response.put("combos", dtos);
        response.put("currentPage", paginaCombos.getNumber());
        response.put("totalItems", paginaCombos.getTotalElements());
        response.put("totalPages", paginaCombos.getTotalPages());

        return response;
    }

    /**
     * Obtiene combos disponibles para asignar: solo 'aprobado'.
     */
    public List<Combo> obtenerDisponiblesParaConcursantes() {
        // Obtener combos aprobados, adjudicados o grabados (disponibles para asignar)
        List<Combo> aprobados = comboRepository.findByEstado(EstadoCombo.aprobado);
        List<Combo> adjudicados = comboRepository.findByEstado(EstadoCombo.adjudicado);
        List<Combo> grabados = comboRepository.findByEstado(EstadoCombo.grabado);
        
        // Combinar todas las listas - permitir cualquiera de estos estados
        List<Combo> disponibles = new ArrayList<>();
        disponibles.addAll(aprobados);
        disponibles.addAll(adjudicados);
        disponibles.addAll(grabados);
        
        // Ordenar por ID descendente (más recientes primero)
        disponibles.sort((a, b) -> b.getId().compareTo(a.getId()));
        return disponibles;
    }

    public List<Combo> obtenerPorUsuario(Usuario usuario) {
        return comboRepository.findByCreacionUsuario(usuario);
    }

    public Combo actualizar(Long id, Combo combo) {
        if (comboRepository.existsById(id)) {
            combo.setId(id);
            return comboRepository.save(combo);
        }
        return null;
    }

    public void validarCompletoParaAprobar(Combo combo) {
        if (combo == null) {
            throw new IllegalArgumentException("Combo no encontrado");
        }
        Set<PreguntaCombo> preguntas = combo.getPreguntas();
        int total = preguntas == null ? 0 : preguntas.size();
        if (total != 3) {
            throw new IllegalArgumentException(
                "Un combo debe tener exactamente 3 preguntas multiplicadoras (PM1, PM2, PM3) para pasar a aprobado. Actual: " + total);
        }
        boolean factorX2 = false;
        boolean factorX3 = false;
        boolean factorX = false;
        Set<Integer> posiciones = new HashSet<>();
        for (PreguntaCombo pc : preguntas) {
            if (pc.getPregunta() == null || pc.getPregunta().getNivel() == null) {
                throw new IllegalArgumentException("El combo tiene preguntas incompletas");
            }
            if (!pc.getPregunta().getNivel().name().startsWith("_5")) {
                throw new IllegalArgumentException("Todas las preguntas del combo deben ser de nivel 5");
            }
            if (pc.getPosicion() != null) {
                posiciones.add(pc.getPosicion());
            }
            String factor = pc.getFactorMultiplicacion();
            if (factor == null) {
                throw new IllegalArgumentException("Todas las preguntas del combo deben tener factor (X2, X3, X)");
            }
            if ("X2".equals(factor) || "2".equals(factor)) {
                factorX2 = true;
            } else if ("X3".equals(factor) || "3".equals(factor)) {
                factorX3 = true;
            } else if ("X".equals(factor) || "0".equals(factor) || "1".equals(factor)) {
                factorX = true;
            }
        }
        if (posiciones.size() == 3 && posiciones.contains(1) && posiciones.contains(2) && posiciones.contains(3)) {
            return;
        }
        if (!factorX2 || !factorX3 || !factorX) {
            throw new IllegalArgumentException(
                "El combo debe tener las tres preguntas multiplicadoras (PM1/X2, PM2/X3, PM3/X) para pasar a aprobado");
        }
    }

    public Combo cambiarEstado(Long id, EstadoCombo nuevoEstado) {
        Optional<Combo> conPreguntas = obtenerConPreguntas(id);
        if (conPreguntas.isEmpty()) {
            return null;
        }
        Combo combo = conPreguntas.get();
        if (nuevoEstado == EstadoCombo.aprobado) {
            validarCompletoParaAprobar(combo);
        }
        combo.setEstado(nuevoEstado);
        return comboRepository.save(combo);
    }

    public boolean estaAsignadoAJornada(Long comboId) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(j) FROM Jornada j JOIN j.combos c WHERE c.id = :id", Long.class)
            .setParameter("id", comboId)
            .getSingleResult();
        return count != null && count > 0;
    }

    public boolean agregarPregunta(Long comboId, Long preguntaId, Integer factorMultiplicacion, Integer posicion) {
        Optional<Combo> comboOpt = comboRepository.findById(comboId);
        Optional<Pregunta> preguntaOpt = preguntaRepository.findById(preguntaId);
        
        if (comboOpt.isPresent() && preguntaOpt.isPresent()) {
            Combo combo = comboOpt.get();
            Pregunta pregunta = preguntaOpt.get();
            
            // Verificar que la pregunta est? aprobada; si est? verificada, promover a aprobada autom?ticamente
            if (pregunta.getEstado() != Pregunta.EstadoPregunta.aprobada) {
                if (pregunta.getEstado() == Pregunta.EstadoPregunta.verificada) {
                    entityManager.createNativeQuery("UPDATE preguntas SET estado = 'aprobada' WHERE id = ? AND estado = 'verificada'")
                        .setParameter(1, preguntaId)
                        .executeUpdate();
                    pregunta = preguntaRepository.findById(preguntaId).orElse(pregunta);
                } else {
                    throw new RuntimeException("La pregunta debe estar aprobada para ser agregada a un combo");
                }
            }
            
            // Verificar que sea pregunta de nivel 5
            if (!pregunta.getNivel().name().startsWith("_5")) {
                throw new RuntimeException("Solo se pueden agregar preguntas de nivel 5 a los combos");
            }
            
            // Verificar que la pregunta est? disponible o liberada
            // Tratar null como disponible para compatibilidad con datos antiguos
            if (pregunta.getEstadoDisponibilidad() != null &&
                pregunta.getEstadoDisponibilidad() != Pregunta.EstadoDisponibilidad.disponible && 
                pregunta.getEstadoDisponibilidad() != Pregunta.EstadoDisponibilidad.liberada) {
                throw new RuntimeException("La pregunta no est? disponible (estado: " + pregunta.getEstadoDisponibilidad() + ")");
            }
            
            // Verificar que la pregunta no est? ya en este combo
            PreguntaCombo.PreguntaComboId checkId = new PreguntaCombo.PreguntaComboId();
            checkId.setPreguntaId(preguntaId);
            checkId.setComboId(comboId);
            
            if (preguntaComboRepository.existsById(checkId)) {
                throw new RuntimeException("La pregunta ya est? agregada a este combo");
            }
            
            // Crear la relaci?n pregunta-combo
            PreguntaCombo pc = new PreguntaCombo();
            PreguntaCombo.PreguntaComboId id = new PreguntaCombo.PreguntaComboId();
            id.setPreguntaId(preguntaId);
            id.setComboId(comboId);
            
            pc.setId(id);
            pc.setPregunta(pregunta);
            pc.setCombo(combo);
            pc.setFactorMultiplicacion(factorMultiplicacion != null ? factorMultiplicacion.toString() : "1");
            pc.setPosicion(posicion);
            
            // Guardar la relaci?n en la base de datos
            preguntaComboRepository.save(pc);
            
            // Marcar pregunta como usada
            int rowsUpdated = entityManager.createNativeQuery(
                "UPDATE preguntas SET estado = 'usada', estado_disponibilidad = 'usada' WHERE id = ?")
                .setParameter(1, preguntaId)
                .executeUpdate();
            
            return true;
        }
        return false;
    }

    public long contarPreguntasCombo(Long comboId) {
        Optional<Combo> comboOpt = comboRepository.findById(comboId);
        if (comboOpt.isEmpty()) {
            return 0;
        }
        Combo combo = comboOpt.get();
        return combo.getPreguntas() != null ? combo.getPreguntas().size() : 0;
    }

    public boolean quitarPregunta(Long comboId, Long preguntaId) {
        Optional<Combo> comboOpt = comboRepository.findById(comboId);
        Optional<Pregunta> preguntaOpt = preguntaRepository.findById(preguntaId);
        
        if (comboOpt.isPresent() && preguntaOpt.isPresent()) {
            Combo combo = comboOpt.get();
            Pregunta pregunta = preguntaOpt.get();
            
            // Eliminar la relaci?n directamente con consulta nativa
            int relacionesEliminadas = entityManager.createNativeQuery(
                "DELETE FROM combos_preguntas WHERE combo_id = ? AND pregunta_id = ?")
                .setParameter(1, comboId)
                .setParameter(2, preguntaId)
                .executeUpdate();
            
            // Liberar la pregunta solo si no est? en otros combos
            long otrosCombos = entityManager.createQuery(
                "SELECT COUNT(pc) FROM PreguntaCombo pc WHERE pc.pregunta.id = :preguntaId", Long.class)
                .setParameter("preguntaId", preguntaId)
                .getSingleResult();
                
            if (otrosCombos == 0 && pregunta.getEstadoDisponibilidad() == Pregunta.EstadoDisponibilidad.usada) {
                entityManager.createNativeQuery(
                    "UPDATE preguntas SET estado = 'aprobada', estado_disponibilidad = 'liberada' WHERE id = ?")
                    .setParameter(1, preguntaId)
                    .executeUpdate();
            }
            
            return relacionesEliminadas > 0;
        }
        return false;
    }

    @Transactional
    public boolean actualizarFactorPregunta(Long comboId, Long preguntaId, String factorMultiplicacion) {
        try {
            // Validar que el factor no est? vac?o
            if (factorMultiplicacion == null || factorMultiplicacion.trim().isEmpty()) {
                factorMultiplicacion = "X"; // Valor por defecto
            }
            
            // Verificar que el combo existe
            Optional<Combo> comboOpt = comboRepository.findById(comboId);
            if (comboOpt.isEmpty()) {
                return false;
            }
            
            // Verificar que la pregunta existe
            Optional<Pregunta> preguntaOpt = preguntaRepository.findById(preguntaId);
            if (preguntaOpt.isEmpty()) {
                return false;
            }
            
            // Crear la clave primaria compuesta para buscar la relaci?n
            PreguntaCombo.PreguntaComboId id = new PreguntaCombo.PreguntaComboId();
            id.setComboId(comboId);
            id.setPreguntaId(preguntaId);
            
            // Buscar la relaci?n
            Optional<PreguntaCombo> pcOpt = preguntaComboRepository.findById(id);
            if (pcOpt.isEmpty()) {
                return false;
            }
            
            PreguntaCombo preguntaCombo = pcOpt.get();
            
            // Actualizar el factor
            preguntaCombo.setFactorMultiplicacion(factorMultiplicacion);
            
            // Guardar la relaci?n actualizada
            preguntaComboRepository.save(preguntaCombo);
            
            // Forzar flush para asegurar que se guarde en la base de datos
            entityManager.flush();
            
            return true;
        } catch (Exception e) {
            System.err.println("Error al actualizar factor: " + e.getMessage());
            return false;
        }
    }

    public void eliminar(Long id) {
        // Verificar que el combo existe
        Optional<Combo> comboOpt = comboRepository.findById(id);
        if (comboOpt.isEmpty()) {
            throw new IllegalArgumentException("Combo con ID " + id + " no encontrado");
        }

        Combo combo = comboOpt.get();

        // Verificar dependencias - no se puede eliminar si est? adjudicado o grabado
        if (combo.getEstado() == Combo.EstadoCombo.adjudicado) {
            throw new IllegalArgumentException("No se puede eliminar el combo porque est? adjudicado a una jornada. Desas?gnalo primero.");
        }
        if (combo.getEstado() == Combo.EstadoCombo.grabado) {
            throw new IllegalArgumentException("No se puede eliminar el combo porque est? grabado (asignado a concursantes). Desas?gnalo primero.");
        }

        // Verificar si hay concursantes usando este combo
        Long concursantesCount = entityManager.createQuery(
            "SELECT COUNT(c) FROM Concursante c WHERE c.combo.id = :comboId", Long.class)
            .setParameter("comboId", id)
            .getSingleResult();
        
        if (concursantesCount > 0) {
            throw new IllegalArgumentException("No se puede eliminar el combo porque est? siendo usado por " + 
                concursantesCount + " concursante(s). Desas?gnalo primero.");
        }

        // Verificar si est? en alguna jornada
        Long jornadasCount = entityManager.createQuery(
            "SELECT COUNT(j) FROM Jornada j JOIN j.combos c WHERE c.id = :comboId", Long.class)
            .setParameter("comboId", id)
            .getSingleResult();
            
        if (jornadasCount > 0) {
            throw new IllegalArgumentException("No se puede eliminar el combo porque est? asignado a " + 
                jornadasCount + " jornada(s). Desas?gnalo primero.");
        }

        // Eliminar registros del historial que referencian este combo (si la tabla existe)
        try {
            // Verificar si la tabla existe antes de intentar eliminar
            Object result = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'historial_jornadas'")
                .getSingleResult();
            Long tableExists = ((Number) result).longValue();
            
            if (tableExists > 0) {
                entityManager.createNativeQuery(
                    "DELETE FROM historial_jornadas WHERE combo_id = ?")
                    .setParameter(1, id)
                    .executeUpdate();
            }
        } catch (Exception e) {
            // Si hay error al eliminar el historial, continuamos de todas formas
            System.err.println("Advertencia: No se pudieron eliminar algunos registros del historial para el combo " + id + ": " + e.getMessage());
        }

        // Si llegamos aqu?, es seguro eliminar - liberar las preguntas asociadas
        Set<PreguntaCombo> preguntas = combo.getPreguntas();
        for (PreguntaCombo pc : preguntas) {
            // Devolver a aprobada y marcar como liberada para poder reutilizar
            entityManager.createNativeQuery(
                "UPDATE preguntas SET estado = 'aprobada', estado_disponibilidad = 'liberada' WHERE id = ?")
                .setParameter(1, pc.getPregunta().getId())
                .executeUpdate();
        }
        
        comboRepository.deleteById(id);
    }

    public int limpiarPreguntasInvalidas(Long comboId) {
        // Ya no eliminamos preguntas basadas en el factor, ya que ahora es un campo de texto libre
        return 0;
    }

    /**
     * Devuelve un combo con las preguntas mapeadas a DTOs con slot/hueco.
     */
    public Map<String, Object> obtenerComboConSlots(Long id) {
        Optional<Combo> opt = obtenerConPreguntas(id);
        if (opt.isEmpty()) return null;
        Combo c = opt.get();
        Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("id", c.getId());
        dto.put("version", c.getVersion());
        dto.put("estado", c.getEstado());
        dto.put("tipo", c.getTipo());
        dto.put("tematica", c.getTematica());
        dto.put("notasDireccion", c.getNotasDireccion());
        dto.put("fechaCreacion", c.getFechaCreacion() != null ? c.getFechaCreacion().toString() : null);
        // Jornada asignada (si existe)
        try {
            Long jornadaId = entityManager.createQuery(
                "SELECT j.id FROM Jornada j JOIN j.combos co WHERE co.id = :id", Long.class)
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
                "WHERE h.combo_id = :cid AND h.estado_asignacion = 'reaprovechado' " +
                "ORDER BY h.fecha_asignacion DESC " +
                "LIMIT 1")
                .setParameter("cid", id)
                .getResultList();
            
            if (!historialReutilizado.isEmpty()) {
                Object[] registro = historialReutilizado.get(0);
                dto.put("reutilizadoDeJornadaId", ((Number) registro[0]).longValue());
                dto.put("reutilizadoDeJornadaNombre", (String) registro[1]);
                System.out.println("[DTO-COMBO] Combo " + id + " | estado=" + c.getEstado() + " | jornada=" + dto.get("jornadaAsignada") + " | reutilizadoDe=" + dto.get("reutilizadoDeJornadaId"));
            }
        } catch (Exception e) {
            System.err.println("[DTO-COMBO] ERROR al buscar historial para combo " + id);
        }
        
        // Mapear preguntas a slots PM1, PM2, PM3
        java.util.Map<String, Object> mapPorSlot = new java.util.HashMap<>();
        
        // Mapear preguntas a su slot usando la posicion almacenada (si existe)
        // o fallback por orden de ID para datos legacy sin posicion
        boolean todosConPosicion = c.getPreguntas().stream()
            .allMatch(pc -> pc.getPosicion() != null);

        if (todosConPosicion) {
            // Camino principal: usar posicion persistida — garantiza orden estable
            for (PreguntaCombo pc : c.getPreguntas()) {
                String slot = "PM" + pc.getPosicion();
                Object pcdto = new java.util.HashMap<>();
                ((Map<String, Object>) pcdto).put("pregunta", mapPreguntaToDTO(pc.getPregunta()));
                ((Map<String, Object>) pcdto).put("factorMultiplicacion", pc.getFactorMultiplicacion());
                ((Map<String, Object>) pcdto).put("slot", slot);
                mapPorSlot.put(slot, pcdto);
            }
        } else {
            // Fallback legacy: inferir slot desde el factor convencional (PM1=X2, PM2=X3, PM3=X/0)
            // antes de caer en orden por ID
            for (PreguntaCombo pc : c.getPreguntas()) {
                int pos = posicionDesdeFactor(pc.getFactorMultiplicacion());
                String slot = "PM" + pos;
                if (!mapPorSlot.containsKey(slot)) {
                    Object pcdto = new java.util.HashMap<>();
                    ((Map<String, Object>) pcdto).put("pregunta", mapPreguntaToDTO(pc.getPregunta()));
                    ((Map<String, Object>) pcdto).put("factorMultiplicacion", pc.getFactorMultiplicacion());
                    ((Map<String, Object>) pcdto).put("slot", slot);
                    mapPorSlot.put(slot, pcdto);
                }
            }
            // Si tras inferir por factor quedan colisiones sin resolver, asignar por ID
            if (mapPorSlot.size() < c.getPreguntas().size()) {
                java.util.List<PreguntaCombo> sinSlot = new java.util.ArrayList<>();
                for (PreguntaCombo pc : c.getPreguntas()) {
                    int pos = posicionDesdeFactor(pc.getFactorMultiplicacion());
                    if (!mapPorSlot.containsKey("PM" + pos)) sinSlot.add(pc);
                }
                sinSlot.sort((a, b) -> a.getPregunta().getId().compareTo(b.getPregunta().getId()));
                java.util.List<String> libres = new java.util.ArrayList<>(java.util.Arrays.asList("PM1","PM2","PM3"));
                libres.removeAll(mapPorSlot.keySet());
                for (int i = 0; i < Math.min(sinSlot.size(), libres.size()); i++) {
                    PreguntaCombo pc = sinSlot.get(i);
                    String slot = libres.get(i);
                    Object pcdto = new java.util.HashMap<>();
                    ((Map<String, Object>) pcdto).put("pregunta", mapPreguntaToDTO(pc.getPregunta()));
                    ((Map<String, Object>) pcdto).put("factorMultiplicacion", pc.getFactorMultiplicacion());
                    ((Map<String, Object>) pcdto).put("slot", slot);
                    mapPorSlot.put(slot, pcdto);
                }
            }
        }
        
        // Asegurar los 3 slots PM
        java.util.List<Object> preguntasDTO = new java.util.ArrayList<>();
        for (String slot : java.util.Arrays.asList("PM1", "PM2", "PM3")) {
            if (mapPorSlot.containsKey(slot)) {
                preguntasDTO.add(mapPorSlot.get(slot));
            } else {
                // Slot vac?o
                Object vacio = new java.util.HashMap<>();
                ((Map<String, Object>) vacio).put("slot", slot);
                ((Map<String, Object>) vacio).put("pregunta", null);
                ((Map<String, Object>) vacio).put("factorMultiplicacion", null);
                preguntasDTO.add(vacio);
            }
        }
        dto.put("preguntas", preguntasDTO);
        return dto;
    }

    /** Sincroniza estados adjudicado/aprobado con asignaciones de jornada (lote) */
    private void sincronizarEstadosAsignaciones() {
        // Combos adjudicados por estar en jornadas
        // IMPORTANTE: no tocar los que ya est?n en 'aprobado' porque pueden estar marcados como reutilizados/liberados
        entityManager.createNativeQuery(
            "UPDATE combos SET estado='adjudicado' " +
            "WHERE id IN (SELECT combo_id FROM jornadas_combos) " +
            "AND estado NOT IN ('adjudicado','grabado','aprobado')")
            .executeUpdate();
        // Combos sin jornada ? aprobado (solo si estaban adjudicados)
        entityManager.createNativeQuery(
            "UPDATE combos SET estado='aprobado' WHERE estado='adjudicado' AND id NOT IN (SELECT combo_id FROM jornadas_combos)")
            .executeUpdate();
    }

    private Map<String, Object> mapPreguntaToDTO(Pregunta p) {
        Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("id", p.getId());
        dto.put("pregunta", p.getPregunta());
        dto.put("respuesta", p.getRespuesta());
        dto.put("tematica", p.getTematica());
        dto.put("nivel", p.getNivel());
        dto.put("estado", p.getEstado());
        dto.put("fuentes", p.getFuentes());
        return dto;
    }

    /**
     * Valida que un combo est? en el estado esperado para prevenir conflictos de concurrencia
     */
    private void validarEstadoComboParaAsignacion(Long comboId, Combo.EstadoCombo estadoEsperado) {
        Optional<Combo> comboOpt = comboRepository.findById(comboId);
        if (comboOpt.isEmpty()) {
            throw new IllegalArgumentException("Combo con ID " + comboId + " no encontrado");
        }
        
        Combo combo = comboOpt.get();
        if (combo.getEstado() != estadoEsperado) {
            throw new IllegalStateException("El combo " + comboId + " no est? en estado '" + estadoEsperado + 
                "'. Estado actual: '" + combo.getEstado() + "'. Otro usuario pudo haberlo modificado.");
        }
    }

    /**
     * Cambia el estado de un combo de forma at?mica con validaci?n de concurrencia
     */
    @Transactional
    public boolean cambiarEstadoAtomico(Long comboId, Combo.EstadoCombo estadoActualEsperado, 
                                       Combo.EstadoCombo nuevoEstado) {
        // Usar query nativa para cambio at?mico con verificaci?n de estado
        int filasActualizadas = entityManager.createNativeQuery(
            "UPDATE combos SET estado = ? WHERE id = ? AND estado = ?")
            .setParameter(1, nuevoEstado.name())
            .setParameter(2, comboId)
            .setParameter(3, estadoActualEsperado.name())
            .executeUpdate();
            
        if (filasActualizadas == 0) {
            throw new IllegalStateException("No se pudo cambiar el estado del combo " + comboId + 
                " porque otro usuario lo modific? simult?neamente. Estado esperado: " + estadoActualEsperado);
        }
        
        return true;
    }

    /**
     * Verifica y reserva m?ltiples preguntas nivel 5 at?micamente para combos
     * @param preguntaIdsConFactores Map de ID de pregunta -> factor multiplicaci?n
     * @return true si todas las preguntas fueron reservadas exitosamente
     * @throws IllegalArgumentException si alguna pregunta no est? disponible
     */
    @Transactional
    public boolean verificarYReservarPreguntasComboAtomico(Map<Long, Integer> preguntaIdsConFactores) {
        // Normalizar IDs: quitar nulos y duplicados para evitar falsos positivos de validaci?n
        java.util.List<Long> preguntaIds = new java.util.ArrayList<>();
        java.util.Set<Long> vistos = new java.util.LinkedHashSet<>();
        for (Long id : preguntaIdsConFactores.keySet()) {
            if (id == null) continue;
            if (vistos.add(id)) {
                preguntaIds.add(id);
            }
        }
        
        if (preguntaIds.isEmpty()) {
            return true;
        }
        
        // PASO 1: Verificar que todas las preguntas existen y est?n en estado correcto
        List<Pregunta> preguntas = preguntaRepository.findAllById(preguntaIds);
        
        if (preguntas.size() != preguntaIds.size()) {
            // Construir lista expl?cita de IDs faltantes para facilitar el debug
            java.util.Set<Long> encontrados = new java.util.HashSet<>();
            for (Pregunta p : preguntas) {
                if (p != null && p.getId() != null) {
                    encontrados.add(p.getId());
                }
            }
            java.util.List<Long> faltantes = new java.util.ArrayList<>();
            for (Long id : preguntaIds) {
                if (id != null && !encontrados.contains(id)) {
                    faltantes.add(id);
                }
            }
            String detalle = faltantes.isEmpty()
                ? ""
                : " (IDs faltantes: " + faltantes + ")";
            throw new IllegalArgumentException("Una o m?s preguntas no fueron encontradas" + detalle);
        }
        
        // Verificar el estado de cada pregunta (promover 'verificada' -> 'aprobada' si aplica)
        for (Pregunta pregunta : preguntas) {
            if (pregunta.getEstado() != Pregunta.EstadoPregunta.aprobada) {
                if (pregunta.getEstado() == Pregunta.EstadoPregunta.verificada) {
                    entityManager.createNativeQuery("UPDATE preguntas SET estado = 'aprobada' WHERE id = ? AND estado = 'verificada'")
                        .setParameter(1, pregunta.getId())
                        .executeUpdate();
                    pregunta.setEstado(Pregunta.EstadoPregunta.aprobada);
                } else {
                    throw new IllegalArgumentException("La pregunta " + pregunta.getId() + " no est? aprobada (estado: " + pregunta.getEstado() + ")");
                }
            }
            
            if (pregunta.getEstadoDisponibilidad() != Pregunta.EstadoDisponibilidad.disponible && 
                pregunta.getEstadoDisponibilidad() != Pregunta.EstadoDisponibilidad.liberada) {
                throw new IllegalArgumentException("La pregunta " + pregunta.getId() + " no est? disponible (estado: " + pregunta.getEstadoDisponibilidad() + ")");
            }
            
            // Verificar que sea pregunta de nivel 5 para combos
            if (!pregunta.getNivel().name().startsWith("_5")) {
                throw new IllegalArgumentException("La pregunta " + pregunta.getId() + " no es de nivel 5. Solo se pueden usar preguntas de nivel 5 en combos");
            }
        }
        
        // PASO 2: Reservar todas las preguntas AT?MICAMENTE con una sola query
        String preguntaIdsStr = preguntaIds.stream()
            .map(String::valueOf)
            .reduce((a, b) -> a + "," + b)
            .orElse("");
            
        int preguntasReservadas = entityManager.createNativeQuery(
            "UPDATE preguntas SET estado = 'usada', estado_disponibilidad = 'usada' " +
            "WHERE id IN (" + preguntaIdsStr + ") " +
            "AND estado_disponibilidad IN ('disponible', 'liberada') " +
            "AND estado = 'aprobada' " +
            "AND nivel LIKE '_5%'"
        ).executeUpdate();
        
        // PASO 3: Verificar que se reservaron TODAS las preguntas
        if (preguntasReservadas != preguntaIds.size()) {
            // Rollback - alguna pregunta fue tomada por otro usuario
            throw new IllegalStateException("Conflicto de concurrencia: " + (preguntaIds.size() - preguntasReservadas) + 
                " pregunta(s) fueron reservadas por otro usuario. Por favor, verifica la disponibilidad e intenta nuevamente.");
        }
        
        return true;
    }

    /**
     * Libera m?ltiples preguntas de combo at?micamente
     */
    @Transactional
    public void liberarPreguntasComboAtomico(List<Long> preguntaIds) {
        if (preguntaIds == null || preguntaIds.isEmpty()) return;
        
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

    /**
     * Crea un combo con m?ltiples preguntas de forma at?mica
     */
    @Transactional
    public Combo crearComboDesdeDTO(CrearComboDTO dto, Usuario usuario) {
        // PASO 1: Preparar mapa de preguntas con factores
        Map<Long, Integer> preguntaIdsConFactores = new java.util.HashMap<>();
        for (CrearComboDTO.PreguntaMultiplicadoraDTO pm : dto.getPreguntasMultiplicadoras()) {
            String factor = pm.getFactor();
            if (factor == null || factor.trim().isEmpty()) {
                factor = "1";
            }
            // Extraer el n?mero del factor (X2 -> 2, X3 -> 3, X -> 1)
            String numeroFactor = factor.replaceAll("[^0-9]", "");
            if (numeroFactor.isEmpty()) {
                numeroFactor = "1"; // Si no hay n?mero, usar 1
            }
            preguntaIdsConFactores.put(pm.getId(), Integer.valueOf(numeroFactor));
        }
        
        // PASO 2: VERIFICACI?N Y RESERVA AT?MICA de todas las preguntas
        try {
            verificarYReservarPreguntasComboAtomico(preguntaIdsConFactores);
        } catch (IllegalStateException e) {
            // Error de concurrencia - mensaje espec?fico
            throw new IllegalArgumentException("Error de concurrencia al reservar preguntas: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // Error de validaci?n - reenviar tal como est?
            throw e;
        }
        
        // PASO 3: Crear el combo (las preguntas ya est?n reservadas)
        Combo combo = new Combo();
        combo.setCreacionUsuario(usuario);
        // Usar el estado del DTO si se proporciona, sino usar "borrador" por defecto
        if (dto.getEstado() != null && !dto.getEstado().trim().isEmpty()) {
            try {
                combo.setEstado(EstadoCombo.valueOf(dto.getEstado()));
            } catch (IllegalArgumentException e) {
                combo.setEstado(EstadoCombo.borrador); // Si el estado no es válido, usar borrador
            }
        } else {
            combo.setEstado(EstadoCombo.borrador);
        }
        combo.setNivel(NivelCombo.NORMAL);
        combo.setTipo(Combo.TipoCombo.valueOf(dto.getTipo()));
        combo.setTematica(dto.getTematica());
        combo.setNotasDireccion(dto.getNotasDireccion());
        combo = comboRepository.save(combo);

        // PASO 4: Crear las relaciones pregunta-combo (preguntas ya reservadas)
        // Iteramos sobre la lista del DTO para preservar el orden y asignar posicion 1,2,3
        try {
            java.util.List<CrearComboDTO.PreguntaMultiplicadoraDTO> pmList = dto.getPreguntasMultiplicadoras();
            for (int idx = 0; idx < pmList.size(); idx++) {
                CrearComboDTO.PreguntaMultiplicadoraDTO pmDto = pmList.get(idx);
                Long preguntaId = pmDto.getId();
                String factorStr = pmDto.getFactor();
                if (factorStr == null || factorStr.trim().isEmpty()) factorStr = "1";
                String numeroFactor = factorStr.replaceAll("[^0-9]", "");
                if (numeroFactor.isEmpty()) numeroFactor = "1";
                
                Pregunta pregunta = preguntaRepository.findById(preguntaId)
                    .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada: " + preguntaId));
                
                PreguntaCombo pc = new PreguntaCombo();
                PreguntaCombo.PreguntaComboId id = new PreguntaCombo.PreguntaComboId();
                id.setPreguntaId(preguntaId);
                id.setComboId(combo.getId());
                
                pc.setId(id);
                pc.setPregunta(pregunta);
                pc.setCombo(combo);
                pc.setFactorMultiplicacion(numeroFactor);
                pc.setPosicion(idx + 1); // PM1=1, PM2=2, PM3=3
                
                preguntaComboRepository.save(pc);
                // Nota: La pregunta ya fue marcada como 'usada' en verificarYReservarPreguntasComboAtomico()
            }
            return comboRepository.findById(combo.getId()).orElse(combo);
        } catch (Exception e) {
            // En caso de error, liberar las preguntas reservadas
            liberarPreguntasComboAtomico(new java.util.ArrayList<>(preguntaIdsConFactores.keySet()));
            // Eliminar el combo creado
            comboRepository.deleteById(combo.getId());
            throw new RuntimeException("Error al crear relaciones pregunta-combo: " + e.getMessage());
        }
    }

    /**
     * Infiere la posicion (1=PM1, 2=PM2, 3=PM3) a partir del factor almacenado.
     * Convención: PM1=X2 ("2"), PM2=X3 ("3"), PM3=X ("0" o "1").
     */
    private int posicionDesdeFactor(String factor) {
        if (factor == null) return 3;
        String f = factor.trim();
        // Formato numérico directo ("2", "3", "0", "1")
        if ("2".equals(f)) return 1;
        if ("3".equals(f)) return 2;
        if ("0".equals(f) || "1".equals(f)) return 3;
        // Formato texto ("X2", "X3", "X")
        String num = f.replaceAll("[^0-9]", "");
        if ("2".equals(num)) return 1;
        if ("3".equals(num)) return 2;
        return 3; // "X" sin número → PM3
    }

    /**
     * Obtiene las preguntas de un combo espec?fico
     */
    public List<Map<String, Object>> obtenerPreguntasCombo(Long comboId) {
        Combo combo = comboRepository.findById(comboId)
            .orElseThrow(() -> new IllegalArgumentException("Combo no encontrado con ID: " + comboId));
        
        List<Map<String, Object>> preguntas = new java.util.ArrayList<>();
        
        if (combo.getPreguntas() != null) {
            for (PreguntaCombo pc : combo.getPreguntas()) {
                Pregunta pregunta = pc.getPregunta();
                Map<String, Object> preguntaMap = new java.util.HashMap<>();
                preguntaMap.put("id", pregunta.getId());
                preguntaMap.put("pregunta", pregunta.getPregunta());
                preguntaMap.put("respuesta", pregunta.getRespuesta());
                preguntaMap.put("tematica", pregunta.getTematica());
                preguntaMap.put("nivel", pregunta.getNivel().name());
                preguntaMap.put("factor", pc.getFactorMultiplicacion());
                preguntas.add(preguntaMap);
            }
        }
        
        return preguntas;
    }
} 