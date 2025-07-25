// Módulo de gestión de cuestionarios
const CuestionariosManager = {
    async cargarCuestionarios() {
        try {
            if (!authManager.isAuthenticated()) {
                console.error('Usuario no autenticado');
                return;
            }
            const response = await fetch('/api/cuestionarios', {
                headers: authManager.getAuthHeaders()
            });
            if (!response.ok) throw new Error('Error al cargar los cuestionarios');
            const cuestionarios = await response.json();
            await this.mostrarCuestionarios(cuestionarios);
        } catch (error) {
            if (error && error.message && error.message.startsWith('401')) {
                // No mostrar mensaje, la redirección ya ocurre en api.js
                return;
            }
            console.error('Error al cargar cuestionarios:', error);
            Toastify({
                text: `Error: ${error.message}`,
                duration: 3000,
                close: true,
                gravity: "top",
                position: "right",
                style: { background: "linear-gradient(to right, #ff0000, #cc0000)" }
            }).showToast();
        }
    },
    async mostrarCuestionarios(cuestionarios) {
        const tbody = document.getElementById('tabla-cuestionarios');
        if (!tbody) {
            console.error('No se encontró el elemento tabla-cuestionarios');
            return;
        }
        
        // Cargar temáticas gestionadas del backend
        let tematicasGestionadas = [];
        try {
            const response = await fetch('/api/cuestionarios/tematicas', {
                headers: authManager.getAuthHeaders()
            });
            if (response.ok) {
                tematicasGestionadas = await response.json();
            }
        } catch (error) {
            console.error('Error al cargar temáticas:', error);
        }
        
        tbody.innerHTML = '';
        if (!Array.isArray(cuestionarios) || cuestionarios.length === 0) {
            const tr = document.createElement('tr');
            tr.innerHTML = '<td colspan="5" class="text-center">No hay cuestionarios</td>';
            tbody.appendChild(tr);
            return;
        }
        
        cuestionarios.forEach(c => {
            // Determinar si hay huecos usando slot - solo niveles 1-4
            const niveles = ['1LS','2NLS','3LS','4NLS'];
            const preguntasPorSlot = {};
            if (Array.isArray(c.preguntas)) {
                c.preguntas.forEach(pc => {
                    if (pc && pc.slot) preguntasPorSlot[pc.slot] = pc.pregunta;
                });
            }
            const tieneHuecos = niveles.some(nivel => !preguntasPorSlot[nivel]);
            const estadoMostrar = c.estado ?? '';
            const tr = document.createElement('tr');
            tr.setAttribute('data-id', c.id);
            
            // Crear opciones del dropdown de temáticas dinámicamente
            let opcionesTematicas = '<option value="" ' + (!c.tematica || c.tematica === '' ? 'selected' : '') + '>Sin temática</option>';
            
            // Añadir temáticas gestionadas
            tematicasGestionadas.forEach(tematica => {
                opcionesTematicas += `<option value="${tematica}" ${c.tematica === tematica ? 'selected' : ''}>${tematica}</option>`;
            });
            
            // Si la temática actual no está en la lista gestionada, añadirla como opción
            if (c.tematica && !tematicasGestionadas.includes(c.tematica)) {
                opcionesTematicas += `<option value="${c.tematica}" selected>${c.tematica} (no gestionada)</option>`;
            }
            
            tr.innerHTML = `
                <td style="font-weight: bold; font-size: 1.2em; color: #0066cc;">${c.id ?? ''}</td>
                <td>
                    <select class="form-select form-select-sm" onchange="cambiarTematicaCuestionario(${c.id}, this.value)">
                        ${opcionesTematicas}
                    </select>
                </td>
                <td>
                    <select class="form-select form-select-sm" onchange="cambiarEstadoCuestionario(${c.id}, this.value)">
                        <option value="borrador" ${c.estado === 'borrador' ? 'selected' : ''}>Borrador</option>
                        <option value="creado" ${c.estado === 'creado' ? 'selected' : ''}>Creado</option>
                        <option value="adjudicado" ${c.estado === 'adjudicado' ? 'selected' : ''}>Adjudicado</option>
                        <option value="grabado" ${c.estado === 'grabado' ? 'selected' : ''}>Grabado</option>
                    </select>
                </td>
                <td>${(c.preguntas && c.preguntas.length) || 0}</td>
                <td>${c.fechaCreacion ? Utils.formatearFecha(String(c.fechaCreacion)) : ''}</td>
                <td>
                    <button class="btn btn-sm btn-danger" onclick="eliminarCuestionario(${c.id})">
                        <i class="fas fa-trash"></i> Eliminar
                    </button>
                </td>
            `;
            tbody.appendChild(tr);

            // Subtabla de preguntas con la estética de la tabla de preguntas
            const subtr = document.createElement('tr');
            subtr.classList.add('cuestionario-subtabla');
            const puedeEditarNotas = authManager.hasRole('ROLE_ADMIN') || authManager.hasRole('ROLE_DIRECCION');
            
            // Generar exactamente 4 filas para las preguntas del cuestionario
            let filasPreguntas = '';
            for (let i = 1; i <= 4; i++) {
                const slotNivel = niveles[i-1]; // '1LS', '2NLS', '3LS', '4NLS'
                const p = preguntasPorSlot[slotNivel];
                
                if (p) {
                    // Fila con pregunta
                    filasPreguntas += `<tr data-id="${p.id}" data-nivel="${slotNivel}" style="cursor:pointer;">
                        <td><span class='${CuestionariosManager.getNivelColor ? CuestionariosManager.getNivelColor(p.nivel) : ''}'>${slotNivel}</span></td>
                        <td>${p.pregunta ?? ''}</td>
                        <td>${p.respuesta ?? ''}</td>
                        <td><button class='btn btn-sm btn-danger' onclick='event.stopPropagation();eliminarPreguntaDeCuestionario(${c.id}, "${slotNivel}")'><i class='fas fa-trash'></i></button></td>
                    </tr>`;
                } else {
                    // Fila vacía con botón añadir
                    filasPreguntas += `<tr data-nivel="${slotNivel}">
                        <td><span class='${CuestionariosManager.getNivelColor ? CuestionariosManager.getNivelColor(slotNivel) : ''}'>${slotNivel}</span></td>
                        <td class="text-center text-muted">(Vacío)</td>
                        <td class="text-center text-muted">-</td>
                        <td><button class='btn btn-sm btn-success' onclick='event.stopPropagation();anadirPreguntaACuestionario(${c.id}, "${slotNivel}")'><i class='fas fa-plus'></i></button></td>
                    </tr>`;
                }
            }
            
            subtr.innerHTML = `<td colspan="6">
                ${puedeEditarNotas ? `
                <div class="mb-3">
                    <label class="form-label fw-bold">Notas de Dirección:</label>
                    <textarea class="form-control" rows="2" placeholder="Añadir notas para dirección..." 
                              onblur="actualizarNotasDireccion(${c.id}, this.value)">${c.notasDireccion || ''}</textarea>
                </div>` : ''}
                <div>
                    <table class="table table-preguntas-cuestionario mb-0">
                        <thead>
                            <tr>
                                <th>Nivel</th>
                                <th>Pregunta</th>
                                <th>Respuesta</th>
                                <th>Acción</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${filasPreguntas}
                        </tbody>
                    </table>
                </div>
            </td>`;
            tbody.appendChild(subtr);
            // Añadir evento de click a filas con pregunta para redirigir
            setTimeout(() => {
                const filas = subtr.querySelectorAll('tbody tr[data-id]');
                filas.forEach(fila => {
                    fila.addEventListener('click', function() {
                        const id = this.getAttribute('data-id');
                        if (id) window.location.href = `preguntas.html?id=${id}`;
                    });
                });
            }, 0);
        });
        // Delegación para enlaces de preguntas
        tbody.querySelectorAll('.enlace-pregunta').forEach(a => {
            a.addEventListener('click', function(e) {
                e.preventDefault();
                const id = this.dataset.id;
                window.location.href = `preguntas.html?id=${id}`;
            });
        });
        // Resaltar y hacer scroll si hay id en la URL
        const params = new URLSearchParams(window.location.search);
        const idDestacado = params.get('id');
        if (idDestacado) {
            setTimeout(() => {
                const fila = tbody.querySelector(`tr[data-id='${idDestacado}']`);
                if (fila) {
                    fila.classList.add('table-warning');
                    fila.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }
            }, 500);
        }
    },
    getNivelColor(nivel) {
        if (["_2NLS", "_4NLS", "_5NLS", "2NLS", "4NLS"].includes(nivel)) return 'text-danger fw-bold';
        if (["_1LS", "_3LS", "_5LS", "1LS", "3LS"].includes(nivel)) return 'text-success fw-bold';
        return '';
    },
};

