package com.lsnls.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "configuracion_global")
public class ConfiguracionGlobal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "clave", unique = true, nullable = false)
    private String clave;

    @Column(name = "valor", nullable = false)
    private String valor;

    @Column(name = "descripcion")
    private String descripcion;

    // Constructor para facilitar la creación
    public ConfiguracionGlobal(String clave, String valor, String descripcion) {
        this.clave = clave;
        this.valor = valor;
        this.descripcion = descripcion;
    }
} 