package com.lsnls.service;

import com.lsnls.dto.ConcursanteDTO;
import com.lsnls.entity.Concursante;
import com.lsnls.entity.Cuestionario;
import com.lsnls.entity.Combo;
import com.lsnls.entity.Jornada;
import com.lsnls.repository.ConcursanteRepository;
import com.lsnls.repository.CuestionarioRepository;
import com.lsnls.repository.ComboRepository;
import com.lsnls.repository.JornadaRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Service
public class ConcursanteService {

    @Autowired
    private ConcursanteRepository concursanteRepository;

    @Autowired
    private CuestionarioRepository cuestionarioRepository;

    @Autowired
    private ComboRepository comboRepository;

    @Autowired
    private JornadaRepository jornadaRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private CuestionarioService cuestionarioService;

    @Autowired
    private ComboService comboService;

    @Value("${upload.directory}")
    private String uploadDirectory;

    public List<ConcursanteDTO> findAll() {
        return concursanteRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public Page<ConcursanteDTO> findAllPaginated(Pageable pageable) {
        return concursanteRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    public Page<ConcursanteDTO> findAllPaginatedWithFilters(Pageable pageable,
            String estado, String jornada, String lugar, String numeroPrograma,
            String duracionFinalMin, String duracionFinalMax, String valoracionFinal, String bonico, String busqueda) {
        Specification<Concursante> spec = Specification.where(null);

        if (estado != null && !estado.isBlank()) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("estado"), estado));
        }
        if (jornada != null && !jornada.isBlank()) {
            spec = spec.and((root, cq, cb) -> {
                try {
                    // si es numérico, comparar por id
                    Long jid = Long.valueOf(jornada);
                    return cb.equal(root.join("jornada", javax.persistence.criteria.JoinType.LEFT).get("id"), jid);
                } catch (NumberFormatException e) {
                    // si no, comparar por nombre like
                    return cb.like(cb.lower(root.join("jornada", javax.persistence.criteria.JoinType.LEFT).get("nombre")), "%" + jornada.toLowerCase() + "%");
                }
            });
        }
        if (lugar != null && !lugar.isBlank()) {
            spec = spec.and((root, cq, cb) -> cb.like(cb.lower(root.get("lugar")), "%" + lugar.toLowerCase() + "%"));
        }
        if (numeroPrograma != null && !numeroPrograma.isBlank()) {
            spec = spec.and((root, cq, cb) -> cb.like(root.get("numeroPrograma").as(String.class), "%" + numeroPrograma + "%"));
        }
        if (duracionFinalMin != null && !duracionFinalMin.isBlank()) {
            spec = spec.and((root, cq, cb) -> {
                javax.persistence.criteria.Expression<String> df = root.get("duracionFinal").as(String.class);
                javax.persistence.criteria.Expression<String> dd = root.get("duracionDireccion").as(String.class);
                javax.persistence.criteria.Expression<String> dg = root.get("duracion").as(String.class);
                javax.persistence.criteria.Predicate p1 = cb.and(cb.isNotNull(df), cb.greaterThanOrEqualTo(df, duracionFinalMin));
                javax.persistence.criteria.Predicate p2 = cb.and(cb.isNull(df), cb.isNotNull(dd), cb.greaterThanOrEqualTo(dd, duracionFinalMin));
                javax.persistence.criteria.Predicate p3 = cb.and(cb.isNull(df), cb.isNull(dd), cb.isNotNull(dg), cb.greaterThanOrEqualTo(dg, duracionFinalMin));
                return cb.or(p1, p2, p3);
            });
        }
        if (duracionFinalMax != null && !duracionFinalMax.isBlank()) {
            spec = spec.and((root, cq, cb) -> {
                javax.persistence.criteria.Expression<String> df = root.get("duracionFinal").as(String.class);
                javax.persistence.criteria.Expression<String> dd = root.get("duracionDireccion").as(String.class);
                javax.persistence.criteria.Expression<String> dg = root.get("duracion").as(String.class);
                javax.persistence.criteria.Predicate p1 = cb.and(cb.isNotNull(df), cb.lessThanOrEqualTo(df, duracionFinalMax));
                javax.persistence.criteria.Predicate p2 = cb.and(cb.isNull(df), cb.isNotNull(dd), cb.lessThanOrEqualTo(dd, duracionFinalMax));
                javax.persistence.criteria.Predicate p3 = cb.and(cb.isNull(df), cb.isNull(dd), cb.isNotNull(dg), cb.lessThanOrEqualTo(dg, duracionFinalMax));
                return cb.or(p1, p2, p3);
            });
        }
        if (valoracionFinal != null && !valoracionFinal.isBlank()) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("valoracionFinal"), valoracionFinal));
        }
        if (bonico != null && !bonico.isBlank()) {
            spec = spec.and((root, cq, cb) -> {
                if ("vacio".equalsIgnoreCase(bonico)) {
                    return cb.or(cb.isNull(root.get("bonico")), cb.equal(root.get("bonico"), ""));
                } else if ("contenido".equalsIgnoreCase(bonico)) {
                    return cb.and(cb.isNotNull(root.get("bonico")), cb.notEqual(root.get("bonico"), ""));
                }
                return cb.conjunction();
            });
        }
        if (busqueda != null && !busqueda.isBlank()) {
            final String like = "%" + busqueda.toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> cb.like(cb.lower(root.get("nombre")), like));
        }

        Page<Concursante> page = concursanteRepository.findAll(spec, pageable);
        return page.map(this::convertToDTO);
    }

    public ConcursanteDTO findById(Long id) {
        return concursanteRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    @Transactional
    public ConcursanteDTO create(ConcursanteDTO concursanteDTO) {
        Concursante concursante = convertToEntity(concursanteDTO);
        
        // Generar número de concursante automáticamente
        if (concursante.getNumeroConcursante() == null) {
            Integer siguienteNumero = generarSiguienteNumeroConcursante();
            concursante.setNumeroConcursante(siguienteNumero);
        }
        
        // Si se asignó un cuestionario, verificar si ya está asignado a otro concursante y desasignarlo
        if (concursante.getCuestionario() != null) {
            Cuestionario cuestionario = concursante.getCuestionario();
            
            // Verificar si el cuestionario ya está asignado a otro concursante
            @SuppressWarnings("unchecked")
            List<Concursante> concursantesConMismoCuestionario = entityManager.createQuery(
                "SELECT c FROM Concursante c WHERE c.cuestionario.id = :cuestionarioId"
            )
            .setParameter("cuestionarioId", cuestionario.getId())
            .getResultList();
            
            // Si está asignado a otro concursante, lanzar error
            if (!concursantesConMismoCuestionario.isEmpty()) {
                Concursante otroConcursante = concursantesConMismoCuestionario.get(0);
                throw new RuntimeException("El cuestionario " + cuestionario.getId() + 
                    " ya está asignado al concursante " + otroConcursante.getNumeroConcursante() + 
                    " (" + otroConcursante.getNombre() + "). Debe desasignarlo primero antes de asignarlo a otro concursante.");
            }
            
            // Validar que el cuestionario esté en estado válido para asignación
            // Permitir aprobado, adjudicado y grabado (grabado puede estar siendo reasignado)
            if (cuestionario.getEstado() != Cuestionario.EstadoCuestionario.aprobado && 
                cuestionario.getEstado() != Cuestionario.EstadoCuestionario.adjudicado &&
                cuestionario.getEstado() != Cuestionario.EstadoCuestionario.grabado) {
                throw new RuntimeException("Solo se pueden asignar cuestionarios en estado 'aprobado', 'adjudicado' o 'grabado'. El cuestionario " + 
                                         cuestionario.getId() + " está en estado: " + cuestionario.getEstado());
            }
            
            // Cambiar estado a 'grabado' cuando se asigna a un concursante (solo si no está ya en grabado)
            if (cuestionario.getEstado() != Cuestionario.EstadoCuestionario.grabado) {
                cuestionario.setEstado(Cuestionario.EstadoCuestionario.grabado);
                cuestionarioRepository.save(cuestionario);
            }
        }
        
        // Si se asignó un combo, verificar si ya está asignado a otro concursante y desasignarlo
        if (concursante.getCombo() != null) {
            Combo combo = concursante.getCombo();
            
            // Verificar si el combo ya está asignado a otro concursante
            @SuppressWarnings("unchecked")
            List<Concursante> concursantesConMismoCombo = entityManager.createQuery(
                "SELECT c FROM Concursante c WHERE c.combo.id = :comboId"
            )
            .setParameter("comboId", combo.getId())
            .getResultList();
            
            // Si está asignado a otro concursante, lanzar error
            if (!concursantesConMismoCombo.isEmpty()) {
                Concursante otroConcursante = concursantesConMismoCombo.get(0);
                throw new RuntimeException("El combo " + combo.getId() + 
                    " ya está asignado al concursante " + otroConcursante.getNumeroConcursante() + 
                    " (" + otroConcursante.getNombre() + "). Debe desasignarlo primero antes de asignarlo a otro concursante.");
            }
            
            // Validar que el combo esté en estado válido para asignación
            // Permitir aprobado, adjudicado y grabado (grabado puede estar siendo reasignado)
            if (combo.getEstado() != Combo.EstadoCombo.aprobado && 
                combo.getEstado() != Combo.EstadoCombo.adjudicado &&
                combo.getEstado() != Combo.EstadoCombo.grabado) {
                throw new RuntimeException("Solo se pueden asignar combos en estado 'aprobado', 'adjudicado' o 'grabado'. El combo " + 
                                         combo.getId() + " está en estado: " + combo.getEstado());
            }
            
            // Cambiar estado a 'grabado' cuando se asigna a un concursante (solo si no está ya en grabado)
            if (combo.getEstado() != Combo.EstadoCombo.grabado) {
                combo.setEstado(Combo.EstadoCombo.grabado);
                comboRepository.save(combo);
            }
        }
        
        concursante = concursanteRepository.save(concursante);
        return convertToDTO(concursante);
    }

    /**
     * Genera el siguiente número de concursante automáticamente
     * PROTEGIDO CONTRA RACE CONDITIONS con synchronized
     */
    private synchronized Integer generarSiguienteNumeroConcursante() {
        Integer maxNumero = concursanteRepository.findMaxNumeroConcursante();
        return (maxNumero != null) ? maxNumero + 1 : 1;
    }

    @Transactional
    public ConcursanteDTO update(Long id, ConcursanteDTO concursanteDTO) {
        Concursante concursante = concursanteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Concursante no encontrado"));

        // Obtener el cuestionario anterior para comparar
        Cuestionario cuestionarioAnterior = concursante.getCuestionario();
        
        // Obtener el combo anterior para comparar
        Combo comboAnterior = concursante.getCombo();
        
        BeanUtils.copyProperties(concursanteDTO, concursante, "id");
        
        // Si se asignó un cuestionario nuevo, cambiar su estado a grabado
        if (concursante.getCuestionario() != null && 
            (cuestionarioAnterior == null || !cuestionarioAnterior.getId().equals(concursante.getCuestionario().getId()))) {
            Cuestionario cuestionario = concursante.getCuestionario();
            if (cuestionario.getEstado() == Cuestionario.EstadoCuestionario.adjudicado) {
                cuestionario.setEstado(Cuestionario.EstadoCuestionario.grabado);
                cuestionarioRepository.save(cuestionario);
            }
        }
        
        // Si se asignó un combo nuevo, cambiar su estado a grabado
        if (concursante.getCombo() != null && 
            (comboAnterior == null || !comboAnterior.getId().equals(concursante.getCombo().getId()))) {
            Combo combo = concursante.getCombo();
            if (combo.getEstado() == Combo.EstadoCombo.adjudicado) {
                combo.setEstado(Combo.EstadoCombo.grabado);
                comboRepository.save(combo);
            }
        }
        
        // Manejar cuestionario con lógica de estados
        if (concursanteDTO.getCuestionarioId() != null) {
            Cuestionario cuestionarioNuevo = cuestionarioRepository.findById(concursanteDTO.getCuestionarioId())
                    .orElseThrow(() -> new RuntimeException("Cuestionario no encontrado: " + concursanteDTO.getCuestionarioId()));
            
            // Solo cambiar estado si es un cuestionario diferente al anterior
            if (cuestionarioAnterior == null || !cuestionarioAnterior.getId().equals(cuestionarioNuevo.getId())) {
                // Si había un cuestionario anterior, restaurar su estado a 'aprobado'
                if (cuestionarioAnterior != null && cuestionarioAnterior.getEstado() == Cuestionario.EstadoCuestionario.grabado) {
                    cuestionarioAnterior.setEstado(Cuestionario.EstadoCuestionario.aprobado);
                    cuestionarioRepository.save(cuestionarioAnterior);
                }
                
                // Verificar si el cuestionario ya está asignado a otro concursante
                @SuppressWarnings("unchecked")
                List<Concursante> concursantesConMismoCuestionario = entityManager.createQuery(
                    "SELECT c FROM Concursante c WHERE c.cuestionario.id = :cuestionarioId AND c.id != :concursanteId"
                )
                .setParameter("cuestionarioId", cuestionarioNuevo.getId())
                .setParameter("concursanteId", id)
                .getResultList();
                
                // Si está asignado a otro concursante, lanzar error
                if (!concursantesConMismoCuestionario.isEmpty()) {
                    Concursante otroConcursante = concursantesConMismoCuestionario.get(0);
                    throw new RuntimeException("El cuestionario " + cuestionarioNuevo.getId() + 
                        " ya está asignado al concursante " + otroConcursante.getNumeroConcursante() + 
                        " (" + otroConcursante.getNombre() + "). Debe desasignarlo primero antes de asignarlo a otro concursante.");
                }
                
                // Validar que el cuestionario esté en estado válido para asignación
                // Permitir aprobado, adjudicado y grabado (grabado puede estar siendo reasignado)
                if (cuestionarioNuevo.getEstado() != Cuestionario.EstadoCuestionario.aprobado && 
                    cuestionarioNuevo.getEstado() != Cuestionario.EstadoCuestionario.adjudicado &&
                    cuestionarioNuevo.getEstado() != Cuestionario.EstadoCuestionario.grabado) {
                    throw new RuntimeException("Solo se pueden asignar cuestionarios en estado 'aprobado', 'adjudicado' o 'grabado'. El cuestionario " + 
                                             cuestionarioNuevo.getId() + " está en estado: " + cuestionarioNuevo.getEstado());
                }
                
                // Cambiar estado a 'grabado' cuando se asigna a un concursante (solo si no está ya en grabado)
                if (cuestionarioNuevo.getEstado() != Cuestionario.EstadoCuestionario.grabado) {
                    cuestionarioNuevo.setEstado(Cuestionario.EstadoCuestionario.grabado);
                    cuestionarioRepository.save(cuestionarioNuevo);
                }
            }
            
            concursante.setCuestionario(cuestionarioNuevo);
        } else {
            // Si se desasigna el cuestionario, restaurar su estado a 'aprobado'
            if (cuestionarioAnterior != null && cuestionarioAnterior.getEstado() == Cuestionario.EstadoCuestionario.grabado) {
                cuestionarioAnterior.setEstado(Cuestionario.EstadoCuestionario.aprobado);
                cuestionarioRepository.save(cuestionarioAnterior);
            }
            concursante.setCuestionario(null);
        }
        
        // Manejar combo con lógica de estados
        if (concursanteDTO.getComboId() != null) {
            Combo comboNuevo = comboRepository.findById(concursanteDTO.getComboId())
                    .orElseThrow(() -> new RuntimeException("Combo no encontrado: " + concursanteDTO.getComboId()));
            
            // Solo cambiar estado si es un combo diferente al anterior
            if (comboAnterior == null || !comboAnterior.getId().equals(comboNuevo.getId())) {
                // Si había un combo anterior, restaurar su estado a 'aprobado'
                if (comboAnterior != null && comboAnterior.getEstado() == Combo.EstadoCombo.grabado) {
                    comboAnterior.setEstado(Combo.EstadoCombo.aprobado);
                    comboRepository.save(comboAnterior);
                }
                
                // Verificar si el combo ya está asignado a otro concursante
                @SuppressWarnings("unchecked")
                List<Concursante> concursantesConMismoCombo = entityManager.createQuery(
                    "SELECT c FROM Concursante c WHERE c.combo.id = :comboId AND c.id != :concursanteId"
                )
                .setParameter("comboId", comboNuevo.getId())
                .setParameter("concursanteId", id)
                .getResultList();
                
                // Si está asignado a otro concursante, lanzar error
                if (!concursantesConMismoCombo.isEmpty()) {
                    Concursante otroConcursante = concursantesConMismoCombo.get(0);
                    throw new RuntimeException("El combo " + comboNuevo.getId() + 
                        " ya está asignado al concursante " + otroConcursante.getNumeroConcursante() + 
                        " (" + otroConcursante.getNombre() + "). Debe desasignarlo primero antes de asignarlo a otro concursante.");
                }
                
                // Validar que el combo esté en estado válido para asignación
                // Permitir aprobado, adjudicado y grabado (grabado puede estar siendo reasignado)
                if (comboNuevo.getEstado() != Combo.EstadoCombo.aprobado && 
                    comboNuevo.getEstado() != Combo.EstadoCombo.adjudicado &&
                    comboNuevo.getEstado() != Combo.EstadoCombo.grabado) {
                    throw new RuntimeException("Solo se pueden asignar combos en estado 'aprobado', 'adjudicado' o 'grabado'. El combo " + 
                                             comboNuevo.getId() + " está en estado: " + comboNuevo.getEstado());
                }
                
                // Cambiar estado a 'grabado' cuando se asigna a un concursante (solo si no está ya en grabado)
                if (comboNuevo.getEstado() != Combo.EstadoCombo.grabado) {
                    comboNuevo.setEstado(Combo.EstadoCombo.grabado);
                    comboRepository.save(comboNuevo);
                }
            }
            
            concursante.setCombo(comboNuevo);
        } else {
            // Si se desasigna el combo, restaurar su estado a 'aprobado'
            if (comboAnterior != null && comboAnterior.getEstado() == Combo.EstadoCombo.grabado) {
                comboAnterior.setEstado(Combo.EstadoCombo.aprobado);
                comboRepository.save(comboAnterior);
            }
            concursante.setCombo(null);
        }
        
        concursante = concursanteRepository.save(concursante);
        return convertToDTO(concursante);
    }

    @Transactional
    public void delete(Long id) {
        // Verificar que el concursante existe
        Concursante concursante = concursanteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Concursante con ID " + id + " no encontrado"));
        
        // Verificar si está asignado a un programa
        if (concursante.getNumeroPrograma() != null) {
            throw new IllegalArgumentException("No se puede eliminar el concursante porque está asignado al programa " + 
                concursante.getNumeroPrograma() + ". Desasígnalo del programa primero.");
        }
        
        // Verificar estado del concursante
        if (concursante.getEstado() == "grabado") {
            throw new IllegalArgumentException("No se puede eliminar el concursante porque ya está grabado. " +
                "Los concursantes grabados no pueden ser eliminados.");
        }
        
        // Restaurar estado del cuestionario si estaba grabado
        if (concursante.getCuestionario() != null && 
            concursante.getCuestionario().getEstado() == Cuestionario.EstadoCuestionario.grabado) {
            concursante.getCuestionario().setEstado(Cuestionario.EstadoCuestionario.aprobado);
            cuestionarioRepository.save(concursante.getCuestionario());
        }
        
        // Restaurar estado del combo si estaba grabado
        if (concursante.getCombo() != null && 
            concursante.getCombo().getEstado() == Combo.EstadoCombo.grabado) {
            concursante.getCombo().setEstado(Combo.EstadoCombo.aprobado);
            comboRepository.save(concursante.getCombo());
        }
        
        concursanteRepository.deleteById(id);
    }

    public List<ConcursanteDTO> findByEstado(String estado) {
        return concursanteRepository.findByEstado(estado).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ConcursanteDTO> findByProgramaId(Long programaId) {
        Integer numeroPrograma = programaId != null ? programaId.intValue() : null;
        return concursanteRepository.findByNumeroProgramaOrderByNumeroConcursanteAsc(numeroPrograma).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ConcursanteDTO> findConcursantesSinPrograma() {
        return concursanteRepository.findByNumeroProgramaIsNull().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Page<ConcursanteDTO> findConcursantesSinProgramaPaginated(Pageable pageable) {
        return concursanteRepository.findByNumeroProgramaIsNull(pageable)
                .map(this::convertToDTO);
    }

    public Page<ConcursanteDTO> findConcursantesSinProgramaPaginated(Pageable pageable, String busqueda) {
        return concursanteRepository.findByNumeroProgramaIsNullWithSearch(pageable, busqueda)
                .map(this::convertToDTO);
    }

    @Transactional
    public ConcursanteDTO asignarAPrograma(Long concursanteId, Long programaId) {
        return asignarAPrograma(concursanteId, programaId, null);
    }

    @Transactional
    public ConcursanteDTO asignarAPrograma(Long concursanteId, Long programaId, Integer posicion) {
        Concursante concursante = concursanteRepository.findById(concursanteId)
                .orElseThrow(() -> new RuntimeException("Concursante no encontrado"));

        Integer numeroPrograma = programaId.intValue();
        Integer posicionAsignada = posicion;

        if (posicionAsignada != null) {
            if (posicionAsignada < 1 || posicionAsignada > 3) {
                throw new RuntimeException("La posición debe estar entre 1 y 3");
            }
            long ocupada = concursanteRepository.countByNumeroProgramaAndNumeroConcursante(numeroPrograma, posicionAsignada);
            if (ocupada > 0) {
                throw new RuntimeException("La posición " + posicionAsignada + " ya está ocupada en este programa");
            }
        } else {
            // Fallback: asignar primer hueco libre entre 1..3
            java.util.Set<Integer> usadas = concursanteRepository.findByNumeroProgramaOrderByNumeroConcursanteAsc(numeroPrograma)
                .stream()
                .map(Concursante::getNumeroConcursante)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
            for (int i = 1; i <= 3; i++) {
                if (!usadas.contains(i)) {
                    posicionAsignada = i;
                    break;
                }
            }
            if (posicionAsignada == null) {
                throw new RuntimeException("El programa ya tiene ocupadas las 3 posiciones de concursante");
            }
        }

        concursante.setNumeroPrograma(numeroPrograma);
        concursante.setNumeroConcursante(posicionAsignada);
        concursante = concursanteRepository.save(concursante);
        return convertToDTO(concursante);
    }

    @Transactional
    public ConcursanteDTO desasignarDePrograma(Long concursanteId) {
        Concursante concursante = concursanteRepository.findById(concursanteId)
                .orElseThrow(() -> new RuntimeException("Concursante no encontrado"));
        
        concursante.setNumeroPrograma(null);
        concursante = concursanteRepository.save(concursante);
        return convertToDTO(concursante);
    }

    @Transactional
    public ConcursanteDTO asignarAJornada(Long concursanteId, Long jornadaId) {
        Concursante concursante = concursanteRepository.findById(concursanteId)
                .orElseThrow(() -> new RuntimeException("Concursante no encontrado"));
        
        // Verificar que la jornada existe
        Jornada jornada = jornadaRepository.findById(jornadaId)
                .orElseThrow(() -> new RuntimeException("Jornada no encontrada"));
        
        // Validar que la jornada esté en un estado válido para asignación
        // Permitir jornadas en cualquier estado (preparacion, lista, en_grabacion, completada, archivada)
        // No hay restricción de estado para jornadas
        
        // Si ya tenía una jornada asignada, desasignar primero
        if (concursante.getJornada() != null) {
            desasignarDeJornada(concursanteId);
        }
        
        concursante.setJornada(jornada);
        concursante = concursanteRepository.save(concursante);
        return convertToDTO(concursante);
    }

    @Transactional
    public ConcursanteDTO desasignarDeJornada(Long concursanteId) {
        Concursante concursante = concursanteRepository.findById(concursanteId)
                .orElseThrow(() -> new RuntimeException("Concursante no encontrado"));
        
        concursante.setJornada(null);
        concursante = concursanteRepository.save(concursante);
        return convertToDTO(concursante);
    }

    public ConcursanteDTO updateCampo(Long id, Map<String, Object> campo) {
        Concursante concursante = concursanteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Concursante no encontrado con id: " + id));
        
        for (Map.Entry<String, Object> entry : campo.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            switch (key) {
                case "resultado":
                    if (value != null) {
                        try {
                            Integer resultadoInt = Integer.valueOf(value.toString());
                            concursante.setResultado(resultadoInt);
                        } catch (NumberFormatException e) {
                            throw new RuntimeException("El campo resultado debe ser un número entero");
                        }
                    } else {
                        concursante.setResultado(null);
                    }
                    break;
                case "estado":
                    if (value != null) {
                        concursante.setEstado(value.toString());
                    }
                    break;
                case "premio":
                    concursante.setPremio(value != null ? new BigDecimal(value.toString()) : null);
                    break;
                case "foto":
                    concursante.setFoto((String) value);
                    break;
                case "momentosDestacados":
                    concursante.setMomentosDestacados((String) value);
                    break;
                case "factorX":
                    concursante.setFactorX((String) value);
                    break;
                case "valoracionFinal":
                    concursante.setValoracionFinal((String) value);
                    break;
                case "creditosEspeciales":
                    concursante.setCreditosEspeciales((String) value);
                    break;
                case "xusoker":
                    concursante.setXusoker((String) value);
                    break;
            }
        }
        
        concursante = concursanteRepository.save(concursante);
        return convertToDTO(concursante);
    }

    private ConcursanteDTO convertToDTO(Concursante concursante) {
        ConcursanteDTO dto = new ConcursanteDTO();
        BeanUtils.copyProperties(concursante, dto);
        
        if (concursante.getCuestionario() != null) {
            dto.setCuestionarioId(concursante.getCuestionario().getId());
        }
        
        if (concursante.getCombo() != null) {
            dto.setComboId(concursante.getCombo().getId());
            
            // Verificar si el combo ha sido reciclado para esta jornada
            if (concursante.getJornada() != null && concursante.getJornada().getId() != null) {
                try {
                    Long count = entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM historial_jornadas WHERE jornada_id = :jid AND combo_id = :cid AND estado_asignacion = 'reaprovechado'")
                        .setParameter("jid", concursante.getJornada().getId())
                        .setParameter("cid", concursante.getCombo().getId())
                        .getSingleResult() instanceof Number ? ((Number) entityManager.createNativeQuery(
                            "SELECT COUNT(*) FROM historial_jornadas WHERE jornada_id = :jid AND combo_id = :cid AND estado_asignacion = 'reaprovechado'")
                            .setParameter("jid", concursante.getJornada().getId())
                            .setParameter("cid", concursante.getCombo().getId())
                            .getSingleResult()).longValue() : 0L;
                    dto.setComboReciclado(count != null && count > 0);
                } catch (Exception e) {
                    // Si hay error, asumir que no está reciclado
                    dto.setComboReciclado(false);
                }
            } else {
                dto.setComboReciclado(false);
            }
        } else {
            dto.setComboReciclado(false);
        }
        
        if (concursante.getJornada() != null) {
            dto.setJornadaId(concursante.getJornada().getId());
            dto.setJornadaNombre(concursante.getJornada().getNombre());
        }
        
        return dto;
    }

    private Concursante convertToEntity(ConcursanteDTO dto) {
        Concursante concursante = new Concursante();
        BeanUtils.copyProperties(dto, concursante, "id", "cuestionarioId", "comboId", "jornadaId", "jornadaNombre");
        
        if (dto.getCuestionarioId() != null) {
            Cuestionario cuestionario = cuestionarioRepository.findById(dto.getCuestionarioId())
                    .orElseThrow(() -> new RuntimeException("Cuestionario no encontrado: " + dto.getCuestionarioId()));
            concursante.setCuestionario(cuestionario);
        }

        if (dto.getComboId() != null) {
            Combo combo = comboRepository.findById(dto.getComboId())
                    .orElseThrow(() -> new RuntimeException("Combo no encontrado: " + dto.getComboId()));
            concursante.setCombo(combo);
        }
        
        if (dto.getJornadaId() != null) {
            Jornada jornada = jornadaRepository.findById(dto.getJornadaId())
                    .orElseThrow(() -> new RuntimeException("Jornada no encontrada: " + dto.getJornadaId()));
            concursante.setJornada(jornada);
        }
        
        return concursante;
    }

    @Transactional
    public String subirFoto(Long concursanteId, MultipartFile file) throws IOException {
        // Validar el archivo
        if (file.isEmpty()) {
            throw new RuntimeException("El archivo está vacío");
        }
        
        // Validar tipo de archivo
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("El archivo debe ser una imagen");
        }
        
        // Buscar el concursante
        Concursante concursante = concursanteRepository.findById(concursanteId)
                .orElseThrow(() -> new RuntimeException("Concursante no encontrado"));
        
        // Crear directorio si no existe
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generar nombre único para el archivo
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        
        // Guardar el archivo
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Actualizar solo el nombre del archivo en el concursante
        concursante.setFoto(fileName);
        concursanteRepository.save(concursante);
        
        return fileName;
    }

    private BigDecimal extraerNumerosDelTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal total = BigDecimal.ZERO;
        
        // Buscar patrones de números incluyendo decimales con punto o coma
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+(?:[.,]\\d+)?");
        java.util.regex.Matcher matcher = pattern.matcher(texto);
        
        while (matcher.find()) {
            try {
                String numeroStr = matcher.group().replace(',', '.');
                BigDecimal numero = new BigDecimal(numeroStr);
                total = total.add(numero);
            } catch (NumberFormatException e) {
                // Ignorar números mal formateados
                continue;
            }
        }
        
        return total;
    }
} 