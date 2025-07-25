package com.lsnls.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonFormat;
import javax.persistence.*;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "combos")
public class Combo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne
    @JoinColumn(name = "creacion_usuario_id", nullable = false)
    @JsonIgnoreProperties({"preguntasCreadas", "preguntasVerificadas", "cuestionariosCreados", "combosCreados"})
    private Usuario creacionUsuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelCombo nivel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCombo estado = EstadoCombo.borrador;

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    private TipoCombo tipo;

    @Column(name = "fecha_creacion")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "combo", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("combo")
    private Set<PreguntaCombo> preguntas;

    // Enums
    public enum NivelCombo {
        _5LS, _5NLS, NORMAL
    }

    public enum EstadoCombo {
        borrador, creado, adjudicado, grabado
    }

    public enum TipoCombo {
        P, // Premio
        A, // Asequible 
        D  // Difícil
    }

    // Métodos equals y hashCode que evitan el bucle infinito
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Combo that = (Combo) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
} 