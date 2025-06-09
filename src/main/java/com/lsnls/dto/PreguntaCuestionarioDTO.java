package com.lsnls.dto;

import lombok.Data;

@Data
public class PreguntaCuestionarioDTO {
    private String slot; // Ej: "PM1", "1LS", etc
    private String slotSolicitado; // slot solicitado explícitamente (opcional)
    private PreguntaDTO pregunta;
    private Integer factorMultiplicacion;
} 