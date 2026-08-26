package com.lsnls.config;

import java.util.Map;
import java.util.Set;

/**
 * Grafo de estados compartido entre combo y cuestionario.
 * Combo añade reaprovechado y liberado como estados terminales.
 */
public final class TransicionesEstado {

    private static final Map<String, Set<String>> GRAFO = Map.of(
            "borrador", Set.of("revisar"),
            "revisar", Set.of("corregir", "aprobado"),
            "corregir", Set.of("revisar", "aprobado"),
            "aprobado", Set.of(),
            "adjudicado", Set.of(),
            "grabado", Set.of(),
            "reaprovechado", Set.of(),
            "liberado", Set.of()
    );

    private TransicionesEstado() {
    }

    public static void validar(Enum<?> estadoActual, Enum<?> nuevoEstado, boolean usuarioEsAdmin) {
        if (estadoActual == null || nuevoEstado == null || estadoActual == nuevoEstado) {
            return;
        }
        if (usuarioEsAdmin) {
            return;
        }
        String actual = estadoActual.name();
        String nuevo = nuevoEstado.name();
        Set<String> permitidos = GRAFO.getOrDefault(actual, Set.of());
        if (!permitidos.contains(nuevo)) {
            throw new IllegalArgumentException(
                    "Transición de estado no permitida: " + actual + " -> " + nuevo);
        }
    }
}
