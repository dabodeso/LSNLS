package com.lsnls.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Data;

@Entity
@Data
@Table(name = "concursantes")
public class Concursante {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private Integer edad;

    @Column(name = "datos_interes", columnDefinition = "TEXT")
    private String datosInteres;

    @ManyToOne(optional = true)
    @JoinColumn(name = "cuestionario_id")
    private Cuestionario cuestionario;

    private LocalDate fecha;
    private String lugar;
    private String guionista;
    private String resultado;

    @Column(name = "notas_grabacion", columnDefinition = "TEXT")
    private String notasGrabacion;

    private String editor;

    @Column(name = "notas_edicion", columnDefinition = "TEXT")
    private String notasEdicion;

    private Integer duracion; // en minutos

    @ManyToOne
    @JoinColumn(name = "programa_id")
    private Programa programa;

    @Column(name = "orden_programa")
    private Integer ordenPrograma;

    @Enumerated(EnumType.STRING)
    private EstadoConcursante estado = EstadoConcursante.BORRADOR;

    @Column(name = "imagen", columnDefinition = "MEDIUMTEXT")
    private String imagen;

    // Método para actualizar el estado automáticamente
    public void actualizarEstado() {
        if (programa != null && ordenPrograma != null) {
            this.estado = EstadoConcursante.PROGRAMADO;
        } else if (duracion != null) {
            this.estado = EstadoConcursante.EDITADO;
        } else if (resultado != null && !resultado.isEmpty()) {
            this.estado = EstadoConcursante.GRABADO;
        } else {
            this.estado = EstadoConcursante.BORRADOR;
        }
    }
} 