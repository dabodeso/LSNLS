package com.lsnls.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "programas")
public class Programa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "temporada", nullable = false)
    private Integer temporada;

    @Column(name = "duracion_acumulada")
    private LocalTime duracionAcumulada;

    @Column(name = "resultado_acumulado", precision = 10, scale = 2)
    private BigDecimal resultadoAcumulado;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @Column(name = "dato_audiencia_share", precision = 5, scale = 2)
    private BigDecimal datoAudienciaShare;

    @Column(name = "dato_audiencia_target", precision = 5, scale = 2)
    private BigDecimal datoAudienciaTarget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPrograma estado = EstadoPrograma.borrador;

    @Column(name = "total_premios", precision = 10, scale = 2)
    private BigDecimal totalPremios;

    @Column(name = "gap")
    private String gap;

    @Column(name = "total_concursantes")
    private Integer totalConcursantes;

    @Column(name = "creditos_especiales", columnDefinition = "TEXT")
    private String creditosEspeciales;

    public enum EstadoPrograma {
        borrador, grabado, editado, programado, emitido
    }

    public void actualizarEstado() {
        if (fechaEmision != null) {
            this.estado = EstadoPrograma.programado;
        } else if (duracionAcumulada != null) {
            this.estado = EstadoPrograma.editado;
        } else if (resultadoAcumulado != null) {
            this.estado = EstadoPrograma.grabado;
        } else {
            this.estado = EstadoPrograma.borrador;
        }
    }
} 