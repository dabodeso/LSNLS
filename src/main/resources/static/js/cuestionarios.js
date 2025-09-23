// Módulo de gestión de cuestionarios
const CuestionariosManager = {
    cuestionarios: [],
    paginaActual: 0,
    tamanioPagina: 25,
    totalCuestionarios: 0,
    totalPaginas: 0,
    cargando: false,
    
    async cargarCuestionarios(resetear = true) {
        try {
            if (!authManager.isAuthenticated()) {
                console.error('Usuario no autenticado');
                return;
            }
            
            if (resetear) {
                this.paginaActual = 0;
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
        console.log('📊 [PAGINACIÓN] Iniciando actualizarPaginacion...');
        console.log(`📊 [PAGINACIÓN] Estado actual: cargando=${this.cargando}, paginaActual=${this.paginaActual}, totalPaginas=${this.totalPaginas}, totalCuestionarios=${this.totalCuestionarios}, cuestionarios.length=${this.cuestionarios.length}`);
        
        let paginacionContainer = document.getElementById('paginacion-cuestionarios');
        if (!paginacionContainer) {
            console.log('📊 [PAGINACIÓN] Contenedor no existe, creando uno nuevo...');
            // Crear el contenedor si no existe
            const tablaContainer = document.querySelector('.table-responsive');
            if (tablaContainer) {
                const paginacionDiv = document.createElement('div');
                paginacionDiv.id = 'paginacion-cuestionarios';
                paginacionDiv.className = 'mt-3 d-flex justify-content-between align-items-center';
                tablaContainer.parentNode.insertBefore(paginacionDiv, tablaContainer.nextSibling);
                paginacionContainer = document.getElementById('paginacion-cuestionarios');
                console.log('📊 [PAGINACIÓN] Contenedor creado correctamente');
            } else {
                console.log('⚠️ [PAGINACIÓN] No se encontró el contenedor .table-responsive');
            }
        } else {
            console.log('📊 [PAGINACIÓN] Contenedor encontrado');
        }
        
        if (paginacionContainer) {
            // Limpiar el contenedor antes de añadir nuevos elementos
            console.log('📊 [PAGINACIÓN] Limpiando contenedor...');
            paginacionContainer.innerHTML = '';
            
            const infoPagina = document.createElement('div');
            const paginaActual = this.paginaActual + 1;
            const totalPaginas = Math.max(1, Math.ceil(this.totalPaginas));
            infoPagina.innerHTML = `Mostrando ${this.cuestionarios.length} de ${this.totalCuestionarios} cuestionarios (Página ${paginaActual} de ${totalPaginas})`;
            paginacionContainer.appendChild(infoPagina);
            console.log(`📊 [PAGINACIÓN] Información de página añadida: Página ${paginaActual} de ${totalPaginas}`);
            
            // Verificar si hay más páginas para cargar
            const hayMasPaginas = this.paginaActual < this.totalPaginas - 1;
            console.log(`📊 [PAGINACIÓN] ¿Hay más páginas? ${hayMasPaginas} (paginaActual=${this.paginaActual}, totalPaginas=${this.totalPaginas})`);
            
            if (hayMasPaginas) {
                console.log('📊 [PAGINACIÓN] Creando botón "Cargar más"...');
                const botonCargarMas = document.createElement('button');
                botonCargarMas.className = 'btn btn-primary';
                botonCargarMas.innerHTML = '<i class="fas fa-plus"></i> Cargar más cuestionarios';
                botonCargarMas.type = 'button';
                botonCargarMas.id = 'btn-cargar-mas-cuestionarios';
                
                // Deshabilitar el botón mientras se está cargando
                if (this.cargando) {
                    console.log('📊 [PAGINACIÓN] Deshabilitando botón (cargando=true)');
                    botonCargarMas.disabled = true;
                    botonCargarMas.style.opacity = '0.6';
                    botonCargarMas.style.cursor = 'not-allowed';
                    botonCargarMas.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Cargando...';
                } else {
                    console.log('📊 [PAGINACIÓN] Habilitando botón (cargando=false)');
                    botonCargarMas.disabled = false;
                }
                
                // Usar onclick en lugar de addEventListener para evitar duplicación de eventos
                botonCargarMas.onclick = (e) => {
                    console.log('🖱️ [PAGINACIÓN] Botón "Cargar más" clickeado');
                    e.preventDefault();
                    e.stopPropagation();
                    // Usar una referencia explícita al objeto CuestionariosManager
                    CuestionariosManager.cargarMasCuestionarios();
                };
                
                paginacionContainer.appendChild(botonCargarMas);
                console.log('📊 [PAGINACIÓN] Botón "Cargar más" añadido al DOM');
                
                // Verificar que el botón esté correctamente añadido y tenga el evento onclick
                setTimeout(() => {
                    const botonEnDOM = document.getElementById('btn-cargar-mas-cuestionarios');
                    if (botonEnDOM) {
                        console.log('✅ [PAGINACIÓN] Botón verificado en el DOM');
                        if (typeof botonEnDOM.onclick === 'function') {
                            console.log('✅ [PAGINACIÓN] El botón tiene un manejador de eventos onclick');
                        } else {
                            console.error('❌ [PAGINACIÓN] El botón NO tiene un manejador de eventos onclick');
                        }
                    } else {
                        console.error('❌ [PAGINACIÓN] No se pudo encontrar el botón en el DOM después de añadirlo');
                    }
                }, 100);
            } else if (this.cuestionarios.length > 0) {
                // Si no hay más páginas pero hay resultados, mostrar un mensaje
                console.log('📊 [PAGINACIÓN] No hay más páginas, mostrando mensaje de "No hay más resultados"');
                const noMasResultados = document.createElement('div');
                noMasResultados.className = 'text-muted';
                noMasResultados.textContent = 'No hay más resultados para mostrar';
                paginacionContainer.appendChild(noMasResultados);
            }
        } else {
            console.error('❌ [PAGINACIÓN] No se pudo encontrar ni crear el contenedor de paginación');
        }
        
        console.log('📊 [PAGINACIÓN] actualizarPaginacion completado');
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
                
                // Llenar el filtro de temáticas
                const filtroTematica = document.getElementById('filtro-tematica-cuestionario');
                if (filtroTematica) {
                    // Mantener la primera opción "Todas"
                    filtroTematica.innerHTML = '<option value="">Todas</option>';
                    
                    // Añadir las temáticas
                    tematicasGestionadas.forEach(tematica => {
                        const option = document.createElement('option');
                        option.value = tematica;
                        option.textContent = tematica;
                        filtroTematica.appendChild(option);
                    });
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
                <td style="font-weight: bold; font-size: 1.1em; color: #0066cc; padding-right: 20px;">${c.id ?? ''}</td>
                <td>
                    <select class="form-select form-select-sm" onchange="cambiarTematicaCuestionario(${c.id}, this.value)">
                        ${opcionesTematicas}
                    </select>
                </td>
                <td>
                    <select class="form-select form-select-sm" onchange="cambiarEstadoCuestionario(${c.id}, this.value)">
                        ${getOpcionesEstadoCuestionario(c.estado)}
                    </select>
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
    const id = document.getElementById('buscador-id').value.trim();
    const pregunta = document.getElementById('buscador-pregunta').value.trim();
    const respuesta = document.getElementById('buscador-respuesta').value.trim();
    const tematica = document.getElementById('buscador-tematica').value.trim();
    
    // Detectar si estamos en contexto de combo o cuestionario
    const esCombo = window.contextoAnadirPregunta && window.contextoAnadirPregunta.comboId;
    const esCuestionario = window.contextoAnadirPregunta && window.contextoAnadirPregunta.cuestionarioId;
    
    try {
        let preguntas = [];
        let totalPages = 1;
        
        if (esCombo) {
            // Para combos, buscar solo preguntas de nivel 5 (_5LS y _5NLS)
            const respLS = await fetch(`/api/preguntas/buscar?nivel=_5LS&page=${page}&size=20&id=${encodeURIComponent(id)}&pregunta=${encodeURIComponent(pregunta)}&respuesta=${encodeURIComponent(respuesta)}&tematica=${encodeURIComponent(tematica)}`, { headers: authManager.getAuthHeaders() });
            const respNLS = await fetch(`/api/preguntas/buscar?nivel=_5NLS&page=${page}&size=20&id=${encodeURIComponent(id)}&pregunta=${encodeURIComponent(pregunta)}&respuesta=${encodeURIComponent(respuesta)}&tematica=${encodeURIComponent(tematica)}`, { headers: authManager.getAuthHeaders() });
            const dataLS = await respLS.json();
            const dataNLS = await respNLS.json();
            preguntas = [...(dataLS.content || []), ...(dataNLS.content || [])];
            totalPages = Math.max(dataLS.totalPages || 1, dataNLS.totalPages || 1);
        } else {
            // Para cuestionarios, buscar según el nivel del contexto
            const nivel = selectorPreguntaContext.nivel;
            let url = '';
            if (normales.includes(nivel)) {
                url = `/api/preguntas/buscar?nivel=_${nivel}`;
            } else {
                url = `/api/preguntas/buscar?nivel=_5LS`;
            }
            if (id) url += `&id=${encodeURIComponent(id)}`;
            if (pregunta) url += `&pregunta=${encodeURIComponent(pregunta)}`;
            if (respuesta) url += `&respuesta=${encodeURIComponent(respuesta)}`;
            if (tematica) url += `&tematica=${encodeURIComponent(tematica)}`;
            url += `&page=${page}&size=20`;
            
            console.log('[FRONT] URL de búsqueda:', url);
            console.log('[FRONT] Contexto:', selectorPreguntaContext);
            
            const resp = await fetch(url, { headers: authManager.getAuthHeaders() });
            console.log('[FRONT] Respuesta del servidor:', resp.status, resp.statusText);
            
            if (!resp.ok) throw new Error('Error al buscar preguntas');
            const data = await resp.json();
            console.log('[FRONT] Datos recibidos:', data);
            
            preguntas = data.content || [];
            totalPages = data.totalPages || 1;
        }
        
        console.log('[FRONT] Preguntas encontradas:', preguntas.length);
        console.log('[FRONT] Total páginas:', totalPages);
        
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
        else if (nivel === 'PM3') factorMultiplicacion = 1; // Cambiado de 0 a 1
        fetch(`/api/cuestionarios/${cuestionarioId}/preguntas`, {
            method: 'POST',
            headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' },
            body: JSON.stringify({ preguntaId: id, factorMultiplicacion })
        })
        .then(resp => {
            if (!resp.ok) {
                return resp.text().then(text => {
                    console.error('Error del servidor:', text);
                    throw new Error(`Error ${resp.status}: ${text}`);
                });
            }
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
    const notasElement = document.getElementById('cuestionario-notas');
    
    const cuestionarioId = cuestionarioIdElement ? cuestionarioIdElement.value : '';
    const tematica = tematicaElement ? tematicaElement.value : '';
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
        await CuestionariosManager.cargarCuestionarios();
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
        const resp = await fetch(`/api/cuestionarios/${id}`, { headers: authManager.getAuthHeaders() });
        if (!resp.ok) throw new Error('No se pudo cargar el cuestionario');
        const cuestionario = await resp.json();
        
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