function inicializarCuestionarios() {
    CuestionariosManager.cargarCuestionarios();
}

document.addEventListener('DOMContentLoaded', inicializarCuestionarios);

// IDs de los campos para las preguntas normales (solo niveles 1-4)
const normales = ['1LS','2NLS','3LS','4NLS'];

let selectorPreguntaContext = { nivel: null, factor: null, inputId: null, textoId: null };

async function mostrarFormularioCuestionario() {
    // Limpiar selects normales y campos de texto
    normales.forEach(nivel => {
        const sel = document.getElementById(`pregunta-${nivel}`);
        const texto = document.getElementById(`pregunta-${nivel}-texto`);
        if (sel) sel.value = '';
        if (texto) texto.value = '';
    });
    
    // Limpiar campos nuevos
    document.getElementById('cuestionario-tematica').value = '';
    document.getElementById('cuestionario-notas').value = '';
    document.getElementById('cuestionario-id').value = '';
    
    // Cargar temáticas gestionadas para el dropdown
    try {
        const response = await fetch('/api/cuestionarios/tematicas', {
            headers: authManager.getAuthHeaders()
        });
        if (response.ok) {
            const tematicas = await response.json();
            const tematicaSelect = document.getElementById('cuestionario-tematica');
            if (tematicaSelect) {
                tematicaSelect.innerHTML = '<option value="">Sin temática</option>';
                tematicas.forEach(tematica => {
                    const option = document.createElement('option');
                    option.value = tematica;
                    option.textContent = tematica;
                    tematicaSelect.appendChild(option);
                });
            }
        }
    } catch (error) {
        console.error('Error al cargar temáticas para el formulario:', error);
    }
    
    // Mostrar modal
    const modal = new bootstrap.Modal(document.getElementById('modal-cuestionario'));
    modal.show();
}

