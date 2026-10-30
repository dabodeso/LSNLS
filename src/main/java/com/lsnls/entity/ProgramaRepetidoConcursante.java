package com.lsnls.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Data
@Entity
@Table(
    name = "programas_repetidos_concursantes",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_repe_conc", columnNames = {"programa_repetido_id", "concursante_id"}),
        @UniqueConstraint(name = "uk_repe_orden", columnNames = {"programa_repetido_id", "orden_escaleta"})
    }
)
public class ProgramaRepetidoConcursante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "programa_repetido_id", nullable = false)
    private Long programaRepetidoId;

    @Column(name = "concursante_id", nullable = false)
    private Long concursanteId;

    @Column(name = "orden_escaleta", nullable = false)
    private Integer ordenEscaleta;
}
