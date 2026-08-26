package com.lsnls.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "jornadas_cuestionarios")
public class JornadaCuestionarioAsignacion {

    @EmbeddedId
    private Id id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("jornadaId")
    @JoinColumn(name = "jornada_id")
    @JsonIgnore
    private Jornada jornada;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("cuestionarioId")
    @JoinColumn(name = "cuestionario_id")
    @JsonIgnoreProperties({"preguntas", "creacionUsuario"})
    private Cuestionario cuestionario;

    @Column(name = "slot")
    private Integer slot;

    public void vincular(Jornada jornada, Cuestionario cuestionario, int slot) {
        this.jornada = jornada;
        this.cuestionario = cuestionario;
        this.slot = slot;
        Id clave = new Id();
        clave.setJornadaId(jornada.getId());
        clave.setCuestionarioId(cuestionario.getId());
        this.id = clave;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JornadaCuestionarioAsignacion that = (JornadaCuestionarioAsignacion) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Id implements Serializable {
        @Column(name = "jornada_id")
        private Long jornadaId;
        @Column(name = "cuestionario_id")
        private Long cuestionarioId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Id that = (Id) o;
            return Objects.equals(jornadaId, that.jornadaId)
                    && Objects.equals(cuestionarioId, that.cuestionarioId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(jornadaId, cuestionarioId);
        }
    }
}
