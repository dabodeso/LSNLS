package com.lsnls.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
@Table(
    name = "programas_concursantes",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_programa_concursante", columnNames = {"programa_id", "concursante_id"}),
        @UniqueConstraint(name = "uk_programa_orden", columnNames = {"programa_id", "orden_escaleta"})
    }
)
public class ProgramaConcursante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "programa_id", nullable = false)
    private Long programaId;

    @Column(name = "concursante_id", nullable = false)
    private Long concursanteId;

    @Column(name = "orden_escaleta", nullable = false)
    private Integer ordenEscaleta;
}
