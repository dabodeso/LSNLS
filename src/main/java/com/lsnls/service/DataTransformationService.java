package com.lsnls.service;

import org.springframework.stereotype.Service;

@Service
public class DataTransformationService {

    /**
     * Transforma texto a mayúsculas y limpia caracteres no permitidos
     */
    public String normalizarTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return texto;
        }
        
        // Convertir a mayúsculas
        String normalizado = texto.toUpperCase().trim();
        
        // Remover saltos de línea
        normalizado = normalizado.replaceAll("[\r\n]+", " ");
        
        // Limpiar múltiples espacios
        normalizado = normalizado.replaceAll("\\s+", " ");
        
        return normalizado;
    }

    /**
     * Valida que el texto cumple con los requisitos de LSNOLS
     */
    public boolean esTextoValido(String texto, int maxLength) {
        if (texto == null || texto.trim().isEmpty()) {
            return false;
        }
        
        // Verificar longitud
        if (texto.length() > maxLength) {
            return false;
        }
        
        // Verificar que esté en mayúsculas
        if (!texto.equals(texto.toUpperCase())) {
            return false;
        }
        
        // Verificar que no tenga saltos de línea
        if (texto.contains("\n") || texto.contains("\r")) {
            return false;
        }
        
        // Verificar caracteres permitidos (letras, números, espacios, signos básicos)
        String patronPermitido = "^[A-Za-zÀ-ÿÑñ0-9\\s.,;:!?¡¿()\\[\\]\"'\\-]+$";
        return texto.matches(patronPermitido);
    }

    /**
     * Normaliza específicamente las preguntas (150 caracteres máximo)
     */
    public String normalizarPregunta(String pregunta) {
        String normalizada = normalizarTexto(pregunta);
        if (normalizada != null && normalizada.length() > 150) {
            normalizada = normalizada.substring(0, 150).trim();
        }
        return normalizada;
    }

    /**
     * Normaliza específicamente las respuestas (50 caracteres máximo)
     */
    public String normalizarRespuesta(String respuesta) {
        String normalizada = normalizarTexto(respuesta);
        if (normalizada != null && normalizada.length() > 50) {
            normalizada = normalizada.substring(0, 50).trim();
        }
        return normalizada;
    }

    /**
     * Normaliza específicamente las temáticas (100 caracteres máximo)
     */
    public String normalizarTematica(String tematica) {
        String normalizada = normalizarTexto(tematica);
        if (normalizada != null && normalizada.length() > 100) {
            normalizada = normalizada.substring(0, 100).trim();
        }
        return normalizada;
    }

    /**
     * Valida específicamente una pregunta completa
     */
    public ValidationResult validarPreguntaCompleta(String pregunta, String respuesta, String tematica) {
        ValidationResult result = new ValidationResult();
        
        // Validar pregunta
        if (!esTextoValido(pregunta, 150)) {
            result.addError("pregunta", "La pregunta no cumple con el formato requerido (máximo 150 caracteres, solo mayúsculas, sin saltos de línea)");
        }
        
        // Validar respuesta
        if (!esTextoValido(respuesta, 50)) {
            result.addError("respuesta", "La respuesta no cumple con el formato requerido (máximo 50 caracteres, solo mayúsculas, sin saltos de línea)");
        }
        
        // Validar temática
        if (!esTextoValido(tematica, 100)) {
            result.addError("tematica", "La temática no cumple con el formato requerido (máximo 100 caracteres, solo mayúsculas, sin saltos de línea)");
        }
        
        return result;
    }

    /**
     * Clase para manejar resultados de validación
     */
    public static class ValidationResult {
        private boolean valid = true;
        private java.util.Map<String, String> errors = new java.util.HashMap<>();
        
        public void addError(String field, String message) {
            valid = false;
            errors.put(field, message);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public java.util.Map<String, String> getErrors() {
            return errors;
        }
        
        public String getErrorsAsString() {
            return String.join("; ", errors.values());
        }
    }
} 