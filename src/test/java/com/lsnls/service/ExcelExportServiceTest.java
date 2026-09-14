package com.lsnls.service;

import com.lsnls.entity.Combo;
import com.lsnls.entity.Cuestionario;
import com.lsnls.entity.Jornada;
import com.lsnls.entity.Pregunta;
import com.lsnls.entity.PreguntaCombo;
import com.lsnls.entity.PreguntaCuestionario;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelExportServiceTest {

    private final ExcelExportService excelExportService = new ExcelExportService();

    @Test
    void exportarJornadaVacia_generaLasDosHojasSinBloques() throws Exception {
        Jornada jornada = new Jornada();
        jornada.setNombre("Jornada test");
        jornada.setFechaJornada(LocalDate.of(2026, 8, 24));
        jornada.setCuestionarios(new HashSet<>());
        jornada.setCombos(new HashSet<>());

        byte[] bytes = excelExportService.exportarJornada(jornada);

        assertTrue(bytes.length > 0);
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertEquals(2, wb.getNumberOfSheets());
            assertEquals("CUESTIONARIOS ADJUDICADOS", wb.getSheetAt(0).getSheetName());
            assertEquals("COMBOS ADJUDICADOS", wb.getSheetAt(1).getSheetName());
            // Sin huecos ocupados no se pinta ningún bloque
            assertNull(wb.getSheetAt(0).getRow(0));
            assertNull(wb.getSheetAt(1).getRow(0));
            String pie = wb.getSheetAt(0).getFooter().getCenter();
            assertTrue(pie.contains("Jornada test - LSNLS"));
            assertTrue(pie.contains("&09"));
            assertTrue(pie.contains("&K808080"));
            assertTrue(wb.getSheetAt(1).getFooter().getCenter().contains("Jornada test - LSNLS"));
        }
    }

    @Test
    void hojaCuestionarios_sigueElBloqueDeLaPlantilla() throws Exception {
        Jornada jornada = new Jornada();
        jornada.setNombre("Grabación");
        jornada.setFechaJornada(LocalDate.of(2026, 1, 15));
        jornada.reemplazarCuestionariosPorSlot(Arrays.asList(
                cuestionarioConPreguntas(101L), null, null, null, null, null));
        jornada.reemplazarCombosPorSlot(Arrays.asList(new Combo[6]));

        byte[] bytes = excelExportService.exportarJornada(jornada, null);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet hoja = wb.getSheetAt(0);
            assertEquals("CUEST", hoja.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Nº DE PREGUNTA", hoja.getRow(0).getCell(1).getStringCellValue());
            assertEquals("REC", hoja.getRow(0).getCell(6).getStringCellValue());

            // Cuatro preguntas ordenadas por nivel y quinta fila libre para la multiplicadora
            assertEquals("1LS", hoja.getRow(1).getCell(2).getStringCellValue());
            assertEquals("2NOLS", hoja.getRow(2).getCell(2).getStringCellValue());
            assertEquals("3LS", hoja.getRow(3).getCell(2).getStringCellValue());
            assertEquals("4NOLS", hoja.getRow(4).getCell(2).getStringCellValue());
            assertEquals("101", hoja.getRow(1).getCell(0).getStringCellValue());
            assertEquals("", hoja.getRow(5).getCell(0).getStringCellValue());
            assertEquals("", hoja.getRow(5).getCell(3).getStringCellValue());

            // Pie del bloque, vacío para rellenar en grabación
            assertEquals("CONCURSANTE", hoja.getRow(6).getCell(0).getStringCellValue());
            assertEquals("RESULTADO", hoja.getRow(7).getCell(0).getStringCellValue());
            assertEquals("GRABACIÓN", hoja.getRow(8).getCell(0).getStringCellValue());
            assertEquals("NOTAS GUION", hoja.getRow(9).getCell(0).getStringCellValue());
            assertEquals("", hoja.getRow(6).getCell(3).getStringCellValue());
            assertNull(hoja.getRow(10));

            // Etiqueta combinada en A:C y hueco en D:G
            assertTrue(hoja.getMergedRegions().stream()
                    .anyMatch(r -> r.getFirstRow() == 6 && r.getFirstColumn() == 0 && r.getLastColumn() == 2));
            assertTrue(hoja.getMergedRegions().stream()
                    .anyMatch(r -> r.getFirstRow() == 6 && r.getFirstColumn() == 3 && r.getLastColumn() == 6));
        }
    }

    @Test
    void hojaCombos_sigueElBloqueDeLaPlantilla() throws Exception {
        Jornada jornada = new Jornada();
        jornada.setNombre("Grabación");
        jornada.reemplazarCuestionariosPorSlot(Arrays.asList(new Cuestionario[6]));
        jornada.reemplazarCombosPorSlot(Arrays.asList(
                comboConPreguntas(201L), null, null, null, null, null));

        byte[] bytes = excelExportService.exportarJornada(jornada, null);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet hoja = wb.getSheetAt(1);
            assertEquals("COMBO", hoja.getRow(0).getCell(0).getStringCellValue());
            assertEquals("FAC", hoja.getRow(0).getCell(3).getStringCellValue());
            assertEquals("REC", hoja.getRow(0).getCell(8).getStringCellValue());

            assertEquals("201", hoja.getRow(1).getCell(0).getStringCellValue());
            assertEquals("P", hoja.getRow(1).getCell(2).getStringCellValue());
            assertEquals("X2", hoja.getRow(1).getCell(3).getStringCellValue());
            assertEquals("X3", hoja.getRow(2).getCell(3).getStringCellValue());
            assertEquals("5LS", hoja.getRow(1).getCell(4).getStringCellValue());

            assertEquals("CONCURSANTE", hoja.getRow(4).getCell(0).getStringCellValue());
            assertEquals("NOTAS GUION", hoja.getRow(7).getCell(0).getStringCellValue());
            assertNull(hoja.getRow(8));

            assertTrue(hoja.getMergedRegions().stream()
                    .anyMatch(r -> r.getFirstRow() == 4 && r.getFirstColumn() == 0 && r.getLastColumn() == 4));
            assertTrue(hoja.getMergedRegions().stream()
                    .anyMatch(r -> r.getFirstRow() == 4 && r.getFirstColumn() == 5 && r.getLastColumn() == 8));
        }
    }

    @Test
    void comboReciclado_muestraLosCombosDeOrigenBajoElId() throws Exception {
        Jornada jornada = new Jornada();
        jornada.setNombre("Grabación");
        jornada.reemplazarCuestionariosPorSlot(Arrays.asList(new Cuestionario[6]));
        jornada.reemplazarCombosPorSlot(Arrays.asList(
                comboConPreguntas(331L), null, null, null, null, null));

        Map<String, Object> opciones = new HashMap<>();
        opciones.put(ExcelExportService.OPCION_ANCESTROS_COMBO,
                Collections.singletonMap(331L, "312/216"));

        byte[] bytes = excelExportService.exportarJornada(jornada, opciones);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertEquals("331\n(312/216)", wb.getSheetAt(1).getRow(1).getCell(0).getStringCellValue());
        }
    }

    @Test
    void soloSePintanLosHuecosOcupados() throws Exception {
        Jornada jornada = new Jornada();
        jornada.setNombre("Set Madrid");
        jornada.setFechaJornada(LocalDate.of(2026, 8, 24));

        Cuestionario[] cuestionarios = new Cuestionario[6];
        Combo[] combos = new Combo[6];
        for (int i = 0; i < 5; i++) {
            cuestionarios[i] = cuestionarioConPreguntas(101L + i);
            combos[i] = comboConPreguntas(201L + i);
        }
        jornada.reemplazarCuestionariosPorSlot(Arrays.asList(cuestionarios));
        jornada.reemplazarCombosPorSlot(Arrays.asList(combos));

        byte[] bytes = excelExportService.exportarJornada(jornada, null);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet hojaCuest = wb.getSheetAt(0);
            Sheet hojaCombos = wb.getSheetAt(1);
            // Cinco bloques: 10 filas por cuestionario y 8 por combo de tres preguntas
            assertEquals(50, hojaCuest.getLastRowNum() + 1);
            assertEquals(40, hojaCombos.getLastRowNum() + 1);

            String cuestionariosTxt = textoHoja(hojaCuest);
            String combosTxt = textoHoja(hojaCombos);
            for (int i = 0; i < 5; i++) {
                assertTrue(cuestionariosTxt.contains(String.valueOf(101 + i)),
                        "Falta el cuestionario " + (101 + i) + " en Excel");
                assertTrue(combosTxt.contains(String.valueOf(201 + i)),
                        "Falta el combo " + (201 + i) + " en Excel");
            }
            assertTrue(cuestionariosTxt.contains("Pregunta 1LS"));
            assertTrue(combosTxt.contains("Pregunta PM"));
            assertTrue(wb.getSheetAt(0).getFooter().getCenter().contains("Set Madrid - LSNLS"));
        }
    }

    private static Cuestionario cuestionarioConPreguntas(long id) {
        Cuestionario c = new Cuestionario();
        c.setId(id);
        c.setNivel(Cuestionario.NivelCuestionario.NORMAL);
        c.setEstado(Cuestionario.EstadoCuestionario.adjudicado);
        Set<PreguntaCuestionario> preguntas = new HashSet<>();
        Pregunta.NivelPregunta[] niveles = {
                Pregunta.NivelPregunta._1LS,
                Pregunta.NivelPregunta._2NLS,
                Pregunta.NivelPregunta._3LS,
                Pregunta.NivelPregunta._4NLS
        };
        for (int i = 0; i < niveles.length; i++) {
            Pregunta p = new Pregunta();
            p.setId(id * 10 + i);
            p.setNivel(niveles[i]);
            p.setPregunta("Pregunta " + niveles[i].name().replace("_", "") + " del " + id);
            p.setRespuesta("R" + i);
            p.setDatosExtra("extra");
            PreguntaCuestionario pc = new PreguntaCuestionario();
            PreguntaCuestionario.PreguntaCuestionarioId clave = new PreguntaCuestionario.PreguntaCuestionarioId();
            clave.setCuestionarioId(id);
            clave.setPreguntaId(p.getId());
            pc.setId(clave);
            pc.setCuestionario(c);
            pc.setPregunta(p);
            preguntas.add(pc);
        }
        c.setPreguntas(preguntas);
        return c;
    }

    private static Combo comboConPreguntas(long id) {
        Combo combo = new Combo();
        combo.setId(id);
        combo.setNivel(Combo.NivelCombo._5LS);
        combo.setTipo(Combo.TipoCombo.P);
        combo.setTematica("Cine");
        combo.setEstado(Combo.EstadoCombo.adjudicado);
        Set<PreguntaCombo> preguntas = new HashSet<>();
        String[] factores = {"2", "3", "X"};
        for (int i = 0; i < 3; i++) {
            Pregunta p = new Pregunta();
            p.setId(id * 10 + i);
            p.setNivel(Pregunta.NivelPregunta._5LS);
            p.setPregunta("Pregunta PM" + (i + 1) + " del " + id);
            p.setRespuesta("R" + i);
            p.setDatosExtra("extra");
            PreguntaCombo pc = new PreguntaCombo();
            PreguntaCombo.PreguntaComboId clave = new PreguntaCombo.PreguntaComboId();
            clave.setComboId(id);
            clave.setPreguntaId(p.getId());
            pc.setId(clave);
            pc.setCombo(combo);
            pc.setPregunta(p);
            pc.setFactorMultiplicacion(factores[i]);
            pc.setPosicion(i + 1);
            preguntas.add(pc);
        }
        combo.setPreguntas(preguntas);
        return combo;
    }

    private static String textoHoja(Sheet sheet) {
        StringBuilder sb = new StringBuilder();
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell == null) {
                    continue;
                }
                if (cell.getCellType() == CellType.STRING) {
                    sb.append(cell.getStringCellValue()).append('\n');
                } else if (cell.getCellType() == CellType.NUMERIC) {
                    sb.append((long) cell.getNumericCellValue()).append('\n');
                }
            }
        }
        return sb.toString();
    }
}
