package com.lsnls.config;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Convierte excepciones y textos de error en mensajes comprensibles
 * para alguien que no programa. Nunca debe devolver SQL, clases Java,
 * trazas ni ingles de infraestructura.
 */
public final class MensajesUsuario {

    public static final String GENERICO = "No se ha podido completar la operación. Inténtalo de nuevo.";
    public static final String RED = "No hay conexión con el servidor. Comprueba tu red e inténtalo de nuevo.";
    public static final String SESION = "Tu sesión ha caducado. Vuelve a iniciar sesión.";
    public static final String PERMISOS = "No tienes permisos para realizar esta acción.";
    public static final String CONCURRENCIA = "Otro usuario ha modificado estos datos. Recarga la página e inténtalo de nuevo.";
    public static final String VALIDACION = "Los datos no son válidos. Revisa los campos e inténtalo de nuevo.";
    public static final String NO_ENCONTRADO = "No se ha encontrado lo que buscas.";
    public static final String ARCHIVO_GRANDE = "El archivo es demasiado grande. El tamaño máximo permitido es 10 MB.";
    public static final String RELACIONADOS = "No se puede completar porque hay datos relacionados o duplicados.";

    private static final Pattern EMOJIS = Pattern.compile(
            "[\\x{1F300}-\\x{1FAFF}\\x{2600}-\\x{27BF}\\x{FE0F}\\x{200D}]");

    private static final Pattern TECNICO = Pattern.compile(
            "(?i)(failed to fetch|networkerror|load failed|net::err|"
                    + "typeerror|referenceerror|syntaxerror|unexpected token|"
                    + "json\\.parse|is not (a function|defined|iterable)|"
                    + "cannot read propert|sql(state|exception|syntax)|hibernate|jdbc|"
                    + "org\\.springframework|org\\.hibernate|java\\.(lang|sql|io|util)|"
                    + "nested exception|could not (execute|extract|open|obtain|serialize)|"
                    + "no enum constant|optimistic.?lock|objectoptimisticlocking|"
                    + "data integrity|constraint(violation)?|duplicate entry|whitelabel|"
                    + "http/1\\.[01]|content-type:|stack trace|unauthorized:\\s*token|"
                    + "nullpointer|classcastexception|transaction.*rolled back|deadlock|"
                    + "lock wait timeout|unknown column|doesn't exist|data truncated|"
                    + "caused by:|exception in thread|\\bat\\s+[a-z0-9_.$]+\\(|"
                    + "row was updated or deleted|was concurrently updated|"
                    + "\\bundefined\\b|\\[object object\\]|revisa la consola|"
                    + "el body debe ser un json)");

    private MensajesUsuario() {
    }

    public static String sanitizar(String texto) {
        return sanitizar(texto, GENERICO);
    }

    public static String sanitizar(String texto, String fallback) {
        String reserva = (fallback == null || fallback.isBlank()) ? GENERICO : fallback;
        if (texto == null) {
            return reserva;
        }
        String t = quitarEmojis(texto).trim();
        if (t.isEmpty()) {
            return reserva;
        }
        String lower = t.toLowerCase(Locale.ROOT);
        if (esRed(lower)) {
            return RED;
        }
        if (esSesion(lower)) {
            return SESION;
        }
        if (esConcurrencia(lower)) {
            return CONCURRENCIA;
        }
        if (esRelacionados(lower)) {
            return RELACIONADOS;
        }
        if (esTecnico(t) || esGenericoIngles(lower)) {
            return reserva;
        }
        return t;
    }

    public static String de(Throwable error, String accion) {
        String fallback = accion == null || accion.isBlank()
                ? GENERICO
                : "No se ha podido " + accion + ". Inténtalo de nuevo.";
        if (error == null) {
            return fallback;
        }
        return sanitizar(error.getMessage(), fallback);
    }

    public static String porHttp(int status) {
        if (status == 401) {
            return SESION;
        }
        if (status == 403) {
            return PERMISOS;
        }
        if (status == 404) {
            return NO_ENCONTRADO;
        }
        if (status == 409) {
            return CONCURRENCIA;
        }
        if (status == 413) {
            return ARCHIVO_GRANDE;
        }
        if (status == 400 || status == 422) {
            return VALIDACION;
        }
        return GENERICO;
    }

    public static boolean esTecnico(String texto) {
        if (texto == null || texto.isBlank()) {
            return true;
        }
        return TECNICO.matcher(texto).find() || texto.contains("\n\tat ");
    }

    static String quitarEmojis(String texto) {
        if (texto == null) {
            return "";
        }
        return EMOJIS.matcher(texto).replaceAll("").replaceAll("\\s{2,}", " ").trim();
    }

    private static boolean esRed(String lower) {
        return lower.contains("failed to fetch")
                || lower.contains("networkerror")
                || lower.contains("load failed")
                || lower.contains("net::err");
    }

    private static boolean esSesion(String lower) {
        return lower.contains("unauthorized")
                || lower.contains("token expirado")
                || lower.contains("token inválido")
                || lower.contains("token invalido")
                || lower.contains("jwt");
    }

    private static boolean esConcurrencia(String lower) {
        return lower.contains("optimistic")
                || lower.contains("concurrencia")
                || lower.contains("row was updated")
                || lower.contains("was concurrently updated");
    }

    private static boolean esRelacionados(String lower) {
        return lower.contains("constraint")
                || lower.contains("duplicate entry")
                || lower.contains("foreign key")
                || lower.contains("integridad");
    }

    private static boolean esGenericoIngles(String lower) {
        return lower.equals("forbidden")
                || lower.equals("access denied")
                || lower.equals("unauthorized")
                || lower.equals("bad request")
                || lower.equals("not found")
                || lower.equals("internal server error")
                || lower.equals("conflict")
                || lower.equals("error")
                || lower.matches("^(error|http error)\\s*\\d{0,3}$");
    }
}
