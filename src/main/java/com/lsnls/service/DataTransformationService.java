package com.lsnls.service;

import org.springframework.stereotype.Service;

@Service
public class DataTransformationService {

    /**
     * Transforma texto y limpia espacios/saltos PERO PRESERVA MAYÚSCULAS/MINÚSCULAS
     * y el resto de caracteres (p. ej. +, /, %, #), que ya vienen de la BBDD.
     */
    public String normalizarTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return texto;
        }
        
        // YA NO convertir a mayúsculas automáticamente
        String normalizado = texto.trim();
        
        // Remover saltos de línea
        normalizado = normalizado.replaceAll("[\r\n]+", " ");
        
        // Limpiar múltiples espacios
        normalizado = normalizado.replaceAll("\\s+", " ");
        
        return normalizado;
    }

    /**
     * Texto usable en pregunta/respuesta/temática: no vacío, sin saltos ni controles.
     * Se permiten símbolos (+, /, %, #, @, etc.); la importación ya los trae.
     */
    public boolean tieneCaracteresUsables(String texto) {
        if (texto == null || texto.isEmpty()) {
            return false;
        }
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '\n' || c == '\r') {
                return false;
            }
            if (Character.isISOControl(c) && !Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Valida que el texto cumple con los requisitos de LSNOLS (SIN EXIGIR MAYÚSCULAS)
     */
    public boolean esTextoValido(String texto, int maxLength) {
        if (texto == null || texto.trim().isEmpty()) {
            return false;
        }
        
        // Verificar longitud
        if (texto.length() > maxLength) {
            return false;
        }
        
        return tieneCaracteresUsables(texto);
    }

    /**
     * Normaliza específicamente las preguntas (sin límite de caracteres)
     */
    public String normalizarPregunta(String pregunta) {
        String normalizada = normalizarTexto(pregunta);
        // Ya no se trunca a 150 caracteres
        return normalizada;
    }

    /**
     * Normaliza específicamente las respuestas (500 caracteres máximo)
     */
    public String normalizarRespuesta(String respuesta) {
        String normalizada = normalizarTexto(respuesta);
        if (normalizada != null && normalizada.length() > 500) {
            normalizada = normalizada.substring(0, 500).trim();
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
        
        // Validar pregunta - sin límite de caracteres; se permiten símbolos (+, /, %, etc.)
        if (pregunta == null || pregunta.trim().isEmpty() || !tieneCaracteresUsables(pregunta)) {
            result.addError("pregunta", "La pregunta no puede estar vacía ni contener saltos de línea");
        }
        
        // Validar respuesta - aumentado a 500 caracteres
        if (respuesta == null || respuesta.trim().isEmpty() || !tieneCaracteresUsables(respuesta)) {
            result.addError("respuesta", "La respuesta no puede estar vacía ni contener saltos de línea");
        } else if (respuesta.length() > 500) {
            result.addError("respuesta", "La respuesta no puede exceder los 500 caracteres");
        }
        
        // Validar temática
        if (!esTextoValido(tematica, 100)) {
            result.addError("tematica", "La temática no cumple con el formato requerido (máximo 100 caracteres, sin saltos de línea)");
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