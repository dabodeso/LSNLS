package com.lsnls.entity;

import javax.persistence.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import java.time.LocalDate;
import java.math.BigDecimal;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Data
@Table(name = "concursantes")
public class Concursante {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "numero_concursante")
    private Integer numeroConcursante;

    // Relación real con Jornada (puede estar ausente en datos legacy)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "jornada_id")
    @NotFound(action = NotFoundAction.IGNORE)
    @JsonIgnoreProperties({"cuestionarios", "combos", "creacionUsuario"})
    private Jornada jornada;

    @Column(name = "dia_grabacion")
    private LocalDate diaGrabacion;

    private String lugar;
    private String nombre;
    private Integer edad;
    private String ocupacion;

    @Column(name = "redes_sociales")
    private String redesSociales;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "cuestionario_id")
    @NotFound(action = NotFoundAction.IGNORE)
    @JsonIgnoreProperties({"preguntas", "creacionUsuario"})
    private Cuestionario cuestionario;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "combo_id")
    @NotFound(action = NotFoundAction.IGNORE)
    @JsonIgnoreProperties({"preguntas", "creacionUsuario"})
    private Combo combo;

    private String xusoker;

    @Column(name = "factor_x")
    private String factorX;

    private Integer resultado;

    @Column(name = "notas_grabacion", columnDefinition = "TEXT")
    private String notasGrabacion;

    private String guionista;

    @Column(name = "valoracion_guionista", columnDefinition = "TEXT")
    private String valoracionGuionista;

    private String estado;

    @Column(name = "momentos_destacados", columnDefinition = "TEXT")
    private String momentosDestacados;

    private String duracion; // formato MM:SS

    @Column(name = "duracion_direccion")
    private String duracionDireccion; // formato MM:SS

    @Column(name = "duracion_final")
    private String duracionFinal; // formato MM:SS

    @Column(name = "valoracion_final", columnDefinition = "TEXT")
    private String valoracionFinal;

    @Column(name = "numero_programa")
    private Integer numeroPrograma;

    @Column(name = "orden_escaleta")
    private Integer ordenEscaleta;

    private String bonico;

    @Column(name = "premio")
    private BigDecimal premio;

    @Column(name = "foto")
    private String foto;

    @Column(name = "creditos_especiales", columnDefinition = "TEXT")
    private String creditosEspeciales;
} 