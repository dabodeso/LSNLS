package com.lsnls.service;

import com.lsnls.entity.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;

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
            configurarPagina(hojaCuestionarios, "Cuestionarios - " + jornada.getNombre());
            crearHojaCuestionarios(hojaCuestionarios, jornada, workbook, opciones);
            
            // Crear hoja de COMBOS
            Sheet hojaCombos = workbook.createSheet("COMBOS");
            configurarPagina(hojaCombos, "Combos - " + jornada.getNombre());
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
        
        // Configurar anchos de columna
		// Ajustes: devolver A como antes (4000) y B a tamaño normal (~2500),
		// mantener C con -20% y D a la mitad como acordado
		final int anchoA = 4000;
		final int anchoB = 2500;
		final int anchoC = (int) (22000 * 0.8); // 17600
		final int anchoD = 16000 / 2;          // 8000

		sheet.setColumnWidth(0, anchoA);  // ID PREGUNTA
		sheet.setColumnWidth(1, anchoB);  // NIVEL (1ls, 2nls, ...)
		sheet.setColumnWidth(2, anchoC);  // PREGUNTA
		sheet.setColumnWidth(3, anchoD);  // RESPUESTA
		sheet.setColumnWidth(4, 10000); // DATOS EXTRA
		sheet.setColumnWidth(5, 2000);  // REC (estrecha para marcar X)
        
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
            
            // Estilos de datos
            CellStyle estiloDatoWrap = crearEstiloDato(workbook, true);
            CellStyle estiloDatoPlano = crearEstiloDato(workbook, false);

			// Limitar a 4 preguntas por cuestionario
			int limite = Math.min(4, preguntas.size());
			for (int idx = 0; idx < limite; idx++) {
				PreguntaCuestionario pc = preguntas.get(idx);
                Row filaPregunta = sheet.createRow(filaActual++);
                Pregunta p = pc.getPregunta();

				// Columna 0: ID PREGUNTA
				Cell c0 = filaPregunta.createCell(0);
				if (p != null && p.getId() != null) {
					c0.setCellValue(p.getId());
				} else {
					c0.setCellValue("");
				}
				c0.setCellStyle(estiloDatoPlano);

                Cell c1 = filaPregunta.createCell(1);
				// Nivel en formato corto: "1ls", "2nls", etc. (en minúsculas)
				if (p != null && p.getNivel() != null) {
					String nivelName = p.getNivel().name(); // ej: _1LS, _2NLS, PM1...
					String numerico = nivelName.replaceAll("\\D+", ""); // extraer dígitos
					boolean esLS = nivelName.toUpperCase().contains("LS");
					String sufijo = esLS ? "ls" : "nls";
					String valor;
					if (!numerico.isEmpty()) {
						valor = numerico.toLowerCase() + sufijo; // p.ej. "1ls", "2nls"
					} else {
						// Fallback para PM1/PM2/NORMAL a partir de name()
						String val = p.getNivel().name(); // p.ej. "PM1" o "NORMAL"
						valor = val != null ? val.toLowerCase() : "";
					}
					c1.setCellValue(valor);
				} else {
					c1.setCellValue("");
				}
                c1.setCellStyle(estiloDatoPlano);

                Cell c2 = filaPregunta.createCell(2);
				c2.setCellValue(p != null ? p.getPregunta() : "");
                c2.setCellStyle(estiloDatoWrap);

                Cell c3 = filaPregunta.createCell(3);
				c3.setCellValue(p != null ? p.getRespuesta() : "");
                c3.setCellStyle(estiloDatoWrap);

                Cell c4 = filaPregunta.createCell(4);
				c4.setCellValue(p != null && p.getDatosExtra() != null ? p.getDatosExtra() : "");
                c4.setCellStyle(estiloDatoWrap);

                Cell c5 = filaPregunta.createCell(5);
                c5.setCellValue(""); // Campo REC editable
                c5.setCellStyle(estiloDatoPlano);

				// Altura doble (aprox. 2 líneas) para leer bien
				filaPregunta.setHeightInPoints(36);
            }
        } else {
            // Cuestionario vacío - crear filas en blanco
            for (int i = 0; i < 4; i++) {
                Row filaVacia = sheet.createRow(filaActual++);
                for (int j = 0; j < 6; j++) {
                    Cell c = filaVacia.createCell(j);
                    c.setCellValue("");
                    c.setCellStyle(crearEstiloDato(workbook, false));
                }
				// Altura doble para filas vacías también
				filaVacia.setHeightInPoints(36);
            }
        }
        
        // Campos adicionales debajo del cuestionario - organizados verticalmente
        filaActual++; // Fila en blanco
        
        // CONCURSANTE
        Row filaConcursante = sheet.createRow(filaActual++);
        filaConcursante.createCell(0).setCellValue("CONCURSANTE:");
        Cell celdaConcursante = filaConcursante.createCell(1);
		celdaConcursante.setCellValue(""); // Campo editable que se extiende
		celdaConcursante.setCellStyle(crearEstiloDato(workbook, true));
		CellRangeAddress rgConc = new CellRangeAddress(filaActual-1, filaActual-1, 1, 5);
		sheet.addMergedRegion(rgConc);
		aplicarBordeRegion(sheet, rgConc);
		// Altura aproximada a 2 líneas
		filaConcursante.setHeightInPoints(36);
        
        // RESULTADO  
        Row filaResultado = sheet.createRow(filaActual++);
        filaResultado.createCell(0).setCellValue("RESULTADO:");
        Cell celdaResultado = filaResultado.createCell(1);
		celdaResultado.setCellValue(""); // Campo editable que se extiende
		celdaResultado.setCellStyle(crearEstiloDato(workbook, true));
		CellRangeAddress rgRes = new CellRangeAddress(filaActual-1, filaActual-1, 1, 5);
		sheet.addMergedRegion(rgRes);
		aplicarBordeRegion(sheet, rgRes);
		filaResultado.setHeightInPoints(36);
        
        // GRABACIÓN
        Row filaGrabacion = sheet.createRow(filaActual++);
		filaGrabacion.createCell(0).setCellValue("GRABACION:");
        Cell celdaGrabacion = filaGrabacion.createCell(1);
		celdaGrabacion.setCellValue(""); // Campo editable que se extiende
		celdaGrabacion.setCellStyle(crearEstiloDato(workbook, true));
		CellRangeAddress rgGrab = new CellRangeAddress(filaActual-1, filaActual-1, 1, 5);
		sheet.addMergedRegion(rgGrab);
		aplicarBordeRegion(sheet, rgGrab);
		filaGrabacion.setHeightInPoints(36);
        
        // NOTAS GUION (sin tilde)
        Row filaNotasGuion = sheet.createRow(filaActual++);
        filaNotasGuion.createCell(0).setCellValue("NOTAS GUION:");
        Cell celdaNotasGuion = filaNotasGuion.createCell(1);
		// Mostrar las notas de los guionistas si existen (notasDireccion del cuestionario)
		String notasGuion = (cuestionario != null && cuestionario.getNotasDireccion() != null) ? cuestionario.getNotasDireccion() : "";
		celdaNotasGuion.setCellValue(notasGuion);
		celdaNotasGuion.setCellStyle(crearEstiloDato(workbook, true));
		CellRangeAddress rgNG = new CellRangeAddress(filaActual-1, filaActual-1, 1, 5);
		sheet.addMergedRegion(rgNG);
		aplicarBordeRegion(sheet, rgNG);
		// Altura grande (aprox. 10 líneas)
		filaNotasGuion.setHeightInPoints(150);

        // Salto de página para imprimir un cuestionario por página
        sheet.setRowBreak(filaActual);
        
        return filaActual;
    }

	private void aplicarBordeRegion(Sheet sheet, CellRangeAddress region) {
		RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
		RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
		RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
		RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
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
        
		// Configurar anchos de columna para COMBOS (7 columnas)
		final int anchoAComb = 4000;            // ID COMBO
		final int anchoBComb = 2500;            // TIPO
		final int anchoCComb = 2500;            // FAC
		final int anchoDComb = (int) (22000 * 0.8); // PREGUNTA (igual que cuestionarios)
		final int anchoEComb = 16000 / 2;       // RESPUESTA (igual que cuestionarios)
		final int anchoFComb = 10000;           // DATOS EXTRA
		final int anchoGComb = 2000;            // REC

		sheet.setColumnWidth(0, anchoAComb);
		sheet.setColumnWidth(1, anchoBComb);
		sheet.setColumnWidth(2, anchoCComb);
		sheet.setColumnWidth(3, anchoDComb);
		sheet.setColumnWidth(4, anchoEComb);
		sheet.setColumnWidth(5, anchoFComb);
		sheet.setColumnWidth(6, anchoGComb);
        
        // Título de la jornada
        Row filaTitulo = sheet.createRow(filaActual++);
        Cell celdaTitulo = filaTitulo.createCell(0);
        celdaTitulo.setCellValue("COMBOS - JORNADA: " + jornada.getNombre() + " - " + jornada.getFechaJornada());
        CellStyle estiloTitulo = crearEstiloTitulo(workbook);
        celdaTitulo.setCellStyle(estiloTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
        
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
        sheet.addMergedRegion(new CellRangeAddress(filaActual-1, filaActual-1, 0, 6));
        
        // Encabezados de tabla (7 columnas)
        Row filaEncabezados = sheet.createRow(filaActual++);
        String[] encabezados = {"ID COMBO", "TIPO", "FAC", "PREGUNTA", "RESPUESTA", "DATOS EXTRA", "REC"};
        
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
            
            // Estilos de datos
            CellStyle estiloDatoWrap = crearEstiloDato(workbook, true);
            CellStyle estiloDatoPlano = crearEstiloDato(workbook, false);

            for (PreguntaCombo pc : preguntas) {
                Row filaPregunta = sheet.createRow(filaActual++);
                Pregunta p = pc.getPregunta();
                
                // Columna 0: ID COMBO (repetido en cada fila)
                Cell c0 = filaPregunta.createCell(0);
                if (combo != null && combo.getId() != null) {
                    c0.setCellValue(combo.getId());
                } else {
                    c0.setCellValue("");
                }
                c0.setCellStyle(estiloDatoPlano);

                // Columna 1: TIPO
                Cell c1 = filaPregunta.createCell(1);
                String tipo = (combo != null && combo.getTipo() != null) ? combo.getTipo().name() : "";
                c1.setCellValue(tipo);
                c1.setCellStyle(estiloDatoPlano);

                // Columna 2: FAC (xN)
                Cell c2 = filaPregunta.createCell(2);
                String factorStr = pc.getFactorMultiplicacion() != null ? pc.getFactorMultiplicacion() : "";
                int factorNum = 0;
                try {
                    factorNum = Integer.parseInt(factorStr.replaceAll("\\D+", ""));
                    factorStr = "x" + factorNum;
                } catch (Exception e) {
                    if (factorStr == null || factorStr.trim().isEmpty()) {
                        factorStr = "x";
                    }
                }
                c2.setCellValue(factorStr);
                c2.setCellStyle(estiloDatoPlano);

                // Columna 3: PREGUNTA
                Cell c3 = filaPregunta.createCell(3);
                c3.setCellValue(p != null ? p.getPregunta() : "");
                c3.setCellStyle(estiloDatoWrap);

                // Columna 4: RESPUESTA
                Cell c4 = filaPregunta.createCell(4);
                c4.setCellValue(p != null ? p.getRespuesta() : "");
                c4.setCellStyle(estiloDatoWrap);

                // Columna 5: DATOS EXTRA
                Cell c5 = filaPregunta.createCell(5);
                c5.setCellValue(p != null && p.getDatosExtra() != null ? p.getDatosExtra() : "");
                c5.setCellStyle(estiloDatoWrap);

                // Columna 6: REC
                Cell c6 = filaPregunta.createCell(6);
                c6.setCellValue(""); // Campo REC editable
                c6.setCellStyle(estiloDatoPlano);

                // Altura doble como en cuestionarios
                filaPregunta.setHeightInPoints(36);
            }
        } else {
            // Combo vacío - crear filas en blanco
            for (int i = 0; i < 3; i++) {
                Row filaVacia = sheet.createRow(filaActual++);
                for (int j = 0; j < 7; j++) {
                    Cell c = filaVacia.createCell(j);
                    c.setCellValue("");
                    c.setCellStyle(crearEstiloDato(workbook, false));
                }
                filaVacia.setHeightInPoints(36);
            }
        }
        
        // Campos adicionales debajo del combo - organizados verticalmente
        filaActual++; // Fila en blanco
        
        // CONCURSANTE
        Row filaConcursante = sheet.createRow(filaActual++);
        filaConcursante.createCell(0).setCellValue("CONCURSANTE:");
        Cell celdaConcursante = filaConcursante.createCell(1);
        celdaConcursante.setCellValue(""); // Campo editable que se extiende
        CellRangeAddress rgConc = new CellRangeAddress(filaActual-1, filaActual-1, 1, 6);
        sheet.addMergedRegion(rgConc);
        aplicarBordeRegion(sheet, rgConc);
        filaConcursante.setHeightInPoints(36);
        
        // RESULTADO  
        Row filaResultado = sheet.createRow(filaActual++);
        filaResultado.createCell(0).setCellValue("RESULTADO:");
        Cell celdaResultado = filaResultado.createCell(1);
        celdaResultado.setCellValue(""); // Campo editable que se extiende
        CellRangeAddress rgRes = new CellRangeAddress(filaActual-1, filaActual-1, 1, 6);
        sheet.addMergedRegion(rgRes);
        aplicarBordeRegion(sheet, rgRes);
        filaResultado.setHeightInPoints(36);
        
        // GRABACIÓN
        Row filaGrabacion = sheet.createRow(filaActual++);
        filaGrabacion.createCell(0).setCellValue("GRABACION:");
        Cell celdaGrabacion = filaGrabacion.createCell(1);
        celdaGrabacion.setCellValue(""); // Campo editable que se extiende
        CellRangeAddress rgGrab = new CellRangeAddress(filaActual-1, filaActual-1, 1, 6);
        sheet.addMergedRegion(rgGrab);
        aplicarBordeRegion(sheet, rgGrab);
        filaGrabacion.setHeightInPoints(36);
        
        // NOTAS GUION (sin tilde)
        Row filaNotasGuion = sheet.createRow(filaActual++);
        filaNotasGuion.createCell(0).setCellValue("NOTAS GUION:");
        Cell celdaNotasGuion = filaNotasGuion.createCell(1);
        // Mostrar las notas de los guionistas si existen (notasDireccion del combo)
        String notasGuion = (combo != null && combo.getNotasDireccion() != null) ? combo.getNotasDireccion() : "";
        celdaNotasGuion.setCellValue(notasGuion); // Campo editable que se extiende
        CellRangeAddress rgNG = new CellRangeAddress(filaActual-1, filaActual-1, 1, 6);
        sheet.addMergedRegion(rgNG);
        aplicarBordeRegion(sheet, rgNG);
        filaNotasGuion.setHeightInPoints(150);


        // Salto de página por combo
        sheet.setRowBreak(filaActual);
        
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

    private CellStyle crearEstiloDato(Workbook workbook, boolean wrap) {
        CellStyle estilo = workbook.createCellStyle();
        estilo.setAlignment(HorizontalAlignment.LEFT);
        estilo.setVerticalAlignment(VerticalAlignment.TOP);
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setWrapText(wrap);
        return estilo;
    }

    private void configurarPagina(Sheet sheet, String tituloCabecera) {
        PrintSetup ps = sheet.getPrintSetup();
        ps.setLandscape(true);
        sheet.setFitToPage(true);
        ps.setFitWidth((short) 1);
        ps.setFitHeight((short) 0);
        sheet.setMargin(Sheet.LeftMargin, 0.3);
        sheet.setMargin(Sheet.RightMargin, 0.3);
        sheet.setMargin(Sheet.TopMargin, 0.5);
        sheet.setMargin(Sheet.BottomMargin, 0.5);
        Header header = sheet.getHeader();
        header.setCenter(tituloCabecera);
        Footer footer = sheet.getFooter();
        footer.setRight("Página &P de &N");
    }
} 