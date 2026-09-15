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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
        }

        Map<Long, JornadaCuestionarioAsignacion> existentes = new HashMap<>();
        for (JornadaCuestionarioAsignacion a : cuestionarioAsignaciones) {
            if (a.getCuestionario() != null && a.getCuestionario().getId() != null) {
                existentes.put(a.getCuestionario().getId(), a);
            }
        }

        Set<Long> idsDeseados = new HashSet<>();
        List<JornadaCuestionarioAsignacion> resultado = new ArrayList<>();
        if (porSlot != null) {
            for (int i = 0; i < porSlot.size() && i < SlotsJornada.TOTAL; i++) {
                Cuestionario c = porSlot.get(i);
                if (c == null || c.getId() == null) {
                    continue;
                }
                idsDeseados.add(c.getId());
                JornadaCuestionarioAsignacion a = existentes.get(c.getId());
                if (a != null) {
                    a.setSlot(i + 1);
                    resultado.add(a);
                } else {
                    JornadaCuestionarioAsignacion nueva = new JornadaCuestionarioAsignacion();
                    nueva.vincular(this, c, i + 1);
                    resultado.add(nueva);
                }
            }
        }

        cuestionarioAsignaciones.removeIf(a ->
            a.getCuestionario() == null
                || a.getCuestionario().getId() == null
                || !idsDeseados.contains(a.getCuestionario().getId()));
        cuestionarioAsignaciones.addAll(resultado);
    }

    public void reemplazarCombosPorSlot(List<Combo> porSlot) {
        if (comboAsignaciones == null) {
            comboAsignaciones = new HashSet<>();
        }

        Map<Long, JornadaComboAsignacion> existentes = new HashMap<>();
        for (JornadaComboAsignacion a : comboAsignaciones) {
            if (a.getCombo() != null && a.getCombo().getId() != null) {
                existentes.put(a.getCombo().getId(), a);
            }
        }

        Set<Long> idsDeseados = new HashSet<>();
        List<JornadaComboAsignacion> resultado = new ArrayList<>();
        if (porSlot != null) {
            for (int i = 0; i < porSlot.size() && i < SlotsJornada.TOTAL; i++) {
                Combo c = porSlot.get(i);
                if (c == null || c.getId() == null) {
                    continue;
                }
                idsDeseados.add(c.getId());
                JornadaComboAsignacion a = existentes.get(c.getId());
                if (a != null) {
                    a.setSlot(i + 1);
                    resultado.add(a);
                } else {
                    JornadaComboAsignacion nueva = new JornadaComboAsignacion();
                    nueva.vincular(this, c, i + 1);
                    resultado.add(nueva);
                }
            }
        }

        comboAsignaciones.removeIf(a ->
            a.getCombo() == null
                || a.getCombo().getId() == null
                || !idsDeseados.contains(a.getCombo().getId()));
        comboAsignaciones.addAll(resultado);
    }

    public enum EstadoJornada {
        preparacion, lista, en_grabacion, completada, archivada
    }

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }
}
