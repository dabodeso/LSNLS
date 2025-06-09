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
import jakarta.persistence.EntityManager;
import com.lsnls.dto.CrearCuestionarioDTO;
import com.lsnls.dto.PreguntaCuestionarioDTO;
import com.lsnls.dto.PreguntaDTO;
import java.util.Map;

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

    public Cuestionario crear(Cuestionario cuestionario) {
        cuestionario.setFechaCreacion(LocalDateTime.now());
        cuestionario.setEstado(EstadoCuestionario.borrador);
        return cuestionarioRepository.save(cuestionario);
    }

    public List<Cuestionario> obtenerTodos() {
        return cuestionarioRepository.findAll();
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

    public boolean agregarPregunta(Long cuestionarioId, Long preguntaId, Integer factorMultiplicacion) {
        Optional<Cuestionario> cuestionarioOpt = cuestionarioRepository.findById(cuestionarioId);
        Optional<Pregunta> preguntaOpt = preguntaRepository.findById(preguntaId);
        
        if (cuestionarioOpt.isPresent() && preguntaOpt.isPresent()) {
            Cuestionario cuestionario = cuestionarioOpt.get();
            Pregunta pregunta = preguntaOpt.get();
            
            // Verificar que la pregunta esté aprobada
            if (pregunta.getEstado() != Pregunta.EstadoPregunta.aprobada) {
                throw new RuntimeException("La pregunta debe estar aprobada para ser agregada a un cuestionario");
            }
            
            // Verificar que la pregunta esté disponible o liberada (puede reutilizarse)
            if (pregunta.getEstadoDisponibilidad() != Pregunta.EstadoDisponibilidad.disponible && 
                pregunta.getEstadoDisponibilidad() != Pregunta.EstadoDisponibilidad.liberada) {
                throw new RuntimeException("La pregunta no está disponible (estado: " + pregunta.getEstadoDisponibilidad() + ")");
            }
            
            // Verificar que la pregunta no esté ya en este cuestionario
            PreguntaCuestionario.PreguntaCuestionarioId checkId = new PreguntaCuestionario.PreguntaCuestionarioId();
            checkId.setPreguntaId(preguntaId);
            checkId.setCuestionarioId(cuestionarioId);
            
            if (preguntaCuestionarioRepository.existsById(checkId)) {
                throw new RuntimeException("La pregunta ya está agregada a este cuestionario");
            }
            
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
            int rowsUpdated = entityManager.createNativeQuery(
                "UPDATE preguntas SET estado_disponibilidad = 'usada' WHERE id = ?")
                .setParameter(1, preguntaId)
                .executeUpdate();
            
            return true;
        }
        return false;
    }

    public boolean quitarPregunta(Long cuestionarioId, Long preguntaId) {
        Optional<Cuestionario> cuestionarioOpt = cuestionarioRepository.findById(cuestionarioId);
        Optional<Pregunta> preguntaOpt = preguntaRepository.findById(preguntaId);
        
        if (cuestionarioOpt.isPresent() && preguntaOpt.isPresent()) {
            Cuestionario cuestionario = cuestionarioOpt.get();
            Pregunta pregunta = preguntaOpt.get();
            
            // Buscar y eliminar la relación pregunta-cuestionario
            PreguntaCuestionario.PreguntaCuestionarioId id = new PreguntaCuestionario.PreguntaCuestionarioId();
            id.setPreguntaId(preguntaId);
            id.setCuestionarioId(cuestionarioId);
            
            // Eliminar la relación
            preguntaCuestionarioRepository.deleteById(id);
            
            // Liberar la pregunta usando consulta SQL directa para evitar validaciones
            if (pregunta.getEstadoDisponibilidad() == Pregunta.EstadoDisponibilidad.usada) {
                entityManager.createNativeQuery(
                    "UPDATE preguntas SET estado_disponibilidad = 'liberada' WHERE id = ?")
                    .setParameter(1, preguntaId)
                    .executeUpdate();
            }
            
            return true;
        }
        return false;
    }

    public void eliminar(Long id) {
        // Antes de eliminar, poner las preguntas asociadas como disponibles
        Optional<Cuestionario> cuestionarioOpt = cuestionarioRepository.findById(id);
        if (cuestionarioOpt.isPresent()) {
            Cuestionario cuestionario = cuestionarioOpt.get();
            Set<PreguntaCuestionario> preguntas = cuestionario.getPreguntas();
            for (PreguntaCuestionario pc : preguntas) {
                // Cambiar a disponible
                entityManager.createNativeQuery(
                    "UPDATE preguntas SET estado_disponibilidad = 'disponible' WHERE id = ?")
                    .setParameter(1, pc.getPregunta().getId())
                    .executeUpdate();
            }
        }
        cuestionarioRepository.deleteById(id);
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

    public Cuestionario crearDesdeDTO(CrearCuestionarioDTO dto, Usuario usuario) {
        Cuestionario cuestionario = new Cuestionario();
        cuestionario.setCreacionUsuario(usuario);
        cuestionario.setEstado(Cuestionario.EstadoCuestionario.borrador);
        cuestionario.setFechaCreacion(LocalDateTime.now());
        cuestionario.setNivel(Cuestionario.NivelCuestionario.NORMAL);
        cuestionario = cuestionarioRepository.save(cuestionario);

        // Asociar preguntas normales (factor 1)
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
            // Marcar pregunta como usada
            pregunta.setEstadoDisponibilidad(Pregunta.EstadoDisponibilidad.usada);
            preguntaRepository.save(pregunta);
        }
        // Asociar preguntas multiplicadoras
        for (CrearCuestionarioDTO.PreguntaMultiplicadoraDTO pm : dto.getPreguntasMultiplicadoras()) {
            Pregunta pregunta = preguntaRepository.findById(pm.getId())
                .orElseThrow(() -> new IllegalArgumentException("Pregunta multiplicadora no encontrada: " + pm.getId()));
            PreguntaCuestionario pc = new PreguntaCuestionario();
            PreguntaCuestionario.PreguntaCuestionarioId pcid = new PreguntaCuestionario.PreguntaCuestionarioId();
            pcid.setPreguntaId(pm.getId());
            pcid.setCuestionarioId(cuestionario.getId());
            pc.setId(pcid);
            pc.setPregunta(pregunta);
            pc.setCuestionario(cuestionario);
            int factor = 1;
            if ("X2".equalsIgnoreCase(pm.getFactor())) factor = 2;
            else if ("X3".equalsIgnoreCase(pm.getFactor())) factor = 3;
            else if ("X".equalsIgnoreCase(pm.getFactor())) factor = 0;
            pc.setFactorMultiplicacion(factor);
            preguntaCuestionarioRepository.save(pc);
            // Marcar pregunta como usada
            pregunta.setEstadoDisponibilidad(Pregunta.EstadoDisponibilidad.usada);
            preguntaRepository.save(pregunta);
        }
        return cuestionarioRepository.findById(cuestionario.getId()).orElse(cuestionario);
    }

    public Cuestionario actualizarDesdeDTO(Long id, CrearCuestionarioDTO dto) {
        Optional<Cuestionario> optCuestionario = cuestionarioRepository.findById(id);
        if (optCuestionario.isEmpty()) {
            throw new IllegalArgumentException("Cuestionario no encontrado");
        }
        Cuestionario cuestionario = optCuestionario.get();

        // Eliminar todas las relaciones actuales de preguntas
        Set<PreguntaCuestionario> actuales = new HashSet<>(cuestionario.getPreguntas());
        for (PreguntaCuestionario pc : actuales) {
            // Liberar la pregunta
            Pregunta pregunta = pc.getPregunta();
            pregunta.setEstadoDisponibilidad(Pregunta.EstadoDisponibilidad.liberada);
            preguntaRepository.save(pregunta);
            preguntaCuestionarioRepository.delete(pc);
        }
        cuestionario.getPreguntas().clear();

        // Asociar nuevas preguntas normales (factor 1)
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
            // Marcar pregunta como usada
            pregunta.setEstadoDisponibilidad(Pregunta.EstadoDisponibilidad.usada);
            preguntaRepository.save(pregunta);
        }
        // Asociar nuevas preguntas multiplicadoras
        for (CrearCuestionarioDTO.PreguntaMultiplicadoraDTO pm : dto.getPreguntasMultiplicadoras()) {
            Pregunta pregunta = preguntaRepository.findById(pm.getId())
                .orElseThrow(() -> new IllegalArgumentException("Pregunta multiplicadora no encontrada: " + pm.getId()));
            PreguntaCuestionario pc = new PreguntaCuestionario();
            PreguntaCuestionario.PreguntaCuestionarioId pcid = new PreguntaCuestionario.PreguntaCuestionarioId();
            pcid.setPreguntaId(pm.getId());
            pcid.setCuestionarioId(cuestionario.getId());
            pc.setId(pcid);
            pc.setPregunta(pregunta);
            pc.setCuestionario(cuestionario);
            int factor = 1;
            if ("X2".equalsIgnoreCase(pm.getFactor())) factor = 2;
            else if ("X3".equalsIgnoreCase(pm.getFactor())) factor = 3;
            else if ("X".equalsIgnoreCase(pm.getFactor())) factor = 0;
            pc.setFactorMultiplicacion(factor);
            preguntaCuestionarioRepository.save(pc);
            // Marcar pregunta como usada
            pregunta.setEstadoDisponibilidad(Pregunta.EstadoDisponibilidad.usada);
            preguntaRepository.save(pregunta);
        }
        return cuestionarioRepository.findById(cuestionario.getId()).orElse(cuestionario);
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
        // Mapear preguntas a slots
        java.util.Map<String, PreguntaCuestionarioDTO> mapPorSlot = new java.util.HashMap<>();
        // Primero, mapear las preguntas existentes a su slot
        for (PreguntaCuestionario pc : c.getPreguntas()) {
            PreguntaCuestionarioDTO pcdto = new PreguntaCuestionarioDTO();
            Pregunta p = pc.getPregunta();
            pcdto.setPregunta(mapPreguntaToDTO(p));
            pcdto.setFactorMultiplicacion(pc.getFactorMultiplicacion());
            // Determinar slot
            String slot = null;
            if (pc.getFactorMultiplicacion() == null || pc.getFactorMultiplicacion() == 1) {
                // Buscar el primer slot normal libre
                for (String s : new String[]{"1LS","2NLS","3LS","4NLS"}) {
                    if (!mapPorSlot.containsKey(s)) { slot = s; break; }
                }
            } else {
                if (pc.getFactorMultiplicacion() == 2) slot = "PM1";
                else if (pc.getFactorMultiplicacion() == 3) slot = "PM2";
                else if (pc.getFactorMultiplicacion() == 0) slot = "PM3";
            }
            pcdto.setSlot(slot);
            mapPorSlot.put(slot, pcdto);
        }
        // Ahora, asegurar los 7 slots
        java.util.List<PreguntaCuestionarioDTO> preguntasDTO = new java.util.ArrayList<>();
        for (String slot : new String[]{"1LS","2NLS","3LS","4NLS","PM1","PM2","PM3"}) {
            PreguntaCuestionarioDTO pcdto = mapPorSlot.getOrDefault(slot, null);
            if (pcdto == null) {
                pcdto = new PreguntaCuestionarioDTO();
                pcdto.setSlot(slot);
                pcdto.setPregunta(null);
                pcdto.setFactorMultiplicacion(null);
            }
            preguntasDTO.add(pcdto);
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
} 