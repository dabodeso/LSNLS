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
            String duracionFinalMin, String duracionFinalMax, String valoracionFinal, String bonico) {
        return concursanteRepository.findAllWithFilters(pageable, estado, jornada, 
                lugar, numeroPrograma, duracionFinalMin, duracionFinalMax, valoracionFinal, bonico)
                .map(this::convertToDTO);
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
        
        // Si se asignó un cuestionario, cambiar su estado a grabado
        if (concursante.getCuestionario() != null) {
            Cuestionario cuestionario = concursante.getCuestionario();
            if (cuestionario.getEstado() == Cuestionario.EstadoCuestionario.adjudicado) {
                cuestionario.setEstado(Cuestionario.EstadoCuestionario.grabado);
                cuestionarioRepository.save(cuestionario);
            }
        }
        
        // Si se asignó un combo, cambiar su estado a grabado
        if (concursante.getCombo() != null) {
            Combo combo = concursante.getCombo();
            if (combo.getEstado() == Combo.EstadoCombo.adjudicado) {
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
                // Si había un cuestionario anterior, restaurar su estado a 'creado'
                if (cuestionarioAnterior != null && cuestionarioAnterior.getEstado() == Cuestionario.EstadoCuestionario.grabado) {
                    cuestionarioAnterior.setEstado(Cuestionario.EstadoCuestionario.aprobado);
                    cuestionarioRepository.save(cuestionarioAnterior);
                }
                
                // Validar que el cuestionario esté en estado válido para asignación
                if (cuestionarioNuevo.getEstado() != Cuestionario.EstadoCuestionario.aprobado && 
                    cuestionarioNuevo.getEstado() != Cuestionario.EstadoCuestionario.adjudicado) {
                    throw new RuntimeException("Solo se pueden asignar cuestionarios en estado 'aprobado' o 'adjudicado'. El cuestionario " + 
                                             cuestionarioNuevo.getId() + " está en estado: " + cuestionarioNuevo.getEstado());
                }
                
                // Cambiar estado a 'grabado' cuando se asigna a un concursante
                cuestionarioNuevo.setEstado(Cuestionario.EstadoCuestionario.grabado);
                cuestionarioRepository.save(cuestionarioNuevo);
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
                // Si había un combo anterior, restaurar su estado a 'creado'
                if (comboAnterior != null && comboAnterior.getEstado() == Combo.EstadoCombo.grabado) {
                    comboAnterior.setEstado(Combo.EstadoCombo.aprobado);
                    comboRepository.save(comboAnterior);
                }
                
                // Validar que el combo esté en estado válido para asignación
                if (comboNuevo.getEstado() != Combo.EstadoCombo.aprobado && 
                    comboNuevo.getEstado() != Combo.EstadoCombo.adjudicado) {
                    throw new RuntimeException("Solo se pueden asignar combos en estado 'aprobado' o 'adjudicado'. El combo " + 
                                             comboNuevo.getId() + " está en estado: " + comboNuevo.getEstado());
                }
                
                // Cambiar estado a 'grabado' cuando se asigna a un concursante
                comboNuevo.setEstado(Combo.EstadoCombo.grabado);
                comboRepository.save(comboNuevo);
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
        return concursanteRepository.findByNumeroPrograma(numeroPrograma).stream()
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
        Concursante concursante = concursanteRepository.findById(concursanteId)
                .orElseThrow(() -> new RuntimeException("Concursante no encontrado"));
        
        concursante.setNumeroPrograma(programaId.intValue());
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