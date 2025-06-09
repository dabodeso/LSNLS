package com.lsnls.dto;

import com.lsnls.entity.EstadoConcursante;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ConcursanteDTO {
    private Long id;
    private String nombre;
    private Integer edad;
    private String datosInteres;
    private Long cuestionarioId;
    private String fecha;
    private String lugar;
    private String guionista;
    private String resultado;
    private String notasGrabacion;
    private String editor;
    private String notasEdicion;
    private Integer duracion;
    private Integer numeroPrograma;
    private Integer ordenPrograma;
    private EstadoConcursante estado;
    private ProgramaResumen programa;
    private String imagen;

    @Data
    public static class ProgramaResumen {
        private Long id;
        private java.time.LocalDate fechaEmision;
    }
} 