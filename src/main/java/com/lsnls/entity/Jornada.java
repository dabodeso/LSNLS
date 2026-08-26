package com.lsnls.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lsnls.config.SlotsJornada;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "jornadas")
public class Jornada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "fecha_jornada")
    private LocalDate fechaJornada;

    @Column(name = "lugar")
    private String lugar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoJornada estado = EstadoJornada.preparacion;

    @ManyToOne
    @JoinColumn(name = "creacion_usuario_id", nullable = false)
    @JsonIgnoreProperties({"preguntasCreadas", "preguntasVerificadas", "cuestionariosCreados"})
    private Usuario creacionUsuario;

    @Column(name = "fecha_creacion")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaCreacion;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @OneToMany(mappedBy = "jornada", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private Set<JornadaCuestionarioAsignacion> cuestionarioAsignaciones = new HashSet<>();

    @OneToMany(mappedBy = "jornada", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private Set<JornadaComboAsignacion> comboAsignaciones = new HashSet<>();

    public Set<Cuestionario> getCuestionarios() {
        LinkedHashSet<Cuestionario> set = new LinkedHashSet<>();
        for (Cuestionario c : getCuestionariosPorSlot()) {
            if (c != null) {
                set.add(c);
            }
        }
        return set;
    }

    public void setCuestionarios(Set<Cuestionario> cuestionarios) {
        List<Cuestionario> porSlot = new ArrayList<>(Collections.nCopies(SlotsJornada.TOTAL, null));
        int i = 0;
        if (cuestionarios != null) {
            for (Cuestionario c : cuestionarios) {
                if (i >= SlotsJornada.TOTAL) {
                    break;
                }
                porSlot.set(i++, c);
            }
        }
        reemplazarCuestionariosPorSlot(porSlot);
    }

    public Set<Combo> getCombos() {
        LinkedHashSet<Combo> set = new LinkedHashSet<>();
        for (Combo c : getCombosPorSlot()) {
            if (c != null) {
                set.add(c);
            }
        }
        return set;
    }

    public void setCombos(Set<Combo> combos) {
        List<Combo> porSlot = new ArrayList<>(Collections.nCopies(SlotsJornada.TOTAL, null));
        int i = 0;
        if (combos != null) {
            for (Combo c : combos) {
                if (i >= SlotsJornada.TOTAL) {
                    break;
                }
                porSlot.set(i++, c);
            }
        }
        reemplazarCombosPorSlot(porSlot);
    }

    public List<Cuestionario> getCuestionariosPorSlot() {
        List<Cuestionario> slots = new ArrayList<>(Collections.nCopies(SlotsJornada.TOTAL, null));
        if (cuestionarioAsignaciones == null) {
            return slots;
        }
        for (JornadaCuestionarioAsignacion a : cuestionarioAsignaciones) {
            Integer slot = a.getSlot();
            if (slot != null && slot >= 1 && slot <= SlotsJornada.TOTAL) {
                slots.set(slot - 1, a.getCuestionario());
            }
        }
        return slots;
    }

    public List<Combo> getCombosPorSlot() {
        List<Combo> slots = new ArrayList<>(Collections.nCopies(SlotsJornada.TOTAL, null));
        if (comboAsignaciones == null) {
            return slots;
        }
        for (JornadaComboAsignacion a : comboAsignaciones) {
            Integer slot = a.getSlot();
            if (slot != null && slot >= 1 && slot <= SlotsJornada.TOTAL) {
                slots.set(slot - 1, a.getCombo());
            }
        }
        return slots;
    }

    public void reemplazarCuestionariosPorSlot(List<Cuestionario> porSlot) {
        if (cuestionarioAsignaciones == null) {
            cuestionarioAsignaciones = new HashSet<>();
        } else {
            cuestionarioAsignaciones.clear();
        }
        if (porSlot == null) {
            return;
        }
        for (int i = 0; i < porSlot.size() && i < SlotsJornada.TOTAL; i++) {
            Cuestionario c = porSlot.get(i);
            if (c == null) {
                continue;
            }
            JornadaCuestionarioAsignacion a = new JornadaCuestionarioAsignacion();
            a.vincular(this, c, i + 1);
            cuestionarioAsignaciones.add(a);
        }
    }

    public void reemplazarCombosPorSlot(List<Combo> porSlot) {
        if (comboAsignaciones == null) {
            comboAsignaciones = new HashSet<>();
        } else {
            comboAsignaciones.clear();
        }
        if (porSlot == null) {
            return;
        }
        for (int i = 0; i < porSlot.size() && i < SlotsJornada.TOTAL; i++) {
            Combo c = porSlot.get(i);
            if (c == null) {
                continue;
            }
            JornadaComboAsignacion a = new JornadaComboAsignacion();
            a.vincular(this, c, i + 1);
            comboAsignaciones.add(a);
        }
    }

    public enum EstadoJornada {
        preparacion, lista, en_grabacion, completada, archivada
    }

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }
}
