package com.lsnls.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    /** Solo en la respuesta de alta, cuando hubo que recortar la escaleta. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String aviso;
}
