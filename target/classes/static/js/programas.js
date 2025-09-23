let programas = [];
let concursantesPorPrograma = {};

// Variables para paginación
let paginaActual = 0;
let totalPaginas = 0;
let tamañoPagina = 5;
let totalItems = 0;

async function inicializarProgramas() {
    // Mostrar enlace de administración solo para admins
    const usuario = JSON.parse(localStorage.getItem('usuario'));
    if (usuario && usuario.rol === 'ROLE_ADMIN') {
        const navAdmin = document.getElementById('nav-admin');
        if (navAdmin) {
            navAdmin.style.display = 'block';
        }
    }
    
    await cargarProgramas();
}

async function cargarProgramas() {
    try {
        await cargarProgramasPaginados(0);
    } catch (error) {
        if (error && error.message && error.message.startsWith('401')) {
            return;
        }
        mostrarError('Error al cargar programas: ' + error.message);
    }
}

// Función auxiliar para recargar programas y configurar scroll
async function recargarProgramas() {
    await cargarProgramas();
    configurarScrollTablas();
}

async function cargarProgramasPaginados(pagina, ordenPor = 'id', direccionOrden = 'asc') {
    try {
        const response = await apiManager.get(
            `/api/programas/pagina?page=${pagina}&size=${tamañoPagina}&sortBy=${ordenPor}&sortDir=${direccionOrden}`
        );
        
        programas = response.programas;
        paginaActual = response.currentPage;
        totalItems = response.totalItems;
        totalPaginas = response.totalPages;
        
        await cargarConcursantesPorPrograma();
        mostrarProgramas();
        configurarScrollTablas(); // Configurar scroll automático en las tablas
        renderizarPaginacion();
    } catch (error) {
        if (error && error.message && error.message.startsWith('401')) {
            return;
        }
        mostrarError('Error al cargar programas: ' + error.message);
    }
}

async function cargarConcursantesPorPrograma() {
    concursantesPorPrograma = {};
    for (const programa of programas) {
        try {
            const concursantes = await apiManager.get(`/api/concursantes/programa/${programa.id}`);
            concursantesPorPrograma[programa.id] = concursantes;
        } catch (e) {
            concursantesPorPrograma[programa.id] = [];
        }
    }
}

