package com.lsnls.service;

import com.lsnls.entity.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelExportService {

    /**
     * Exporta una jornada a Excel con la configuración por defecto.
     */
    public byte[] exportarJornada(Jornada jornada) throws IOException {
        return exportarJornada(jornada, null);
    }
    
    /**
     * Exporta una jornada a Excel con opciones personalizadas.
     * 
     * @param jornada La jornada a exportar
     * @param opciones Opciones de configuración para el Excel:
     *                 - cambiarColumnaID: Cambia el título de la columna "ID PREGUNTA"
     *                 - mostrarFactorMultiplicacion: Si es true, muestra el factor de multiplicación en la columna
     *                 - ordenarCuestionariosPorNivel: Si es true, ordena preguntas de cuestionarios por nivel numérico
     *                 - ordenarCombosPorFactor: Si es true, ordena preguntas de combos por factor de multiplicación
     * @return Bytes del archivo Excel generado
     */
    public byte[] exportarJornada(Jornada jornada, Map<String, Object> opciones) throws IOException {
        // Si las opciones son nulas, usar un mapa vacío para evitar NullPointerException
        if (opciones == null) {
            opciones = new HashMap<>();
        }
        
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            
            // Crear hoja de CUESTIONARIOS
            Sheet hojaCuestionarios = workbook.createSheet("CUESTIONARIOS");
            crearHojaCuestionarios(hojaCuestionarios, jornada, workbook, opciones);
            
            // Crear hoja de COMBOS
            Sheet hojaCombos = workbook.createSheet("COMBOS");
            crearHojaCombos(hojaCombos, jornada, workbook, opciones);
            
            // Convertir a bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void crearHojaCuestionarios(Sheet sheet, Jornada jornada, Workbook workbook) {
        crearHojaCuestionarios(sheet, jornada, workbook, null);
    }
    
    private void crearHojaCuestionarios(Sheet sheet, Jornada jornada, Workbook workbook, Map<String, Object> opciones) {
        int filaActual = 0;
        
        // Si opciones es null, inicializamos un mapa vacío para evitar NullPointerException
        if (opciones == null) {
            opciones = new HashMap<>();
        }
        
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
        for (int i = 0; i < 6; i++) {
            Cuestionario cuestionario = i < cuestionarios.size() ? cuestionarios.get(i) : null;
            filaActual = crearTablaCuestionario(sheet, cuestionario, i + 1, filaActual, workbook, opciones);
            filaActual += 2; // Espacio entre cuestionarios
        }
    }

    private int crearTablaCuestionario(Sheet sheet, Cuestionario cuestionario, int numeroCuestionario, 
                                     int filaInicial, Workbook workbook) {
        return crearTablaCuestionario(sheet, cuestionario, numeroCuestionario, filaInicial, workbook, null);
    }
                                     
    private int crearTablaCuestionario(Sheet sheet, Cuestionario cuestionario, int numeroCuestionario, 
                                     int filaInicial, Workbook workbook, Map<String, Object> opciones) {
        int filaActual = filaInicial;
        
        // Si opciones es null, inicializamos un mapa vacío para evitar NullPointerException
        if (opciones == null) {
            opciones = new HashMap<>();
        }
        
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
        
        // Para cuestionarios, mantenemos los encabezados originales
        
        CellStyle estiloEncabezado = crearEstiloEncabezado(workbook);
        for (int i = 0; i < encabezados.length; i++) {
            Cell celda = filaEncabezados.createCell(i);
            celda.setCellValue(encabezados[i]);
            celda.setCellStyle(estiloEncabezado);
        }
        
        // Datos de las preguntas
        if (cuestionario != null && cuestionario.getPreguntas() != null) {
            List<PreguntaCuestionario> preguntas = cuestionario.getPreguntas().stream().collect(java.util.stream.Collectors.toList());
            
            // Ordenar preguntas por nivel si se solicita
            if (Boolean.TRUE.equals(opciones.get("ordenarCuestionariosPorNivel"))) {
                preguntas.sort((pc1, pc2) -> {
                    Pregunta p1 = pc1.getPregunta();
                    Pregunta p2 = pc2.getPregunta();
                    
                    if (p1 != null && p2 != null) {
                        String nivel1Str = p1.getNivel().name().replace("_", "");
                        String nivel2Str = p2.getNivel().name().replace("_", "");
                        
                        // Extraer solo la parte numérica
                        int nivel1 = 0;
                        int nivel2 = 0;
                        try {
                            nivel1 = Integer.parseInt(nivel1Str.replaceAll("\\D+", ""));
                        } catch (NumberFormatException e) {}
                        try {
                            nivel2 = Integer.parseInt(nivel2Str.replaceAll("\\D+", ""));
                        } catch (NumberFormatException e) {}
                        
                        return Integer.compare(nivel1, nivel2);
                    }
                    return 0;
                });
            }
            
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
        crearHojaCombos(sheet, jornada, workbook, null);
    }
    
    private void crearHojaCombos(Sheet sheet, Jornada jornada, Workbook workbook, Map<String, Object> opciones) {
        int filaActual = 0;
        
        // Si opciones es null, inicializamos un mapa vacío para evitar NullPointerException
        if (opciones == null) {
            opciones = new HashMap<>();
        }
        
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
        for (int i = 0; i < 6; i++) {
            Combo combo = i < combos.size() ? combos.get(i) : null;
            filaActual = crearTablaCombo(sheet, combo, i + 1, filaActual, workbook, opciones);
            filaActual += 2; // Espacio entre combos
        }
    }

    private int crearTablaCombo(Sheet sheet, Combo combo, int numeroCombo, 
                               int filaInicial, Workbook workbook) {
        return crearTablaCombo(sheet, combo, numeroCombo, filaInicial, workbook, null);
    }
                               
    private int crearTablaCombo(Sheet sheet, Combo combo, int numeroCombo, 
                               int filaInicial, Workbook workbook, Map<String, Object> opciones) {
        int filaActual = filaInicial;
        
        // Si opciones es null, inicializamos un mapa vacío para evitar NullPointerException
        if (opciones == null) {
            opciones = new HashMap<>();
        }
        
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
        
        // En combos: cambiar "ID PREGUNTA" por "MULT" y mantener "NIVEL" para la segunda columna
        if (opciones.containsKey("cambiarColumnaID") && opciones.get("cambiarColumnaID") instanceof String) {
            encabezados[0] = (String) opciones.get("cambiarColumnaID");
        }
        
        CellStyle estiloEncabezado = crearEstiloEncabezado(workbook);
        for (int i = 0; i < encabezados.length; i++) {
            Cell celda = filaEncabezados.createCell(i);
            celda.setCellValue(encabezados[i]);
            celda.setCellStyle(estiloEncabezado);
        }
        
        // Datos de las preguntas del combo
        if (combo != null && combo.getPreguntas() != null) {
            List<PreguntaCombo> preguntas = combo.getPreguntas().stream().collect(java.util.stream.Collectors.toList());
            
            // Ordenar preguntas por factor de multiplicación si se solicita
            if (Boolean.TRUE.equals(opciones.get("ordenarCombosPorFactor"))) {
                preguntas.sort((pc1, pc2) -> {
                    int factor1 = 0;
                    int factor2 = 0;
                    
                    try {
                        factor1 = Integer.parseInt(pc1.getFactorMultiplicacion().replaceAll("\\D+", ""));
                    } catch (Exception e) {}
                    try {
                        factor2 = Integer.parseInt(pc2.getFactorMultiplicacion().replaceAll("\\D+", ""));
                    } catch (Exception e) {}
                    
                    return Integer.compare(factor1, factor2);
                });
            }
            
            for (PreguntaCombo pc : preguntas) {
                Row filaPregunta = sheet.createRow(filaActual++);
                Pregunta p = pc.getPregunta();
                
                // Columna 0: ID PREGUNTA o FACTOR
                if (Boolean.TRUE.equals(opciones.get("mostrarFactorMultiplicacion"))) {
                    // Mostrar el factor de multiplicación en lugar del ID
                    String factorStr = pc.getFactorMultiplicacion() != null ? pc.getFactorMultiplicacion() : "";
                    int factorNum = 0;
                    try {
                        factorNum = Integer.parseInt(factorStr.replaceAll("\\D+", ""));
                        factorStr = "x" + factorNum;
                    } catch (Exception e) {
                        if (factorStr.isEmpty()) {
                            factorStr = "x";  // Factor vacío
                        }
                    }
                    filaPregunta.createCell(0).setCellValue(factorStr);
                } else {
                    // Mostrar ID normal
                    filaPregunta.createCell(0).setCellValue(p.getId());
                }
                
                // Columna 1: NIVEL o 5LS/5NLS
                if (Boolean.TRUE.equals(opciones.get("mostrarFactorMultiplicacion"))) {
                    // Mostrar 5LS o 5NLS según el nivel
                    String nivel = p.getNivel() != null ? p.getNivel().name() : "";
                    // Verificar si el nivel contiene "LS" para distinguir entre 5LS y 5NLS
                    String nivelStr;
                    if (nivel.toUpperCase().contains("LS")) {
                        nivelStr = "5LS";
                    } else {
                        nivelStr = "5NLS";
                    }
                    filaPregunta.createCell(1).setCellValue(nivelStr);
                } else {
                    // Mostrar nivel normal
                    filaPregunta.createCell(1).setCellValue(p.getNivel() != null ? p.getNivel().name() : "");
                }
                
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