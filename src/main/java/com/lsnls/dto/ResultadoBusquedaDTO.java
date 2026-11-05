package com.lsnls.dto;

import lombok.Data;

import java.util.List;

@Data
public class ResultadoBusquedaDTO<T> {
    private List<T> resultados;
    private boolean hayMas;
}