function mostrarProgramas() {
    const contenedor = document.getElementById('contenedor-programas');
    
    if (programas.length === 0) {
        contenedor.innerHTML = '<div class="alert alert-info">No hay programas registrados.</div>';
        return;
    }
    
    const visibles = aplicarFiltros(programas);
    if (visibles.length === 0) {
        contenedor.innerHTML = '<div class="alert alert-warning">No hay programas que coincidan con los filtros.</div>';
        return;
    }
    
    // Añadir información de paginación
    let infoPaginacion = '';
    if (totalItems > 0) {
        const inicio = paginaActual * tamañoPagina + 1;
        const fin = Math.min((paginaActual + 1) * tamañoPagina, totalItems);
        infoPaginacion = `<div class="d-flex justify-content-between align-items-center mb-3">
            <div class="text-muted">
                Mostrando ${inicio}-${fin} de ${totalItems} programas
            </div>
            <div id="paginacion-programas" class="btn-group">
                <!-- Los botones de paginación se insertarán aquí -->
            </div>
        </div>`;
    }
    
    contenedor.innerHTML = infoPaginacion + visibles.map(programa => {
        const concursantes = concursantesPorPrograma[programa.id] || [];
        const fechaFormateada = formatearFechaPrograma(programa.fechaEmision);
        const totalResultados = calcularTotalResultados(concursantes);
        const duracionReal = calcularDuracionReal(concursantes);
        const duracionObjetivo = programa.duracionObjetivo || '1h 5m';
        const gap = calcularGap(duracionObjetivo, duracionReal);
        
        // Definir colores para estados
        const estadoColores = {
            'borrador': '#6c757d',     // Gris
            'programado': '#28a745',   // Verde (Listo)
            'emitido': '#dc3545',      // Rojo
            // Estados extra por compatibilidad
            'grabado': '#17a2b8',
            'editado': '#ffc107'
        };
        
        const estadoColor = estadoColores[programa.estado] || '#6c757d';
        
        // Crear 3 filas vacías si hay menos de 3 concursantes
        const filasVacias = [];
        for (let i = concursantes.length; i < 3; i++) {
            filasVacias.push(`
                <tr class="fila-vacia">
                    <td class="col-numero"></td>
                    <td class="col-lugar"></td>
                    <td class="col-nombre"><em style="color: #999;">Hueco disponible</em></td>
                    <td class="col-edad"></td>
                    <td class="col-ocupacion"></td>
                    <td class="col-rrss"></td>
                    <td class="col-resultado"></td>
                    <td class="col-duracion"></td>
                    <td class="col-foto"></td>
                    <td class="col-momentos"></td>
                    <td class="col-factor-x"></td>
                    <td class="col-valoracion"></td>
                    <td class="col-creditos"></td>
                    <td class="col-acciones"></td>
                </tr>
            `);
        }
        
        return `
            <div class="programa-container" data-programa-id="${programa.id}">
                <div class="programa-header">
                    <div class="programa-info">
                        <div class="programa-info-item">
                            <div class="programa-info-label">Temporada</div>
                            <div class="programa-info-value">
                                <input type="number" class="form-control form-control-sm" min="1" value="${programa.temporada || ''}"
                                       onchange="actualizarTemporadaPrograma(${programa.id}, this.value)" style="width: 80px;">
                            </div>
                        </div>
                        <div class="programa-info-item" style="min-width: 80px;">
                            <div class="programa-info-label">Programa</div>
                            <div class="programa-info-value">${programa.id}</div>
                        </div>
                        <div class="programa-info-item">
                            <div class="programa-info-label">Estado</div>
                            <div class="programa-info-value">
                                <select class="form-select form-select-sm" style="min-width: 140px;"
                                        onchange="actualizarEstadoPrograma(${programa.id}, this.value)">
                                    ${renderOpcionEstado(programa.estado, 'borrador', 'Borrador')}
                                    ${renderOpcionEstado(programa.estado, 'grabado', 'Grabado')}
                                    ${renderOpcionEstado(programa.estado, 'editado', 'Editado')}
                                    ${renderOpcionEstado(programa.estado, 'programado', 'Programado')}
                                    ${renderOpcionEstado(programa.estado, 'emitido', 'Emitido')}
                                </select>
                            </div>
                        </div>
                        <div class="programa-info-item">
                            <div class="programa-info-label">Fecha de emisión</div>
                            <div class="programa-info-value">
                                <input type="date" class="form-control form-control-sm" value="${programa.fechaEmision || ''}"
                                       onchange="actualizarFechaEmision(${programa.id}, this.value)" style="width: 150px;">
                            </div>
                        </div>
                        <div class="programa-info-item">
                            <div class="programa-info-label">Total Premios</div>
                            <div class="programa-info-value">
                                <span class="programa-info-readonly">${totalResultados}€</span>
                            </div>
                        </div>
                        <div class="programa-info-item">
                            <div class="programa-info-label">Duración Objetivo</div>
                            <div class="programa-info-value">
                                <input type="text" class="form-control form-control-sm" 
                                       value="${duracionObjetivo}" 
                                       onchange="actualizarDuracionObjetivoPrograma(${programa.id}, this.value)"
                                       placeholder="1h 5m"
                                       style="width: 80px; font-size: 0.9em;">
                            </div>
                        </div>
                        <div class="programa-info-item">
                            <div class="programa-info-label">GAP</div>
                            <div class="programa-info-value">
                                <span class="programa-info-readonly">${gap}</span>
                            </div>
                        </div>
                        <div class="programa-acciones">
                            <button class="btn btn-success" onclick="mostrarConcursantesDisponibles(${programa.id})" title="Añadir concursante">
                                <i class="fas fa-user-plus"></i>
                            </button>
                            <button class="btn btn-danger" onclick="eliminarPrograma(${programa.id})" title="Borrar programa">
                                <i class="fas fa-trash"></i>
                            </button>
                        </div>
                    </div>
                </div>
                
                <div class="concursantes-table">
                    <div class="table-responsive">
                        <table class="table table-excel table-striped">
                            <thead>
                                <tr>
                                    <th class="col-numero">Nº CONC</th>
                                    <th class="col-lugar">LUGAR</th>
                                    <th class="col-nombre">NOMBRE</th>
                                    <th class="col-edad">EDAD</th>
                                    <th class="col-ocupacion">OCUPACIÓN</th>
                                    <th class="col-rrss">RR SS</th>
                                    <th class="col-resultado">RESULTADO</th>
                                    <th class="col-duracion">DUR CONC</th>
                                    <th class="col-foto">FOTO</th>
                                    <th class="col-momentos">MOM. DESTACADOS</th>
                                    <th class="col-factor-x">X</th>
                                    <th class="col-valoracion">VAL</th>
                                    <th class="col-creditos">CRÉDITOS ESPECIALES</th>
                                    <th style="width: 5%;">ACC</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${concursantes.map(concursante => `
                                    <tr class="concursante-row" onclick="irAConcursante(${concursante.id})">
                                        <td class="col-numero">${concursante.numeroConcursante || ''}</td>
                                        <td class="col-lugar">${concursante.lugar || ''}</td>
                                        <td class="col-nombre"><strong>${concursante.nombre || ''}</strong></td>
                                        <td class="col-edad">${concursante.edad || ''}</td>
                                        <td class="col-ocupacion">${concursante.ocupacion || ''}</td>
                                        <td class="col-rrss">${concursante.redesSociales || ''}</td>
                                        <td class="col-resultado">
                                            <input type="text" class="campo-editable" 
                                                   value="${concursante.resultado || ''}" 
                                                   onchange="actualizarCampoConcursante(${concursante.id}, 'resultado', this.value)"
                                                   onclick="event.stopPropagation()"
                                                   placeholder="0€">
                                        </td>
                                        <td class="col-duracion">${obtenerDuracionConcursante(concursante)}</td>
                                        <td class="col-foto">
                                            ${concursante.foto ? 
                                                `<img src="/uploads/${concursante.foto}" class="foto-concursante" alt="Foto" onclick="abrirExploradorFoto(${concursante.id}, event)" title="Click para cambiar foto">` : 
                                                `<div class="campo-foto-vacio" onclick="abrirExploradorFoto(${concursante.id}, event)" title="Click para añadir foto">
                                                    <i class="fas fa-camera"></i>
                                                    <span>Añadir foto</span>
                                                 </div>`
                                            }
                                        </td>
                                        <td class="col-momentos">
                                            <textarea class="campo-editable" 
                                                      onchange="actualizarCampoConcursante(${concursante.id}, 'momentosDestacados', this.value)"
                                                      onclick="event.stopPropagation()"
                                                      placeholder="Momentos destacados"
                                                      rows="2">${concursante.momentosDestacados || ''}</textarea>
                                        </td>
                                        <td class="col-factor-x">
                                            <input type="text" class="campo-editable" 
                                                   value="${concursante.factorX || ''}" 
                                                   onchange="actualizarCampoConcursante(${concursante.id}, 'factorX', this.value)"
                                                   onclick="event.stopPropagation()"
                                                   placeholder="Factor X">
                                        </td>
                                        <td class="col-valoracion">
                                            <textarea class="campo-editable" 
                                                      onchange="actualizarCampoConcursante(${concursante.id}, 'valoracionFinal', this.value)"
                                                      onclick="event.stopPropagation()"
                                                      placeholder="Valoración"
                                                      rows="2">${concursante.valoracionFinal || ''}</textarea>
                                        </td>
                                        <td class="col-creditos">
                                            <textarea class="campo-editable" 
                                                      onchange="actualizarCampoConcursante(${concursante.id}, 'creditosEspeciales', this.value)"
                                                      onclick="event.stopPropagation()"
                                                      placeholder="Créditos especiales"
                                                      rows="2">${concursante.creditosEspeciales || ''}</textarea>
                                        </td>
                                        <td class="col-acciones">
                                            <button class="btn btn-sm btn-danger" onclick="quitarConcursanteDePrograma(${concursante.id}, event)" title="Quitar del programa">
                                                <i class="fas fa-times"></i>
                                            </button>
                                        </td>
                                    </tr>
                                `).join('')}
                                ${filasVacias.join('')}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;
    }).join('');

    // Ajustar automáticamente la altura de los textareas para ver todo el contenido sin scroll interno
    autoResizeTextareasEnProgramas();
    
    // Configurar scroll horizontal con cursor se hará al final del DOMContentLoaded
    
    // Renderizar controles de paginación
    renderizarPaginacion();
}

function autoResizeTextareasEnProgramas() {
    const textareas = document.querySelectorAll('.concursantes-table textarea.campo-editable');
    const autoResize = (el) => {
        el.style.height = 'auto';
        el.style.overflow = 'hidden';
        el.style.height = `${el.scrollHeight}px`;
    };
    textareas.forEach((ta) => {
        autoResize(ta);
        ta.addEventListener('input', () => autoResize(ta));
        ta.addEventListener('change', () => autoResize(ta));
    });
}


function formatearFechaPrograma(fecha) {
    if (!fecha) return 'N/A';
    
    try {
        // Manejar fecha en formato ISO (YYYY-MM-DD)
        let fechaObj;
        if (fecha.includes('-')) {
            // Formato ISO: 2023-04-01
            const partes = fecha.split('-');
            if (partes.length === 3) {
                const año = parseInt(partes[0]);
                const mes = parseInt(partes[1]) - 1; // Los meses en JavaScript van de 0-11
                const dia = parseInt(partes[2]);
                fechaObj = new Date(año, mes, dia);
            } else {
                fechaObj = new Date(fecha);
            }
        } else {
            fechaObj = new Date(fecha);
        }
        
        // Verificar si la fecha es válida
        if (isNaN(fechaObj.getTime())) {
            return 'Fecha inválida';
        }
        
        const dia = fechaObj.getDate().toString().padStart(2, '0');
        const mes = (fechaObj.getMonth() + 1).toString().padStart(2, '0');
        const año = fechaObj.getFullYear();
        
        return `${dia}/${mes}/${año}`;
    } catch (error) {
        console.error('Error al formatear fecha:', fecha, error);
        return 'Error en fecha';
    }
}

function calcularTotalResultados(concursantes) {
    const total = concursantes.reduce((total, c) => {
        let valor = 0;
        
        // Prioritario: usar el campo premio (numérico) si está disponible
        if (c.premio !== null && c.premio !== undefined && c.premio !== '') {
            valor = parseFloat(c.premio) || 0;
        } 
        // Secundario: extraer números del campo resultado (puede ser string o número)
        else if (c.resultado !== null && c.resultado !== undefined && c.resultado !== '') {
            // Si es un número, usarlo directamente
            if (typeof c.resultado === 'number') {
                valor = c.resultado;
            } 
            // Si es un string, extraer números
            else if (typeof c.resultado === 'string' && c.resultado.trim() !== '') {
                valor = extraerNumeroDeString(c.resultado);
            }
        }
        
        return total + valor;
    }, 0);
    
    // Formatear como moneda
    return total.toLocaleString('es-ES', { 
        minimumFractionDigits: 0, 
        maximumFractionDigits: 0 
    });
}

// Versión numérica para filtros (sin formateo)
function calcularTotalPremiosNumero(concursantes) {
    const total = concursantes.reduce((total, c) => {
        let valor = 0;
        if (c.premio !== null && c.premio !== undefined && c.premio !== '') {
            valor = parseFloat(c.premio) || 0;
        } else if (c.resultado !== null && c.resultado !== undefined && c.resultado !== '') {
            if (typeof c.resultado === 'number') {
                valor = c.resultado;
            } else if (typeof c.resultado === 'string' && c.resultado.trim() !== '') {
                valor = extraerNumeroDeString(c.resultado);
            }
        }
        return total + valor;
    }, 0);
    return Number(total) || 0;
}

function extraerNumeroDeString(texto) {
    if (!texto || typeof texto !== 'string') return 0;
    
    // Buscar todos los números en el texto (incluyendo decimales)
    const numerosEncontrados = texto.match(/\d+(?:[.,]\d+)?/g);
    
    if (!numerosEncontrados || numerosEncontrados.length === 0) return 0;
    
    // Si hay múltiples números, sumar todos
    let total = 0;
    for (const numero of numerosEncontrados) {
        // Convertir comas a puntos para decimales y parsear
        const numeroLimpio = numero.replace(',', '.');
        const valor = parseFloat(numeroLimpio);
        if (!isNaN(valor)) {
            total += valor;
        }
    }
    
    return total;
}

// Función de prueba para validar extracción de números (solo para debug)
function probarExtraccionNumeros() {
    const ejemplos = [
        "Ganó 15000€",
        "15.000 euros",
        "Perdió en la pregunta 3, ganó 5000€",
        "15,500.50€",
        "10.000 + 5.000 = 15.000€",
        "Sin premio: 0€",
        "1500,75 euros",
        "Texto sin números",
        "",
        null
    ];
    
    console.log("=== Prueba de extracción de números ===");
    ejemplos.forEach(ejemplo => {
        const resultado = extraerNumeroDeString(ejemplo);
        console.log(`"${ejemplo}" → ${resultado}`);
    });
}

// Función para obtener la duración prioritaria de un concursante individual
function obtenerDuracionConcursante(concursante) {
    // Lógica de prioridad: primero duracionFinal, luego duracionDireccion, finalmente duracion
    if (concursante.duracionFinal && concursante.duracionFinal.trim() !== '') {
        console.log(`🔍 [PROGRAMAS] Concursante ${concursante.id}: usando duracionFinal = ${concursante.duracionFinal}`);
        return concursante.duracionFinal;
    } else if (concursante.duracionDireccion && concursante.duracionDireccion.trim() !== '') {
        console.log(`🔍 [PROGRAMAS] Concursante ${concursante.id}: usando duracionDireccion = ${concursante.duracionDireccion}`);
        return concursante.duracionDireccion;
    } else if (concursante.duracion && concursante.duracion.trim() !== '') {
        console.log(`🔍 [PROGRAMAS] Concursante ${concursante.id}: usando duracion = ${concursante.duracion}`);
        return concursante.duracion;
    }
    console.log(`🔍 [PROGRAMAS] Concursante ${concursante.id}: ninguna duración disponible`);
    return '';
}

function calcularDuracionReal(concursantes) {
    let totalSegundos = 0;
    
    concursantes.forEach(c => {
        const duracionAUsar = obtenerDuracionConcursante(c);
        
        if (duracionAUsar) {
            const partes = duracionAUsar.split(':');
            if (partes.length === 2) {
                const minutos = parseInt(partes[0]) || 0;
                const segundos = parseInt(partes[1]) || 0;
                totalSegundos += minutos * 60 + segundos;
            }
        }
    });
    
    const horas = Math.floor(totalSegundos / 3600);
    const minutos = Math.floor((totalSegundos % 3600) / 60);
    const segundos = totalSegundos % 60;
    
    if (horas > 0) return `${horas}h ${minutos}m ${segundos}s`;
    if (minutos > 0) return `${minutos}m ${segundos}s`;
    return `${segundos}s`;
}

function calcularGap(duracionObjetivo, duracionReal) {
    const minutosObjetivo = parsearDuracion(duracionObjetivo);
    const minutosReal = parsearDuracion(duracionReal);
    
    const diferencia = minutosReal - minutosObjetivo;
    
    if (diferencia === 0) {
        return 'Perfecto';
    }
    
    // Convertir la diferencia a minutos y segundos
    const diferenciaAbsoluta = Math.abs(diferencia);
    const minutos = Math.floor(diferenciaAbsoluta);
    const segundos = Math.round((diferenciaAbsoluta - minutos) * 60);
    
    let resultado = '';
    if (diferencia > 0) {
        resultado = 'Sobran ';
    } else {
        resultado = 'Faltan ';
    }
    
    if (minutos > 0) {
        resultado += `${minutos}m`;
        if (segundos > 0) {
            resultado += ` ${segundos}s`;
        }
    } else if (segundos > 0) {
        resultado += `${segundos}s`;
    } else {
        resultado += '0s';
    }
    
    return resultado;
}

function parsearDuracion(duracion) {
    if (!duracion) return 0;
    
    let totalMin = 0;
    const horasMatch = duracion.match(/(\d+)h/);
    const minutosMatch = duracion.match(/(\d+)m/);
    const segundosMatch = duracion.match(/(\d+)s/);
    
    if (horasMatch) {
        totalMin += parseInt(horasMatch[1]) * 60;
    }
    if (minutosMatch) {
        totalMin += parseInt(minutosMatch[1]);
    }
    // segundos a fracción de minuto
    if (segundosMatch) {
        totalMin += (parseInt(segundosMatch[1]) || 0) / 60;
    }
    
    return totalMin;
}


async function actualizarCampoConcursante(concursanteId, campo, valor) {
    try {
        const data = {};
        data[campo] = valor;
        
        await apiManager.patch(`/api/concursantes/${concursanteId}/campo`, data);
        mostrarExito('Campo actualizado correctamente');
        
        // Actualizar el concursante en la lista local y encontrar el programa
        let programaId = null;
        for (const pId in concursantesPorPrograma) {
            const concursante = concursantesPorPrograma[pId].find(c => c.id === concursanteId);
            if (concursante) {
                concursante[campo] = valor;
                programaId = pId;
                break;
            }
        }
        
        // Si se actualiza el resultado o cualquier campo de duración, recalcular valores del programa
        if (programaId && (campo === 'resultado' || campo === 'duracion' || campo === 'duracionDireccion' || campo === 'duracionFinal')) {
            const programaContainer = document.querySelector(`[data-programa-id="${programaId}"]`);
            if (programaContainer) {
                const concursantes = concursantesPorPrograma[programaId] || [];
                const duracionReal = calcularDuracionReal(concursantes);
                const programa = programas.find(p => p.id === programaId);
                const duracionObjetivo = programa ? (programa.duracionObjetivo || '1h 5m') : '1h 5m';
                const gap = calcularGap(duracionObjetivo, duracionReal);
                programaContainer.querySelector('.programa-info-item:nth-child(8) .programa-info-readonly').textContent = gap;
                
                // Recalcular total de resultados si se actualiza un resultado
                if (campo === 'resultado') {
                    const nuevoTotalResultados = calcularTotalResultados(concursantes);
                    const premiosElement = programaContainer.querySelector('.programa-info-item:nth-child(6) .programa-info-readonly');
                    if (premiosElement) {
                        premiosElement.textContent = nuevoTotalResultados + '€';
                    }
                }
                
                // Si se actualiza un campo de duración, actualizar la celda de duración en la tabla
                if (campo === 'duracion' || campo === 'duracionDireccion' || campo === 'duracionFinal') {
                    const concursanteActualizado = concursantes.find(c => c.id === concursanteId);
                    if (concursanteActualizado) {
                        const nuevaDuracion = obtenerDuracionConcursante(concursanteActualizado);
                        const filaConcursante = programaContainer.querySelector(`tr[onclick*="${concursanteId}"]`);
                        if (filaConcursante) {
                            const celdaDuracion = filaConcursante.querySelector('.col-duracion');
                            if (celdaDuracion) {
                                celdaDuracion.textContent = nuevaDuracion;
                            }
                        }
                    }
                }
                
                // Actualizar estado del programa automáticamente según los datos
                if (campo === 'resultado' || campo === 'duracion' || campo === 'duracionDireccion' || campo === 'duracionFinal') {
                    await actualizarEstadoProgramaAutomatico(programaId);
                }
            }
        }
        
    } catch (error) {
        mostrarError('Error al actualizar campo: ' + error.message);
    }
}

async function actualizarDuracionObjetivoPrograma(programaId, nuevaDuracion) {
    try {
        // Validar formato de duración (opcional: 1h 5m, 65m, etc.)
        if (!nuevaDuracion || nuevaDuracion.trim() === '') {
            nuevaDuracion = '1h 5m';
        }
        
        // Actualizar en el backend
        await apiManager.patch(`/api/programas/${programaId}/duracion-objetivo`, { duracionObjetivo: nuevaDuracion });
        
        // Actualizar en la lista local
        const programa = programas.find(p => p.id === programaId);
        if (programa) {
            programa.duracionObjetivo = nuevaDuracion;
        }
        
        // Recalcular GAP
        const concursantes = concursantesPorPrograma[programaId] || [];
        const duracionReal = calcularDuracionReal(concursantes);
        const nuevoGap = calcularGap(nuevaDuracion, duracionReal);
        
        // Actualizar en la UI
        const programaContainer = document.querySelector(`[data-programa-id="${programaId}"]`);
        if (programaContainer) {
            // El GAP está en el bloque con label "GAP" dentro de .programa-info-item
            const gapWrapper = Array.from(programaContainer.querySelectorAll('.programa-info-item'))
              .find(item => item.querySelector('.programa-info-label')?.textContent?.trim().toUpperCase() === 'GAP');
            const gapElement = gapWrapper?.querySelector('.programa-info-readonly');
            if (gapElement) gapElement.textContent = nuevoGap;
            // actualizar fecha emisión visible si cambiara por backend (no aplica aquí, pero refrescamos listados)
        }
        
        mostrarExito('Duración objetivo actualizada');
        await recargarProgramas();
        
    } catch (error) {
        mostrarError('Error al actualizar duración objetivo: ' + error.message);
    }
}

function irAConcursante(concursanteId) {
    window.location.href = `concursantes.html?id=${concursanteId}`;
}

// Variables para filtros globales
let programasFiltrados = [];
let aplicandoFiltros = false;

function filtrarProgramas() {
    // Reiniciar a la primera página al aplicar filtros
    paginaActual = 0;
    aplicandoFiltros = true;
    cargarProgramasFiltrados();
}

// Nueva función para cargar programas con filtros aplicados
async function cargarProgramasFiltrados() {
    try {
        // Obtener todos los programas sin paginación
        const response = await apiManager.get('/api/programas');
        const todosLosProgramas = response;
        
        // Aplicar filtros
        programasFiltrados = aplicarFiltros(todosLosProgramas);
        
        // Cargar concursantes para los programas filtrados
        await cargarConcursantesParaProgramasFiltrados();
        
        // Mostrar solo los primeros 5 programas filtrados
        mostrarProgramasFiltrados();
        renderizarPaginacionFiltrada();
        
    } catch (error) {
        if (error && error.message && error.message.startsWith('401')) {
            return;
        }
        mostrarError('Error al cargar programas filtrados: ' + error.message);
    }
}

// Cargar concursantes solo para los programas filtrados
async function cargarConcursantesParaProgramasFiltrados() {
    concursantesPorPrograma = {};
    for (const programa of programasFiltrados) {
        try {
            const concursantes = await apiManager.get(`/api/concursantes/programa/${programa.id}`);
            concursantesPorPrograma[programa.id] = concursantes;
        } catch (e) {
            console.warn(`No se pudieron cargar concursantes para programa ${programa.id}:`, e);
            concursantesPorPrograma[programa.id] = [];
        }
    }
}

// Mostrar programas filtrados con paginación de 5
function mostrarProgramasFiltrados() {
    const inicio = paginaActual * 5;
    const fin = inicio + 5;
    const programasVisibles = programasFiltrados.slice(inicio, fin);
    
    // Actualizar variables globales para la paginación
    programas = programasVisibles;
    totalItems = programasFiltrados.length;
    totalPaginas = Math.ceil(programasFiltrados.length / 5);
    
    mostrarProgramas();
    configurarScrollTablas();
}

// Renderizar paginación para resultados filtrados
function renderizarPaginacionFiltrada() {
    const paginacion = document.getElementById('paginacion-programas');
    if (!paginacion) return;
    
    if (totalPaginas <= 1) {
        paginacion.innerHTML = '';
        return;
    }
    
    let html = '<nav aria-label="Paginación de programas"><ul class="pagination justify-content-center">';
    
    // Botón anterior
    html += `<li class="page-item ${paginaActual === 0 ? 'disabled' : ''}">
        <a class="page-link" href="#" onclick="cambiarPaginaFiltrada(${paginaActual - 1})">Anterior</a>
    </li>`;
    
    // Números de página
    const inicioPagina = Math.max(0, paginaActual - 2);
    const finPagina = Math.min(totalPaginas, inicioPagina + 5);
    
    for (let i = inicioPagina; i < finPagina; i++) {
        html += `<li class="page-item ${i === paginaActual ? 'active' : ''}">
            <a class="page-link" href="#" onclick="cambiarPaginaFiltrada(${i})">${i + 1}</a>
        </li>`;
    }
    
    // Botón siguiente
    html += `<li class="page-item ${paginaActual === totalPaginas - 1 ? 'disabled' : ''}">
        <a class="page-link" href="#" onclick="cambiarPaginaFiltrada(${paginaActual + 1})">Siguiente</a>
    </li>`;
    
    html += '</ul></nav>';
    paginacion.innerHTML = html;
}

// Cambiar página en resultados filtrados
function cambiarPaginaFiltrada(nuevaPagina) {
    if (nuevaPagina < 0 || nuevaPagina >= totalPaginas) return;
    
    paginaActual = nuevaPagina;
    mostrarProgramasFiltrados();
    renderizarPaginacionFiltrada();
}

function aplicarFiltros(lista) {
    const estadoFiltro = (document.getElementById('filtro-estado-programa')?.value || '').toLowerCase();
    const temporadaFiltro = document.getElementById('filtro-temporada')?.value;
    const programaIdFiltro = document.getElementById('filtro-programa-id')?.value;
    const fechaFiltro = document.getElementById('filtro-fecha-emision')?.value; // YYYY-MM-DD
    const premiosMin = document.getElementById('filtro-premios-min')?.value;
    const premiosMax = document.getElementById('filtro-premios-max')?.value;

    return lista.filter(programa => {
        // Estado
        if (estadoFiltro && (!programa.estado || programa.estado.toLowerCase() !== estadoFiltro)) {
            return false;
        }
        // Temporada
        if (temporadaFiltro && String(programa.temporada) !== String(temporadaFiltro)) {
            return false;
        }
        // Nº Programa (ID)
        if (programaIdFiltro && String(programa.id) !== String(programaIdFiltro)) {
            return false;
        }
        // Fecha de emisión exacta (ISO)
        if (fechaFiltro && (!programa.fechaEmision || programa.fechaEmision !== fechaFiltro)) {
            return false;
        }
        // Total de premios (min/máx)
        const concursantes = concursantesPorPrograma[programa.id] || [];
        const totalPremios = calcularTotalPremiosNumero(concursantes);
        if (premiosMin && totalPremios < Number(premiosMin)) {
            return false;
        }
        if (premiosMax && totalPremios > Number(premiosMax)) {
            return false;
        }
        return true;
    });
}

function limpiarFiltrosProgramas() {
    ['filtro-estado-programa','filtro-temporada','filtro-programa-id','filtro-fecha-emision','filtro-premios-min','filtro-premios-max']
        .forEach(id => { const el = document.getElementById(id); if (el) el.value = ''; });
    
    // Volver al modo normal (sin filtros)
    aplicandoFiltros = false;
    programasFiltrados = [];
    paginaActual = 0;
    cargarProgramasPaginados(paginaActual);
}

function mostrarFormularioPrograma() {
    document.getElementById('form-programa').reset();
    document.getElementById('programa-id').value = '';
    document.getElementById('modal-programa-titulo').textContent = 'Nuevo Programa';
    
    const modal = new bootstrap.Modal(document.getElementById('modal-programa'));
    modal.show();
}

async function guardarPrograma() {
    const programaId = document.getElementById('programa-id').value;
    const temporada = document.getElementById('temporada-programa').value;
    const fechaEmision = document.getElementById('fecha-emision').value || null; // Puede ser null
    
    if (!temporada) {
        mostrarError('La temporada es obligatoria');
        return;
    }
    
    const programaData = {
        temporada: parseInt(temporada),
        fechaEmision
    };
    
    try {
        if (programaId) {
            // Actualizar (PUT) y reforzar campos críticos con PATCH
            await apiManager.put(`/api/programas/${programaId}`, programaData);
            await apiManager.patch(`/api/programas/${programaId}/campo`, { temporada: parseInt(temporada) });
            await apiManager.patch(`/api/programas/${programaId}/campo`, { fechaEmision });
            mostrarExito('Programa actualizado correctamente');
        } else {
            const creado = await apiManager.post('/api/programas', programaData);
            mostrarExito('Programa creado correctamente');
        }
        
        bootstrap.Modal.getInstance(document.getElementById('modal-programa')).hide();
        await recargarProgramas();
        
    } catch (error) {
        mostrarError('Error al guardar programa: ' + error.message);
    }
}

// Función para abrir el explorador de archivos para seleccionar foto
function abrirExploradorFoto(concursanteId, event) {
    // Detener la propagación del evento para evitar que se active el click del row
    if (event) {
        event.stopPropagation();
        event.preventDefault();
    }
    
    // Crear input file dinámicamente
    const inputFile = document.createElement('input');
    inputFile.type = 'file';
    inputFile.accept = 'image/*';
    inputFile.style.display = 'none';
    
    inputFile.onchange = function(event) {
        const file = event.target.files[0];
        if (file) {
            subirFotoConcursante(concursanteId, file);
        }
        // Limpiar el input después de usar
        document.body.removeChild(inputFile);
    };
    
    // Añadir al DOM y hacer click
    document.body.appendChild(inputFile);
    inputFile.click();
}

// Función para subir la foto del concursante
async function subirFotoConcursante(concursanteId, file) {
    try {
        // Mostrar indicador de carga
        mostrarMensaje('Subiendo foto...', 'info');
        
        // Crear FormData para enviar el archivo
        const formData = new FormData();
        formData.append('foto', file);
        
        // Subir la foto
        const response = await fetch(`/api/concursantes/${concursanteId}/foto`, {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            },
            body: formData
        });
        
        if (!response.ok) {
            throw new Error('Error al subir la foto');
        }
        
        const resultado = await response.json();
        
        // Actualizar la vista
        await recargarProgramas();
        mostrarMensaje('Foto subida correctamente', 'success');
        
    } catch (error) {
        console.error('Error al subir foto:', error);
        mostrarError('Error al subir la foto: ' + error.message);
    }
}

// Función para mostrar mensajes
function mostrarMensaje(mensaje, tipo = 'info') {
    // Crear elemento de mensaje
    const mensajeDiv = document.createElement('div');
    mensajeDiv.className = `alert alert-${tipo === 'success' ? 'success' : tipo === 'error' ? 'danger' : 'info'} alert-dismissible fade show`;
    mensajeDiv.style.position = 'fixed';
    mensajeDiv.style.top = '20px';
    mensajeDiv.style.right = '20px';
    mensajeDiv.style.zIndex = '9999';
    mensajeDiv.style.minWidth = '300px';
    
    mensajeDiv.innerHTML = `
        ${mensaje}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    document.body.appendChild(mensajeDiv);
    
    // Auto-eliminar después de 3 segundos
    setTimeout(() => {
        if (mensajeDiv.parentNode) {
            mensajeDiv.parentNode.removeChild(mensajeDiv);
        }
    }, 3000);
}

// Función para actualizar el estado del programa automáticamente
async function actualizarEstadoProgramaAutomatico(programaId) {
    try {
        await apiManager.put(`/api/programas/${programaId}/actualizar-estado`);
        await cargarProgramas();
    } catch (error) {
        console.error('Error al actualizar estado del programa:', error);
    }
}

async function actualizarEstadoPrograma(programaId, nuevoEstado) {
    try {
        await apiManager.patch(`/api/programas/${programaId}/campo`, { estado: nuevoEstado });
        await cargarProgramas();
        mostrarExito('Estado del programa actualizado');
    } catch (error) {
        mostrarError('Error al actualizar el estado: ' + error.message);
    }
}

function renderOpcionEstado(estadoActual, valor, texto) {
    const selected = estadoActual === valor ? 'selected' : '';
    return `<option value="${valor}" ${selected}>${texto}</option>`;
}

async function actualizarTemporadaPrograma(programaId, nuevaTemporada) {
    try {
        const numero = parseInt(nuevaTemporada);
        if (!numero || numero < 1) {
            mostrarError('La temporada debe ser un número mayor o igual a 1');
            return;
        }
        await apiManager.patch(`/api/programas/${programaId}/campo`, { temporada: numero });
        mostrarExito('Temporada actualizada');
        await cargarProgramas();
    } catch (error) {
        mostrarError('No se pudo actualizar la temporada');
    }
}

async function actualizarFechaEmision(programaId, fechaISO) {
    try {
        const valor = (fechaISO && fechaISO.trim() !== '') ? fechaISO : null;
        await apiManager.patch(`/api/programas/${programaId}/campo`, { fechaEmision: valor });
        mostrarExito('Fecha de emisión actualizada');
        await cargarProgramas();
    } catch (error) {
        mostrarError('No se pudo actualizar la fecha de emisión');
    }
}

// Funciones para gestión de concursantes
let concursantesDisponibles = [];
let totalConcursantesDisponibles = 0;
let paginaConcursantesDisponibles = 0;
let totalPaginasConcursantesDisponibles = 1;
let debounceTimer = null;

async function mostrarConcursantesDisponibles(programaId) {
    try {
        document.getElementById('programa-seleccionado-id').value = programaId;
        
        // Limpiar filtro de búsqueda
        document.getElementById('buscar-concursante-disponible').value = '';
        
        // Cargar concursantes disponibles con paginación (10 por página)
        const response = await apiManager.get('/api/concursantes/disponibles?page=0&size=10');
        concursantesDisponibles = response.content || [];
        totalConcursantesDisponibles = response.totalElements || 0;
        paginaConcursantesDisponibles = 0;
        totalPaginasConcursantesDisponibles = response.totalPages || 1;
        
        renderizarConcursantesDisponibles();
        
        const modal = new bootstrap.Modal(document.getElementById('modal-añadir-concursantes'));
        modal.show();
    } catch (error) {
        mostrarError('Error al cargar concursantes disponibles: ' + error.message);
    }
}

function renderizarConcursantesDisponibles() {
    const lista = document.getElementById('lista-concursantes-disponibles');
    
    if (concursantesDisponibles.length === 0) {
        lista.innerHTML = '<div class="alert alert-info">No hay concursantes disponibles sin asignar a programas.</div>';
        return;
    }
    
    // Mostrar información de paginación
    const infoPaginacion = document.getElementById('info-paginacion-concursantes');
    if (infoPaginacion) {
        infoPaginacion.innerHTML = `Mostrando ${concursantesDisponibles.length} de ${totalConcursantesDisponibles} concursantes (Página ${paginaConcursantesDisponibles + 1} de ${totalPaginasConcursantesDisponibles})`;
    }
    
    lista.innerHTML = `
        <div class="table-responsive">
            <table class="table table-hover">
                <thead>
                    <tr>
                        <th>Nombre</th>
                        <th>Edad</th>
                        <th>Ocupación</th>
                        <th>Lugar</th>
                        <th>Estado</th>
                        <th>Acción</th>
                    </tr>
                </thead>
                <tbody>
                    ${concursantesDisponibles.map(concursante => `
                        <tr>
                            <td><strong>${concursante.nombre || ''}</strong></td>
                            <td>${concursante.edad || ''}</td>
                            <td>${concursante.ocupacion || ''}</td>
                            <td>${concursante.lugar || ''}</td>
                            <td>
                                <span class="badge bg-success">${concursante.estado || 'Disponible'}</span>
                            </td>
                            <td>
                                <button class="btn btn-sm btn-primary" onclick="asignarConcursanteAPrograma(${concursante.id})">
                                    <i class="fas fa-plus"></i> Añadir
                                </button>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;
}

// Función de debounce para filtrar concursantes
function debounceFiltrarConcursantes() {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
        filtrarConcursantesDisponibles();
    }, 500); // Esperar 500ms después del último tecleo
}

