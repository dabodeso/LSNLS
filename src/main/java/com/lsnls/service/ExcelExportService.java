package com.lsnls.service;

import com.lsnls.entity.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Genera el Excel de una jornada calcando la plantilla de producción.
 *
 * Antes de completar: dos hojas (cuestionarios / combos) con los bloques
 * uno detrás de otro y el pie vacío para rellenar en grabación.
 *
 * Completada (con datos de pie): una pestaña por concursante (cuestionario),
 * pie relleno, y la pregunta usada del combo en la 5.ª fila (bajo el nivel 4)
 * si existe; si no, esa fila queda vacía.
 */
@Service
public class ExcelExportService {

    /** Clave de opciones con los combos de origen por combo derivado: id -> "312/216". */
    public static final String OPCION_ANCESTROS_COMBO = "ancestrosCombo";

    /** Pie de cada cuestionario: id -> DatosPieBloque. */
    public static final String OPCION_PIE_CUESTIONARIO = "pieCuestionario";

    /** Pie de cada combo: id -> DatosPieBloque. (plantilla / compat; no crea pestañas en completada) */
    public static final String OPCION_PIE_COMBO = "pieCombo";

    /** Valores de las filas de cierre de un bloque. */
    public static class DatosPieBloque {
        public final String concursante;
        public final String resultado;
        public final String grabacion;
        /** Contenido de NOTAS GUION: notas de grabación del concursante. */
        public final String notasGuion;
        /** Valoración del guionista (p. ej. 2, 1+); va en rojo en A3–A5 fusionadas. */
        public final String valoracionGuionista;
        /** Nombre corto para la pestaña Excel (nombre del concursante). */
        public final String nombreHoja;
        /** Pregunta del combo usada en grabación; va en la 5.ª fila bajo el nivel 4. */
        public final Pregunta preguntaMultiplicadora;

        public DatosPieBloque(String concursante, String resultado, String grabacion, String notasGuion) {
            this(concursante, resultado, grabacion, notasGuion, null, null, null);
        }

        public DatosPieBloque(String concursante, String resultado, String grabacion,
                              String notasGuion, String nombreHoja) {
            this(concursante, resultado, grabacion, notasGuion, null, nombreHoja, null);
        }

        public DatosPieBloque(String concursante, String resultado, String grabacion,
                              String notasGuion, String nombreHoja, Pregunta preguntaMultiplicadora) {
            this(concursante, resultado, grabacion, notasGuion, null, nombreHoja, preguntaMultiplicadora);
        }

        public DatosPieBloque(String concursante, String resultado, String grabacion,
                              String notasGuion, String valoracionGuionista,
                              String nombreHoja, Pregunta preguntaMultiplicadora) {
            this.concursante = concursante;
            this.resultado = resultado;
            this.grabacion = grabacion;
            this.notasGuion = notasGuion;
            this.valoracionGuionista = valoracionGuionista;
            this.nombreHoja = nombreHoja;
            this.preguntaMultiplicadora = preguntaMultiplicadora;
        }
    }
    private static final String[] CABECERA_CUESTIONARIOS =
            {"CUEST", "NIVEL", "PREGUNTA", "RESPUESTA", "DATOS EXTRA", "REC"};
    private static final double[] ANCHOS_CUESTIONARIOS = {6.9, 6.7, 50.6, 19.6, 44.3, 8.4};
    /** Última columna del bloque cuestionario (0-based): REC. */
    private static final int ULTIMA_COL_CUEST = CABECERA_CUESTIONARIOS.length - 1;
    /** Etiqueta del pie ocupa CUEST+NIVEL (A:B). */
    private static final int ULTIMA_COL_ETIQUETA_PIE_CUEST = 1;

    private static final String[] CABECERA_COMBOS =
            {"COMBO", "", "TIPO", "FAC", "NIVEL", "PREGUNTA", "RESPUESTA", "DATOS EXTRA", "REC"};
    private static final double[] ANCHOS_COMBOS = {7.3, 0.6, 4.4, 4.7, 6.7, 45, 17.1, 43, 8.4};

    /** Filas de pregunta por bloque. La quinta de cuestionarios queda vacía para la pregunta multiplicadora. */
    private static final int FILAS_PREGUNTA_CUESTIONARIO = 5;
    private static final int PREGUNTAS_CUESTIONARIO = 4;

