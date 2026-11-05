package com.lsnls.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProgramaEmitidoOpcionDTO {
    private Long id;
    private String codigo;
    private Integer temporada;
    private LocalDate fechaEmision;
    private int totalConcursantes;
    private String resumenConcursantes;
}