async function filtrarConcursantesDisponibles() {
    const filtro = document.getElementById('buscar-concursante-disponible').value.trim();
    
    try {
        // Resetear a la primera página cuando se filtra
        paginaConcursantesDisponibles = 0;
        
        // Construir URL con filtro
        let url = '/api/concursantes/disponibles?page=0&size=10';
        if (filtro) {
            url += `&busqueda=${encodeURIComponent(filtro)}`;
        }
        
        const response = await apiManager.get(url);
        concursantesDisponibles = response.content || [];
        totalConcursantesDisponibles = response.totalElements || 0;
        totalPaginasConcursantesDisponibles = response.totalPages || 1;
        
        renderizarConcursantesDisponibles();
    } catch (error) {
        mostrarError('Error al filtrar concursantes: ' + error.message);
    }
}

async function asignarConcursanteAPrograma(concursanteId) {
    try {
        const programaId = document.getElementById('programa-seleccionado-id').value;
        
        await apiManager.post(`/api/concursantes/${concursanteId}/asignar-programa/${programaId}`);
        
        // Cerrar modal
        const modal = bootstrap.Modal.getInstance(document.getElementById('modal-añadir-concursantes'));
        modal.hide();
        
        // Recargar programas
        await recargarProgramas();
        
        mostrarMensaje('Concursante añadido al programa correctamente', 'success');
    } catch (error) {
        mostrarError('Error al asignar concursante: ' + error.message);
    }
}

