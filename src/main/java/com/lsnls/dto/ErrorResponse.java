package com.lsnls.dto;

import lombok.Data;

@Data
public class ErrorResponse {
    private String error;
    private String mensaje;

    public ErrorResponse(String error, String mensaje) {
        this.error = error;
        this.mensaje = mensaje;
    }
} 