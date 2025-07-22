package com.lsnls.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lsnls.validation.NoLineBreaks;
import com.lsnls.validation.NoSpecialCharacters;
import com.lsnls.validation.UpperCase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "preguntas")
public class Pregunta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne
    @JoinColumn(name = "creacion_usuario_id")
    @JsonIgnoreProperties({"preguntasCreadas", "preguntasVerificadas", "cuestionariosCreados"})
    private Usuario creacionUsuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelPregunta nivel;

    @NotBlank(message = "La temática es obligatoria")
    @Size(max = 100, message = "La temática no puede exceder 100 caracteres")
    @NoLineBreaks(message = "La temática no puede contener saltos de línea")
    @NoSpecialCharacters(message = "La temática contiene caracteres no permitidos")
    @Column(nullable = false, length = 100)
    private String tematica;

    @NotBlank(message = "La pregunta es obligatoria")
    @Size(max = 150, message = "La pregunta no puede exceder 150 caracteres")
    @NoLineBreaks(message = "La pregunta no puede contener saltos de línea")
    @NoSpecialCharacters(message = "La pregunta contiene caracteres no permitidos")
    @Column(nullable = false)
    private String pregunta;

    @NotBlank(message = "La respuesta es obligatoria")
    @Size(max = 50, message = "La respuesta no puede exceder 50 caracteres")
    @NoLineBreaks(message = "La respuesta no puede contener saltos de línea")
    @NoSpecialCharacters(message = "La respuesta contiene caracteres no permitidos")
    @Column(nullable = false, length = 50)
    private String respuesta;

    @Column(name = "datos_extra")
    private String datosExtra;

    private String fuentes;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Column(name = "notas_verificacion", columnDefinition = "TEXT")
    private String notasVerificacion;

    @Column(name = "notas_direccion", columnDefinition = "TEXT")
    private String notasDireccion;

    @Column(name = "verificacion", length = 500)
    private String verificacion;

    @ManyToOne
    @JoinColumn(name = "verificacion_usuario_id")
    @JsonIgnoreProperties({"preguntasCreadas", "preguntasVerificadas", "cuestionariosCreados"})
    private Usuario verificacionUsuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPregunta estado = EstadoPregunta.borrador;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_disponibilidad")
    private EstadoDisponibilidad estadoDisponibilidad = EstadoDisponibilidad.disponible;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_verificacion")
    private LocalDateTime fechaVerificacion;

    @Column(name = "subtema", length = 100)
    private String subtema;

    @JsonIgnore
    @OneToMany(mappedBy = "pregunta")
    private Set<PreguntaCuestionario> cuestionarios;

    @Enumerated(EnumType.STRING)
    @Column(length = 2)
    private FactorPregunta factor;

    public enum NivelPregunta {
        _0, _1LS, _2NLS, _3LS, _4NLS, _5LS, _5NLS
    }

    public enum EstadoPregunta {
        borrador, para_verificar, verificada, revisar, corregir, rechazada, aprobada
    }

    public enum EstadoDisponibilidad {
        disponible, usada, liberada, descartada
    }

    public enum FactorPregunta {
        X, X2, X3
    }

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }
} 