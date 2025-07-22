package com.lsnls.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.persistence.*;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "combos_preguntas")
public class PreguntaCombo {
    @EmbeddedId
    private PreguntaComboId id;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("preguntaId")
    @JoinColumn(name = "pregunta_id")
    @JsonIgnoreProperties({"cuestionarios", "combos", "subtemas", "creacionUsuario", "verificacionUsuario"})
    private Pregunta pregunta;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("comboId")
    @JoinColumn(name = "combo_id")
    @JsonBackReference
    private Combo combo;

    @Column(name = "factor_multiplicacion")
    private Integer factorMultiplicacion;

    // Clase embebida para la clave compuesta
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    public static class PreguntaComboId {
        @Column(name = "pregunta_id")
        private Long preguntaId;

        @Column(name = "combo_id")
        private Long comboId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PreguntaComboId that = (PreguntaComboId) o;
            return preguntaId != null && preguntaId.equals(that.preguntaId) &&
                   comboId != null && comboId.equals(that.comboId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(preguntaId, comboId);
        }
    }

    // Métodos equals y hashCode que evitan el bucle infinito
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PreguntaCombo that = (PreguntaCombo) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
} 