package com.lsnls.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "jornadas")
public class Jornada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "fecha_jornada")
    private LocalDate fechaJornada;

    @Column(name = "lugar")
    private String lugar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoJornada estado = EstadoJornada.preparacion;

    @ManyToOne
    @JoinColumn(name = "creacion_usuario_id", nullable = false)
    @JsonIgnoreProperties({"preguntasCreadas", "preguntasVerificadas", "cuestionariosCreados"})
    private Usuario creacionUsuario;

    @Column(name = "fecha_creacion")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaCreacion;

    @Column(columnDefinition = "TEXT")
    private String notas;

    // Cuestionarios de la jornada (exactamente 5)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "jornadas_cuestionarios",
        joinColumns = @JoinColumn(name = "jornada_id"),
        inverseJoinColumns = @JoinColumn(name = "cuestionario_id")
    )
    @JsonIgnoreProperties({"preguntas", "creacionUsuario"})
    private Set<Cuestionario> cuestionarios;

    // Combos de la jornada (exactamente 5)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "jornadas_combos",
        joinColumns = @JoinColumn(name = "jornada_id"),
        inverseJoinColumns = @JoinColumn(name = "combo_id")
    )
    @JsonIgnoreProperties({"preguntas", "creacionUsuario"})
    private Set<Combo> combos;

    public enum EstadoJornada {
        preparacion, lista, en_grabacion, completada, archivada
    }

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }
} 