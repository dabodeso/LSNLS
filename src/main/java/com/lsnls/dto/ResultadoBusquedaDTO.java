package com.lsnls.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
public class ResultadoBusquedaDTO<T> {
    private List<T> resultados;
    private boolean hayMas;

    /** Total de coincidencias, no solo las de esta página. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long total;

    /** Página 0-based. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer page;

    /** Tamaño de página pedido. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer size;
}