// Añadir eventos reactivos a los inputs del modal de búsqueda de preguntas
function inicializarBuscadorPreguntasModal() {
    ['buscador-id', 'buscador-pregunta', 'buscador-respuesta', 'buscador-tematica'].forEach(id => {
        const input = document.getElementById(id);
        if (input) {
            input.removeEventListener('keyup', input._buscadorHandler || (()=>{}));
            input._buscadorHandler = () => buscarPreguntasModal(0);
            input.addEventListener('keyup', input._buscadorHandler);
        }
    });
}

function abrirSelectorPregunta(nivel, factor = null) {
    selectorPreguntaContext.nivel = nivel;
    selectorPreguntaContext.factor = factor;
    selectorPreguntaContext.inputId = `pregunta-${nivel}`;
    selectorPreguntaContext.textoId = `pregunta-${nivel}-texto`;
    document.getElementById('buscador-id').value = '';
    document.getElementById('buscador-pregunta').value = '';
    document.getElementById('buscador-respuesta').value = '';
    document.getElementById('buscador-tematica').value = '';
    inicializarBuscadorPreguntasModal();
    buscarPreguntasModal(0);
    const modal = new bootstrap.Modal(document.getElementById('modal-selector-pregunta'));
    modal.show();
}

// Mejorar paginación para PM
async function buscarPreguntasModal(page = 0) {
    const nivel = selectorPreguntaContext.nivel;
    let url = '';
    if (normales.includes(nivel)) {
        url = `/api/preguntas/buscar?nivel=_${nivel}`;
    } else {
        url = `/api/preguntas/buscar?nivel=_5LS`;
    }
    const id = document.getElementById('buscador-id').value.trim();
    const pregunta = document.getElementById('buscador-pregunta').value.trim();
    const respuesta = document.getElementById('buscador-respuesta').value.trim();
    const tematica = document.getElementById('buscador-tematica').value.trim();
    if (id) url += `&id=${encodeURIComponent(id)}`;
    if (pregunta) url += `&pregunta=${encodeURIComponent(pregunta)}`;
    if (respuesta) url += `&respuesta=${encodeURIComponent(respuesta)}`;
    if (tematica) url += `&tematica=${encodeURIComponent(tematica)}`;
    url += `&page=${page}&size=20`;
    
    try {
        let preguntas = [];
        let totalPages = 1;
        
        // Ejecutar la búsqueda siempre, no solo para niveles normales
        const resp = await fetch(url, { headers: authManager.getAuthHeaders() });
        if (!resp.ok) throw new Error('Error al buscar preguntas');
        const data = await resp.json();
        preguntas = data.content || [];
        totalPages = data.totalPages || 1;
        
        renderPreguntasModal(preguntas, page, totalPages);
    } catch (e) {
        console.error('Error en buscarPreguntasModal:', e);
        document.getElementById('tbody-selector-pregunta').innerHTML = `<tr><td colspan="6">Error al cargar preguntas: ${e.message}</td></tr>`;
        document.getElementById('paginacion-selector-pregunta').innerHTML = '';
    }
}

