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
        concursante = concursanteRepository.save(concursante);
        return convertToDTO(concursante);
    }

    @Transactional
    public ConcursanteDTO update(Long id, ConcursanteDTO concursanteDTO) {
        Concursante concursante = concursanteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Concursante no encontrado"));

        BeanUtils.copyProperties(concursanteDTO, concursante, "id");
        
        // Manejar cuestionario
        if (concursanteDTO.getCuestionarioId() != null) {
            Cuestionario cuestionario = cuestionarioRepository.findById(concursanteDTO.getCuestionarioId())
                    .orElse(null);
            concursante.setCuestionario(cuestionario);
        } else {
            concursante.setCuestionario(null);
        }
        
        // Manejar combo
        if (concursanteDTO.getComboId() != null) {
            Combo combo = comboRepository.findById(concursanteDTO.getComboId())
                    .orElse(null);
            concursante.setCombo(combo);
        } else {
            concursante.setCombo(null);
        }
        
        concursante = concursanteRepository.save(concursante);
        return convertToDTO(concursante);
    }

    public void delete(Long id) {
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
                    .orElse(null);
            concursante.setCuestionario(cuestionario);
        }
        
        if (dto.getComboId() != null) {
            Combo combo = comboRepository.findById(dto.getComboId())
                    .orElse(null);
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