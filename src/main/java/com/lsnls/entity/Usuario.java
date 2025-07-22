package com.lsnls.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, length = 50)
    private String nombre;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolUsuario rol;

    @JsonIgnore
    @OneToMany(mappedBy = "creacionUsuario")
    private Set<Pregunta> preguntasCreadas;

    @JsonIgnore
    @OneToMany(mappedBy = "verificacionUsuario")
    private Set<Pregunta> preguntasVerificadas;

    @JsonIgnore
    @OneToMany(mappedBy = "creacionUsuario")
    private Set<Cuestionario> cuestionariosCreados;

    @JsonIgnore
    @OneToMany(mappedBy = "creacionUsuario")
    private Set<Combo> combosCreados;

    public enum RolUsuario {
        ROLE_ADMIN, ROLE_CONSULTA, ROLE_GUION, ROLE_VERIFICACION, ROLE_DIRECCION;

        @Override
        public String toString() {
            return name();
        }
    }
} 