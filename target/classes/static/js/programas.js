let programas = [];
let concursantesPorPrograma = {};

let rolUsuarioActual = null;

function puedeEditarCamposPrograma() {
    return ['ROLE_ADMIN', 'ROLE_VERIFICACION', 'ROLE_DIRECCION'].includes(rolUsuarioActual);
}

function puedeCrearPrograma() {
    return ['ROLE_ADMIN', 'ROLE_VERIFICACION', 'ROLE_DIRECCION'].includes(rolUsuarioActual);
}

function puedeEliminarPrograma() {
    return ['ROLE_ADMIN', 'ROLE_DIRECCION'].includes(rolUsuarioActual);
}

function puedeGestionarConcursantesPrograma() {
    return ['ROLE_ADMIN', 'ROLE_DIRECCION'].includes(rolUsuarioActual);
}

function puedeEditarCamposConcursante() {
    return ['ROLE_ADMIN', 'ROLE_DIRECCION'].includes(rolUsuarioActual);
}

// Valoraciones permitidas (guionista/dirección) - solo front
const VALORACIONES_PERMITIDAS = ['1', '1+', '2-', '2', '2+', '3-', '3', '3+'];

const TAMANO_MAX_FOTO_BYTES = 10 * 1024 * 1024; // 10MB

function obtenerMensajeErrorProgramas(error, accion = 'operación') {
    const raw = String(error?.message || error || '');
    const m = raw.match(/^(\d{3}):\s*([\s\S]*)$/);
    const status = m ? Number(m[1]) : null;
    const detalle = (m ? m[2] : raw).trim();
    const d = detalle.toLowerCase();

    if (d.includes('maximum upload size exceeded') || d.includes('filesizelimitexceededexception') || status === 413) {
        return 'La foto es demasiado grande. El tamaño máximo permitido es 10MB.';
    }
    if (d.includes('unauthorized') || status === 401) {
        return 'Tu sesión ha expirado. Vuelve a iniciar sesión.';
    }
    if (status === 403 || d.includes('forbidden') || d.includes('no tienes permisos')) {
        return 'No tienes permisos para realizar esta acción.';
    }
    if (status === 404 || d.includes('no encontrado')) {
        return 'No se ha encontrado el recurso solicitado.';
    }
    if (status === 409 || d.includes('conflicto') || d.includes('ocupad')) {
        return detalle || 'Hay un conflicto con los datos actuales. Recarga e inténtalo de nuevo.';
    }
    if (status === 400 || d.includes('validaci') || d.includes('inválid') || d.includes('inval')) {
        return detalle || 'Los datos no son válidos. Revisa los campos e inténtalo de nuevo.';
    }
    if (d.includes('failed to fetch') || d.includes('networkerror') || d.includes('network request failed')) {
        return 'Error de conexión con el servidor. Revisa tu red e inténtalo de nuevo.';
    }
    if (status && status >= 500) {
        return 'Error interno del servidor. Inténtalo de nuevo en unos minutos.';
    }
    return detalle || `Ha fallado la ${accion}.`;
}

// Variables para paginación
let paginaActual = 0;
let totalPaginas = 0;
let tamañoPagina = 5;
let totalItems = 0;
let lastScrollYProgramas = 0;

async function inicializarProgramas() {
    const usuario = JSON.parse(localStorage.getItem('usuario'));
    rolUsuarioActual = usuario ? usuario.rol : null;

    if (usuario && usuario.rol === 'ROLE_ADMIN') {
        const navAdmin = document.getElementById('nav-admin');
        if (navAdmin) navAdmin.style.display = 'block';
    }

    if (!puedeCrearPrograma()) {
        const btnNuevo = document.querySelector('[onclick="mostrarFormularioPrograma()"]');
        if (btnNuevo) btnNuevo.style.display = 'none';
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
        mostrarError(obtenerMensajeErrorProgramas(error, 'carga de programas'));
    }
}

// Función auxiliar para recargar programas y configurar scroll
async function recargarProgramas() {
    await cargarProgramas();
    configurarScrollTablas();
}

