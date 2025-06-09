package com.lsnls.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cuestionarios_preguntas")
public class PreguntaCuestionario {
    @EmbeddedId
    private PreguntaCuestionarioId id;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("preguntaId")
    @JoinColumn(name = "pregunta_id")
    @JsonIgnoreProperties({"cuestionarios", "subtemas", "creacionUsuario", "verificacionUsuario"})
    private Pregunta pregunta;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("cuestionarioId")
    @JoinColumn(name = "cuestionario_id")
    @JsonBackReference
    private Cuestionario cuestionario;

    @Column(name = "factor_multiplicacion")
    private Integer factorMultiplicacion;

    // Métodos equals y hashCode que evitan el bucle infinito
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PreguntaCuestionario that = (PreguntaCuestionario) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Embeddable
    @Getter
    @Setter
    public static class PreguntaCuestionarioId implements java.io.Serializable {
        @Column(name = "pregunta_id")
        private Long preguntaId;

        @Column(name = "cuestionario_id")
        private Long cuestionarioId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PreguntaCuestionarioId that = (PreguntaCuestionarioId) o;
            return preguntaId != null && preguntaId.equals(that.preguntaId) &&
                   cuestionarioId != null && cuestionarioId.equals(that.cuestionarioId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(preguntaId, cuestionarioId);
        }
    }
} 