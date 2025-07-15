package com.lsnls.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ProgramaDTO {
    private Long id;
    private Integer temporada;
    private LocalTime duracionAcumulada;
    private BigDecimal resultadoAcumulado;
    private LocalDate fechaEmision;
    private BigDecimal datoAudienciaShare;
    private BigDecimal datoAudienciaTarget;
    private String estado;
    
    // Campos para la vista mejorada
    private BigDecimal totalPremios;
    private String gap;
    private Integer totalConcursantes;
    private String creditosEspeciales;
} 