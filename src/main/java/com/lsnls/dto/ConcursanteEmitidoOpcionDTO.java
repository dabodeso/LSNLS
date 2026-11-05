package com.lsnls.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ConcursanteEmitidoOpcionDTO {
    private Long id;
    private Integer numeroConcursante;
    private String nombre;
    private String lugar;
    private String valoracionFinal;
    private String duracion;
    private String momentosDestacados;
    private String foto;
    private String programaCodigo;
    private LocalDate fechaEmisionPrograma;
    private Integer ordenEscaleta;
}
