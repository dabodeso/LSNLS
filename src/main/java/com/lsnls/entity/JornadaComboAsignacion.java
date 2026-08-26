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
@Table(name = "jornadas_combos")
public class JornadaComboAsignacion {

    @EmbeddedId
    private Id id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("jornadaId")
    @JoinColumn(name = "jornada_id")
    @JsonIgnore
    private Jornada jornada;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("comboId")
    @JoinColumn(name = "combo_id")
    @JsonIgnoreProperties({"preguntas", "creacionUsuario"})
    private Combo combo;

    @Column(name = "slot")
    private Integer slot;

    public void vincular(Jornada jornada, Combo combo, int slot) {
        this.jornada = jornada;
        this.combo = combo;
        this.slot = slot;
        Id clave = new Id();
        clave.setJornadaId(jornada.getId());
        clave.setComboId(combo.getId());
        this.id = clave;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JornadaComboAsignacion that = (JornadaComboAsignacion) o;
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
        @Column(name = "combo_id")
        private Long comboId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Id that = (Id) o;
            return Objects.equals(jornadaId, that.jornadaId)
                    && Objects.equals(comboId, that.comboId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(jornadaId, comboId);
        }
    }
}