async function quitarConcursanteDePrograma(concursanteId, event) {
    event.stopPropagation();
    
    if (!confirm('¿Estás seguro de que quieres quitar este concursante del programa?')) {
        return;
    }
    
    try {
        await apiManager.delete(`/api/concursantes/${concursanteId}/desasignar-programa`);
        
        // Recargar programas
        await recargarProgramas();
        
        mostrarMensaje('Concursante quitado del programa correctamente', 'success');
    } catch (error) {
        mostrarError('Error al quitar concursante: ' + error.message);
    }
}

async function editarPrograma(programaId) {
    try {
        const programa = await apiManager.get(`/api/programas/${programaId}`);
        document.getElementById('programa-id').value = programa.id;
        document.getElementById('temporada-programa').value = programa.temporada;
        document.getElementById('fecha-emision').value = programa.fechaEmision || '';
        document.getElementById('modal-programa-titulo').textContent = 'Editar Programa';

        const modal = new bootstrap.Modal(document.getElementById('modal-programa'));
        modal.show();
    } catch (error) {
        mostrarError('Error al cargar datos del programa para editar: ' + error.message);
    }
}

// Función para renderizar los controles de paginación
function renderizarPaginacion() {
    const paginacionDiv = document.getElementById('paginacion-programas');
    if (!paginacionDiv) return;
    
    let html = '';
    
    // Botón Anterior
    html += `<button class="btn btn-outline-primary" ${paginaActual <= 0 ? 'disabled' : ''} 
            onclick="cambiarPagina(${paginaActual - 1})">
            <i class="fas fa-chevron-left"></i> Anterior
        </button>`;
    
    // Botón Siguiente
    html += `<button class="btn btn-outline-primary" ${paginaActual >= totalPaginas - 1 ? 'disabled' : ''} 
            onclick="cambiarPagina(${paginaActual + 1})">
            Siguiente <i class="fas fa-chevron-right"></i>
        </button>`;
    
    paginacionDiv.innerHTML = html;
}

