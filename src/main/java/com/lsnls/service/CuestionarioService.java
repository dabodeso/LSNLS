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

    public Optional<Cuestionario> obtenerPorId(Long id) {
        return cuestionarioRepository.findById(id);
    }

    public Optional<Cuestionario> obtenerConPreguntas(Long id) {
        try {
            System.out.println("==========================================");
            System.out.println("CARGANDO CUESTIONARIO " + id + " CON PREGUNTAS");
            System.out.println("==========================================");
            
            // Usar consulta JPQL que carga todo de una vez
            @SuppressWarnings("unchecked")
            List<Cuestionario> resultados = entityManager.createQuery(
                "SELECT DISTINCT c FROM Cuestionario c " +
                "LEFT JOIN FETCH c.preguntas pc " +
                "LEFT JOIN FETCH pc.pregunta p " +
                "LEFT JOIN FETCH p.creacionUsuario " +
                "WHERE c.id = :cuestionarioId"
            ).setParameter("cuestionarioId", id).getResultList();
            
            if (resultados.isEmpty()) {
                System.out.println("❌ CUESTIONARIO " + id + " NO ENCONTRADO");
                return Optional.empty();
            }
            
            Cuestionario cuestionario = resultados.get(0);
            System.out.println("✅ CUESTIONARIO " + id + " ENCONTRADO");
            System.out.println("📊 PREGUNTAS CARGADAS: " + cuestionario.getPreguntas().size());
            
            // Mostrar detalles de las preguntas
            if (!cuestionario.getPreguntas().isEmpty()) {
                System.out.println("📋 LISTADO DE PREGUNTAS:");
                int i = 1;
                for (PreguntaCuestionario pc : cuestionario.getPreguntas()) {
                    System.out.println("  " + i + ". ID: " + pc.getPregunta().getId() + 
                                     " | TEXTO: " + pc.getPregunta().getPregunta() + 
                                     " | FACTOR: " + pc.getFactorMultiplicacion());
                    i++;
                }
            } else {
                System.out.println("❌ NO SE ENCONTRARON PREGUNTAS PARA EL CUESTIONARIO " + id);
            }
            
            System.out.println("==========================================");
            return Optional.of(cuestionario);
            
        } catch (Exception e) {
            System.out.println("💥 ERROR EN obtenerConPreguntas: " + e.getMessage());
            e.printStackTrace();
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
     * Obtiene cuestionarios disponibles para asignar a concursantes.
     * Incluye cuestionarios en estado 'creado' y 'adjudicado'.
     */
    public List<Cuestionario> obtenerDisponiblesParaConcursantes() {
        List<Cuestionario> creados = cuestionarioRepository.findByEstado(EstadoCuestionario.creado);
        List<Cuestionario> adjudicados = cuestionarioRepository.findByEstado(EstadoCuestionario.adjudicado);
        
        List<Cuestionario> disponibles = new ArrayList<>();
        disponibles.addAll(creados);
        disponibles.addAll(adjudicados);
        
        // Ordenar por ID descendente (más recientes primero)
        disponibles.sort((a, b) -> b.getId().compareTo(a.getId()));
        
        return disponibles;
    }

    public boolean agregarPregunta(Long cuestionarioId, Long preguntaId, Integer factorMultiplicacion) {
        System.out.println("==========================================");
        System.out.println("AGREGANDO PREGUNTA " + preguntaId + " AL CUESTIONARIO " + cuestionarioId);
        System.out.println("Factor multiplicación: " + factorMultiplicacion);
        System.out.println("==========================================");
        
        Optional<Cuestionario> cuestionarioOpt = cuestionarioRepository.findById(cuestionarioId);
        Optional<Pregunta> preguntaOpt = preguntaRepository.findById(preguntaId);
        
        if (cuestionarioOpt.isEmpty()) {
            System.out.println("❌ CUESTIONARIO NO ENCONTRADO: " + cuestionarioId);
            throw new RuntimeException("Cuestionario no encontrado: " + cuestionarioId);
        }
        
        if (preguntaOpt.isEmpty()) {
            System.out.println("❌ PREGUNTA NO ENCONTRADA: " + preguntaId);
            throw new RuntimeException("Pregunta no encontrada: " + preguntaId);
        }
        
        Cuestionario cuestionario = cuestionarioOpt.get();
        Pregunta pregunta = preguntaOpt.get();
        
        System.out.println("✅ CUESTIONARIO Y PREGUNTA ENCONTRADOS");
        System.out.println("📋 Pregunta: " + pregunta.getPregunta());
        System.out.println("📋 Nivel: " + pregunta.getNivel());
        System.out.println("📋 Estado: " + pregunta.getEstado());
        System.out.println("📋 Estado disponibilidad: " + pregunta.getEstadoDisponibilidad());
        
        // Verificar que la pregunta esté aprobada
        if (pregunta.getEstado() != Pregunta.EstadoPregunta.aprobada) {
            System.out.println("❌ PREGUNTA NO APROBADA - Estado actual: " + pregunta.getEstado());
            throw new RuntimeException("La pregunta debe estar aprobada para ser agregada a un cuestionario");
        }
        
        // Verificar que la pregunta esté disponible o liberada (puede reutilizarse)
        if (pregunta.getEstadoDisponibilidad() != Pregunta.EstadoDisponibilidad.disponible && 
            pregunta.getEstadoDisponibilidad() != Pregunta.EstadoDisponibilidad.liberada) {
            System.out.println("❌ PREGUNTA NO DISPONIBLE - Estado disponibilidad: " + pregunta.getEstadoDisponibilidad());
            throw new RuntimeException("La pregunta no está disponible (estado: " + pregunta.getEstadoDisponibilidad() + ")");
        }
        
        // Verificar que la pregunta no esté ya en este cuestionario
        PreguntaCuestionario.PreguntaCuestionarioId checkId = new PreguntaCuestionario.PreguntaCuestionarioId();
        checkId.setPreguntaId(preguntaId);
        checkId.setCuestionarioId(cuestionarioId);
        
        boolean yaExiste = preguntaCuestionarioRepository.existsById(checkId);
        System.out.println("🔍 ¿Ya existe en cuestionario? " + yaExiste);
        
        if (yaExiste) {
            System.out.println("❌ PREGUNTA YA AGREGADA AL CUESTIONARIO");
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
            
            System.out.println("💾 Guardando relación pregunta-cuestionario...");
            // Guardar la relación en la base de datos
            preguntaCuestionarioRepository.save(pc);
            System.out.println("✅ Relación guardada exitosamente");
            
            // Marcar pregunta como usada usando consulta SQL directa para evitar validaciones
            System.out.println("🔄 Actualizando estado de disponibilidad a 'usada'...");
            int rowsUpdated = entityManager.createNativeQuery(
                "UPDATE preguntas SET estado_disponibilidad = 'usada' WHERE id = ?")
                .setParameter(1, preguntaId)
                .executeUpdate();
            System.out.println("✅ Filas actualizadas: " + rowsUpdated);
            
            System.out.println("🎉 PREGUNTA AGREGADA EXITOSAMENTE");
            System.out.println("==========================================");
            return true;
            
        } catch (Exception e) {
            System.out.println("❌ ERROR AL GUARDAR: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al agregar pregunta: " + e.getMessage());
        }
    }

    public boolean quitarPregunta(Long cuestionarioId, Long preguntaId) {
        System.out.println("==========================================");
        System.out.println("QUITANDO PREGUNTA " + preguntaId + " DEL CUESTIONARIO " + cuestionarioId);
        System.out.println("==========================================");
        
        Optional<Cuestionario> cuestionarioOpt = cuestionarioRepository.findById(cuestionarioId);
        Optional<Pregunta> preguntaOpt = preguntaRepository.findById(preguntaId);
        
        if (cuestionarioOpt.isPresent() && preguntaOpt.isPresent()) {
            Cuestionario cuestionario = cuestionarioOpt.get();
            Pregunta pregunta = preguntaOpt.get();
            
            System.out.println("✅ CUESTIONARIO Y PREGUNTA ENCONTRADOS");
            System.out.println("📋 Pregunta: " + pregunta.getPregunta());
            System.out.println("📋 Nivel: " + pregunta.getNivel());
            
            // Verificar si existe la relación antes de eliminar
            PreguntaCuestionario.PreguntaCuestionarioId checkId = new PreguntaCuestionario.PreguntaCuestionarioId();
            checkId.setPreguntaId(preguntaId);
            checkId.setCuestionarioId(cuestionarioId);
            
            boolean existeRelacion = preguntaCuestionarioRepository.existsById(checkId);
            System.out.println("🔍 ¿Existe relación? " + existeRelacion);
            
            if (!existeRelacion) {
                System.out.println("❌ NO EXISTE LA RELACIÓN PREGUNTA-CUESTIONARIO");
                return false;
            }
            
            // Eliminar la relación usando SQL directo para asegurar que funcione
            int filasEliminadas = entityManager.createNativeQuery(
                "DELETE FROM cuestionarios_preguntas WHERE cuestionario_id = ? AND pregunta_id = ?")
                .setParameter(1, cuestionarioId)
                .setParameter(2, preguntaId)
                .executeUpdate();
            
            System.out.println("🗑️ Filas eliminadas de cuestionarios_preguntas: " + filasEliminadas);
            
            // Liberar la pregunta usando consulta SQL directa
            if (pregunta.getEstadoDisponibilidad() == Pregunta.EstadoDisponibilidad.usada) {
                int filasActualizadas = entityManager.createNativeQuery(
                    "UPDATE preguntas SET estado_disponibilidad = 'liberada' WHERE id = ?")
                    .setParameter(1, preguntaId)
                    .executeUpdate();
                System.out.println("🔄 Filas actualizadas en preguntas: " + filasActualizadas);
            }
            
            System.out.println("✅ PREGUNTA QUITADA EXITOSAMENTE");
            System.out.println("==========================================");
            return filasEliminadas > 0;
        }
        
        System.out.println("❌ CUESTIONARIO O PREGUNTA NO ENCONTRADOS");
        System.out.println("==========================================");
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

        // Si llegamos aquí, es seguro eliminar - liberar las preguntas asociadas
        Set<PreguntaCuestionario> preguntas = cuestionario.getPreguntas();
        for (PreguntaCuestionario pc : preguntas) {
            // Cambiar a disponible
            entityManager.createNativeQuery(
                "UPDATE preguntas SET estado_disponibilidad = 'disponible' WHERE id = ?")
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

    public List<Cuestionario> filtrarCuestionarios(String estado, String tematica) {
        if (estado != null && !estado.isEmpty() && tematica != null && !tematica.isEmpty()) {
            return cuestionarioRepository.findByEstadoAndTematicaContainingIgnoreCaseOrderByIdDesc(
                EstadoCuestionario.valueOf(estado), tematica);
        } else if (estado != null && !estado.isEmpty()) {
            return cuestionarioRepository.findByEstadoOrderByIdDesc(EstadoCuestionario.valueOf(estado));
        } else if (tematica != null && !tematica.isEmpty()) {
            return cuestionarioRepository.findByTematicaContainingIgnoreCaseOrderByIdDesc(tematica);
        } else {
            return cuestionarioRepository.findAllOrderByIdDesc();
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
        System.out.println("🔍 [SERVICIO] Verificando y reservando preguntas: " + preguntaIds);
        
        // Verificar que la lista no sea nula
        if (preguntaIds == null || preguntaIds.isEmpty()) {
            System.out.println("⚠️ [SERVICIO] Lista de preguntas vacía o nula");
            return true; // No hay nada que reservar
        }
        
        // PASO 1: Verificar que todas las preguntas existen y están en estado correcto
        // Usar una sola query para obtener todas las preguntas
        List<Pregunta> preguntas = preguntaRepository.findAllById(preguntaIds);
        
        if (preguntas.size() != preguntaIds.size()) {
            System.out.println("❌ [SERVICIO] No se encontraron todas las preguntas. Solicitadas: " + preguntaIds.size() + ", Encontradas: " + preguntas.size());
            throw new IllegalArgumentException("Una o más preguntas no fueron encontradas");
        }
        
        // Verificar el estado de cada pregunta
        for (Pregunta pregunta : preguntas) {
            if (pregunta.getEstado() != Pregunta.EstadoPregunta.aprobada) {
                System.out.println("❌ [SERVICIO] Pregunta no aprobada: " + pregunta.getId() + " - Estado: " + pregunta.getEstado());
                throw new IllegalArgumentException("La pregunta " + pregunta.getId() + " no está aprobada (estado: " + pregunta.getEstado() + ")");
            }
            
            if (pregunta.getEstadoDisponibilidad() != Pregunta.EstadoDisponibilidad.disponible && 
                pregunta.getEstadoDisponibilidad() != Pregunta.EstadoDisponibilidad.liberada) {
                System.out.println("❌ [SERVICIO] Pregunta no disponible: " + pregunta.getId() + " - Estado: " + pregunta.getEstadoDisponibilidad());
                throw new IllegalArgumentException("La pregunta " + pregunta.getId() + " no está disponible (estado: " + pregunta.getEstadoDisponibilidad() + ")");
            }
            
            // Verificar que sea pregunta de nivel 1-4 para cuestionarios
            if (pregunta.getNivel().name().startsWith("_5")) {
                System.out.println("❌ [SERVICIO] Pregunta de nivel incorrecto: " + pregunta.getId() + " - Nivel: " + pregunta.getNivel());
                throw new IllegalArgumentException("La pregunta " + pregunta.getId() + " es de nivel 5 y debe ir en combos, no en cuestionarios");
            }
        }
        
        // PASO 2: Reservar todas las preguntas ATÓMICAMENTE con una sola query
        // Usar query nativa para update condicional en batch
        String preguntaIdsStr = preguntaIds.stream()
            .map(String::valueOf)
            .reduce((a, b) -> a + "," + b)
            .orElse("");
            
        System.out.println("🔄 [SERVICIO] Ejecutando update para reservar preguntas: " + preguntaIdsStr);
        int preguntasReservadas = entityManager.createNativeQuery(
            "UPDATE preguntas SET estado_disponibilidad = 'usada' " +
            "WHERE id IN (" + preguntaIdsStr + ") " +
            "AND estado_disponibilidad IN ('disponible', 'liberada') " +
            "AND estado = 'aprobada'"
        ).executeUpdate();
        
        // PASO 3: Verificar que se reservaron TODAS las preguntas
        if (preguntasReservadas != preguntaIds.size()) {
            System.out.println("❌ [SERVICIO] No se pudieron reservar todas las preguntas. Solicitadas: " + preguntaIds.size() + ", Reservadas: " + preguntasReservadas);
            // Rollback - alguna pregunta fue tomada por otro usuario
            throw new IllegalStateException("Conflicto de concurrencia: " + (preguntaIds.size() - preguntasReservadas) + 
                " pregunta(s) fueron reservadas por otro usuario. Por favor, verifica la disponibilidad e intenta nuevamente.");
        }
        
        System.out.println("✅ [SERVICIO] Preguntas reservadas correctamente: " + preguntasReservadas);
        return true;
    }

    /**
     * Libera múltiples preguntas atómicamente (rollback en caso de error)
     */
    @Transactional
    public void liberarPreguntasAtomico(List<Long> preguntaIds) {
        String preguntaIdsStr = preguntaIds.stream()
            .map(String::valueOf)
            .reduce((a, b) -> a + "," + b)
            .orElse("");
            
        entityManager.createNativeQuery(
            "UPDATE preguntas SET estado_disponibilidad = 'liberada' " +
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
        System.out.println("🔍 [SERVICIO] Inicio actualizarDesdeDTO - ID: " + id);
        System.out.println("🔍 [SERVICIO] DTO recibido: " + dto);
        System.out.println("🔍 [SERVICIO] Preguntas recibidas: " + (dto.getPreguntasNormales() != null ? dto.getPreguntasNormales().size() : 0));
        
        Optional<Cuestionario> optCuestionario = cuestionarioRepository.findById(id);
        if (optCuestionario.isEmpty()) {
            System.out.println("❌ [SERVICIO] Cuestionario no encontrado: " + id);
            throw new IllegalArgumentException("Cuestionario no encontrado");
        }
        Cuestionario cuestionario = optCuestionario.get();
        System.out.println("✅ [SERVICIO] Cuestionario encontrado: " + cuestionario.getId() + " - Estado: " + cuestionario.getEstado());
        
        // PASO 1: VERIFICACIÓN Y RESERVA ATÓMICA de las nuevas preguntas
        try {
            System.out.println("🔄 [SERVICIO] Verificando y reservando preguntas nuevas...");
            verificarYReservarPreguntasAtomico(dto.getPreguntasNormales());
            System.out.println("✅ [SERVICIO] Preguntas nuevas verificadas y reservadas");
        } catch (IllegalStateException e) {
            // Error de concurrencia - mensaje específico
            System.out.println("❌ [SERVICIO] Error de concurrencia: " + e.getMessage());
            throw new IllegalArgumentException("Error de concurrencia al reservar preguntas: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // Error de validación - reenviar tal como está
            System.out.println("❌ [SERVICIO] Error de validación: " + e.getMessage());
            throw e;
        }

        // PASO 2: Liberar las preguntas actuales
        Set<PreguntaCuestionario> actuales = new HashSet<>(cuestionario.getPreguntas());
        List<Long> preguntasALiberar = new ArrayList<>();
        for (PreguntaCuestionario pc : actuales) {
            preguntasALiberar.add(pc.getPregunta().getId());
            preguntaCuestionarioRepository.delete(pc);
        }
        cuestionario.getPreguntas().clear();
        System.out.println("🔄 [SERVICIO] Preguntas actuales liberadas: " + preguntasALiberar.size());
        
        // Liberar preguntas anteriores atómicamente
        if (!preguntasALiberar.isEmpty()) {
            System.out.println("🔄 [SERVICIO] Liberando preguntas anteriores: " + preguntasALiberar);
            liberarPreguntasAtomico(preguntasALiberar);
            System.out.println("✅ [SERVICIO] Preguntas anteriores liberadas");
        }

        // PASO 3: Crear las nuevas relaciones pregunta-cuestionario
        try {
            System.out.println("🔄 [SERVICIO] Creando nuevas relaciones pregunta-cuestionario...");
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
                System.out.println("✅ [SERVICIO] Relación creada para pregunta: " + idPregunta);
                // Nota: La pregunta ya fue marcada como 'usada' en verificarYReservarPreguntasAtomico()
            }
            
            // Actualizar otros campos del cuestionario
            cuestionario.setTematica(dto.getTematica());
            cuestionario.setNotasDireccion(dto.getNotasDireccion());
            cuestionarioRepository.save(cuestionario);
            
            Cuestionario resultado = cuestionarioRepository.findById(cuestionario.getId()).orElse(cuestionario);
            System.out.println("✅ [SERVICIO] Cuestionario actualizado: " + resultado.getId() + " - Preguntas: " + 
                (resultado.getPreguntas() != null ? resultado.getPreguntas().size() : 0));
            return resultado;
        } catch (Exception e) {
            // En caso de error, liberar las preguntas nuevas reservadas
            System.out.println("❌ [SERVICIO] Error al actualizar relaciones: " + e.getMessage());
            liberarPreguntasAtomico(dto.getPreguntasNormales());
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
        dto.put("estado", c.getEstado());
        dto.put("fechaCreacion", c.getFechaCreacion() != null ? c.getFechaCreacion().toString() : null);
        
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
        System.out.println("==========================================");
        System.out.println("QUITANDO PREGUNTA POR SLOT: " + slot + " DEL CUESTIONARIO " + cuestionarioId);
        System.out.println("==========================================");
        
        Optional<Cuestionario> cuestionarioOpt = obtenerConPreguntas(cuestionarioId);
        if (cuestionarioOpt.isEmpty()) {
            System.out.println("❌ CUESTIONARIO NO ENCONTRADO");
            return false;
        }
        
        Cuestionario cuestionario = cuestionarioOpt.get();
        System.out.println("✅ CUESTIONARIO ENCONTRADO CON " + cuestionario.getPreguntas().size() + " PREGUNTAS");
        
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
            
            System.out.println("🔍 Pregunta ID " + p.getId() + " - Nivel: " + nivelPregunta + " - Slot: " + slotPregunta + " - Buscando: " + slot);
            
            if (slot.equals(slotPregunta)) {
                System.out.println("✅ PREGUNTA ENCONTRADA EN SLOT " + slot + " - ID: " + p.getId());
                // Encontramos la pregunta en el slot, la eliminamos
                boolean resultado = quitarPregunta(cuestionarioId, p.getId());
                System.out.println("🔄 Resultado de quitarPregunta: " + resultado);
                return resultado;
            }
        }
        
        System.out.println("❌ NO SE ENCONTRÓ PREGUNTA EN EL SLOT " + slot);
        System.out.println("==========================================");
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