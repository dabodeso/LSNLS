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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelExportServiceTest {

    private final ExcelExportService excelExportService = new ExcelExportService();

    @Test
    void exportarJornadaVacia_generaHojasCuestionariosYCombos() throws Exception {
        Jornada jornada = new Jornada();
        jornada.setNombre("Jornada test");
        jornada.setFechaJornada(LocalDate.of(2026, 8, 24));
        jornada.setCuestionarios(new HashSet<>());
        jornada.setCombos(new HashSet<>());

        byte[] bytes = excelExportService.exportarJornada(jornada);

        assertTrue(bytes.length > 0);
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertEquals(2, wb.getNumberOfSheets());
            assertEquals("CUESTIONARIOS", wb.getSheetAt(0).getSheetName());
            assertEquals("COMBOS", wb.getSheetAt(1).getSheetName());
            assertTrue(wb.getSheetAt(1).getRow(0).getCell(0).getStringCellValue().contains("Jornada test"));
            String pie = wb.getSheetAt(0).getFooter().getCenter();
            assertTrue(pie.contains("Jornada test - LSNLS"));
            assertTrue(pie.contains("&09"));
            assertTrue(pie.contains("&K808080"));
            assertTrue(wb.getSheetAt(1).getFooter().getCenter().contains("Jornada test - LSNLS"));
        }
    }

    @Test
    void exportarJornada_conUnCuestionarioYUnCombo() throws Exception {
        Jornada jornada = new Jornada();
        jornada.setNombre("Grabación");
        jornada.setFechaJornada(LocalDate.of(2026, 1, 15));
        Cuestionario cuestionario = new Cuestionario();
        cuestionario.setId(7L);
        cuestionario.setPreguntas(new HashSet<>());
        Combo combo = new Combo();
        combo.setId(3L);
        combo.setPreguntas(new HashSet<>());
        HashSet<Cuestionario> cuestionarios = new HashSet<>();
        cuestionarios.add(cuestionario);
        HashSet<Combo> combos = new HashSet<>();
        combos.add(combo);
        jornada.setCuestionarios(cuestionarios);
        jornada.setCombos(combos);

        byte[] bytes = excelExportService.exportarJornada(jornada, null);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertEquals(2, wb.getNumberOfSheets());
            String tituloCombo = wb.getSheetAt(1).getRow(2).getCell(0).getStringCellValue();
            assertTrue(tituloCombo.contains("ID: 3") || tituloCombo.contains("COMBO"));
        }
    }

    @Test
    void exportarJornada_cincoCuestionariosYCincoCombos() throws Exception {
        Jornada jornada = new Jornada();
        jornada.setNombre("Set Madrid");
        jornada.setFechaJornada(LocalDate.of(2026, 8, 24));

        Cuestionario[] cuestionarios = new Cuestionario[6];
        Combo[] combos = new Combo[6];
        for (int i = 0; i < 5; i++) {
            long id = 101L + i;
            cuestionarios[i] = cuestionarioConPreguntas(id);
            combos[i] = comboConPreguntas(201L + i);
        }
        jornada.reemplazarCuestionariosPorSlot(Arrays.asList(cuestionarios));
        jornada.reemplazarCombosPorSlot(Arrays.asList(combos));

        Map<String, Object> opciones = new HashMap<>();
        opciones.put("ordenarCuestionariosPorNivel", true);
        opciones.put("ordenarCombosPorFactor", true);
        opciones.put("mostrarFactorMultiplicacion", true);

        byte[] bytes = excelExportService.exportarJornada(jornada, opciones);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            String cuestionariosTxt = textoHoja(wb.getSheetAt(0));
            String combosTxt = textoHoja(wb.getSheetAt(1));
            for (int i = 0; i < 5; i++) {
                assertTrue(cuestionariosTxt.contains(String.valueOf(101 + i)),
                        "Falta el cuestionario " + (101 + i) + " en Excel");
                assertTrue(combosTxt.contains("ID: " + (201 + i)),
                        "Falta el combo " + (201 + i) + " en Excel");
            }
            assertTrue(combosTxt.contains("COMBO 6 (VACÍO)"));
            assertTrue(combosTxt.contains("Set Madrid"));
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