// Función para cambiar de página
async function cambiarPagina(nuevaPagina) {
    if (nuevaPagina < 0 || nuevaPagina >= totalPaginas) return;
    
    paginaActual = nuevaPagina;
    
    if (aplicandoFiltros) {
        // Si estamos aplicando filtros, usar la paginación filtrada
        mostrarProgramasFiltrados();
        renderizarPaginacionFiltrada();
    } else {
        // Modo normal sin filtros
        await cargarProgramasPaginados(paginaActual);
    }
}

document.addEventListener('DOMContentLoaded', inicializarProgramas); 

async function eliminarPrograma(programaId) {
    if (!confirm('¿Seguro que deseas borrar este programa? Esta acción no se puede deshacer.')) return;
    try {
        await apiManager.delete(`/api/programas/${programaId}`);
        mostrarExito('Programa eliminado');
        await recargarProgramas();
    } catch (error) {
        // Traducir mensajes técnicos a mensajes entendibles
        const msg = (error && error.message) ? error.message : '';
        let amigable = 'No se pudo eliminar el programa.';
        if (msg.includes('403')) {
            amigable = 'No tienes permisos para eliminar programas.';
        } else if (msg.includes('No se puede eliminar el programa') || msg.toLowerCase().includes('concursante')) {
            amigable = 'No se puede eliminar porque hay concursantes asignados. Desasigna los concursantes primero.';
        } else if (msg.toLowerCase().includes('programado')) {
            amigable = 'No se puede eliminar un programa programado. Cambia su estado a Borrador primero.';
        } else if (msg.toLowerCase().includes('emitido')) {
            amigable = 'No se puede eliminar un programa emitido.';
        } else if (msg.includes('400') && msg.toLowerCase().includes('parameter value')) {
            amigable = 'No se pudo eliminar por un problema interno. Vuelve a intentarlo.';
        }
        mostrarError(amigable);
    }
}