function renderPreguntasModal(preguntas, page, totalPages) {
    const tbody = document.getElementById('tbody-selector-pregunta');
    if (!preguntas || preguntas.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6">No hay preguntas disponibles</td></tr>';
        document.getElementById('paginacion-selector-pregunta').innerHTML = '';
        return;
    }
    console.log('[FRONT] Renderizando preguntas en modal:', preguntas);
    tbody.innerHTML = preguntas.map(p => {
        return `
        <tr>
            <td style="width:80px;vertical-align:top;"><b>ID:</b> ${p.id}<br>
                <button class="btn btn-success btn-sm btn-seleccionar-pregunta"
                    data-id="${p.id}"
                    data-pregunta="${encodeURIComponent(p.pregunta ?? '')}"
                    data-tematica="${encodeURIComponent(p.tematica ?? '')}"
                    data-respuesta="${encodeURIComponent(p.respuesta ?? '')}"
                    data-subtema="${encodeURIComponent(p.subtema ?? '')}">
                    Seleccionar
                </button>
            </td>
            <td colspan="4" style="white-space:pre-line;word-break:break-word;max-width:700px;vertical-align:top;">
                <div style="font-weight:bold;">${p.pregunta}</div>
            </td>
        </tr>
        <tr>
            <td></td>
            <td style="width:180px;"><b>Temática:</b> ${p.tematica}</td>
            <td style="width:180px;"><b>Respuesta:</b> ${p.respuesta}</td>
            <td style="width:180px;"><b>Subtema:</b> ${p.subtema ?? ''}</td>
            <td></td>
        </tr>
        `;
    }).join('');
    // Paginación
    let paginacion = '';
    for (let i = 0; i < totalPages; i++) {
        paginacion += `<li class="page-item${i === page ? ' active' : ''}"><a class="page-link" href="#" onclick="buscarPreguntasModal(${i});return false;">${i + 1}</a></li>`;
    }
    document.getElementById('paginacion-selector-pregunta').innerHTML = paginacion;

    // Añadir event listener delegado para los botones
    tbody.querySelectorAll('.btn-seleccionar-pregunta').forEach(btn => {
        btn.addEventListener('click', function() {
            seleccionarPreguntaModal(
                this.dataset.id,
                decodeURIComponent(this.dataset.pregunta),
                decodeURIComponent(this.dataset.tematica),
                decodeURIComponent(this.dataset.respuesta),
                decodeURIComponent(this.dataset.subtema)
            );
        });
    });
}

