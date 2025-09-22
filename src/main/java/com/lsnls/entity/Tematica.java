package com.lsnls.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.persistence.*;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tematicas")
public class Tematica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "creacion_usuario_id", nullable = false)
    @JsonIgnoreProperties({"preguntasCreadas", "preguntasVerificadas", "cuestionariosCreados", "combosCreados"})
    private Usuario creacionUsuario;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    // Constructor para crear nuevas temáticas
    public Tematica(String nombre, Usuario creacionUsuario) {
        this.nombre = nombre;
        this.creacionUsuario = creacionUsuario;
        this.fechaCreacion = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
