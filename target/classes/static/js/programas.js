let vistaRepetidos = false;
let programas = [];
let concursantesPorPrograma = {};
const programasEnEdicion = new Set();

let rolUsuarioActual = null;

function puedeEditarCamposPrograma() {
    return ['ROLE_ADMIN', 'ROLE_DIRECCION'].includes(rolUsuarioActual);
}

function esFlagRepetido(repetido) {
    return repetido === true || repetido === 'true';
}

function claveEdicionPrograma(programaId, repetido) {
    return `${esFlagRepetido(repetido) ? 'r' : 'n'}:${programaId}`;
}

function buscarProgramaListado(programaId, repetido) {
    const esRepetido = esFlagRepetido(repetido);
    return programas.find(p => String(p.id) === String(programaId) && !!p.repetido === esRepetido) || null;
}

function urlBasePrograma(programaId, repetido) {
    return esFlagRepetido(repetido)
        ? `/api/programas/repetidos/${programaId}`
        : `/api/programas/${programaId}`;
}

function alternarEdicionPrograma(programaId, repetido) {
    if (!puedeEditarCamposPrograma()) {
        mostrarError('No tienes permisos para editar programas.');
        return;
    }
    const programa = buscarProgramaListado(programaId, repetido);
    if (!programa) return;
    if ((programa.estado || '').toLowerCase() === 'emitido') {
        mostrarError('Un programa emitido no se puede editar.');
        return;
    }
    const clave = claveEdicionPrograma(programa.id, programa.repetido);
    if (programasEnEdicion.has(clave)) {
        programasEnEdicion.delete(clave);
    } else {
        programasEnEdicion.add(clave);
    }
    // El botón Editar/Listo solo abre o cierra los campos. No reenfoca un input.
    const ancla = capturarAnclaScroll();
    if (ancla) ancla.foco = null;
    mostrarProgramas();
    restaurarAnclaScroll(ancla, { enfocar: false });
}

