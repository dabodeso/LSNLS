package com.lsnls.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tematicas")
public class Tematica {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;
    
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creacion_usuario_id")
    private Usuario creacionUsuario;
    
    @Version
    private Long version;
    
    // Constructores
    public Tematica() {}
    
    public Tematica(String nombre, Usuario creacionUsuario) {
        this.nombre = nombre;
        this.creacionUsuario = creacionUsuario;
        this.fechaCreacion = LocalDateTime.now();
    }
    
    // Getters y Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
    
    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
    
    public Usuario getCreacionUsuario() {
        return creacionUsuario;
    }
    
    public void setCreacionUsuario(Usuario creacionUsuario) {
        this.creacionUsuario = creacionUsuario;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
} 