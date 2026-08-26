package com.lsnls.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Seis huecos fijos de cuestionario y combo en una jornada.
 * Un null es un hueco vacío: quitar el 2 no mueve el 1 ni el 3.
 */
public final class SlotsJornada {

    public static final int TOTAL = 6;

    private SlotsJornada() {
    }

    /**
     * Interpreta la lista del cliente. Si trae nulls o tiene longitud 6,
     * cada índice es un hueco. Si viene compacta (legacy), se coloca desde el 1.
     */
    public static List<Long> normalizarIds(List<Long> ids) {
        List<Long> slots = new ArrayList<>(Collections.nCopies(TOTAL, null));
        if (ids == null || ids.isEmpty()) {
            return slots;
        }
        if (ids.size() > TOTAL) {
            throw new IllegalArgumentException("Máximo " + TOTAL + " cuestionarios o combos por jornada");
        }
        boolean posicional = ids.size() == TOTAL || ids.stream().anyMatch(Objects::isNull);
        if (posicional) {
            for (int i = 0; i < ids.size(); i++) {
                slots.set(i, ids.get(i));
            }
        } else {
            int i = 0;
            for (Long id : ids) {
                slots.set(i++, id);
            }
        }
        Set<Long> vistos = new LinkedHashSet<>();
        for (Long id : slots) {
            if (id == null) {
                continue;
            }
            if (!vistos.add(id)) {
                throw new IllegalArgumentException("El mismo elemento no puede ocupar dos huecos de la jornada");
            }
        }
        return slots;
    }

    public static Set<Long> idsAsignados(List<Long> slots) {
        Set<Long> ids = new LinkedHashSet<>();
        if (slots == null) {
            return ids;
        }
        for (Long id : slots) {
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }
}
