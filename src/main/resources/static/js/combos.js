// Módulo de gestión de combos
const CombosManager = {
    ultimoListado: [],
    combos: [],
    paginaActual: 0,
    tamanioPagina: 25,
    totalCombos: 0,
    totalPaginas: 0,
    cargando: false,
    
    async cargarCombos(resetear = true) {
        try {
            if (!authManager.isAuthenticated()) {
                console.error('Usuario no autenticado');
                return;
            }
            
            if (resetear) {
                this.paginaActual = 0;
                this.combos = [];
            }
            
            this.cargando = true;
            this.mostrarEstadoCarga();
            
            const params = new URLSearchParams({
                page: this.paginaActual,
                size: this.tamanioPagina
            });
            
            const response = await fetch(`/api/combos?${params}`, {
                headers: authManager.getAuthHeaders()
            });
            
            if (!response.ok) throw new Error('Error al cargar los combos');
            
            const data = await response.json();
            
            if (resetear) {
                this.combos = data.combos;
            } else {
                this.combos = [...this.combos, ...data.combos];
            }
            
            this.ultimoListado = this.combos;
            this.totalCombos = data.totalItems;
            this.totalPaginas = data.totalPages;
            this.paginaActual = data.currentPage;
            
            await this.mostrarCombos(this.combos);
            this.actualizarPaginacion();
            this.cargando = false;
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
            this.cargando = false;
        }
    },
    
    async cargarMasCombos() {
        if (this.cargando) return;
        if (this.paginaActual >= this.totalPaginas - 1) return;
        
        this.paginaActual++;
        await this.cargarCombos(false);
    },
    
    mostrarEstadoCarga() {
        const tbody = document.getElementById('tabla-combos');
        if (!tbody) return;
        
        if (this.cargando && this.combos.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center"><div class="spinner-border text-primary" role="status"><span class="visually-hidden">Cargando...</span></div><p class="mt-2">Cargando combos...</p></td></tr>';
        }
    },
    
    actualizarPaginacion() {
        let paginacionContainer = document.getElementById('paginacion-combos');
        if (!paginacionContainer) {
            // Crear el contenedor si no existe
            const tablaContainer = document.querySelector('.table-responsive');
            if (tablaContainer) {
                const paginacionDiv = document.createElement('div');
                paginacionDiv.id = 'paginacion-combos';
                paginacionDiv.className = 'mt-3 d-flex justify-content-between align-items-center';
                tablaContainer.parentNode.insertBefore(paginacionDiv, tablaContainer.nextSibling);
                paginacionContainer = document.getElementById('paginacion-combos');
            }
        }
        
        if (paginacionContainer) {
            const infoPagina = document.createElement('div');
            infoPagina.innerHTML = `Mostrando ${this.combos.length} de ${this.totalCombos} combos (Página ${this.paginaActual + 1} de ${this.totalPaginas})`;
            
            const botonCargarMas = document.createElement('button');
            botonCargarMas.className = 'btn btn-primary';
            botonCargarMas.innerHTML = '<i class="fas fa-plus"></i> Cargar más combos';
            botonCargarMas.type = 'button';
            botonCargarMas.id = 'btn-cargar-mas-combos';
            
            const botonDeshabilitado = this.cargando || this.paginaActual >= this.totalPaginas - 1;
            
            if (botonDeshabilitado) {
                botonCargarMas.disabled = true;
                botonCargarMas.style.opacity = '0.6';
                botonCargarMas.style.cursor = 'not-allowed';
            } else {
                botonCargarMas.disabled = false;
                botonCargarMas.style.opacity = '1';
                botonCargarMas.style.cursor = 'pointer';
                
                botonCargarMas.addEventListener('click', (e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    this.cargarMasCombos();
                });
            }
            
            paginacionContainer.innerHTML = '';
            paginacionContainer.appendChild(infoPagina);
            paginacionContainer.appendChild(botonCargarMas);
        }
    },

    async mostrarCombos(combos) {
        const tbody = document.getElementById('tabla-combos');
        if (!tbody) {
            console.error('No se encontró el elemento tabla-combos');
            return;
        }
        
        // Cargar temáticas para el filtro
        try {
            const response = await fetch('/api/temas', {
                headers: authManager.getAuthHeaders()
            });
            if (response.ok) {
                const tematicas = await response.json();
                
                // Llenar el filtro de temáticas
                const filtroTematica = document.getElementById('filtro-tematica-combo');
                if (filtroTematica) {
                    // Mantener la primera opción "Todas"
                    filtroTematica.innerHTML = '<option value="">Todas</option>';
                    
                    // Añadir las temáticas
                    tematicas.forEach(tematica => {
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
            const preguntasAsignadas = new Set(); // Para controlar qué preguntas ya se han asignado
            

            
            // Primero, intentar asignar preguntas a slots según la información del backend
            if (Array.isArray(c.preguntas)) {
                c.preguntas.forEach(pc => {
                    if (pc && pc.slot && pc.pregunta) {
                        preguntasPorSlot[pc.slot] = pc.pregunta;
                        preguntasAsignadas.add(pc.pregunta.id);

                    }
                });
            }
            
            // Segundo, asignar cualquier pregunta que no tenga slot a un slot vacío
            if (Array.isArray(c.preguntas)) {
                c.preguntas.forEach(pc => {
                    if (pc && pc.pregunta && !preguntasAsignadas.has(pc.pregunta.id)) {
                        // Buscar un slot vacío
                        for (let i = 0; i < niveles.length; i++) {
                            const slot = niveles[i];
                            if (!preguntasPorSlot[slot]) {
                                preguntasPorSlot[slot] = pc.pregunta;
                                preguntasAsignadas.add(pc.pregunta.id);

                                break;
                            }
                        }
                    }
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
                        <option value="R" ${c.tipo === 'R' ? 'selected' : ''}>R (Rescate)</option>
                    </select>
                </td>
                <td>
                    <select class="form-select form-select-sm" onchange="cambiarEstadoCombo(${c.id}, this.value)">
                        ${getOpcionesEstadoCombo(c.estado)}
                    </select>
                </td>
                <td>${(c.preguntas && c.preguntas.length) || 0}</td>
                <td>${c.fechaCreacion ? Utils.formatearFecha(String(c.fechaCreacion)) : ''}</td>
                <td>
                    <button class="btn btn-sm btn-primary me-1" onclick="editarCombo(${c.id})" title="Editar combo">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="eliminarCombo(${c.id})" title="Eliminar combo">
                        <i class="fas fa-trash"></i>
                    </button>
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
                    // Mostrar nivel real de la pregunta y el factor como campos separados
                    const nivelReal = p.nivel ? p.nivel.replace('_', '') : slotNivel;
                    
                    // Obtener el factor multiplicador de la pregunta o usar un valor por defecto basado en el slot
                    let factorMostrar = '';
                                // Buscamos el factor en las preguntas del combo
            // Primero buscamos en el array de preguntas del combo
            let preguntaCombo = null;
            if (c.preguntas && Array.isArray(c.preguntas)) {
                for (let i = 0; i < c.preguntas.length; i++) {
                    const pCombo = c.preguntas[i];
                    if (pCombo && pCombo.pregunta && pCombo.pregunta.id === p.id) {
                        preguntaCombo = pCombo;
                        break;
                    }
                }
            }
            
            if (preguntaCombo && preguntaCombo.factorMultiplicacion) {
                factorMostrar = preguntaCombo.factorMultiplicacion;
                console.log(`[DEBUG] Factor encontrado para pregunta ${p.id}: ${factorMostrar}`);
            } else {
                // Asignar un factor por defecto según el slot
                if (slotNivel === 'PM1') factorMostrar = 'X2';
                else if (slotNivel === 'PM2') factorMostrar = 'X3';
                else if (slotNivel === 'PM3') factorMostrar = 'X';
                console.log(`[DEBUG] Factor por defecto para pregunta ${p.id}: ${factorMostrar}`);
                
                // Actualizar el factor en el backend para evitar inconsistencias
                // Usamos un setTimeout para evitar conflictos de actualización
                setTimeout(() => {
                    // Solo actualizamos en backend sin recargar la interfaz
                    fetch(`/api/combos/${c.id}/preguntas/${p.id}/factor`, {
                        method: 'PUT',
                        headers: {
                            ...authManager.getAuthHeaders(),
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({ factorMultiplicacion: factorMostrar })
                    }).catch(e => console.error('Error al actualizar factor inicial:', e));
                }, 100);
            }
                    
                    filasPreguntas += `<tr data-id="${p.id}" data-nivel="${slotNivel}" style="cursor:pointer;">
                        <td style="width:60px; max-width:60px;"><span class='${CombosManager.getNivelColor ? CombosManager.getNivelColor(p.nivel) : ''}'>${nivelReal}</span></td>
                        <td style="width:60px; max-width:60px;">
                            <div class="input-group input-group-sm">
                                <input type="text" class="form-control form-control-sm" value="${factorMostrar}" 
                                       onchange="actualizarFactorPregunta(${c.id}, ${p.id}, this.value)" 
                                       onclick="event.stopPropagation();">
                            </div>
                        </td>
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
                        <td style="width:60px; max-width:60px;"><span class='${CombosManager.getNivelColor ? CombosManager.getNivelColor(slotNivel) : ''}'>${tipoSlot}</span></td>
                        <td style="width:60px; max-width:60px;" class="text-center text-muted">${multiplicador}</td>
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
                                <th style="width:60px; max-width:60px;">Nivel</th>
                                <th style="width:60px; max-width:60px;">Factor</th>
                                <th style="width:45%; min-width:200px;">Pregunta</th>
                                <th style="width:30%; min-width:150px;">Respuesta</th>
                                <th style="width:60px; max-width:60px;">Acción</th>
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
            case 'R': return 'Rescate (R)';
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
        const tematica = document.getElementById('filtro-tematica-combo')?.value || '';
        const subtema = document.getElementById('filtro-subtema-combo')?.value || '';
        const busqueda = document.getElementById('buscar-combo')?.value || '';

        // Si hay filtros de tematica o subtema, usar backend
        if (tematica || subtema) {
            const params = new URLSearchParams();
            if (estado) params.append('estado', estado);
            if (tipo) params.append('tipo', tipo);
            if (tematica) params.append('tematica', tematica);
            if (subtema) params.append('subtema', subtema);
            
            const response = await fetch(`/api/combos/filtrar?${params.toString()}`, {
                headers: authManager.getAuthHeaders()
            });
            
            if (!response.ok) throw new Error('Error al filtrar combos');
            const combos = await response.json();
            
            // Aplicar filtro de búsqueda por ID si existe
            let combosFiltrados = combos;
            if (busqueda) {
                combosFiltrados = combos.filter(c => 
                    c.id.toString().includes(busqueda)
                );
            }
            
            CombosManager.mostrarCombos(combosFiltrados);
            return;
        }
        
        // Filtrado en memoria para los filtros básicos
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
            // Buscar por ID directamente en el backend
            try {
                // Intentar buscar por ID exacto primero
                if (!isNaN(parseInt(busqueda))) {
                    const idBusqueda = parseInt(busqueda);
                    const response = await fetch(`/api/combos/${idBusqueda}`, {
                        headers: authManager.getAuthHeaders()
                    });
                    
                    if (response.ok) {
                        // Si se encuentra el combo por ID exacto
                        const combo = await response.json();
                        CombosManager.mostrarCombos([combo]);
                        return;
                    }
                }
                
                // Si no se encuentra por ID exacto o no es un número, buscar por coincidencia parcial
                const params = new URLSearchParams();
                params.append('id', busqueda);
                
                const response = await fetch(`/api/combos/filtrar?${params.toString()}`, {
                    headers: authManager.getAuthHeaders()
                });
                
                if (!response.ok) throw new Error('Error al buscar combos por ID');
                const combos = await response.json();
                CombosManager.mostrarCombos(combos);
                return;
            } catch (error) {
                console.error('Error al buscar combo por ID:', error);
                Toastify({
                    text: 'Error al buscar combo por ID',
                    duration: 3000,
                    close: true,
                    gravity: "top",
                    position: "right",
                    style: { background: "linear-gradient(to right, #ff0000, #cc0000)" }
                }).showToast();
            }
        }

        // Solo si no hay búsqueda por ID, mostrar los filtrados en memoria
        CombosManager.mostrarCombos(combosFiltrados);
    } catch (error) {
        console.error('Error al filtrar combos:', error);
        await CombosManager.cargarCombos();
    }
}

window.limpiarFiltrosCombos = function() {
    document.getElementById('filtro-estado-combo').value = '';
    document.getElementById('filtro-tipo-combo').value = '';
    document.getElementById('filtro-tematica-combo').value = '';
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
        const factor = document.getElementById(`factor-${pm.id}`);
        if (sel) sel.value = '';
        if (texto) texto.value = '';
        if (factor) {
            // Establecer valores por defecto para los factores
            if (pm.id === 'PM1') factor.value = 'X2';
            else if (pm.id === 'PM2') factor.value = 'X3';
            else if (pm.id === 'PM3') factor.value = 'X';
        }
    });
    
    // Resetear título y formulario para nuevo combo
    document.getElementById('modal-combo-titulo').textContent = 'Nuevo Combo';
    document.getElementById('combo-id').value = '';
    document.getElementById('combo-tipo').value = '';
    
    // Mostrar modal
    const modal = new bootstrap.Modal(document.getElementById('modal-combo'));
    modal.show();
}

async function editarCombo(id) {
    try {
        console.log(`[DEBUG_EDIT] Iniciando edición del combo ${id}`);
        const response = await fetch(`/api/combos/${id}`, {
            headers: authManager.getAuthHeaders()
        });
        if (!response.ok) {
            throw new Error('Error al cargar el combo');
        }
        const combo = await response.json();
        console.log(`[DEBUG_EDIT] Datos del combo cargados:`, combo);
        
        // Cambiar título del modal
        document.getElementById('modal-combo-titulo').textContent = 'Editar Combo';
        
        // Rellenar datos básicos
        document.getElementById('combo-id').value = combo.id;
        document.getElementById('combo-tipo').value = combo.tipo || '';
        
        // Limpiar primero todos los campos PM
        console.log(`[DEBUG_EDIT] Limpiando campos de formulario`);
        pms.forEach(pm => {
            const sel = document.getElementById(`pm-${pm.id}`);
            const texto = document.getElementById(`pm-${pm.id}-texto`);
            const factor = document.getElementById(`factor-${pm.id}`);
            if (sel) sel.value = '';
            if (texto) texto.value = '';
            // Valores por defecto para los factores
            if (factor) {
                if (pm.id === 'PM1') factor.value = 'X2';
                else if (pm.id === 'PM2') factor.value = 'X3';
                else if (pm.id === 'PM3') factor.value = 'X';
            }
        });
        
        // Rellenar preguntas multiplicadoras
        if (combo.preguntas && Array.isArray(combo.preguntas)) {
            console.log(`[DEBUG_EDIT] Procesando ${combo.preguntas.length} preguntas del combo`);
            
            combo.preguntas.forEach(pc => {
                console.log(`[DEBUG_EDIT] Procesando pregunta:`, pc);
                if (pc.slot && pc.pregunta) {
                    const selId = `pm-${pc.slot}`;
                    const textoId = `pm-${pc.slot}-texto`;
                    const factorId = `factor-${pc.slot}`;
                    console.log(`[DEBUG_EDIT] Asignando pregunta al slot ${pc.slot}, IDs de elementos: selId=${selId}, textoId=${textoId}, factorId=${factorId}`);
                    
                    const sel = document.getElementById(selId);
                    const texto = document.getElementById(textoId);
                    const factor = document.getElementById(factorId);
                    
                    if (sel && texto) {
                        sel.value = pc.pregunta.id;
                        texto.value = `${pc.pregunta.pregunta} → ${pc.pregunta.respuesta}`;
                        console.log(`[DEBUG_EDIT] Pregunta ID=${pc.pregunta.id} asignada al campo ${selId}`);
                    } else {
                        console.error(`[DEBUG_EDIT] No se encontraron los elementos para el slot ${pc.slot}`);
                    }
                    
                    // Establecer el factor si existe
                    if (factor && pc.factorMultiplicacion) {
                        factor.value = pc.factorMultiplicacion;
                        console.log(`[DEBUG_EDIT] Factor '${pc.factorMultiplicacion}' asignado al campo ${factorId}`);
                    }
                } else {
                    console.warn(`[DEBUG_EDIT] Datos de pregunta incompletos:`, pc);
                }
            });
        } else {
            console.warn(`[DEBUG_EDIT] No hay preguntas en el combo o no es un array`);
        }
        
        // Mostrar el modal
        const modal = new bootstrap.Modal(document.getElementById('modal-combo'));
        modal.show();
        
    } catch (error) {
        Toastify({
            text: 'Error al cargar combo: ' + error.message,
            duration: 3000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
        }).showToast();
    }
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
        
        // Log para debug
        console.log('[DEBUG] Procesando pregunta ID:', p.id);
        console.log('[DEBUG] Pregunta texto:', p.pregunta);
        console.log('[DEBUG] Respuesta texto:', p.respuesta);
        
        // Crear el botón de forma más segura usando addEventListener en lugar de onclick
        tr.innerHTML = `
            <td>${p.id}</td>
            <td>${p.pregunta}</td>
            <td>${p.respuesta}</td>
            <td>${p.tematica}</td>
            <td><span class="${CombosManager.getNivelColor(p.nivel)}">${p.nivel}</span></td>
            <td>
                <button class="btn btn-sm btn-success" data-pregunta-id="${p.id}" data-pregunta-texto="${encodeURIComponent(p.pregunta)}" data-tematica="${encodeURIComponent(p.tematica)}" data-respuesta="${encodeURIComponent(p.respuesta)}" data-subtema="${encodeURIComponent(p.subtema || '')}">
                    Seleccionar
                </button>
            </td>
        `;
        
        // Agregar event listener de forma segura
        const button = tr.querySelector('button');
        button.addEventListener('click', function() {
            const preguntaId = this.getAttribute('data-pregunta-id');
            const preguntaTexto = decodeURIComponent(this.getAttribute('data-pregunta-texto'));
            const tematica = decodeURIComponent(this.getAttribute('data-tematica'));
            const respuesta = decodeURIComponent(this.getAttribute('data-respuesta'));
            const subtema = decodeURIComponent(this.getAttribute('data-subtema'));
            
            console.log('[DEBUG] Seleccionando pregunta:', preguntaId, preguntaTexto);
            seleccionarPreguntaModal(preguntaId, preguntaTexto, tematica, respuesta, subtema);
        });
        
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
        const factorInput = document.getElementById(`factor-${pm.id}`);
        const factor = factorInput ? factorInput.value.trim() : pm.factor;
        
        console.log(`[DEBUG] PM${idx + 1}: ID=${id}, Factor=${factor}, FactorInput=${factorInput ? factorInput.value : 'null'}`);
        
        if (!id) valid = false;
        else preguntasMultiplicadoras.push({ id: Number(id), factor: factor });
    });
    
    console.log('[DEBUG] Preguntas multiplicadoras a enviar:', preguntasMultiplicadoras);
    
    if (!valid) {
        Toastify({
            text: 'Debes seleccionar todas las preguntas multiplicadoras (Pregunta 1, 2, 3)',
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
        
        if (!resp.ok) {
            let errorMessage = 'No se pudo eliminar el combo';
            
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
        
        Toastify({ text: 'Combo eliminado', duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' } }).showToast();
        await CombosManager.cargarCombos();
    } catch (e) {
        console.error('Error al eliminar combo:', e);
        Toastify({ text: 'Error al eliminar combo: ' + e.message, duration: 5000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' } }).showToast();
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

window.actualizarFactorPregunta = async function(comboId, preguntaId, nuevoFactor) {
    try {
        console.log(`[DEBUG] Actualizando factor para combo ${comboId}, pregunta ${preguntaId}, nuevo factor: ${nuevoFactor}`);
        
        // Guardar el valor visual actual de la pregunta para restaurarlo después de la recarga
        const filaPregunta = document.querySelector(`tr[data-id="${preguntaId}"]`);
        const textoPregunta = filaPregunta ? filaPregunta.querySelector('td:nth-child(3)').innerHTML : null;
        const textoRespuesta = filaPregunta ? filaPregunta.querySelector('td:nth-child(4)').innerHTML : null;
        
        // Validar que el factor no esté vacío
        if (!nuevoFactor || nuevoFactor.trim() === '') {
            nuevoFactor = 'X'; // Valor por defecto
        }
        
        const response = await fetch(`/api/combos/${comboId}/preguntas/${preguntaId}/factor`, {
            method: 'PUT',
            headers: {
                ...authManager.getAuthHeaders(),
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ factorMultiplicacion: nuevoFactor })
        });
        
        let responseData;
        try {
            responseData = await response.json();
        } catch (e) {
            responseData = await response.text();
        }
        
        if (!response.ok) {
            throw new Error(typeof responseData === 'string' ? responseData : 'Error al actualizar el factor');
        }
        
        console.log(`[DEBUG] Factor actualizado correctamente en el servidor:`, responseData);
        
        Toastify({
            text: 'Factor actualizado correctamente',
            duration: 2000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' }
        }).showToast();
        
        // Actualizar el valor en la UI directamente sin recargar todos los combos
        // Esto evita que se pierda la pregunta visualmente
        const inputField = document.querySelector(`tr[data-id="${preguntaId}"] input[type="text"]`);
        if (inputField) {
            inputField.value = nuevoFactor;
        }
        
        // NO recargamos todos los combos aquí para evitar perder el contenido de la pregunta
        // await CombosManager.cargarCombos();
        
    } catch (error) {
        console.error('[DEBUG] Error al actualizar factor:', error);
        Toastify({
            text: 'Error: ' + error.message,
            duration: 3000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
        }).showToast();
    }
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

function getOpcionesEstadoCombo(estadoActual) {
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

window.cambiarEstadoCombo = async function(id, nuevoEstado) {
    try {
        const response = await fetch(`/api/combos/${id}/estado?nuevoEstado=${nuevoEstado}`, {
            method: 'PUT',
            headers: authManager.getAuthHeaders()
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText);
        }

        const data = await response.json();
        await CombosManager.cargarCombos();
        
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