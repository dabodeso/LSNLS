package com.lsnls.service;

import com.lsnls.dto.ConcursanteDTO;
import com.lsnls.entity.Concursante;
import com.lsnls.entity.Cuestionario;
import com.lsnls.entity.Combo;
import com.lsnls.repository.ConcursanteRepository;
import com.lsnls.repository.CuestionarioRepository;
import com.lsnls.repository.ComboRepository;
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
        
        // Manejar cuestionario con lógica de estados
        if (concursanteDTO.getCuestionarioId() != null) {
            Cuestionario cuestionarioNuevo = cuestionarioRepository.findById(concursanteDTO.getCuestionarioId())
                    .orElseThrow(() -> new RuntimeException("Cuestionario no encontrado: " + concursanteDTO.getCuestionarioId()));
            
            // Solo cambiar estado si es un cuestionario diferente al anterior
            if (cuestionarioAnterior == null || !cuestionarioAnterior.getId().equals(cuestionarioNuevo.getId())) {
                // Validar que el cuestionario esté en estado válido para asignación
                if (cuestionarioNuevo.getEstado() != Cuestionario.EstadoCuestionario.creado && 
                    cuestionarioNuevo.getEstado() != Cuestionario.EstadoCuestionario.asignado_jornada) {
                    throw new RuntimeException("Solo se pueden asignar cuestionarios en estado 'creado' o 'asignado_jornada'. El cuestionario " + 
                                             cuestionarioNuevo.getId() + " está en estado: " + cuestionarioNuevo.getEstado());
                }
                
                // Cambiar estado del cuestionario nuevo a "asignado_concursantes"
                cuestionarioNuevo.setEstado(Cuestionario.EstadoCuestionario.asignado_concursantes);
                cuestionarioRepository.save(cuestionarioNuevo);
            }
            
            concursante.setCuestionario(cuestionarioNuevo);
        } else {
            concursante.setCuestionario(null);
        }
        
        // Si se quitó un cuestionario (había uno antes y ahora es null)
        if (cuestionarioAnterior != null && concursanteDTO.getCuestionarioId() == null) {
            // Liberar el cuestionario anterior - volver a estado "creado"
            if (cuestionarioAnterior.getEstado() == Cuestionario.EstadoCuestionario.asignado_concursantes) {
                cuestionarioAnterior.setEstado(Cuestionario.EstadoCuestionario.creado);
                cuestionarioRepository.save(cuestionarioAnterior);
            }
        }
        
        // Si se cambió de un cuestionario a otro (había uno antes y ahora hay otro diferente)
        if (cuestionarioAnterior != null && concursanteDTO.getCuestionarioId() != null && 
            !cuestionarioAnterior.getId().equals(concursanteDTO.getCuestionarioId())) {
            // Liberar el cuestionario anterior
            if (cuestionarioAnterior.getEstado() == Cuestionario.EstadoCuestionario.asignado_concursantes) {
                cuestionarioAnterior.setEstado(Cuestionario.EstadoCuestionario.creado);
                cuestionarioRepository.save(cuestionarioAnterior);
            }
        }
        
        // Manejar combo con lógica de estados
        if (concursanteDTO.getComboId() != null) {
            Combo comboNuevo = comboRepository.findById(concursanteDTO.getComboId())
                    .orElseThrow(() -> new RuntimeException("Combo no encontrado: " + concursanteDTO.getComboId()));
            
            // Solo cambiar estado si es un combo diferente al anterior
            if (comboAnterior == null || !comboAnterior.getId().equals(comboNuevo.getId())) {
                // Validar que el combo esté en estado válido para asignación
                if (comboNuevo.getEstado() != Combo.EstadoCombo.creado && 
                    comboNuevo.getEstado() != Combo.EstadoCombo.asignado_jornada) {
                    throw new RuntimeException("Solo se pueden asignar combos en estado 'creado' o 'asignado_jornada'. El combo " + 
                                             comboNuevo.getId() + " está en estado: " + comboNuevo.getEstado());
                }
                
                // Cambiar estado del combo nuevo a "asignado_concursantes"
                comboNuevo.setEstado(Combo.EstadoCombo.asignado_concursantes);
                comboRepository.save(comboNuevo);
            }
            
            concursante.setCombo(comboNuevo);
        } else {
            concursante.setCombo(null);
        }
        
        // Si se quitó un combo (había uno antes y ahora es null)
        if (comboAnterior != null && concursanteDTO.getComboId() == null) {
            // Liberar el combo anterior - volver a estado "creado"
            if (comboAnterior.getEstado() == Combo.EstadoCombo.asignado_concursantes) {
                comboAnterior.setEstado(Combo.EstadoCombo.creado);
                comboRepository.save(comboAnterior);
            }
        }
        
        // Si se cambió de un combo a otro (había uno antes y ahora hay otro diferente)
        if (comboAnterior != null && concursanteDTO.getComboId() != null && 
            !comboAnterior.getId().equals(concursanteDTO.getComboId())) {
            // Liberar el combo anterior
            if (comboAnterior.getEstado() == Combo.EstadoCombo.asignado_concursantes) {
                comboAnterior.setEstado(Combo.EstadoCombo.creado);
                comboRepository.save(comboAnterior);
            }
        }
        
        concursante = concursanteRepository.save(concursante);
        return convertToDTO(concursante);
    }

    @Transactional
    public void delete(Long id) {
        // Obtener el concursante antes de eliminarlo para liberar el cuestionario
        Concursante concursante = concursanteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Concursante no encontrado"));
        
        // Liberar el cuestionario si está asignado
        if (concursante.getCuestionario() != null) {
            Cuestionario cuestionario = concursante.getCuestionario();
            if (cuestionario.getEstado() == Cuestionario.EstadoCuestionario.asignado_concursantes) {
                cuestionario.setEstado(Cuestionario.EstadoCuestionario.creado);
                cuestionarioRepository.save(cuestionario);
            }
        }
        
        // Liberar el combo si está asignado
        if (concursante.getCombo() != null) {
            Combo combo = concursante.getCombo();
            if (combo.getEstado() == Combo.EstadoCombo.asignado_concursantes) {
                combo.setEstado(Combo.EstadoCombo.creado);
                comboRepository.save(combo);
            }
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

    public ConcursanteDTO updateCampo(Long id, Map<String, Object> campo) {
        Concursante concursante = concursanteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Concursante no encontrado con id: " + id));
        
        for (Map.Entry<String, Object> entry : campo.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            switch (key) {
                case "resultado":
                    concursante.setResultado((String) value);
                    // También actualizar el campo premio extrayendo el valor numérico
                    if (value != null) {
                        String resultadoStr = value.toString();
                        BigDecimal premioTotal = extraerNumerosDelTexto(resultadoStr);
                        concursante.setPremio(premioTotal);
                    } else {
                        concursante.setPremio(null);
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
        
        return dto;
    }

    private Concursante convertToEntity(ConcursanteDTO dto) {
        Concursante concursante = new Concursante();
        BeanUtils.copyProperties(dto, concursante, "id", "cuestionarioId", "comboId");
        
        if (dto.getCuestionarioId() != null) {
            Cuestionario cuestionario = cuestionarioRepository.findById(dto.getCuestionarioId())
                    .orElseThrow(() -> new RuntimeException("Cuestionario no encontrado: " + dto.getCuestionarioId()));
            
            // VALIDACIÓN ATÓMICA: Verificar y cambiar estado en una sola operación
            try {
                // Intentar cambio atómico desde 'creado' o 'asignado_jornada' a 'asignado_concursantes'
                boolean estadoCambiado = false;
                if (cuestionario.getEstado() == Cuestionario.EstadoCuestionario.creado) {
                    estadoCambiado = cuestionarioService.cambiarEstadoAtomico(
                        cuestionario.getId(), 
                        Cuestionario.EstadoCuestionario.creado, 
                        Cuestionario.EstadoCuestionario.asignado_concursantes
                    );
                } else if (cuestionario.getEstado() == Cuestionario.EstadoCuestionario.asignado_jornada) {
                    estadoCambiado = cuestionarioService.cambiarEstadoAtomico(
                        cuestionario.getId(), 
                        Cuestionario.EstadoCuestionario.asignado_jornada, 
                        Cuestionario.EstadoCuestionario.asignado_concursantes
                    );
                } else {
                    throw new RuntimeException("Solo se pueden asignar cuestionarios en estado 'creado' o 'asignado_jornada'. El cuestionario " + 
                                             cuestionario.getId() + " está en estado: " + cuestionario.getEstado());
                }
                
                if (!estadoCambiado) {
                    throw new RuntimeException("El cuestionario fue modificado por otro usuario. Por favor, recarga e intenta nuevamente.");
                }
                
            } catch (IllegalStateException e) {
                throw new RuntimeException("Conflicto de concurrencia: " + e.getMessage());
            }
            
            concursante.setCuestionario(cuestionario);
        }

        // OPERACIÓN ATÓMICA SIMILAR PARA COMBOS
        if (dto.getComboId() != null) {
            Combo combo = comboRepository.findById(dto.getComboId())
                    .orElseThrow(() -> new RuntimeException("Combo no encontrado: " + dto.getComboId()));
            
            // VALIDACIÓN ATÓMICA: Verificar y cambiar estado en una sola operación
            try {
                boolean estadoCambiado = false;
                if (combo.getEstado() == Combo.EstadoCombo.creado) {
                    estadoCambiado = comboService.cambiarEstadoAtomico(
                        combo.getId(), 
                        Combo.EstadoCombo.creado, 
                        Combo.EstadoCombo.asignado_concursantes
                    );
                } else if (combo.getEstado() == Combo.EstadoCombo.asignado_jornada) {
                    estadoCambiado = comboService.cambiarEstadoAtomico(
                        combo.getId(), 
                        Combo.EstadoCombo.asignado_jornada, 
                        Combo.EstadoCombo.asignado_concursantes
                    );
                } else {
                    throw new RuntimeException("Solo se pueden asignar combos en estado 'creado' o 'asignado_jornada'. El combo " + 
                                             combo.getId() + " está en estado: " + combo.getEstado());
                }
                
                if (!estadoCambiado) {
                    throw new RuntimeException("El combo fue modificado por otro usuario. Por favor, recarga e intenta nuevamente.");
                }
                
            } catch (IllegalStateException e) {
                throw new RuntimeException("Conflicto de concurrencia: " + e.getMessage());
            }
            
            concursante.setCombo(combo);
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
        
        // Actualizar la URL de la foto en el concursante
        String fotoUrl = "/uploads/" + fileName;
        concursante.setFoto(fotoUrl);
        concursanteRepository.save(concursante);
        
        return fotoUrl;
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