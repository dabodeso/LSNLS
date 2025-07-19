package com.lsnls.entity;

import jakarta.persistence.*;
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

    private String jornada;

    @Column(name = "dia_grabacion")
    private LocalDate diaGrabacion;

    private String lugar;
    private String nombre;
    private Integer edad;
    private String ocupacion;

    @Column(name = "redes_sociales")
    private String redesSociales;

    @ManyToOne
    @JoinColumn(name = "cuestionario_id")
    @JsonIgnoreProperties({"preguntas", "creacionUsuario"})
    private Cuestionario cuestionario;

    @ManyToOne
    @JoinColumn(name = "combo_id")
    @JsonIgnoreProperties({"preguntas", "creacionUsuario"})
    private Combo combo;

    @Column(name = "factor_x")
    private String factorX;

    private String resultado;

    @Column(name = "notas_grabacion", columnDefinition = "TEXT")
    private String notasGrabacion;

    private String guionista;

    @Column(name = "valoracion_guionista", columnDefinition = "TEXT")
    private String valoracionGuionista;

    @Column(name = "concursantes_por_jornada")
    private Integer concursantesPorJornada;

    private String estado;

    @Column(name = "momentos_destacados", columnDefinition = "TEXT")
    private String momentosDestacados;

    private String duracion; // formato MM:SS

    @Column(name = "valoracion_final", columnDefinition = "TEXT")
    private String valoracionFinal;

    @Column(name = "numero_programa")
    private Integer numeroPrograma;

    @Column(name = "orden_escaleta")
    private Integer ordenEscaleta;

    // Nuevos campos para la vista de programas
    @Column(name = "premio", precision = 10, scale = 2)
    private BigDecimal premio; // Premio obtenido por el concursante

    @Column(name = "foto")
    private String foto; // URL o path de la foto del concursante

    @Column(name = "creditos_especiales", columnDefinition = "TEXT")
    private String creditosEspeciales; // Créditos especiales del concursante

    // Método para actualizar el estado automáticamente
    public void actualizarEstado() {
        if (numeroPrograma != null && ordenEscaleta != null) {
            this.estado = "PROGRAMADO";
        } else if (duracion != null && !duracion.isEmpty()) {
            this.estado = "EDITADO";
        } else if (resultado != null && !resultado.isEmpty()) {
            this.estado = "GRABADO";
        } else {
            this.estado = "BORRADOR";
        }
    }
} 