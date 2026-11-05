package com.lsnls.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * «Ha salido» cuenta programas distintos en los que el concursante está asignado.
 * Un programa normal cuenta una vez aunque figure a la vez en numero_programa y en
 * programas_concursantes. Cada programa de la tabla de repetidos suma otra salida,
 * aunque su id coincida con el de un programa normal.
 * El campo estado (grabado, editado, emitido…) no significa eso: emitido del filtro
 * es exactamente una salida y repetido es más de una. Filtrar no reescribe el estado.
 */
public final class SalidasConcursante {

    /** Exactamente un programa. Valor de :salidas en la consulta de disponibles. */
    public static final String UNA = "una";
    /** Más de un programa. Valor de :salidas en la consulta de disponibles. */
    public static final String MAS = "mas";

    private SalidasConcursante() {
    }

    public static final class Filtro {
        /** Estado de producción (grabado o editado), o null si no se filtra por la columna. */
        public final String estado;
        /** {@link #UNA}, {@link #MAS} o null. */
        public final String salidas;

        Filtro(String estado, String salidas) {
            this.estado = estado;
            this.salidas = salidas;
        }
    }

    /**
     * emitido y repetido no se traducen a la columna estado: el primero pide una salida
     * y el segundo, más de una. Grabado y editado siguen filtrando por esa columna.
     */
    public static Filtro interpretar(String crudo) {
        if (crudo == null || crudo.isBlank()) {
            return new Filtro(null, null);
        }
        String valor = crudo.trim().toLowerCase(Locale.ROOT);
        if ("emitido".equals(valor)) {
            return new Filtro(null, UNA);
        }
        if ("repetido".equals(valor)) {
            return new Filtro(null, MAS);
        }
        if ("grabado".equals(valor) || "editado".equals(valor)) {
            return new Filtro(valor, null);
        }
        return new Filtro(null, null);
    }

    public static int contar(Integer numeroPrograma, Collection<Long> programasNormales, Collection<Long> programasRepetidos) {
        Set<Long> normales = new HashSet<>();
        if (programasNormales != null) {
            for (Long id : programasNormales) {
                if (id != null) {
                    normales.add(id);
                }
            }
        }
        if (numeroPrograma != null) {
            normales.add(numeroPrograma.longValue());
        }
        Set<Long> repetidos = new HashSet<>();
        if (programasRepetidos != null) {
            for (Long id : programasRepetidos) {
                if (id != null) {
                    repetidos.add(id);
                }
            }
        }
        return normales.size() + repetidos.size();
    }

    /** null si no ha salido; "emitido" con una salida; "repetido" con más de una. */
    public static String clasificar(int salidas) {
        if (salidas == 1) {
            return "emitido";
        }
        if (salidas > 1) {
            return "repetido";
        }
        return null;
    }
}
