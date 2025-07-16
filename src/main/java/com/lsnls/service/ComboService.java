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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import jakarta.persistence.EntityManager;
import com.lsnls.dto.CrearComboDTO;
import java.util.Map;

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
    private EntityManager entityManager;

    public Combo crear(Combo combo) {
        combo.setFechaCreacion(LocalDateTime.now());
        combo.setEstado(EstadoCombo.borrador);
        return comboRepository.save(combo);
    }

    public List<Combo> obtenerTodos() {
        return comboRepository.findAll();
    }

    public Optional<Combo> obtenerPorId(Long id) {
        return comboRepository.findById(id);
    }

    public Optional<Combo> obtenerConPreguntas(Long id) {
        try {
            System.out.println("==========================================");
            System.out.println("CARGANDO COMBO " + id + " CON PREGUNTAS");
            System.out.println("==========================================");
            
            // Usar consulta JPQL que carga todo de una vez
            @SuppressWarnings("unchecked")
            List<Combo> resultados = entityManager.createQuery(
                "SELECT DISTINCT c FROM Combo c " +
                "LEFT JOIN FETCH c.preguntas pc " +
                "LEFT JOIN FETCH pc.pregunta p " +
                "LEFT JOIN FETCH p.creacionUsuario " +
                "WHERE c.id = :comboId"
            ).setParameter("comboId", id).getResultList();
            
            if (resultados.isEmpty()) {
                System.out.println("❌ COMBO " + id + " NO ENCONTRADO");
                return Optional.empty();
            }
            
            Combo combo = resultados.get(0);
            System.out.println("✅ COMBO " + id + " ENCONTRADO");
            System.out.println("📊 PREGUNTAS CARGADAS: " + combo.getPreguntas().size());
            
            // Mostrar detalles de las preguntas
            if (!combo.getPreguntas().isEmpty()) {
                System.out.println("📋 LISTADO DE PREGUNTAS:");
                int i = 1;
                for (PreguntaCombo pc : combo.getPreguntas()) {
                    System.out.println("  " + i + ". ID: " + pc.getPregunta().getId() + 
                                     " | TEXTO: " + pc.getPregunta().getPregunta() + 
                                     " | FACTOR: " + pc.getFactorMultiplicacion());
                    i++;
                }
            } else {
                System.out.println("❌ NO SE ENCONTRARON PREGUNTAS PARA EL COMBO " + id);
            }
            
            System.out.println("==========================================");
            return Optional.of(combo);
            
        } catch (Exception e) {
            System.out.println("💥 ERROR EN obtenerConPreguntas: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public List<Combo> obtenerPorEstado(EstadoCombo estado) {
        return comboRepository.findByEstado(estado);
    }

    public List<Combo> obtenerPorNivel(NivelCombo nivel) {
        return comboRepository.findByNivel(nivel);
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

    public Combo cambiarEstado(Long id, EstadoCombo nuevoEstado) {
        return comboRepository.findById(id).map(combo -> {
            combo.setEstado(nuevoEstado);
            return comboRepository.save(combo);
        }).orElse(null);
    }

    public boolean agregarPregunta(Long comboId, Long preguntaId, Integer factorMultiplicacion) {
        Optional<Combo> comboOpt = comboRepository.findById(comboId);
        Optional<Pregunta> preguntaOpt = preguntaRepository.findById(preguntaId);
        
        if (comboOpt.isPresent() && preguntaOpt.isPresent()) {
            Combo combo = comboOpt.get();
            Pregunta pregunta = preguntaOpt.get();
            
            // Verificar que la pregunta esté aprobada
            if (pregunta.getEstado() != Pregunta.EstadoPregunta.aprobada) {
                throw new RuntimeException("La pregunta debe estar aprobada para ser agregada a un combo");
            }
            
            // Verificar que sea pregunta de nivel 5
            if (!pregunta.getNivel().name().startsWith("_5")) {
                throw new RuntimeException("Solo se pueden agregar preguntas de nivel 5 a los combos");
            }
            
            // Verificar que la pregunta esté disponible o liberada
            if (pregunta.getEstadoDisponibilidad() != Pregunta.EstadoDisponibilidad.disponible && 
                pregunta.getEstadoDisponibilidad() != Pregunta.EstadoDisponibilidad.liberada) {
                throw new RuntimeException("La pregunta no está disponible (estado: " + pregunta.getEstadoDisponibilidad() + ")");
            }
            
            // Verificar que la pregunta no esté ya en este combo
            PreguntaCombo.PreguntaComboId checkId = new PreguntaCombo.PreguntaComboId();
            checkId.setPreguntaId(preguntaId);
            checkId.setComboId(comboId);
            
            if (preguntaComboRepository.existsById(checkId)) {
                throw new RuntimeException("La pregunta ya está agregada a este combo");
            }
            
            // Crear la relación pregunta-combo
            PreguntaCombo pc = new PreguntaCombo();
            PreguntaCombo.PreguntaComboId id = new PreguntaCombo.PreguntaComboId();
            id.setPreguntaId(preguntaId);
            id.setComboId(comboId);
            
            pc.setId(id);
            pc.setPregunta(pregunta);
            pc.setCombo(combo);
            pc.setFactorMultiplicacion(factorMultiplicacion != null ? factorMultiplicacion : 1);
            
            // Guardar la relación en la base de datos
            preguntaComboRepository.save(pc);
            
            // Marcar pregunta como usada
            int rowsUpdated = entityManager.createNativeQuery(
                "UPDATE preguntas SET estado_disponibilidad = 'usada' WHERE id = ?")
                .setParameter(1, preguntaId)
                .executeUpdate();
            
            return true;
        }
        return false;
    }

    public boolean quitarPregunta(Long comboId, Long preguntaId) {
        Optional<Combo> comboOpt = comboRepository.findById(comboId);
        Optional<Pregunta> preguntaOpt = preguntaRepository.findById(preguntaId);
        
        if (comboOpt.isPresent() && preguntaOpt.isPresent()) {
            Combo combo = comboOpt.get();
            Pregunta pregunta = preguntaOpt.get();
            
            // Buscar y eliminar la relación pregunta-combo
            PreguntaCombo.PreguntaComboId id = new PreguntaCombo.PreguntaComboId();
            id.setPreguntaId(preguntaId);
            id.setComboId(comboId);
            
            // Eliminar la relación
            preguntaComboRepository.deleteById(id);
            
            // Liberar la pregunta
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
        Optional<Combo> comboOpt = comboRepository.findById(id);
        if (comboOpt.isPresent()) {
            Combo combo = comboOpt.get();
            Set<PreguntaCombo> preguntas = combo.getPreguntas();
            for (PreguntaCombo pc : preguntas) {
                // Cambiar a disponible
                entityManager.createNativeQuery(
                    "UPDATE preguntas SET estado_disponibilidad = 'disponible' WHERE id = ?")
                    .setParameter(1, pc.getPregunta().getId())
                    .executeUpdate();
            }
        }
        comboRepository.deleteById(id);
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
        dto.put("estado", c.getEstado());
        dto.put("tipo", c.getTipo());
        dto.put("fechaCreacion", c.getFechaCreacion() != null ? c.getFechaCreacion().toString() : null);
        
        // Mapear preguntas a slots PM1, PM2, PM3
        java.util.Map<String, Object> mapPorSlot = new java.util.HashMap<>();
        
        // Primero, mapear las preguntas existentes a su slot
        for (PreguntaCombo pc : c.getPreguntas()) {
            Object pcdto = new java.util.HashMap<>();
            Pregunta p = pc.getPregunta();
            ((Map<String, Object>) pcdto).put("pregunta", mapPreguntaToDTO(p));
            ((Map<String, Object>) pcdto).put("factorMultiplicacion", pc.getFactorMultiplicacion());
            
            // Determinar slot según factor
            String slot = null;
            if (pc.getFactorMultiplicacion() == 2) slot = "PM1";
            else if (pc.getFactorMultiplicacion() == 3) slot = "PM2";
            else if (pc.getFactorMultiplicacion() == 0) slot = "PM3";
            
            ((Map<String, Object>) pcdto).put("slot", slot);
            mapPorSlot.put(slot, pcdto);
        }
        
        // Asegurar los 3 slots PM
        java.util.List<Object> preguntasDTO = new java.util.ArrayList<>();
        for (String slot : new String[]{"PM1", "PM2", "PM3"}) {
            if (mapPorSlot.containsKey(slot)) {
                preguntasDTO.add(mapPorSlot.get(slot));
            } else {
                // Slot vacío
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
} 