// Función para configurar scroll automático en las tablas de concursantes
function configurarScrollTablas() {
    console.log('🔧 [SCROLL] Configurando scroll en tablas existentes...');
    
    // Buscar todos los contenedores de tablas de concursantes
    const contenedores = document.querySelectorAll('.concursantes-table');
    console.log(`🔧 [SCROLL] Encontrados ${contenedores.length} contenedores de tablas`);
    
    contenedores.forEach((contenedor, index) => {
        console.log(`🔧 [SCROLL] Configurando scroll para tabla ${index + 1}`);
        configurarScrollEnTabla(contenedor);
    });
}

// Función para configurar scroll en una tabla específica
function configurarScrollEnTabla(contenedor) {
    if (!contenedor) {
        console.error('❌ [SCROLL] Contenedor no válido');
        return;
    }
    
    // Evitar configurar múltiples veces la misma tabla
    if (contenedor.dataset.scrollConfigurado === 'true') {
        console.log('🔧 [SCROLL] Tabla ya configurada, saltando...');
        return;
    }
    
    // Buscar el contenedor real del scroll (table-responsive dentro de concursantes-table)
    const scrollContainer = contenedor.querySelector('.table-responsive');
    if (!scrollContainer) {
        console.error('❌ [SCROLL] No se encontró .table-responsive dentro del contenedor');
        return;
    }
    
    console.log('✅ [SCROLL] Configurando scroll para tabla:', contenedor);
    console.log('✅ [SCROLL] Contenedor de scroll real:', scrollContainer);
    console.log(`📏 [SCROLL] Dimensiones: Width=${scrollContainer.scrollWidth}, Client=${scrollContainer.clientWidth}, ScrollLeft=${scrollContainer.scrollLeft}`);
    
    // OPCIÓN 1: Scroll automático con cursor (mejorado)
    let scrollInterval = null;
    let scrollDirection = null;
    
    contenedor.addEventListener('mousemove', function(e) {
        const borde = 100; // px desde el borde para activar scroll (aumentado)
        const { left, right } = contenedor.getBoundingClientRect();
        const x = e.clientX;
        const scrollSpeed = 20; // px por frame (aumentado para más velocidad)
        clearInterval(scrollInterval);
        
        // Debug: mostrar información del cursor
        const distanciaIzquierda = x - left;
        const distanciaDerecha = right - x;
        console.log(`🖱️ [CURSOR] Posición: ${x}, Izq: ${distanciaIzquierda}px, Der: ${distanciaDerecha}px, Borde: ${borde}px`);
        
        if (x - left < borde) {
            // Scroll hacia la izquierda (mostrar columnas ocultas de la izquierda)
            console.log('🔄 [SCROLL] Activando scroll hacia la izquierda');
            scrollDirection = 'left';
            contenedor.style.cursor = 'w-resize';
            contenedor.classList.add('scrolling-left');
            contenedor.classList.remove('scrolling-right');
            scrollInterval = setInterval(() => {
                const oldScroll = scrollContainer.scrollLeft;
                const maxScroll = scrollContainer.scrollWidth - scrollContainer.clientWidth;
                scrollContainer.scrollLeft -= scrollSpeed;
                console.log(`🔄 [SCROLL] Scroll: ${oldScroll} → ${scrollContainer.scrollLeft} (Max: ${maxScroll}, Width: ${scrollContainer.scrollWidth}, Client: ${scrollContainer.clientWidth})`);
            }, 16);
        } else if (right - x < borde) {
            // Scroll hacia la derecha (mostrar columnas ocultas de la derecha)
            console.log('🔄 [SCROLL] Activando scroll hacia la derecha');
            scrollDirection = 'right';
            contenedor.style.cursor = 'e-resize';
            contenedor.classList.add('scrolling-right');
            contenedor.classList.remove('scrolling-left');
            scrollInterval = setInterval(() => {
                const oldScroll = scrollContainer.scrollLeft;
                const maxScroll = scrollContainer.scrollWidth - scrollContainer.clientWidth;
                scrollContainer.scrollLeft += scrollSpeed;
                console.log(`🔄 [SCROLL] Scroll: ${oldScroll} → ${scrollContainer.scrollLeft} (Max: ${maxScroll}, Width: ${scrollContainer.scrollWidth}, Client: ${scrollContainer.clientWidth})`);
            }, 16);
        } else {
            // Cursor normal cuando no está en los bordes
            contenedor.style.cursor = 'default';
            contenedor.classList.remove('scrolling-left', 'scrolling-right');
            scrollDirection = null;
        }
    });
    
    contenedor.addEventListener('mouseleave', function() {
        clearInterval(scrollInterval);
        contenedor.style.cursor = 'default';
        contenedor.classList.remove('scrolling-left', 'scrolling-right');
        scrollDirection = null;
    });
    
    // OPCIÓN 2: Botones de navegación
    crearBotonesNavegacion(contenedor, scrollContainer);
    
    // OPCIÓN 3: Scroll con rueda del mouse (horizontal) - DESHABILITADO
    // contenedor.addEventListener('wheel', function(e) {
    //     if (e.deltaY !== 0) {
    //         e.preventDefault();
    //         scrollContainer.scrollLeft += e.deltaY;
    //     }
    // });
    
    // OPCIÓN 4: Scroll con teclado
    contenedor.addEventListener('keydown', function(e) {
        if (e.key === 'ArrowLeft') {
            e.preventDefault();
            scrollContainer.scrollLeft -= 50;
        } else if (e.key === 'ArrowRight') {
            e.preventDefault();
            scrollContainer.scrollLeft += 50;
        }
    });
    
    // Hacer el contenedor focusable para el scroll con teclado
    contenedor.setAttribute('tabindex', '0');
    
    // Marcar como configurado para evitar configuraciones múltiples
    contenedor.dataset.scrollConfigurado = 'true';
    
    console.log('✅ [SCROLL] Todas las opciones de scroll configuradas para esta tabla');
}

