package com.lsnls.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Impide marcar un programa como editado si algún concursante no lo está. */
final class EstadoEditadoPrograma {

    private EstadoEditadoPrograma() {
    }

    static boolean esEditado(String estado) {
        return estado != null && "editado".equals(estado.trim().toLowerCase(Locale.ROOT));
    }

    static void exigir(List<Integer> numeros, int sinNumero) {
        String mensaje = mensaje(numeros, sinNumero);
        if (mensaje != null) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    static String mensaje(List<Integer> numeros, int sinNumero) {
        List<Integer> ordenados = new ArrayList<>();
        if (numeros != null) {
            for (Integer numero : numeros) {
                if (numero != null && !ordenados.contains(numero)) {
                    ordenados.add(numero);
                }
            }
        }
        ordenados.sort(Comparator.naturalOrder());
        List<String> etiquetas = new ArrayList<>();
        for (Integer numero : ordenados) {
            etiquetas.add(String.valueOf(numero));
        }
        if (sinNumero == 1) {
            etiquetas.add("sin número");
        } else if (sinNumero > 1) {
            etiquetas.add(sinNumero + " sin número");
        }
        if (etiquetas.isEmpty()) {
            return null;
        }
        if (etiquetas.size() == 1 && sinNumero == 0) {
            return "No se puede pasar el programa a editado: el concursante "
                    + etiquetas.get(0) + " no está editado.";
        }
        return "No se puede pasar el programa a editado: los concursantes "
                + String.join(", ", etiquetas) + " no están editados.";
    }
}
