package com.lsnls.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean exito;
    private String mensaje;
    private T datos;

    public static <T> ApiResponse<T> exitoso(T datos) {
        return new ApiResponse<>(true, "Operación exitosa", datos);
    }

    public static <T> ApiResponse<T> exitoso(String mensaje, T datos) {
        return new ApiResponse<>(true, mensaje, datos);
    }

    public static <T> ApiResponse<T> error(String mensaje) {
        return new ApiResponse<>(false, mensaje, null);
    }
} 