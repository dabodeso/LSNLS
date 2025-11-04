// Módulo de gestión de cuestionarios
const CuestionariosManager = {
    cuestionarios: [],
    paginaActual: 0,
    tamanioPagina: 25,
    totalCuestionarios: 0,
    totalPaginas: 0,
    cargando: false,
    lastScrollY: 0,
    lastFocusCuestionarioId: null,

    rememberScroll() {
        this.lastScrollY = window.scrollY || window.pageYOffset || 0;
    },
    restoreScrollOrFocus() {
        if (this.lastFocusCuestionarioId) {
            const row = document.querySelector(`tr.fila-cuestionario[data-id="${this.lastFocusCuestionarioId}"]`);
            if (row) {
                row.scrollIntoView({ block: 'start', behavior: 'auto' });
                try { window.scrollBy(0, -80); } catch (e) {}
                return;
            }
        }
        window.scrollTo({ top: this.lastScrollY || 0, behavior: 'auto' });
    },
    
    async cargarCuestionarios(resetear = true, mantenerPagina = false) {
        try {
            if (!authManager.isAuthenticated()) {
                console.error('Usuario no autenticado');
                return;
            }
            
            if (resetear) {
                if (!mantenerPagina) {
                    this.paginaActual = 0;
                }
                this.cuestionarios = [];
            }
            
            this.cargando = true;
            this.mostrarEstadoCarga();
            
            const params = new URLSearchParams({
                page: this.paginaActual,
                size: this.tamanioPagina
            });
            
            const response = await fetch(`/api/cuestionarios?${params}`, {
                headers: authManager.getAuthHeaders()
            });
            
            if (!response.ok) throw new Error('Error al cargar los cuestionarios');
            
            const data = await response.json();
            
            if (resetear) {
                this.cuestionarios = data.cuestionarios;
            } else {
                this.cuestionarios = [...this.cuestionarios, ...data.cuestionarios];
            }
            
            this.totalCuestionarios = data.totalItems;
            this.totalPaginas = data.totalPages;
            this.paginaActual = data.currentPage;
            
            await this.mostrarCuestionarios(this.cuestionarios);
            this.actualizarPaginacion();
            this.cargando = false;
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
            this.cargando = false;
        }
    },
    
    async cargarMasCuestionarios() {
        console.log('🔄 [CARGAR MÁS] Iniciando cargarMasCuestionarios...');
        console.log(`🔄 [CARGAR MÁS] Estado actual: cargando=${this.cargando}, paginaActual=${this.paginaActual}, totalPaginas=${this.totalPaginas}`);
        
        // Evitar cargar más si ya está cargando o si no hay más páginas
        if (this.cargando) {
            console.log('⚠️ [CARGAR MÁS] Ya está cargando, saliendo...');
            return;
        }
        if (this.paginaActual >= this.totalPaginas - 1) {
            console.log('⚠️ [CARGAR MÁS] No hay más páginas, saliendo...');
            return;
        }
        
        try {
            // Incrementar la página ANTES de marcar como cargando
            this.paginaActual++;
            console.log(`🔄 [CARGAR MÁS] Incrementada paginaActual a ${this.paginaActual}`);
            
            // Marcar como cargando y actualizar la UI
            console.log('🔄 [CARGAR MÁS] Marcando como cargando y actualizando UI...');
            this.cargando = true;
            this.actualizarPaginacion();
            
            // Verificar si hay filtros activos
            const estado = document.getElementById('filtro-estado-cuestionario')?.value || '';
            const tematica = document.getElementById('filtro-tematica-cuestionario')?.value || '';
            const busqueda = document.getElementById('buscar-cuestionario')?.value || '';
            
            console.log(`🔍 [CARGAR MÁS] Filtros activos: estado=${estado}, tematica=${tematica}, busqueda=${busqueda}`);
            
            // Realizar la solicitud directamente aquí en lugar de llamar a filtrarCuestionarios
            if (estado || tematica || busqueda) {
                console.log('🔄 [CARGAR MÁS] Realizando solicitud con filtros para página ' + this.paginaActual);
                
                // Construir parámetros de búsqueda
                const params = new URLSearchParams({
                    page: this.paginaActual,
                    size: this.tamanioPagina
                });
                
                // Añadir filtros si existen
                if (estado) params.append('estado', estado);
                if (tematica) params.append('tematica', tematica);
                if (busqueda) params.append('id', busqueda);
                
                console.log('🔄 [CARGAR MÁS] Parámetros:', params.toString());
                
                // Realizar la solicitud
                const response = await fetch(`/api/cuestionarios/filtrar?${params.toString()}`, {
                    headers: authManager.getAuthHeaders()
                });
                
                if (!response.ok) {
                    throw new Error(`Error al cargar más cuestionarios: ${response.status} ${response.statusText}`);
                }
                
                const data = await response.json();
                console.log('🔄 [CARGAR MÁS] Respuesta recibida:', data);
                
                // Añadir los nuevos cuestionarios a los existentes
                if (data.cuestionarios && data.cuestionarios.length > 0) {
                    console.log(`🔄 [CARGAR MÁS] Añadiendo ${data.cuestionarios.length} cuestionarios a los ${this.cuestionarios.length} existentes`);
                    this.cuestionarios = [...this.cuestionarios, ...data.cuestionarios];
                    
                    // Actualizar datos de paginación
                    this.totalCuestionarios = data.totalItems || this.totalCuestionarios;
                    this.totalPaginas = data.totalPages || this.totalPaginas;
                    
                    // Mostrar los cuestionarios
                    await this.mostrarCuestionarios(this.cuestionarios);
                } else {
                    console.log('⚠️ [CARGAR MÁS] No se recibieron nuevos cuestionarios');
                }
            } else {
                console.log('🔄 [CARGAR MÁS] Usando cargarCuestionarios con resetear=false');
                // Si no hay filtros, usar la carga normal
                await this.cargarCuestionarios(false);
            }
            
            console.log(`✅ [CARGAR MÁS] Completado. Cuestionarios cargados: ${this.cuestionarios.length}`);
        } catch (error) {
            console.error('❌ [CARGAR MÁS] Error:', error);
            Toastify({
                text: 'Error al cargar más cuestionarios: ' + error.message,
                duration: 3000,
                close: true,
                gravity: "top",
                position: "right",
                style: { background: "linear-gradient(to right, #ff0000, #cc0000)" }
            }).showToast();
            
            // Revertir el incremento de página si hay error
            this.paginaActual = Math.max(0, this.paginaActual - 1);
        } finally {
            // Asegurar que siempre se marque como no cargando
            console.log('🔄 [CARGAR MÁS] Finalizando, marcando como no cargando...');
            this.cargando = false;
            this.actualizarPaginacion();
        }
    },
    
    mostrarEstadoCarga() {
        const tbody = document.getElementById('tabla-cuestionarios');
        if (!tbody) return;
        
        if (this.cargando && this.cuestionarios.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center"><div class="spinner-border text-primary" role="status"><span class="visually-hidden">Cargando...</span></div><p class="mt-2">Cargando cuestionarios...</p></td></tr>';
        }
    },
    
    actualizarPaginacion() {
        const infoElement = document.getElementById('info-paginacion-cuestionarios');
        if (infoElement) {
            const inicio = (this.paginaActual * this.tamanioPagina) + 1;
            const fin = Math.min((this.paginaActual + 1) * this.tamanioPagina, this.totalCuestionarios);
            infoElement.textContent = `Mostrando ${inicio}-${fin} de ${this.totalCuestionarios} cuestionarios`;
        }

        const paginacionElement = document.getElementById('paginacion-cuestionarios');
        if (!paginacionElement) return;

        paginacionElement.innerHTML = '';
        if (this.totalPaginas <= 1) return;

        // Primera
        const primera = document.createElement('li');
        primera.className = `page-item ${this.paginaActual === 0 ? 'disabled' : ''}`;
        primera.innerHTML = `<a class="page-link" href="#" onclick="CuestionariosManager.irAPagina(0);return false;">Primera</a>`;
        paginacionElement.appendChild(primera);

        // Anterior
        const anterior = document.createElement('li');
        anterior.className = `page-item ${this.paginaActual === 0 ? 'disabled' : ''}`;
        anterior.innerHTML = `<a class="page-link" href="#" onclick="CuestionariosManager.irAPagina(${this.paginaActual - 1});return false;">Anterior</a>`;
        paginacionElement.appendChild(anterior);

        // Rango de páginas
        const inicio = Math.max(0, this.paginaActual - 2);
        const fin = Math.min(this.totalPaginas - 1, this.paginaActual + 2);
        for (let i = inicio; i <= fin; i++) {
            const li = document.createElement('li');
            li.className = `page-item ${i === this.paginaActual ? 'active' : ''}`;
            li.innerHTML = `<a class="page-link" href="#" onclick="CuestionariosManager.irAPagina(${i});return false;">${i + 1}</a>`;
            paginacionElement.appendChild(li);
        }

        // Siguiente
        const siguiente = document.createElement('li');
        siguiente.className = `page-item ${this.paginaActual >= this.totalPaginas - 1 ? 'disabled' : ''}`;
        siguiente.innerHTML = `<a class="page-link" href="#" onclick="CuestionariosManager.irAPagina(${this.paginaActual + 1});return false;">Siguiente</a>`;
        paginacionElement.appendChild(siguiente);

        // Última
        const ultima = document.createElement('li');
        ultima.className = `page-item ${this.paginaActual >= this.totalPaginas - 1 ? 'disabled' : ''}`;
        ultima.innerHTML = `<a class="page-link" href="#" onclick="CuestionariosManager.irAPagina(${this.totalPaginas - 1});return false;">Última</a>`;
        paginacionElement.appendChild(ultima);
    },

    async irAPagina(pagina) {
        if (pagina < 0 || pagina >= this.totalPaginas || pagina === this.paginaActual || this.cargando) return;
        this.paginaActual = pagina;

        // Detectar filtros activos
        const estado = document.getElementById('filtro-estado-cuestionario')?.value || '';
        const tematica = document.getElementById('filtro-tematica-cuestionario')?.value || '';
        const busqueda = document.getElementById('buscar-cuestionario')?.value || '';
        const hayFiltros = !!(estado || tematica || busqueda);

        if (hayFiltros) {
            // Reemplazar contenido, manteniendo la página actual
            this.cuestionarios = [];
            await window.filtrarCuestionarios(false);
        } else {
            // Reemplazar contenido de la página actual (no resetear a 0)
            await this.cargarCuestionarios(true, true);
        }
    },

    // (sin uso)
    
    
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
                
                // Llenar el filtro de temáticas
                const filtroTematica = document.getElementById('filtro-tematica-cuestionario');
                if (filtroTematica) {
                    const valorSeleccionado = filtroTematica.value || '';
                    // Mantener la primera opción "Todas"
                    filtroTematica.innerHTML = '<option value="">Todas</option>';
                    
                    // Añadir las temáticas
                    tematicasGestionadas.forEach(tematica => {
                        const option = document.createElement('option');
                        option.value = tematica;
                        option.textContent = tematica;
                        filtroTematica.appendChild(option);
                    });

                    // Restaurar selección previa si existe
                    if (valorSeleccionado) {
                        filtroTematica.value = valorSeleccionado;
                        // Si no existe en la lista (temática no gestionada), mantener "Todas"
                        if (filtroTematica.value !== valorSeleccionado) {
                            filtroTematica.value = '';
                        }
                    }
                }
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
            tr.classList.add('fila-cuestionario');
            
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
                <td class="celda-numero-cuestionario">${c.id ?? ''}</td>
                <td>
                    <select class="form-select form-select-sm" onchange="cambiarTematicaCuestionario(${c.id}, this.value)">
                        ${opcionesTematicas}
                    </select>
                </td>
                <td>
                    ${c.jornadaAsignada ? `<div class="text-muted">Asignado a jornada ${c.jornadaAsignada}</div>` :
                    `<select class="form-select form-select-sm" onchange="cambiarEstadoCuestionario(${c.id}, this.value)">
                        ${getOpcionesEstadoCuestionario(c.estado)}
                    </select>`}
                </td>
                <td>${(c.preguntas && c.preguntas.length) || 0}</td>
                <td>${c.fechaCreacion ? Utils.formatearFecha(String(c.fechaCreacion)) : ''}</td>
                <td>
                    <button class="btn btn-sm btn-primary me-1" onclick="editarCuestionario(${c.id})" title="Editar cuestionario">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="eliminarCuestionario(${c.id})" title="Eliminar cuestionario">
                        <i class="fas fa-trash"></i>
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
                        <td>${p.datosExtra ?? ''}</td>
                        <td><button class='btn btn-sm btn-danger' onclick='event.stopPropagation();eliminarPreguntaDeCuestionario(${c.id}, "${slotNivel}")'><i class='fas fa-trash'></i></button></td>
                    </tr>`;
                } else {
                    // Fila vacía con botón añadir
                    filasPreguntas += `<tr data-nivel="${slotNivel}">
                        <td><span class='${CuestionariosManager.getNivelColor ? CuestionariosManager.getNivelColor(slotNivel) : ''}'>${slotNivel}</span></td>
                        <td class="text-center text-muted">(Vacío)</td>
                        <td class="text-center text-muted">-</td>
                        <td class="text-center text-muted">-</td>
                        <td><button class='btn btn-sm btn-success' onclick='event.stopPropagation();anadirPreguntaACuestionario(${c.id}, "${slotNivel}")'><i class='fas fa-plus'></i></button></td>
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
                                <th>Datos extra</th>
                                <th>Acción</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${filasPreguntas}
                        </tbody>
                    </table>
                </div>
                ${puedeEditarNotas ? `
                <div class="mt-3">
                    <label class="form-label fw-bold">Añadir notas</label>
                    <textarea class="form-control" rows="2" placeholder="Añadir notas" 
                              onblur="actualizarNotasDireccion(${c.id}, this.value)">${c.notasDireccion || ''}</textarea>
                </div>` : ''}
            </td>`;
            tbody.appendChild(subtr);
            // Añadir separador de espacio entre cuestionarios
            const sep = document.createElement('tr');
            sep.classList.add('separador-cuestionario');
            sep.innerHTML = '<td colspan="6"></td>';
            tbody.appendChild(sep);
            // Añadir evento de click a filas con pregunta para redirigir
            setTimeout(() => {
                const filas = subtr.querySelectorAll('tbody tr[data-id]');
                filas.forEach(fila => {
                    fila.addEventListener('click', function() {
                        const id = this.getAttribute('data-id');
                        if (id) window.open(`preguntas.html?id=${id}`, '_blank');
                    });
                });
            }, 0);
        });
        // Delegación para enlaces de preguntas
        tbody.querySelectorAll('.enlace-pregunta').forEach(a => {
            a.addEventListener('click', function(e) {
                e.preventDefault();
                const id = this.dataset.id;
                window.open(`preguntas.html?id=${id}`, '_blank');
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
        // Restaurar posición/foco
        setTimeout(() => CuestionariosManager.restoreScrollOrFocus(), 0);
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
    
    // Reiniciar estado/jornada asignada
    const estadoSelect = document.getElementById('cuestionario-estado');
    if (estadoSelect) {
        estadoSelect.disabled = false;
    }
    const asignadoDiv = document.getElementById('cuestionario-jornada-asignada');
    if (asignadoDiv) asignadoDiv.classList.add('d-none');

    // Mostrar modal
    const modal = new bootstrap.Modal(document.getElementById('modal-cuestionario'));
    modal.show();
}

// Añadir eventos reactivos a los inputs del modal de búsqueda de preguntas
function inicializarBuscadorPreguntasModal() {
    const cargarTematicas = async () => {
        try {
            const sel = document.getElementById('buscador-tematica-select');
            if (!sel) return;
            // cargar temáticas de preguntas (distintas)
            const resp = await fetch('/api/preguntas/tematicas', { headers: authManager.getAuthHeaders() });
            if (!resp.ok) return;
            const tematicas = await resp.json();
            sel.innerHTML = '<option value="">Todas las temáticas</option>';
            (Array.isArray(tematicas) ? tematicas : []).forEach(t => {
                const nombre = typeof t === 'string' ? t : t?.nombre;
                if (!nombre) return;
                const opt = document.createElement('option');
                opt.value = nombre;
                opt.textContent = nombre;
                sel.appendChild(opt);
            });
        } catch {}
    };
    cargarTematicas();

    ['buscador-id', 'buscador-texto', 'buscador-tematica-select'].forEach(id => {
        const input = document.getElementById(id);
        if (input) {
            input.removeEventListener('keyup', input._buscadorHandler || (()=>{}));
            const handler = () => buscarPreguntasModal(0);
            input._buscadorHandler = handler;
            if (input.tagName === 'SELECT') {
                input.removeEventListener('change', input._buscadorHandler || (()=>{}));
                input.addEventListener('change', handler);
            } else {
                input.addEventListener('keyup', handler);
                if (id === 'buscador-id') {
                    input.removeEventListener('change', input._buscadorHandlerChange || (()=>{}));
                    input._buscadorHandlerChange = handler;
                    input.addEventListener('change', handler);
                }
            }
        }
    });
}

function abrirSelectorPregunta(nivel, factor = null) {
    selectorPreguntaContext.nivel = nivel;
    selectorPreguntaContext.factor = factor;
    selectorPreguntaContext.inputId = `pregunta-${nivel}`;
    selectorPreguntaContext.textoId = `pregunta-${nivel}-texto`;
    const idInput = document.getElementById('buscador-id');
    if (idInput) idInput.value = '';
    const textoInput = document.getElementById('buscador-texto');
    if (textoInput) textoInput.value = '';
    const temaSelect = document.getElementById('buscador-tematica-select');
    if (temaSelect) temaSelect.value = '';
    inicializarBuscadorPreguntasModal();
    buscarPreguntasModal(0);
    const modal = new bootstrap.Modal(document.getElementById('modal-selector-pregunta'));
    modal.show();
}

// Búsqueda en modal de preguntas (para cuestionarios)
async function buscarPreguntasModal(page = 0) {
    const id = (document.getElementById('buscador-id')?.value || '').trim();
    const texto = (document.getElementById('buscador-texto')?.value || '').trim();
    const tematica = document.getElementById('buscador-tematica-select')?.value || '';

    try {
        const nivel = selectorPreguntaContext.nivel;
        const params = new URLSearchParams();
        let url = '';
        if (normales.includes(nivel)) params.set('nivel', `_${nivel}`);
        params.set('page', page);
        params.set('size', 20);

        if (id) {
            // Búsqueda exacta por ID
            params.set('id', id);
            if (tematica) params.set('tematica', tematica);
            url = `/api/preguntas/buscar?${params.toString()}`;
        } else {
            if (texto) params.set('texto', texto); // OR pregunta/respuesta
            if (tematica) params.set('tematica', tematica);
            // Solo aprobadas para evitar errores al guardar
            params.set('estado', 'aprobada');
            url = `/api/preguntas/filtrar?${params.toString()}`;
        }

        console.log('[FRONT][CUEST] URL de búsqueda:', url);
        const resp = await fetch(url, { headers: authManager.getAuthHeaders() });
        if (!resp.ok) throw new Error('Error al buscar preguntas');
        const data = await resp.json();
        let preguntas = data.content || [];
        // Si entramos por ID, filtrar a aprobadas para coherencia
        if (id) preguntas = preguntas.filter(p => p.estado === 'aprobada');

        console.log('[FRONT][CUEST] Preguntas encontradas:', preguntas.length);
        renderPreguntasModal(preguntas, page, data.totalPages || 1);
    } catch (e) {
        console.error('Error en buscarPreguntasModal:', e);
        const tbody = document.getElementById('tbody-selector-pregunta');
        if (tbody) tbody.innerHTML = `<tr><td colspan="6">Error al cargar preguntas: ${e.message}</td></tr>`;
        const pag = document.getElementById('paginacion-selector-pregunta');
        if (pag) pag.innerHTML = '';
    }
}

function renderPreguntasModal(preguntas, currentPage, totalPages) {
    const tbody = document.getElementById('tbody-selector-pregunta');
    if (!tbody) return;

    tbody.innerHTML = '';
    if (!preguntas || preguntas.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center">No se encontraron preguntas</td></tr>';
        const pag = document.getElementById('paginacion-selector-pregunta');
        if (pag) pag.innerHTML = '';
        return;
    }

    preguntas.forEach(pregunta => {
        const row = document.createElement('tr');
        // Determinar color del nivel (coherente con combos)
        let nivelColor = '';
        if (pregunta.nivel === '_5NLS' || (pregunta.nivel && pregunta.nivel.includes('NLS'))) {
            nivelColor = 'text-danger fw-bold';
        } else if (pregunta.nivel === '_5LS' || (pregunta.nivel && pregunta.nivel.includes('LS'))) {
            nivelColor = 'text-success fw-bold';
        } else {
            nivelColor = 'text-muted';
        }

        row.innerHTML = `
            <td>${pregunta.id}</td>
            <td colspan="4">
                <div class="pregunta-item">
                    <div class="pregunta-texto">${pregunta.pregunta}</div>
                    <div class="respuesta-texto"><strong>Respuesta:</strong> ${pregunta.respuesta}</div>
                    <div class="pregunta-meta">
                        <small class="text-muted">
                            Temática: ${pregunta.tematica || 'N/A'} | 
                            Nivel: <span class="${nivelColor}">${pregunta.nivel || '-'}</span> | 
                            Estado: ${pregunta.estado || '-'}
                        </small>
                    </div>
                </div>
            </td>
        `;

        row.style.cursor = 'pointer';
        row.addEventListener('click', () => {
            seleccionarPreguntaModal(
                pregunta.id,
                pregunta.pregunta,
                pregunta.tematica,
                pregunta.respuesta,
                pregunta.subtema
            );
        });

        tbody.appendChild(row);
    });

    // Renderizar paginación como en combos
    const paginacion = document.getElementById('paginacion-selector-pregunta');
    if (paginacion) {
        let paginacionHTML = '';
        for (let i = 0; i < totalPages; i++) {
            const activeClass = i === currentPage ? 'active' : '';
            paginacionHTML += `
                <li class="page-item ${activeClass}">
                    <a class="page-link" href="#" onclick="buscarPreguntasModal(${i})">${i + 1}</a>
                </li>
            `;
        }
        paginacion.innerHTML = paginacionHTML;
    }
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
        else if (nivel === 'PM3') factorMultiplicacion = 1;

        const doAdd = async () => {
            await apiManager.post(`/api/cuestionarios/${cuestionarioId}/preguntas`, { preguntaId: id, factorMultiplicacion }, { headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' } });
            await CuestionariosManager.cargarCuestionarios();
        };
        const undoDelete = async () => {
            await apiManager.delete(`/api/cuestionarios/${cuestionarioId}/preguntas/${id}`, { headers: authManager.getAuthHeaders() });
            await CuestionariosManager.cargarCuestionarios();
        };

        doAdd()
        .then(() => {
            if (window.UndoManager) {
                window.UndoManager.record({ do: doAdd, undo: undoDelete, label: `Añadir pregunta ${id} a cuestionario ${cuestionarioId}` });
            }
            Toastify({ text: 'Pregunta añadida al cuestionario', duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' } }).showToast();
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
    CuestionariosManager.rememberScroll();
    let preguntasNormales = [];
    normales.forEach(nivel => {
        const element = document.getElementById(`pregunta-${nivel}`);
        const id = element ? element.value : '';
        if (id) preguntasNormales.push(Number(id));
    });
    
    console.log('🔍 [FRONTEND] Preguntas seleccionadas:', preguntasNormales);
    
    // Cambiar validación: permitir al menos 1 pregunta en lugar de requerir todas las 4
    if (preguntasNormales.length === 0) {
        Toastify({
            text: 'Debes seleccionar al menos 1 pregunta para crear el cuestionario',
            duration: 3000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
        }).showToast();
        return;
    }
    
    const cuestionarioIdElement = document.getElementById('cuestionario-id');
    const tematicaElement = document.getElementById('cuestionario-tematica');
    const estadoElement = document.getElementById('cuestionario-estado');
    const notasElement = document.getElementById('cuestionario-notas');
    
    const cuestionarioId = cuestionarioIdElement ? cuestionarioIdElement.value : '';
    const tematica = tematicaElement ? tematicaElement.value : '';
    const estadoSeleccionado = estadoElement ? estadoElement.value : '';
    const notasDireccion = notasElement ? notasElement.value : '';
    const esEdicion = !!cuestionarioId;
    
    console.log('🔍 [FRONTEND] Datos del formulario:', { 
        cuestionarioId, 
        tematica, 
        notasDireccion, 
        esEdicion 
    });
    
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
    
    console.log('📤 [FRONTEND] Enviando payload:', payload);
    
    try {
        let resp, data;
        if (esEdicion) {
            // PUT para editar
            console.log(`📤 [FRONTEND] Enviando PUT a /api/cuestionarios/${cuestionarioId}`);
            resp = await fetch(`/api/cuestionarios/${cuestionarioId}`, {
                method: 'PUT',
                headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        } else {
            // POST para crear
            console.log('📤 [FRONTEND] Enviando POST a /api/cuestionarios/nuevo');
            resp = await fetch('/api/cuestionarios/nuevo', {
                method: 'POST',
                headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        }
        
        console.log(`📥 [FRONTEND] Respuesta recibida: ${resp.status} ${resp.statusText}`);
        
        try { 
            data = await resp.json(); 
            console.log('📥 [FRONTEND] Datos recibidos:', data);
        } catch (e) { 
            console.error('❌ [FRONTEND] Error al parsear JSON:', e);
            data = null; 
        }
        
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
        // Si el usuario seleccionó un estado distinto a borrador, aplicar cambio de estado
        try {
            if (estadoSeleccionado && estadoSeleccionado !== 'borrador') {
                const idFinal = (data && (data.id || data.ID)) || (cuestionarioId || null);
                if (idFinal) {
                    await fetch(`/api/cuestionarios/${idFinal}/estado?nuevoEstado=${encodeURIComponent(estadoSeleccionado)}`, {
                        method: 'PUT',
                        headers: authManager.getAuthHeaders()
                    });
                }
            }
        } catch (e) {
            console.warn('No se pudo aplicar el estado seleccionado tras guardar:', e);
        }
        await CuestionariosManager.cargarCuestionarios();
        CuestionariosManager.restoreScrollOrFocus();
    } catch (error) {
        console.error('❌ [FRONTEND] Error al guardar:', error);
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
        CuestionariosManager.lastFocusCuestionarioId = id;
        const resp = await fetch(`/api/cuestionarios/${id}`, { headers: authManager.getAuthHeaders() });
        if (!resp.ok) throw new Error('No se pudo cargar el cuestionario');
        const cuestionario = await resp.json();
        
        // Cargar temáticas gestionadas y poblar el select antes de asignar valor
        try {
            const respTem = await fetch('/api/cuestionarios/tematicas', { headers: authManager.getAuthHeaders() });
            if (respTem.ok) {
                const tematicas = await respTem.json();
                const tematicaSelect = document.getElementById('cuestionario-tematica');
                if (tematicaSelect) {
                    tematicaSelect.innerHTML = '<option value="">Sin temática</option>';
                    tematicas.forEach(t => {
                        const opt = document.createElement('option');
                        opt.value = t;
                        opt.textContent = t;
                        tematicaSelect.appendChild(opt);
                    });
                }
            }
        } catch (e) { console.warn('No se pudieron cargar temáticas gestionadas:', e); }

        // Precargar preguntas normales y PM en el modal
        const normalesIds = ['1LS','2NLS','3LS','4NLS'];
        const pmsIds = ['PM1','PM2','PM3'];
        
        // Limpiar primero - con verificación de existencia
        normalesIds.forEach(nivel => {
            const preguntaElement = document.getElementById(`pregunta-${nivel}`);
            const textoElement = document.getElementById(`pregunta-${nivel}-texto`);
            if (preguntaElement) preguntaElement.value = '';
            if (textoElement) textoElement.value = '';
        });
        pmsIds.forEach(pm => {
            const pmElement = document.getElementById(`pm-${pm}`);
            const pmTextoElement = document.getElementById(`pm-${pm}-texto`);
            if (pmElement) pmElement.value = '';
            if (pmTextoElement) pmTextoElement.value = '';
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
                        // PM - con verificación de existencia
                        const pmElement = document.getElementById(`pm-${ordenNivel[nivel]}`);
                        const pmTextoElement = document.getElementById(`pm-${ordenNivel[nivel]}-texto`);
                        if (pmElement) pmElement.value = p.id;
                        if (pmTextoElement) pmTextoElement.value = `${p.pregunta} [${p.tematica}] (${p.respuesta})`;
                    } else {
                        // Normal - con verificación de existencia
                        const preguntaElement = document.getElementById(`pregunta-${ordenNivel[nivel]}`);
                        const textoElement = document.getElementById(`pregunta-${ordenNivel[nivel]}-texto`);
                        if (preguntaElement) preguntaElement.value = p.id;
                        if (textoElement) textoElement.value = `${p.pregunta} [${p.tematica}] (${p.respuesta})`;
                    }
                }
            });
        }
        
        // Asignar ID del cuestionario con verificación
        const cuestionarioIdElement = document.getElementById('cuestionario-id');
        if (cuestionarioIdElement) cuestionarioIdElement.value = cuestionario.id;
        
        // Asignar estado actual al selector si existe
        const estadoElement = document.getElementById('cuestionario-estado');
        if (estadoElement) {
            estadoElement.value = (cuestionario.estado || 'borrador');
            if (cuestionario.jornadaAsignada) {
                estadoElement.disabled = true;
            } else {
                estadoElement.disabled = false;
            }
        }

        // Mostrar info de jornada asignada en el modal
        const asignadoDiv = document.getElementById('cuestionario-jornada-asignada');
        if (asignadoDiv) {
            if (cuestionario.jornadaAsignada) {
                asignadoDiv.textContent = `Asignado a jornada ${cuestionario.jornadaAsignada}`;
                asignadoDiv.classList.remove('d-none');
            } else {
                asignadoDiv.classList.add('d-none');
            }
        }

        // Asignar temática actual si existe
        const tematicaSelect = document.getElementById('cuestionario-tematica');
        if (tematicaSelect) tematicaSelect.value = (cuestionario.tematica || '');

        // Cambiar título del modal con verificación
        const tituloElement = document.getElementById('modal-cuestionario-titulo');
        if (tituloElement) tituloElement.innerText = 'Editar Cuestionario';
        
        // Mostrar modal
        const modalElement = document.getElementById('modal-cuestionario');
        if (modalElement) {
            const modal = new bootstrap.Modal(modalElement);
            modal.show();
        }
    } catch (e) {
        console.error('Error en editarCuestionario:', e);
        Toastify({ 
            text: 'Error al cargar cuestionario: ' + e.message, 
            duration: 3000, 
            close: true, 
            gravity: 'top', 
            position: 'right', 
            style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' } 
        }).showToast();
    }
};

window.eliminarCuestionario = async function(id) {
    if (!confirm('¿Seguro que quieres eliminar este cuestionario? Esta acción no se puede deshacer.')) return;
    try {
        CuestionariosManager.rememberScroll();
        const resp = await fetch(`/api/cuestionarios/${id}`, { method: 'DELETE', headers: authManager.getAuthHeaders() });
        
        if (!resp.ok) {
            let errorMessage = 'No se pudo eliminar el cuestionario';
            
            // Intentar obtener el mensaje de error específico del servidor
            try {
                const errorData = await resp.json();
                if (errorData && errorData.mensaje) {
                    errorMessage = errorData.mensaje;
                } else if (errorData && errorData.message) {
                    errorMessage = errorData.message;
                }
            } catch (parseError) {
                // Si no se puede parsear como JSON, intentar obtener el texto
                try {
                    const errorText = await resp.text();
                    if (errorText) {
                        errorMessage = errorText;
                    }
                } catch (textError) {
                    // Si todo falla, usar el mensaje por defecto
                    console.error('Error al parsear respuesta del servidor:', textError);
                }
            }
            
            throw new Error(errorMessage);
        }
        
        Toastify({ text: 'Cuestionario eliminado', duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' } }).showToast();
        await CuestionariosManager.cargarCuestionarios();
        CuestionariosManager.restoreScrollOrFocus();
    } catch (e) {
        console.error('Error al eliminar cuestionario:', e);
        Toastify({ text: 'Error al eliminar cuestionario: ' + e.message, duration: 5000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' } }).showToast();
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
        // Preparar undo/redo: localizar pregunta en ese slot
        const cuest = CuestionariosManager.ultimoListado?.find(c => c.id === cuestionarioId);
        let preguntaId = null;
        if (cuest && Array.isArray(cuest.preguntas)) {
            const pc = cuest.preguntas.find(p => p.slot === slot);
            if (pc && pc.pregunta && pc.pregunta.id) preguntaId = pc.pregunta.id;
        }
        const doDelete = async () => {
            await apiManager.delete(`/api/cuestionarios/${cuestionarioId}/preguntas/slot/${slot}`, { headers: authManager.getAuthHeaders() });
            await CuestionariosManager.cargarCuestionarios();
        };
        const undoAdd = async () => {
            if (preguntaId) {
                await apiManager.post(`/api/cuestionarios/${cuestionarioId}/preguntas`, { preguntaId, factorMultiplicacion: 1 }, { headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' } });
                await CuestionariosManager.cargarCuestionarios();
            }
        };
        await doDelete();
        if (window.UndoManager) {
            window.UndoManager.record({ do: doDelete, undo: undoAdd, label: `Quitar pregunta slot ${slot} de cuestionario ${cuestionarioId}` });
        }
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
window.filtrarCuestionarios = async function(resetear = true) {
    console.log(`🔍 [FILTRAR] Iniciando filtrarCuestionarios con resetear=${resetear}`);
    console.log(`🔍 [FILTRAR] Estado actual: cargando=${CuestionariosManager.cargando}, paginaActual=${CuestionariosManager.paginaActual}, totalPaginas=${CuestionariosManager.totalPaginas}`);
    
    // No verificamos si está cargando cuando viene de cargarMasCuestionarios (resetear=false)
    // porque cargarMasCuestionarios ya establece cargando=true antes de llamar a esta función
    
    try {
        const estado = document.getElementById('filtro-estado-cuestionario')?.value || '';
        const tematica = document.getElementById('filtro-tematica-cuestionario')?.value || '';
        const subtema = document.getElementById('filtro-subtema-cuestionario')?.value || '';
        const busqueda = document.getElementById('buscar-cuestionario')?.value || '';
        
        console.log(`🔍 [FILTRAR] Filtros: estado=${estado}, tematica=${tematica}, subtema=${subtema}, busqueda=${busqueda}`);

        // Resetear paginación si es una nueva búsqueda
        if (resetear) {
            console.log('🔍 [FILTRAR] Reseteando paginación y cuestionarios');
            CuestionariosManager.paginaActual = 0;
            CuestionariosManager.cuestionarios = [];
        }
        
        // Marcar como cargando y actualizar la UI
        console.log('🔍 [FILTRAR] Marcando como cargando y actualizando UI');
        CuestionariosManager.cargando = true;
        CuestionariosManager.mostrarEstadoCarga();
        CuestionariosManager.actualizarPaginacion();
        
        // Construir parámetros de búsqueda
        const params = new URLSearchParams({
            page: CuestionariosManager.paginaActual,
            size: CuestionariosManager.tamanioPagina
        });
        
        // Añadir filtros si existen
        if (estado) params.append('estado', estado);
        if (tematica) params.append('tematica', tematica);
        if (subtema) params.append('subtema', subtema);
        if (busqueda) params.append('id', busqueda);

        console.log('🔍 [FILTRAR] Parámetros de búsqueda:', params.toString());

        // Realizar la búsqueda con filtros y paginación
        console.log(`🔍 [FILTRAR] Enviando solicitud a /api/cuestionarios/filtrar?${params.toString()}`);
        const response = await fetch(`/api/cuestionarios/filtrar?${params.toString()}`, {
            headers: authManager.getAuthHeaders()
        });

        if (!response.ok) {
            console.error(`❌ [FILTRAR] Error en respuesta: ${response.status} ${response.statusText}`);
            throw new Error(`Error al filtrar cuestionarios: ${response.status} ${response.statusText}`);
        }
        
        const data = await response.json();
        console.log('🔍 [FILTRAR] Respuesta recibida:', data);
        console.log(`🔍 [FILTRAR] Datos de paginación: currentPage=${data.currentPage}, totalItems=${data.totalItems}, totalPages=${data.totalPages}`);
        console.log(`🔍 [FILTRAR] Cuestionarios recibidos: ${data.cuestionarios ? data.cuestionarios.length : 0}`);
        
        // Actualizar datos de paginación
        if (resetear) {
            console.log('🔍 [FILTRAR] Reemplazando cuestionarios existentes');
            CuestionariosManager.cuestionarios = data.cuestionarios || [];
        } else {
            console.log(`🔍 [FILTRAR] Añadiendo ${data.cuestionarios ? data.cuestionarios.length : 0} cuestionarios a los ${CuestionariosManager.cuestionarios.length} existentes`);
            CuestionariosManager.cuestionarios = [...CuestionariosManager.cuestionarios, ...(data.cuestionarios || [])];
        }
        
        CuestionariosManager.totalCuestionarios = data.totalItems || 0;
        CuestionariosManager.totalPaginas = data.totalPages || 0;
        CuestionariosManager.paginaActual = data.currentPage || 0;
        
        console.log(`🔍 [FILTRAR] Estado actualizado: totalCuestionarios=${CuestionariosManager.totalCuestionarios}, totalPaginas=${CuestionariosManager.totalPaginas}, paginaActual=${CuestionariosManager.paginaActual}`);
        
        // Mostrar cuestionarios y actualizar paginación
        console.log(`🔍 [FILTRAR] Mostrando ${CuestionariosManager.cuestionarios.length} cuestionarios`);
        await CuestionariosManager.mostrarCuestionarios(CuestionariosManager.cuestionarios);
        console.log('✅ [FILTRAR] Cuestionarios mostrados correctamente');
    } catch (error) {
        console.error('❌ [FILTRAR] Error:', error);
        Toastify({
            text: 'Error al filtrar cuestionarios: ' + error.message,
            duration: 3000,
            close: true,
            gravity: "top",
            position: "right",
            style: { background: "linear-gradient(to right, #ff0000, #cc0000)" }
        }).showToast();
    } finally {
        // Asegurar que siempre se marque como no cargando y se actualice la UI
        console.log('🔍 [FILTRAR] Finalizando, marcando como no cargando');
        CuestionariosManager.cargando = false;
        CuestionariosManager.actualizarPaginacion();
    }
};

window.limpiarFiltrosCuestionarios = function() {
    document.getElementById('filtro-estado-cuestionario').value = '';
    document.getElementById('filtro-tematica-cuestionario').value = '';
    document.getElementById('buscar-cuestionario').value = '';
    CuestionariosManager.paginaActual = 0;
    CuestionariosManager.cargarCuestionarios(true);
};

window.actualizarNotasDireccion = async function(cuestionarioId, notas) {
    try {
        // Obtener valor anterior desde el listado
        const previo = (CuestionariosManager.ultimoListado || []).find(c => c.id === cuestionarioId);
        const notasPrevias = previo ? (previo.notasDireccion || '') : '';
        const doAction = async () => {
            await apiManager.put(`/api/cuestionarios/${cuestionarioId}/notas-direccion`, { notasDireccion: notas });
            await CuestionariosManager.cargarCuestionarios();
        };
        const undoAction = async () => {
            await apiManager.put(`/api/cuestionarios/${cuestionarioId}/notas-direccion`, { notasDireccion: notasPrevias });
            await CuestionariosManager.cargarCuestionarios();
        };
        await doAction();
        if (window.UndoManager) window.UndoManager.record({ do: doAction, undo: undoAction, label: `Notas dirección cuestionario ${cuestionarioId}` });
        
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

function getOpcionesEstadoCuestionario(estadoActual) {
    // Definimos las transiciones permitidas para cada estado
    const transiciones = {
        'borrador': ['revisar'],
        'revisar': ['corregir', 'aprobado'],
        'corregir': ['revisar'],
        'aprobado': [], // Solo cambia automáticamente a adjudicado al asignarse a una jornada
        'adjudicado': [], // Solo cambia automáticamente a grabado al asignarse a un concursante
        'grabado': []
    };
    
    // Obtenemos las opciones disponibles según el estado actual
    const opcionesDisponibles = transiciones[estadoActual] || [];
    
    // Siempre incluimos el estado actual como seleccionado
    let opciones = `<option value="${estadoActual}" selected>${estadoActual.charAt(0).toUpperCase() + estadoActual.slice(1)}</option>`;
    
    // Añadimos las opciones de transición permitidas
    opcionesDisponibles.forEach(estado => {
        opciones += `<option value="${estado}">${estado.charAt(0).toUpperCase() + estado.slice(1)}</option>`;
    });
    
    // Si el usuario es admin, permitimos todas las opciones
    if (authManager.hasRole('ROLE_ADMIN')) {
        const todosEstados = ['borrador', 'revisar', 'corregir', 'aprobado', 'adjudicado', 'grabado'];
        todosEstados.forEach(estado => {
            if (estado !== estadoActual && !opcionesDisponibles.includes(estado)) {
                opciones += `<option value="${estado}">${estado.charAt(0).toUpperCase() + estado.slice(1)}</option>`;
            }
        });
    }
    
    return opciones;
}

window.cambiarEstadoCuestionario = async function(id, nuevoEstado) {
    try {
        const previo = (CuestionariosManager.ultimoListado || []).find(c => c.id === id);
        const estadoPrevio = previo ? previo.estado : null;
        const doAction = async () => {
            await apiManager.put(`/api/cuestionarios/${id}/estado?nuevoEstado=${encodeURIComponent(nuevoEstado)}`, null);
            await CuestionariosManager.cargarCuestionarios();
        };
        const undoAction = async () => {
            if (estadoPrevio) {
                await apiManager.put(`/api/cuestionarios/${id}/estado?nuevoEstado=${encodeURIComponent(estadoPrevio)}`, null);
                await CuestionariosManager.cargarCuestionarios();
            }
        };
        await doAction();
        if (window.UndoManager) window.UndoManager.record({ do: doAction, undo: undoAction, label: `Estado cuestionario ${id}` });
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
        const previo = (CuestionariosManager.ultimoListado || []).find(c => c.id === id);
        const tematicaPrevia = previo ? (previo.tematica || '') : '';
        const doAction = async () => {
            await apiManager.put(`/api/cuestionarios/${id}/tematica`, { tematica: nuevaTematica });
            await CuestionariosManager.cargarCuestionarios();
        };
        const undoAction = async () => {
            await apiManager.put(`/api/cuestionarios/${id}/tematica`, { tematica: tematicaPrevia });
            await CuestionariosManager.cargarCuestionarios();
        };
        await doAction();
        if (window.UndoManager) window.UndoManager.record({ do: doAction, undo: undoAction, label: `Temática cuestionario ${id}` });
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