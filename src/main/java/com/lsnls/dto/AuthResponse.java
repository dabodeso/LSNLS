package com.lsnls.dto;

import com.lsnls.entity.Usuario;
import lombok.Data;

@Data
public class AuthResponse {
    private String token;
    private String tipo = "Bearer";
    private Long id;
    private String nombre;
    private Usuario.RolUsuario rol;
    
    public AuthResponse(String token, Usuario usuario) {
        this.token = token;
        this.id = usuario.getId();
        this.nombre = usuario.getNombre();
        this.rol = usuario.getRol();
    }
} 