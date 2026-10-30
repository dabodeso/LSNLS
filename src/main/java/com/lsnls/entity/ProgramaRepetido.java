package com.lsnls.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(
    name = "programas_repetidos",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_repetido",
        columnNames = {"tipo", "codigo", "temporada", "fecha_emision"}
    )
)
public class ProgramaRepetido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, length = 16)
    private String tipo;

    @Column(nullable = false, length = 32)
    private String codigo;

    @Column(nullable = false)
    private Integer temporada;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @Column(name = "programa_original_codigo", length = 32)
    private String programaOriginalCodigo;

    @Column(name = "fecha_original")
    private LocalDate fechaOriginal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Programa.EstadoPrograma estado;

    @Column(name = "total_premios", precision = 10, scale = 2)
    private BigDecimal totalPremios;

    private String gap;

    @Column(name = "total_concursantes")
    private Integer totalConcursantes;

    @Column(name = "creditos_especiales", columnDefinition = "TEXT")
    private String creditosEspeciales;

    @Column(columnDefinition = "TEXT")
    private String notas;
}