function puedeCrearPrograma() {
    return ['ROLE_ADMIN', 'ROLE_DIRECCION'].includes(rolUsuarioActual);
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

function esProgramaEmitido(programaId) {
    const programa = programas.find(p => String(p.id) === String(programaId));
    return (programa?.estado || '').toLowerCase() === 'emitido';
}

// Valoraciones permitidas (guionista/dirección) - solo front
const VALORACIONES_PERMITIDAS = ['1', '1+', '2-', '2', '2+', '3-', '3', '3+'];

const TAMANO_MAX_FOTO_BYTES = 10 * 1024 * 1024; // 10MB

function obtenerMensajeErrorProgramas(error, accion = 'realizar esta acción') {
    const msg = Utils.mensajeErrorApi(error, accion);
    if (msg.includes('demasiado grande')) {
        return 'La foto es demasiado grande. El tamaño máximo permitido es 10MB.';
    }
    return msg;
}

// Variables para paginación
let paginaActual = 0;
let totalPaginas = 0;
let tamañoPagina = 5;
let totalItems = 0;
let lastScrollYProgramas = 0;
let cargandoProgramas = false;
let filtrarProgramasTimeout = null;

function mostrarEstadoCargaProgramas(mensaje = 'Cargando programas...', porcentaje = null) {
    const indicador = document.getElementById('indicador-carga-programas');
    const mensajeEl = indicador?.querySelector('.carga-programas-mensaje');
    const barra = document.getElementById('barra-carga-programas');
    if (indicador) {
        indicador.classList.remove('d-none');
    }
    if (mensajeEl) {
        mensajeEl.textContent = mensaje;
    }
    if (barra && porcentaje !== null) {
        const pct = Math.max(0, Math.min(100, porcentaje));
        barra.style.width = `${pct}%`;
        barra.setAttribute('aria-valuenow', String(pct));
    }
}

function ocultarEstadoCargaProgramas() {
    document.getElementById('indicador-carga-programas')?.classList.add('d-none');
    const barra = document.getElementById('barra-carga-programas');
    if (barra) {
        barra.style.width = '0%';
        barra.setAttribute('aria-valuenow', '0');
    }
}

function actualizarProgresoCargaProgramas(actual, total, mensaje) {
    const pct = total > 0 ? Math.round((actual / total) * 100) : 100;
    mostrarEstadoCargaProgramas(mensaje, pct);
}

async function inicializarProgramas() {
    if (inicializarProgramas.hecho) return;
    inicializarProgramas.hecho = true;

    const usuario = JSON.parse(localStorage.getItem('usuario'));
    rolUsuarioActual = usuario ? usuario.rol : null;

    if (usuario && usuario.rol === 'ROLE_ADMIN') {
        const navAdmin = document.getElementById('nav-admin');
        if (navAdmin) navAdmin.style.display = 'block';
    }

    if (!puedeCrearPrograma()) {
        const btnNuevo = document.querySelector('[onclick="mostrarFormularioPrograma()"]');
        if (btnNuevo) btnNuevo.style.display = 'none';
        const btnRepetido = document.getElementById('btn-nuevo-repetido');
        if (btnRepetido) btnRepetido.style.display = 'none';
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

async function refrescarVistaProgramas() {
    while (cargandoProgramas) {
        await new Promise(resolve => setTimeout(resolve, 40));
    }
    if (aplicandoFiltros) {
        await cargarProgramasFiltrados();
    } else {
        await cargarProgramasPaginados(paginaActual);
    }
}

function actualizarEstadoProgramaEnDom(programaId, nuevoEstado) {
    if (!nuevoEstado) return;
    const programa = programas.find(p => p.id === programaId);
    if (programa) programa.estado = nuevoEstado;
    const contenedor = document.querySelector(`[data-programa-id="${programaId}"]`);
    const select = contenedor?.querySelector('select[onchange*="actualizarEstadoPrograma"]');
    if (select) {
        select.value = nuevoEstado;
    }
}

function mapRevertCampoPrograma(key, raw) {
    if (key === 'fechaEmision') {
        if (raw == null || raw === '') return null;
        return normalizarFechaProgramaISO(raw) || null;
    }
    return raw ?? null;
}

async function cargarProgramasPaginados(pagina, ordenPor = 'id', direccionOrden = 'desc') {
    if (cargandoProgramas) {
        return;
    }
    cargandoProgramas = true;
    vistaRepetidos = false;
    mostrarEstadoCargaProgramas('Cargando programas...', 15);
    try {
        const anclaScroll = capturarAnclaScroll();
        const response = await apiManager.get(
            `/api/programas/pagina?page=${pagina}&size=${tamañoPagina}&sortBy=${ordenPor}&sortDir=${direccionOrden}`
        );
        
        programas = response.programas;
        paginaActual = response.currentPage;
        totalItems = response.totalItems;
        totalPaginas = response.totalPages;
        
        mostrarEstadoCargaProgramas('Cargando concursantes...', 50);
        await cargarConcursantesPorPrograma();
        mostrarProgramas();
        configurarScrollTablas(); // Configurar scroll automático en las tablas
        renderizarPaginacion();
        restaurarAnclaScroll(anclaScroll, { enfocar: false });
    } catch (error) {
        if (error && error.message && error.message.startsWith('401')) {
            return;
        }
        mostrarError(obtenerMensajeErrorProgramas(error, 'carga de programas'));
    } finally {
        cargandoProgramas = false;
        ocultarEstadoCargaProgramas();
    }
}

function normalizarListaConcursantes(data) {
    if (Array.isArray(data)) return data;
    if (data && Array.isArray(data.content)) return data.content;
    if (data && Array.isArray(data.concursantes)) return data.concursantes;
    return [];
}

function programasDeLaPaginaFiltrada() {
    const inicio = paginaActual * tamañoPagina;
    return programasFiltrados.slice(inicio, inicio + tamañoPagina);
}

async function cargarConcursantesDeProgramas(lista) {
    const programasPagina = Array.isArray(lista) ? lista : [];
    const siguientes = {};
    const total = programasPagina.length;
    for (let i = 0; i < total; i++) {
        const programa = programasPagina[i];
        if (total > 1) {
            actualizarProgresoCargaProgramas(
                i + 1,
                total,
                `Cargando concursantes (${i + 1}/${total})...`
            );
        }
        try {
            const concursantes = await apiManager.get(urlConcursantesDePrograma(programa));
            siguientes[programa.id] = normalizarListaConcursantes(concursantes);
        } catch (e) {
            siguientes[programa.id] = [];
        }
    }
    concursantesPorPrograma = siguientes;
}

async function cargarConcursantesPorPrograma() {
    await cargarConcursantesDeProgramas(programas);
}

function mostrarProgramas() {
    const contenedor = document.getElementById('contenedor-programas');
    
    if (programas.length === 0) {
        contenedor.innerHTML = '<div class="alert alert-info">No hay programas registrados.</div>';
        return;
    }
    
    const visibles = aplicandoFiltros ? programas : aplicarFiltros(programas);
    if (visibles.length === 0) {
        contenedor.innerHTML = '<div class="alert alert-warning">No hay programas que coincidan con los filtros.</div>';
        return;
    }
    
    // Actualizar información de paginación en el bloque estático (arriba y abajo)
    const infosEl = [
        document.getElementById('info-paginacion-programas'),
        document.getElementById('info-paginacion-programas-top')
    ].filter(Boolean);
    const seccionPag = document.getElementById('seccion-paginacion-programas');
    const wrapTop = document.getElementById('info-paginacion-programas-top-wrap');
    if (infosEl.length && totalItems > 0) {
        const inicio = paginaActual * tamañoPagina + 1;
        const fin = Math.min((paginaActual + 1) * tamañoPagina, totalItems);
        const textoInfo = `Mostrando ${inicio}-${fin} de ${totalItems} programas`;
        infosEl.forEach(el => { el.textContent = textoInfo; });
        if (seccionPag) seccionPag.style.display = '';
        if (wrapTop) wrapTop.style.display = '';
    } else {
        if (seccionPag) seccionPag.style.display = 'none';
        if (wrapTop) wrapTop.style.display = 'none';
    }

    const editProg = puedeEditarCamposPrograma();
    const editConc = puedeEditarCamposConcursante();
    const gestionConc = puedeGestionarConcursantesPrograma();
    const elimProg = puedeEliminarPrograma();

    contenedor.innerHTML = visibles.map(programa => {
        const concursantes = concursantesPorPrograma[programa.id] || [];
        const fechaFormateada = formatearFechaPrograma(programa.fechaEmision);
        const totalResultados = calcularTotalResultados(concursantes);
        const duracionReal = calcularDuracionReal(concursantes);
        const duracionObjetivo = programa.duracionObjetivo || '45m';
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
        const esEmitido = (programa.estado || '').toLowerCase() === 'emitido';
        const claveEdicion = claveEdicionPrograma(programa.id, programa.repetido);
        if (esEmitido) {
            programasEnEdicion.delete(claveEdicion);
        }
        const editProgCampos = editProg && !esEmitido && programasEnEdicion.has(claveEdicion);
        const repetidoJs = programa.repetido ? 'true' : 'false';
        const editConcCampos = editConc && !esEmitido;
        const gestionConcCampos = gestionConc && (!esEmitido || programa.repetido);
        
        // Render por slots fijos 1..3 para mantener huecos al borrar
        const puedeQuitarConc = gestionConc && editConc && (!esEmitido || programa.repetido);
        const concursantePorSlot = { 1: null, 2: null, 3: null };
        const sinSlot = [];
        (concursantes || []).forEach(c => {
            const slot = Number(c.ordenEscaleta);
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
                                ${editProgCampos
                                    ? `<input type="number" class="form-control form-control-sm" min="1" value="${programa.temporada || ''}"
                                               data-campo="temporada"
                                               onchange="actualizarTemporadaPrograma(${programa.id}, this.value, ${repetidoJs})" style="width: 80px;">`
                                    : `<span class="programa-info-readonly">${programa.temporada || '—'}</span>`}
                            </div>
                        </div>
                        <div class="programa-info-item" style="min-width: 80px;">
                            <div class="programa-info-label">Código programa</div>
                            <div class="programa-info-value">
                                ${editProgCampos
                                    ? `<input type="text" class="form-control form-control-sm" maxlength="32"
                                               data-campo="codigo"
                                               value="${programa.codigo || programa.id}"
                                               onchange="actualizarCodigoPrograma(${programa.id}, this.value, ${repetidoJs})" style="width: 100px;">`
                                    : `<span class="programa-info-readonly">${programa.codigo || programa.id}</span>`}
                                ${programa.repetido ? `<span class="badge bg-secondary ms-1">${programa.tipo || 'REPE'}${programa.programaOriginalCodigo ? ' · PGM ' + programa.programaOriginalCodigo : ''}</span>` : ''}
                            </div>
                        </div>
                        <div class="programa-info-item">
                            <div class="programa-info-label">Estado</div>
                            <div class="programa-info-value">
                                ${editProgCampos
                                    ? `<select class="form-select form-select-sm" style="min-width: 140px;"
                                               data-campo="estado"
                                               onchange="actualizarEstadoPrograma(${programa.id}, this.value, ${repetidoJs})">
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
                                ${editProgCampos
                                    ? `<input type="date" class="form-control form-control-sm" value="${normalizarFechaProgramaISO(programa.fechaEmision)}"
                                               data-campo="fechaEmision"
                                               onchange="actualizarFechaEmision(${programa.id}, this.value, ${repetidoJs})" style="width: 150px;">`
                                    : `<span class="programa-info-readonly">${fechaFormateada}</span>`}
                            </div>
                        </div>
                        <div class="programa-info-item" data-campo="premios">
                            <div class="programa-info-label">Total Premios</div>
                            <div class="programa-info-value">
                                <span class="programa-info-readonly">${totalResultados}€</span>
                            </div>
                        </div>
                        <div class="programa-info-item">
                            <div class="programa-info-label">Duración Objetivo</div>
                            <div class="programa-info-value">
                                ${(editProgCampos && !programa.repetido)
                                    ? `<input type="text" class="form-control form-control-sm"
                                               data-campo="duracionObjetivo"
                                               value="${duracionObjetivo}"
                                               onchange="actualizarDuracionObjetivoPrograma(${programa.id}, this.value)"
                                               placeholder="45m"
                                               style="width: 80px; font-size: 0.9em;">`
                                    : `<span class="programa-info-readonly">${duracionObjetivo}</span>`}
                            </div>
                        </div>
                        <div class="programa-info-item" data-campo="gap">
                            <div class="programa-info-label">GAP</div>
                            <div class="programa-info-value">
                                <span class="programa-info-readonly">${gap}</span>
                            </div>
                        </div>
                        <div class="programa-recuadros">
                            <div class="programa-recuadro">
                                <div class="programa-info-label">Créditos especiales</div>
                                ${editProgCampos
                                    ? `<textarea class="editable-field programa-creditos-textarea"
                                                 rows="2"
                                                 data-campo="creditosEspeciales"
                                                 placeholder="Créditos especiales..."
                                                 onblur="actualizarCreditosEspecialesPrograma(${programa.id}, this.value, ${repetidoJs})">${programa.creditosEspeciales || ''}</textarea>`
                                    : `<p class="editable-field programa-creditos-textarea" style="cursor:default; pointer-events:none; margin:0;">${programa.creditosEspeciales || ''}</p>`}
                            </div>
                            <div class="programa-recuadro">
                                <div class="programa-info-label">Notas del programa</div>
                                ${editProgCampos
                                    ? `<textarea class="editable-field programa-creditos-textarea"
                                                 rows="2"
                                                 data-campo="notas"
                                                 placeholder="Notas internas..."
                                                 onblur="actualizarNotasPrograma(${programa.id}, this.value, ${repetidoJs})">${programa.notas || ''}</textarea>`
                                    : `<p class="editable-field programa-creditos-textarea" style="cursor:default; pointer-events:none; margin:0;">${programa.notas || ''}</p>`}
                            </div>
                        </div>
                        <div class="programa-acciones">
                            ${editProg && !esEmitido ? `<button type="button" class="btn btn-editar-programa ${editProgCampos ? 'btn-primary' : 'btn-outline-primary'}" onclick="alternarEdicionPrograma(${programa.id}, ${repetidoJs})" title="${editProgCampos ? 'Cerrar edición' : 'Editar programa'}">
                                <i class="fas ${editProgCampos ? 'fa-check' : 'fa-edit'}"></i>
                                <span>${editProgCampos ? 'Listo' : 'Editar'}</span>
                            </button>` : ''}
                            ${elimProg && !esEmitido ? `<button class="btn btn-danger" onclick="eliminarPrograma(${programa.id})" title="Borrar programa">
                                <i class="fas fa-trash"></i>
                            </button>` : ''}
                        </div>
                    </div>
                </div>
                
                <div class="concursantes-table" data-programa-id="${programa.id}">
                    <div class="concursantes-table-header-wrapper">
                        <div class="concursantes-table-header">
                            <table id="tabla-programa-${programa.id}-concursantes-header"
                                   class="table table-excel table-striped tabla-header"
                                   data-resizer-key="programa-concursantes">
                                <colgroup>
                                    <col class="col-numero">
                                    <col class="col-lugar">
                                    <col class="col-nombre">
                                    <col class="col-edad">
                                    <col class="col-ocupacion">
                                    <col class="col-rrss">
                                    <col class="col-resultado">
                                    <col class="col-duracion">
                                    <col class="col-foto">
                                    <col class="col-momentos">
                                    <col class="col-xusoker">
                                    <col class="col-factor-x">
                                    <col class="col-valoracion">
                                    <col class="col-acciones">
                                </colgroup>
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
                                        <th class="col-acciones">ACC</th>
                                    </tr>
                                </thead>
                            </table>
                        </div>
                    </div>
                    <div class="concursantes-table-body-wrapper">
                        <table id="tabla-programa-${programa.id}-concursantes"
                               class="table table-excel table-striped"
                               data-resizer-key="programa-concursantes">
                            <colgroup>
                                <col class="col-numero">
                                <col class="col-lugar">
                                <col class="col-nombre">
                                <col class="col-edad">
                                <col class="col-ocupacion">
                                <col class="col-rrss">
                                <col class="col-resultado">
                                <col class="col-duracion">
                                <col class="col-foto">
                                <col class="col-momentos">
                                <col class="col-xusoker">
                                <col class="col-factor-x">
                                <col class="col-valoracion">
                                <col class="col-acciones">
                            </colgroup>
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
                                        <tr class="concursante-row" data-concursante-id="${concursante.id}" onclick="irAConcursante(${concursante.id})">
                                            <td class="col-numero">${slot}</td>
                                            <td class="col-lugar">${concursante.lugar || ''}</td>
                                            <td class="col-nombre"><strong>${concursante.nombre || ''}</strong></td>
                                            <td class="col-edad">${concursante.edad || ''}</td>
                                            <td class="col-ocupacion">${concursante.ocupacion || ''}</td>
                                            <td class="col-rrss">${concursante.redesSociales || ''}</td>
                                            <td class="col-resultado">
                                                ${editConcCampos
                                                    ? `<input type="text" class="campo-editable"
                                                               data-campo="resultado" data-concursante-id="${concursante.id}"
                                                               value="${concursante.resultado || ''}"
                                                               onchange="actualizarCampoConcursante(${concursante.id}, 'resultado', this.value)"
                                                               onclick="event.stopPropagation()"
                                                               placeholder="0€">`
                                                    : `<span>${concursante.resultado || ''}</span>`}
                                            </td>
                                            <td class="col-duracion">${obtenerDuracionConcursante(concursante)}</td>
                                            <td class="col-foto">
                                                ${concursante.foto
                                                    ? (gestionConcCampos
                                                        ? `<img src="/uploads/${concursante.foto}" class="foto-concursante" alt="Foto" onclick="abrirExploradorFoto(${concursante.id}, event)" title="Click para cambiar foto">`
                                                        : `<img src="/uploads/${concursante.foto}" class="foto-concursante" alt="Foto">`)
                                                    : (gestionConcCampos
                                                        ? `<div class="campo-foto-vacio" onclick="abrirExploradorFoto(${concursante.id}, event)" title="Click para añadir foto">
                                                               <i class="fas fa-camera"></i>
                                                               <span>Añadir foto</span>
                                                           </div>`
                                                        : '')}
                                            </td>
                                            <td class="col-momentos">
                                                ${editConcCampos
                                                    ? `<textarea class="campo-editable campo-momentos"
                                                                 data-campo="momentosDestacados" data-concursante-id="${concursante.id}"
                                                                 onchange="actualizarCampoConcursante(${concursante.id}, 'momentosDestacados', this.value)"
                                                                 onclick="event.stopPropagation()"
                                                                 placeholder="Momentos destacados"
                                                                 rows="2">${concursante.momentosDestacados || ''}</textarea>`
                                                    : `<span>${concursante.momentosDestacados || ''}</span>`}
                                            </td>
                                            <td class="col-xusoker">
                                                ${editConcCampos
                                                    ? `<select class="campo-editable"
                                                               data-campo="xusoker" data-concursante-id="${concursante.id}"
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
                                                ${editConcCampos
                                                    ? `<input type="text" class="campo-editable"
                                                               data-campo="factorX" data-concursante-id="${concursante.id}"
                                                               value="${concursante.factorX || ''}"
                                                               onchange="actualizarCampoConcursante(${concursante.id}, 'factorX', this.value)"
                                                               onclick="event.stopPropagation()"
                                                               placeholder="Factor X">`
                                                    : `<span>${concursante.factorX || ''}</span>`}
                                            </td>
                                            <td class="col-valoracion">
                                                ${editConcCampos
                                                    ? `<select class="campo-editable"
                                                               data-campo="valoracionFinal" data-concursante-id="${concursante.id}"
                                                               onchange="actualizarCampoConcursante(${concursante.id}, 'valoracionFinal', this.value)"
                                                               onclick="event.stopPropagation()">
                                                           <option value="">—</option>
                                                           ${VALORACIONES_PERMITIDAS.map(v => `<option value="${v}" ${(concursante.valoracionFinal || '') === v ? 'selected' : ''}>${v}</option>`).join('')}
                                                       </select>`
                                                    : `<span>${concursante.valoracionFinal || ''}</span>`}
                                            </td>
                                            <td class="col-acciones">
                                                ${puedeQuitarConc
                                                    ? `<button class="btn btn-sm btn-danger" onclick="quitarConcursanteDePrograma(${concursante.id}, ${programa.id}, event)" title="Quitar del programa">
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

    if (typeof SyncMonitor !== 'undefined') {
        SyncMonitor.resetFromVisible(visibles.map(p => ({
            entityType: 'PROGRAMA',
            entityId: p.id,
            version: p.version || 0,
            label: `Programa T${p.temporada} #${p.codigo || p.id}`
        })));
    }
}

function rechazarSiProgramaEmitido(programaId, repetido) {
    const programa = buscarProgramaListado(programaId, repetido);
    if (programa && (programa.estado || '').toLowerCase() === 'emitido') {
        mostrarError('Un programa emitido no se puede editar.');
        return true;
    }
    return false;
}

function fusionarProgramaEnListas(programaId, repetido, campos) {
    if (!campos || typeof campos !== 'object' || Array.isArray(campos)) return;
    const esRepetido = esFlagRepetido(repetido);
    const aplicar = (lista) => {
        if (!Array.isArray(lista)) return;
        lista.forEach(programa => {
            if (String(programa.id) === String(programaId) && !!programa.repetido === esRepetido) {
                Object.assign(programa, campos);
            }
        });
    };
    aplicar(programas);
    aplicar(programasFiltrados);
}

let anclaScrollProgramas = null;
let anclaScrollEn = 0;
let tokenRestauracionScroll = 0;
let anclaUndoProfundidad = 0;
let recordUndoOriginal = null;

function idSelectorProgramas(valor) {
    return String(valor == null ? '' : valor).replace(/[^a-zA-Z0-9_-]/g, '');
}

function selectorProgramaVisible(programaId) {
    return `.programa-container[data-programa-id="${idSelectorProgramas(programaId)}"]`;
}

function selectoresCampoPrograma(programaId, campo) {
    const programa = selectorProgramaVisible(programaId);
    const nombre = idSelectorProgramas(campo);
    if (!nombre) return [programa];
    return [`${programa} [data-campo="${nombre}"]`, programa];
}

function selectoresCampoConcursante(concursanteId, campo) {
    const id = idSelectorProgramas(concursanteId);
    const fila = `tr[data-concursante-id="${id}"]`;
    const nombre = idSelectorProgramas(campo);
    if (!nombre) return [fila];
    return [`${fila} [data-campo="${nombre}"]`, fila];
}

function leerScrollVentana() {
    return {
        x: window.scrollX || window.pageXOffset || 0,
        y: window.scrollY || window.pageYOffset || 0
    };
}

function esDesplazable(el) {
    if (!el || el === document.body || el === document.documentElement) return false;
    if (typeof window.getComputedStyle !== 'function') return false;
    let style;
    try {
        style = window.getComputedStyle(el);
    } catch (e) {
        return false;
    }
    if (!style) return false;
    const puedeY = style.overflowY === 'auto' || style.overflowY === 'scroll' || style.overflowY === 'overlay';
    const puedeX = style.overflowX === 'auto' || style.overflowX === 'scroll' || style.overflowX === 'overlay';
    const alto = (el.scrollHeight || 0) > (el.clientHeight || 0) + 1;
    const ancho = (el.scrollWidth || 0) > (el.clientWidth || 0) + 1;
    return (puedeY && alto) || (puedeX && ancho);
}

function firmaElementoScroll(el) {
    if (!el || !el.classList) return null;
    if (el.id) return { tipo: 'id', id: el.id };
    const clases = ['concursantes-table-body-wrapper', 'concursantes-table-header-wrapper'];
    const clase = clases.find(nombre => el.classList.contains(nombre));
    if (!clase) return null;
    const programa = el.closest ? el.closest('[data-programa-id]') : null;
    return {
        tipo: 'clase',
        clase,
        programaId: programa ? programa.getAttribute('data-programa-id') : null
    };
}

function capturarContenedoresScroll() {
    const vistos = new Set();
    const lista = [];
    const anotar = (el) => {
        if (!el || vistos.has(el) || el === document.body || el === document.documentElement) return;
        vistos.add(el);
        const dentroDeLista = !!(el.closest && el.closest('#contenedor-programas'));
        const firma = firmaElementoScroll(el);
        if (dentroDeLista && !firma) return;
        if (!dentroDeLista && !firma && !esDesplazable(el)) return;
        lista.push({
            top: el.scrollTop || 0,
            left: el.scrollLeft || 0,
            firma,
            nodo: dentroDeLista ? null : el
        });
    };
    if (document.querySelectorAll) {
        document.querySelectorAll('.concursantes-table-body-wrapper, .concursantes-table-header-wrapper').forEach(anotar);
    }
    const recorrer = (inicio) => {
        let nodo = inicio;
        while (nodo && nodo !== document.body) {
            if (esDesplazable(nodo)) anotar(nodo);
            nodo = nodo.parentElement;
        }
    };
    recorrer(document.activeElement);
    recorrer(document.getElementById ? document.getElementById('contenedor-programas') : null);
    return lista;
}

function describirFocoActivo() {
    const el = document.activeElement;
    if (!el || el === document.body || el === document.documentElement || !el.getAttribute) return null;
    const tag = (el.tagName || '').toLowerCase();
    if (!['input', 'textarea', 'select'].includes(tag)) return null;
    if (el.closest && el.closest('.modal')) return null;
    const campo = el.getAttribute('data-campo');
    if (!campo) return null;
    const desdeFila = el.closest ? el.closest('[data-concursante-id]') : null;
    const concursanteId = el.getAttribute('data-concursante-id')
        || (desdeFila ? desdeFila.getAttribute('data-concursante-id') : null);
    const programaNodo = el.closest ? el.closest('[data-programa-id]') : null;
    return {
        campo,
        concursanteId,
        programaId: programaNodo ? programaNodo.getAttribute('data-programa-id') : null
    };
}

function buscarCampoEditado(foco) {
    if (!foco || !foco.campo || !document.querySelector) return null;
    const campo = idSelectorProgramas(foco.campo);
    if (!campo) return null;
    if (foco.concursanteId != null && foco.concursanteId !== '') {
        const id = idSelectorProgramas(foco.concursanteId);
        return document.querySelector(`[data-concursante-id="${id}"][data-campo="${campo}"]`);
    }
    if (foco.programaId != null && foco.programaId !== '') {
        const id = idSelectorProgramas(foco.programaId);
        return document.querySelector(`.programa-container[data-programa-id="${id}"] [data-campo="${campo}"]`);
    }
    return null;
}

function resolverContenedorScroll(item) {
    if (!item) return null;
    if (item.nodo && (!document.contains || document.contains(item.nodo))) return item.nodo;
    if (!item.firma) return null;
    if (item.firma.tipo === 'id' && item.firma.id && document.getElementById) {
        return document.getElementById(item.firma.id);
    }
    if (item.firma.tipo === 'clase' && item.firma.clase && document.querySelector) {
        if (item.firma.programaId) {
            const id = idSelectorProgramas(item.firma.programaId);
            const scope = document.querySelector(`.programa-container[data-programa-id="${id}"]`);
            return scope && scope.querySelector ? scope.querySelector('.' + item.firma.clase) : null;
        }
        return document.querySelector('.' + item.firma.clase);
    }
    return null;
}

function capturarAnclaScroll() {
    const { x, y } = leerScrollVentana();
    lastScrollYProgramas = y;
    const ahora = Date.now();
    const reciente = anclaScrollProgramas && (ahora - anclaScrollEn) < 400;
    const foco = describirFocoActivo();
    if (reciente && y + 8 < (anclaScrollProgramas.y || 0)) {
        if (foco) anclaScrollProgramas.foco = foco;
        return anclaScrollProgramas;
    }
    anclaScrollProgramas = {
        x,
        y,
        contenedores: capturarContenedoresScroll(),
        foco: foco || (reciente ? anclaScrollProgramas.foco : null)
    };
    if (!reciente) anclaScrollEn = ahora;
    return anclaScrollProgramas;
}

function campoSigueSiendoElActivo(foco) {
    const activo = document.activeElement;
    if (!activo || !activo.getAttribute || !foco) return false;
    if (activo.getAttribute('data-campo') !== foco.campo) return false;
    if (foco.concursanteId != null && foco.concursanteId !== '') {
        return activo.getAttribute('data-concursante-id') === String(foco.concursanteId);
    }
    return true;
}

function restaurarAnclaScroll(ancla, opciones = {}) {
    if (!ancla) return;
    const token = ++tokenRestauracionScroll;
    const enfocar = !!opciones.enfocar;
    const aplicar = () => {
        if (token !== tokenRestauracionScroll) return;
        const scroller = document.scrollingElement;
        try {
            window.scrollTo({ top: ancla.y || 0, left: ancla.x || 0, behavior: 'auto' });
        } catch (e) {
            try { window.scrollTo(ancla.x || 0, ancla.y || 0); } catch (e2) {}
        }
        if (scroller) {
            scroller.scrollTop = ancla.y || 0;
            scroller.scrollLeft = ancla.x || 0;
        }
        (ancla.contenedores || []).forEach(item => {
            const el = resolverContenedorScroll(item);
            if (!el) return;
            el.scrollTop = item.top || 0;
            el.scrollLeft = item.left || 0;
        });
        if (!enfocar || !ancla.foco) return;
        const activo = document.activeElement;
        const tagActivo = activo && activo.tagName ? activo.tagName.toLowerCase() : '';
        const enOtroCampo = ['input', 'textarea', 'select'].includes(tagActivo) && !campoSigueSiendoElActivo(ancla.foco);
        if (enOtroCampo || campoSigueSiendoElActivo(ancla.foco)) return;
        const campo = buscarCampoEditado(ancla.foco);
        if (!campo || (document.contains && !document.contains(campo)) || typeof campo.focus !== 'function') return;
        try {
            campo.focus({ preventScroll: true });
        } catch (e) {
            try { campo.focus(); } catch (e2) {}
        }
    };
    aplicar();
    if (opciones.soloUnaVez) return;
    setTimeout(aplicar, 0);
    if (typeof requestAnimationFrame === 'function') {
        requestAnimationFrame(() => {
            aplicar();
            requestAnimationFrame(aplicar);
        });
    }
}

function cancelarRestauracionScroll() {
    tokenRestauracionScroll++;
}

function instalarGuardiaScrollEdicion() {
    if (instalarGuardiaScrollEdicion.hecho || !document.addEventListener) return;
    instalarGuardiaScrollEdicion.hecho = true;
    const esCampoListado = (el) => {
        if (!el || !el.closest) return false;
        const tag = (el.tagName || '').toLowerCase();
        if (!['input', 'textarea', 'select'].includes(tag)) return false;
        if (el.closest('.modal')) return false;
        return !!el.closest('#contenedor-programas');
    };
    document.addEventListener('pointerdown', (evento) => {
        const origen = evento.target && evento.target.closest
            ? evento.target.closest('input, textarea, select')
            : null;
        if (!esCampoListado(origen)) return;
        capturarAnclaScroll();
    }, true);
    document.addEventListener('focusin', (evento) => {
        if (!esCampoListado(evento.target)) return;
        const corregir = () => {
            const y = window.scrollY || window.pageYOffset || 0;
            if (anclaScrollProgramas && y + 8 < (anclaScrollProgramas.y || 0) && (Date.now() - anclaScrollEn) < 400) {
                restaurarAnclaScroll(anclaScrollProgramas, { enfocar: false, soloUnaVez: true });
            }
        };
        corregir();
        setTimeout(corregir, 0);
    }, true);
}

async function conAnclaUndo(scrollSelectors, fn) {
    const manager = window.UndoManager;
    const selectores = Array.isArray(scrollSelectors) ? scrollSelectors.filter(Boolean) : [scrollSelectors].filter(Boolean);
    if (!manager || typeof manager.record !== 'function' || selectores.length === 0) {
        return fn();
    }
    if (anclaUndoProfundidad === 0) {
        recordUndoOriginal = manager.record.bind(manager);
        manager.record = (action) => {
            if (action && typeof action === 'object' && !action.scrollSelector && !action.scrollSelectors) {
                action.scrollSelectors = selectores.slice();
            }
            return recordUndoOriginal(action);
        };
    }
    anclaUndoProfundidad++;
    try {
        return await fn();
    } finally {
        anclaUndoProfundidad--;
        if (anclaUndoProfundidad === 0 && recordUndoOriginal) {
            manager.record = recordUndoOriginal;
            recordUndoOriginal = null;
        }
    }
}

window.capturarScrollVista = capturarAnclaScroll;
window.restaurarScrollVista = restaurarAnclaScroll;
window.cancelarRestauracionScroll = cancelarRestauracionScroll;
instalarGuardiaScrollEdicion();

function pintarVistaProgramasLocal() {
    const ancla = capturarAnclaScroll();
    if (aplicandoFiltros) {
        mostrarProgramasFiltrados();
        renderizarPaginacionFiltrada();
    } else {
        mostrarProgramas();
        renderizarPaginacion();
    }
    configurarScrollTablas();
    restaurarAnclaScroll(ancla, { enfocar: true });
}

function aplicarCampoProgramaEnPantalla(programaId, repetido, campos) {
    fusionarProgramaEnListas(programaId, repetido, campos);
    pintarVistaProgramasLocal();
}

async function persistirCampoPrograma(programaId, repetido, cuerpo, etiqueta) {
    if (rechazarSiProgramaEmitido(programaId, repetido)) {
        pintarVistaProgramasLocal();
        return false;
    }
    const campo = cuerpo && typeof cuerpo === 'object' ? Object.keys(cuerpo)[0] : '';
    return conAnclaUndo(selectoresCampoPrograma(programaId, campo), async () => {
        const base = urlBasePrograma(programaId, repetido);
        const actualizado = await apiManager.patchUndoable(
            `${base}/campo`,
            cuerpo,
            {
                label: etiqueta,
                snapshotEndpoint: base,
                mapRevert: mapRevertCampoPrograma,
                onAfter: (data, _anterior, _fase, respuesta) => aplicarCampoProgramaEnPantalla(
                    programaId,
                    repetido,
                    (respuesta && typeof respuesta === 'object' && respuesta.id != null) ? respuesta : data
                )
            }
        );
        if (actualizado && typeof actualizado === 'object' && actualizado.id != null) {
            aplicarCampoProgramaEnPantalla(programaId, repetido, actualizado);
        }
        return true;
    });
}

async function actualizarCreditosEspecialesPrograma(programaId, creditos, repetido = false) {
    try {
        const valor = (creditos != null && String(creditos).trim() !== '') ? String(creditos) : null;
        const guardado = await persistirCampoPrograma(
            programaId,
            repetido,
            { creditosEspeciales: valor },
            `Créditos especiales programa ${programaId}`
        );
        if (guardado) mostrarExito('Créditos especiales actualizados');
    } catch (error) {
        mostrarError(obtenerMensajeErrorProgramas(error, 'actualización de créditos especiales'));
    }
}

async function actualizarCodigoPrograma(programaId, codigo, repetido = false) {
    try {
        const valor = (codigo || '').trim();
        if (!valor) {
            throw new Error('El código de programa es obligatorio.');
        }
        const guardado = await persistirCampoPrograma(programaId, repetido, { codigo: valor }, `Código programa ${valor}`);
        if (guardado) mostrarExito('Código de programa actualizado');
    } catch (error) {
        mostrarError(obtenerMensajeErrorProgramas(error, 'actualización del código de programa'));
    }
}

async function actualizarNotasPrograma(programaId, notas, repetido = false) {
    try {
        const valor = (notas != null && String(notas).trim() !== '') ? String(notas) : null;
        const guardado = await persistirCampoPrograma(programaId, repetido, { notas: valor }, `Notas programa ${programaId}`);
        if (guardado) mostrarExito('Notas del programa actualizadas');
    } catch (error) {
        mostrarError(obtenerMensajeErrorProgramas(error, 'actualización de notas del programa'));
    }
}

function autoResizeTextareasEnProgramas() {
    const textareas = document.querySelectorAll('.concursantes-table textarea.campo-editable');
    const autoResize = (el) => {
        if (el.dataset.userResized === '1') return;
        el.style.height = 'auto';
        el.style.height = `${el.scrollHeight}px`;
    };
    textareas.forEach((ta) => {
        autoResize(ta);
        ta.addEventListener('input', () => autoResize(ta));
        ta.addEventListener('mousedown', () => {
            const startW = ta.offsetWidth;
            const startH = ta.offsetHeight;
            const onUp = () => {
                if (Math.abs(ta.offsetWidth - startW) > 2 || Math.abs(ta.offsetHeight - startH) > 2) {
                    ta.dataset.userResized = '1';
                }
                window.removeEventListener('mouseup', onUp);
            };
            window.addEventListener('mouseup', onUp);
        });
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

    const matchIsoPrefix = valor.match(/^(\d{4})-(\d{1,2})-(\d{1,2})/);
    if (matchIsoPrefix) {
        const [, año, mes, dia] = matchIsoPrefix;
        return `${año}-${mes.padStart(2, '0')}-${dia.padStart(2, '0')}`;
    }

    return valor;
}

function coincideFechaPrograma(programa, fechaFiltro) {
    if (!fechaFiltro) return true;
    const fechaPrograma = normalizarFechaProgramaISO(programa.fechaEmision);
    return fechaPrograma !== '' && fechaPrograma === fechaFiltro;
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
            if (partes.length === 3) {
                const horas   = parseInt(partes[0]) || 0;
                const minutos = parseInt(partes[1]) || 0;
                const segundos = parseInt(partes[2]) || 0;
                totalSegundos += horas * 3600 + minutos * 60 + segundos;
            } else if (partes.length === 2) {
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
    const ancla = capturarAnclaScroll();
    try {
        const snapshot = await apiManager.get(`/api/concursantes/${concursanteId}`);
        const data = {};
        data[campo] = valor;

        const aplicarCambioLocal = async () => {
            await apiManager.patch(`/api/concursantes/${concursanteId}/campo`, data);
            mostrarExito('Campo actualizado correctamente');

            let programaId = null;
            for (const pId in concursantesPorPrograma) {
                const concursante = concursantesPorPrograma[pId].find(c => c.id === concursanteId);
                if (concursante) {
                    concursante[campo] = valor;
                    programaId = pId;
                    break;
                }
            }

            if (programaId && (campo === 'resultado' || campo === 'duracion' || campo === 'duracionDireccion' || campo === 'duracionFinal')) {
                const programaContainer = document.querySelector(`[data-programa-id="${programaId}"]`);
                if (programaContainer) {
                    const concursantes = concursantesPorPrograma[programaId] || [];
                    const duracionReal = calcularDuracionReal(concursantes);
                    const programa = programas.find(p => p.id === programaId);
                    const duracionObjetivo = programa ? (programa.duracionObjetivo || '45m') : '45m';
                    const gap = calcularGap(duracionObjetivo, duracionReal);
                    const gapElement = programaContainer.querySelector('[data-campo="gap"] .programa-info-readonly');
                    if (gapElement) gapElement.textContent = gap;

                    if (campo === 'resultado') {
                        const nuevoTotalResultados = calcularTotalResultados(concursantes);
                        const premiosElement = programaContainer.querySelector('[data-campo="premios"] .programa-info-readonly');
                        if (premiosElement) {
                            premiosElement.textContent = nuevoTotalResultados + '€';
                        }
                    }

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

                    if (campo === 'resultado' || campo === 'duracion' || campo === 'duracionDireccion' || campo === 'duracionFinal') {
                        await actualizarEstadoProgramaAutomatico(programaId, { registrarUndo: false, refrescar: false });
                    }
                }
            }
        };

        const revertirCambio = async () => {
            const revert = { [campo]: snapshot[campo] ?? null };
            await apiManager.patch(`/api/concursantes/${concursanteId}/campo`, revert);
            await refrescarVistaProgramas();
        };

        await aplicarCambioLocal();
        if (window.UndoManager) {
            window.UndoManager.record({
                do: aplicarCambioLocal,
                undo: revertirCambio,
                label: `Actualizar concursante ${concursanteId} - ${campo}`,
                scrollSelectors: selectoresCampoConcursante(concursanteId, campo)
            });
        }

    } catch (error) {
        mostrarError(obtenerMensajeErrorProgramas(error, 'actualización de campo'));
    } finally {
        restaurarAnclaScroll(ancla, { enfocar: true });
    }
}

async function actualizarDuracionObjetivoPrograma(programaId, nuevaDuracion) {
    try {
        if (rechazarSiProgramaEmitido(programaId, false)) {
            pintarVistaProgramasLocal();
            return;
        }
        if (!nuevaDuracion || nuevaDuracion.trim() === '') {
            nuevaDuracion = '45m';
        }

        const actualizado = await conAnclaUndo(selectoresCampoPrograma(programaId, 'duracionObjetivo'), () => apiManager.patchUndoable(
            `/api/programas/${programaId}/duracion-objetivo`,
            { duracionObjetivo: nuevaDuracion },
            {
                label: `Duración objetivo programa ${programaId}`,
                snapshotEndpoint: `/api/programas/${programaId}`,
                mapRevert: (_key, raw) => raw ?? '45m',
                onAfter: (data, _anterior, _fase, respuesta) => aplicarCampoProgramaEnPantalla(
                    programaId,
                    false,
                    (respuesta && typeof respuesta === 'object' && respuesta.id != null) ? respuesta : data
                )
            }
        ));
        if (actualizado && typeof actualizado === 'object' && actualizado.id != null) {
            aplicarCampoProgramaEnPantalla(programaId, false, actualizado);
        }
        mostrarExito('Duración objetivo actualizada');
    } catch (error) {
        mostrarError(obtenerMensajeErrorProgramas(error, 'actualización de duración objetivo'));
    }
}

function irAConcursante(concursanteId) {
    Utils.abrirEnNuevaPestana(`concursantes.html?id=${concursanteId}`);
}

// Variables para filtros globales
let programasFiltrados = [];
let aplicandoFiltros = false;

function hayFiltrosProgramasActivos() {
    const listado = document.getElementById('filtro-listado-programa')?.value || '';
    if (listado === 'repetidos') return true;
    return ['filtro-estado-programa', 'filtro-temporada', 'filtro-programa-id', 'filtro-fecha-emision']
        .some(id => {
            const valor = document.getElementById(id)?.value;
            return valor != null && String(valor).trim() !== '';
        });
}

function filtrarProgramas() {
    paginaActual = 0;
    clearTimeout(filtrarProgramasTimeout);
    filtrarProgramasTimeout = setTimeout(() => {
        if (!hayFiltrosProgramasActivos()) {
            aplicandoFiltros = false;
            programasFiltrados = [];
            vistaRepetidos = false;
            cargarProgramasPaginados(0);
            return;
        }
        aplicandoFiltros = true;
        cargarProgramasFiltrados();
    }, 400);
}

// Nueva función para cargar programas con filtros aplicados
function urlConcursantesDePrograma(programa) {
    if (programa && programa.repetido) {
        return `/api/programas/repetidos/${programa.id}/concursantes`;
    }
    return `/api/concursantes/programa/${programa.id}`;
}

async function cargarProgramasFiltrados() {
    if (cargandoProgramas) {
        return;
    }
    cargandoProgramas = true;
    mostrarEstadoCargaProgramas('Obteniendo programas...', 10);
    const anclaScroll = capturarAnclaScroll();
    try {
        const listado = document.getElementById('filtro-listado-programa')?.value || '';
        vistaRepetidos = listado === 'repetidos';
        const response = await apiManager.get(vistaRepetidos ? '/api/programas/repetidos' : '/api/programas');
        const todosLosProgramas = response;

        mostrarEstadoCargaProgramas('Aplicando filtros...', 30);
        programasFiltrados = aplicarFiltros(todosLosProgramas)
            .sort((a, b) => b.id - a.id);
        const paginas = Math.ceil(programasFiltrados.length / tamañoPagina) || 0;
        if (paginaActual >= paginas) {
            paginaActual = Math.max(0, paginas - 1);
        }

        await cargarConcursantesParaProgramasFiltrados();

        mostrarProgramasFiltrados();
        renderizarPaginacionFiltrada();
        restaurarAnclaScroll(anclaScroll, { enfocar: false });

    } catch (error) {
        if (error && error.message && error.message.startsWith('401')) {
            return;
        }
        mostrarError(obtenerMensajeErrorProgramas(error, 'carga de programas filtrados'));
    } finally {
        cargandoProgramas = false;
        ocultarEstadoCargaProgramas();
    }
}

// Concursantes solo de la página visible. El catálogo filtrado no trae concursantes.
async function cargarConcursantesParaProgramasFiltrados() {
    const pagina = programasDeLaPaginaFiltrada();
    if (pagina.length === 0) {
        concursantesPorPrograma = {};
        actualizarProgresoCargaProgramas(1, 1, 'Sin resultados');
        return;
    }
    await cargarConcursantesDeProgramas(pagina);
}

// Mostrar programas filtrados con la misma página que el listado normal
function mostrarProgramasFiltrados() {
    const inicio = paginaActual * tamañoPagina;
    const fin = inicio + tamañoPagina;
    const programasVisibles = programasFiltrados.slice(inicio, fin);
    
    // Actualizar variables globales para la paginación
    programas = programasVisibles;
    totalItems = programasFiltrados.length;
    totalPaginas = Math.ceil(programasFiltrados.length / tamañoPagina);
    
    mostrarProgramas();
    configurarScrollTablas();
}

// Renderizar paginación para resultados filtrados
function renderizarPaginacionFiltrada() {
    const paginacionEl = document.getElementById('paginacion-programas');
    const seccionPag = document.getElementById('seccion-paginacion-programas');
    if (!paginacionEl) return;

    // Actualizar info de paginación (arriba y abajo)
    const infosEl = [
        document.getElementById('info-paginacion-programas'),
        document.getElementById('info-paginacion-programas-top')
    ].filter(Boolean);
    const wrapTop = document.getElementById('info-paginacion-programas-top-wrap');
    if (infosEl.length && totalItems > 0) {
        const inicio = paginaActual * tamañoPagina + 1;
        const fin = Math.min((paginaActual + 1) * tamañoPagina, totalItems);
        const textoInfo = `Mostrando ${inicio}-${fin} de ${totalItems} programas`;
        infosEl.forEach(el => { el.textContent = textoInfo; });
        if (seccionPag) seccionPag.style.display = '';
        if (wrapTop) wrapTop.style.display = '';
    }

    paginacionEl.innerHTML = '';
    if (totalPaginas <= 1) return;

    // Primera
    const primera = document.createElement('li');
    primera.className = `page-item ${paginaActual === 0 ? 'disabled' : ''}`;
    primera.innerHTML = `<a class="page-link" href="#" onclick="cambiarPaginaFiltrada(0); return false;">Primera</a>`;
    paginacionEl.appendChild(primera);

    // Anterior
    const anterior = document.createElement('li');
    anterior.className = `page-item ${paginaActual === 0 ? 'disabled' : ''}`;
    anterior.innerHTML = `<a class="page-link" href="#" onclick="cambiarPaginaFiltrada(${paginaActual - 1}); return false;">Anterior</a>`;
    paginacionEl.appendChild(anterior);

    // Números de página
    const inicioPagina = Math.max(0, paginaActual - 2);
    const finPagina = Math.min(totalPaginas - 1, paginaActual + 2);
    for (let i = inicioPagina; i <= finPagina; i++) {
        const li = document.createElement('li');
        li.className = `page-item ${i === paginaActual ? 'active' : ''}`;
        li.innerHTML = `<a class="page-link" href="#" onclick="cambiarPaginaFiltrada(${i}); return false;">${i + 1}</a>`;
        paginacionEl.appendChild(li);
    }

    // Siguiente
    const siguiente = document.createElement('li');
    siguiente.className = `page-item ${paginaActual === totalPaginas - 1 ? 'disabled' : ''}`;
    siguiente.innerHTML = `<a class="page-link" href="#" onclick="cambiarPaginaFiltrada(${paginaActual + 1}); return false;">Siguiente</a>`;
    paginacionEl.appendChild(siguiente);

    // Última
    const ultima = document.createElement('li');
    ultima.className = `page-item ${paginaActual === totalPaginas - 1 ? 'disabled' : ''}`;
    ultima.innerHTML = `<a class="page-link" href="#" onclick="cambiarPaginaFiltrada(${totalPaginas - 1}); return false;">Última</a>`;
    paginacionEl.appendChild(ultima);
}

// Cambiar página en resultados filtrados: solo los concursantes de esa página
async function cambiarPaginaFiltrada(nuevaPagina) {
    if (nuevaPagina < 0 || nuevaPagina >= totalPaginas || cargandoProgramas) return;

    paginaActual = nuevaPagina;
    cargandoProgramas = true;
    mostrarEstadoCargaProgramas('Cargando concursantes...', 40);
    try {
        await cargarConcursantesParaProgramasFiltrados();
        mostrarProgramasFiltrados();
        renderizarPaginacionFiltrada();
        configurarScrollTablas();
    } catch (error) {
        mostrarError(obtenerMensajeErrorProgramas(error, 'carga de programas filtrados'));
    } finally {
        cargandoProgramas = false;
        ocultarEstadoCargaProgramas();
    }
}

function aplicarFiltros(lista) {
    const estadoFiltro = (document.getElementById('filtro-estado-programa')?.value || '').toLowerCase();
    const temporadaFiltro = document.getElementById('filtro-temporada')?.value;
    const programaIdFiltro = document.getElementById('filtro-programa-id')?.value;
    const fechaFiltro = document.getElementById('filtro-fecha-emision')?.value; // YYYY-MM-DD

    return lista.filter(programa => {
        // Estado
        if (estadoFiltro && (!programa.estado || programa.estado.toLowerCase() !== estadoFiltro)) {
            return false;
        }
        // Temporada
        if (temporadaFiltro && String(programa.temporada) !== String(temporadaFiltro)) {
            return false;
        }
        // Código de programa, manteniendo coincidencia por ID interno durante la transición.
        if (programaIdFiltro
                && String(programa.codigo || '').toLowerCase() !== String(programaIdFiltro).toLowerCase()
                && String(programa.id) !== String(programaIdFiltro)) {
            return false;
        }
        // Fecha de emisión exacta (normalizada a YYYY-MM-DD)
        if (!coincideFechaPrograma(programa, fechaFiltro)) {
            return false;
        }
        return true;
    });
}

function limpiarFiltrosProgramas() {
    ['filtro-listado-programa','filtro-estado-programa','filtro-temporada','filtro-programa-id','filtro-fecha-emision']
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
    const inputCodigo = document.getElementById('codigo-programa');
    inputCodigo.value = '';
    inputCodigo.placeholder = 'Automático si se deja vacío';
    const ayudaCodigo = document.getElementById('codigo-programa-ayuda');
    if (ayudaCodigo) {
        ayudaCodigo.textContent = 'Si lo dejas vacío, al guardar se asigna el último número de programa + 1. El ID interno no cambia.';
    }
    document.getElementById('modal-programa-titulo').textContent = 'Nuevo Programa';
    
    const modal = new bootstrap.Modal(document.getElementById('modal-programa'));
    modal.show();
    sugerirCodigoPrograma();
}

async function sugerirCodigoPrograma() {
    try {
        const sugerido = await apiManager.get('/api/programas/siguiente-codigo');
        const codigo = sugerido && sugerido.codigo;
        const inputCodigo = document.getElementById('codigo-programa');
        const programaId = document.getElementById('programa-id').value;
        if (!codigo || programaId || !inputCodigo || inputCodigo.value) {
            return;
        }
        inputCodigo.placeholder = 'Sugerido: ' + codigo;
        const ayudaCodigo = document.getElementById('codigo-programa-ayuda');
        if (ayudaCodigo) {
            ayudaCodigo.textContent = 'Sugerido: ' + codigo + '. Si lo dejas vacío, se asigna al guardar (último número + 1). El ID interno no cambia.';
        }
    } catch (error) {
        // La asignación real sigue haciéndose en el servidor al crear.
    }
}

async function guardarPrograma() {
    const programaId = document.getElementById('programa-id').value;
    const temporada = document.getElementById('temporada-programa').value;
    const fechaEmision = document.getElementById('fecha-emision').value || null; // Puede ser null
    const codigo = (document.getElementById('codigo-programa').value || '').trim() || null;
    
    if (!temporada) {
        mostrarError('La temporada es obligatoria');
        return;
    }
    
    const programaData = {
        temporada: parseInt(temporada),
        fechaEmision,
        codigo: codigo || (programaId ? String(programaId) : null)
    };
    
    try {
        if (programaId) {
            // Actualizar (PUT) y reforzar campos críticos con PATCH
            await apiManager.putUndoable(`/api/programas/${programaId}`, programaData, { label: `Actualizar programa ${programaId}` });
            await apiManager.patch(`/api/programas/${programaId}/campo`, { temporada: parseInt(temporada) });
            await apiManager.patch(`/api/programas/${programaId}/campo`, { fechaEmision });
            await apiManager.patch(`/api/programas/${programaId}/campo`, { codigo: codigo || programaId.toString() });
            mostrarExito('Programa actualizado correctamente');
        } else {
            const creado = await apiManager.postUndoable('/api/programas', programaData, {
                label: 'Crear programa',
                idExtractor: (r) => r?.id || r?.datos?.id,
                deleteEndpointBuilder: (id) => `/api/programas/${id}`
            });
            mostrarExito(creado && creado.codigo
                ? `Programa ${creado.codigo} creado correctamente`
                : 'Programa creado correctamente');
        }
        
        bootstrap.Modal.getInstance(document.getElementById('modal-programa')).hide();
        await refrescarVistaProgramas();
        
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
        const endpoint = `/api/concursantes/${concursanteId}/foto`;
        await conAnclaUndo(selectoresCampoConcursante(concursanteId), () => apiManager.postFormDataUndoable(endpoint, formData, {
            label: `Cambiar foto del concursante ${concursanteId}`,
            redo: async () => {
                const rehacer = new FormData();
                rehacer.append('foto', file);
                await apiManager.postMultipart(endpoint, rehacer);
            }
        }));

        // Actualizar la vista
        await refrescarVistaProgramas();
        mostrarMensaje('Foto subida correctamente', 'success');
        
    } catch (error) {
        console.error('Error al subir foto:', error);
        mostrarError(obtenerMensajeErrorProgramas(error, 'subida de foto'));
    }
}

// Función para mostrar mensajes
function mostrarMensaje(mensaje, tipo = 'info') {
    if (tipo === 'success') {
        Utils.mostrarToastExito(mensaje);
        return;
    }
    if (tipo === 'error') {
        Utils.mostrarToastError(mensaje);
        return;
    }
    Utils.showAlert(mensaje, 'info', 3000);
}

// Función para actualizar el estado del programa automáticamente
async function actualizarEstadoProgramaAutomatico(programaId, opciones = {}) {
    const { registrarUndo = true, refrescar = true } = opciones;
    try {
        const snapshot = registrarUndo ? await apiManager.get(`/api/programas/${programaId}`) : null;
        const actualizado = await apiManager.put(`/api/programas/${programaId}/actualizar-estado`, {});

        if (actualizado && typeof actualizado === 'object') {
            const idx = programas.findIndex(p => p.id === programaId);
            if (idx !== -1) {
                programas[idx] = { ...programas[idx], ...actualizado };
            }
            actualizarEstadoProgramaEnDom(programaId, actualizado.estado);
        }

        if (registrarUndo && snapshot && window.UndoManager) {
            const doAction = async () => {
                await apiManager.put(`/api/programas/${programaId}/actualizar-estado`, {});
                await refrescarVistaProgramas();
            };
            const undoAction = async () => {
                await apiManager.put(`/api/programas/${programaId}`, snapshot);
                await refrescarVistaProgramas();
            };
            window.UndoManager.record({
                do: doAction,
                undo: undoAction,
                label: `Actualizar estado programa ${programaId}`,
                scrollSelectors: selectoresCampoPrograma(programaId, 'estado')
            });
        }

        if (refrescar) {
            await refrescarVistaProgramas();
        }
    } catch (error) {
        console.error('Error al actualizar estado del programa:', error);
    }
}

function mensajeConcursantesSinEditar(programaId) {
    const lista = concursantesPorPrograma[programaId] || concursantesPorPrograma[String(programaId)];
    if (!Array.isArray(lista)) return '';
    const numeros = [];
    let sinNumero = 0;
    lista.forEach(concursante => {
        const estado = String(concursante?.estado || '').trim().toLowerCase();
        if (estado === 'editado') return;
        const numero = concursante?.numeroConcursante;
        if (numero == null || numero === '') sinNumero += 1;
        else numeros.push(Number(numero));
    });
    const unicos = [...new Set(numeros)].filter(n => Number.isFinite(n)).sort((a, b) => a - b);
    const etiquetas = unicos.map(String);
    if (sinNumero === 1) etiquetas.push('sin número');
    else if (sinNumero > 1) etiquetas.push(`${sinNumero} sin número`);
    if (!etiquetas.length) return '';
    if (etiquetas.length === 1 && sinNumero === 0) {
        return `No se puede pasar el programa a editado: el concursante ${etiquetas[0]} no está editado.`;
    }
    return `No se puede pasar el programa a editado: los concursantes ${etiquetas.join(', ')} no están editados.`;
}

function restaurarSelectEstadoPrograma(programaId, repetido) {
    const programa = buscarProgramaListado(programaId, repetido);
    const estado = programa?.estado || '';
    if (!estado || typeof document.querySelectorAll !== 'function') return;
    document.querySelectorAll(`[data-programa-id="${programaId}"]`).forEach(contenedor => {
        const select = contenedor.querySelector('select[onchange*="actualizarEstadoPrograma"]');
        if (select) select.value = estado;
    });
}

async function actualizarEstadoPrograma(programaId, nuevoEstado, repetido = false) {
    if (String(nuevoEstado || '').trim().toLowerCase() === 'editado') {
        const mensaje = mensajeConcursantesSinEditar(programaId);
        if (mensaje) {
            restaurarSelectEstadoPrograma(programaId, repetido);
            mostrarError(mensaje);
            return;
        }
    }
    try {
        const guardado = await persistirCampoPrograma(
            programaId,
            repetido,
            { estado: nuevoEstado },
            `Estado programa ${programaId}`
        );
        if (guardado) mostrarExito('Estado del programa actualizado');
    } catch (error) {
        restaurarSelectEstadoPrograma(programaId, repetido);
        mostrarError(obtenerMensajeErrorProgramas(error, 'actualización del estado'));
    }
}

function renderOpcionEstado(estadoActual, valor, texto) {
    const selected = estadoActual === valor ? 'selected' : '';
    return `<option value="${valor}" ${selected}>${texto}</option>`;
}

async function actualizarTemporadaPrograma(programaId, nuevaTemporada, repetido = false) {
    try {
        const numero = parseInt(nuevaTemporada);
        if (!numero || numero < 1) {
            mostrarError('La temporada debe ser un número mayor o igual a 1');
            return;
        }
        const guardado = await persistirCampoPrograma(
            programaId,
            repetido,
            { temporada: numero },
            `Temporada programa ${programaId}`
        );
        if (guardado) mostrarExito('Temporada actualizada');
    } catch (error) {
        mostrarError(obtenerMensajeErrorProgramas(error, 'actualización de temporada'));
    }
}

async function actualizarFechaEmision(programaId, fechaISO, repetido = false) {
    try {
        const valor = (fechaISO && fechaISO.trim() !== '') ? fechaISO : null;
        const cuerpo = { fechaEmision: valor };
        if (esFlagRepetido(repetido) && valor) {
            const programa = buscarProgramaListado(programaId, repetido);
            if ((programa?.estado || '').toLowerCase() === 'editado') {
                cuerpo.estado = 'programado';
            }
        }
        const guardado = await persistirCampoPrograma(
            programaId,
            repetido,
            cuerpo,
            `Fecha emisión programa ${programaId}`
        );
        if (guardado) mostrarExito('Fecha de emisión actualizada');
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

const TAMANO_PAGINA_CONCURSANTES_DISPONIBLES = 50;
const TAMANO_PAGINA_CONCURSANTES_REPETIDO = 25;
const ESTADOS_ASIGNABLES_PROGRAMA = ['GRABADO', 'EDITADO', 'EMITIDO'];

function tamanoPaginaConcursantesDisponibles() {
    return vistaRepetidos ? TAMANO_PAGINA_CONCURSANTES_REPETIDO : TAMANO_PAGINA_CONCURSANTES_DISPONIBLES;
}

function duracionConcursanteEnSegundos(duracion) {
    const texto = (duracion || '').trim();
    const match = texto.match(/^(\d{1,3}):([0-5]\d)$/);
    if (!match) return null;
    return Number(match[1]) * 60 + Number(match[2]);
}

// En un repetido ya creado el margen es ±30 s. En un programa normal la duración es exacta.
// Vacío: sin filtro. Formato inválido: { invalido: true }.
function margenFiltroTiempoDisponibles() {
    return vistaRepetidos ? 30 : 0;
}

function filtroTiempoDesdeTexto(texto) {
    const limpio = (texto || '').trim();
    if (!limpio) return null;
    const segundos = duracionConcursanteEnSegundos(limpio);
    if (segundos === null) return { invalido: true };
    const margen = margenFiltroTiempoDisponibles();
    return { min: segundos - margen, max: segundos + margen };
}

function textoAyudaFiltroTiempoDisponibles() {
    const prioridad = 'Duración de más prioridad: final, si no dirección, si no grabación.';
    if (vistaRepetidos) {
        return `Margen de ±30 segundos. ${prioridad}`;
    }
    return `Duración exacta (MM:SS). ${prioridad}`;
}

function actualizarAyudaFiltroTiempoDisponibles() {
    const ayuda = document.getElementById('ayuda-filtro-tiempo-concursante-disponible');
    if (ayuda) ayuda.textContent = textoAyudaFiltroTiempoDisponibles();
}

function duracionCabeEnFiltroTiempo(duracion, filtro) {
    if (!filtro) return true;
    if (filtro.invalido) return false;
    const segundos = duracionConcursanteEnSegundos(duracion);
    if (segundos === null) return false;
    return segundos >= filtro.min && segundos <= filtro.max;
}

function extraerPaginaConcursantes(response) {
    if (!response) {
        return { content: [], totalElements: 0, totalPages: 1 };
    }
    if (Array.isArray(response)) {
        return { content: response, totalElements: response.length, totalPages: 1 };
    }
    const content = response.content || response.datos?.content || [];
    const lista = Array.isArray(content) ? content : [];
    return {
        content: lista,
        totalElements: response.totalElements ?? response.total_elements ?? lista.length,
        totalPages: response.totalPages ?? response.total_pages ?? 1
    };
}

// emitido = exactamente 1 programa; repetido = más de 1. No es el texto del campo estado.
function esFiltroPorSalidas(valor) {
    const normalizado = (valor || '').trim().toLowerCase();
    return normalizado === 'emitido' || normalizado === 'repetido';
}

function estadoFiltroConcursantesDisponibles() {
    const valor = (document.getElementById('filtro-estado-concursante-disponible')?.value || '').trim().toLowerCase();
    if (esFiltroPorSalidas(valor)) {
        return valor;
    }
    return ESTADOS_ASIGNABLES_PROGRAMA.includes(valor.toUpperCase()) ? valor : '';
}

function urlConcursantesDisponibles(pagina = 0, busqueda = '') {
    const programaId = document.getElementById('programa-seleccionado-id')?.value;
    const tamano = tamanoPaginaConcursantesDisponibles();
    if (vistaRepetidos && programaId) {
        let url = `/api/programas/repetidos/${encodeURIComponent(programaId)}/disponibles?page=${pagina}&size=${tamano}`;
        if (busqueda) {
            url += `&busqueda=${encodeURIComponent(busqueda)}`;
        }
        const estado = estadoFiltroConcursantesDisponibles();
        if (estado) {
            url += `&estado=${encodeURIComponent(estado)}`;
        }
        return url;
    }
    let url = `/api/concursantes/disponibles?page=${pagina}&size=${tamano}`;
    if (busqueda) {
        url += `&busqueda=${encodeURIComponent(busqueda)}`;
    }
    const estado = estadoFiltroConcursantesDisponibles();
    if (estado) {
        url += `&estado=${encodeURIComponent(estado)}`;
    }
    if (programaId) {
        url += `&programaId=${encodeURIComponent(programaId)}`;
    }
    return url;
}

function aplicarPaginaConcursantesDisponibles(response, { acumular = false, pagina = 0 } = {}) {
    const paginaResp = extraerPaginaConcursantes(response);
    concursantesDisponibles = acumular
        ? [...concursantesDisponibles, ...paginaResp.content]
        : paginaResp.content;
    totalConcursantesDisponibles = paginaResp.totalElements;
    paginaConcursantesDisponibles = pagina;
    totalPaginasConcursantesDisponibles = paginaResp.totalPages || 1;
}

// Lugar, valoración y tiempo se aplican sobre la página cargada.
// Grabado y editado también, porque el servidor ya los acotó por la columna estado.
// Emitido y repetido los resuelve el servidor por número de programas: no se vuelven a comparar con el texto del estado.
function aplicarFiltrosConcursantesDisponiblesEnMemoria() {
    const lugarInput = document.getElementById('filtro-lugar-concursante-disponible');
    const valoracionSelect = document.getElementById('filtro-valoracion-final-concursante-disponible');
    const estadoSelect = document.getElementById('filtro-estado-concursante-disponible');
    const tiempoInput = document.getElementById('filtro-tiempo-concursante-disponible');

    const lugarFiltro = (lugarInput?.value || '').trim().toLowerCase();
    const valoracionFiltro = (valoracionSelect?.value || '').trim();
    const estadoFiltro = (estadoSelect?.value || '').trim().toUpperCase();
    const filtroTiempo = filtroTiempoDesdeTexto(tiempoInput?.value);

    return (concursantesDisponibles || []).filter(c => {
        const estado = (c.estado || '').trim().toUpperCase();

        if (!ESTADOS_ASIGNABLES_PROGRAMA.includes(estado)) {
            return false;
        }

        // Grabado/editado: la columna estado. Emitido/repetido ya vienen filtrados por salidas.
        if (estadoFiltro && !esFiltroPorSalidas(estadoFiltro) && estado !== estadoFiltro) {
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

        if (!duracionCabeEnFiltroTiempo(obtenerDuracionConcursante(c), filtroTiempo)) {
            return false;
        }

        return true;
    });
}

// Re-renderiza la tabla cuando cambian los filtros del modal
function onCambioFiltrosConcursantesDisponibles() {
    renderizarConcursantesDisponibles();
}

async function onCambioEstadoConcursantesDisponibles() {
    try {
        const filtro = document.getElementById('buscar-concursante-disponible')?.value.trim() || '';
        const response = await apiManager.get(urlConcursantesDisponibles(0, filtro));
        aplicarPaginaConcursantesDisponibles(response, { pagina: 0 });
        renderizarConcursantesDisponibles();
    } catch (error) {
        mostrarError(obtenerMensajeErrorProgramas(error, 'filtrado de concursantes'));
    }
}

async function mostrarConcursantesDisponibles(programaId, posicionPreferida = null) {
    if (!puedeGestionarConcursantesPrograma()) {
        mostrarError('No tienes permisos para añadir concursantes a programas.');
        return;
    }
    if (esProgramaEmitido(programaId) && !vistaRepetidos) {
        mostrarError('No se pueden añadir concursantes a un programa emitido.');
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
        const filtroTiempo = document.getElementById('filtro-tiempo-concursante-disponible');
        if (filtroLugar) filtroLugar.value = '';
        if (filtroValoracion) filtroValoracion.value = '';
        if (filtroEstado) filtroEstado.value = 'editado';
        if (filtroTiempo) filtroTiempo.value = '';
        actualizarAyudaFiltroTiempoDisponibles();
        
        const response = await apiManager.get(urlConcursantesDisponibles(0));
        aplicarPaginaConcursantesDisponibles(response, { pagina: 0 });

        renderizarConcursantesDisponibles();
        
        const modal = new bootstrap.Modal(document.getElementById('modal-añadir-concursantes'));
        modal.show();
    } catch (error) {
        mostrarError(obtenerMensajeErrorProgramas(error, 'carga de concursantes disponibles'));
    }
}

function htmlFotoSelectorConcursante(concursante) {
    const foto = concursante && concursante.foto ? String(concursante.foto).trim() : '';
    if (!foto) {
        return '';
    }
    const ruta = /^https?:\/\//i.test(foto) || foto.startsWith('/') ? foto : `/uploads/${foto}`;
    return `<img src="${Utils.escapeHtml(ruta)}" class="foto-selector" alt="Foto">`;
}

function textoInfoPaginacionConcursantes(cantidadVisible) {
    if (!vistaRepetidos) {
        return `Mostrando ${cantidadVisible} de ${totalConcursantesDisponibles} concursantes (Página ${paginaConcursantesDisponibles + 1} de ${totalPaginasConcursantesDisponibles})`;
    }
    const total = Number(totalConcursantesDisponibles) || 0;
    const tamano = tamanoPaginaConcursantesDisponibles();
    const paginas = Math.max(1, Number(totalPaginasConcursantesDisponibles) || 1);
    const pagina = Math.min(Math.max(0, paginaConcursantesDisponibles), paginas - 1);
    if (total <= 0) {
        return '0 resultados.';
    }
    const inicio = pagina * tamano + 1;
    const fin = Math.min((pagina + 1) * tamano, total);
    return `${total} resultados. Mostrando ${inicio}–${fin} de ${total}. Página ${pagina + 1} de ${paginas}`;
}

function pintarInfoPaginacionConcursantes(cantidadVisible) {
    const infoPaginacion = document.getElementById('info-paginacion-concursantes');
    if (infoPaginacion) {
        infoPaginacion.textContent = textoInfoPaginacionConcursantes(cantidadVisible);
    }
}

// En un repetido la lista es una página de 25. En un programa normal se mantiene «cargar más».
function sincronizarControlesConcursantesDisponibles() {
    const lista = document.getElementById('lista-concursantes-disponibles');
    if (lista) lista.classList.toggle('modo-repetido', !!vistaRepetidos);

    const cargarMas = document.getElementById('btn-cargar-mas-concursantes');
    const btnPrimera = document.getElementById('btn-primera-pagina');
    const btnAnt = document.getElementById('btn-pagina-anterior');
    const btnSig = document.getElementById('btn-pagina-siguiente');
    const btnUlt = document.getElementById('btn-ultima-pagina');
    if (!vistaRepetidos) {
        if (cargarMas) cargarMas.style.display = '';
        [btnPrimera, btnAnt, btnSig, btnUlt].forEach(boton => {
            if (boton) boton.disabled = false;
        });
        return;
    }
    const enPrimera = paginaConcursantesDisponibles <= 0;
    const enUltima = paginaConcursantesDisponibles >= totalPaginasConcursantesDisponibles - 1
        || (Number(totalConcursantesDisponibles) || 0) <= 0;
    if (cargarMas) cargarMas.style.display = 'none';
    if (btnPrimera) btnPrimera.disabled = enPrimera;
    if (btnAnt) btnAnt.disabled = enPrimera;
    if (btnSig) btnSig.disabled = enUltima;
    if (btnUlt) btnUlt.disabled = enUltima;
}

function renderizarConcursantesDisponibles() {
    const lista = document.getElementById('lista-concursantes-disponibles');
    sincronizarControlesConcursantesDisponibles();

    const infoPaginacion = document.getElementById('info-paginacion-concursantes');
    const filtroTiempo = filtroTiempoDesdeTexto(document.getElementById('filtro-tiempo-concursante-disponible')?.value);
    if (filtroTiempo && filtroTiempo.invalido) {
        if (infoPaginacion) {
            infoPaginacion.textContent = vistaRepetidos ? textoInfoPaginacionConcursantes(0) : '';
        }
        lista.innerHTML = '<div class="alert alert-warning">El tiempo debe tener formato MM:SS.</div>';
        return;
    }

    if (concursantesDisponibles.length === 0) {
        pintarInfoPaginacionConcursantes(0);
        lista.innerHTML = '<div class="alert alert-info">No hay concursantes sin asignar a un programa (grabado, editado o emitido).</div>';
        return;
    }

    const listaFiltrada = aplicarFiltrosConcursantesDisponiblesEnMemoria();
    pintarInfoPaginacionConcursantes(listaFiltrada.length);

    if (listaFiltrada.length === 0) {
        lista.innerHTML = vistaRepetidos
            ? '<div class="alert alert-warning">Ningún concursante de esta página cumple los filtros. Ajusta los filtros o pasa a otra página.</div>'
            : '<div class="alert alert-warning">Ningún concursante de los cargados cumple los filtros. Ajusta los filtros o carga más concursantes.</div>';
        return;
    }

    lista.innerHTML = `
        <div class="table-responsive">
            <table class="table table-hover">
                <thead>
                    <tr>
                        <th>Foto</th>
                        <th>Lugar</th>
                        <th>Nombre</th>
                        <th>Edad</th>
                        <th>Premio</th>
                        <th>Valoración final</th>
                        <th>Duración</th>
                        <th>Mom. destacados</th>
                    </tr>
                </thead>
                <tbody>
                    ${listaFiltrada.map(concursante => `
                        <tr style="cursor: pointer;" onclick="asignarConcursanteAPrograma(${concursante.id})">
                            <td>${htmlFotoSelectorConcursante(concursante)}</td>
                            <td>${Utils.escapeHtml(concursante.lugar || '')}</td>
                            <td><strong>${Utils.escapeHtml(concursante.nombre || '')}</strong></td>
                            <td>${Utils.escapeHtml(concursante.edad || '')}</td>
                            <td>${concursante.premio != null && concursante.premio !== '' ? Utils.escapeHtml(concursante.premio) : ''}</td>
                            <td>${Utils.escapeHtml(concursante.valoracionFinal || '')}</td>
                            <td>${Utils.escapeHtml(obtenerDuracionConcursante(concursante))}</td>
                            <td class="celda-momentos">${Utils.escapeHtml(concursante.momentosDestacados || '')}</td>
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

        const response = await apiManager.get(urlConcursantesDisponibles(0, filtro));
        aplicarPaginaConcursantesDisponibles(response, { pagina: 0 });

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
        if (esProgramaEmitido(programaId) && !vistaRepetidos) {
            mostrarError('No se pueden añadir concursantes a un programa emitido.');
            return;
        }

        const queryPos = (posicionPreferidaPrograma != null) ? `?posicion=${encodeURIComponent(posicionPreferidaPrograma)}` : '';
        const endpointAsignar = vistaRepetidos
            ? `/api/programas/repetidos/${programaId}/concursantes/${concursanteId}${queryPos}`
            : `/api/concursantes/${concursanteId}/asignar-programa/${programaId}${queryPos}`;
        const endpointQuitar = vistaRepetidos
            ? `/api/programas/repetidos/${programaId}/concursantes/${concursanteId}`
            : `/api/concursantes/${concursanteId}/desasignar-programa`;
        await conAnclaUndo([selectorProgramaVisible(programaId)], () => apiManager.postUndoable(endpointAsignar, {}, {
            label: vistaRepetidos
                ? `Asignar concursante ${concursanteId} al repetido ${programaId}`
                : `Asignar programa ${programaId} a concursante ${concursanteId}`,
            idExtractor: () => concursanteId,
            deleteEndpointBuilder: () => endpointQuitar
        }));
        
        // Cerrar modal
        const modal = bootstrap.Modal.getInstance(document.getElementById('modal-añadir-concursantes'));
        modal.hide();
        posicionPreferidaPrograma = null;
        
        // Recargar programas
        await refrescarVistaProgramas();
        
        mostrarMensaje('Concursante añadido al programa correctamente', 'success');
    } catch (error) {
        posicionPreferidaPrograma = null;
        mostrarError(obtenerMensajeErrorProgramas(error, 'asignación de concursante'));
    }
}

async function quitarConcursanteDePrograma(concursanteId, programaId, event) {
    event.stopPropagation();
    if (!(puedeGestionarConcursantesPrograma() && puedeEditarCamposConcursante())) {
        mostrarError('No tienes permisos para quitar concursantes de programas.');
        return;
    }
    if (esProgramaEmitido(programaId) && !vistaRepetidos) {
        mostrarError('No se pueden quitar concursantes de un programa emitido.');
        return;
    }
    
    if (!confirm('¿Estás seguro de que quieres quitar este concursante del programa?')) {
        return;
    }
    
    try {
        if (vistaRepetidos) {
            const orden = (concursantesPorPrograma[programaId] || [])
                .find(c => String(c.id) === String(concursanteId))?.ordenEscaleta;
            await apiManager.delete(`/api/programas/repetidos/${programaId}/concursantes/${concursanteId}`);
            if (window.UndoManager) {
                const queryPos = (orden != null) ? `?posicion=${encodeURIComponent(orden)}` : '';
                window.UndoManager.record({
                    do: async () => { await apiManager.delete(`/api/programas/repetidos/${programaId}/concursantes/${concursanteId}`); },
                    undo: async () => { await apiManager.post(`/api/programas/repetidos/${programaId}/concursantes/${concursanteId}${queryPos}`, {}); },
                    label: `Quitar concursante ${concursanteId} del repetido ${programaId}`,
                    scrollSelectors: [selectorProgramaVisible(programaId)]
                });
            }
            await refrescarVistaProgramas();
            mostrarMensaje('Concursante quitado del repetido correctamente', 'success');
            return;
        }

        // Capturar programa y posición actuales para poder deshacer reasignando
        let programaPrevio = null;
        let posicionPrevia = null;
        try {
            const snap = await apiManager.get(`/api/concursantes/${concursanteId}`);
            programaPrevio = snap?.numeroPrograma ?? null;
            posicionPrevia = snap?.ordenEscaleta ?? null;
        } catch (e) {
            console.warn('No se pudo capturar el programa previo del concursante:', e);
        }

        await apiManager.delete(`/api/concursantes/${concursanteId}/desasignar-programa`);

        if (window.UndoManager && programaPrevio != null) {
            const queryPos = (posicionPrevia != null) ? `?posicion=${encodeURIComponent(posicionPrevia)}` : '';
            window.UndoManager.record({
                do: async () => { await apiManager.delete(`/api/concursantes/${concursanteId}/desasignar-programa`); },
                undo: async () => { await apiManager.post(`/api/concursantes/${concursanteId}/asignar-programa/${programaPrevio}${queryPos}`, {}); },
                label: `Quitar concursante ${concursanteId} del programa ${programaPrevio}`,
                scrollSelectors: [selectorProgramaVisible(programaPrevio)]
            });
        }
        
        // Recargar programas
        await refrescarVistaProgramas();
        
        mostrarMensaje('Concursante quitado del programa correctamente', 'success');
    } catch (error) {
        mostrarError(obtenerMensajeErrorProgramas(error, 'desasignación de concursante'));
    }
}

async function editarPrograma(programaId) {
    try {
        await EditLockManager.tryAcquire('PROGRAMA', programaId);
        const programa = await apiManager.get(`/api/programas/${programaId}`);
        document.getElementById('programa-id').value = programa.id;
        document.getElementById('codigo-programa').value = programa.codigo || programa.id;
        document.getElementById('temporada-programa').value = programa.temporada;
        document.getElementById('fecha-emision').value = normalizarFechaProgramaISO(programa.fechaEmision);
        document.getElementById('modal-programa-titulo').textContent = 'Editar Programa';

        const modal = new bootstrap.Modal(document.getElementById('modal-programa'));
        modal.show();
        EditLockManager.startSession({
            entityType: 'PROGRAMA',
            entityId: programaId,
            modalSelector: '#modal-programa',
            onExpire: () => guardarPrograma()
        });
    } catch (error) {
        mostrarError(obtenerMensajeErrorProgramas(error, 'carga de datos del programa'));
    }
}

// Función para renderizar los controles de paginación
function renderizarPaginacion() {
    const paginacionEl = document.getElementById('paginacion-programas');
    const seccionPag = document.getElementById('seccion-paginacion-programas');
    if (!paginacionEl) return;

    paginacionEl.innerHTML = '';
    if (totalPaginas <= 1) {
        if (seccionPag && totalItems <= tamañoPagina) seccionPag.style.display = 'none';
        return;
    }

    // Primera
    const primera = document.createElement('li');
    primera.className = `page-item ${paginaActual === 0 ? 'disabled' : ''}`;
    primera.innerHTML = `<a class="page-link" href="#" onclick="cambiarPagina(0); return false;">Primera</a>`;
    paginacionEl.appendChild(primera);

    // Anterior
    const anterior = document.createElement('li');
    anterior.className = `page-item ${paginaActual === 0 ? 'disabled' : ''}`;
    anterior.innerHTML = `<a class="page-link" href="#" onclick="cambiarPagina(${paginaActual - 1}); return false;">Anterior</a>`;
    paginacionEl.appendChild(anterior);

    // Números de página
    const inicioR = Math.max(0, paginaActual - 2);
    const finR = Math.min(totalPaginas - 1, paginaActual + 2);
    for (let i = inicioR; i <= finR; i++) {
        const li = document.createElement('li');
        li.className = `page-item ${i === paginaActual ? 'active' : ''}`;
        li.innerHTML = `<a class="page-link" href="#" onclick="cambiarPagina(${i}); return false;">${i + 1}</a>`;
        paginacionEl.appendChild(li);
    }

    // Siguiente
    const siguiente = document.createElement('li');
    siguiente.className = `page-item ${paginaActual === totalPaginas - 1 ? 'disabled' : ''}`;
    siguiente.innerHTML = `<a class="page-link" href="#" onclick="cambiarPagina(${paginaActual + 1}); return false;">Siguiente</a>`;
    paginacionEl.appendChild(siguiente);

    // Última
    const ultima = document.createElement('li');
    ultima.className = `page-item ${paginaActual === totalPaginas - 1 ? 'disabled' : ''}`;
    ultima.innerHTML = `<a class="page-link" href="#" onclick="cambiarPagina(${totalPaginas - 1}); return false;">Última</a>`;
    paginacionEl.appendChild(ultima);
}

// Función para cambiar de página
async function cambiarPagina(nuevaPagina) {
    if (nuevaPagina < 0 || nuevaPagina >= totalPaginas) return;
    
    if (aplicandoFiltros) {
        await cambiarPaginaFiltrada(nuevaPagina);
        return;
    }

    paginaActual = nuevaPagina;
    await cargarProgramasPaginados(paginaActual);
}

document.addEventListener('DOMContentLoaded', inicializarProgramas); 

async function eliminarPrograma(programaId) {
    const programa = programas.find(p => String(p.id) === String(programaId));
    const esRepetido = !!(programa && programa.repetido);
    const url = esRepetido
        ? `/api/programas/repetidos/${programaId}`
        : `/api/programas/${programaId}`;
    const etiqueta = esRepetido ? 'programa repetido' : 'programa';
    if (!confirm(`¿Seguro que deseas borrar este ${etiqueta}?\n\nPodrás deshacerlo con Ctrl+Z durante la próxima hora.`)) return;
    try {
        await apiManager.deleteUndoable(url, { label: `Eliminar ${etiqueta} ${programaId}` });
        mostrarExito(esRepetido ? 'Programa repetido eliminado' : 'Programa eliminado');
        await refrescarVistaProgramas();
    } catch (error) {
        const msg = Utils.mensajeErrorApi(error, 'eliminar programas');
        let amigable = msg;
        if (msg.toLowerCase().includes('concursante')) {
            amigable = 'No se puede eliminar porque hay concursantes asignados. Desasigna los concursantes primero.';
        } else if (msg.toLowerCase().includes('programado')) {
            amigable = 'No se puede eliminar un programa programado. Cambia su estado a Borrador primero.';
        } else if (msg.toLowerCase().includes('emitido')) {
            amigable = 'No se puede eliminar un programa emitido.';
        }
        mostrarError(amigable);
    }
}

// Función para configurar scroll en las tablas de concursantes (barra horizontal; cabecera sin barra)
function configurarScrollTablas() {
    document.querySelectorAll('.concursantes-table').forEach(configurarScrollEnTabla);
}

// Sincroniza anchos de columna de la tabla de cabecera a la tabla de cuerpo (mismo programa).
// Con table-layout: fixed manda el <col>, así que se copia el colgroup, no las celdas.
function sincronizarAnchosCabeceraCuerpo(contenedor) {
    const headerTable = contenedor.querySelector('.concursantes-table-header-wrapper table');
    const bodyTable = contenedor.querySelector('.concursantes-table-body-wrapper table');
    if (!headerTable || !bodyTable) return;
    const headerCols = headerTable.querySelectorAll('colgroup col');
    const bodyCols = bodyTable.querySelectorAll('colgroup col');
    headerCols.forEach((col, colIndex) => {
        const ancho = col.style.width;
        if (!ancho || !bodyCols[colIndex]) return;
        bodyCols[colIndex].style.width = ancho;
    });
    if (headerTable.style.width) {
        bodyTable.style.width = headerTable.style.width;
        bodyTable.style.minWidth = headerTable.style.width;
    }
    // Re-ajustar altura de textareas tras cambio de anchos de columna
    requestAnimationFrame(() => {
        bodyTable.querySelectorAll('textarea.campo-editable').forEach(ta => {
            if (ta.dataset.userResized === '1') return;
            ta.style.height = 'auto';
            ta.style.height = ta.scrollHeight + 'px';
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
    if (vistaRepetidos) {
        await cargarPaginaConcursantesDisponibles(paginaConcursantesDisponibles + 1);
        return;
    }
    if (paginaConcursantesDisponibles >= totalPaginasConcursantesDisponibles - 1) {
        return; // Ya estamos en la última página
    }
    
    try {
        const siguientePagina = paginaConcursantesDisponibles + 1;
        const filtro = document.getElementById('buscar-concursante-disponible').value.trim();
        const response = await apiManager.get(urlConcursantesDisponibles(siguientePagina, filtro));
        aplicarPaginaConcursantesDisponibles(response, { acumular: true, pagina: siguientePagina });

        renderizarConcursantesDisponibles();
    } catch (error) {
        mostrarError(obtenerMensajeErrorProgramas(error, 'carga de más concursantes'));
    }
}

// Función para cargar página específica de concursantes disponibles
async function cargarPaginaConcursantesDisponibles(pagina) {
    const destino = Math.max(0, Number(pagina) || 0);
    const ultima = Math.max(0, (Number(totalPaginasConcursantesDisponibles) || 1) - 1);
    const paginaPedida = Math.min(destino, ultima);
    try {
        const filtro = document.getElementById('buscar-concursante-disponible').value.trim();
        const response = await apiManager.get(urlConcursantesDisponibles(paginaPedida, filtro));
        aplicarPaginaConcursantesDisponibles(response, { pagina: paginaPedida });

        renderizarConcursantesDisponibles();
        const lista = document.getElementById('lista-concursantes-disponibles');
        if (lista) lista.scrollTop = 0;
    } catch (error) {
        mostrarError(obtenerMensajeErrorProgramas(error, 'carga de página de concursantes'));
    }
}

// La semana del lunes 5 de octubre de 2026 es la 111.
// El contador sigue hacia atrás y hacia adelante desde esa fecha.
const ANCLA_SEMANA_PROGRAMACION = { anio: 2026, mes: 9, dia: 5, numero: 111 };
let numeroProgramacionSemana = null;
let tokenProgramacionSemana = 0;

function lunesAnclaProgramacion() {
    return new Date(
        ANCLA_SEMANA_PROGRAMACION.anio,
        ANCLA_SEMANA_PROGRAMACION.mes,
        ANCLA_SEMANA_PROGRAMACION.dia
    );
}

function isoDesdeFechaLocal(fecha) {
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${fecha.getFullYear()}-${mes}-${dia}`;
}

function diasEntreFechas(desde, hasta) {
    const utcDesde = Date.UTC(desde.getFullYear(), desde.getMonth(), desde.getDate());
    const utcHasta = Date.UTC(hasta.getFullYear(), hasta.getMonth(), hasta.getDate());
    return Math.round((utcHasta - utcDesde) / 86400000);
}

function numeroSemanaDeFecha(fecha) {
    return ANCLA_SEMANA_PROGRAMACION.numero + Math.floor(diasEntreFechas(lunesAnclaProgramacion(), fecha) / 7);
}

function maxNumeroSemana() {
    const horizonte = new Date().getFullYear() + 1;
    return Math.max(ANCLA_SEMANA_PROGRAMACION.numero, numeroSemanaDeFecha(new Date(horizonte, 11, 31)));
}

function diasIsoDeSemana(numero) {
    const lunes = lunesAnclaProgramacion();
    lunes.setDate(lunes.getDate() + (numero - ANCLA_SEMANA_PROGRAMACION.numero) * 7);
    const dias = [];
    for (let i = 0; i < 7; i++) {
        const fecha = new Date(lunes.getFullYear(), lunes.getMonth(), lunes.getDate() + i);
        dias.push(isoDesdeFechaLocal(fecha));
    }
    return dias;
}

function escaparHtmlHoja(valor) {
    return String(valor ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function urlFotoHoja(foto) {
    if (!foto) return '';
    const ruta = String(foto);
    if (/^https?:\/\//i.test(ruta) || ruta.startsWith('/')) return ruta;
    return `/uploads/${ruta}`;
}

function importePremioHoja(concursante) {
    if (concursante.premio !== null && concursante.premio !== undefined && concursante.premio !== '') {
        const valor = Number(concursante.premio);
        return Number.isNaN(valor) ? null : valor;
    }
    if (concursante.resultado !== null && concursante.resultado !== undefined && concursante.resultado !== '') {
        const valor = Number(concursante.resultado);
        return Number.isNaN(valor) ? null : valor;
    }
    return null;
}

function eurosNumeroHoja(valor) {
    const numero = Math.round(Number(valor));
    const texto = String(Math.abs(numero)).replace(/\B(?=(\d{3})+(?!\d))/g, '.');
    return `${numero < 0 ? '-' : ''}${texto} €`;
}

function eurosHoja(concursante) {
    const valor = importePremioHoja(concursante);
    if (valor === null) return '';
    return eurosNumeroHoja(valor);
}

function totalPremiosHoja(programa) {
    const concursantes = programa._concursantes || [];
    if (concursantes.length) {
        const total = concursantes.reduce((suma, concursante) => suma + (importePremioHoja(concursante) || 0), 0);
        return eurosNumeroHoja(total);
    }
    if (programa.totalPremios != null && programa.totalPremios !== '' && !Number.isNaN(Number(programa.totalPremios))) {
        return eurosNumeroHoja(programa.totalPremios);
    }
    return '';
}

function duracionHoja(concursante) {
    return String(concursante.duracionFinal || concursante.duracionDireccion || concursante.duracion || '').trim();
}

function tituloProgramaHoja(programa) {
    if (programa.repetido) {
        const original = programa.programaOriginalCodigo || programa.codigo || programa.id;
        return `REPE PGM ${original}`;
    }
    return `PGM ${programa.codigo || programa.id}`;
}

function numeroOrdenPrograma(programa) {
    const bruto = String(programa.codigo || programa.programaOriginalCodigo || programa.id || '');
    const numero = parseInt(bruto.replace(/\D/g, ''), 10);
    return Number.isFinite(numero) ? numero : 0;
}

function compararProgramasHoja(a, b) {
    const fechaA = normalizarFechaProgramaISO(a.fechaEmision);
    const fechaB = normalizarFechaProgramaISO(b.fechaEmision);
    if (fechaA !== fechaB) return fechaA < fechaB ? -1 : 1;
    return numeroOrdenPrograma(a) - numeroOrdenPrograma(b);
}

function compararConcursantesHoja(a, b) {
    const ordenA = Number(a.ordenEscaleta) || 99;
    const ordenB = Number(b.ordenEscaleta) || 99;
    if (ordenA !== ordenB) return ordenA - ordenB;
    return (Number(a.numeroConcursante) || 0) - (Number(b.numeroConcursante) || 0);
}

function actualizarControlesSemana(numero) {
    const maximo = maxNumeroSemana();
    const dias = diasIsoDeSemana(numero);
    const input = document.getElementById('programacion-semana-numero');
    const rango = document.getElementById('programacion-semana-rango');
    const ayuda = document.getElementById('programacion-semana-ayuda');
    const anterior = document.getElementById('btn-semana-anterior');
    const siguiente = document.getElementById('btn-semana-siguiente');
    if (input) {
        input.min = '1';
        input.max = String(maximo);
        input.value = String(numero);
    }
    if (rango) {
        rango.textContent = `Lunes ${formatearFechaPrograma(dias[0])} – domingo ${formatearFechaPrograma(dias[6])}`;
    }
    if (ayuda) {
        const ancla = diasIsoDeSemana(ANCLA_SEMANA_PROGRAMACION.numero);
        ayuda.textContent = `La semana ${ANCLA_SEMANA_PROGRAMACION.numero} es la del lunes ${formatearFechaPrograma(ancla[0])}. Desde ahí el contador sigue hacia atrás y hacia adelante.`;
    }
    if (anterior) anterior.disabled = numero <= 1;
    if (siguiente) siguiente.disabled = numero >= maximo;
}

function abrirProgramacionSemana() {
    const hoy = new Date();
    let numero = numeroSemanaDeFecha(hoy);
    if (numero < 1) numero = 1;
    const maximo = maxNumeroSemana();
    if (numero > maximo) numero = maximo;
    numeroProgramacionSemana = numero;
    actualizarControlesSemana(numero);
    const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById('modal-programacion-semana'));
    modal.show();
    cargarHojaProgramacionSemana();
}

function cambiarSemanaProgramacion(delta) {
    const maximo = maxNumeroSemana();
    const siguiente = Math.min(maximo, Math.max(1, (numeroProgramacionSemana || 1) + delta));
    if (siguiente === numeroProgramacionSemana) return;
    numeroProgramacionSemana = siguiente;
    actualizarControlesSemana(siguiente);
    cargarHojaProgramacionSemana();
}

function fijarSemanaProgramacion(valor) {
    const maximo = maxNumeroSemana();
    let numero = parseInt(valor, 10);
    if (!Number.isFinite(numero)) numero = numeroProgramacionSemana || 1;
    numero = Math.min(maximo, Math.max(1, numero));
    numeroProgramacionSemana = numero;
    actualizarControlesSemana(numero);
    cargarHojaProgramacionSemana();
}

async function obtenerProgramasDeSemana(dias) {
    const desde = dias[0];
    const hasta = dias[6];
    const [programasApi, repetidosApi] = await Promise.all([
        apiManager.get('/api/programas'),
        apiManager.get('/api/programas/repetidos').catch(() => [])
    ]);
    const normales = (Array.isArray(programasApi) ? programasApi : []).map(programa => ({ ...programa, repetido: false }));
    const repetidos = (Array.isArray(repetidosApi) ? repetidosApi : []).map(programa => ({ ...programa, repetido: true }));
    const enSemana = [...normales, ...repetidos].filter(programa => {
        const fecha = normalizarFechaProgramaISO(programa.fechaEmision);
        return fecha && fecha >= desde && fecha <= hasta;
    });
    enSemana.sort(compararProgramasHoja);
    await Promise.all(enSemana.map(async programa => {
        try {
            const concursantes = normalizarListaConcursantes(await apiManager.get(urlConcursantesDePrograma(programa)));
            programa._concursantes = concursantes.slice().sort(compararConcursantesHoja);
        } catch (error) {
            programa._concursantes = [];
        }
    }));
    return enSemana;
}

function htmlFilaConcursanteHoja(concursante) {
    const foto = urlFotoHoja(concursante.foto);
    const fotoHtml = foto
        ? `<img class="foto-hoja" src="${escaparHtmlHoja(foto)}" alt="" width="160" height="90" onerror="this.remove()">`
        : '';
    return `
        <tr class="fila-concursante">
            <td class="num">${escaparHtmlHoja(concursante.numeroConcursante ?? '')}</td>
            <td class="mayus">${escaparHtmlHoja(concursante.lugar || '')}</td>
            <td class="nombre">${escaparHtmlHoja(concursante.nombre || '')}</td>
            <td class="edad">${escaparHtmlHoja(concursante.edad || '')}</td>
            <td class="ocupacion mayus">${escaparHtmlHoja(concursante.ocupacion || '')}</td>
            <td class="rrss">${escaparHtmlHoja(concursante.redesSociales || '')}</td>
            <td>${escaparHtmlHoja(concursante.xusoker || '')}</td>
            <td class="premio">${escaparHtmlHoja(eurosHoja(concursante))}</td>
            <td class="duracion">${escaparHtmlHoja(duracionHoja(concursante))}</td>
            <td class="celda-foto">${fotoHtml}</td>
            <td class="celda-momentos">${escaparHtmlHoja(concursante.momentosDestacados || '')}</td>
            <td class="factor">${escaparHtmlHoja(concursante.factorX || '')}</td>
            <td class="val">${escaparHtmlHoja(concursante.valoracionFinal || '')}</td>
        </tr>
    `;
}

function htmlProgramaHoja(programa, fechaDia) {
    const titulo = tituloProgramaHoja(programa);
    const claseTitulo = programa.repetido ? 'pgm-repe' : '';
    const fecha = fechaDia
        ? `<td class="dia-fecha" colspan="2">${escaparHtmlHoja(fechaDia)}</td>`
        : `<td colspan="2"></td>`;
    const concursantes = programa._concursantes || [];
    const filas = concursantes.length
        ? concursantes.map(htmlFilaConcursanteHoja).join('')
        : `<tr><td class="sin-conc" colspan="13">Sin concursantes</td></tr>`;
    return `
        <tr class="fila-pgm">
            <td class="${claseTitulo}" colspan="3">${escaparHtmlHoja(titulo)}</td>
            <td class="pgm-nota" colspan="3">${escaparHtmlHoja(programa.notas || '')}</td>
            <td class="pgm-creditos" colspan="4">${escaparHtmlHoja(programa.creditosEspeciales || '')}</td>
            ${fecha}
            <td class="pgm-total">${escaparHtmlHoja(totalPremiosHoja(programa))}</td>
        </tr>
        ${filas}
    `;
}

function htmlHojaSemana(numero, dias, programas) {
    const nombres = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
    const porDia = dias.map(iso => programas.filter(programa => normalizarFechaProgramaISO(programa.fechaEmision) === iso));
    const cuerpo = dias.map((iso, indice) => {
        const lista = porDia[indice];
        const etiqueta = `${nombres[indice]}, ${formatearFechaPrograma(iso)}`;
        if (!lista.length) return '';
        return lista.map((programa, i) => htmlProgramaHoja(programa, i === 0 ? etiqueta : '')).join('');
    }).join('');

    return `
        <div class="hoja-semana-banner"><span>SEMANA ${numero}</span></div>
        <table>
            <colgroup>
                <col style="width:4%">
                <col style="width:8%">
                <col style="width:8%">
                <col style="width:3%">
                <col style="width:9%">
                <col style="width:7%">
                <col style="width:6%">
                <col style="width:6%">
                <col style="width:5%">
                <col style="width:16%">
                <col style="width:12%">
                <col style="width:8%">
                <col style="width:8%">
            </colgroup>
            <tbody>${cuerpo}</tbody>
        </table>
    `;
}

async function cargarHojaProgramacionSemana() {
    const hoja = document.getElementById('hoja-programacion-semana');
    const estado = document.getElementById('programacion-semana-estado');
    const boton = document.getElementById('btn-descargar-programacion-semana');
    if (!hoja) return;
    const token = ++tokenProgramacionSemana;
    const numero = numeroProgramacionSemana;
    const dias = diasIsoDeSemana(numero);
    if (estado) estado.innerHTML = '<span class="text-muted"><i class="fas fa-spinner fa-spin"></i> Cargando la semana...</span>';
    if (boton) boton.disabled = true;
    hoja.innerHTML = '';
    try {
        const programas = await obtenerProgramasDeSemana(dias);
        if (token !== tokenProgramacionSemana) return;
        hoja.innerHTML = htmlHojaSemana(numero, dias, programas);
        if (estado) estado.innerHTML = '';
    } catch (error) {
        if (token !== tokenProgramacionSemana) return;
        hoja.innerHTML = '';
        if (estado) estado.innerHTML = '';
        mostrarError(obtenerMensajeErrorProgramas(error, 'carga de la programación semanal'));
    } finally {
        if (token === tokenProgramacionSemana && boton) boton.disabled = false;
    }
}

function esperarImagenesHoja(raiz) {
    const imagenes = [...raiz.querySelectorAll('img')];
    return Promise.all(imagenes.map(img => {
        if (img.complete) return Promise.resolve();
        return new Promise(resolver => {
            img.onload = resolver;
            img.onerror = resolver;
        });
    }));
}

function recortarFotoA16x9(img) {
    const ancho = 160;
    const alto = 90;
    img.width = ancho;
    img.height = alto;
    img.style.width = ancho + 'px';
    img.style.height = alto + 'px';
    img.style.objectFit = 'cover';
    const origenAncho = img.naturalWidth;
    const origenAlto = img.naturalHeight;
    if (!origenAncho || !origenAlto) return Promise.resolve();
    try {
        const lienzo = document.createElement('canvas');
        lienzo.width = ancho * 2;
        lienzo.height = alto * 2;
        const ctx = lienzo.getContext('2d');
        const escala = Math.max(lienzo.width / origenAncho, lienzo.height / origenAlto);
        const dibujoAncho = origenAncho * escala;
        const dibujoAlto = origenAlto * escala;
        ctx.drawImage(
            img,
            (lienzo.width - dibujoAncho) / 2,
            (lienzo.height - dibujoAlto) / 2,
            dibujoAncho,
            dibujoAlto
        );
        const datos = lienzo.toDataURL('image/jpeg', 0.9);
        return new Promise(resolver => {
            img.onload = () => resolver();
            img.onerror = () => resolver();
            img.src = datos;
        });
    } catch (error) {
        return Promise.resolve();
    }
}

async function fijarFotos16x9(raiz) {
    await esperarImagenesHoja(raiz);
    const fotos = [...raiz.querySelectorAll('img.foto-hoja')];
    await Promise.all(fotos.map(recortarFotoA16x9));
}

function cortesPaginaHoja(raiz, altoPaginaPx) {
    const alto = raiz.scrollHeight;
    const filas = [...raiz.querySelectorAll('tr')];
    if (!filas.length || alto <= altoPaginaPx) return [0, alto];
    const topRaiz = raiz.getBoundingClientRect().top;
    const cortes = [0];
    let limite = altoPaginaPx;
    filas.forEach(fila => {
        const rect = fila.getBoundingClientRect();
        const inicio = rect.top - topRaiz;
        const fin = rect.bottom - topRaiz;
        const ultimo = cortes[cortes.length - 1];
        if (fin > limite + 1 && inicio > ultimo + 1) {
            cortes.push(inicio);
            limite = inicio + altoPaginaPx;
        }
    });
    if (cortes[cortes.length - 1] < alto) cortes.push(alto);
    return cortes;
}

async function descargarPdfProgramacionSemana() {
    const origen = document.getElementById('hoja-programacion-semana');
    if (!origen || !origen.querySelector('table')) {
        mostrarError('Todavía no hay una programación para generar el PDF');
        return;
    }
    if (typeof html2canvas !== 'function' || !window.jspdf || !window.jspdf.jsPDF) {
        mostrarError('No se pudo generar el PDF');
        return;
    }
    const boton = document.getElementById('btn-descargar-programacion-semana');
    if (boton) boton.disabled = true;
    const contenedor = document.createElement('div');
    contenedor.style.position = 'fixed';
    contenedor.style.left = '-10000px';
    contenedor.style.top = '0';
    contenedor.style.background = '#ffffff';
    const clon = origen.cloneNode(true);
    contenedor.appendChild(clon);
    document.body.appendChild(contenedor);
    try {
        await fijarFotos16x9(clon);
        const { jsPDF } = window.jspdf;
        const pdf = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
        const margen = 6;
        const anchoUtil = pdf.internal.pageSize.getWidth() - margen * 2;
        const altoUtil = pdf.internal.pageSize.getHeight() - margen * 2;
        const altoPaginaPx = Math.floor(clon.offsetWidth * altoUtil / anchoUtil);
        const cortes = cortesPaginaHoja(clon, altoPaginaPx);
        const canvas = await html2canvas(clon, {
            backgroundColor: '#ffffff',
            scale: 2,
            useCORS: true,
            logging: false
        });
        const escala = canvas.width / clon.offsetWidth;
        cortes.slice(0, -1).forEach((inicio, indice) => {
            const y0 = Math.round(inicio * escala);
            const y1 = Math.round(cortes[indice + 1] * escala);
            const alto = Math.max(1, y1 - y0);
            const recorte = document.createElement('canvas');
            recorte.width = canvas.width;
            recorte.height = alto;
            const ctx = recorte.getContext('2d');
            ctx.fillStyle = '#ffffff';
            ctx.fillRect(0, 0, recorte.width, recorte.height);
            ctx.drawImage(canvas, 0, y0, canvas.width, alto, 0, 0, canvas.width, alto);
            const altoMm = (alto / escala) * anchoUtil / clon.offsetWidth;
            if (indice > 0) pdf.addPage();
            pdf.addImage(recorte.toDataURL('image/jpeg', 0.92), 'JPEG', margen, margen, anchoUtil, altoMm);
        });
        pdf.save(`ESCALETAS LSNLS SEMANA ${numeroProgramacionSemana}.pdf`);
        mostrarExito('PDF de la semana descargado');
    } catch (error) {
        mostrarError('No se pudo generar el PDF');
    } finally {
        contenedor.remove();
        if (boton) boton.disabled = false;
    }
}

let repetidoModo = 'programa';
let repetidoProgramaSeleccionado = null;
let repetidoConcursantesSeleccionados = [];
let repetidoResultadosConcursantes = [];
let repetidoConcursantesMeta = { total: 0, page: 0, size: 25 };
let guardandoRepetido = false;

function mostrarFormularioRepetido() {
    if (!puedeCrearPrograma()) return;
    const form = document.getElementById('form-repetido');
    if (form) form.reset();
    const tipo = document.getElementById('repe-tipo');
    if (tipo) tipo.value = 'REPE';
    const modoPrograma = document.getElementById('repe-modo-programa');
    if (modoPrograma) modoPrograma.checked = true;
    repetidoProgramaSeleccionado = null;
    repetidoConcursantesSeleccionados = [];
    repetidoResultadosConcursantes = [];
    repetidoConcursantesMeta = { total: 0, page: 0, size: 25 };
    const resultadosProgramas = document.getElementById('repe-resultados-programas');
    if (resultadosProgramas) {
        resultadosProgramas.innerHTML = '<span class="text-muted">Usa los filtros y pulsa Buscar.</span>';
    }
    const resultadosConcursantes = document.getElementById('repe-resultados-concursantes');
    if (resultadosConcursantes) {
        resultadosConcursantes.innerHTML = '<span class="text-muted">Usa los filtros y pulsa Buscar.</span>';
    }
    const seleccion = document.getElementById('repe-seleccion-concursantes');
    if (seleccion) seleccion.innerHTML = '';
    const ayuda = document.getElementById('repe-codigo-ayuda');
    if (ayuda) {
        ayuda.textContent = 'Si lo dejas vacío, se asigna el último número de este tipo en la temporada, más uno.';
    }
    cambiarModoRepetido('programa');
    const temporadas = (programas || [])
        .map(p => Number(p.temporada))
        .filter(n => Number.isFinite(n) && n > 0);
    const inputTemporada = document.getElementById('repe-temporada');
    if (inputTemporada && temporadas.length) {
        inputTemporada.value = String(Math.max(...temporadas));
    }
    const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById('modal-repetido'));
    modal.show();
    sugerirCodigoRepetido();
}

function cambiarModoRepetido(modo) {
    repetidoModo = modo === 'concursantes' ? 'concursantes' : 'programa';
    document.getElementById('repe-panel-programa')?.classList.toggle('d-none', repetidoModo !== 'programa');
    document.getElementById('repe-panel-concursantes')?.classList.toggle('d-none', repetidoModo !== 'concursantes');
}

async function sugerirCodigoRepetido() {
    const temporada = parseInt(document.getElementById('repe-temporada')?.value, 10);
    const tipo = document.getElementById('repe-tipo')?.value || 'REPE';
    const input = document.getElementById('repe-codigo');
    const ayuda = document.getElementById('repe-codigo-ayuda');
    if (!input || !Number.isFinite(temporada) || temporada < 1) return;
    try {
        const sugerido = await apiManager.get(
            `/api/programas/repetidos/siguiente-codigo?tipo=${encodeURIComponent(tipo)}&temporada=${temporada}`
        );
        const codigo = sugerido && sugerido.codigo;
        if (!codigo || input.value) return;
        input.placeholder = 'Sugerido: ' + codigo;
        if (ayuda) {
            ayuda.textContent = 'Sugerido: ' + codigo + '. Si lo dejas vacío, se asigna al guardar (último número de este tipo en la temporada, más uno).';
        }
    } catch (error) {
        // La asignación real sigue haciéndose en el servidor al crear.
    }
}

function paramsBusquedaRepetido(campos) {
    const params = new URLSearchParams();
    campos.forEach(([clave, id]) => {
        const valor = (document.getElementById(id)?.value || '').trim();
        if (valor) params.set(clave, valor);
    });
    const texto = params.toString();
    return texto ? `?${texto}` : '';
}

const TAMANO_PAGINA_PROGRAMAS_REPE = 25;
const TAMANO_PAGINA_CONCURSANTES_REPE = 25;
let busquedaProgramasRepeSeq = 0;
let busquedaConcursantesRepeSeq = 0;

function queryProgramasParaRepetido(pagina) {
    const base = paramsBusquedaRepetido([
        ['fechaEmision', 'repe-filtro-fecha'],
        ['codigo', 'repe-filtro-codigo'],
        ['valoracionMinima', 'repe-filtro-valoracion-programa']
    ]);
    const params = new URLSearchParams(base.startsWith('?') ? base.slice(1) : '');
    params.set('page', String(Math.max(0, pagina)));
    return `?${params.toString()}`;
}

function htmlPaginacionProgramasRepetido(total, page, size) {
    const n = Number(total) || 0;
    const tamano = Number(size) > 0 ? Number(size) : TAMANO_PAGINA_PROGRAMAS_REPE;
    const paginas = Math.max(1, Math.ceil(n / tamano) || 1);
    const pagina = Math.min(Math.max(0, Number(page) || 0), paginas - 1);
    const etiqueta = n === 1 ? '1 resultado' : `${n} resultados`;
    if (!n) {
        return `<div class="text-muted small mb-2">${etiqueta}</div>`;
    }
    const inicio = pagina * tamano + 1;
    const fin = Math.min((pagina + 1) * tamano, n);
    const rango = `${etiqueta}. Mostrando ${inicio}–${fin} de ${n}`;
    if (paginas <= 1) {
        return `<div class="text-muted small mb-2">${rango}</div>`;
    }
    const anterior = pagina <= 0 ? 'disabled' : '';
    const siguiente = pagina >= paginas - 1 ? 'disabled' : '';
    return `
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-2 sticky-top bg-white">
            <div class="text-muted small">${rango}</div>
            <div class="btn-group btn-group-sm" role="group" aria-label="Páginas de programas">
                <button type="button" class="btn btn-outline-secondary" ${anterior}
                    onclick="buscarProgramasParaRepetido(${pagina - 1})">Anterior</button>
                <span class="btn btn-outline-secondary disabled">Página ${pagina + 1} de ${paginas}</span>
                <button type="button" class="btn btn-outline-secondary" ${siguiente}
                    onclick="buscarProgramasParaRepetido(${pagina + 1})">Siguiente</button>
            </div>
        </div>`;
}

async function buscarProgramasParaRepetido(pagina) {
    const contenedor = document.getElementById('repe-resultados-programas');
    const boton = document.getElementById('btn-buscar-programas-repe');
    if (!contenedor) return;
    const cambioDePagina = Number.isFinite(Number(pagina));
    const paginaPedida = cambioDePagina ? Math.max(0, Math.floor(Number(pagina))) : 0;
    if (!cambioDePagina) {
        repetidoProgramaSeleccionado = null;
    }
    const seq = ++busquedaProgramasRepeSeq;
    if (boton) boton.disabled = true;
    contenedor.innerHTML = '<span class="text-muted">Buscando programas emitidos...</span>';
    try {
        const query = queryProgramasParaRepetido(paginaPedida);
        const respuesta = await apiManager.get(`/api/programas/repetidos/origen/programas${query}`);
        if (seq !== busquedaProgramasRepeSeq) return;
        const lista = Array.isArray(respuesta?.resultados) ? respuesta.resultados : [];
        const total = Number.isFinite(Number(respuesta?.total)) ? Number(respuesta.total) : lista.length;
        const page = Number.isFinite(Number(respuesta?.page)) ? Number(respuesta.page) : paginaPedida;
        const size = Number(respuesta?.size) > 0 ? Number(respuesta.size) : TAMANO_PAGINA_PROGRAMAS_REPE;
        if (!lista.length) {
            contenedor.innerHTML = htmlPaginacionProgramasRepetido(total, page, size)
                + '<span class="text-muted">No hay programas emitidos que coincidan con los filtros.</span>';
            return;
        }
        contenedor.innerHTML = htmlPaginacionProgramasRepetido(total, page, size) + lista.map(programa => {
            const fecha = formatearFechaPrograma(programa.fechaEmision);
            const codigo = programa.codigo || programa.id;
            const resumen = programa.resumenConcursantes
                ? `<span class="text-muted small d-block">${escaparHtmlHoja(programa.resumenConcursantes)}</span>`
                : '<span class="text-muted small d-block">Sin concursantes</span>';
            const marcado = Number(repetidoProgramaSeleccionado) === Number(programa.id) ? 'checked' : '';
            return `
                <div class="form-check">
                    <input class="form-check-input" type="radio" name="repe-programa-origen" id="repe-pgm-${programa.id}"
                           value="${programa.id}" ${marcado} onchange="seleccionarProgramaParaRepetido(${programa.id})">
                    <label class="form-check-label" for="repe-pgm-${programa.id}">
                        PGM ${escaparHtmlHoja(codigo)} · ${escaparHtmlHoja(fecha)} · T${escaparHtmlHoja(programa.temporada ?? '')}
                        · ${escaparHtmlHoja(programa.totalConcursantes ?? 0)} concursantes
                        ${resumen}
                    </label>
                </div>`;
        }).join('');
        contenedor.scrollTop = 0;
    } catch (error) {
        if (seq !== busquedaProgramasRepeSeq) return;
        contenedor.innerHTML = '';
        mostrarError(obtenerMensajeErrorProgramas(error, 'búsqueda de programas emitidos'));
    } finally {
        if (seq === busquedaProgramasRepeSeq && boton) boton.disabled = false;
    }
}

function seleccionarProgramaParaRepetido(programaId) {
    repetidoProgramaSeleccionado = programaId;
}

function queryConcursantesParaRepetido(pagina) {
    // estado: emitido = exactamente 1 programa; repetido = más de 1. No es el texto del campo estado.
    const base = paramsBusquedaRepetido([
        ['valoracionMinima', 'repe-filtro-valoracion-conc'],
        ['tiempo', 'repe-filtro-tiempo'],
        ['numero', 'repe-filtro-numero'],
        ['ciudad', 'repe-filtro-ciudad'],
        ['estado', 'repe-filtro-estado-conc']
    ]);
    const params = new URLSearchParams(base.startsWith('?') ? base.slice(1) : '');
    params.set('page', String(Math.max(0, pagina)));
    params.set('size', String(TAMANO_PAGINA_CONCURSANTES_REPE));
    return `?${params.toString()}`;
}

function htmlPaginacionConcursantesRepetido(total, page, size) {
    const n = Number(total) || 0;
    const tamano = Number(size) > 0 ? Number(size) : TAMANO_PAGINA_CONCURSANTES_REPE;
    const paginas = Math.max(1, Math.ceil(n / tamano) || 1);
    const pagina = Math.min(Math.max(0, Number(page) || 0), paginas - 1);
    const etiqueta = n === 1 ? '1 resultado' : `${n} resultados`;
    if (!n) {
        return `<div class="text-muted small mb-2">${etiqueta}</div>`;
    }
    const inicio = pagina * tamano + 1;
    const fin = Math.min((pagina + 1) * tamano, n);
    const rango = `${etiqueta}. Mostrando ${inicio}–${fin} de ${n}. Página ${pagina + 1} de ${paginas}`;
    if (paginas <= 1) {
        return `<div class="text-muted small mb-2">${rango}</div>`;
    }
    const anterior = pagina <= 0 ? 'disabled' : '';
    const siguiente = pagina >= paginas - 1 ? 'disabled' : '';
    return `
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-2 sticky-top bg-white">
            <div class="text-muted small">${rango}</div>
            <div class="btn-group btn-group-sm" role="group" aria-label="Páginas de concursantes">
                <button type="button" class="btn btn-outline-secondary" ${anterior}
                    onclick="buscarConcursantesParaRepetido(${pagina - 1})">Anterior</button>
                <button type="button" class="btn btn-outline-secondary" ${siguiente}
                    onclick="buscarConcursantesParaRepetido(${pagina + 1})">Siguiente</button>
            </div>
        </div>`;
}

async function buscarConcursantesParaRepetido(pagina) {
    const contenedor = document.getElementById('repe-resultados-concursantes');
    const boton = document.getElementById('btn-buscar-concursantes-repe');
    if (!contenedor) return;
    const paginaPedida = Number.isFinite(Number(pagina)) ? Math.max(0, Math.floor(Number(pagina))) : 0;
    const seq = ++busquedaConcursantesRepeSeq;
    if (boton) boton.disabled = true;
    contenedor.innerHTML = '<span class="text-muted">Buscando concursantes...</span>';
    try {
        const tiempoRepe = (document.getElementById('repe-filtro-tiempo')?.value || '').trim();
        if (tiempoRepe && duracionConcursanteEnSegundos(tiempoRepe) === null) {
            if (seq !== busquedaConcursantesRepeSeq) return;
            repetidoResultadosConcursantes = [];
            repetidoConcursantesMeta = { total: 0, page: 0, size: TAMANO_PAGINA_CONCURSANTES_REPE };
            contenedor.innerHTML = '<span class="text-muted">El tiempo debe tener formato MM:SS.</span>';
            return;
        }
        const query = queryConcursantesParaRepetido(paginaPedida);
        const respuesta = await apiManager.get(`/api/programas/repetidos/origen/concursantes${query}`);
        if (seq !== busquedaConcursantesRepeSeq) return;
        repetidoResultadosConcursantes = Array.isArray(respuesta?.resultados) ? respuesta.resultados : [];
        const total = Number.isFinite(Number(respuesta?.total)) ? Number(respuesta.total) : repetidoResultadosConcursantes.length;
        const page = Number.isFinite(Number(respuesta?.page)) ? Number(respuesta.page) : paginaPedida;
        const size = Number(respuesta?.size) > 0 ? Number(respuesta.size) : TAMANO_PAGINA_CONCURSANTES_REPE;
        repetidoConcursantesMeta = { total, page, size };
        renderResultadosConcursantesRepetido();
        pintarSeleccionConcursantesRepetido();
        contenedor.scrollTop = 0;
    } catch (error) {
        if (seq !== busquedaConcursantesRepeSeq) return;
        contenedor.innerHTML = '';
        mostrarError(obtenerMensajeErrorProgramas(error, 'búsqueda de concursantes'));
    } finally {
        if (seq === busquedaConcursantesRepeSeq && boton) boton.disabled = false;
    }
}

function htmlOpcionConcursanteRepetido(concursante) {
    const marcado = repetidoConcursantesSeleccionados.some(item => item.id === concursante.id);
    const posicion = repetidoConcursantesSeleccionados.findIndex(item => item.id === concursante.id);
    const orden = posicion >= 0 ? `<span class="badge bg-secondary ms-1">${posicion + 1}</span>` : '';
    const fecha = concursante.fechaEmisionPrograma ? formatearFechaPrograma(concursante.fechaEmisionPrograma) : '';
    const programa = `PGM ${escaparHtmlHoja(concursante.programaCodigo || '')}${fecha ? ' · ' + escaparHtmlHoja(fecha) : ''}`;
    return `
        <tr>
            <td>
                <input class="form-check-input" type="checkbox" id="repe-conc-${concursante.id}"
                       ${marcado ? 'checked' : ''} onchange="alternarConcursanteRepetido(${concursante.id}, this.checked)">
                ${orden}
            </td>
            <td>${htmlFotoSelectorConcursante(concursante)}</td>
            <td>${escaparHtmlHoja(concursante.numeroConcursante ?? '')}</td>
            <td><strong>${escaparHtmlHoja(concursante.nombre || 'Sin nombre')}</strong></td>
            <td>${escaparHtmlHoja(concursante.lugar || '')}</td>
            <td>${escaparHtmlHoja(concursante.valoracionFinal || '')}</td>
            <td>${escaparHtmlHoja(duracionHoja(concursante))}</td>
            <td class="celda-momentos">${escaparHtmlHoja(concursante.momentosDestacados || '')}</td>
            <td>${programa}</td>
        </tr>`;
}

function alternarConcursanteRepetido(concursanteId, marcado) {
    const id = Number(concursanteId);
    if (marcado) {
        if (repetidoConcursantesSeleccionados.some(item => item.id === id)) {
            pintarSeleccionConcursantesRepetido();
            return;
        }
        if (repetidoConcursantesSeleccionados.length >= 3) {
            const input = document.getElementById(`repe-conc-${id}`);
            if (input) input.checked = false;
            mostrarError('Un programa repetido admite como máximo 3 concursantes.');
            return;
        }
        const encontrado = repetidoResultadosConcursantes.find(item => item.id === id);
        repetidoConcursantesSeleccionados.push(encontrado || { id });
    } else {
        repetidoConcursantesSeleccionados = repetidoConcursantesSeleccionados.filter(item => item.id !== id);
    }
    pintarSeleccionConcursantesRepetido();
}

function quitarConcursanteRepetido(concursanteId) {
    alternarConcursanteRepetido(concursanteId, false);
    const input = document.getElementById(`repe-conc-${concursanteId}`);
    if (input) input.checked = false;
}

function renderResultadosConcursantesRepetido() {
    const contenedor = document.getElementById('repe-resultados-concursantes');
    if (!contenedor) return;
    const cabecera = htmlPaginacionConcursantesRepetido(
        repetidoConcursantesMeta.total,
        repetidoConcursantesMeta.page,
        repetidoConcursantesMeta.size
    );
    if (!repetidoResultadosConcursantes.length) {
        contenedor.innerHTML = cabecera
            + '<span class="text-muted">No hay concursantes de programas emitidos que coincidan con los filtros.</span>';
        return;
    }
    contenedor.innerHTML = cabecera + `
        <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
                <thead>
                    <tr>
                        <th></th>
                        <th>Foto</th>
                        <th>Nº</th>
                        <th>Nombre</th>
                        <th>Lugar</th>
                        <th>Valoración</th>
                        <th>Duración</th>
                        <th>Mom. destacados</th>
                        <th>Programa</th>
                    </tr>
                </thead>
                <tbody>
                    ${repetidoResultadosConcursantes.map(htmlOpcionConcursanteRepetido).join('')}
                </tbody>
            </table>
        </div>`;
}

function pintarSeleccionConcursantesRepetido() {
    const seleccion = document.getElementById('repe-seleccion-concursantes');
    if (seleccion) {
        if (!repetidoConcursantesSeleccionados.length) {
            seleccion.innerHTML = '<span class="text-muted">Ningún concursante seleccionado (máximo 3).</span>';
        } else {
            seleccion.innerHTML = repetidoConcursantesSeleccionados.map((item, indice) => `
                <span class="badge bg-primary me-1">
                    ${indice + 1}. ${escaparHtmlHoja(item.numeroConcursante ?? '')} ${escaparHtmlHoja(item.nombre || '')}
                    <button type="button" class="btn-close btn-close-white ms-1" style="font-size: 0.6rem;"
                            aria-label="Quitar" onclick="quitarConcursanteRepetido(${item.id})"></button>
                </span>
            `).join('') + '<span class="text-muted ms-1">Máximo 3. El orden de esta lista es la escaleta.</span>';
        }
    }
    if (repetidoResultadosConcursantes.length) {
        renderResultadosConcursantesRepetido();
    }
}

async function guardarRepetido() {
    if (guardandoRepetido) return;
    if (!puedeCrearPrograma()) {
        mostrarError('No tienes permisos para crear un programa repetido.');
        return;
    }
    const temporada = parseInt(document.getElementById('repe-temporada')?.value, 10);
    if (!Number.isFinite(temporada) || temporada < 1) {
        mostrarError('La temporada es obligatoria');
        return;
    }
    const modo = document.getElementById('repe-modo-concursantes')?.checked ? 'concursantes' : 'programa';
    const cuerpo = {
        tipo: document.getElementById('repe-tipo')?.value || 'REPE',
        codigo: (document.getElementById('repe-codigo')?.value || '').trim() || null,
        temporada,
        fechaEmision: document.getElementById('repe-fecha')?.value || null
    };
    const estadoAlta = (document.getElementById('repe-estado')?.value || '').trim();
    if (estadoAlta) {
        cuerpo.estado = estadoAlta;
    }
    if (modo === 'programa') {
        if (!repetidoProgramaSeleccionado) {
            mostrarError('Selecciona un programa emitido.');
            return;
        }
        cuerpo.programaOriginalId = repetidoProgramaSeleccionado;
    } else {
        if (repetidoConcursantesSeleccionados.length === 0) {
            mostrarError('Selecciona al menos un concursante.');
            return;
        }
        if (repetidoConcursantesSeleccionados.length > 3) {
            mostrarError('Un programa repetido admite como máximo 3 concursantes.');
            return;
        }
        cuerpo.concursanteIds = repetidoConcursantesSeleccionados.map(item => item.id);
    }

    const boton = document.getElementById('btn-guardar-repetido');
    guardandoRepetido = true;
    if (boton) boton.disabled = true;
    try {
        const creado = await apiManager.postUndoable('/api/programas/repetidos', cuerpo, {
            label: 'Crear programa repetido',
            idExtractor: (respuesta) => respuesta?.id,
            deleteEndpointBuilder: (id) => `/api/programas/repetidos/${id}`
        });
        const codigo = creado && (creado.codigo || creado.id);
        mostrarExito(codigo ? `Repetido ${creado.tipo || 'REPE'} ${codigo} creado` : 'Programa repetido creado');
        if (creado && creado.aviso) {
            mostrarMensaje(creado.aviso, 'info');
        }
        bootstrap.Modal.getInstance(document.getElementById('modal-repetido'))?.hide();
        const filtroListado = document.getElementById('filtro-listado-programa');
        if (filtroListado) filtroListado.value = 'repetidos';
        aplicandoFiltros = true;
        paginaActual = 0;
        await refrescarVistaProgramas();
    } catch (error) {
        mostrarError(obtenerMensajeErrorProgramas(error, 'creación del programa repetido'));
    } finally {
        guardandoRepetido = false;
        if (boton) boton.disabled = false;
    }
}