function seleccionarPreguntaModal(id, pregunta, tematica, respuesta, subtema) {
    console.log('[FRONT] seleccionarPreguntaModal llamada con:', {id, pregunta, tematica, respuesta, subtema, selectorPreguntaContext});
    // --- NUEVO: Si hay contexto de añadir pregunta a cuestionario, hacer petición AJAX ---
    if (window.contextoAnadirPregunta) {
        const { cuestionarioId, nivel } = window.contextoAnadirPregunta;
        // Determinar el factor según el nivel
        let factorMultiplicacion = 1;
        if (nivel === 'PM1') factorMultiplicacion = 2;
        else if (nivel === 'PM2') factorMultiplicacion = 3;
        else if (nivel === 'PM3') factorMultiplicacion = 0;
        fetch(`/api/cuestionarios/${cuestionarioId}/preguntas`, {
            method: 'POST',
            headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' },
            body: JSON.stringify({ preguntaId: id, factorMultiplicacion })
        })
        .then(resp => {
            if (!resp.ok) throw new Error('No se pudo añadir la pregunta');
            return resp.json();
        })
        .then(() => {
            Toastify({ text: 'Pregunta añadida al cuestionario', duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' } }).showToast();
            CuestionariosManager.cargarCuestionarios();
        })
        .catch(e => {
            Toastify({ text: 'Error al añadir pregunta: ' + e.message, duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' } }).showToast();
        })
        .finally(() => {
            window.contextoAnadirPregunta = null;
            // Cerrar modal
            const modal = bootstrap.Modal.getInstance(document.getElementById('modal-selector-pregunta'));
            if (modal) modal.hide();
        });
        return;
    }
    // --- FIN NUEVO ---
    try {
        const input = document.getElementById(selectorPreguntaContext.inputId);
        const texto = document.getElementById(selectorPreguntaContext.textoId);
        if (!input || !texto) {
            console.error('[FRONT] No se encontró el input o el campo de texto para el selector:', selectorPreguntaContext);
            alert('Error interno: no se encontró el campo para asignar la pregunta seleccionada.');
            return;
        }
        input.value = id;
        texto.value = `${pregunta} [${tematica}] (${respuesta})${subtema ? ' - ' + subtema : ''}`;
        console.log('[FRONT] Pregunta seleccionada y asignada:', {inputId: selectorPreguntaContext.inputId, textoId: selectorPreguntaContext.textoId, id, texto: texto.value});
        // Cerrar modal
        const modal = bootstrap.Modal.getInstance(document.getElementById('modal-selector-pregunta'));
        if (modal) {
            modal.hide();
        } else {
            console.warn('[FRONT] No se pudo cerrar el modal porque no se encontró la instancia.');
        }
    } catch (e) {
        console.error('[FRONT] Error en seleccionarPreguntaModal:', e);
        alert('Error al seleccionar la pregunta. Revisa la consola para más detalles.');
    }
}

// --- FIN NUEVO SISTEMA DE SELECCIÓN ---

async function guardarCuestionario() {
    let valid = true;
    let preguntasNormales = [];
    normales.forEach(nivel => {
        const id = document.getElementById(`pregunta-${nivel}`).value;
        if (!id) valid = false;
        else preguntasNormales.push(Number(id));
    });
    if (!valid) {
        Toastify({
            text: 'Debes seleccionar todas las preguntas normales (niveles 1-4)',
            duration: 3000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
        }).showToast();
        return;
    }
    
    const cuestionarioId = document.getElementById('cuestionario-id').value;
    const tematica = document.getElementById('cuestionario-tematica').value;
    const notasDireccion = document.getElementById('cuestionario-notas').value;
    const esEdicion = !!cuestionarioId;
    
    // Validar que la temática esté gestionada si se proporciona
    if (tematica && tematica.trim() !== '') {
        try {
            const response = await fetch('/api/cuestionarios/tematicas', {
                headers: authManager.getAuthHeaders()
            });
            if (response.ok) {
                const tematicasGestionadas = await response.json();
                if (!tematicasGestionadas.includes(tematica.trim())) {
                    Toastify({
                        text: `La temática "${tematica}" no está gestionada. Debes añadirla desde "Gestionar Temáticas" primero.`,
                        duration: 5000,
                        close: true,
                        gravity: 'top',
                        position: 'right',
                        style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
                    }).showToast();
                    return;
                }
            }
        } catch (error) {
            console.error('Error al validar temática:', error);
        }
    }
    
    const payload = { 
        preguntasNormales,
        tematica,
        notasDireccion
    };
    
    try {
        let resp, data;
        if (esEdicion) {
            // PUT para editar
            resp = await fetch(`/api/cuestionarios/${cuestionarioId}`, {
                method: 'PUT',
                headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        } else {
            // POST para crear
            resp = await fetch('/api/cuestionarios/nuevo', {
                method: 'POST',
                headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        }
        try { data = await resp.json(); } catch (e) { data = null; }
        if (!resp.ok) throw new Error(data && data.message ? data.message : 'Error al guardar el cuestionario');
        Toastify({
            text: data && data.message ? data.message : (esEdicion ? 'Cuestionario editado correctamente' : 'Cuestionario creado correctamente'),
            duration: 3000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' }
        }).showToast();
        const modal = bootstrap.Modal.getInstance(document.getElementById('modal-cuestionario'));
        modal.hide();
        await CuestionariosManager.cargarCuestionarios();
    } catch (error) {
        Toastify({
            text: 'Error al guardar cuestionario: ' + error.message,
            duration: 3000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
        }).showToast();
    }
}

// --- EDICIÓN Y ELIMINACIÓN DE CUESTIONARIOS ---
window.editarCuestionario = async function(id) {
    try {
        const resp = await fetch(`/api/cuestionarios/${id}`, { headers: authManager.getAuthHeaders() });
        if (!resp.ok) throw new Error('No se pudo cargar el cuestionario');
        const cuestionario = await resp.json();
        // Precargar preguntas normales y PM en el modal
        const normalesIds = ['1LS','2NLS','3LS','4NLS'];
        const pmsIds = ['PM1','PM2','PM3'];
        // Limpiar primero
        normalesIds.forEach(nivel => {
            document.getElementById(`pregunta-${nivel}`).value = '';
            document.getElementById(`pregunta-${nivel}-texto`).value = '';
        });
        pmsIds.forEach(pm => {
            document.getElementById(`pm-${pm}`).value = '';
            document.getElementById(`pm-${pm}-texto`).value = '';
        });
        // Mapear preguntas por nivel
        if (cuestionario.preguntas && Array.isArray(cuestionario.preguntas)) {
            // Ordenar igual que en la tabla
            const ordenNivel = { '_1LS': '1LS', '_2NLS': '2NLS', '_3LS': '3LS', '_4NLS': '4NLS', 'PM1': 'PM1', 'PM2': 'PM2', 'PM3': 'PM3' };
            cuestionario.preguntas.forEach(pq => {
                const p = pq.pregunta || pq;
                const nivel = (p.nivel || '').toUpperCase();
                if (ordenNivel[nivel]) {
                    if (ordenNivel[nivel].startsWith('P')) {
                        // PM
                        document.getElementById(`pm-${ordenNivel[nivel]}`).value = p.id;
                        document.getElementById(`pm-${ordenNivel[nivel]}-texto`).value = `${p.pregunta} [${p.tematica}] (${p.respuesta})`;
                    } else {
                        // Normal
                        document.getElementById(`pregunta-${ordenNivel[nivel]}`).value = p.id;
                        document.getElementById(`pregunta-${ordenNivel[nivel]}-texto`).value = `${p.pregunta} [${p.tematica}] (${p.respuesta})`;
                    }
                }
            });
        }
        document.getElementById('cuestionario-id').value = cuestionario.id;
        document.getElementById('modal-cuestionario-titulo').innerText = 'Editar Cuestionario';
        const modal = new bootstrap.Modal(document.getElementById('modal-cuestionario'));
        modal.show();
    } catch (e) {
        Toastify({ text: 'Error al cargar cuestionario: ' + e.message, duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' } }).showToast();
    }
};

window.eliminarCuestionario = async function(id) {
    if (!confirm('¿Seguro que quieres eliminar este cuestionario? Esta acción no se puede deshacer.')) return;
    try {
        const resp = await fetch(`/api/cuestionarios/${id}`, { method: 'DELETE', headers: authManager.getAuthHeaders() });
        if (!resp.ok) throw new Error('No se pudo eliminar el cuestionario');
        Toastify({ text: 'Cuestionario eliminado', duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' } }).showToast();
        await CuestionariosManager.cargarCuestionarios();
    } catch (e) {
        Toastify({ text: 'Error al eliminar cuestionario: ' + e.message, duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' } }).showToast();
    }
};

// --- AUTO-SCROLL HORIZONTAL EN TABLA DE CUESTIONARIOS ---
document.addEventListener('DOMContentLoaded', function() {
    const contenedor = document.querySelector('.table-responsive');
    if (!contenedor) return;
    let scrollInterval = null;
    contenedor.addEventListener('mousemove', function(e) {
        const borde = 60; // px desde el borde para activar scroll
        const { left, right } = contenedor.getBoundingClientRect();
        const x = e.clientX;
        const scrollSpeed = 15; // px por frame
        clearInterval(scrollInterval);
        if (x - left < borde) {
            // Scroll a la izquierda
            scrollInterval = setInterval(() => {
                contenedor.scrollLeft -= scrollSpeed;
            }, 16);
        } else if (right - x < borde) {
            // Scroll a la derecha
            scrollInterval = setInterval(() => {
                contenedor.scrollLeft += scrollSpeed;
            }, 16);
        }
    });
    contenedor.addEventListener('mouseleave', function() {
        clearInterval(scrollInterval);
    });
});

// --- NUEVAS FUNCIONES PARA ELIMINAR Y AÑADIR PREGUNTA EN CUESTIONARIO ---
window.eliminarPreguntaDeCuestionario = async function(cuestionarioId, slot) {
    if (!confirm('¿Seguro que quieres quitar esta pregunta del cuestionario?')) return;
    try {
        const resp = await fetch(`/api/cuestionarios/${cuestionarioId}/preguntas/slot/${slot}`, {
            method: 'DELETE',
            headers: authManager.getAuthHeaders()
        });
        if (!resp.ok) throw new Error('No se pudo quitar la pregunta');
        Toastify({ text: 'Pregunta eliminada del cuestionario', duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' } }).showToast();
        await CuestionariosManager.cargarCuestionarios();
    } catch (e) {
        Toastify({ text: 'Error al quitar pregunta: ' + e.message, duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' } }).showToast();
    }
}
window.anadirPreguntaACuestionario = function(cuestionarioId, nivel) {
    // Aquí puedes abrir el modal de selección de pregunta, reutilizando el existente
    abrirSelectorPregunta(nivel); // Debes adaptar para que asigne la pregunta al cuestionario correspondiente
    // Puedes guardar el contexto de cuestionarioId y nivel para usar al seleccionar
    window.contextoAnadirPregunta = { cuestionarioId, nivel };
}

// Guardar el último listado de cuestionarios para búsquedas rápidas
CuestionariosManager.ultimoListado = [];
const _oldMostrar = CuestionariosManager.mostrarCuestionarios;
CuestionariosManager.mostrarCuestionarios = function(cuestionarios) {
    CuestionariosManager.ultimoListado = cuestionarios;
    _oldMostrar.call(this, cuestionarios);
}

// Funciones de filtrado
window.filtrarCuestionarios = async function() {
    try {
        const estado = document.getElementById('filtro-estado-cuestionario')?.value || '';
        const tematica = document.getElementById('filtro-tematica-cuestionario')?.value || '';
        const busqueda = document.getElementById('buscar-cuestionario')?.value || '';

        // Si hay filtros de estado o temática, usar backend
        if (estado || tematica) {
            const params = new URLSearchParams();
            if (estado) params.append('estado', estado);
            if (tematica) params.append('tematica', tematica);

            const response = await fetch(`/api/cuestionarios/filtrar?${params.toString()}`, {
                headers: authManager.getAuthHeaders()
            });

            if (!response.ok) throw new Error('Error al filtrar cuestionarios');
            const cuestionarios = await response.json();
            
            // Aplicar filtro de búsqueda por ID si existe
            let cuestionariosFiltrados = cuestionarios;
            if (busqueda) {
                cuestionariosFiltrados = cuestionarios.filter(c => 
                    c.id.toString().includes(busqueda)
                );
            }
            
            CuestionariosManager.mostrarCuestionarios(cuestionariosFiltrados);
        } else if (busqueda) {
            // Solo filtro de búsqueda, usar datos en memoria
            const cuestionariosFiltrados = CuestionariosManager.ultimoListado.filter(c => 
                c.id.toString().includes(busqueda)
            );
            CuestionariosManager.mostrarCuestionarios(cuestionariosFiltrados);
        } else {
            // Sin filtros, recargar todos
            await CuestionariosManager.cargarCuestionarios();
        }
    } catch (error) {
        console.error('Error al filtrar cuestionarios:', error);
        Toastify({
            text: 'Error al filtrar cuestionarios',
            duration: 3000,
            close: true,
            gravity: "top",
            position: "right",
            style: { background: "linear-gradient(to right, #ff0000, #cc0000)" }
        }).showToast();
    }
};

window.limpiarFiltrosCuestionarios = function() {
    document.getElementById('filtro-estado-cuestionario').value = '';
    document.getElementById('filtro-tematica-cuestionario').value = '';
    document.getElementById('buscar-cuestionario').value = '';
    CuestionariosManager.cargarCuestionarios();
};

window.actualizarNotasDireccion = async function(cuestionarioId, notas) {
    try {
        const response = await fetch(`/api/cuestionarios/${cuestionarioId}/notas-direccion`, {
            method: 'PUT',
            headers: {
                ...authManager.getAuthHeaders(),
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ notasDireccion: notas })
        });

        if (!response.ok) throw new Error('Error al actualizar notas');
        
        Toastify({
            text: 'Notas de dirección actualizadas',
            duration: 2000,
            close: true,
            gravity: "top",
            position: "right",
            style: { background: "linear-gradient(to right, #00b09b, #96c93d)" }
        }).showToast();
    } catch (error) {
        console.error('Error al actualizar notas:', error);
        Toastify({
            text: 'Error al actualizar notas de dirección',
            duration: 3000,
            close: true,
            gravity: "top",
            position: "right",
            style: { background: "linear-gradient(to right, #ff0000, #cc0000)" }
        }).showToast();
    }
};

window.cambiarPassword = function() {
    document.getElementById('form-cambiar-password').reset();
    const modal = new bootstrap.Modal(document.getElementById('modal-cambiar-password'));
    modal.show();
};

window.cambiarEstadoCuestionario = async function(id, nuevoEstado) {
    try {
        const response = await fetch(`/api/cuestionarios/${id}/estado?nuevoEstado=${nuevoEstado}`, {
            method: 'PUT',
            headers: authManager.getAuthHeaders()
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText);
        }

        const data = await response.json();
        await CuestionariosManager.cargarCuestionarios();
        
        Toastify({
            text: `Estado cambiado a: ${nuevoEstado}`,
            duration: 3000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' }
        }).showToast();
    } catch (error) {
        Toastify({
            text: `Error: ${error.message}`,
            duration: 4000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
        }).showToast();
    }
}; 

window.cambiarTematicaCuestionario = async function(id, nuevaTematica) {
    try {
        const response = await fetch(`/api/cuestionarios/${id}/tematica`, {
            method: 'PUT',
            headers: {
                ...authManager.getAuthHeaders(),
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ tematica: nuevaTematica })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText);
        }

        const data = await response.json();
        await CuestionariosManager.cargarCuestionarios();
        
        Toastify({
            text: `Temática cambiada a: ${nuevaTematica || 'Genérico'}`,
            duration: 3000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' }
        }).showToast();
    } catch (error) {
        Toastify({
            text: `Error: ${error.message}`,
            duration: 4000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
        }).showToast();
    }
};

// Gestión de Temáticas de Cuestionarios
const TematicasManager = {
    tematicas: [],

    async cargarTematicas() {
        try {
            const response = await fetch('/api/cuestionarios/tematicas', {
                headers: authManager.getAuthHeaders()
            });
            if (!response.ok) throw new Error('Error al cargar temáticas');
            this.tematicas = await response.json();
            this.mostrarTematicas();
        } catch (error) {
            console.error('Error al cargar temáticas:', error);
            mostrarError('Error al cargar temáticas: ' + error.message);
        }
    },

    async cargarEstadisticas() {
        try {
            const response = await fetch('/api/cuestionarios/tematicas/estadisticas', {
                headers: authManager.getAuthHeaders()
            });
            if (!response.ok) throw new Error('Error al cargar estadísticas');
            const stats = await response.json();
            
            document.getElementById('total-tematicas').textContent = stats.totalTematicas;
        } catch (error) {
            console.error('Error al cargar estadísticas:', error);
        }
    },

    mostrarTematicas() {
        const tbody = document.getElementById('lista-tematicas');
        if (!tbody) return;
        
        tbody.innerHTML = '';
        this.tematicas.forEach((tematica, index) => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${index + 1}</td>
                <td>${tematica}</td>
                <td>
                    <button class="btn btn-sm btn-danger" onclick="TematicasManager.eliminarTematica('${tematica}')">
                        <i class="fas fa-trash"></i> Eliminar
                    </button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    },

    async añadirTematica(nombreTematica) {
        try {
            const response = await fetch('/api/cuestionarios/tematicas', {
                method: 'POST',
                headers: {
                    ...authManager.getAuthHeaders(),
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ tematica: nombreTematica })
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            const result = await response.json();
            mostrarExito(result.mensaje);
            
            // Limpiar formulario
            document.getElementById('nueva-tematica').value = '';
            
            // Recargar datos
            await this.cargarTematicas();
            await this.cargarEstadisticas();
            
        } catch (error) {
            mostrarError('Error al añadir temática: ' + error.message);
        }
    },

    async eliminarTematica(nombreTematica) {
        if (!confirm(`¿Estás seguro de que quieres eliminar la temática "${nombreTematica}"?`)) {
            return;
        }

        try {
            const response = await fetch(`/api/cuestionarios/tematicas/${encodeURIComponent(nombreTematica)}`, {
                method: 'DELETE',
                headers: authManager.getAuthHeaders()
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            const result = await response.json();
            mostrarExito(result.mensaje);
            
            // Recargar datos
            await this.cargarTematicas();
            await this.cargarEstadisticas();
            
        } catch (error) {
            mostrarError('Error al eliminar temática: ' + error.message);
        }
    }
};

// Funciones globales para los botones
window.mostrarGestionTematicas = function() {
    const modal = new bootstrap.Modal(document.getElementById('modal-gestion-temas-subtemas'));
    modal.show();
    TematicasManager.cargarTematicas();
    TematicasManager.cargarEstadisticas();
};

// Event listeners para los formularios
document.addEventListener('DOMContentLoaded', function() {
    // Formulario añadir temática
    document.getElementById('form-añadir-tematica')?.addEventListener('submit', function(e) {
        e.preventDefault();
        const nombreTematica = document.getElementById('nueva-tematica').value.trim();
        if (nombreTematica) {
            TematicasManager.añadirTematica(nombreTematica);
        }
    });
}); 