// Función para crear botones de navegación
function crearBotonesNavegacion(contenedor, scrollContainer) {
    console.log('🔧 [BOTONES] Creando botones de navegación...');
    
    // Crear contenedor de botones
    const botonesContainer = document.createElement('div');
    botonesContainer.className = 'd-flex justify-content-center gap-2 mb-2';
    botonesContainer.style.marginTop = '10px';
    
    // Botón izquierda
    const btnIzquierda = document.createElement('button');
    btnIzquierda.className = 'btn btn-outline-primary btn-sm';
    btnIzquierda.innerHTML = '<i class="fas fa-chevron-left"></i> ←';
    btnIzquierda.title = 'Desplazar hacia la izquierda';
    btnIzquierda.onclick = () => {
        scrollContainer.scrollLeft -= 200;
    };
    
    // Botón derecha
    const btnDerecha = document.createElement('button');
    btnDerecha.className = 'btn btn-outline-primary btn-sm';
    btnDerecha.innerHTML = '→ <i class="fas fa-chevron-right"></i>';
    btnDerecha.title = 'Desplazar hacia la derecha';
    btnDerecha.onclick = () => {
        scrollContainer.scrollLeft += 200;
    };
    
    // Botón inicio
    const btnInicio = document.createElement('button');
    btnInicio.className = 'btn btn-outline-secondary btn-sm';
    btnInicio.innerHTML = '<i class="fas fa-home"></i> Inicio';
    btnInicio.title = 'Ir al inicio de la tabla';
    btnInicio.onclick = () => {
        scrollContainer.scrollLeft = 0;
    };
    
    // Botón final
    const btnFinal = document.createElement('button');
    btnFinal.className = 'btn btn-outline-secondary btn-sm';
    btnFinal.innerHTML = 'Final <i class="fas fa-home"></i>';
    btnFinal.title = 'Ir al final de la tabla';
    btnFinal.onclick = () => {
        scrollContainer.scrollLeft = scrollContainer.scrollWidth;
    };
    
    // Agregar botones al contenedor
    botonesContainer.appendChild(btnInicio);
    botonesContainer.appendChild(btnIzquierda);
    botonesContainer.appendChild(btnDerecha);
    botonesContainer.appendChild(btnFinal);
    
    // Insertar botones después del contenedor de la tabla
    contenedor.parentNode.insertBefore(botonesContainer, contenedor.nextSibling);
    
    console.log('✅ [BOTONES] Botones de navegación creados');
}

