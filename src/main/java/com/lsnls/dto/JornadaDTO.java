package com.lsnls.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class JornadaDTO {
    private Long id;
    private String nombre;
    private LocalDate fechaJornada;
    private String lugar;
    private String estado;
    private Long creacionUsuarioId;
    private String creacionUsuarioNombre;
    private LocalDateTime fechaCreacion;
    private String notas;
    private List<Long> cuestionarioIds;
    private List<Long> comboIds;
    
    // Para mostrar información detallada
    private List<CuestionarioResumenDTO> cuestionarios;
    private List<ComboResumenDTO> combos;
    
    @Data
    public static class CuestionarioResumenDTO {
        private Long id;
        private String nivel;
        private String estado;
        private String tematica;
        private String notasDireccion;
        private Integer totalPreguntas;
    }
    
    @Data
    public static class ComboResumenDTO {
        private Long id;
        private String nivel;
        private String estado;
        private String tipo;
        private Integer totalPreguntas;
    }
} 