    private static final float ALTO_CABECERA = 15f;
    private static final float ALTO_PREGUNTA_CUESTIONARIO = 39.9f;
    private static final float ALTO_PREGUNTA_COMBO = 50.1f;
    private static final float ALTO_PIE_CUESTIONARIO = 33f;
    private static final float ALTO_NOTAS_CUESTIONARIO = 216f;
    private static final float ALTO_PIE_COMBO = 35.25f;
    private static final float ALTO_GRABACION_COMBO = 36.75f;
    private static final float ALTO_NOTAS_COMBO = 254.25f;

    /**
     * Exporta una jornada a Excel con la configuración por defecto.
     */
    public byte[] exportarJornada(Jornada jornada) throws IOException {
        return exportarJornada(jornada, null);
    }

    /**
     * Exporta una jornada a Excel.
     *
     * @param jornada  La jornada a exportar
     * @param opciones Opciones de configuración:
     *                 {@link #OPCION_ANCESTROS_COMBO}, mapa id combo → orígenes;
     *                 {@link #OPCION_PIE_CUESTIONARIO} / {@link #OPCION_PIE_COMBO},
     *                 mapas id → {@link DatosPieBloque}.
     * @return Bytes del archivo Excel generado
     */
    public byte[] exportarJornada(Jornada jornada, Map<String, Object> opciones) throws IOException {
        Map<Long, String> ancestros = leerAncestros(opciones);
        Map<Long, DatosPieBloque> pieCuestionarios = leerPies(opciones, OPCION_PIE_CUESTIONARIO);
        Map<Long, DatosPieBloque> pieCombos = leerPies(opciones, OPCION_PIE_COMBO);
        boolean porConcursante = !pieCuestionarios.isEmpty();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Estilos estilos = new Estilos(workbook);

            if (porConcursante) {
                crearHojasPorConcursante(workbook, jornada, estilos, pieCuestionarios);
            } else {
                Sheet hojaCuestionarios = workbook.createSheet("CUESTIONARIOS ADJUDICADOS");
                configurarPagina(hojaCuestionarios, "Cuestionarios - " + jornada.getNombre(), jornada.getNombre());
                crearHojaCuestionarios(hojaCuestionarios, jornada, estilos, pieCuestionarios);

                Sheet hojaCombos = workbook.createSheet("COMBOS ADJUDICADOS");
                configurarPagina(hojaCombos, "Combos - " + jornada.getNombre(), jornada.getNombre());
                crearHojaCombos(hojaCombos, jornada, estilos, ancestros, pieCombos);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Una pestaña por concursante (cuestionario), nombrada con su nombre.
     * Solo incluye cuestionarios con concursante asignado (entrada en pie).
     * La pregunta usada del combo del mismo slot va en la 5.ª fila si existe.
     */
    private void crearHojasPorConcursante(Workbook workbook, Jornada jornada, Estilos estilos,
                                          Map<Long, DatosPieBloque> pieCuestionarios) {
        Set<String> nombresUsados = new LinkedHashSet<>();
        String nombreJornada = jornada.getNombre();
        List<Cuestionario> cuestionarios = jornada.getCuestionariosPorSlot();
        List<Combo> combos = jornada.getCombosPorSlot();

        for (int slot = 0; slot < cuestionarios.size(); slot++) {
            Cuestionario cuestionario = cuestionarios.get(slot);
            if (cuestionario == null || cuestionario.getId() == null) {
                continue;
            }
            DatosPieBloque pie = pieCuestionarios.get(cuestionario.getId());
            if (pie == null) {
                continue;
            }
            Combo combo = slot < combos.size() ? combos.get(slot) : null;
            Pregunta multiplicadora = pie != null ? pie.preguntaMultiplicadora : null;
            if (multiplicadora == null) {
                multiplicadora = preguntaUsadaDelCombo(combo);
            }
            Long idCombo = combo != null ? combo.getId() : null;
            String titulo = nombrePestana(
                    pie != null && pie.nombreHoja != null ? pie.nombreHoja : null,
                    "Cuest " + (cuestionario.getId() != null ? cuestionario.getId() : ""),
                    nombresUsados);
            Sheet hoja = workbook.createSheet(titulo);
            for (int i = 0; i < ANCHOS_CUESTIONARIOS.length; i++) {
                hoja.setColumnWidth(i, excelWidth(ANCHOS_CUESTIONARIOS[i]));
            }
            configurarPagina(hoja, titulo + " - " + nombreJornada, nombreJornada);
            crearBloqueCuestionario(hoja, cuestionario, 0, estilos, pie, multiplicadora, idCombo);
        }
    }

    /** Resuelve la pregunta marcada como usada en un combo, o null si no hay. */
    static Pregunta preguntaUsadaDelCombo(Combo combo) {
        if (combo == null || combo.getPreguntaUsadaId() == null || combo.getPreguntas() == null) {
            return null;
        }
        Long usadaId = combo.getPreguntaUsadaId();
        for (PreguntaCombo pc : combo.getPreguntas()) {
            if (pc.getPregunta() != null && usadaId.equals(pc.getPregunta().getId())) {
                return pc.getPregunta();
            }
        }
        return null;
    }

    /** Excel limita el nombre de hoja a 31 caracteres y prohíbe \ / ? * [ ] : */
    static String nombrePestana(String preferido, String respaldo, Set<String> usados) {
        String base = sanitizarNombreHoja(preferido);
        if (base.isEmpty()) {
            base = sanitizarNombreHoja(respaldo);
        }
        if (base.isEmpty()) {
            base = "Sin nombre";
        }
        String candidato = base;
        int n = 2;
        while (contieneIgnorandoMayusculas(usados, candidato)) {
            String sufijo = " (" + n + ")";
            int maxBase = Math.max(1, 31 - sufijo.length());
            candidato = base.substring(0, Math.min(base.length(), maxBase)) + sufijo;
            n++;
        }
        usados.add(candidato);
        return candidato;
    }

    private static boolean contieneIgnorandoMayusculas(Set<String> usados, String candidato) {
        for (String u : usados) {
            if (u.equalsIgnoreCase(candidato)) {
                return true;
            }
        }
        return false;
    }

    private static String sanitizarNombreHoja(String bruto) {
        if (bruto == null) {
            return "";
        }
        String limpio = bruto.trim().replaceAll("[\\\\/?*\\[\\]:]", " ").replaceAll("\\s+", " ").trim();
        if (limpio.length() > 31) {
            limpio = limpio.substring(0, 31).trim();
        }
        return limpio;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, String> leerAncestros(Map<String, Object> opciones) {
        if (opciones == null) {
            return Collections.emptyMap();
        }
        Object valor = opciones.get(OPCION_ANCESTROS_COMBO);
        return valor instanceof Map ? (Map<Long, String>) valor : Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private Map<Long, DatosPieBloque> leerPies(Map<String, Object> opciones, String clave) {
        if (opciones == null) {
            return Collections.emptyMap();
        }
        Object valor = opciones.get(clave);
        return valor instanceof Map ? (Map<Long, DatosPieBloque>) valor : Collections.emptyMap();
    }

    /**
     * Texto de la fila CONCURSANTE:
     * {@code numero nombre, edad, (lugar) - profesion |Redes sociales| [NOLOSABELOTODO]}.
     * NOLOSABELOTODO solo si aparece en momentos destacados (sin mirar mayúsculas).
     */
    public static String textoConcursantePie(Concursante c) {
        if (c == null) {
            return "";
        }
        String numero = c.getNumeroConcursante() != null ? String.valueOf(c.getNumeroConcursante()) : "";
        String nombre = textoOVacio(c.getNombre());
        String edad = textoOVacio(c.getEdad());
        String lugar = textoOVacio(c.getLugar());
        String profesion = textoOVacio(c.getOcupacion());
        String redes = textoOVacio(c.getRedesSociales());
        StringBuilder sb = new StringBuilder();
        if (!numero.isEmpty()) {
            sb.append(numero);
            if (!nombre.isEmpty()) {
                sb.append(' ');
            }
        }
        sb.append(nombre).append(", ").append(edad)
                .append(", (").append(lugar).append(") - ").append(profesion)
                .append(" |").append(redes).append("|");
        String momentos = c.getMomentosDestacados();
        if (momentos != null && momentos.toLowerCase(Locale.ROOT).contains("nolosabelotodo")) {
            sb.append(" NOLOSABELOTODO");
        }
        return sb.toString();
    }

    private static String textoOVacio(String valor) {
        return valor != null ? valor.trim() : "";
    }
    // ---------------------------------------------------------------- CUESTIONARIOS

    private void crearHojaCuestionarios(Sheet sheet, Jornada jornada, Estilos estilos,
                                       Map<Long, DatosPieBloque> pies) {
        for (int i = 0; i < ANCHOS_CUESTIONARIOS.length; i++) {
            sheet.setColumnWidth(i, excelWidth(ANCHOS_CUESTIONARIOS[i]));
        }

        List<Cuestionario> cuestionarios = jornada.getCuestionariosPorSlot();
        List<Combo> combos = jornada.getCombosPorSlot();
        int filaActual = 0;
        for (int slot = 0; slot < cuestionarios.size(); slot++) {
            Cuestionario cuestionario = cuestionarios.get(slot);
            if (cuestionario == null) {
                continue;
            }
            DatosPieBloque pie = cuestionario.getId() != null ? pies.get(cuestionario.getId()) : null;
            Combo combo = slot < combos.size() ? combos.get(slot) : null;
            Long idCombo = combo != null ? combo.getId() : null;
            filaActual = crearBloqueCuestionario(sheet, cuestionario, filaActual, estilos, pie, null, idCombo);
        }
    }

    private int crearBloqueCuestionario(Sheet sheet, Cuestionario cuestionario, int filaInicial,
                                        Estilos estilos, DatosPieBloque pie, Pregunta multiplicadora,
                                        Long idCombo) {
        int filaActual = crearCabecera(sheet, filaInicial, CABECERA_CUESTIONARIOS, estilos);
        int filaInicioValoracion = filaActual + 1;

        List<PreguntaCuestionario> preguntas = preguntasOrdenadasPorNivel(cuestionario);
        String idCuestionario = cuestionario.getId() != null ? String.valueOf(cuestionario.getId()) : "";
        String idComboTexto = idCombo != null ? String.valueOf(idCombo) : "";
        String valoracion = pie != null ? textoOVacio(pie.valoracionGuionista) : "";

        for (int i = 0; i < FILAS_PREGUNTA_CUESTIONARIO; i++) {
            Row fila = sheet.createRow(filaActual++);
            fila.setHeightInPoints(ALTO_PREGUNTA_CUESTIONARIO);
            // Filas 1–4: niveles del cuestionario. 5.ª: pregunta usada del combo (o vacía).
            PreguntaCuestionario pc = i < Math.min(PREGUNTAS_CUESTIONARIO, preguntas.size()) ? preguntas.get(i) : null;
            Pregunta pregunta = pc != null ? pc.getPregunta()
                    : (i == PREGUNTAS_CUESTIONARIO ? multiplicadora : null);

            // A2 = nº cuestionario; A3–A5 fusionadas = valoración del guionista (rojo); A6 = nº combo.
            if (i == 0) {
                escribir(fila, 0, idCuestionario, estilos.primeraColumnaCentrada);
            } else if (i == 1) {
                escribir(fila, 0, valoracion, estilos.valoracionGuionista);
            } else if (i < PREGUNTAS_CUESTIONARIO) {
                escribir(fila, 0, "", estilos.valoracionGuionista);
            } else {
                escribir(fila, 0, idComboTexto, estilos.primeraColumnaCentrada);
            }
            escribir(fila, 1, nivelCorto(pregunta), estilos.datoPlano);
            escribir(fila, 2, pregunta != null ? pregunta.getPregunta() : "", estilos.datoAjustado);
            escribir(fila, 3, pregunta != null ? pregunta.getRespuesta() : "", estilos.datoAjustado);
            escribir(fila, 4, pregunta != null && pregunta.getDatosExtra() != null ? pregunta.getDatosExtra() : "",
                    estilos.datoAjustado);
            escribir(fila, 5, "", estilos.datoPlano);
        }
        // Solo A3–A5 (i=1..3); A6 queda fuera con el nº de combo.
        combinarVerticalConBorde(sheet, filaInicioValoracion, filaInicioValoracion + 2, 0);

        filaActual = crearFilaPie(sheet, filaActual, "CONCURSANTE",
                ULTIMA_COL_ETIQUETA_PIE_CUEST, ULTIMA_COL_CUEST, ALTO_PIE_CUESTIONARIO, estilos,
                pie != null ? pie.concursante : null, true);
        filaActual = crearFilaPie(sheet, filaActual, "RESULTADO",
                ULTIMA_COL_ETIQUETA_PIE_CUEST, ULTIMA_COL_CUEST, ALTO_PIE_CUESTIONARIO, estilos,
                pie != null ? pie.resultado : null, true);
        filaActual = crearFilaPie(sheet, filaActual, "GRABACIÓN",
                ULTIMA_COL_ETIQUETA_PIE_CUEST, ULTIMA_COL_CUEST, ALTO_PIE_CUESTIONARIO, estilos,
                pie != null ? pie.grabacion : null, true);
        filaActual = crearFilaPie(sheet, filaActual, "NOTAS GUION",
                ULTIMA_COL_ETIQUETA_PIE_CUEST, ULTIMA_COL_CUEST, ALTO_NOTAS_CUESTIONARIO, estilos,
                pie != null ? pie.notasGuion : null, false);

        sheet.setRowBreak(filaActual - 1);
        return filaActual;
    }

    private List<PreguntaCuestionario> preguntasOrdenadasPorNivel(Cuestionario cuestionario) {
        if (cuestionario.getPreguntas() == null) {
            return Collections.emptyList();
        }
        List<PreguntaCuestionario> preguntas = new ArrayList<>(cuestionario.getPreguntas());
        preguntas.sort(Comparator.comparingInt(pc -> ordenNivel(pc.getPregunta())));
        return preguntas;
    }

    private int ordenNivel(Pregunta pregunta) {
        if (pregunta == null || pregunta.getNivel() == null) {
            return Integer.MAX_VALUE;
        }
        String digitos = pregunta.getNivel().name().replaceAll("\\D+", "");
        return digitos.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(digitos);
    }

    // ---------------------------------------------------------------------- COMBOS

    private void crearHojaCombos(Sheet sheet, Jornada jornada, Estilos estilos, Map<Long, String> ancestros,
                                 Map<Long, DatosPieBloque> pies) {
        for (int i = 0; i < ANCHOS_COMBOS.length; i++) {
            sheet.setColumnWidth(i, excelWidth(ANCHOS_COMBOS[i]));
        }

        int filaActual = 0;
        for (Combo combo : jornada.getCombosPorSlot()) {
            if (combo == null) {
                continue;
            }
            DatosPieBloque pie = combo.getId() != null ? pies.get(combo.getId()) : null;
            filaActual = crearBloqueCombo(sheet, combo, filaActual, estilos, ancestros, pie);
        }
    }

    private int crearBloqueCombo(Sheet sheet, Combo combo, int filaInicial, Estilos estilos,
                                 Map<Long, String> ancestros, DatosPieBloque pie) {
        int filaActual = crearCabecera(sheet, filaInicial, CABECERA_COMBOS, estilos);

        String etiquetaCombo = etiquetaCombo(combo, ancestros);
        String tipo = combo.getTipo() != null ? combo.getTipo().name() : "";

        for (PreguntaCombo pc : preguntasOrdenadasPorFactor(combo)) {
            Row fila = sheet.createRow(filaActual++);
            fila.setHeightInPoints(ALTO_PREGUNTA_COMBO);
            Pregunta pregunta = pc.getPregunta();

            escribir(fila, 0, etiquetaCombo, estilos.primeraColumna);
            escribir(fila, 1, "", estilos.datoPlano);
            escribir(fila, 2, tipo, estilos.datoPlano);
            escribir(fila, 3, factorCorto(pc), estilos.datoPlano);
            escribir(fila, 4, nivelCorto(pregunta), estilos.datoPlano);
            escribir(fila, 5, pregunta != null ? pregunta.getPregunta() : "", estilos.datoAjustado);
            escribir(fila, 6, pregunta != null ? pregunta.getRespuesta() : "", estilos.datoAjustado);
            escribir(fila, 7, pregunta != null && pregunta.getDatosExtra() != null ? pregunta.getDatosExtra() : "",
                    estilos.datoAjustado);
            escribir(fila, 8, "", estilos.datoPlano);
        }

        filaActual = crearFilaPie(sheet, filaActual, "CONCURSANTE", 4, 8, ALTO_PIE_COMBO, estilos,
                pie != null ? pie.concursante : null, true);
        filaActual = crearFilaPie(sheet, filaActual, "RESULTADO", 4, 8, ALTO_PIE_COMBO, estilos,
                pie != null ? pie.resultado : null, true);
        filaActual = crearFilaPie(sheet, filaActual, "GRABACIÓN", 4, 8, ALTO_GRABACION_COMBO, estilos,
                pie != null ? pie.grabacion : null, true);
        filaActual = crearFilaPie(sheet, filaActual, "NOTAS GUION", 4, 8, ALTO_NOTAS_COMBO, estilos,
                pie != null ? pie.notasGuion : null, false);

        sheet.setRowBreak(filaActual - 1);
        return filaActual;
    }

    /** Un combo reciclado muestra debajo del id los combos de los que procede. */
    private String etiquetaCombo(Combo combo, Map<Long, String> ancestros) {
        if (combo.getId() == null) {
            return "";
        }
        String cadena = ancestros.get(combo.getId());
        return (cadena == null || cadena.isEmpty())
                ? String.valueOf(combo.getId())
                : combo.getId() + "\n(" + cadena + ")";
    }

    private List<PreguntaCombo> preguntasOrdenadasPorFactor(Combo combo) {
        if (combo.getPreguntas() == null) {
            return Collections.emptyList();
        }
        List<PreguntaCombo> preguntas = new ArrayList<>(combo.getPreguntas());
        preguntas.sort(Comparator.comparingInt(pc -> pc.getPosicion() != null ? pc.getPosicion() : 999));
        return preguntas;
    }

    private String factorCorto(PreguntaCombo pc) {
        String factor = pc.getFactorMultiplicacion();
        if (factor == null || factor.trim().isEmpty()) {
            return "";
        }
        String digitos = factor.replaceAll("\\D+", "");
        return digitos.isEmpty() ? factor.trim().toUpperCase() : "X" + digitos;
    }

    // ------------------------------------------------------------------- COMPARTIDO

    private int crearCabecera(Sheet sheet, int fila, String[] titulos, Estilos estilos) {
        Row filaCabecera = sheet.createRow(fila);
        filaCabecera.setHeightInPoints(ALTO_CABECERA);
        for (int i = 0; i < titulos.length; i++) {
            escribir(filaCabecera, i, titulos[i], estilos.cabecera);
        }
        return fila + 1;
    }

    /** Fila de cierre: etiqueta combinada a la izquierda y valor combinado a la derecha. */
    private int crearFilaPie(Sheet sheet, int fila, String etiqueta, int ultimaColumnaEtiqueta,
                             int ultimaColumnaValor, float alto, Estilos estilos, String valor,
                             boolean centrarValor) {
        Row filaPie = sheet.createRow(fila);
        filaPie.setHeightInPoints(alto);

        escribir(filaPie, 0, etiqueta, estilos.primeraColumna);
        for (int i = 1; i <= ultimaColumnaEtiqueta; i++) {
            escribir(filaPie, i, "", estilos.primeraColumna);
        }
        combinarConBorde(sheet, fila, 0, ultimaColumnaEtiqueta);

        int primeraColumnaValor = ultimaColumnaEtiqueta + 1;
        CellStyle estiloValor = centrarValor ? estilos.datoCentrado : estilos.datoAjustado;
        escribir(filaPie, primeraColumnaValor, valor != null ? valor : "", estiloValor);
        for (int i = primeraColumnaValor + 1; i <= ultimaColumnaValor; i++) {
            escribir(filaPie, i, "", estiloValor);
        }
        combinarConBorde(sheet, fila, primeraColumnaValor, ultimaColumnaValor);

        return fila + 1;
    }

    private void combinarConBorde(Sheet sheet, int fila, int primeraColumna, int ultimaColumna) {
        CellRangeAddress region = new CellRangeAddress(fila, fila, primeraColumna, ultimaColumna);
        sheet.addMergedRegion(region);
        RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
    }

    private void combinarVerticalConBorde(Sheet sheet, int primeraFila, int ultimaFila, int columna) {
        if (ultimaFila <= primeraFila) {
            return;
        }
        CellRangeAddress region = new CellRangeAddress(primeraFila, ultimaFila, columna, columna);
        sheet.addMergedRegion(region);
        RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
    }

    private void escribir(Row fila, int columna, String valor, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(valor != null ? valor : "");
        celda.setCellStyle(estilo);
    }

    /** La plantilla escribe los niveles en mayúsculas y con NOLS, no NLS: 1LS, 2NOLS, 3LS, 4NOLS. */
    private String nivelCorto(Pregunta pregunta) {
        if (pregunta == null || pregunta.getNivel() == null) {
            return "";
        }
        String nombre = pregunta.getNivel().name().replace("_", "");
        return nombre.replace("NLS", "NOLS");
    }

    private int excelWidth(double width) {
        return (int) Math.round(width * 256);
    }

    private void configurarPagina(Sheet sheet, String tituloCabecera, String nombreJornada) {
        PrintSetup ps = sheet.getPrintSetup();
        ps.setLandscape(true);
        sheet.setFitToPage(true);
        ps.setFitWidth((short) 1);
        ps.setFitHeight((short) 0);
        sheet.setMargin(Sheet.LeftMargin, 0.25);
        sheet.setMargin(Sheet.RightMargin, 0.25);
        sheet.setMargin(Sheet.TopMargin, 0.5);
        sheet.setMargin(Sheet.BottomMargin, 0.5);
        Header header = sheet.getHeader();
        header.setCenter(tituloCabecera);
        Footer footer = sheet.getFooter();
        footer.setCenter(formatearPieJornada(nombreJornada));
        footer.setRight("Página &P de &N");
    }

    /**
     * Pie de página de impresión: "Nombre jornada - LSNLS" en 9 pt y gris.
     * Excel usa códigos en el encabezado/pie: {@code &09} = 9 pt, {@code &K808080} = gris RGB.
     */
    private String formatearPieJornada(String nombreJornada) {
        String nombre = (nombreJornada == null || nombreJornada.trim().isEmpty())
                ? "Jornada"
                : nombreJornada.trim().replace("&", "&&");
        return "&K808080&09" + nombre + " - LSNLS";
    }

    /** Los estilos se crean una vez por libro: Excel tiene un tope de 64.000. */
    private static final class Estilos {
        private final CellStyle cabecera;
        private final CellStyle primeraColumna;
        private final CellStyle primeraColumnaCentrada;
        private final CellStyle valoracionGuionista;
        private final CellStyle datoPlano;
        private final CellStyle datoAjustado;
        private final CellStyle datoCentrado;

        private Estilos(Workbook workbook) {
            this.cabecera = crearCabecera(workbook);
            this.primeraColumna = crearPrimeraColumna(workbook, HorizontalAlignment.LEFT, IndexedColors.WHITE);
            this.primeraColumnaCentrada = crearPrimeraColumna(workbook, HorizontalAlignment.CENTER, IndexedColors.WHITE);
            this.valoracionGuionista = crearPrimeraColumna(workbook, HorizontalAlignment.CENTER, IndexedColors.RED);
            this.datoPlano = crearDato(workbook, false, false);
            this.datoAjustado = crearDato(workbook, true, false);
            this.datoCentrado = crearDato(workbook, true, true);
        }

        private static CellStyle crearCabecera(Workbook workbook) {
            CellStyle estilo = workbook.createCellStyle();
            Font fuente = workbook.createFont();
            fuente.setBold(true);
            fuente.setFontHeightInPoints((short) 10);
            fuente.setColor(IndexedColors.WHITE.getIndex());
            estilo.setFont(fuente);
            estilo.setAlignment(HorizontalAlignment.CENTER);
            estilo.setVerticalAlignment(VerticalAlignment.CENTER);
            estilo.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
            estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            aplicarBordes(estilo);
            return estilo;
        }

        private static CellStyle crearPrimeraColumna(Workbook workbook, HorizontalAlignment alineacion,
                                                     IndexedColors colorTexto) {
            CellStyle estilo = workbook.createCellStyle();
            Font fuente = workbook.createFont();
            fuente.setBold(true);
            fuente.setColor(colorTexto.getIndex());
            estilo.setFont(fuente);
            estilo.setAlignment(alineacion);
            estilo.setVerticalAlignment(VerticalAlignment.CENTER);
            estilo.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
            estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estilo.setWrapText(true);
            aplicarBordes(estilo);
            return estilo;
        }

        private static CellStyle crearDato(Workbook workbook, boolean ajustar, boolean centrar) {
            CellStyle estilo = workbook.createCellStyle();
            estilo.setAlignment(centrar ? HorizontalAlignment.CENTER : HorizontalAlignment.LEFT);
            estilo.setVerticalAlignment(centrar ? VerticalAlignment.CENTER : VerticalAlignment.TOP);
            estilo.setWrapText(ajustar);
            aplicarBordes(estilo);
            return estilo;
        }

        private static void aplicarBordes(CellStyle estilo) {
            estilo.setBorderTop(BorderStyle.THIN);
            estilo.setBorderBottom(BorderStyle.THIN);
            estilo.setBorderLeft(BorderStyle.THIN);
            estilo.setBorderRight(BorderStyle.THIN);
        }
    }
}
