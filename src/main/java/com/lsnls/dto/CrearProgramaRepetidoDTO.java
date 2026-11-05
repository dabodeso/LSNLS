package com.lsnls.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CrearProgramaRepetidoDTO {
    private String tipo;
    private String codigo;
    private Integer temporada;
    private LocalDate fechaEmision;
    /** Opcional. Si viene vacío, el alta queda en editado. */
    private String estado;
    /** Programa ya emitido del que se copian los concursantes. */
    private Long programaOriginalId;
    /** Concursantes sueltos, en el orden de la escaleta (máximo 3). */
    private List<Long> concursanteIds;
}
