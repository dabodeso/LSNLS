package com.lsnls.dto;

import lombok.Data;
import java.time.LocalDate;
import java.math.BigDecimal;

@Data
public class ConcursanteDTO {
    private Long id;
    private Integer numeroConcursante;
    private String jornada;
    private LocalDate diaGrabacion;
    private String lugar;
    private String nombre;
    private Integer edad;
    private String ocupacion;
    private String redesSociales;
    private Long cuestionarioId;
    private Long comboId;
    private String factorX;
    private String resultado;
    private String notasGrabacion;
    private String guionista;
    private String valoracionGuionista;
    private Integer concursantesPorJornada;
    private String estado;
    private String momentosDestacados;
    private String duracion; // formato MM:SS
    private String valoracionFinal;
    private Integer numeroPrograma;
    private Integer ordenEscaleta;
    
    // Nuevos campos para la vista de programas
    private BigDecimal premio;
    private String foto;
    private String creditosEspeciales;
} 