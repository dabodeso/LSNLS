package com.lsnls.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cuestionarios")
public class Cuestionario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne
    @JoinColumn(name = "creacion_usuario_id", nullable = false)
    @JsonIgnoreProperties({"preguntasCreadas", "preguntasVerificadas", "cuestionariosCreados"})
    private Usuario creacionUsuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelCuestionario nivel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCuestionario estado = EstadoCuestionario.borrador;

    @Column(name = "tematica")
    private String tematica;

    @Column(name = "notas_direccion", columnDefinition = "TEXT")
    private String notasDireccion;

    @Column(name = "fecha_creacion")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "cuestionario", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("cuestionario")
    private Set<PreguntaCuestionario> preguntas;

    // Métodos equals y hashCode que evitan el bucle infinito
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cuestionario that = (Cuestionario) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public enum NivelCuestionario {
        _1LS("1LS"), _2NLS("2NLS"), _3LS("3LS"), _4NLS("4NLS"), 
        PM1("PM1"), PM2("PM2"), PM3("PM3"), NORMAL("NORMAL");

        private final String valor;

        NivelCuestionario(String valor) {
            this.valor = valor;
        }

        public String getValor() {
            return valor;
        }
    }

    public enum EstadoCuestionario {
        borrador, creado, adjudicado, grabado, asignado_jornada, asignado_concursantes
    }

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }
} 