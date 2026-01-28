// Módulo de gestión de combos
const CombosManager = {
    ultimoListado: [],
    combos: [],
    paginaActual: 0,
    tamanioPagina: 25,
    totalCombos: 0,
    totalPaginas: 0,
    cargando: false,
    tematicas: [],
    lastScrollY: 0,
    lastFocusComboId: null,

    rememberScroll() {
        this.lastScrollY = window.scrollY || window.pageYOffset || 0;
    },

    restoreScrollOrFocus() {
        if (this.lastFocusComboId) {
            const row = document.querySelector(`tr.fila-combo[data-id="${this.lastFocusComboId}"]`);
            if (row) {
                row.scrollIntoView({ block: 'start', behavior: 'auto' });
                try { window.scrollBy(0, -80); } catch (e) {}
                return;
            }
        }
        window.scrollTo({ top: this.lastScrollY || 0, behavior: 'auto' });
    },
    
    async cargarTematicas() {
        try {
            const response = await fetch('/api/tematicas', {
                headers: authManager.getAuthHeaders()
            });
            
            if (!response.ok) {
                throw new Error(`Error al cargar temáticas: ${response.status} ${response.statusText}`);
            }
            
            const tematicasData = await response.json();
            // Extraer solo los nombres de las temáticas
            this.tematicas = tematicasData.map(tematica => tematica.nombre);
            return this.tematicas;
        } catch (error) {
            console.error('❌ [COMBOS] Error al cargar temáticas:', error);
            // Usar temáticas por defecto si falla la carga
            this.tematicas = ['GEOGRAFÍA', 'HISTORIA', 'DEPORTES', 'CIENCIA', 'ARTE', 'MÚSICA', 'CINE', 'LITERATURA', 'TECNOLOGÍA', 'GENERAL'];
            return this.tematicas;
        }
    },
    
    async cargarCombos(resetear = true, mantenerPagina = false) {
        try {
            if (!authManager.isAuthenticated()) {
                console.error('Usuario no autenticado');
                return;
            }
            
            if (resetear) {
                if (!mantenerPagina) {
                    this.paginaActual = 0;
                }
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
            
            // Siempre sustituir por la página actual
            this.combos = data.combos;
            
            this.ultimoListado = this.combos;
            this.totalCombos = data.totalItems;
            this.totalPaginas = data.totalPages;
            this.paginaActual = data.currentPage;
            
            console.log('📥 [CARGAR COMBOS] Combos recibidos del servidor:', this.combos);
            
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
        console.log('🔄 [CARGAR MÁS COMBOS] Iniciando cargarMasCombos...');
        console.log(`🔄 [CARGAR MÁS COMBOS] Estado actual: cargando=${this.cargando}, paginaActual=${this.paginaActual}, totalPaginas=${this.totalPaginas}`);
        
        // Evitar cargar más si ya está cargando o si no hay más páginas
        if (this.cargando) {
            console.log('⚠️ [CARGAR MÁS COMBOS] Ya está cargando, saliendo...');
            return;
        }
        if (this.paginaActual >= this.totalPaginas - 1) {
            console.log('⚠️ [CARGAR MÁS COMBOS] No hay más páginas, saliendo...');
            return;
        }
        
        try {
            // Incrementar la página ANTES de marcar como cargando
            this.paginaActual++;
            console.log(`🔄 [CARGAR MÁS COMBOS] Incrementada paginaActual a ${this.paginaActual}`);
            
            // Marcar como cargando y actualizar la UI
            console.log('🔄 [CARGAR MÁS COMBOS] Marcando como cargando y actualizando UI...');
            this.cargando = true;
            this.actualizarPaginacion();
            
            // Verificar si hay filtros activos
            const estado = document.getElementById('filtro-estado-combo')?.value || '';
            const tipo = document.getElementById('filtro-tipo-combo')?.value || '';
            const tematica = document.getElementById('filtro-tematica-combo')?.value || '';
            const subtema = document.getElementById('filtro-subtema-combo')?.value || '';
            const busqueda = document.getElementById('buscar-combo')?.value || '';
            
            console.log(`🔍 [CARGAR MÁS COMBOS] Filtros activos: estado=${estado}, tipo=${tipo}, tematica=${tematica}, subtema=${subtema}, busqueda=${busqueda}`);
            
            // Si hay filtros activos, realizar solicitud con filtros
            if (estado || tipo || tematica || subtema || busqueda) {
                console.log('🔄 [CARGAR MÁS COMBOS] Realizando solicitud con filtros para página ' + this.paginaActual);
                
                // Construir parámetros de búsqueda
                const params = new URLSearchParams({
                    page: this.paginaActual,
                    size: this.tamanioPagina
                });
                
                // Añadir filtros si existen
                if (estado) params.append('estado', estado);
                if (tipo) params.append('tipo', tipo);
                if (tematica) params.append('tematica', tematica);
                if (subtema) params.append('subtema', subtema);
                // Si la búsqueda es numérica, usar 'id', sino usar 'texto' para buscar en preguntas/respuestas
                if (busqueda) {
                    if (/^\d+$/.test(busqueda.trim())) {
                        params.append('id', busqueda);
                    } else {
                        params.append('texto', busqueda);
                    }
                }
                
                console.log('🔄 [CARGAR MÁS COMBOS] Parámetros:', params.toString());
                
                // Realizar la solicitud
                const response = await fetch(`/api/combos/filtrar?${params.toString()}`, {
                    headers: authManager.getAuthHeaders()
                });
                
                if (!response.ok) {
                    throw new Error(`Error al cargar más combos: ${response.status} ${response.statusText}`);
                }
                
                const data = await response.json();
                console.log('🔄 [CARGAR MÁS COMBOS] Respuesta recibida:', data);
                
                // Añadir los nuevos combos a los existentes
                if (data.combos && data.combos.length > 0) {
                    console.log(`🔄 [CARGAR MÁS COMBOS] Añadiendo ${data.combos.length} combos a los ${this.combos.length} existentes`);
                    this.combos = [...this.combos, ...data.combos];
                    
                    // Actualizar datos de paginación
                    this.totalCombos = data.totalItems || this.totalCombos;
                    this.totalPaginas = data.totalPages || this.totalPaginas;
                    
                    // Mostrar los combos
                    await this.mostrarCombos(this.combos);
                } else {
                    console.log('⚠️ [CARGAR MÁS COMBOS] No se recibieron nuevos combos');
                }
            } else {
                console.log('🔄 [CARGAR MÁS COMBOS] Usando cargarCombos con resetear=false');
                // Si no hay filtros, usar la carga normal
                await this.cargarCombos(false);
            }
            
            console.log(`✅ [CARGAR MÁS COMBOS] Completado. Combos cargados: ${this.combos.length}`);
        } catch (error) {
            console.error('❌ [CARGAR MÁS COMBOS] Error:', error);
            Toastify({
                text: 'Error al cargar más combos: ' + error.message,
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
            console.log('🔄 [CARGAR MÁS COMBOS] Finalizando, marcando como no cargando...');
            this.cargando = false;
            this.actualizarPaginacion();
        }
    },
    
    mostrarEstadoCarga() {
        const tbody = document.getElementById('tabla-combos');
        if (!tbody) return;
        
        if (this.cargando && this.combos.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center"><div class="spinner-border text-primary" role="status"><span class="visually-hidden">Cargando...</span></div><p class="mt-2">Cargando combos...</p></td></tr>';
        }
    },
    
    actualizarPaginacion() {
        const infoElement = document.getElementById('info-paginacion-combos');
        if (infoElement) {
            const inicio = (this.paginaActual * this.tamanioPagina) + 1;
            const fin = Math.min((this.paginaActual + 1) * this.tamanioPagina, this.totalCombos);
            infoElement.textContent = `Mostrando ${inicio}-${fin} de ${this.totalCombos} combos`;
        }

        const paginacionElement = document.getElementById('paginacion-combos');
        if (!paginacionElement) return;

        paginacionElement.innerHTML = '';
        if (this.totalPaginas <= 1) return;

        const primera = document.createElement('li');
        primera.className = `page-item ${this.paginaActual === 0 ? 'disabled' : ''}`;
        primera.innerHTML = `<a class="page-link" href="#" onclick="CombosManager.irAPagina(0);return false;">Primera</a>`;
        paginacionElement.appendChild(primera);

        const anterior = document.createElement('li');
        anterior.className = `page-item ${this.paginaActual === 0 ? 'disabled' : ''}`;
        anterior.innerHTML = `<a class="page-link" href="#" onclick="CombosManager.irAPagina(${this.paginaActual - 1});return false;">Anterior</a>`;
        paginacionElement.appendChild(anterior);

        const inicio = Math.max(0, this.paginaActual - 2);
        const fin = Math.min(this.totalPaginas - 1, this.paginaActual + 2);
        for (let i = inicio; i <= fin; i++) {
            const li = document.createElement('li');
            li.className = `page-item ${i === this.paginaActual ? 'active' : ''}`;
            li.innerHTML = `<a class="page-link" href="#" onclick="CombosManager.irAPagina(${i});return false;">${i + 1}</a>`;
            paginacionElement.appendChild(li);
        }

        const siguiente = document.createElement('li');
        siguiente.className = `page-item ${this.paginaActual >= this.totalPaginas - 1 ? 'disabled' : ''}`;
        siguiente.innerHTML = `<a class="page-link" href="#" onclick="CombosManager.irAPagina(${this.paginaActual + 1});return false;">Siguiente</a>`;
        paginacionElement.appendChild(siguiente);

        const ultima = document.createElement('li');
        ultima.className = `page-item ${this.paginaActual >= this.totalPaginas - 1 ? 'disabled' : ''}`;
        ultima.innerHTML = `<a class="page-link" href="#" onclick="CombosManager.irAPagina(${this.totalPaginas - 1});return false;">Última</a>`;
        paginacionElement.appendChild(ultima);
    },

    async irAPagina(pagina) {
        if (pagina < 0 || pagina >= this.totalPaginas || pagina === this.paginaActual || this.cargando) return;
        this.paginaActual = pagina;

        const estado = document.getElementById('filtro-estado-combo')?.value || '';
        const tipo = document.getElementById('filtro-tipo-combo')?.value || '';
        const tematica = document.getElementById('filtro-tematica-combo')?.value || '';
        const subtema = document.getElementById('filtro-subtema-combo')?.value || '';
        const busqueda = document.getElementById('buscar-combo')?.value || '';
        const hayFiltros = !!(estado || tipo || tematica || subtema || busqueda);

        if (hayFiltros) {
            this.combos = [];
            await window.filtrarCombos(false);
        } else {
            await this.cargarCombos(true, true);
        }
    },

    async mostrarCombos(combos) {
        const tbody = document.getElementById('tabla-combos');
        if (!tbody) {
            console.error('No se encontró el elemento tabla-combos');
            return;
        }
        
        // Cargar temáticas para el filtro
        await cargarOpcionesTematicas();
        

        
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
            // Determinar icono y tooltip para reutilización
            let iconoReutilizadoCombo = '';
            if (c.reutilizadoDeJornadaId) {
                iconoReutilizadoCombo = `<span class="ms-2" title="Reutilizado de ${c.reutilizadoDeJornadaNombre || 'jornada ' + c.reutilizadoDeJornadaId}" style="cursor: help;">♻️</span>`;
                console.log(`[FRONT-COMBO] Combo ${c.id} | estado=${c.estado} | jornada=${c.jornadaAsignada} | mostrarSelector=${!(c.jornadaAsignada && (c.estado === 'adjudicado' || c.estado === 'grabado'))}`);
            }
            
            // Determinar si debe mostrar "Asignado a jornada X" o el selector de estado
            // Solo mostrar "Asignado/Grabado" si está en estado adjudicado o grabado
            const estadoActual = typeof c.estado === 'string' ? c.estado : c.estado?.name || 'borrador';
            const estaReservado = c.jornadaAsignada && (estadoActual === 'adjudicado' || estadoActual === 'grabado');
            
            const tr = document.createElement('tr');
            tr.setAttribute('data-id', c.id);
            tr.classList.add('fila-combo');
            tr.innerHTML = `
                <td class="celda-numero-combo">${c.id ?? ''}</td>
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
                    <select class="form-select form-select-sm" onchange="actualizarCombo(${c.id}, 'tematica', this.value)">
                        <option value="">Sin temática</option>
                        ${getOpcionesTematicaCombo(c.tematica)}
                    </select>
                </td>
                <td>
                    ${estaReservado ? `<div class="text-muted">${(estadoActual === 'grabado') ? `Grabado en jornada ${c.jornadaAsignada}` : `Asignado a jornada ${c.jornadaAsignada}`}</div>` :
                    `<select class="form-select form-select-sm" onchange="cambiarEstadoCombo(${c.id}, this.value)">
                        ${getOpcionesEstadoCombo(estadoActual)}
                    </select>${iconoReutilizadoCombo}`}
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
                        <td>${p.datosExtra ?? ''}</td>
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
                        <td class="text-center text-muted">-</td>
                        <td><button class='btn btn-sm btn-success' onclick='event.stopPropagation();anadirPreguntaACombo(${c.id}, "${slotNivel}")'><i class='fas fa-plus'></i></button></td>
                    </tr>`;
                }
            }
            
            const puedeEditarNotas = authManager.hasRole('ROLE_ADMIN') || authManager.hasRole('ROLE_DIRECCION');
            subtr.innerHTML = `<td colspan="6">
                <div>
                    <table class="table table-preguntas-cuestionario mb-0">
                        <thead>
                            <tr>
                                <th style="width:60px; max-width:60px;">Nivel</th>
                                <th style="width:60px; max-width:60px;">Factor</th>
                                <th style="width:40%; min-width:200px;">Pregunta</th>
                                <th style="width:22%; min-width:120px;">Respuesta</th>
                                <th style="width:18%; min-width:120px;">Datos extra</th>
                                <th style="width:60px; max-width:60px;">Acción</th>
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
                              onblur="actualizarNotasDireccionCombo(${c.id}, this.value)">${c.notasDireccion || ''}</textarea>
                </div>` : ''}
            </td>`;
            tbody.appendChild(subtr);
            // Separador entre combos
            const sep = document.createElement('tr');
            sep.classList.add('separador-combo');
            sep.innerHTML = '<td colspan="7"></td>';
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
        // Restaurar posición o foco
        setTimeout(() => this.restoreScrollOrFocus(), 0);
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
window.filtrarCombos = async function(resetear = true) {
    console.log(`🔍 [FILTRAR COMBOS] Iniciando filtrarCombos con resetear=${resetear}`);
    console.log(`🔍 [FILTRAR COMBOS] Estado actual: cargando=${CombosManager.cargando}, paginaActual=${CombosManager.paginaActual}, totalPaginas=${CombosManager.totalPaginas}`);
    try {
        const estado = document.getElementById('filtro-estado-combo')?.value || '';
        const tipo = document.getElementById('filtro-tipo-combo')?.value || '';
        const tematica = document.getElementById('filtro-tematica-combo')?.value || '';
        const subtema = document.getElementById('filtro-subtema-combo')?.value || '';
        const busqueda = document.getElementById('buscar-combo')?.value || '';
        
        console.log(`🔍 [FILTRAR COMBOS] Filtros: estado=${estado}, tipo=${tipo}, tematica=${tematica}, subtema=${subtema}, busqueda=${busqueda}`);

        // Resetear paginación si es una nueva búsqueda
        if (resetear) {
            console.log('🔍 [FILTRAR COMBOS] Reseteando paginación y combos');
            CombosManager.paginaActual = 0;
            CombosManager.combos = [];
        }
        
        // Marcar como cargando y actualizar la UI
        console.log('🔍 [FILTRAR COMBOS] Marcando como cargando y actualizando UI');
        CombosManager.cargando = true;
        CombosManager.mostrarEstadoCarga();
        CombosManager.actualizarPaginacion();
        
        // Construir parámetros de búsqueda con paginación
        const params = new URLSearchParams({
            page: CombosManager.paginaActual,
            size: CombosManager.tamanioPagina
        });
        
        // Añadir filtros si existen
        if (estado) params.append('estado', estado);
        if (tipo) params.append('tipo', tipo);
        if (tematica) params.append('tematica', tematica);
        if (subtema) params.append('subtema', subtema);
        if (busqueda) params.append('texto', busqueda); // Buscar en preguntas y respuestas

        console.log('🔍 [FILTRAR COMBOS] Parámetros de búsqueda:', params.toString());

        // Realizar la búsqueda con filtros y paginación
        console.log(`🔍 [FILTRAR COMBOS] Enviando solicitud a /api/combos/filtrar?${params.toString()}`);
        const response = await fetch(`/api/combos/filtrar?${params.toString()}`, {
            headers: authManager.getAuthHeaders()
        });

        if (!response.ok) {
            console.error(`❌ [FILTRAR COMBOS] Error en respuesta: ${response.status} ${response.statusText}`);
            throw new Error(`Error al filtrar combos: ${response.status} ${response.statusText}`);
        }
        
        const data = await response.json();
        console.log('🔍 [FILTRAR COMBOS] Respuesta recibida:', data);
        
        // Actualizar datos de paginación
        if (resetear) {
            console.log('🔍 [FILTRAR COMBOS] Reemplazando combos existentes');
            CombosManager.combos = data.combos || [];
        } else {
            console.log(`🔍 [FILTRAR COMBOS] Añadiendo ${data.combos ? data.combos.length : 0} combos a los ${CombosManager.combos.length} existentes`);
            CombosManager.combos = [...CombosManager.combos, ...(data.combos || [])];
        }
        
        CombosManager.totalCombos = data.totalItems || 0;
        CombosManager.totalPaginas = data.totalPages || 0;
        CombosManager.paginaActual = data.currentPage || 0;
        
        console.log(`🔍 [FILTRAR COMBOS] Estado actualizado: totalCombos=${CombosManager.totalCombos}, totalPaginas=${CombosManager.totalPaginas}, paginaActual=${CombosManager.paginaActual}`);
        
        // Mostrar combos y actualizar paginación
        console.log(`🔍 [FILTRAR COMBOS] Mostrando ${CombosManager.combos.length} combos`);
        await CombosManager.mostrarCombos(CombosManager.combos);
        console.log('✅ [FILTRAR COMBOS] Combos mostrados correctamente');
    } catch (error) {
        console.error('❌ [FILTRAR COMBOS] Error:', error);
        Toastify({
            text: 'Error al filtrar combos: ' + error.message,
            duration: 3000,
            close: true,
            gravity: "top",
            position: "right",
            style: { background: "linear-gradient(to right, #ff0000, #cc0000)" }
        }).showToast();
    } finally {
        // Asegurar que siempre se marque como no cargando y se actualice la UI
        console.log('🔍 [FILTRAR COMBOS] Finalizando, marcando como no cargando');
        CombosManager.cargando = false;
        CombosManager.actualizarPaginacion();
    }
}

window.limpiarFiltrosCombos = async function() {
    document.getElementById('filtro-estado-combo').value = '';
    document.getElementById('filtro-tipo-combo').value = '';
    document.getElementById('filtro-tematica-combo').value = '';
    document.getElementById('buscar-combo').value = '';
    await CombosManager.cargarCombos();
}

// Función para actualizar tipo o estado de combo
window.actualizarCombo = async function(comboId, campo, valor) {
    try {
        CombosManager.rememberScroll();
        CombosManager.lastFocusComboId = comboId;
        const datos = {};
        datos[campo] = valor;
        
        console.log(`🔄 [ACTUALIZAR COMBO] Actualizando combo ${comboId}, campo: ${campo}, valor: ${valor}`);
        console.log('📤 [ACTUALIZAR COMBO] Datos a enviar:', datos);
        
        const previo = (CombosManager.ultimoListado || []).find(c => c.id === comboId);
        const valorPrevio = previo ? previo[campo] : null;
        const doAction = async () => { await apiManager.put(`/api/combos/${comboId}`, datos); await CombosManager.cargarCombos(); };
        const undoAction = async () => {
            if (valorPrevio !== null && valorPrevio !== undefined) {
                const back = {}; back[campo] = valorPrevio;
                await apiManager.put(`/api/combos/${comboId}`, back);
                await CombosManager.cargarCombos();
            }
        };
        await doAction();
        if (window.UndoManager) window.UndoManager.record({ do: doAction, undo: undoAction, label: `Actualizar combo ${comboId} - ${campo}` });
        
        const mensaje = 'Campo actualizado correctamente' || 
            (campo === 'tipo' ? 'Tipo actualizado correctamente' :
             campo === 'tematica' ? 'Temática actualizada correctamente' :
             'Campo actualizado correctamente');
        
        Toastify({
            text: mensaje,
            duration: 2000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' }
        }).showToast();
        
        // Recargar la lista para reflejar el cambio
        console.log('🔄 [ACTUALIZAR COMBO] Recargando lista de combos...');
        await CombosManager.cargarCombos();
        console.log('✅ [ACTUALIZAR COMBO] Lista de combos recargada');
        CombosManager.restoreScrollOrFocus();
        
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

async function inicializarCombos() {
    // Cargar temáticas primero
    await CombosManager.cargarTematicas();
    cargarOpcionesTematicas();
    
    // Luego cargar combos
    await CombosManager.cargarCombos();
}

async function cargarOpcionesTematicas() {
    const selectTematica = document.getElementById('filtro-tematica-combo');
    if (!selectTematica) return;
    
    try {
        const valorSeleccionado = selectTematica.value || '';
        // Cargar temáticas desde la tabla tematicas
        const response = await fetch('/api/tematicas', {
            headers: authManager.getAuthHeaders()
        });
        
        if (response.ok) {
            const tematicasData = await response.json();
            
            // Limpiar opciones existentes (excepto "Todas")
            selectTematica.innerHTML = '<option value="">Todas</option>';
            
            // Añadir las temáticas de la tabla tematicas
            tematicasData.forEach(tematica => {
                const option = document.createElement('option');
                option.value = tematica.nombre;
                option.textContent = tematica.nombre;
                selectTematica.appendChild(option);
            });
            // Restaurar selección previa si existe
            if (valorSeleccionado) {
                selectTematica.value = valorSeleccionado;
                if (selectTematica.value !== valorSeleccionado) {
                    selectTematica.value = '';
                }
            }
        } else {
            console.error('Error al cargar temáticas:', response.status);
        }
    } catch (error) {
        console.error('Error al cargar temáticas:', error);
    }
}

document.addEventListener('DOMContentLoaded', inicializarCombos);

// Reutilizar el gestor de temáticas ya existente (como en cuestionarios)
window.mostrarGestionTematicas = function() {
    const modalCuest = document.getElementById('modal-gestion-temas-subtemas');
    if (modalCuest) {
        const modal = new bootstrap.Modal(modalCuest);
        modal.show();
        if (typeof TematicasManager !== 'undefined') {
            TematicasManager.cargarTematicas?.();
            TematicasManager.cargarEstadisticas?.();
        }
        return;
    }
    const modalLocal = document.getElementById('modal-gestion-tematicas');
    if (modalLocal) {
        const modal = new bootstrap.Modal(modalLocal);
        modal.show();
        (async () => {
            try {
                const resp = await fetch('/api/cuestionarios/tematicas', { headers: authManager.getAuthHeaders() });
                const tematicas = resp.ok ? await resp.json() : [];
                const tbody = document.getElementById('lista-tematicas');
                const total = document.getElementById('total-tematicas');
                if (tbody) {
                    tbody.innerHTML = '';
                    tematicas.forEach((t, i) => {
                        const tr = document.createElement('tr');
                        tr.innerHTML = `<td>${i + 1}</td><td>${t}</td><td></td>`;
                        tbody.appendChild(tr);
                    });
                }
                if (total) total.textContent = tematicas.length;
            } catch {}
        })();
        const form = document.getElementById('form-añadir-tematica');
        if (form && !form._bound) {
            form._bound = true;
            form.addEventListener('submit', async function(e) {
                e.preventDefault();
                const input = document.getElementById('nueva-tematica');
                const nombre = (input?.value || '').trim();
                if (!nombre) return;
                try {
                    const resp = await fetch('/api/cuestionarios/tematicas', {
                        method: 'POST',
                        headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' },
                        body: JSON.stringify({ tematica: nombre })
                    });
                    if (!resp.ok) {
                        const txt = await resp.text();
                        throw new Error(txt || 'Error al añadir temática');
                    }
                    Toastify({ text: 'Temática añadida', duration: 2000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' } }).showToast();
                    input.value = '';
                    await CombosManager.cargarTematicas();
                    await cargarOpcionesTematicas();
                    window.mostrarGestionTematicas();
                } catch (err) {
                    Toastify({ text: 'Error: ' + err.message, duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' } }).showToast();
                }
            });
        }
    }
};

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
    document.getElementById('combo-tematica').value = '';
    const comboEstado = document.getElementById('combo-estado');
    if (comboEstado) {
        comboEstado.value = 'borrador';
        comboEstado.disabled = false;
    }
    const asignadoDiv = document.getElementById('combo-jornada-asignada');
    if (asignadoDiv) asignadoDiv.classList.add('d-none');
    const comboNotas = document.getElementById('combo-notas');
    if (comboNotas) comboNotas.value = '';
    
    // Cargar temáticas en el desplegable
    await cargarTematicasEnModal();
    
    // Mostrar modal
    const modal = new bootstrap.Modal(document.getElementById('modal-combo'));
    modal.show();
}

async function cargarTematicasEnModal() {
    const selectTematica = document.getElementById('combo-tematica');
    if (!selectTematica) return;
    
    try {
        // Cargar temáticas desde la tabla tematicas
        const response = await fetch('/api/tematicas', {
            headers: authManager.getAuthHeaders()
        });
        
        if (response.ok) {
            const tematicas = await response.json();
            
            // Limpiar opciones existentes (excepto "Sin temática")
            selectTematica.innerHTML = '<option value="">Sin temática</option>';
            
            // Añadir temáticas
            tematicas.forEach(tematica => {
                const option = document.createElement('option');
                option.value = tematica.nombre;
                option.textContent = tematica.nombre;
                selectTematica.appendChild(option);
            });
            
        } else {
            console.error('❌ [COMBOS] Error al cargar temáticas para modal');
        }
    } catch (error) {
        console.error('❌ [COMBOS] Error al cargar temáticas para modal:', error);
    }
}

async function editarCombo(id) {
    try {
        CombosManager.lastFocusComboId = id;
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
        await cargarTematicasEnModal();
        document.getElementById('combo-tematica').value = combo.tematica || '';
        const comboNotas = document.getElementById('combo-notas');
        if (comboNotas) comboNotas.value = combo.notasDireccion || '';
        const comboEstado = document.getElementById('combo-estado');
        if (comboEstado) {
            comboEstado.value = (combo.estado || 'borrador');
            comboEstado.disabled = !!combo.jornadaAsignada;
        }
        const asignadoDiv = document.getElementById('combo-jornada-asignada');
        if (asignadoDiv) {
            if (combo.jornadaAsignada) {
                asignadoDiv.textContent = (combo.estado === 'grabado')
                    ? `Grabado en jornada ${combo.jornadaAsignada}`
                    : `Asignado a jornada ${combo.jornadaAsignada}`;
                asignadoDiv.classList.remove('d-none');
            } else {
                asignadoDiv.classList.add('d-none');
            }
        }
        
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
        
        // Cargar temáticas en el desplegable
        await cargarTematicasEnModal();
        
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
    const cargarTematicas = async () => {
        try {
            const sel = document.getElementById('buscador-tematica-select');
            if (!sel) return;
            // Temáticas de preguntas (distintas)
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

    const ids = ['buscador-id', 'buscador-texto', 'buscador-tematica-select', 'buscador-nivel-combo'];
    ids.forEach(id => {
        const el = document.getElementById(id);
        if (!el) return;
        const handler = () => buscarPreguntasModal(0);
        if (el.tagName === 'SELECT') {
            el.removeEventListener('change', el._buscadorHandler || (()=>{}));
            el._buscadorHandler = handler;
            el.addEventListener('change', handler);
        } else {
            el.removeEventListener('keyup', el._buscadorHandler || (()=>{}));
            el._buscadorHandler = handler;
            el.addEventListener('keyup', handler);
            if (id === 'buscador-id') {
                el.removeEventListener('change', el._buscadorHandlerChange || (()=>{}));
                el._buscadorHandlerChange = handler;
                el.addEventListener('change', handler);
            }
        }
    });
}

function abrirSelectorPregunta(nivel, factor = null) {
    selectorPreguntaContext.nivel = nivel;
    selectorPreguntaContext.factor = factor;
    selectorPreguntaContext.inputId = `pm-${nivel}`;
    selectorPreguntaContext.textoId = `pm-${nivel}-texto`;
    
    const idInput = document.getElementById('buscador-id');
    if (idInput) idInput.value = '';
    const textoInput = document.getElementById('buscador-texto');
    if (textoInput) textoInput.value = '';
    const temaSelect = document.getElementById('buscador-tematica-select');
    if (temaSelect) temaSelect.value = '';
    const nivelSelect = document.getElementById('buscador-nivel-combo');
    if (nivelSelect) nivelSelect.value = '';
    inicializarBuscadorPreguntasModal();
    buscarPreguntasModal(0);
    const modal = new bootstrap.Modal(document.getElementById('modal-selector-pregunta'));
    modal.show();
}

// Función para buscar preguntas en el modal (específica para combos)
async function buscarPreguntasModal(page = 0) {
    const id = document.getElementById('buscador-id').value.trim();
    const texto = (document.getElementById('buscador-texto')?.value || '').trim();
    const tematica = document.getElementById('buscador-tematica-select')?.value || '';
    const filtroNivel = document.getElementById('buscador-nivel-combo')?.value || '';
    const estadoSel = (document.getElementById('buscador-estado')?.value || 'todos').trim().toLowerCase();

    try {
        // Para combos, buscar preguntas de nivel 5 con endpoint /filtrar y 'texto'
        let preguntas = [];
        let totalPages = 1;
        const params = new URLSearchParams();
        params.set('page', page);
        params.set('size', 20);
        let url = '';
        if (id) {
            // id exacto -> usar /buscar
            params.set('id', id.trim());
            if (tematica) params.set('tematica', tematica);
            if (filtroNivel) params.set('nivel', filtroNivel);
            url = `/api/preguntas/buscar?${params.toString()}`;
        } else {
            if (texto) params.set('texto', texto);
            if (tematica) params.set('tematica', tematica);
            // Estado: por defecto 'todos' (aprobada + verificada). Si el usuario elige uno, aplicarlo.
            if (estadoSel === 'aprobada' || estadoSel === 'verificada') {
                params.set('estado', estadoSel);
            } else {
                // Enviar CSV para que el backend filtre ambos sin sobrecargar al cliente
                params.set('estado', 'aprobada,verificada');
            }
            // nivel: por defecto trae _5LS y _5NLS; si el usuario elige uno, filtrar
            if (filtroNivel) {
                params.set('nivel', filtroNivel);
            }
            url = `/api/preguntas/filtrar?${params.toString()}`;
        }
        console.log('[FRONT][COMBO] URL de búsqueda:', url);
        const resp = await fetch(url, { headers: authManager.getAuthHeaders() });
        if (!resp.ok) throw new Error('Error al buscar preguntas');
        const data = await resp.json();
        // Sin restricción por nivel; mostrar todos por defecto
        let lista = data.content || [];
        // Filtro por estado en cliente si el usuario pide 'todos'
        if (estadoSel === 'todos') {
            lista = lista.filter(p => p.estado === 'aprobada' || p.estado === 'verificada');
        } else if (estadoSel === 'aprobada' || estadoSel === 'verificada') {
            lista = lista.filter(p => p.estado === estadoSel);
        }
        // Si el usuario selecciona nivel en el modal, el backend ya lo filtró. Mostrar la lista tal cual.
        preguntas = lista;
        totalPages = data.totalPages || 1;
        
        console.log('[COMBO] Total preguntas nivel 5 filtradas:', preguntas.length);
        
        renderizarPreguntasModal(preguntas, totalPages, page);
    } catch (error) {
        console.error('[COMBO] Error al buscar preguntas:', error);
        document.getElementById('tbody-selector-pregunta').innerHTML = `<tr><td colspan="4">Error al cargar preguntas: ${error.message}</td></tr>`;
        document.getElementById('paginacion-selector-pregunta').innerHTML = '';
    }
}

// Función para renderizar las preguntas en el modal
function renderizarPreguntasModal(preguntas, totalPages, currentPage) {
    const tbody = document.getElementById('tbody-selector-pregunta');
    if (!tbody) return;
    
    tbody.innerHTML = '';
    
    preguntas.forEach(pregunta => {
        const row = document.createElement('tr');
        
        // Determinar el color del nivel
        let nivelColor = '';
        if (pregunta.nivel === '_5NLS') {
            nivelColor = 'text-danger fw-bold'; // Rojo para 5NLS
        } else if (pregunta.nivel === '_5LS') {
            nivelColor = 'text-success fw-bold'; // Verde para 5LS
        } else {
            nivelColor = 'text-muted'; // Gris para otros niveles
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
                            Nivel: <span class="${nivelColor}">${pregunta.nivel}</span> | 
                            Estado: ${pregunta.estado}
                        </small>
                    </div>
                </div>
            </td>
        `;
        
        // Añadir evento de clic para seleccionar la pregunta
        row.style.cursor = 'pointer';
        row.addEventListener('click', () => {
            seleccionarPreguntaModal(
                pregunta.id,
                pregunta.pregunta,
                pregunta.tematica,
                pregunta.respuesta,
                pregunta.subtema,
                pregunta.estado
            );
        });
        
        tbody.appendChild(row);
    });
    
    // Renderizar paginación
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
            const estado = p.estado;
            
            console.log('[DEBUG] Seleccionando pregunta:', preguntaId, preguntaTexto);
            seleccionarPreguntaModal(preguntaId, preguntaTexto, tematica, respuesta, subtema, estado);
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

function seleccionarPreguntaModal(id, pregunta, tematica, respuesta, subtema, estado = null) {
    console.log('[FRONT] seleccionarPreguntaModal llamada con:', {id, pregunta, tematica, respuesta, subtema, selectorPreguntaContext});
    
    // Si hay contexto de añadir pregunta a combo, hacer petición AJAX
    if (window.contextoAnadirPregunta) {
        const { comboId, nivel } = window.contextoAnadirPregunta;
        // Determinar el factor según el nivel
        let factorMultiplicacion = 1;
        if (nivel === 'PM1') factorMultiplicacion = 2;
        else if (nivel === 'PM2') factorMultiplicacion = 3;
        else if (nivel === 'PM3') factorMultiplicacion = 0;
        
        const doAdd = async () => {
            await apiManager.post(`/api/combos/${comboId}/preguntas`, { preguntaId: id, factorMultiplicacion }, { headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' } });
            await CombosManager.cargarCombos();
        };
        const undoDelete = async () => {
            await apiManager.delete(`/api/combos/${comboId}/preguntas/${id}`, { headers: authManager.getAuthHeaders() });
            await CombosManager.cargarCombos();
        };
        doAdd()
        .then(async () => {
            if (window.UndoManager) {
                window.UndoManager.record({ do: doAdd, undo: undoDelete, label: `Añadir pregunta ${id} a combo ${comboId}` });
            }
            Toastify({ text: 'Pregunta añadida al combo', duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' } }).showToast();
            if (estado && String(estado).toLowerCase() === 'verificada') {
                Toastify({ text: 'Aviso: La pregunta estaba VERIFICADA y se ha marcado como APROBADA automáticamente.', duration: 4000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #ffc107, #ff9800)' } }).showToast();
            }
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
    CombosManager.rememberScroll();
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
        else preguntasMultiplicadoras.push({ id: Number(id), factor: factor, slot: pm.id });
    });
    
    console.log('[DEBUG] Preguntas multiplicadoras a enviar:', preguntasMultiplicadoras);
    
    // Obtener el estado del combo antes de validar
    const estadoCombo = document.getElementById('combo-estado')?.value || '';
    console.log('[DEBUG] Estado del combo:', estadoCombo);
    
    // Si el estado es "borrador", permitir guardar aunque falten preguntas
    if (!valid && estadoCombo !== 'borrador') {
        Toastify({
            text: 'Para estados distintos de Borrador, debes seleccionar todas las preguntas multiplicadoras (Pregunta 1, 2, 3)',
            duration: 4000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
        }).showToast();
        return;
    }
    
    // Si es borrador y no hay ninguna pregunta, también es inválido
    if (estadoCombo === 'borrador' && preguntasMultiplicadoras.length === 0) {
        Toastify({
            text: 'Debes seleccionar al menos una pregunta multiplicadora',
            duration: 3000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
        }).showToast();
        return;
    }
    
    // Validaciones adicionales: IDs únicos y factores no vacíos y únicos (solo si hay más de 1 pregunta)
    if (preguntasMultiplicadoras.length > 1) {
        const idsSet = new Set(preguntasMultiplicadoras.map(pm => pm.id));
        if (idsSet.size !== preguntasMultiplicadoras.length) {
            Toastify({
                text: 'No puedes usar la misma pregunta en varios multiplicadores',
                duration: 4000,
                close: true,
                gravity: 'top',
                position: 'right',
                style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
            }).showToast();
            return;
        }
    }
    
    // Validar factores solo para las preguntas que existen
    if (preguntasMultiplicadoras.length > 0) {
        const factores = preguntasMultiplicadoras.map(pm => (pm.factor || '').trim());
        if (factores.some(f => !f)) {
            Toastify({
                text: 'Todas las preguntas multiplicadoras deben tener un factor (X2, X3, X)',
                duration: 4000,
                close: true,
                gravity: 'top',
                position: 'right',
                style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
            }).showToast();
            return;
        }
    }
    
    // Aviso 1: comprobar preguntas en estado VERIFICADA (se promocionarán a APROBADA) - solo si hay preguntas
    if (preguntasMultiplicadoras.length > 0) {
        try {
        const detallesPreguntas = await Promise.all(
            preguntasMultiplicadoras.map(async pm => {
                try {
                    const resp = await fetch(`/api/preguntas/${pm.id}`, {
                        headers: authManager.getAuthHeaders()
                    });
                    if (!resp.ok) return null;
                    return await resp.json();
                } catch {
                    return null;
                }
            })
        );

        const preguntasVerificadas = (detallesPreguntas || []).filter(p =>
            p && typeof p.estado === 'string' &&
            String(p.estado).toLowerCase() === 'verificada'
        );

        if (preguntasVerificadas.length > 0) {
            const lista = preguntasVerificadas
                .map(p => `- ${p.id}: ${p.pregunta || '(sin texto)'}`)
                .join('\n');

            const mensajeVerificadas =
                'Atención: vas a crear/editar un combo que utiliza preguntas en estado VERIFICADA (no APROBADA todavía):\n\n' +
                lista +
                '\n\n' +
                'Si continúas, estas preguntas se marcarán automáticamente como APROBADAS y quedarán reservadas para este combo.\n\n' +
                '¿Quieres continuar y cambiar su estado a APROBADA?';

            const continuarVerificadas = window.confirm(mensajeVerificadas);
            if (!continuarVerificadas) {
                return;
            }
        }

        // Aviso 2: comprobar que las preguntas son de nivel 5 (para combos)
        const detallesPorId = {};
        (detallesPreguntas || []).forEach(p => {
            if (p && p.id != null) detallesPorId[p.id] = p;
        });

        const desajustesNivel = [];
        preguntasMultiplicadoras.forEach(pm => {
            const detalle = detallesPorId[pm.id];
            if (!detalle || !detalle.nivel) return;

            const actualNivel = typeof detalle.nivel === 'string'
                ? detalle.nivel
                : (detalle.nivel.name || detalle.nivel);

            if (!actualNivel || !String(actualNivel).startsWith('_5')) {
                desajustesNivel.push({
                    id: pm.id,
                    slot: pm.slot,
                    actualNivel,
                    targetNivel: '_5' + (String(actualNivel || '').endsWith('NLS') ? 'NLS' : 'LS'),
                    pregunta: detalle.pregunta || ''
                });
            }
        });

        if (desajustesNivel.length > 0) {
            const listaNiveles = desajustesNivel
                .map(m => `- Pregunta ${m.id}: "${m.pregunta}" (nivel actual: ${m.actualNivel || 'null'}, necesario: nivel 5 para combos)`)
                .join('\n');

            const mensajeNiveles =
                'Atención: algunas preguntas que estás usando en el COMBO no son de nivel 5 (recomendado para combos):\n\n' +
                listaNiveles +
                '\n\n' +
                'Si continúas, se cambiará automáticamente el NIVEL de esas preguntas a un nivel 5 (_5LS/_5NLS) compatible.\n\n' +
                '¿Quieres continuar y cambiar el nivel de esas preguntas?';

            const continuarNiveles = window.confirm(mensajeNiveles);
            if (!continuarNiveles) {
                return;
            }

            // Aplicar cambio de nivel en backend para cada pregunta con desajuste
            for (const m of desajustesNivel) {
                try {
                    await fetch(`/api/preguntas/${m.id}`, {
                        method: 'PUT',
                        headers: {
                            ...authManager.getAuthHeaders(),
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({
                            id: m.id,
                            nivel: m.targetNivel
                        })
                    });
                } catch (err) {
                    console.error('Error al cambiar nivel de la pregunta para combo', m.id, err);
                    Toastify({
                        text: `No se pudo cambiar el nivel de la pregunta ${m.id}: ${err.message || err}`,
                        duration: 4000,
                        close: true,
                        gravity: 'top',
                        position: 'right',
                        style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
                    }).showToast();
                }
            }
        }
    } catch (e) {
        console.warn('No se pudo comprobar estado/nivel de las preguntas antes de guardar el combo:', e);
        // Seguimos igualmente: el backend seguirá validando
    }
    } // Cierre del if (preguntasMultiplicadoras.length > 0)

    const comboId = document.getElementById('combo-id').value;
    const tematica = document.getElementById('combo-tematica').value;
    const esEdicion = !!comboId;
    const estadoSeleccionado = (document.getElementById('combo-estado')?.value) || 'borrador';
    
    try {
        const notasDireccion = (document.getElementById('combo-notas')?.value || '').trim();
        let resp, data;
        if (esEdicion) {
            // PUT para editar (implementar si es necesario)
            resp = await fetch(`/api/combos/${comboId}`, {
            method: 'PUT',
                headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' },
                body: JSON.stringify({ preguntasMultiplicadoras, tipo, tematica, notasDireccion, estado: estadoSeleccionado })
            });
        } else {
            // POST para crear - incluir el estado en el body
            resp = await fetch('/api/combos/nuevo', {
                method: 'POST',
                headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' },
                body: JSON.stringify({ preguntasMultiplicadoras, tipo, tematica, notasDireccion, estado: estadoSeleccionado })
            });
        }
        
        // Intentar leer JSON o texto para mostrar el mensaje del backend
        try { data = await resp.json(); } catch (e) { try { data = await resp.text(); } catch { data = null; } }
        if (!resp.ok) {
            const msg = (data && data.message) ? data.message : (typeof data === 'string' && data ? data : 'Error al guardar el combo');
            throw new Error(msg);
        }
        
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
        // Aplicar estado si procede
        try {
            const idFinal = (data && (data.id || data.ID)) || (comboId || null);
            if (idFinal && estadoSeleccionado && estadoSeleccionado !== 'borrador') {
                await fetch(`/api/combos/${idFinal}/estado?nuevoEstado=${encodeURIComponent(estadoSeleccionado)}`, {
                    method: 'PUT',
                    headers: authManager.getAuthHeaders()
                });
            }
            // Asegurar actualización de factores tras guardar (edición o creación)
            const idParaFactores = (data && (data.id || data.ID)) || comboId;
            if (idParaFactores) {
                await Promise.all(preguntasMultiplicadoras.map(pm => (
                    fetch(`/api/combos/${idParaFactores}/preguntas/${pm.id}/factor`, {
                        method: 'PUT',
                        headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' },
                        body: JSON.stringify({ factorMultiplicacion: pm.factor || 'X' })
                    }).catch(() => {})
                )));
            }
        } catch (e) {
            console.warn('No se pudo aplicar el estado seleccionado tras guardar combo:', e);
        }
        await CombosManager.cargarCombos();
        CombosManager.restoreScrollOrFocus();
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
        CombosManager.rememberScroll();
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
        CombosManager.restoreScrollOrFocus();
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
    let factorMultiplicacion = null;
    if (combo && Array.isArray(combo.preguntas)) {
        console.log(`[DEBUG] Preguntas del combo:`, combo.preguntas);
        const pc = combo.preguntas.find(pc => pc.slot === slot);
        console.log(`[DEBUG] Pregunta encontrada para slot ${slot}:`, pc);
        if (pc && pc.pregunta && pc.pregunta.id) {
            preguntaId = pc.pregunta.id;
            factorMultiplicacion = pc.factorMultiplicacion || null;
        }
    }
    
    if (!preguntaId) {
        console.log(`[DEBUG] No se encontró pregunta ID para el slot ${slot}`);
        Toastify({ text: 'No se encontró la pregunta a eliminar', duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' } }).showToast();
        return;
    }
    
    console.log(`[DEBUG] Eliminando pregunta ${preguntaId} del combo ${comboId}`);
    
    if (!confirm('¿Seguro que quieres quitar esta pregunta del combo?')) return;
    try {
        CombosManager.rememberScroll();
        CombosManager.lastFocusComboId = comboId;
        // Acciones do/undo
        const doDelete = async () => {
            await apiManager.delete(`/api/combos/${comboId}/preguntas/${preguntaId}`, { headers: authManager.getAuthHeaders() });
            await CombosManager.cargarCombos();
        };
        const undoAdd = async () => {
            await apiManager.post(`/api/combos/${comboId}/preguntas`, { preguntaId, factorMultiplicacion: factorMultiplicacion ?? 1 }, { headers: authManager.getAuthHeaders() });
            await CombosManager.cargarCombos();
        };
        await doDelete();
        if (window.UndoManager) {
            window.UndoManager.record({ do: doDelete, undo: undoAdd, label: `Quitar pregunta ${preguntaId} de combo ${comboId}` });
        }
        Toastify({ text: 'Pregunta eliminada del combo', duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' } }).showToast();
        await CombosManager.cargarCombos();
        CombosManager.restoreScrollOrFocus();
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
        
        // Obtener factor previo desde el listado
        const combo = (CombosManager.ultimoListado || []).find(c => c.id === comboId);
        let factorPrevio = null;
        if (combo && Array.isArray(combo.preguntas)) {
            const pc = combo.preguntas.find(x => x && x.pregunta && x.pregunta.id === preguntaId);
            factorPrevio = pc && pc.factorMultiplicacion ? pc.factorMultiplicacion : null;
        }
        const doAction = async () => {
            await apiManager.put(`/api/combos/${comboId}/preguntas/${preguntaId}/factor`, { factorMultiplicacion: nuevoFactor });
            await CombosManager.cargarCombos();
        };
        const undoAction = async () => {
            await apiManager.put(`/api/combos/${comboId}/preguntas/${preguntaId}/factor`, { factorMultiplicacion: factorPrevio || 'X' });
            await CombosManager.cargarCombos();
        };
        await doAction();
        if (window.UndoManager) window.UndoManager.record({ do: doAction, undo: undoAction, label: `Factor pregunta ${preguntaId} combo ${comboId}` });
        
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

// Actualizar notas de dirección del combo (en listado)
window.actualizarNotasDireccionCombo = async function(comboId, notas) {
    try {
        const previo = (CombosManager.ultimoListado || []).find(c => c.id === comboId);
        const notasPrevias = previo ? (previo.notasDireccion || '') : '';
        const doAction = async () => { await apiManager.put(`/api/combos/${comboId}`, { notasDireccion: notas }); };
        const undoAction = async () => { await apiManager.put(`/api/combos/${comboId}`, { notasDireccion: notasPrevias }); };
        await doAction();
        if (window.UndoManager) window.UndoManager.record({ do: doAction, undo: undoAction, label: `Notas combo ${comboId}` });
        Toastify({
            text: 'Notas de dirección actualizadas',
            duration: 2000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' }
        }).showToast();
    } catch (e) {
        Toastify({
            text: 'Error al actualizar notas: ' + e.message,
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

function getOpcionesTematicaCombo(tematicaActual) {
    let opciones = '';
    
    // Agregar todas las temáticas disponibles
    CombosManager.tematicas.forEach(tematica => {
        const selected = tematica === tematicaActual ? 'selected' : '';
        opciones += `<option value="${tematica}" ${selected}>${tematica}</option>`;
    });
    
    return opciones;
}

window.cambiarEstadoCombo = async function(id, nuevoEstado) {
    try {
        CombosManager.rememberScroll();
        CombosManager.lastFocusComboId = id;
        const previo = (CombosManager.ultimoListado || []).find(c => c.id === id);
        const estadoPrevio = previo ? previo.estado : null;
        const doAction = async () => {
            await apiManager.put(`/api/combos/${id}/estado?nuevoEstado=${encodeURIComponent(nuevoEstado)}`, null);
            await CombosManager.cargarCombos();
        };
        const undoAction = async () => {
            if (estadoPrevio) {
                await apiManager.put(`/api/combos/${id}/estado?nuevoEstado=${encodeURIComponent(estadoPrevio)}`, null);
                await CombosManager.cargarCombos();
            }
        };
        await doAction();
        if (window.UndoManager) window.UndoManager.record({ do: doAction, undo: undoAction, label: `Estado combo ${id}` });
        await CombosManager.cargarCombos();
        
        Toastify({
            text: `Estado cambiado a: ${nuevoEstado}`,
            duration: 3000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' }
        }).showToast();
        CombosManager.restoreScrollOrFocus();
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