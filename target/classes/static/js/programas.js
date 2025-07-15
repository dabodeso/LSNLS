let programas = [];
let concursantesPorPrograma = {};
let duracionObjetivoGlobal = '1h 5m'; // Variable global para la duración objetivo

async function inicializarProgramas() {
    // Mostrar enlace de administración solo para admins
    const usuario = JSON.parse(localStorage.getItem('usuario'));
    if (usuario && usuario.rol === 'ROLE_ADMIN') {
        const navAdmin = document.getElementById('nav-admin');
        if (navAdmin) {
            navAdmin.style.display = 'block';
        }
    }
    
    await cargarDuracionObjetivo();
    await cargarProgramas();
}

async function cargarDuracionObjetivo() {
    try {
        const duracion = await apiManager.get('/api/configuracion/duracion-objetivo');
        duracionObjetivoGlobal = duracion || '1h 5m';
    } catch (error) {
        console.warn('No se pudo cargar duración objetivo, usando valor por defecto');
        duracionObjetivoGlobal = '1h 5m';
    }
}

async function cargarProgramas() {
    try {
        programas = await apiManager.get('/api/programas');
        await cargarConcursantesPorPrograma();
        mostrarProgramas();
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
    
    contenedor.innerHTML = programas.map(programa => {
        const concursantes = concursantesPorPrograma[programa.id] || [];
        const fechaFormateada = formatearFechaPrograma(programa.fechaEmision);
        const totalResultados = calcularTotalResultados(concursantes);
        const duracionReal = calcularDuracionReal(concursantes);
        const gap = calcularGap(duracionObjetivoGlobal, duracionReal);
        
        // Definir colores para estados
        const estadoColores = {
            'borrador': '#6c757d',     // Gris
            'grabado': '#17a2b8',      // Azul 
            'editado': '#ffc107',      // Amarillo
            'programado': '#28a745',   // Verde
            'emitido': '#dc3545'       // Rojo
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
                            <div class="programa-info-value">T${programa.temporada || '?'}</div>
                        </div>
                        <div class="programa-info-item">
                            <div class="programa-info-label">Programa</div>
                            <div class="programa-info-value">${programa.id}</div>
                        </div>
                        <div class="programa-info-item">
                            <div class="programa-info-label">Estado</div>
                            <div class="programa-info-value">
                                <span style="background-color: ${estadoColor}; color: white; padding: 4px 8px; border-radius: 12px; font-size: 0.85em; text-transform: uppercase;">
                                    ${programa.estado || 'BORRADOR'}
                                </span>
                            </div>
                        </div>
                        <div class="programa-info-item">
                            <div class="programa-info-label">Fecha</div>
                            <div class="programa-info-value">${fechaFormateada}</div>
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
                                <span class="programa-info-readonly">${duracionObjetivoGlobal}</span>
                            </div>
                        </div>
                        <div class="programa-info-item">
                            <div class="programa-info-label">GAP</div>
                            <div class="programa-info-value">
                                <span class="programa-info-readonly">${gap}</span>
                            </div>
                        </div>
                        <div class="programa-info-item">
                            <div class="programa-info-label">Total Concursantes</div>
                            <div class="programa-info-value">
                                <span class="programa-info-readonly">${concursantes.length}</span>
                            </div>
                        </div>
                        <div class="programa-info-item">
                            <button class="btn btn-sm btn-success" onclick="mostrarConcursantesDisponibles(${programa.id})" title="Añadir concursante">
                                <i class="fas fa-user-plus"></i> Añadir
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
                                    <th class="col-momentos">MOMENTOS DESTACADOS</th>
                                    <th class="col-factor-x">X</th>
                                    <th class="col-valoracion">VAL</th>
                                    <th class="col-creditos">CRÉDITOS ESPECIALES</th>
                                    <th style="width: 5%;">ACCIONES</th>
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
                                        <td class="col-duracion">${concursante.duracion || ''}</td>
                                        <td class="col-foto">
                                            ${concursante.foto ? 
                                                `<img src="${concursante.foto}" class="foto-concursante" alt="Foto" onclick="abrirExploradorFoto(${concursante.id}, event)" title="Click para cambiar foto">` : 
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
}

function formatearFechaPrograma(fecha) {
    if (!fecha) return 'Sin fecha';
    
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
        
        const diasSemana = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
        const diaSemana = diasSemana[fechaObj.getDay()];
        
        const dia = fechaObj.getDate().toString().padStart(2, '0');
        const mes = (fechaObj.getMonth() + 1).toString().padStart(2, '0');
        const año = fechaObj.getFullYear();
        
        return `${diaSemana}, ${dia}/${mes}/${año}`;
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
        // Secundario: extraer números del campo resultado (string)
        else if (c.resultado && c.resultado.trim() !== '') {
            valor = extraerNumeroDeString(c.resultado);
        }
        
        return total + valor;
    }, 0);
    
    // Formatear como moneda
    return total.toLocaleString('es-ES', { 
        minimumFractionDigits: 0, 
        maximumFractionDigits: 0 
    });
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

function calcularDuracionReal(concursantes) {
    let totalMinutos = 0;
    
    concursantes.forEach(c => {
        if (c.duracion) {
            const partes = c.duracion.split(':');
            if (partes.length === 2) {
                totalMinutos += parseInt(partes[0]) || 0;
                totalMinutos += (parseInt(partes[1]) || 0) / 60;
            }
        }
    });
    
    const horas = Math.floor(totalMinutos / 60);
    const minutos = Math.round(totalMinutos % 60);
    
    if (horas > 0) {
        return `${horas}h ${minutos}m`;
    } else {
        return `${minutos}m`;
    }
}

function calcularGap(duracionObjetivo, duracionReal) {
    const minutosObjetivo = parsearDuracion(duracionObjetivo);
    const minutosReal = parsearDuracion(duracionReal);
    
    const diferencia = minutosReal - minutosObjetivo;
    
    if (diferencia > 0) {
        return `+${Math.round(diferencia)}m`;
    } else if (diferencia < 0) {
        return `${Math.round(diferencia)}m`;
    } else {
        return '0m';
    }
}

function parsearDuracion(duracion) {
    if (!duracion) return 0;
    
    let minutos = 0;
    const horasMatch = duracion.match(/(\d+)h/);
    const minutosMatch = duracion.match(/(\d+)m/);
    
    if (horasMatch) {
        minutos += parseInt(horasMatch[1]) * 60;
    }
    if (minutosMatch) {
        minutos += parseInt(minutosMatch[1]);
    }
    
    return minutos;
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
        
        // Si se actualiza el resultado o la duración, recalcular valores del programa
        if (programaId && (campo === 'resultado' || campo === 'duracion')) {
            const programaContainer = document.querySelector(`[data-programa-id="${programaId}"]`);
            if (programaContainer) {
                const concursantes = concursantesPorPrograma[programaId] || [];
                const duracionReal = calcularDuracionReal(concursantes);
                const gap = calcularGap(duracionObjetivoGlobal, duracionReal);
                programaContainer.querySelector('.programa-info-item:nth-child(8) .programa-info-readonly').textContent = gap;
                
                // Recalcular total de resultados si se actualiza un resultado
                if (campo === 'resultado') {
                    const nuevoTotalResultados = calcularTotalResultados(concursantes);
                    const premiosElement = programaContainer.querySelector('.programa-info-item:nth-child(6) .programa-info-readonly');
                    if (premiosElement) {
                        premiosElement.textContent = nuevoTotalResultados + '€';
                    }
                }
                
                // Actualizar estado del programa automáticamente según los datos
                if (campo === 'resultado' || campo === 'duracion') {
                    await actualizarEstadoProgramaAutomatico(programaId);
                }
            }
        }
        
    } catch (error) {
        mostrarError('Error al actualizar campo: ' + error.message);
    }
}

function irAConcursante(concursanteId) {
    window.location.href = `concursantes.html?id=${concursanteId}`;
}

function filtrarProgramas() {
    const estadoFiltro = document.getElementById('filtro-estado-programa').value.toLowerCase();
    const busquedaFiltro = document.getElementById('buscar-programa').value.toLowerCase();
    
    const programasVisibles = programas.filter(programa => {
        const cumpleEstado = !estadoFiltro || (programa.estado && programa.estado.toLowerCase() === estadoFiltro);
        const cumpleBusqueda = !busquedaFiltro || 
            programa.id.toString().includes(busquedaFiltro) ||
            (programa.fechaEmision && programa.fechaEmision.includes(busquedaFiltro));
        
        return cumpleEstado && cumpleBusqueda;
    });
    
    // Ocultar/mostrar programas según filtros
    document.querySelectorAll('.programa-container').forEach(container => {
        const programaId = parseInt(container.getAttribute('data-programa-id'));
        const esVisible = programasVisibles.some(p => p.id === programaId);
        container.style.display = esVisible ? 'block' : 'none';
    });
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
        // El estado se asignará automáticamente en el backend
    };
    
    try {
        if (programaId) {
            await apiManager.put(`/api/programas/${programaId}`, programaData);
            mostrarExito('Programa actualizado correctamente');
        } else {
            await apiManager.post('/api/programas', programaData);
            mostrarExito('Programa creado correctamente');
        }
        
        bootstrap.Modal.getInstance(document.getElementById('modal-programa')).hide();
        await cargarProgramas();
        
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
        await cargarProgramas();
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
        await cargarProgramas(); // Recargar para mostrar el estado actualizado
    } catch (error) {
        console.error('Error al actualizar estado del programa:', error);
    }
}

// Funciones para gestión de concursantes
let concursantesDisponibles = [];

async function mostrarConcursantesDisponibles(programaId) {
    try {
        document.getElementById('programa-seleccionado-id').value = programaId;
        concursantesDisponibles = await apiManager.get('/api/concursantes/disponibles');
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

function filtrarConcursantesDisponibles() {
    const filtro = document.getElementById('buscar-concursante-disponible').value.toLowerCase();
    
    if (!filtro) {
        renderizarConcursantesDisponibles();
        return;
    }
    
    const concursantesFiltrados = concursantesDisponibles.filter(concursante => 
        (concursante.nombre && concursante.nombre.toLowerCase().includes(filtro)) ||
        (concursante.ocupacion && concursante.ocupacion.toLowerCase().includes(filtro)) ||
        (concursante.lugar && concursante.lugar.toLowerCase().includes(filtro))
    );
    
    const lista = document.getElementById('lista-concursantes-disponibles');
    
    if (concursantesFiltrados.length === 0) {
        lista.innerHTML = '<div class="alert alert-warning">No se encontraron concursantes que coincidan con la búsqueda.</div>';
        return;
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
                    ${concursantesFiltrados.map(concursante => `
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

async function asignarConcursanteAPrograma(concursanteId) {
    try {
        const programaId = document.getElementById('programa-seleccionado-id').value;
        
        await apiManager.post(`/api/concursantes/${concursanteId}/asignar-programa/${programaId}`);
        
        // Cerrar modal
        const modal = bootstrap.Modal.getInstance(document.getElementById('modal-añadir-concursantes'));
        modal.hide();
        
        // Recargar programas
        await cargarProgramas();
        
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
        await cargarProgramas();
        
        mostrarMensaje('Concursante quitado del programa correctamente', 'success');
    } catch (error) {
        mostrarError('Error al quitar concursante: ' + error.message);
    }
}

document.addEventListener('DOMContentLoaded', inicializarProgramas); 