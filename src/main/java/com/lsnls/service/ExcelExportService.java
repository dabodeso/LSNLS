package com.lsnls.service;

import com.lsnls.entity.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelExportService {

    public byte[] exportarJornada(Jornada jornada) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            
            // Crear hoja de CUESTIONARIOS
            Sheet hojaCuestionarios = workbook.createSheet("CUESTIONARIOS");
            crearHojaCuestionarios(hojaCuestionarios, jornada, workbook);
            
            // Crear hoja de COMBOS
            Sheet hojaCombos = workbook.createSheet("COMBOS");
            crearHojaCombos(hojaCombos, jornada, workbook);
            
            // Convertir a bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void crearHojaCuestionarios(Sheet sheet, Jornada jornada, Workbook workbook) {
        int filaActual = 0;
        
        // Configurar anchos de columna - hacer más anchas las importantes
        sheet.setColumnWidth(0, 4000);  // ID PREGUNTA (más ancha para evitar cortes)
        sheet.setColumnWidth(1, 2500);  // NIVEL  
        sheet.setColumnWidth(2, 12000); // PREGUNTA (más ancha)
        sheet.setColumnWidth(3, 8000);  // RESPUESTA (más ancha)
        sheet.setColumnWidth(4, 6000);  // DATOS EXTRA (más ancha)
        sheet.setColumnWidth(5, 2000);  // REC
        
        // Título de la jornada
        Row filaTitulo = sheet.createRow(filaActual++);
        Cell celdaTitulo = filaTitulo.createCell(0);
        celdaTitulo.setCellValue("JORNADA: " + jornada.getNombre() + " - " + jornada.getFechaJornada());
        CellStyle estiloTitulo = crearEstiloTitulo(workbook);
        celdaTitulo.setCellStyle(estiloTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        
        filaActual++; // Fila en blanco
        
        // Procesar cada cuestionario
        List<Cuestionario> cuestionarios = jornada.getCuestionarios().stream().collect(java.util.stream.Collectors.toList());
        for (int i = 0; i < 5; i++) {
            Cuestionario cuestionario = i < cuestionarios.size() ? cuestionarios.get(i) : null;
            filaActual = crearTablaCuestionario(sheet, cuestionario, i + 1, filaActual, workbook);
            filaActual += 2; // Espacio entre cuestionarios
        }
    }

    private int crearTablaCuestionario(Sheet sheet, Cuestionario cuestionario, int numeroCuestionario, 
                                     int filaInicial, Workbook workbook) {
        int filaActual = filaInicial;
        
        // Título del cuestionario
        Row filaTituloCuest = sheet.createRow(filaActual++);
        Cell celdaTituloCuest = filaTituloCuest.createCell(0);
        String titulo = cuestionario != null ? 
            "CUESTIONARIO " + numeroCuestionario + " (ID: " + cuestionario.getId() + ")" :
            "CUESTIONARIO " + numeroCuestionario + " (VACÍO)";
        celdaTituloCuest.setCellValue(titulo);
        CellStyle estiloSubtitulo = crearEstiloSubtitulo(workbook);
        celdaTituloCuest.setCellStyle(estiloSubtitulo);
        sheet.addMergedRegion(new CellRangeAddress(filaActual-1, filaActual-1, 0, 5));
        
        // Encabezados de tabla
        Row filaEncabezados = sheet.createRow(filaActual++);
        String[] encabezados = {"ID PREGUNTA", "NIVEL", "PREGUNTA", "RESPUESTA", "DATOS EXTRA", "REC"};
        CellStyle estiloEncabezado = crearEstiloEncabezado(workbook);
        for (int i = 0; i < encabezados.length; i++) {
            Cell celda = filaEncabezados.createCell(i);
            celda.setCellValue(encabezados[i]);
            celda.setCellStyle(estiloEncabezado);
        }
        
        // Datos de las preguntas
        if (cuestionario != null && cuestionario.getPreguntas() != null) {
            List<PreguntaCuestionario> preguntas = cuestionario.getPreguntas().stream().collect(java.util.stream.Collectors.toList());
            for (PreguntaCuestionario pc : preguntas) {
                Row filaPregunta = sheet.createRow(filaActual++);
                Pregunta p = pc.getPregunta();
                
                filaPregunta.createCell(0).setCellValue(p.getId());
                filaPregunta.createCell(1).setCellValue(p.getNivel().name());
                filaPregunta.createCell(2).setCellValue(p.getPregunta());
                filaPregunta.createCell(3).setCellValue(p.getRespuesta());
                filaPregunta.createCell(4).setCellValue(p.getDatosExtra() != null ? p.getDatosExtra() : "");
                filaPregunta.createCell(5).setCellValue(""); // Campo REC editable
            }
        } else {
            // Cuestionario vacío - crear filas en blanco
            for (int i = 0; i < 4; i++) {
                Row filaVacia = sheet.createRow(filaActual++);
                for (int j = 0; j < 6; j++) {
                    filaVacia.createCell(j).setCellValue("");
                }
            }
        }
        
        // Campos adicionales debajo del cuestionario - organizados verticalmente
        filaActual++; // Fila en blanco
        
        // CONCURSANTE
        Row filaConcursante = sheet.createRow(filaActual++);
        filaConcursante.createCell(0).setCellValue("CONCURSANTE:");
        Cell celdaConcursante = filaConcursante.createCell(1);
        celdaConcursante.setCellValue(""); // Campo editable que se extiende
        sheet.addMergedRegion(new CellRangeAddress(filaActual-1, filaActual-1, 1, 5));
        
        // RESULTADO  
        Row filaResultado = sheet.createRow(filaActual++);
        filaResultado.createCell(0).setCellValue("RESULTADO:");
        Cell celdaResultado = filaResultado.createCell(1);
        celdaResultado.setCellValue(""); // Campo editable que se extiende
        sheet.addMergedRegion(new CellRangeAddress(filaActual-1, filaActual-1, 1, 5));
        
        // GRABACIÓN
        Row filaGrabacion = sheet.createRow(filaActual++);
        filaGrabacion.createCell(0).setCellValue("GRABACIÓN:");
        Cell celdaGrabacion = filaGrabacion.createCell(1);
        celdaGrabacion.setCellValue(""); // Campo editable que se extiende
        sheet.addMergedRegion(new CellRangeAddress(filaActual-1, filaActual-1, 1, 5));
        
        // NOTAS GUIÓN
        Row filaNotasGuion = sheet.createRow(filaActual++);
        filaNotasGuion.createCell(0).setCellValue("NOTAS GUIÓN:");
        Cell celdaNotasGuion = filaNotasGuion.createCell(1);
        celdaNotasGuion.setCellValue(""); // Campo editable que se extiende
        sheet.addMergedRegion(new CellRangeAddress(filaActual-1, filaActual-1, 1, 5));
        
        return filaActual;
    }

    private void crearHojaCombos(Sheet sheet, Jornada jornada, Workbook workbook) {
        int filaActual = 0;
        
        // Configurar anchos de columna - hacer más anchas las importantes
        sheet.setColumnWidth(0, 4000);  // ID PREGUNTA (más ancha para evitar cortes)
        sheet.setColumnWidth(1, 2500);  // NIVEL  
        sheet.setColumnWidth(2, 12000); // PREGUNTA (más ancha)
        sheet.setColumnWidth(3, 8000);  // RESPUESTA (más ancha)
        sheet.setColumnWidth(4, 6000);  // DATOS EXTRA (más ancha)
        sheet.setColumnWidth(5, 2000);  // REC
        
        // Título de la jornada
        Row filaTitulo = sheet.createRow(filaActual++);
        Cell celdaTitulo = filaTitulo.createCell(0);
        celdaTitulo.setCellValue("COMBOS - JORNADA: " + jornada.getNombre() + " - " + jornada.getFechaJornada());
        CellStyle estiloTitulo = crearEstiloTitulo(workbook);
        celdaTitulo.setCellStyle(estiloTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        
        filaActual++; // Fila en blanco
        
        // Procesar cada combo
        List<Combo> combos = jornada.getCombos().stream().collect(java.util.stream.Collectors.toList());
        for (int i = 0; i < 5; i++) {
            Combo combo = i < combos.size() ? combos.get(i) : null;
            filaActual = crearTablaCombo(sheet, combo, i + 1, filaActual, workbook);
            filaActual += 2; // Espacio entre combos
        }
    }

    private int crearTablaCombo(Sheet sheet, Combo combo, int numeroCombo, 
                               int filaInicial, Workbook workbook) {
        int filaActual = filaInicial;
        
        // Título del combo
        Row filaTituloCombo = sheet.createRow(filaActual++);
        Cell celdaTituloCombo = filaTituloCombo.createCell(0);
        String titulo = combo != null ? 
            "COMBO " + numeroCombo + " (ID: " + combo.getId() + ")" :
            "COMBO " + numeroCombo + " (VACÍO)";
        celdaTituloCombo.setCellValue(titulo);
        CellStyle estiloSubtitulo = crearEstiloSubtitulo(workbook);
        celdaTituloCombo.setCellStyle(estiloSubtitulo);
        sheet.addMergedRegion(new CellRangeAddress(filaActual-1, filaActual-1, 0, 5));
        
        // Encabezados de tabla
        Row filaEncabezados = sheet.createRow(filaActual++);
        String[] encabezados = {"ID PREGUNTA", "NIVEL", "PREGUNTA", "RESPUESTA", "DATOS EXTRA", "REC"};
        CellStyle estiloEncabezado = crearEstiloEncabezado(workbook);
        for (int i = 0; i < encabezados.length; i++) {
            Cell celda = filaEncabezados.createCell(i);
            celda.setCellValue(encabezados[i]);
            celda.setCellStyle(estiloEncabezado);
        }
        
        // Datos de las preguntas del combo
        if (combo != null && combo.getPreguntas() != null) {
            List<PreguntaCombo> preguntas = combo.getPreguntas().stream().collect(java.util.stream.Collectors.toList());
            for (PreguntaCombo pc : preguntas) {
                Row filaPregunta = sheet.createRow(filaActual++);
                Pregunta p = pc.getPregunta();
                
                filaPregunta.createCell(0).setCellValue(p.getId());
                filaPregunta.createCell(1).setCellValue(p.getNivel().name());
                filaPregunta.createCell(2).setCellValue(p.getPregunta());
                filaPregunta.createCell(3).setCellValue(p.getRespuesta());
                filaPregunta.createCell(4).setCellValue(p.getDatosExtra() != null ? p.getDatosExtra() : "");
                filaPregunta.createCell(5).setCellValue(""); // Campo REC editable
            }
        } else {
            // Combo vacío - crear filas en blanco
            for (int i = 0; i < 3; i++) {
                Row filaVacia = sheet.createRow(filaActual++);
                for (int j = 0; j < 6; j++) {
                    filaVacia.createCell(j).setCellValue("");
                }
            }
        }
        
        // Campos adicionales debajo del combo - organizados verticalmente
        filaActual++; // Fila en blanco
        
        // CONCURSANTE
        Row filaConcursante = sheet.createRow(filaActual++);
        filaConcursante.createCell(0).setCellValue("CONCURSANTE:");
        Cell celdaConcursante = filaConcursante.createCell(1);
        celdaConcursante.setCellValue(""); // Campo editable que se extiende
        sheet.addMergedRegion(new CellRangeAddress(filaActual-1, filaActual-1, 1, 5));
        
        // RESULTADO  
        Row filaResultado = sheet.createRow(filaActual++);
        filaResultado.createCell(0).setCellValue("RESULTADO:");
        Cell celdaResultado = filaResultado.createCell(1);
        celdaResultado.setCellValue(""); // Campo editable que se extiende
        sheet.addMergedRegion(new CellRangeAddress(filaActual-1, filaActual-1, 1, 5));
        
        // GRABACIÓN
        Row filaGrabacion = sheet.createRow(filaActual++);
        filaGrabacion.createCell(0).setCellValue("GRABACIÓN:");
        Cell celdaGrabacion = filaGrabacion.createCell(1);
        celdaGrabacion.setCellValue(""); // Campo editable que se extiende
        sheet.addMergedRegion(new CellRangeAddress(filaActual-1, filaActual-1, 1, 5));
        
        // NOTAS GUIÓN
        Row filaNotasGuion = sheet.createRow(filaActual++);
        filaNotasGuion.createCell(0).setCellValue("NOTAS GUIÓN:");
        Cell celdaNotasGuion = filaNotasGuion.createCell(1);
        celdaNotasGuion.setCellValue(""); // Campo editable que se extiende
        sheet.addMergedRegion(new CellRangeAddress(filaActual-1, filaActual-1, 1, 5));
        
        return filaActual;
    }

    private CellStyle crearEstiloTitulo(Workbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        Font fuente = workbook.createFont();
        fuente.setBold(true);
        fuente.setFontHeightInPoints((short) 16);
        estilo.setFont(fuente);
        estilo.setAlignment(HorizontalAlignment.CENTER);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        estilo.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    private CellStyle crearEstiloSubtitulo(Workbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        Font fuente = workbook.createFont();
        fuente.setBold(true);
        fuente.setFontHeightInPoints((short) 12);
        estilo.setFont(fuente);
        estilo.setAlignment(HorizontalAlignment.LEFT);
        estilo.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    private CellStyle crearEstiloEncabezado(Workbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        Font fuente = workbook.createFont();
        fuente.setBold(true);
        fuente.setFontHeightInPoints((short) 10);
        estilo.setFont(fuente);
        estilo.setAlignment(HorizontalAlignment.CENTER);
        estilo.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        return estilo;
    }
} 