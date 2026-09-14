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
import java.util.List;
import java.util.Map;

/**
 * Genera el Excel de una jornada calcando la plantilla de producción
 * "JORNADA nnn_LUGAR_fecha_REC.XLSX": una hoja de cuestionarios y otra de
 * combos, con un bloque imprimible por cada hueco ocupado.
 *
 * Las filas de CONCURSANTE, RESULTADO, GRABACIÓN y NOTAS GUION salen vacías a
 * propósito: el Excel se genera antes de grabar, cuando todavía no hay
 * concursante, y se rellenan a mano durante la jornada.
 */
@Service
public class ExcelExportService {

    /** Clave de opciones con los combos de origen por combo derivado: id -> "312/216". */
    public static final String OPCION_ANCESTROS_COMBO = "ancestrosCombo";

    private static final String[] CABECERA_CUESTIONARIOS =
            {"CUEST", "Nº DE PREGUNTA", "NIVEL", "PREGUNTA", "RESPUESTA", "DATOS EXTRA", "REC"};
    private static final double[] ANCHOS_CUESTIONARIOS = {6.9, 0.4, 6.7, 50.6, 19.6, 44.3, 8.4};

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
     * @param opciones Opciones de configuración. Solo se usa
     *                 {@link #OPCION_ANCESTROS_COMBO}, un mapa de id de combo a
     *                 la cadena de combos de los que se recicló.
     * @return Bytes del archivo Excel generado
     */
    public byte[] exportarJornada(Jornada jornada, Map<String, Object> opciones) throws IOException {
        Map<Long, String> ancestros = leerAncestros(opciones);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Estilos estilos = new Estilos(workbook);

            Sheet hojaCuestionarios = workbook.createSheet("CUESTIONARIOS ADJUDICADOS");
            configurarPagina(hojaCuestionarios, "Cuestionarios - " + jornada.getNombre(), jornada.getNombre());
            crearHojaCuestionarios(hojaCuestionarios, jornada, estilos);

            Sheet hojaCombos = workbook.createSheet("COMBOS ADJUDICADOS");
            configurarPagina(hojaCombos, "Combos - " + jornada.getNombre(), jornada.getNombre());
            crearHojaCombos(hojaCombos, jornada, estilos, ancestros);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Long, String> leerAncestros(Map<String, Object> opciones) {
        if (opciones == null) {
            return Collections.emptyMap();
        }
        Object valor = opciones.get(OPCION_ANCESTROS_COMBO);
        return valor instanceof Map ? (Map<Long, String>) valor : Collections.emptyMap();
    }

    // ---------------------------------------------------------------- CUESTIONARIOS

    private void crearHojaCuestionarios(Sheet sheet, Jornada jornada, Estilos estilos) {
        for (int i = 0; i < ANCHOS_CUESTIONARIOS.length; i++) {
            sheet.setColumnWidth(i, excelWidth(ANCHOS_CUESTIONARIOS[i]));
        }

        int filaActual = 0;
        for (Cuestionario cuestionario : jornada.getCuestionariosPorSlot()) {
            if (cuestionario == null) {
                continue;
            }
            filaActual = crearBloqueCuestionario(sheet, cuestionario, filaActual, estilos);
        }
    }

    private int crearBloqueCuestionario(Sheet sheet, Cuestionario cuestionario, int filaInicial, Estilos estilos) {
        int filaActual = crearCabecera(sheet, filaInicial, CABECERA_CUESTIONARIOS, estilos);

        List<PreguntaCuestionario> preguntas = preguntasOrdenadasPorNivel(cuestionario);
        String idCuestionario = cuestionario.getId() != null ? String.valueOf(cuestionario.getId()) : "";

        for (int i = 0; i < FILAS_PREGUNTA_CUESTIONARIO; i++) {
            Row fila = sheet.createRow(filaActual++);
            fila.setHeightInPoints(ALTO_PREGUNTA_CUESTIONARIO);
            // La quinta fila es la de la pregunta multiplicadora, que se rellena a mano.
            PreguntaCuestionario pc = i < Math.min(PREGUNTAS_CUESTIONARIO, preguntas.size()) ? preguntas.get(i) : null;
            Pregunta pregunta = pc != null ? pc.getPregunta() : null;

            escribir(fila, 0, pc != null ? idCuestionario : "", estilos.primeraColumna);
            escribir(fila, 1, "", estilos.datoPlano);
            escribir(fila, 2, nivelCorto(pregunta), estilos.datoPlano);
            escribir(fila, 3, pregunta != null ? pregunta.getPregunta() : "", estilos.datoAjustado);
            escribir(fila, 4, pregunta != null ? pregunta.getRespuesta() : "", estilos.datoAjustado);
            escribir(fila, 5, pregunta != null && pregunta.getDatosExtra() != null ? pregunta.getDatosExtra() : "",
                    estilos.datoAjustado);
            escribir(fila, 6, "", estilos.datoPlano);
        }

        filaActual = crearFilaPie(sheet, filaActual, "CONCURSANTE", 2, 6, ALTO_PIE_CUESTIONARIO, estilos);
        filaActual = crearFilaPie(sheet, filaActual, "RESULTADO", 2, 6, ALTO_PIE_CUESTIONARIO, estilos);
        filaActual = crearFilaPie(sheet, filaActual, "GRABACIÓN", 2, 6, ALTO_PIE_CUESTIONARIO, estilos);
        filaActual = crearFilaPie(sheet, filaActual, "NOTAS GUION", 2, 6, ALTO_NOTAS_CUESTIONARIO, estilos);

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

    private void crearHojaCombos(Sheet sheet, Jornada jornada, Estilos estilos, Map<Long, String> ancestros) {
        for (int i = 0; i < ANCHOS_COMBOS.length; i++) {
            sheet.setColumnWidth(i, excelWidth(ANCHOS_COMBOS[i]));
        }

        int filaActual = 0;
        for (Combo combo : jornada.getCombosPorSlot()) {
            if (combo == null) {
                continue;
            }
            filaActual = crearBloqueCombo(sheet, combo, filaActual, estilos, ancestros);
        }
    }

    private int crearBloqueCombo(Sheet sheet, Combo combo, int filaInicial, Estilos estilos,
                                 Map<Long, String> ancestros) {
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

        filaActual = crearFilaPie(sheet, filaActual, "CONCURSANTE", 4, 8, ALTO_PIE_COMBO, estilos);
        filaActual = crearFilaPie(sheet, filaActual, "RESULTADO", 4, 8, ALTO_PIE_COMBO, estilos);
        filaActual = crearFilaPie(sheet, filaActual, "GRABACIÓN", 4, 8, ALTO_GRABACION_COMBO, estilos);
        filaActual = crearFilaPie(sheet, filaActual, "NOTAS GUION", 4, 8, ALTO_NOTAS_COMBO, estilos);

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

    /** Fila de cierre: etiqueta combinada a la izquierda y hueco combinado a la derecha. */
    private int crearFilaPie(Sheet sheet, int fila, String etiqueta, int ultimaColumnaEtiqueta,
                             int ultimaColumnaValor, float alto, Estilos estilos) {
        Row filaPie = sheet.createRow(fila);
        filaPie.setHeightInPoints(alto);

        escribir(filaPie, 0, etiqueta, estilos.primeraColumna);
        for (int i = 1; i <= ultimaColumnaEtiqueta; i++) {
            escribir(filaPie, i, "", estilos.primeraColumna);
        }
        combinarConBorde(sheet, fila, 0, ultimaColumnaEtiqueta);

        int primeraColumnaValor = ultimaColumnaEtiqueta + 1;
        for (int i = primeraColumnaValor; i <= ultimaColumnaValor; i++) {
            escribir(filaPie, i, "", estilos.datoAjustado);
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
        private final CellStyle datoPlano;
        private final CellStyle datoAjustado;

        private Estilos(Workbook workbook) {
            this.cabecera = crearCabecera(workbook);
            this.primeraColumna = crearPrimeraColumna(workbook);
            this.datoPlano = crearDato(workbook, false);
            this.datoAjustado = crearDato(workbook, true);
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

        private static CellStyle crearPrimeraColumna(Workbook workbook) {
            CellStyle estilo = workbook.createCellStyle();
            Font fuente = workbook.createFont();
            fuente.setBold(true);
            fuente.setColor(IndexedColors.WHITE.getIndex());
            estilo.setFont(fuente);
            estilo.setAlignment(HorizontalAlignment.LEFT);
            estilo.setVerticalAlignment(VerticalAlignment.CENTER);
            estilo.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
            estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estilo.setWrapText(true);
            aplicarBordes(estilo);
            return estilo;
        }

        private static CellStyle crearDato(Workbook workbook, boolean ajustar) {
            CellStyle estilo = workbook.createCellStyle();
            estilo.setAlignment(HorizontalAlignment.LEFT);
            estilo.setVerticalAlignment(VerticalAlignment.TOP);
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
