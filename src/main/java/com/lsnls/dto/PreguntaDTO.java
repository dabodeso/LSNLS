package com.lsnls.dto;

import com.lsnls.entity.Pregunta.NivelPregunta;
import com.lsnls.entity.Pregunta.FactorPregunta;
import lombok.Data;
import java.util.Set;

@Data
public class PreguntaDTO {
    private Long id;
    private String tematica;
    private String pregunta;
    private String respuesta;
    private String datosExtra;
    private String fuentes;
    private NivelPregunta nivel;
    private Long creacionUsuarioId;
    private String creacionUsuarioNombre;
    private String subtema;
    private String notas;
    private FactorPregunta factor;
    private String notasVerificacion;
    private String notasDireccion;
    private String verificacion;
    private String fechaCreacion;
    private String fechaVerificacion;
    private String estado;
} 