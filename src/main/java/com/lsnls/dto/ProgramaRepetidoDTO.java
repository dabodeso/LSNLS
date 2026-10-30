package com.lsnls.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProgramaRepetidoDTO extends ProgramaDTO {
    private String tipo;
    private String programaOriginalCodigo;
    private LocalDate fechaOriginal;
    private boolean repetido = true;
}