// Función para cargar más concursantes disponibles
async function cargarMasConcursantesDisponibles() {
    if (paginaConcursantesDisponibles >= totalPaginasConcursantesDisponibles - 1) {
        return; // Ya estamos en la última página
    }
    
    try {
        const siguientePagina = paginaConcursantesDisponibles + 1;
        const filtro = document.getElementById('buscar-concursante-disponible').value.trim();
        
        // Construir URL con filtro si existe
        let url = `/api/concursantes/disponibles?page=${siguientePagina}&size=10`;
        if (filtro) {
            url += `&busqueda=${encodeURIComponent(filtro)}`;
        }
        
        const response = await apiManager.get(url);
        
        // Agregar nuevos concursantes a la lista existente
        concursantesDisponibles = [...concursantesDisponibles, ...(response.content || [])];
        paginaConcursantesDisponibles = siguientePagina;
        totalPaginasConcursantesDisponibles = response.totalPages || 1;
        
        renderizarConcursantesDisponibles();
    } catch (error) {
        mostrarError('Error al cargar más concursantes: ' + error.message);
    }
}

// Función para cargar página específica de concursantes disponibles
async function cargarPaginaConcursantesDisponibles(pagina) {
    try {
        const filtro = document.getElementById('buscar-concursante-disponible').value.trim();
        
        // Construir URL con filtro si existe
        let url = `/api/concursantes/disponibles?page=${pagina}&size=10`;
        if (filtro) {
            url += `&busqueda=${encodeURIComponent(filtro)}`;
        }
        
        const response = await apiManager.get(url);
        concursantesDisponibles = response.content || [];
        paginaConcursantesDisponibles = pagina;
        totalPaginasConcursantesDisponibles = response.totalPages || 1;
        
        renderizarConcursantesDisponibles();
    } catch (error) {
        mostrarError('Error al cargar página de concursantes: ' + error.message);
    }
}