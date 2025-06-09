package com.lsnls.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String nombre;
    
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
} 