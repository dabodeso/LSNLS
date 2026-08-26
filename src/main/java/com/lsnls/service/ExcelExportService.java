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
            configurarPagina(hojaCuestionarios, "Cuestionarios - " + jornada.getNombre(), jornada.getNombre());
            crearHojaCuestionarios(hojaCuestionarios, jornada, workbook, opciones);
            
            // Crear hoja de COMBOS
            Sheet hojaCombos = workbook.createSheet("COMBOS");
            configurarPagina(hojaCombos, "Combos - " + jornada.getNombre(), jornada.getNombre());
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
        
        // Configurar anchos exactos en unidades Excel
		sheet.setColumnWidth(0, excelWidth(6.22));   // cuestionario
		sheet.setColumnWidth(1, excelWidth(7));      // nivel
		sheet.setColumnWidth(2, excelWidth(49.78));  // pregunta
		sheet.setColumnWidth(3, excelWidth(18.78));  // respuesta
		sheet.setColumnWidth(4, excelWidth(43.56));  // datos extra
		sheet.setColumnWidth(5, excelWidth(7.67));   // rec
        
        // Procesar cada cuestionario
        List<Cuestionario> cuestionarios = jornada.getCuestionariosPorSlot();
        for (int i = 0; i < 6; i++) {
            Cuestionario cuestionario = cuestionarios.get(i);
            filaActual = crearTablaCuestionario(sheet, cuestionario, i + 1, filaActual, workbook, opciones);
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
        
        // Encabezados de tabla: Nº CUEST, NIVEL, PREGUNTA, RESPUESTA, DATOS EXTRA, REC
        Row filaEncabezados = sheet.createRow(filaActual++);
		String[] encabezados = {"CUEST", "NIVEL", "PREGUNTA", "RESPUESTA", "DATOS EXTRA", "REC"};
        
        CellStyle estiloEncabezado = crearEstiloEncabezado(workbook);
        for (int i = 0; i < encabezados.length; i++) {
            Cell celda = filaEncabezados.createCell(i);
            celda.setCellValue(encabezados[i]);
            celda.setCellStyle(estiloEncabezado);
        }
        filaEncabezados.setHeightInPoints(15f);
        
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
            CellStyle estiloPrimeraColumna = crearEstiloPrimeraColumna(workbook);

			// Limitar a 4 preguntas por cuestionario
			int limite = Math.min(4, preguntas.size());
			for (int idx = 0; idx < limite; idx++) {
				PreguntaCuestionario pc = preguntas.get(idx);
                Row filaPregunta = sheet.createRow(filaActual++);
                Pregunta p = pc.getPregunta();

				// Columna 0: Nº cuestionario
				Cell c0 = filaPregunta.createCell(0);
				c0.setCellValue(cuestionario != null && cuestionario.getId() != null ? cuestionario.getId() : numeroCuestionario);
				c0.setCellStyle(estiloPrimeraColumna);

                // Columna 1: NIVEL en formato corto: "1ls", "2nls", "3ls", "4nls"
                Cell c1 = filaPregunta.createCell(1);
				if (p != null && p.getNivel() != null) {
					String nivelName = p.getNivel().name(); // ej: _1LS, _2NLS, _3LS, _4NLS
					String valor = "";
					// Convertir el enum a formato corto en minúsculas
					if (nivelName.equals("_1LS")) {
						valor = "1ls";
					} else if (nivelName.equals("_2NLS")) {
						valor = "2nls";
					} else if (nivelName.equals("_3LS")) {
						valor = "3ls";
					} else if (nivelName.equals("_4NLS")) {
						valor = "4nls";
					} else if (nivelName.equals("_5LS")) {
						valor = "5ls";
					} else if (nivelName.equals("_5NLS")) {
						valor = "5nls";
					} else if (nivelName.equals("_0")) {
						valor = "0";
					} else {
						// Fallback: extraer número y determinar sufijo
						String numerico = nivelName.replaceAll("\\D+", "");
						if (!numerico.isEmpty()) {
							// Verificar si contiene NLS (mayúsculas)
							if (nivelName.contains("NLS")) {
								valor = numerico + "nls";
							} else if (nivelName.contains("LS")) {
								valor = numerico + "ls";
							} else {
								valor = nivelName.toLowerCase().replace("_", "");
							}
						} else {
							valor = nivelName.toLowerCase().replace("_", "");
						}
					}
					c1.setCellValue(valor);
				} else {
					c1.setCellValue("");
				}
                c1.setCellStyle(estiloDatoPlano);

                // Columna 2: PREGUNTA
                Cell c2 = filaPregunta.createCell(2);
				c2.setCellValue(p != null ? p.getPregunta() : "");
                c2.setCellStyle(estiloDatoWrap);

                // Columna 3: RESPUESTA
                Cell c3 = filaPregunta.createCell(3);
				c3.setCellValue(p != null ? p.getRespuesta() : "");
                c3.setCellStyle(estiloDatoWrap);

                // Columna 4: DATOS EXTRA
                Cell c4 = filaPregunta.createCell(4);
				c4.setCellValue(p != null && p.getDatosExtra() != null ? p.getDatosExtra() : "");
                c4.setCellStyle(estiloDatoWrap);

                // Columna 5: REC
                Cell c5 = filaPregunta.createCell(5);
                c5.setCellValue(""); // Campo REC editable
                c5.setCellStyle(estiloDatoPlano);

				filaPregunta.setHeightInPoints(39.6f);
            }
            // Añadir una fila vacía adicional
            Row filaSeparadora = sheet.createRow(filaActual++);
            for (int j = 0; j < 6; j++) {
                Cell c = filaSeparadora.createCell(j);
                c.setCellValue("");
                c.setCellStyle(j == 0 ? estiloPrimeraColumna : crearEstiloDato(workbook, false));
            }
            filaSeparadora.setHeightInPoints(39.6f);
        } else {
            // Cuestionario vacío - crear 4 filas en blanco + 1 fila vacía adicional
            CellStyle estiloPrimeraColumna = crearEstiloPrimeraColumna(workbook);
            for (int i = 0; i < 5; i++) {
                Row filaVacia = sheet.createRow(filaActual++);
                for (int j = 0; j < 6; j++) {
                    Cell c = filaVacia.createCell(j);
                    c.setCellValue("");
                    c.setCellStyle(j == 0 ? estiloPrimeraColumna : crearEstiloDato(workbook, false));
                }
				filaVacia.setHeightInPoints(39.6f);
            }
        }

        // Campos adicionales debajo del cuestionario - sin fila extra de separación
        // CONCURSANTE
        Row filaConcursante = sheet.createRow(filaActual++);
        CellStyle estiloPrimeraColumna = crearEstiloPrimeraColumna(workbook);
        Cell cellTituloConc = filaConcursante.createCell(0);
        cellTituloConc.setCellValue("CONCURSANTE");
        cellTituloConc.setCellStyle(estiloPrimeraColumna);
        filaConcursante.createCell(1).setCellValue("");
        CellRangeAddress rgLblConc = new CellRangeAddress(filaActual-1, filaActual-1, 0, 1);
        sheet.addMergedRegion(rgLblConc);
        aplicarBordeRegion(sheet, rgLblConc);
        Cell celdaConcursante = filaConcursante.createCell(2);
		celdaConcursante.setCellValue(""); // Campo editable que se extiende
		celdaConcursante.setCellStyle(crearEstiloDato(workbook, true));
		CellRangeAddress rgConc = new CellRangeAddress(filaActual-1, filaActual-1, 2, 5);
		sheet.addMergedRegion(rgConc);
		aplicarBordeRegion(sheet, rgConc);
		filaConcursante.setHeightInPoints(33f);
        
        // RESULTADO  
        Row filaResultado = sheet.createRow(filaActual++);
        Cell cellTituloRes = filaResultado.createCell(0);
        cellTituloRes.setCellValue("RESULTADO");
        cellTituloRes.setCellStyle(estiloPrimeraColumna);
        filaResultado.createCell(1).setCellValue("");
        CellRangeAddress rgLblRes = new CellRangeAddress(filaActual-1, filaActual-1, 0, 1);
        sheet.addMergedRegion(rgLblRes);
        aplicarBordeRegion(sheet, rgLblRes);
        Cell celdaResultado = filaResultado.createCell(2);
		celdaResultado.setCellValue(""); // Campo editable que se extiende
		celdaResultado.setCellStyle(crearEstiloDato(workbook, true));
		CellRangeAddress rgRes = new CellRangeAddress(filaActual-1, filaActual-1, 2, 5);
		sheet.addMergedRegion(rgRes);
		aplicarBordeRegion(sheet, rgRes);
		filaResultado.setHeightInPoints(33f);
        
        // GRABACIÓN
        Row filaGrabacion = sheet.createRow(filaActual++);
        Cell cellTituloGrab = filaGrabacion.createCell(0);
		cellTituloGrab.setCellValue("GRABACION");
        cellTituloGrab.setCellStyle(estiloPrimeraColumna);
        filaGrabacion.createCell(1).setCellValue("");
        CellRangeAddress rgLblGrab = new CellRangeAddress(filaActual-1, filaActual-1, 0, 1);
        sheet.addMergedRegion(rgLblGrab);
        aplicarBordeRegion(sheet, rgLblGrab);
        Cell celdaGrabacion = filaGrabacion.createCell(2);
		celdaGrabacion.setCellValue(""); // Campo editable que se extiende
		celdaGrabacion.setCellStyle(crearEstiloDato(workbook, true));
		CellRangeAddress rgGrab = new CellRangeAddress(filaActual-1, filaActual-1, 2, 5);
		sheet.addMergedRegion(rgGrab);
		aplicarBordeRegion(sheet, rgGrab);
		filaGrabacion.setHeightInPoints(33f);
        
        // NOTAS GUION
        Row filaNotasGuion = sheet.createRow(filaActual++);
        Cell cellTituloNotas = filaNotasGuion.createCell(0);
        cellTituloNotas.setCellValue("NOTAS GUION");
        cellTituloNotas.setCellStyle(estiloPrimeraColumna);
        filaNotasGuion.createCell(1).setCellValue("");
        CellRangeAddress rgLblNG = new CellRangeAddress(filaActual-1, filaActual-1, 0, 1);
        sheet.addMergedRegion(rgLblNG);
        aplicarBordeRegion(sheet, rgLblNG);
        Cell celdaNotasGuion = filaNotasGuion.createCell(2);
		celdaNotasGuion.setCellValue(""); // Campo editable que se extiende
		celdaNotasGuion.setCellStyle(crearEstiloDato(workbook, true));
		CellRangeAddress rgNG = new CellRangeAddress(filaActual-1, filaActual-1, 2, 5);
		sheet.addMergedRegion(rgNG);
		aplicarBordeRegion(sheet, rgNG);
		filaNotasGuion.setHeightInPoints(216f);

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
        
		// Configurar anchos de columna para COMBOS: TEMÁTICA, TIPO, FAC, NIVEL, PREGUNTA, RESPUESTA, DATOS EXTRA, REC (8 columnas)
		sheet.setColumnWidth(0, 5500);  // TEMÁTICA (aumentado para que "NOTAS DIRECCION" no se corte)
		sheet.setColumnWidth(1, 2000);  // TIPO
		sheet.setColumnWidth(2, 2000);  // FAC
		sheet.setColumnWidth(3, 2000);  // NIVEL
		sheet.setColumnWidth(4, 15000); // PREGUNTA
		sheet.setColumnWidth(5, 10000); // RESPUESTA
		sheet.setColumnWidth(6, 8000);  // DATOS EXTRA
		sheet.setColumnWidth(7, 2000);  // REC
        
        // Título de la jornada
        Row filaTitulo = sheet.createRow(filaActual++);
        Cell celdaTitulo = filaTitulo.createCell(0);
        celdaTitulo.setCellValue("COMBOS - JORNADA: " + jornada.getNombre() + " - " + jornada.getFechaJornada());
        CellStyle estiloTitulo = crearEstiloTitulo(workbook);
        celdaTitulo.setCellStyle(estiloTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
        
        filaActual++; // Fila en blanco
        
        // Procesar cada combo
        List<Combo> combos = jornada.getCombosPorSlot();
        for (int i = 0; i < 6; i++) {
            Combo combo = combos.get(i);
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
        String titulo;
        if (combo != null) {
            String nivelCombo = combo.getNivel() != null ? combo.getNivel().name() : "";
            // Convertir nivel a formato corto
            String nivelCorto = "";
            if (nivelCombo.equals("_5LS")) {
                nivelCorto = "5LS";
            } else if (nivelCombo.equals("_5NLS")) {
                nivelCorto = "5NLS";
            } else if (nivelCombo.equals("NORMAL")) {
                nivelCorto = "NORMAL";
            } else {
                nivelCorto = nivelCombo.replace("_", "");
            }
            titulo = "COMBO " + numeroCombo + " (ID: " + combo.getId() + " - NIVEL: " + nivelCorto + ")";
        } else {
            titulo = "COMBO " + numeroCombo + " (VACÍO)";
        }
        celdaTituloCombo.setCellValue(titulo);
        CellStyle estiloSubtitulo = crearEstiloSubtitulo(workbook);
        celdaTituloCombo.setCellStyle(estiloSubtitulo);
        sheet.addMergedRegion(new CellRangeAddress(filaActual-1, filaActual-1, 0, 7));
        
        // Encabezados de tabla: TEMÁTICA, TIPO, FAC, NIVEL, PREGUNTA, RESPUESTA, DATOS EXTRA, REC (8 columnas)
        Row filaEncabezados = sheet.createRow(filaActual++);
        String[] encabezados = {"TEMÁTICA", "TIPO", "FAC", "NIVEL", "PREGUNTA", "RESPUESTA", "DATOS EXTRA", "REC"};
        
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
                
                // Columna 0: TEMÁTICA (del combo o de la pregunta)
                Cell c0 = filaPregunta.createCell(0);
                String tematica = (combo != null && combo.getTematica() != null) 
                    ? combo.getTematica() 
                    : (p != null && p.getTematica() != null ? p.getTematica() : "");
                c0.setCellValue(tematica);
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

                // Columna 3: NIVEL en formato corto: "1ls", "2nls", "3ls", "4nls"
                Cell c3 = filaPregunta.createCell(3);
                if (p != null && p.getNivel() != null) {
                    String nivelName = p.getNivel().name(); // ej: _1LS, _2NLS, _3LS, _4NLS
                    String valor = "";
                    // Convertir el enum a formato corto en minúsculas
                    if (nivelName.equals("_1LS")) {
                        valor = "1ls";
                    } else if (nivelName.equals("_2NLS")) {
                        valor = "2nls";
                    } else if (nivelName.equals("_3LS")) {
                        valor = "3ls";
                    } else if (nivelName.equals("_4NLS")) {
                        valor = "4nls";
                    } else if (nivelName.equals("_5LS")) {
                        valor = "5ls";
                    } else if (nivelName.equals("_5NLS")) {
                        valor = "5nls";
                    } else if (nivelName.equals("_0")) {
                        valor = "0";
                    } else {
                        // Fallback: extraer número y determinar sufijo
                        String numerico = nivelName.replaceAll("\\D+", "");
                        if (!numerico.isEmpty()) {
                            // Verificar si contiene NLS (mayúsculas)
                            if (nivelName.contains("NLS")) {
                                valor = numerico + "nls";
                            } else if (nivelName.contains("LS")) {
                                valor = numerico + "ls";
                            } else {
                                valor = nivelName.toLowerCase().replace("_", "");
                            }
                        } else {
                            valor = nivelName.toLowerCase().replace("_", "");
                        }
                    }
                    c3.setCellValue(valor);
                } else {
                    c3.setCellValue("");
                }
                c3.setCellStyle(estiloDatoPlano);

                // Columna 4: PREGUNTA
                Cell c4 = filaPregunta.createCell(4);
                c4.setCellValue(p != null ? p.getPregunta() : "");
                c4.setCellStyle(estiloDatoWrap);

                // Columna 5: RESPUESTA
                Cell c5 = filaPregunta.createCell(5);
                c5.setCellValue(p != null ? p.getRespuesta() : "");
                c5.setCellStyle(estiloDatoWrap);

                // Columna 6: DATOS EXTRA
                Cell c6 = filaPregunta.createCell(6);
                c6.setCellValue(p != null && p.getDatosExtra() != null ? p.getDatosExtra() : "");
                c6.setCellStyle(estiloDatoWrap);

                // Columna 7: REC
                Cell c7 = filaPregunta.createCell(7);
                c7.setCellValue(""); // Campo REC editable
                c7.setCellStyle(estiloDatoPlano);

                // Altura aumentada un 20% como en cuestionarios
                filaPregunta.setHeightInPoints(43.2f);
            }
        } else {
            // Combo vacío - crear filas en blanco
            for (int i = 0; i < 3; i++) {
                Row filaVacia = sheet.createRow(filaActual++);
                for (int j = 0; j < 8; j++) {
                    Cell c = filaVacia.createCell(j);
                    c.setCellValue("");
                    c.setCellStyle(crearEstiloDato(workbook, false));
                }
                filaVacia.setHeightInPoints(43.2f);
            }
        }
        
        // Campos adicionales debajo del combo - organizados verticalmente
        filaActual++; // Fila en blanco
        
        // CONCURSANTE
        Row filaConcursante = sheet.createRow(filaActual++);
        filaConcursante.createCell(0).setCellValue("CONCURSANTE:");
        Cell celdaConcursante = filaConcursante.createCell(1);
        celdaConcursante.setCellValue(""); // Campo editable que se extiende
        celdaConcursante.setCellStyle(crearEstiloDato(workbook, true));
        CellRangeAddress rgConc = new CellRangeAddress(filaActual-1, filaActual-1, 1, 7);
        sheet.addMergedRegion(rgConc);
        aplicarBordeRegion(sheet, rgConc);
        filaConcursante.setHeightInPoints(43.2f);
        
        // RESULTADO  
        Row filaResultado = sheet.createRow(filaActual++);
        filaResultado.createCell(0).setCellValue("RESULTADO:");
        Cell celdaResultado = filaResultado.createCell(1);
        celdaResultado.setCellValue(""); // Campo editable que se extiende
        celdaResultado.setCellStyle(crearEstiloDato(workbook, true));
        CellRangeAddress rgRes = new CellRangeAddress(filaActual-1, filaActual-1, 1, 7);
        sheet.addMergedRegion(rgRes);
        aplicarBordeRegion(sheet, rgRes);
        filaResultado.setHeightInPoints(43.2f);
        
        // GRABACIÓN
        Row filaGrabacion = sheet.createRow(filaActual++);
        filaGrabacion.createCell(0).setCellValue("GRABACION:");
        Cell celdaGrabacion = filaGrabacion.createCell(1);
        celdaGrabacion.setCellValue(""); // Campo editable que se extiende
        celdaGrabacion.setCellStyle(crearEstiloDato(workbook, true));
        CellRangeAddress rgGrab = new CellRangeAddress(filaActual-1, filaActual-1, 1, 7);
        sheet.addMergedRegion(rgGrab);
        aplicarBordeRegion(sheet, rgGrab);
        filaGrabacion.setHeightInPoints(43.2f);
        
        // NOTAS DIRECCIÓN
        Row filaNotasDireccion = sheet.createRow(filaActual++);
        filaNotasDireccion.createCell(0).setCellValue("NOTAS DIRECCION:");
        Cell celdaNotasDireccion = filaNotasDireccion.createCell(1);
        // Mostrar las notas de dirección si existen (notasDireccion del combo)
        String notasDireccion = (combo != null && combo.getNotasDireccion() != null) ? combo.getNotasDireccion() : "";
        celdaNotasDireccion.setCellValue(notasDireccion);
        celdaNotasDireccion.setCellStyle(crearEstiloDato(workbook, true));
        CellRangeAddress rgND = new CellRangeAddress(filaActual-1, filaActual-1, 1, 7);
        sheet.addMergedRegion(rgND);
        aplicarBordeRegion(sheet, rgND);
        // Altura aumentada un 20%
        filaNotasDireccion.setHeightInPoints(43.2f);
        
        // NOTAS GUION (sin tilde)
        Row filaNotasGuion = sheet.createRow(filaActual++);
        filaNotasGuion.createCell(0).setCellValue("NOTAS GUION:");
        Cell celdaNotasGuion = filaNotasGuion.createCell(1);
        celdaNotasGuion.setCellValue(""); // Campo editable que se extiende
        celdaNotasGuion.setCellStyle(crearEstiloDato(workbook, true));
        CellRangeAddress rgNG = new CellRangeAddress(filaActual-1, filaActual-1, 1, 7);
        sheet.addMergedRegion(rgNG);
        aplicarBordeRegion(sheet, rgNG);
        filaNotasGuion.setHeightInPoints(180f);


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
        fuente.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(fuente);
        estilo.setAlignment(HorizontalAlignment.CENTER);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        estilo.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        return estilo;
    }

    private CellStyle crearEstiloPrimeraColumna(Workbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        Font fuente = workbook.createFont();
        fuente.setBold(true);
        fuente.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(fuente);
        estilo.setAlignment(HorizontalAlignment.LEFT);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        estilo.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setWrapText(true);
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

    private int excelWidth(double width) {
        return (int) Math.round(width * 256);
    }

    private void configurarPagina(Sheet sheet, String tituloCabecera, String nombreJornada) {
        PrintSetup ps = sheet.getPrintSetup();
        ps.setLandscape(true);
        sheet.setFitToPage(true);
        ps.setFitWidth((short) 1);
        ps.setFitHeight((short) 0);
        // Ajustar márgenes para mejor uso del espacio
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
} 