async function cargarProgramasPaginados(pagina, ordenPor = 'id', direccionOrden = 'asc') {
    try {
        lastScrollYProgramas = window.scrollY || window.pageYOffset || 0;
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
        setTimeout(() => { window.scrollTo({ top: lastScrollYProgramas || 0, behavior: 'auto' }); }, 0);
    } catch (error) {
        if (error && error.message && error.message.startsWith('401')) {
            return;
        }
        mostrarError(obtenerMensajeErrorProgramas(error, 'carga de programas'));
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
    
    const editProg = puedeEditarCamposPrograma();
    const editConc = puedeEditarCamposConcursante();
    const gestionConc = puedeGestionarConcursantesPrograma();
    const elimProg = puedeEliminarPrograma();

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
        
        // Render por slots fijos 1..3 para mantener huecos al borrar
        const puedeQuitarConc = gestionConc && editConc;
        const concursantePorSlot = { 1: null, 2: null, 3: null };
        const sinSlot = [];
        (concursantes || []).forEach(c => {
            const slot = Number(c.numeroConcursante);
            if (slot >= 1 && slot <= 3 && !concursantePorSlot[slot]) {
                concursantePorSlot[slot] = c;
            } else {
                sinSlot.push(c);
            }
        });
        // Compatibilidad por si llega algún concursante legacy sin slot válido
        [1, 2, 3].forEach(slot => {
            if (!concursantePorSlot[slot] && sinSlot.length > 0) {
                concursantePorSlot[slot] = sinSlot.shift();
            }
        });
        
        return `
            <div class="programa-container" data-programa-id="${programa.id}">
                <div class="programa-header">
                    <div class="programa-info">
                        <div class="programa-info-item">
                            <div class="programa-info-label">Temporada</div>
                            <div class="programa-info-value">
                                ${editProg
                                    ? `<input type="number" class="form-control form-control-sm" min="1" value="${programa.temporada || ''}"
                                               onchange="actualizarTemporadaPrograma(${programa.id}, this.value)" style="width: 80px;">`
                                    : `<span class="programa-info-readonly">${programa.temporada || '—'}</span>`}
                            </div>
                        </div>
                        <div class="programa-info-item" style="min-width: 80px;">
                            <div class="programa-info-label">Programa</div>
                            <div class="programa-info-value">${programa.id}</div>
                        </div>
                        <div class="programa-info-item">
                            <div class="programa-info-label">Estado</div>
                            <div class="programa-info-value">
                                ${editProg
                                    ? `<select class="form-select form-select-sm" style="min-width: 140px;"
                                               onchange="actualizarEstadoPrograma(${programa.id}, this.value)">
                                           ${renderOpcionEstado(programa.estado, 'borrador', 'Borrador')}
                                           ${renderOpcionEstado(programa.estado, 'grabado', 'Grabado')}
                                           ${renderOpcionEstado(programa.estado, 'editado', 'Editado')}
                                           ${renderOpcionEstado(programa.estado, 'programado', 'Programado')}
                                           ${renderOpcionEstado(programa.estado, 'emitido', 'Emitido')}
                                       </select>`
                                    : `<span class="programa-info-readonly">${programa.estado || '—'}</span>`}
                            </div>
                        </div>
                        <div class="programa-info-item">
                            <div class="programa-info-label">Fecha de emisión</div>
                            <div class="programa-info-value">
                                ${editProg
                                    ? `<input type="date" class="form-control form-control-sm" value="${normalizarFechaProgramaISO(programa.fechaEmision)}"
                                               onchange="actualizarFechaEmision(${programa.id}, this.value)" style="width: 150px;">`
                                    : `<span class="programa-info-readonly">${fechaFormateada}</span>`}
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
                                ${editProg
                                    ? `<input type="text" class="form-control form-control-sm"
                                               value="${duracionObjetivo}"
                                               onchange="actualizarDuracionObjetivoPrograma(${programa.id}, this.value)"
                                               placeholder="1h 5m"
                                               style="width: 80px; font-size: 0.9em;">`
                                    : `<span class="programa-info-readonly">${duracionObjetivo}</span>`}
                            </div>
                        </div>
                        <div class="programa-info-item">
                            <div class="programa-info-label">GAP</div>
                            <div class="programa-info-value">
                                <span class="programa-info-readonly">${gap}</span>
                            </div>
                        </div>
                        <div class="programa-acciones">
                            ${elimProg ? `<button class="btn btn-danger" onclick="eliminarPrograma(${programa.id})" title="Borrar programa">
                                <i class="fas fa-trash"></i>
                            </button>` : ''}
                        </div>
                    </div>
                    <div class="programa-creditos mt-2">
                        <div class="programa-info-label" style="text-align:left; margin-bottom:6px;">CRÉDITOS ESPECIALES (Programa)</div>
                        ${editProg
                            ? `<textarea class="editable-field programa-creditos-textarea"
                                         rows="2"
                                         placeholder="Créditos especiales del programa..."
                                         onblur="actualizarCreditosEspecialesPrograma(${programa.id}, this.value)">${programa.creditosEspeciales || ''}</textarea>`
                            : `<p class="editable-field programa-creditos-textarea" style="cursor:default; pointer-events:none; min-height:44px; margin:0;">${programa.creditosEspeciales || ''}</p>`}
                    </div>
                </div>
                
                <div class="concursantes-table" data-programa-id="${programa.id}">
                    <div class="concursantes-table-header-wrapper">
                        <div class="concursantes-table-header">
                            <table id="tabla-programa-${programa.id}-concursantes-header"
                                   class="table table-excel table-striped tabla-header">
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
                                        <th class="col-xusoker">XUSÓKER</th>
                                        <th class="col-factor-x">X</th>
                                        <th class="col-valoracion">VAL</th>
                                        <th class="col-acciones" style="width: 5%;">ACC</th>
                                    </tr>
                                </thead>
                            </table>
                        </div>
                    </div>
                    <div class="concursantes-table-body-wrapper">
                        <table id="tabla-programa-${programa.id}-concursantes"
                               class="table table-excel table-striped">
                            <tbody>
                                ${[1, 2, 3].map(slot => {
                                    const concursante = concursantePorSlot[slot];
                                    if (!concursante) {
                                        return `
                                            <tr class="fila-vacia" ${puedeQuitarConc ? `onclick="mostrarConcursantesDisponibles(${programa.id}, ${slot})" style="cursor:pointer;" title="Añadir concursante en hueco ${slot}"` : ''}>
                                                <td class="col-numero">${slot}</td>
                                                <td class="col-lugar"></td>
                                                <td class="col-nombre"><em style="color: #999;">Hueco disponible</em></td>
                                                <td class="col-edad"></td>
                                                <td class="col-ocupacion"></td>
                                                <td class="col-rrss"></td>
                                                <td class="col-resultado"></td>
                                                <td class="col-duracion"></td>
                                                <td class="col-foto"></td>
                                                <td class="col-momentos"></td>
                                                <td class="col-xusoker"></td>
                                                <td class="col-factor-x"></td>
                                                <td class="col-valoracion"></td>
                                                <td class="col-acciones">
                                                    ${puedeQuitarConc ? `<button class="btn btn-sm btn-success" onclick="mostrarConcursantesDisponibles(${programa.id}, ${slot}); event.stopPropagation();" title="Añadir en hueco ${slot}"><i class="fas fa-plus"></i></button>` : ''}
                                                </td>
                                            </tr>
                                        `;
                                    }
                                    return `
                                        <tr class="concursante-row" onclick="irAConcursante(${concursante.id})">
                                            <td class="col-numero">${slot}</td>
                                            <td class="col-lugar">${concursante.lugar || ''}</td>
                                            <td class="col-nombre"><strong>${concursante.nombre || ''}</strong></td>
                                            <td class="col-edad">${concursante.edad || ''}</td>
                                            <td class="col-ocupacion">${concursante.ocupacion || ''}</td>
                                            <td class="col-rrss">${concursante.redesSociales || ''}</td>
                                            <td class="col-resultado">
                                                ${editConc
                                                    ? `<input type="text" class="campo-editable"
                                                               value="${concursante.resultado || ''}"
                                                               onchange="actualizarCampoConcursante(${concursante.id}, 'resultado', this.value)"
                                                               onclick="event.stopPropagation()"
                                                               placeholder="0€">`
                                                    : `<span>${concursante.resultado || ''}</span>`}
                                            </td>
                                            <td class="col-duracion">${obtenerDuracionConcursante(concursante)}</td>
                                            <td class="col-foto">
                                                ${concursante.foto
                                                    ? (gestionConc
                                                        ? `<img src="/uploads/${concursante.foto}" class="foto-concursante" alt="Foto" onclick="abrirExploradorFoto(${concursante.id}, event)" title="Click para cambiar foto">`
                                                        : `<img src="/uploads/${concursante.foto}" class="foto-concursante" alt="Foto">`)
                                                    : (gestionConc
                                                        ? `<div class="campo-foto-vacio" onclick="abrirExploradorFoto(${concursante.id}, event)" title="Click para añadir foto">
                                                               <i class="fas fa-camera"></i>
                                                               <span>Añadir foto</span>
                                                           </div>`
                                                        : '')}
                                            </td>
                                            <td class="col-momentos">
                                                ${editConc
                                                    ? `<textarea class="campo-editable"
                                                                 onchange="actualizarCampoConcursante(${concursante.id}, 'momentosDestacados', this.value)"
                                                                 onclick="event.stopPropagation()"
                                                                 placeholder="Momentos destacados"
                                                                 rows="2">${concursante.momentosDestacados || ''}</textarea>`
                                                    : `<span>${concursante.momentosDestacados || ''}</span>`}
                                            </td>
                                            <td class="col-xusoker">
                                                ${editConc
                                                    ? `<select class="campo-editable"
                                                               onchange="actualizarCampoConcursante(${concursante.id}, 'xusoker', this.value)"
                                                               onclick="event.stopPropagation()">
                                                           <option value=""></option>
                                                           <option value="NO USÓ" ${concursante.xusoker === 'NO USÓ' ? 'selected' : ''}>NO USÓ</option>
                                                           <option value="CONTINÚE" ${concursante.xusoker === 'CONTINÚE' ? 'selected' : ''}>CONTINÚE</option>
                                                           <option value="AL VERRÉS" ${concursante.xusoker === 'AL VERRÉS' ? 'selected' : ''}>AL VERRÉS</option>
                                                           <option value="RECICLA" ${concursante.xusoker === 'RECICLA' ? 'selected' : ''}>RECICLA</option>
                                                           <option value="LLAMADA" ${concursante.xusoker === 'LLAMADA' ? 'selected' : ''}>LLAMADA</option>
                                                       </select>`
                                                    : `<span>${concursante.xusoker || ''}</span>`}
                                            </td>
                                            <td class="col-factor-x">
                                                ${editConc
                                                    ? `<input type="text" class="campo-editable"
                                                               value="${concursante.factorX || ''}"
                                                               onchange="actualizarCampoConcursante(${concursante.id}, 'factorX', this.value)"
                                                               onclick="event.stopPropagation()"
                                                               placeholder="Factor X">`
                                                    : `<span>${concursante.factorX || ''}</span>`}
                                            </td>
                                            <td class="col-valoracion">
                                                ${editConc
                                                    ? `<select class="campo-editable"
                                                               onchange="actualizarCampoConcursante(${concursante.id}, 'valoracionFinal', this.value)"
                                                               onclick="event.stopPropagation()">
                                                           <option value="">—</option>
                                                           ${VALORACIONES_PERMITIDAS.map(v => `<option value="${v}" ${(concursante.valoracionFinal || '') === v ? 'selected' : ''}>${v}</option>`).join('')}
                                                       </select>`
                                                    : `<span>${concursante.valoracionFinal || ''}</span>`}
                                            </td>
                                            <td class="col-acciones">
                                                ${puedeQuitarConc
                                                    ? `<button class="btn btn-sm btn-danger" onclick="quitarConcursanteDePrograma(${concursante.id}, event)" title="Quitar del programa">
                                                           <i class="fas fa-times"></i>
                                                       </button>`
                                                    : ''}
                                            </td>
                                        </tr>
                                    `;
                                }).join('')}
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

async function actualizarCreditosEspecialesPrograma(programaId, creditos) {
    try {
        const valor = (creditos != null && String(creditos).trim() !== '') ? String(creditos) : null;
        await apiManager.patch(`/api/programas/${programaId}/campo`, { creditosEspeciales: valor });
        // Mantener estado local para evitar recargar toda la vista
        const p = (programas || []).find(x => x.id === programaId);
        if (p) p.creditosEspeciales = valor;
        mostrarExito('Créditos especiales actualizados');
    } catch (error) {
        mostrarError(obtenerMensajeErrorProgramas(error, 'actualización de créditos especiales'));
    }
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


function normalizarFechaProgramaISO(fecha) {
    if (fecha == null || fecha === '') return '';

    if (Array.isArray(fecha) && fecha.length >= 3) {
        const [año, mes, dia] = fecha;
        return `${String(año).padStart(4, '0')}-${String(mes).padStart(2, '0')}-${String(dia).padStart(2, '0')}`;
    }

    if (typeof fecha === 'object') {
        const año = fecha.year ?? fecha.año;
        const mes = fecha.monthValue ?? fecha.month ?? fecha.mes;
        const dia = fecha.dayOfMonth ?? fecha.day ?? fecha.dia;
        if (año != null && mes != null && dia != null) {
            return `${String(año).padStart(4, '0')}-${String(mes).padStart(2, '0')}-${String(dia).padStart(2, '0')}`;
        }
    }

    const valor = String(fecha).trim();
    const matchIso = valor.match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/);
    if (matchIso) {
        const [, año, mes, dia] = matchIso;
        return `${año}-${mes.padStart(2, '0')}-${dia.padStart(2, '0')}`;
    }

    const matchCsv = valor.match(/^(\d{4}),\s*(\d{1,2}),\s*(\d{1,2})$/);
    if (matchCsv) {
        const [, año, mes, dia] = matchCsv;
        return `${año}-${mes.padStart(2, '0')}-${dia.padStart(2, '0')}`;
    }

    return valor;
}

function formatearFechaPrograma(fecha) {
    const fechaIso = normalizarFechaProgramaISO(fecha);
    if (!fechaIso) return 'N/A';

    try {
        const partes = fechaIso.split('-');
        if (partes.length !== 3) return 'Fecha inválida';

        const año = parseInt(partes[0], 10);
        const mes = parseInt(partes[1], 10) - 1;
        const dia = parseInt(partes[2], 10);
        const fechaObj = new Date(año, mes, dia);

        if (isNaN(fechaObj.getTime())) {
            return 'Fecha inválida';
        }

        return `${String(dia).padStart(2, '0')}/${String(mes + 1).padStart(2, '0')}/${año}`;
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
        mostrarError(obtenerMensajeErrorProgramas(error, 'actualización de campo'));
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
        mostrarError(obtenerMensajeErrorProgramas(error, 'actualización de duración objetivo'));
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
        mostrarError(obtenerMensajeErrorProgramas(error, 'carga de programas filtrados'));
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
            await apiManager.putUndoable(`/api/programas/${programaId}`, programaData, { label: `Actualizar programa ${programaId}` });
            await apiManager.patch(`/api/programas/${programaId}/campo`, { temporada: parseInt(temporada) });
            await apiManager.patch(`/api/programas/${programaId}/campo`, { fechaEmision });
            mostrarExito('Programa actualizado correctamente');
        } else {
            const creado = await apiManager.postUndoable('/api/programas', programaData, {
                label: 'Crear programa',
                idExtractor: (r) => r?.id || r?.datos?.id,
                deleteEndpointBuilder: (id) => `/api/programas/${id}`
            });
            mostrarExito('Programa creado correctamente');
        }
        
        bootstrap.Modal.getInstance(document.getElementById('modal-programa')).hide();
        await recargarProgramas();
        
    } catch (error) {
        mostrarError(obtenerMensajeErrorProgramas(error, 'guardado del programa'));
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
        if (!file) {
            mostrarError('No se ha seleccionado ningún archivo.');
            return;
        }
        if (file.size > TAMANO_MAX_FOTO_BYTES) {
            mostrarError('La foto es demasiado grande. El tamaño máximo permitido es 10MB.');
            return;
        }
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
            const errText = await response.text();
            throw new Error(`${response.status}: ${errText}`);
        }
        
        const resultado = await response.json();
        
        // Actualizar la vista
        await recargarProgramas();
        mostrarMensaje('Foto subida correctamente', 'success');
        
    } catch (error) {
        console.error('Error al subir foto:', error);
        mostrarError(obtenerMensajeErrorProgramas(error, 'subida de foto'));
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
        await apiManager.putUndoable(`/api/programas/${programaId}/actualizar-estado`, {}, { label: `Actualizar estado programa ${programaId}`, snapshotEndpoint: `/api/programas/${programaId}` });
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
        mostrarError(obtenerMensajeErrorProgramas(error, 'actualización del estado'));
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
        mostrarError(obtenerMensajeErrorProgramas(error, 'actualización de temporada'));
    }
}

async function actualizarFechaEmision(programaId, fechaISO) {
    try {
        const valor = (fechaISO && fechaISO.trim() !== '') ? fechaISO : null;
        const programaActualizado = await apiManager.patch(`/api/programas/${programaId}/campo`, { fechaEmision: valor });
        if (programaActualizado && typeof programaActualizado === 'object') {
            const indicePrograma = programas.findIndex(programa => programa.id === programaId);
            if (indicePrograma !== -1) {
                programas[indicePrograma] = {
                    ...programas[indicePrograma],
                    ...programaActualizado,
                    fechaEmision: programaActualizado.fechaEmision ?? valor
                };
            }
            mostrarProgramas();
            configurarScrollTablas();
            renderizarPaginacion();
        } else {
            await cargarProgramasPaginados(paginaActual);
        }
        mostrarExito('Fecha de emisión actualizada');
    } catch (error) {
        mostrarError(obtenerMensajeErrorProgramas(error, 'actualización de fecha de emisión'));
    }
}

// Funciones para gestión de concursantes
let concursantesDisponibles = [];
let totalConcursantesDisponibles = 0;
let paginaConcursantesDisponibles = 0;
let totalPaginasConcursantesDisponibles = 1;
let debounceTimer = null;
let posicionPreferidaPrograma = null;

// Aplica filtros de lugar, valoración final y estado sobre la lista en memoria
function aplicarFiltrosConcursantesDisponiblesEnMemoria() {
    const soloEstadosPermitidos = ['GRABADO', 'EDITADO'];
    const lugarInput = document.getElementById('filtro-lugar-concursante-disponible');
    const valoracionSelect = document.getElementById('filtro-valoracion-final-concursante-disponible');
    const estadoSelect = document.getElementById('filtro-estado-concursante-disponible');

    const lugarFiltro = (lugarInput?.value || '').trim().toLowerCase();
    const valoracionFiltro = (valoracionSelect?.value || '').trim();
    const estadoFiltro = (estadoSelect?.value || '').trim().toUpperCase();

    return (concursantesDisponibles || []).filter(c => {
        const estado = (c.estado || '').toUpperCase();

        // Siempre limitar a Grabado / Editado
        if (!soloEstadosPermitidos.includes(estado)) {
            return false;
        }

        // Filtro de estado explícito
        if (estadoFiltro && estado !== estadoFiltro) {
            return false;
        }

        // Filtro de lugar (contiene)
        if (lugarFiltro) {
            const lugar = (c.lugar || '').toLowerCase();
            if (!lugar.includes(lugarFiltro)) {
                return false;
            }
        }

        // Filtro de valoración final exacta (1,2,3)
        if (valoracionFiltro) {
            const val = (c.valoracionFinal != null ? String(c.valoracionFinal).trim() : '');
            if (val !== valoracionFiltro) {
                return false;
            }
        }

        return true;
    });
}

// Re-renderiza la tabla cuando cambian los filtros del modal
function onCambioFiltrosConcursantesDisponibles() {
    renderizarConcursantesDisponibles();
}

async function mostrarConcursantesDisponibles(programaId, posicionPreferida = null) {
    if (!puedeGestionarConcursantesPrograma()) {
        mostrarError('No tienes permisos para añadir concursantes a programas.');
        return;
    }
    try {
        document.getElementById('programa-seleccionado-id').value = programaId;
        posicionPreferidaPrograma = posicionPreferida;
        
        // Limpiar filtro de búsqueda
        const inputBusqueda = document.getElementById('buscar-concursante-disponible');
        if (inputBusqueda) inputBusqueda.value = '';
        const filtroLugar = document.getElementById('filtro-lugar-concursante-disponible');
        const filtroValoracion = document.getElementById('filtro-valoracion-final-concursante-disponible');
        const filtroEstado = document.getElementById('filtro-estado-concursante-disponible');
        if (filtroLugar) filtroLugar.value = '';
        if (filtroValoracion) filtroValoracion.value = '';
        if (filtroEstado) filtroEstado.value = '';
        
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
        mostrarError(obtenerMensajeErrorProgramas(error, 'carga de concursantes disponibles'));
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
    const listaFiltrada = aplicarFiltrosConcursantesDisponiblesEnMemoria();
    if (infoPaginacion) {
        infoPaginacion.innerHTML = `Mostrando ${listaFiltrada.length} de ${totalConcursantesDisponibles} concursantes (solo estados Grabado/Editado) (Página ${paginaConcursantesDisponibles + 1} de ${totalPaginasConcursantesDisponibles})`;
    }
    
    lista.innerHTML = `
        <div class="table-responsive">
            <table class="table table-hover">
                <thead>
                    <tr>
                        <th>Lugar</th>
                        <th>Nombre</th>
                        <th>Edad</th>
                        <th>Premio</th>
                        <th>Valoración final</th>
                    </tr>
                </thead>
                <tbody>
                    ${listaFiltrada.map(concursante => `
                        <tr style="cursor: pointer;" onclick="asignarConcursanteAPrograma(${concursante.id})">
                            <td>${concursante.lugar || ''}</td>
                            <td><strong>${concursante.nombre || ''}</strong></td>
                            <td>${concursante.edad || ''}</td>
                            <td>${concursante.premio != null && concursante.premio !== '' ? concursante.premio : ''}</td>
                            <td>${concursante.valoracionFinal || ''}</td>
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
        mostrarError(obtenerMensajeErrorProgramas(error, 'filtrado de concursantes'));
    }
}

async function asignarConcursanteAPrograma(concursanteId) {
    if (!puedeGestionarConcursantesPrograma()) {
        mostrarError('No tienes permisos para añadir concursantes a programas.');
        return;
    }
    try {
        const programaId = document.getElementById('programa-seleccionado-id').value;

        const queryPos = (posicionPreferidaPrograma != null) ? `?posicion=${encodeURIComponent(posicionPreferidaPrograma)}` : '';
        await apiManager.postUndoable(`/api/concursantes/${concursanteId}/asignar-programa/${programaId}${queryPos}`, {}, {
            label: `Asignar programa ${programaId} a concursante ${concursanteId}`,
            idExtractor: () => concursanteId,
            deleteEndpointBuilder: () => `/api/concursantes/${concursanteId}/desasignar-programa`
        });
        
        // Cerrar modal
        const modal = bootstrap.Modal.getInstance(document.getElementById('modal-añadir-concursantes'));
        modal.hide();
        posicionPreferidaPrograma = null;
        
        // Recargar programas
        await recargarProgramas();
        
        mostrarMensaje('Concursante añadido al programa correctamente', 'success');
    } catch (error) {
        posicionPreferidaPrograma = null;
        mostrarError(obtenerMensajeErrorProgramas(error, 'asignación de concursante'));
    }
}

async function quitarConcursanteDePrograma(concursanteId, event) {
    event.stopPropagation();
    if (!(puedeGestionarConcursantesPrograma() && puedeEditarCamposConcursante())) {
        mostrarError('No tienes permisos para quitar concursantes de programas.');
        return;
    }
    
    if (!confirm('¿Estás seguro de que quieres quitar este concursante del programa?')) {
        return;
    }
    
    try {
        await apiManager.deleteUndoable(`/api/concursantes/${concursanteId}/desasignar-programa`, { label: `Desasignar programa de concursante ${concursanteId}`, snapshotEndpoint: `/api/concursantes/${concursanteId}` });
        
        // Recargar programas
        await recargarProgramas();
        
        mostrarMensaje('Concursante quitado del programa correctamente', 'success');
    } catch (error) {
        mostrarError(obtenerMensajeErrorProgramas(error, 'desasignación de concursante'));
    }
}

async function editarPrograma(programaId) {
    try {
        const programa = await apiManager.get(`/api/programas/${programaId}`);
        document.getElementById('programa-id').value = programa.id;
        document.getElementById('temporada-programa').value = programa.temporada;
        document.getElementById('fecha-emision').value = normalizarFechaProgramaISO(programa.fechaEmision);
        document.getElementById('modal-programa-titulo').textContent = 'Editar Programa';

        const modal = new bootstrap.Modal(document.getElementById('modal-programa'));
        modal.show();
    } catch (error) {
        mostrarError(obtenerMensajeErrorProgramas(error, 'carga de datos del programa'));
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
        await apiManager.deleteUndoable(`/api/programas/${programaId}`, { label: `Eliminar programa ${programaId}`, snapshotEndpoint: `/api/programas/${programaId}` });
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

// Función para configurar scroll en las tablas de concursantes (barra horizontal; cabecera sin barra)
function configurarScrollTablas() {
    document.querySelectorAll('.concursantes-table').forEach(configurarScrollEnTabla);
}

// Sincroniza anchos de columna de la tabla de cabecera a la tabla de cuerpo (mismo programa)
function sincronizarAnchosCabeceraCuerpo(contenedor) {
    const headerTable = contenedor.querySelector('.concursantes-table-header-wrapper table');
    const bodyTable = contenedor.querySelector('.concursantes-table-body-wrapper table');
    if (!headerTable || !bodyTable) return;
    const headerCells = headerTable.querySelectorAll('thead tr th');
    if (!headerCells.length) return;
    headerCells.forEach((th, colIndex) => {
        const w = th.offsetWidth || (th.style.width && Number.parseInt(th.style.width, 10));
        if (!w) return;
        const px = (typeof w === 'number' ? w : Number.parseInt(w, 10)) + 'px';
        bodyTable.querySelectorAll('tbody tr').forEach((tr) => {
            const cell = tr.cells[colIndex];
            if (cell) cell.style.width = px;
        });
    });
}

// Función para configurar scroll en una tabla específica (barra horizontal; cabecera sincronizada sin barra)
function configurarScrollEnTabla(contenedor) {
    if (!contenedor) return;
    if (contenedor.dataset.scrollConfigurado === 'true') return;
    
    const bodyWrapper = contenedor.querySelector('.concursantes-table-body-wrapper');
    const headerWrapper = contenedor.querySelector('.concursantes-table-header-wrapper');
    if (!bodyWrapper || !headerWrapper) return;
    
    // Sincronizar cabecera con el scroll del cuerpo (cabecera sin barra visible)
    bodyWrapper.addEventListener('scroll', function() {
        headerWrapper.scrollLeft = bodyWrapper.scrollLeft;
    });
    
    // Sincronizar anchos de columna cabecera -> cuerpo (tras TableResizer o al cargar)
    const runSync = () => sincronizarAnchosCabeceraCuerpo(contenedor);
    setTimeout(runSync, 100);
    setTimeout(runSync, 500);
    const headerTable = headerWrapper.querySelector('table');
    if (headerTable && typeof ResizeObserver !== 'undefined') {
        const ro = new ResizeObserver(() => runSync());
        headerTable.querySelectorAll('thead tr th').forEach((th) => ro.observe(th));
    }
    
    // Scroll con teclado (opcional)
    contenedor.setAttribute('tabindex', '0');
    contenedor.addEventListener('keydown', function(e) {
        if (e.key === 'ArrowLeft') {
            e.preventDefault();
            bodyWrapper.scrollLeft -= 50;
        } else if (e.key === 'ArrowRight') {
            e.preventDefault();
            bodyWrapper.scrollLeft += 50;
        }
    });
    
    contenedor.dataset.scrollConfigurado = 'true';
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
        mostrarError(obtenerMensajeErrorProgramas(error, 'carga de más concursantes'));
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
        mostrarError(obtenerMensajeErrorProgramas(error, 'carga de página de concursantes'));
    }
}