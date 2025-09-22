package com.lsnls.service;

import org.springframework.stereotype.Service;

@Service
public class DataTransformationService {

    /**
     * Transforma texto y limpia caracteres no permitidos PERO PRESERVA MAYÚSCULAS/MINÚSCULAS
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
        
        // YA NO verificar que esté en mayúsculas - permitir mayúsculas y minúsculas
        
        // Verificar que no tenga saltos de línea
        if (texto.contains("\n") || texto.contains("\r")) {
            return false;
        }
        
        // Verificar caracteres permitidos (letras, números, espacios, signos básicos)
        String patronPermitido = "^[A-Za-zÀ-ÿÑñ0-9\\s.,;:!?¡¿()\\[\\]\"'\\-]+$";
        return texto.matches(patronPermitido);
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
        
        // Validar pregunta - sin límite de caracteres
        if (pregunta == null || pregunta.trim().isEmpty() || pregunta.contains("\n") || pregunta.contains("\r")) {
            result.addError("pregunta", "La pregunta no puede estar vacía ni contener saltos de línea");
        } else {
            // Verificar caracteres permitidos (letras, números, espacios, signos básicos)
            String patronPermitido = "^[A-Za-zÀ-ÿÑñ0-9\\s.,;:!?¡¿()\\[\\]\"'\\-]+$";
            if (!pregunta.matches(patronPermitido)) {
                result.addError("pregunta", "La pregunta contiene caracteres no permitidos");
            }
        }
        
        // Validar respuesta - aumentado a 500 caracteres
        if (respuesta == null || respuesta.trim().isEmpty() || respuesta.contains("\n") || respuesta.contains("\r")) {
            result.addError("respuesta", "La respuesta no puede estar vacía ni contener saltos de línea");
        } else if (respuesta.length() > 500) {
            result.addError("respuesta", "La respuesta no puede exceder los 500 caracteres");
        } else {
            // Verificar caracteres permitidos (letras, números, espacios, signos básicos)
            String patronPermitido = "^[A-Za-zÀ-ÿÑñ0-9\\s.,;:!?¡¿()\\[\\]\"'\\-]+$";
            if (!respuesta.matches(patronPermitido)) {
                result.addError("respuesta", "La respuesta contiene caracteres no permitidos");
            }
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