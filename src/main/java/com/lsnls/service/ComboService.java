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
import javax.persistence.EntityManager;
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

    /**
     * Obtiene combos disponibles para asignar a concursantes.
     * Incluye combos en estado 'aprobado' y 'adjudicado'.
     */
    public List<Combo> obtenerDisponiblesParaConcursantes() {
        List<Combo> aprobados = comboRepository.findByEstado(EstadoCombo.aprobado);
        List<Combo> adjudicados = comboRepository.findByEstado(EstadoCombo.adjudicado);
        
        List<Combo> disponibles = new java.util.ArrayList<>();
        disponibles.addAll(aprobados);
        disponibles.addAll(adjudicados);
        
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
            pc.setFactorMultiplicacion(factorMultiplicacion != null ? factorMultiplicacion.toString() : "1");
            
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
        System.out.println("==========================================");
        System.out.println("QUITANDO PREGUNTA " + preguntaId + " DEL COMBO " + comboId);
        System.out.println("==========================================");
        
        Optional<Combo> comboOpt = comboRepository.findById(comboId);
        Optional<Pregunta> preguntaOpt = preguntaRepository.findById(preguntaId);
        
        if (comboOpt.isPresent() && preguntaOpt.isPresent()) {
            Combo combo = comboOpt.get();
            Pregunta pregunta = preguntaOpt.get();
            
            System.out.println("✅ COMBO Y PREGUNTA ENCONTRADOS");
            
            // Eliminar la relación directamente con consulta nativa
            int relacionesEliminadas = entityManager.createNativeQuery(
                "DELETE FROM combos_preguntas WHERE combo_id = ? AND pregunta_id = ?")
                .setParameter(1, comboId)
                .setParameter(2, preguntaId)
                .executeUpdate();
            
            System.out.println("🗑️ RELACIONES ELIMINADAS: " + relacionesEliminadas);
            
            // Liberar la pregunta solo si no está en otros combos
            long otrosCombos = entityManager.createQuery(
                "SELECT COUNT(pc) FROM PreguntaCombo pc WHERE pc.pregunta.id = :preguntaId", Long.class)
                .setParameter("preguntaId", preguntaId)
                .getSingleResult();
                
            System.out.println("🔍 PREGUNTA " + preguntaId + " ESTÁ EN " + otrosCombos + " OTROS COMBOS");
            
            if (otrosCombos == 0 && pregunta.getEstadoDisponibilidad() == Pregunta.EstadoDisponibilidad.usada) {
                int preguntasLiberadas = entityManager.createNativeQuery(
                    "UPDATE preguntas SET estado_disponibilidad = 'liberada' WHERE id = ?")
                    .setParameter(1, preguntaId)
                    .executeUpdate();
                System.out.println("🔓 PREGUNTA LIBERADA: " + preguntasLiberadas);
            } else {
                System.out.println("ℹ️ PREGUNTA NO LIBERADA (está en otros combos o ya liberada)");
            }
            
            System.out.println("==========================================");
            return relacionesEliminadas > 0;
        }
        System.out.println("❌ COMBO O PREGUNTA NO ENCONTRADOS");
        System.out.println("==========================================");
        return false;
    }

    @Transactional
    public boolean actualizarFactorPregunta(Long comboId, Long preguntaId, String factorMultiplicacion) {
        try {
            System.out.println("[ACTUALIZAR FACTOR] Iniciando actualización de factor para combo " + comboId + 
                              ", pregunta " + preguntaId + ", nuevo factor: " + factorMultiplicacion);
            
            // Validar que el factor no esté vacío
            if (factorMultiplicacion == null || factorMultiplicacion.trim().isEmpty()) {
                factorMultiplicacion = "X"; // Valor por defecto
            }
            
            // Verificar que el combo existe
            Optional<Combo> comboOpt = comboRepository.findById(comboId);
            if (comboOpt.isEmpty()) {
                System.out.println("[ACTUALIZAR FACTOR] Combo no encontrado: " + comboId);
                return false;
            }
            
            // Verificar que la pregunta existe
            Optional<Pregunta> preguntaOpt = preguntaRepository.findById(preguntaId);
            if (preguntaOpt.isEmpty()) {
                System.out.println("[ACTUALIZAR FACTOR] Pregunta no encontrada: " + preguntaId);
                return false;
            }
            
            // Crear la clave primaria compuesta para buscar la relación
            PreguntaCombo.PreguntaComboId id = new PreguntaCombo.PreguntaComboId();
            id.setComboId(comboId);
            id.setPreguntaId(preguntaId);
            
            // Buscar la relación
            Optional<PreguntaCombo> pcOpt = preguntaComboRepository.findById(id);
            if (pcOpt.isEmpty()) {
                System.out.println("[ACTUALIZAR FACTOR] Relación pregunta-combo no encontrada");
                return false;
            }
            
            PreguntaCombo preguntaCombo = pcOpt.get();
            
            // Actualizar el factor
            System.out.println("[ACTUALIZAR FACTOR] Actualizando factor de " + 
                               preguntaCombo.getFactorMultiplicacion() + " a " + factorMultiplicacion);
            preguntaCombo.setFactorMultiplicacion(factorMultiplicacion);
            
            // Guardar la relación actualizada
            preguntaComboRepository.save(preguntaCombo);
            
            // Forzar flush para asegurar que se guarde en la base de datos
            entityManager.flush();
            
            System.out.println("[ACTUALIZAR FACTOR] Factor actualizado correctamente");
            return true;
        } catch (Exception e) {
            System.out.println("[ACTUALIZAR FACTOR] Error: " + e.getMessage());
            e.printStackTrace();
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

        // Verificar dependencias - no se puede eliminar si está adjudicado o grabado
        if (combo.getEstado() == Combo.EstadoCombo.adjudicado) {
            throw new IllegalArgumentException("No se puede eliminar el combo porque está adjudicado a una jornada. Desasígnalo primero.");
        }
        if (combo.getEstado() == Combo.EstadoCombo.grabado) {
            throw new IllegalArgumentException("No se puede eliminar el combo porque está grabado (asignado a concursantes). Desasígnalo primero.");
        }

        // Verificar si hay concursantes usando este combo
        Long concursantesCount = entityManager.createQuery(
            "SELECT COUNT(c) FROM Concursante c WHERE c.combo.id = :comboId", Long.class)
            .setParameter("comboId", id)
            .getSingleResult();
        
        if (concursantesCount > 0) {
            throw new IllegalArgumentException("No se puede eliminar el combo porque está siendo usado por " + 
                concursantesCount + " concursante(s). Desasígnalo primero.");
        }

        // Verificar si está en alguna jornada
        Long jornadasCount = entityManager.createQuery(
            "SELECT COUNT(j) FROM Jornada j JOIN j.combos c WHERE c.id = :comboId", Long.class)
            .setParameter("comboId", id)
            .getSingleResult();
            
        if (jornadasCount > 0) {
            throw new IllegalArgumentException("No se puede eliminar el combo porque está asignado a " + 
                jornadasCount + " jornada(s). Desasígnalo primero.");
        }

        // Si llegamos aquí, es seguro eliminar - liberar las preguntas asociadas
        Set<PreguntaCombo> preguntas = combo.getPreguntas();
        for (PreguntaCombo pc : preguntas) {
            // Cambiar a disponible
            entityManager.createNativeQuery(
                "UPDATE preguntas SET estado_disponibilidad = 'disponible' WHERE id = ?")
                .setParameter(1, pc.getPregunta().getId())
                .executeUpdate();
        }
        
        comboRepository.deleteById(id);
    }

    public int limpiarPreguntasInvalidas(Long comboId) {
        System.out.println("==========================================");
        System.out.println("LIMPIANDO PREGUNTAS INVÁLIDAS DEL COMBO " + comboId);
        System.out.println("==========================================");
        
        // Ya no eliminamos preguntas basadas en el factor, ya que ahora es un campo de texto libre
        int preguntasEliminadas = 0;
        
        System.out.println("✅ ELIMINADAS " + preguntasEliminadas + " PREGUNTAS CON FACTORES INVÁLIDOS");
        System.out.println("==========================================");
        
        return preguntasEliminadas;
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
            
            // Determinar slot según factor - ahora con manejo más flexible para factores personalizados
            String slot = null;
            String factor = pc.getFactorMultiplicacion();
            
            // Asignación simplificada basada en la posición
            if (factor != null) {
                System.out.println("[DEBUG_SLOT] Analizando pregunta ID=" + pc.getPregunta().getId() + 
                                  ", texto='" + pc.getPregunta().getPregunta() + "'" +
                                  ", factor='" + factor + "'");
                
                // Determinamos el slot según la posición en la lista
                int preguntaIndex = 0;
                for (PreguntaCombo pcTemp : c.getPreguntas()) {
                    if (pcTemp.getPregunta().getId().equals(pc.getPregunta().getId())) {
                        break;
                    }
                    preguntaIndex++;
                }
                
                // Asignamos slot según su posición (cíclica entre PM1, PM2, PM3)
                int slotIndex = preguntaIndex % 3;
                slot = "PM" + (slotIndex + 1);
                
                System.out.println("[SLOT_ASIGNADO] Pregunta " + pc.getPregunta().getId() + 
                                  " con factor '" + factor + "' asignada al slot " + slot);
            }
            
            ((Map<String, Object>) pcdto).put("slot", slot);
            
            // Verificar si el slot ya está ocupado (diagnóstico de problema)
            if (mapPorSlot.containsKey(slot)) {
                System.out.println("[ALERTA_SLOT_DUPLICADO] El slot " + slot + " ya está ocupado con otra pregunta!");
                Object preguntaExistente = mapPorSlot.get(slot);
                Pregunta pExistente = null;
                String factorExistente = null;
                if (preguntaExistente instanceof Map) {
                    Map<String, Object> mapExistente = (Map<String, Object>)preguntaExistente;
                    if (mapExistente.get("pregunta") instanceof Map) {
                        Map<String, Object> preguntaMap = (Map<String, Object>)mapExistente.get("pregunta");
                        System.out.println("  - Pregunta ya asignada: ID=" + preguntaMap.get("id") + 
                                         ", texto='" + preguntaMap.get("pregunta") + "'");
                    }
                    factorExistente = mapExistente.get("factorMultiplicacion") != null ? 
                                    mapExistente.get("factorMultiplicacion").toString() : "null";
                    System.out.println("  - Factor de pregunta existente: " + factorExistente);
                }
                System.out.println("  - Nueva pregunta: ID=" + pc.getPregunta().getId() + 
                                 ", texto='" + pc.getPregunta().getPregunta() + "'" +
                                 ", factor='" + factor + "'");
            }
            
            mapPorSlot.put(slot, pcdto);
        }
        
        // Verificar si hay preguntas sin asignar debido a colisiones
        boolean pm1Asignado = mapPorSlot.containsKey("PM1");
        boolean pm2Asignado = mapPorSlot.containsKey("PM2");
        boolean pm3Asignado = mapPorSlot.containsKey("PM3");
        
        System.out.println("[DEBUG_SLOTS] Estado de asignación: PM1=" + pm1Asignado + 
                         ", PM2=" + pm2Asignado + ", PM3=" + pm3Asignado);
        
        // Reorganizar slots si es necesario (asegurar un slot por pregunta)
        if (c.getPreguntas().size() > 0 && (!pm1Asignado || !pm2Asignado || !pm3Asignado)) {
            System.out.println("[REORGANIZAR_SLOTS] Se detectaron slots vacíos, reorganizando preguntas");
            
            // Crear mapa temporal con todas las preguntas
            java.util.List<Map<String, Object>> todasLasPreguntas = new java.util.ArrayList<>();
            for (PreguntaCombo pc : c.getPreguntas()) {
                Map<String, Object> pregMap = new java.util.HashMap<>();
                pregMap.put("id", pc.getPregunta().getId());
                pregMap.put("texto", pc.getPregunta().getPregunta());
                pregMap.put("factor", pc.getFactorMultiplicacion());
                pregMap.put("preguntaCombo", pc);
                todasLasPreguntas.add(pregMap);
            }
            
            // Ordenamos por ID para tener un orden estable y determinista
            java.util.Collections.sort(todasLasPreguntas, (a, b) -> {
                Long idA = (Long)a.get("id");
                Long idB = (Long)b.get("id");
                return idA.compareTo(idB);
            });
            
            // Resetear el mapa de slots
            mapPorSlot.clear();
            
            // Asignar preguntas a slots en orden
            java.util.List<String> slots = java.util.Arrays.asList("PM1", "PM2", "PM3");
            for (int i = 0; i < Math.min(todasLasPreguntas.size(), slots.size()); i++) {
                Map<String, Object> pregInfo = todasLasPreguntas.get(i);
                PreguntaCombo pc = (PreguntaCombo)pregInfo.get("preguntaCombo");
                
                Object pcdto = new java.util.HashMap<>();
                ((Map<String, Object>) pcdto).put("pregunta", mapPreguntaToDTO(pc.getPregunta()));
                ((Map<String, Object>) pcdto).put("factorMultiplicacion", pc.getFactorMultiplicacion());
                ((Map<String, Object>) pcdto).put("slot", slots.get(i));
                
                mapPorSlot.put(slots.get(i), pcdto);
                
                System.out.println("[REORGANIZAR_SLOTS] Pregunta ID=" + pc.getPregunta().getId() + 
                                 " con factor '" + pc.getFactorMultiplicacion() + 
                                 "' reasignada al slot " + slots.get(i));
            }
        }
        
        // Asegurar los 3 slots PM
        java.util.List<Object> preguntasDTO = new java.util.ArrayList<>();
        for (String slot : java.util.Arrays.asList("PM1", "PM2", "PM3")) {
            if (mapPorSlot.containsKey(slot)) {
                preguntasDTO.add(mapPorSlot.get(slot));
                System.out.println("[SLOTS_FINALES] " + slot + " ocupado");
            } else {
                // Slot vacío
                Object vacio = new java.util.HashMap<>();
                ((Map<String, Object>) vacio).put("slot", slot);
                ((Map<String, Object>) vacio).put("pregunta", null);
                ((Map<String, Object>) vacio).put("factorMultiplicacion", null);
                preguntasDTO.add(vacio);
                System.out.println("[SLOTS_FINALES] " + slot + " vacío");
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

    /**
     * Valida que un combo esté en el estado esperado para prevenir conflictos de concurrencia
     */
    private void validarEstadoComboParaAsignacion(Long comboId, Combo.EstadoCombo estadoEsperado) {
        Optional<Combo> comboOpt = comboRepository.findById(comboId);
        if (comboOpt.isEmpty()) {
            throw new IllegalArgumentException("Combo con ID " + comboId + " no encontrado");
        }
        
        Combo combo = comboOpt.get();
        if (combo.getEstado() != estadoEsperado) {
            throw new IllegalStateException("El combo " + comboId + " no está en estado '" + estadoEsperado + 
                "'. Estado actual: '" + combo.getEstado() + "'. Otro usuario pudo haberlo modificado.");
        }
    }

    /**
     * Cambia el estado de un combo de forma atómica con validación de concurrencia
     */
    @Transactional
    public boolean cambiarEstadoAtomico(Long comboId, Combo.EstadoCombo estadoActualEsperado, 
                                       Combo.EstadoCombo nuevoEstado) {
        // Usar query nativa para cambio atómico con verificación de estado
        int filasActualizadas = entityManager.createNativeQuery(
            "UPDATE combos SET estado = ? WHERE id = ? AND estado = ?")
            .setParameter(1, nuevoEstado.name())
            .setParameter(2, comboId)
            .setParameter(3, estadoActualEsperado.name())
            .executeUpdate();
            
        if (filasActualizadas == 0) {
            throw new IllegalStateException("No se pudo cambiar el estado del combo " + comboId + 
                " porque otro usuario lo modificó simultáneamente. Estado esperado: " + estadoActualEsperado);
        }
        
        return true;
    }

    /**
     * Verifica y reserva múltiples preguntas nivel 5 atómicamente para combos
     * @param preguntaIdsConFactores Map de ID de pregunta -> factor multiplicación
     * @return true si todas las preguntas fueron reservadas exitosamente
     * @throws IllegalArgumentException si alguna pregunta no está disponible
     */
    @Transactional
    public boolean verificarYReservarPreguntasComboAtomico(Map<Long, Integer> preguntaIdsConFactores) {
        List<Long> preguntaIds = new java.util.ArrayList<>(preguntaIdsConFactores.keySet());
        
        // PASO 1: Verificar que todas las preguntas existen y están en estado correcto
        List<Pregunta> preguntas = preguntaRepository.findAllById(preguntaIds);
        
        if (preguntas.size() != preguntaIds.size()) {
            throw new IllegalArgumentException("Una o más preguntas no fueron encontradas");
        }
        
        // Verificar el estado de cada pregunta
        for (Pregunta pregunta : preguntas) {
            if (pregunta.getEstado() != Pregunta.EstadoPregunta.aprobada) {
                throw new IllegalArgumentException("La pregunta " + pregunta.getId() + " no está aprobada (estado: " + pregunta.getEstado() + ")");
            }
            
            if (pregunta.getEstadoDisponibilidad() != Pregunta.EstadoDisponibilidad.disponible && 
                pregunta.getEstadoDisponibilidad() != Pregunta.EstadoDisponibilidad.liberada) {
                throw new IllegalArgumentException("La pregunta " + pregunta.getId() + " no está disponible (estado: " + pregunta.getEstadoDisponibilidad() + ")");
            }
            
            // Verificar que sea pregunta de nivel 5 para combos
            if (!pregunta.getNivel().name().startsWith("_5")) {
                throw new IllegalArgumentException("La pregunta " + pregunta.getId() + " no es de nivel 5. Solo se pueden usar preguntas de nivel 5 en combos");
            }
        }
        
        // PASO 2: Reservar todas las preguntas ATÓMICAMENTE con una sola query
        String preguntaIdsStr = preguntaIds.stream()
            .map(String::valueOf)
            .reduce((a, b) -> a + "," + b)
            .orElse("");
            
        int preguntasReservadas = entityManager.createNativeQuery(
            "UPDATE preguntas SET estado_disponibilidad = 'usada' " +
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
     * Libera múltiples preguntas de combo atómicamente
     */
    @Transactional
    public void liberarPreguntasComboAtomico(List<Long> preguntaIds) {
        if (preguntaIds.isEmpty()) return;
        
        String preguntaIdsStr = preguntaIds.stream()
            .map(String::valueOf)
            .reduce((a, b) -> a + "," + b)
            .orElse("");
            
        entityManager.createNativeQuery(
            "UPDATE preguntas SET estado_disponibilidad = 'liberada' " +
            "WHERE id IN (" + preguntaIdsStr + ") AND estado_disponibilidad = 'usada'"
        ).executeUpdate();
    }

    /**
     * Crea un combo con múltiples preguntas de forma atómica
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
            // Extraer el número del factor (X2 -> 2, X3 -> 3, X -> 1)
            String numeroFactor = factor.replaceAll("[^0-9]", "");
            if (numeroFactor.isEmpty()) {
                numeroFactor = "1"; // Si no hay número, usar 1
            }
            preguntaIdsConFactores.put(pm.getId(), Integer.valueOf(numeroFactor));
        }
        
        // PASO 2: VERIFICACIÓN Y RESERVA ATÓMICA de todas las preguntas
        try {
            verificarYReservarPreguntasComboAtomico(preguntaIdsConFactores);
        } catch (IllegalStateException e) {
            // Error de concurrencia - mensaje específico
            throw new IllegalArgumentException("Error de concurrencia al reservar preguntas: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // Error de validación - reenviar tal como está
            throw e;
        }
        
        // PASO 3: Crear el combo (las preguntas ya están reservadas)
        Combo combo = new Combo();
        combo.setCreacionUsuario(usuario);
        combo.setEstado(EstadoCombo.borrador);
        combo.setNivel(NivelCombo.NORMAL);
        combo.setTipo(Combo.TipoCombo.valueOf(dto.getTipo()));
        combo = comboRepository.save(combo);

        // PASO 4: Crear las relaciones pregunta-combo (preguntas ya reservadas)
        try {
            for (Map.Entry<Long, Integer> entry : preguntaIdsConFactores.entrySet()) {
                Long preguntaId = entry.getKey();
                Integer factor = entry.getValue();
                
                Pregunta pregunta = preguntaRepository.findById(preguntaId)
                    .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada: " + preguntaId));
                
                PreguntaCombo pc = new PreguntaCombo();
                PreguntaCombo.PreguntaComboId id = new PreguntaCombo.PreguntaComboId();
                id.setPreguntaId(preguntaId);
                id.setComboId(combo.getId());
                
                pc.setId(id);
                pc.setPregunta(pregunta);
                pc.setCombo(combo);
                pc.setFactorMultiplicacion(factor.toString());
                
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
} 