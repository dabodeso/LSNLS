package com.lsnls.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonFormat;
import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "historial_jornadas")
public class HistorialJornada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne
    @JoinColumn(name = "jornada_id", nullable = false)
    @JsonIgnoreProperties({"cuestionarios", "combos", "creacionUsuario"})
    private Jornada jornada;

    @ManyToOne
    @JoinColumn(name = "cuestionario_id")
    @JsonIgnoreProperties({"preguntas", "creacionUsuario"})
    private Cuestionario cuestionario;

    @ManyToOne
    @JoinColumn(name = "combo_id")
    @JsonIgnoreProperties({"preguntas", "creacionUsuario"})
    private Combo combo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAsignacion tipoAsignacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoAsignacion estadoAsignacion = EstadoAsignacion.asignado;

    @Column(name = "fecha_asignacion")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaAsignacion;

    @Column(name = "fecha_uso")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaUso;

    @Column(name = "pregunta_usada_id")
    private Long preguntaUsadaId; // Solo para combos, indica qué pregunta se usó

    @Column(name = "notas")
    private String notas;

    public enum TipoAsignacion {
        CUESTIONARIO, COMBO
    }

    public enum EstadoAsignacion {
        asignado, usado, no_usado, reaprovechado
    }

    @PrePersist
    protected void onCreate() {
        fechaAsignacion = LocalDateTime.now();
    }
}
