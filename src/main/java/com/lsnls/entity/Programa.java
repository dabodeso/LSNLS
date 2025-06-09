package com.lsnls.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "programas")
public class Programa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @OneToMany(mappedBy = "programa")
    private Set<Concursante> concursantes;

    public enum EstadoPrograma {
        borrador, programado, emitido
    }
} 