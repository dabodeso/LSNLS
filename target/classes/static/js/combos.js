// Módulo de gestión de combos
const CombosManager = {
    async cargarCombos() {
        try {
            if (!authManager.isAuthenticated()) {
                console.error('Usuario no autenticado');
                return;
            }
            const response = await fetch('/api/combos', {
                headers: authManager.getAuthHeaders()
            });
            if (!response.ok) throw new Error('Error al cargar los combos');
            const combos = await response.json();
            this.mostrarCombos(combos);
        } catch (error) {
            if (error && error.message && error.message.startsWith('401')) {
                return;
            }
            console.error('Error al cargar combos:', error);
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

    mostrarCombos(combos) {
        const tbody = document.getElementById('tabla-combos');
        if (!tbody) {
            console.error('No se encontró el elemento tabla-combos');
            return;
        }
        tbody.innerHTML = '';
        if (!Array.isArray(combos) || combos.length === 0) {
            const tr = document.createElement('tr');
            tr.innerHTML = '<td colspan="5" class="text-center">No hay combos</td>';
            tbody.appendChild(tr);
            return;
        }
        combos.forEach(c => {
            // Determinar si hay huecos usando slot
            const niveles = ['PM1','PM2','PM3'];
            const preguntasPorSlot = {};
            if (Array.isArray(c.preguntas)) {
                c.preguntas.forEach(pc => {
                    if (pc && pc.slot) preguntasPorSlot[pc.slot] = pc.pregunta;
                });
            }
            const tr = document.createElement('tr');
            tr.setAttribute('data-id', c.id);
            tr.innerHTML = `
                <td style="font-weight: bold; font-size: 1.2em; color: #0066cc;">${c.id ?? ''}</td>
                <td>
                    <select class="form-select form-select-sm" onchange="actualizarCombo(${c.id}, 'tipo', this.value)">
                        <option value="">Sin tipo</option>
                        <option value="P" ${c.tipo === 'P' ? 'selected' : ''}>P (Premio)</option>
                        <option value="A" ${c.tipo === 'A' ? 'selected' : ''}>A (Asequible)</option>
                        <option value="D" ${c.tipo === 'D' ? 'selected' : ''}>D (Difícil)</option>
                    </select>
                </td>
                <td>
                    ${c.estado === 'asignado_jornada' || c.estado === 'asignado_concursantes' ? 
                        `<span class="badge ${Utils.getEstadoBadgeClass(c.estado, 'combo')}">${Utils.formatearEstadoCombo(c.estado)}</span>` : 
                        `<select class="form-select form-select-sm" onchange="actualizarCombo(${c.id}, 'estado', this.value)">
                            <option value="borrador" ${c.estado === 'borrador' ? 'selected' : ''}>Borrador</option>
                            <option value="creado" ${c.estado === 'creado' ? 'selected' : ''}>Creado</option>
                            <option value="adjudicado" ${c.estado === 'adjudicado' ? 'selected' : ''}>Adjudicado</option>
                            <option value="grabado" ${c.estado === 'grabado' ? 'selected' : ''}>Grabado</option>
                        </select>`
                    }
                </td>
                <td>${(c.preguntas && c.preguntas.length) || 0}</td>
                <td>${c.fechaCreacion ? Utils.formatearFecha(String(c.fechaCreacion)) : ''}</td>
                <td>
                    <button class="btn btn-sm btn-danger" onclick="eliminarCombo(${c.id})"><i class="fas fa-trash"></i> Eliminar</button>
                </td>
            `;
            tbody.appendChild(tr);

            // Subtabla de preguntas con la estética de cuestionarios
            const subtr = document.createElement('tr');
            subtr.classList.add('cuestionario-subtabla');
            
            // Generar exactamente 3 filas para las preguntas del combo
            let filasPreguntas = '';
            for (let i = 0; i < 3; i++) {
                const slotNivel = niveles[i]; // 'PM1', 'PM2', 'PM3'
                const p = preguntasPorSlot[slotNivel];
                
                if (p) {
                    // Fila con pregunta - mostrar nivel real de la pregunta con su multiplicador
                    let multiplicador = '';
                    if (slotNivel === 'PM1') multiplicador = ' (X2)';
                    else if (slotNivel === 'PM2') multiplicador = ' (X3)';
                    else if (slotNivel === 'PM3') multiplicador = ' (X)';
                    
                    const nivelReal = p.nivel ? p.nivel.replace('_', '') : slotNivel;
                    const nivelMostrar = nivelReal + multiplicador;
                    
                    filasPreguntas += `<tr data-id="${p.id}" data-nivel="${slotNivel}" style="cursor:pointer;">
                        <td><span class='${CombosManager.getNivelColor ? CombosManager.getNivelColor(p.nivel) : ''}'>${nivelMostrar}</span></td>
                        <td>${p.pregunta ?? ''}</td>
                        <td>${p.respuesta ?? ''}</td>
                        <td><button class='btn btn-sm btn-danger' onclick='event.stopPropagation();eliminarPreguntaDeCombo(${c.id}, "${slotNivel}")'><i class='fas fa-trash'></i></button></td>
                    </tr>`;
                } else {
                    // Fila vacía con botón añadir - mostrar slot con multiplicador
                    let multiplicador = '';
                    let tipoSlot = '';
                    if (slotNivel === 'PM1') { multiplicador = ' (X2)'; tipoSlot = 'PM1'; }
                    else if (slotNivel === 'PM2') { multiplicador = ' (X3)'; tipoSlot = 'PM2'; }
                    else if (slotNivel === 'PM3') { multiplicador = ' (X)'; tipoSlot = 'PM3'; }
                    
                    const nivelMostrar = tipoSlot + multiplicador;
                    
                    filasPreguntas += `<tr data-nivel="${slotNivel}">
                        <td><span class='${CombosManager.getNivelColor ? CombosManager.getNivelColor(slotNivel) : ''}'>${nivelMostrar}</span></td>
                        <td class="text-center text-muted">(Vacío)</td>
                        <td class="text-center text-muted">-</td>
                        <td><button class='btn btn-sm btn-success' onclick='event.stopPropagation();anadirPreguntaACombo(${c.id}, "${slotNivel}")'><i class='fas fa-plus'></i></button></td>
                    </tr>`;
                }
            }
            
            subtr.innerHTML = `<td colspan="6">
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
        if (["_5NLS"].includes(nivel)) return 'text-danger fw-bold';
        if (["_5LS"].includes(nivel)) return 'text-success fw-bold';
        return '';
    },

    formatearTipo(tipo) {
        switch(tipo) {
            case 'P': return 'Premio (P)';
            case 'A': return 'Asequible (A)';
            case 'D': return 'Difícil (D)';
            default: return '-';
        }
    },
};

// Guardar el último listado de combos para búsquedas rápidas
CombosManager.ultimoListado = [];
const _oldMostrarCombos = CombosManager.mostrarCombos;
CombosManager.mostrarCombos = function(combos) {
    CombosManager.ultimoListado = combos;
    _oldMostrarCombos.call(this, combos);
}

// Funciones de filtrado
window.filtrarCombos = async function() {
    try {
        const estado = document.getElementById('filtro-estado-combo')?.value || '';
        const tipo = document.getElementById('filtro-tipo-combo')?.value || '';
        const busqueda = document.getElementById('buscar-combo')?.value || '';

        let combosFiltrados = CombosManager.ultimoListado;

        // Filtrar por estado
        if (estado) {
            combosFiltrados = combosFiltrados.filter(c => c.estado === estado);
        }

        // Filtrar por tipo
        if (tipo) {
            combosFiltrados = combosFiltrados.filter(c => c.tipo === tipo);
        }

        // Filtrar por búsqueda de ID
        if (busqueda) {
            combosFiltrados = combosFiltrados.filter(c => 
                c.id.toString().includes(busqueda)
            );
        }

        CombosManager.mostrarCombos(combosFiltrados);
    } catch (error) {
        console.error('Error al filtrar combos:', error);
        await CombosManager.cargarCombos();
    }
}

window.limpiarFiltrosCombos = function() {
    document.getElementById('filtro-estado-combo').value = '';
    document.getElementById('filtro-tipo-combo').value = '';
    document.getElementById('buscar-combo').value = '';
    CombosManager.cargarCombos();
}

// Función para actualizar tipo o estado de combo
window.actualizarCombo = async function(comboId, campo, valor) {
    try {
        const datos = {};
        datos[campo] = valor;
        
        const response = await fetch(`/api/combos/${comboId}`, {
            method: 'PUT',
            headers: {
                ...authManager.getAuthHeaders(),
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(datos)
        });
        
        if (!response.ok) {
            const errorData = await response.text();
            throw new Error(errorData || 'Error al actualizar combo');
        }
        
        const data = await response.json();
        
        Toastify({
            text: data.message || `${campo === 'tipo' ? 'Tipo' : 'Estado'} actualizado correctamente`,
            duration: 2000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' }
        }).showToast();
        
        // Recargar la lista para reflejar el cambio
        await CombosManager.cargarCombos();
        
    } catch (error) {
        console.error('Error al actualizar combo:', error);
        Toastify({
            text: 'Error: ' + error.message,
            duration: 3000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
        }).showToast();
        
        // Recargar para revertir el cambio visual
        await CombosManager.cargarCombos();
    }
};

function inicializarCombos() {
    CombosManager.cargarCombos();
}

document.addEventListener('DOMContentLoaded', inicializarCombos);

// IDs de los campos para las preguntas multiplicadoras
const pms = [
    {id: 'PM1', factor: 'X2'},
    {id: 'PM2', factor: 'X3'},
    {id: 'PM3', factor: 'X'}
];

let selectorPreguntaContext = { nivel: null, factor: null, inputId: null, textoId: null };

async function mostrarFormularioCombo() {
    // Limpiar selects y textos multiplicadores
    pms.forEach(pm => {
        const sel = document.getElementById(`pm-${pm.id}`);
        const texto = document.getElementById(`pm-${pm.id}-texto`);
        if (sel) sel.value = '';
        if (texto) texto.value = '';
    });
    // Mostrar modal
    const modal = new bootstrap.Modal(document.getElementById('modal-combo'));
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
    selectorPreguntaContext.inputId = `pm-${nivel}`;
    selectorPreguntaContext.textoId = `pm-${nivel}-texto`;
    
    document.getElementById('buscador-id').value = '';
    document.getElementById('buscador-pregunta').value = '';
    document.getElementById('buscador-respuesta').value = '';
    document.getElementById('buscador-tematica').value = '';
    inicializarBuscadorPreguntasModal();
    buscarPreguntasModal(0);
    const modal = new bootstrap.Modal(document.getElementById('modal-selector-pregunta'));
    modal.show();
}

// Función para obtener IDs de preguntas ya seleccionadas en el combo
function obtenerPreguntasYaSeleccionadas() {
    const preguntasSeleccionadas = [];
    const pms = ['PM1', 'PM2', 'PM3'];
    
    pms.forEach(pm => {
        const input = document.getElementById(`pm-${pm}`);
        if (input && input.value) {
            preguntasSeleccionadas.push(parseInt(input.value));
        }
    });
    
    return preguntasSeleccionadas;
}

// Función para limpiar la selección de un PM específico
function limpiarSeleccionPM(pm) {
    const input = document.getElementById(`pm-${pm}`);
    const texto = document.getElementById(`pm-${pm}-texto`);
    
    if (input) input.value = '';
    if (texto) texto.value = '';
    
    console.log(`Limpiada selección de ${pm}`);
}

async function buscarPreguntasModal(page = 0) {
    const id = document.getElementById('buscador-id').value.trim();
    const pregunta = document.getElementById('buscador-pregunta').value.trim();
    const respuesta = document.getElementById('buscador-respuesta').value.trim();
    const tematica = document.getElementById('buscador-tematica').value.trim();

    try {
        let preguntas = [];
        let totalPages = 1;
        
        // Para combos, buscar solo preguntas de nivel 5 (_5LS y _5NLS)
        const respLS = await fetch(`/api/preguntas/buscar?nivel=_5LS&page=${page}&size=20&id=${encodeURIComponent(id)}&pregunta=${encodeURIComponent(pregunta)}&respuesta=${encodeURIComponent(respuesta)}&tematica=${encodeURIComponent(tematica)}`, { headers: authManager.getAuthHeaders() });
        const respNLS = await fetch(`/api/preguntas/buscar?nivel=_5NLS&page=${page}&size=20&id=${encodeURIComponent(id)}&pregunta=${encodeURIComponent(pregunta)}&respuesta=${encodeURIComponent(respuesta)}&tematica=${encodeURIComponent(tematica)}`, { headers: authManager.getAuthHeaders() });
        const dataLS = await respLS.json();
        const dataNLS = await respNLS.json();
        preguntas = [...(dataLS.content || []), ...(dataNLS.content || [])];
        totalPages = Math.max(dataLS.totalPages || 1, dataNLS.totalPages || 1);
        
        // Filtrar preguntas ya seleccionadas (solo si estamos creando un combo nuevo)
        if (!window.contextoAnadirPregunta) {
            const preguntasYaSeleccionadas = obtenerPreguntasYaSeleccionadas();
            if (preguntasYaSeleccionadas.length > 0) {
                preguntas = preguntas.filter(p => !preguntasYaSeleccionadas.includes(p.id));
            }
        }
        
        renderPreguntasModal(preguntas, page, totalPages);
    } catch (e) {
        document.getElementById('tbody-selector-pregunta').innerHTML = `<tr><td colspan="6">Error al cargar preguntas</td></tr>`;
        document.getElementById('paginacion-selector-pregunta').innerHTML = '';
    }
}

function renderPreguntasModal(preguntas, currentPage, totalPages) {
    const tbody = document.getElementById('tbody-selector-pregunta');
    tbody.innerHTML = '';
    
    if (!preguntas || preguntas.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center">No se encontraron preguntas</td></tr>';
        return;
    }
    
    preguntas.forEach(p => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${p.id}</td>
            <td>${p.pregunta}</td>
            <td>${p.respuesta}</td>
            <td>${p.tematica}</td>
            <td><span class="${CombosManager.getNivelColor(p.nivel)}">${p.nivel}</span></td>
            <td>
                <button class="btn btn-sm btn-success" onclick="seleccionarPreguntaModal(${p.id}, '${p.pregunta.replace(/'/g, "\\'")}', '${p.tematica.replace(/'/g, "\\'")}', '${p.respuesta.replace(/'/g, "\\'")}', '${p.subtema || ''}')">
                    Seleccionar
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
    
    // Renderizar paginación
    const paginacion = document.getElementById('paginacion-selector-pregunta');
    paginacion.innerHTML = '';
    if (totalPages > 1) {
        const nav = document.createElement('nav');
        const ul = document.createElement('ul');
        ul.className = 'pagination justify-content-center';
        
        for (let i = 0; i < totalPages; i++) {
            const li = document.createElement('li');
            li.className = `page-item ${i === currentPage ? 'active' : ''}`;
            li.innerHTML = `<a class="page-link" href="#" onclick="buscarPreguntasModal(${i})">${i + 1}</a>`;
            ul.appendChild(li);
        }
        nav.appendChild(ul);
        paginacion.appendChild(nav);
    }
}

function seleccionarPreguntaModal(id, pregunta, tematica, respuesta, subtema) {
    console.log('[FRONT] seleccionarPreguntaModal llamada con:', {id, pregunta, tematica, respuesta, subtema, selectorPreguntaContext});
    
    // Si hay contexto de añadir pregunta a combo, hacer petición AJAX
    if (window.contextoAnadirPregunta) {
        const { comboId, nivel } = window.contextoAnadirPregunta;
        // Determinar el factor según el nivel
        let factorMultiplicacion = 1;
        if (nivel === 'PM1') factorMultiplicacion = 2;
        else if (nivel === 'PM2') factorMultiplicacion = 3;
        else if (nivel === 'PM3') factorMultiplicacion = 0;
        
        fetch(`/api/combos/${comboId}/preguntas`, {
            method: 'POST',
            headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' },
            body: JSON.stringify({ preguntaId: id, factorMultiplicacion })
        })
        .then(resp => {
            if (!resp.ok) throw new Error('No se pudo añadir la pregunta');
            return resp.json();
        })
        .then(() => {
            Toastify({ text: 'Pregunta añadida al combo', duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' } }).showToast();
            CombosManager.cargarCombos();
        })
        .catch(e => {
            Toastify({ text: 'Error al añadir pregunta: ' + e.message, duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' } }).showToast();
        })
        .finally(() => {
            window.contextoAnadirPregunta = null;
            const modal = bootstrap.Modal.getInstance(document.getElementById('modal-selector-pregunta'));
            if (modal) modal.hide();
        });
        return;
    }
    
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

async function guardarCombo() {
    const tipo = document.getElementById('combo-tipo').value;
    if (!tipo) {
        Toastify({
            text: 'Debes seleccionar el tipo de combo',
            duration: 3000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
        }).showToast();
        return;
    }

    let valid = true;
    let preguntasMultiplicadoras = [];
    pms.forEach((pm, idx) => {
        const id = document.getElementById(`pm-${pm.id}`).value;
        if (!id) valid = false;
        else preguntasMultiplicadoras.push({ id: Number(id), factor: pm.factor });
    });
    
    if (!valid) {
        Toastify({
            text: 'Debes seleccionar todas las preguntas multiplicadoras (PM1, PM2, PM3)',
            duration: 3000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
        }).showToast();
        return;
    }
    

    
    const comboId = document.getElementById('combo-id').value;
    const esEdicion = !!comboId;
    
    try {
        let resp, data;
        if (esEdicion) {
            // PUT para editar (implementar si es necesario)
            resp = await fetch(`/api/combos/${comboId}`, {
                method: 'PUT',
                headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' },
                body: JSON.stringify({ preguntasMultiplicadoras, tipo })
            });
        } else {
            // POST para crear
            resp = await fetch('/api/combos/nuevo', {
                method: 'POST',
                headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' },
                body: JSON.stringify({ preguntasMultiplicadoras, tipo })
            });
        }
        
        try { data = await resp.json(); } catch (e) { data = null; }
        if (!resp.ok) throw new Error(data && data.message ? data.message : 'Error al guardar el combo');
        
        Toastify({
            text: data && data.message ? data.message : (esEdicion ? 'Combo editado correctamente' : 'Combo creado correctamente'),
            duration: 3000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' }
        }).showToast();
        
        const modal = bootstrap.Modal.getInstance(document.getElementById('modal-combo'));
        modal.hide();
        await CombosManager.cargarCombos();
    } catch (error) {
        Toastify({
            text: 'Error al guardar combo: ' + error.message,
            duration: 3000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
        }).showToast();
    }
}

window.eliminarCombo = async function(id) {
    if (!confirm('¿Seguro que quieres eliminar este combo? Esta acción no se puede deshacer.')) return;
    try {
        const resp = await fetch(`/api/combos/${id}`, { method: 'DELETE', headers: authManager.getAuthHeaders() });
        if (!resp.ok) throw new Error('No se pudo eliminar el combo');
        Toastify({ text: 'Combo eliminado', duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' } }).showToast();
        await CombosManager.cargarCombos();
    } catch (e) {
        Toastify({ text: 'Error al eliminar combo: ' + e.message, duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' } }).showToast();
    }
};

window.eliminarPreguntaDeCombo = async function(comboId, slot) {
    console.log(`[DEBUG] Intentando eliminar pregunta del combo ${comboId}, slot ${slot}`);
    
    // Buscar el id real de la pregunta en ese slot
    const combo = CombosManager.ultimoListado?.find(c => c.id === comboId);
    console.log(`[DEBUG] Combo encontrado:`, combo);
    
    let preguntaId = null;
    if (combo && Array.isArray(combo.preguntas)) {
        console.log(`[DEBUG] Preguntas del combo:`, combo.preguntas);
        const pc = combo.preguntas.find(pc => pc.slot === slot);
        console.log(`[DEBUG] Pregunta encontrada para slot ${slot}:`, pc);
        if (pc && pc.pregunta && pc.pregunta.id) preguntaId = pc.pregunta.id;
    }
    
    if (!preguntaId) {
        console.log(`[DEBUG] No se encontró pregunta ID para el slot ${slot}`);
        Toastify({ text: 'No se encontró la pregunta a eliminar', duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' } }).showToast();
        return;
    }
    
    console.log(`[DEBUG] Eliminando pregunta ${preguntaId} del combo ${comboId}`);
    
    if (!confirm('¿Seguro que quieres quitar esta pregunta del combo?')) return;
    try {
        const resp = await fetch(`/api/combos/${comboId}/preguntas/${preguntaId}`, {
            method: 'DELETE',
            headers: authManager.getAuthHeaders()
        });
        if (!resp.ok) {
            const errorText = await resp.text();
            console.log(`[DEBUG] Error del servidor:`, errorText);
            throw new Error('No se pudo quitar la pregunta: ' + errorText);
        }
        Toastify({ text: 'Pregunta eliminada del combo', duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' } }).showToast();
        await CombosManager.cargarCombos();
    } catch (e) {
        console.error(`[DEBUG] Error al eliminar pregunta:`, e);
        Toastify({ text: 'Error al quitar pregunta: ' + e.message, duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' } }).showToast();
    }
}

window.anadirPreguntaACombo = function(comboId, nivel) {
    abrirSelectorPregunta(nivel);
    window.contextoAnadirPregunta = { comboId, nivel };
}



// Guardar el último listado de combos para búsquedas rápidas
CombosManager.ultimoListado = [];
const _oldMostrar = CombosManager.mostrarCombos;
CombosManager.mostrarCombos = function(combos) {
    CombosManager.ultimoListado = combos;
    _oldMostrar.call(this, combos);
}

window.cambiarPassword = function() {
    document.getElementById('form-cambiar-password').reset();
    const modal = new bootstrap.Modal(document.getElementById('modal-cambiar-password'));
    modal.show();
}; 