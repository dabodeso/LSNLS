package com.lsnls.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ConcursanteEmitidoOpcionDTO {
    private Long id;
    private Integer numeroConcursante;
    private String nombre;
    private String lugar;
    private String xusoker;
    private String valoracionFinal;
    private String programaCodigo;
    private LocalDate fechaEmisionPrograma;
    private Integer ordenEscaleta;
}
