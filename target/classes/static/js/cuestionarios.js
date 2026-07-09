// Módulo de gestión de cuestionarios
const CuestionariosManager = {
    cuestionarios: [],
    ultimoListado: [],
    tematicasGestionadas: [],
    paginaActual: 0,
    tamanioPagina: 25,
    totalCuestionarios: 0,
    totalPaginas: 0,
    cargando: false,
    lastScrollY: 0,
    lastFocusCuestionarioId: null,
    modoDestacado: false,

    obtenerIdDesdeUrl() {
        const id = new URLSearchParams(window.location.search).get('id');
        if (!id || !/^\d+$/.test(String(id).trim())) return null;
        return parseInt(id, 10);
    },

    limpiarIdUrl() {
        try {
            const url = new URL(window.location.href);
            url.searchParams.delete('id');
            window.history.replaceState({}, '', url.pathname + url.search + url.hash);
        } catch (_) {}
    },

    mostrarAvisoDestacado(id) {
        let aviso = document.getElementById('aviso-cuestionario-destacado');
        if (!aviso) {
            const contenedor = document.getElementById('tabla-cuestionarios-tabla')?.closest('.card-body')
                || document.getElementById('tabla-cuestionarios-tabla')?.parentElement;
            if (!contenedor) return;
            aviso = document.createElement('div');
            aviso.id = 'aviso-cuestionario-destacado';
            aviso.className = 'alert alert-info d-flex justify-content-between align-items-center py-2 mb-2';
            contenedor.insertBefore(aviso, contenedor.firstChild);
        }
        aviso.innerHTML = `
            <span><i class="fas fa-link me-1"></i> Vista directa del cuestionario <strong>#${id}</strong></span>
            <button type="button" class="btn btn-sm btn-outline-primary" onclick="CuestionariosManager.salirModoDestacado()">Ver listado completo</button>
        `;
        aviso.style.display = '';
    },

    ocultarAvisoDestacado() {
        const aviso = document.getElementById('aviso-cuestionario-destacado');
        if (aviso) aviso.style.display = 'none';
    },

    salirModoDestacado() {
        this.modoDestacado = false;
        this.ocultarAvisoDestacado();
        this.limpiarIdUrl();
        this.paginaActual = 0;
        return this.cargarCuestionarios(true);
    },

    async cargarDestacado(id) {
        try {
            if (!authManager.isAuthenticated()) return;
            this.cargando = true;
            this.mostrarEstadoCarga();
            const resp = await fetch(`/api/cuestionarios/${id}`, { headers: authManager.getAuthHeaders() });
            if (!resp.ok) throw new Error('Cuestionario no encontrado');
            const cuestionario = await resp.json();
            if (!cuestionario?.id) throw new Error('Cuestionario no encontrado');

            this.modoDestacado = true;
            this.cuestionarios = [cuestionario];
            this.ultimoListado = [cuestionario];
            this.totalCuestionarios = 1;
            this.totalPaginas = 1;
            this.paginaActual = 0;
            this.mostrarAvisoDestacado(id);
            await this.mostrarCuestionarios(this.cuestionarios);
            this.actualizarPaginacion();
            setTimeout(() => {
                const fila = document.querySelector(`tr.fila-cuestionario[data-id='${id}']`);
                if (fila) {
                    fila.classList.add('table-warning');
                    fila.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }
            }, 100);
        } catch (error) {
            this.modoDestacado = false;
            this.ocultarAvisoDestacado();
            Toastify({
                text: `Error: ${error.message}`,
                duration: 3000,
                close: true,
                gravity: 'top',
                position: 'right',
                style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
            }).showToast();
            await this.cargarCuestionarios(true);
        } finally {
            this.cargando = false;
        }
    },

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
                // Si la búsqueda es numérica, usar 'id', sino usar 'texto' para buscar en preguntas/respuestas
                if (busqueda) {
                    if (/^\d+$/.test(busqueda.trim())) {
                        params.append('id', busqueda);
                    } else {
                        params.append('texto', busqueda);
                    }
                }
                
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
        const paginacionElement = document.getElementById('paginacion-cuestionarios');

        if (this.modoDestacado && this.cuestionarios.length === 1) {
            if (infoElement) infoElement.textContent = `Cuestionario #${this.cuestionarios[0].id} (vista directa)`;
            if (paginacionElement) paginacionElement.innerHTML = '';
            return;
        }

        if (infoElement) {
            const inicio = (this.paginaActual * this.tamanioPagina) + 1;
            const fin = Math.min((this.paginaActual + 1) * this.tamanioPagina, this.totalCuestionarios);
            infoElement.textContent = `Mostrando ${inicio}-${fin} de ${this.totalCuestionarios} cuestionarios`;
        }

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
        if (this.modoDestacado) {
            this.modoDestacado = false;
            this.ocultarAvisoDestacado();
            this.limpiarIdUrl();
        }
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

    obtenerFiltrosActivos() {
        const estado = document.getElementById('filtro-estado-cuestionario')?.value || '';
        const tematica = document.getElementById('filtro-tematica-cuestionario')?.value || '';
        const subtema = document.getElementById('filtro-subtema-cuestionario')?.value || '';
        const busqueda = document.getElementById('buscar-cuestionario')?.value || '';
        return { estado, tematica, subtema, busqueda, hayFiltros: !!(estado || tematica || subtema || busqueda) };
    },

    async recargarConFiltros() {
        const filtros = this.obtenerFiltrosActivos();
        if (filtros.hayFiltros) {
            this.cuestionarios = [];
            await window.filtrarCuestionarios(false);
        } else {
            await this.cargarCuestionarios(true, true);
        }
    },

    normalizarEstado(estado) {
        return typeof estado === 'string' ? estado : (estado?.name || '');
    },

    aplicarParcheEnMemoria(id, parche) {
        const merge = (arr) => {
            if (!Array.isArray(arr)) return;
            const idx = arr.findIndex(x => x.id === id);
            if (idx >= 0) arr[idx] = { ...arr[idx], ...parche };
        };
        merge(this.cuestionarios);
        merge(this.ultimoListado);
    },

    aplicarEnMemoria(id, cuestionario) {
        const replace = (arr) => {
            if (!Array.isArray(arr)) return;
            const idx = arr.findIndex(x => x.id === id);
            if (idx >= 0) arr[idx] = cuestionario;
            else arr.unshift(cuestionario);
        };
        replace(this.cuestionarios);
        replace(this.ultimoListado);
    },

    cumpleFiltrosBasicos(cuestionario, filtros) {
        if (filtros.estado && this.normalizarEstado(cuestionario.estado) !== filtros.estado) return false;
        if (filtros.tematica && (cuestionario.tematica || '') !== filtros.tematica) return false;
        if (filtros.busqueda && /^\d+$/.test(filtros.busqueda.trim())) {
            return String(cuestionario.id) === filtros.busqueda.trim();
        }
        return true;
    },

    async sigueEnFiltro(cuestionario, filtros) {
        if (!filtros.hayFiltros) return true;
        if (!this.cumpleFiltrosBasicos(cuestionario, filtros)) return false;
        if (!filtros.subtema && !(filtros.busqueda && !/^\d+$/.test(filtros.busqueda.trim()))) return true;
        const params = new URLSearchParams({ page: 0, size: 100 });
        if (filtros.estado) params.append('estado', filtros.estado);
        if (filtros.tematica) params.append('tematica', filtros.tematica);
        if (filtros.subtema) params.append('subtema', filtros.subtema);
        if (filtros.busqueda && !/^\d+$/.test(filtros.busqueda.trim())) params.append('texto', filtros.busqueda);
        const response = await fetch(`/api/cuestionarios/filtrar?${params}`, { headers: authManager.getAuthHeaders() });
        if (!response.ok) return true;
        const data = await response.json();
        return (data.cuestionarios || []).some(c => c.id === cuestionario.id);
    },

    eliminarFilasDom(id) {
        document.querySelectorAll(
            `tr.fila-cuestionario[data-id="${id}"], tr.cuestionario-subtabla[data-cuestionario-id="${id}"], tr.separador-cuestionario[data-cuestionario-id="${id}"]`
        ).forEach(el => el.remove());
    },

    quitarDeListado(id) {
        this.cuestionarios = (this.cuestionarios || []).filter(c => c.id !== id);
        this.ultimoListado = (this.ultimoListado || []).filter(c => c.id !== id);
        if (this.totalCuestionarios > 0) this.totalCuestionarios--;
        this.actualizarPaginacion();
        this.eliminarFilasDom(id);
    },

    engancharEventosSubtablaCuestionario(subtr) {
        const filas = subtr.querySelectorAll('tbody tr[data-id]');
        filas.forEach(fila => {
            fila.addEventListener('click', (e) => {
                if (e.target.closest('button, input, select, textarea, a')) return;
                const pid = fila.getAttribute('data-id');
                if (pid) Utils.abrirEnNuevaPestana(`preguntas.html?id=${pid}`);
            });
        });
    },

    async cargarTematicasGestionadas() {
        try {
            const response = await fetch('/api/cuestionarios/tematicas', { headers: authManager.getAuthHeaders() });
            if (!response.ok) return this.tematicasGestionadas;
            this.tematicasGestionadas = await response.json();
            const filtroTematica = document.getElementById('filtro-tematica-cuestionario');
            if (filtroTematica) {
                const valorSeleccionado = filtroTematica.value || '';
                filtroTematica.innerHTML = '<option value="">Todas</option>';
                this.tematicasGestionadas.forEach(tematica => {
                    const option = document.createElement('option');
                    option.value = tematica;
                    option.textContent = tematica;
                    filtroTematica.appendChild(option);
                });
                if (valorSeleccionado) {
                    filtroTematica.value = valorSeleccionado;
                    if (filtroTematica.value !== valorSeleccionado) filtroTematica.value = '';
                }
            }
        } catch (error) {
            console.error('Error al cargar temáticas:', error);
        }
        return this.tematicasGestionadas;
    },

    mapearPreguntasPorSlot(c) {
        const preguntasPorSlot = {};
        const slotFromNivel = { '_1LS': '1LS', '_2NLS': '2NLS', '_3LS': '3LS', '_4NLS': '4NLS' };
        if (!Array.isArray(c?.preguntas)) return preguntasPorSlot;
        c.preguntas.forEach(pc => {
            if (!pc) return;
            if (pc.slot && ['1LS', '2NLS', '3LS', '4NLS'].includes(pc.slot)) {
                if (pc.pregunta) preguntasPorSlot[pc.slot] = pc.pregunta;
                return;
            }
            const p = pc.pregunta;
            if (!p) return;
            const nivel = typeof p.nivel === 'string' ? p.nivel : (p.nivel?.name || '');
            const slot = slotFromNivel[nivel];
            if (slot && !preguntasPorSlot[slot]) preguntasPorSlot[slot] = p;
        });
        return preguntasPorSlot;
    },

    contarPreguntasAsignadas(c) {
        return Object.keys(this.mapearPreguntasPorSlot(c)).length;
    },

    async obtenerCuestionarioDto(id) {
        const response = await fetch(`/api/cuestionarios/filtrar?page=0&size=1&id=${id}`, {
            headers: authManager.getAuthHeaders()
        });
        if (!response.ok) throw new Error('No se pudo obtener el cuestionario');
        const data = await response.json();
        const cuestionario = (data.cuestionarios || []).find(c => c.id === id) ?? (data.cuestionarios || [])[0];
        if (!cuestionario) throw new Error('Cuestionario no encontrado');
        return cuestionario;
    },

    renderCuestionarioEnTbody(tbody, c) {
        const tematicasGestionadas = this.tematicasGestionadas || [];
            const niveles = ['1LS','2NLS','3LS','4NLS'];
            const preguntasPorSlot = this.mapearPreguntasPorSlot(c);
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
            
            // Determinar icono y tooltip para reutilización
            let iconoReutilizado = '';
            if (c.reutilizadoDeJornadaId) {
                iconoReutilizado = `<span class="ms-2" title="Reutilizado de ${c.reutilizadoDeJornadaNombre || 'jornada ' + c.reutilizadoDeJornadaId}" style="cursor: help;">♻️</span>`;
                console.log(`[FRONT-CUEST] Cuest ${c.id} | estado=${c.estado} | jornada=${c.jornadaAsignada} | mostrarSelector=${!(c.jornadaAsignada && (c.estado === 'adjudicado' || c.estado === 'grabado'))}`);
            }
            
            // Determinar si debe mostrar "Asignado a jornada X" o el selector de estado
            // Solo mostrar "Asignado/Grabado" si está en estado adjudicado o grabado
            const estadoActual = typeof c.estado === 'string' ? c.estado : c.estado?.name || 'borrador';
            const estaReservado = c.jornadaAsignada && (estadoActual === 'adjudicado' || estadoActual === 'grabado');
            
            tr.innerHTML = `
                <td class="celda-numero-cuestionario">${c.id ?? ''}</td>
                <td>
                    <select class="form-select form-select-sm" onchange="cambiarTematicaCuestionario(${c.id}, this.value)">
                        ${opcionesTematicas}
                    </select>
                </td>
                <td>
                    ${estaReservado ? `<div class="text-muted">${(estadoActual === 'grabado') ? `Grabado en jornada ${c.jornadaAsignada}` : `Asignado a jornada ${c.jornadaAsignada}`}</div>` :
                    `<select class="form-select form-select-sm" onchange="cambiarEstadoCuestionario(${c.id}, this.value)">
                        ${getOpcionesEstadoCuestionario(estadoActual)}
                    </select>${iconoReutilizado}`}
                </td>
                <td>${this.contarPreguntasAsignadas(c)}</td>
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
            subtr.setAttribute('data-cuestionario-id', c.id);
            const puedeEditarNotas = authManager.hasRole('ROLE_ADMIN') || authManager.hasRole('ROLE_DIRECCION');
            
            // Generar exactamente 4 filas para las preguntas del cuestionario
            let filasPreguntas = '';
            for (let i = 1; i <= 4; i++) {
                const slotNivel = niveles[i - 1];
                const p = preguntasPorSlot[slotNivel];
                
                if (p) {
                    filasPreguntas += `<tr data-id="${p.id}" data-nivel="${slotNivel}" style="cursor:pointer;">
                        <td><span class='${CuestionariosManager.getNivelColor ? CuestionariosManager.getNivelColor(p.nivel) : ''}'>${slotNivel}</span></td>
                        <td>${p.pregunta ?? ''}</td>
                        <td>${p.respuesta ?? ''}</td>
                        <td>${p.datosExtra ?? ''}</td>
                        <td><button class='btn btn-sm btn-danger' onclick='event.stopPropagation();eliminarPreguntaDeCuestionario(${c.id}, "${slotNivel}")'><i class='fas fa-trash'></i></button></td>
                    </tr>`;
                } else {
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
            sep.setAttribute('data-cuestionario-id', c.id);
            sep.innerHTML = '<td colspan="6"></td>';
            tbody.appendChild(sep);
            this.engancharEventosSubtablaCuestionario(subtr);
    },

    async reemplazarFilasDom(id, cuestionario) {
        const tbody = document.getElementById('tabla-cuestionarios');
        if (!tbody) return;
        const existente = tbody.querySelector(`tr.fila-cuestionario[data-id="${id}"]`);
        let insertBefore = null;
        if (existente) {
            let node = existente;
            if (node.nextElementSibling?.classList.contains('cuestionario-subtabla')) node = node.nextElementSibling;
            if (node.nextElementSibling?.classList.contains('separador-cuestionario')) node = node.nextElementSibling;
            insertBefore = node.nextElementSibling;
            this.eliminarFilasDom(id);
        }
        const temp = document.createElement('tbody');
        this.renderCuestionarioEnTbody(temp, cuestionario);
        const frag = document.createDocumentFragment();
        while (temp.firstChild) frag.appendChild(temp.firstChild);
        if (insertBefore) tbody.insertBefore(frag, insertBefore);
        else tbody.appendChild(frag);
    },

    async refrescarFila(id) {
        try {
            const cuestionario = await this.obtenerCuestionarioDto(id);
            this.aplicarEnMemoria(id, cuestionario);
            const filtros = this.obtenerFiltrosActivos();
            if (filtros.hayFiltros && !(await this.sigueEnFiltro(cuestionario, filtros))) {
                this.quitarDeListado(id);
                return;
            }
            await this.reemplazarFilasDom(id, cuestionario);
        } catch (error) {
            console.error('Error al refrescar fila de cuestionario:', error);
        }
    },

    async insertarOActualizarFila(id) {
        const tbody = document.getElementById('tabla-cuestionarios');
        if (!tbody) return;
        if (tbody.querySelector(`tr.fila-cuestionario[data-id="${id}"]`)) {
            await this.refrescarFila(id);
            return;
        }
        try {
            const cuestionario = await this.obtenerCuestionarioDto(id);
            const filtros = this.obtenerFiltrosActivos();
            if (filtros.hayFiltros && !(await this.sigueEnFiltro(cuestionario, filtros))) return;
            this.aplicarEnMemoria(id, cuestionario);
            const temp = document.createElement('tbody');
            this.renderCuestionarioEnTbody(temp, cuestionario);
            const frag = document.createDocumentFragment();
            while (temp.firstChild) frag.appendChild(temp.firstChild);
            const vacio = tbody.querySelector('tr td.text-center');
            if (vacio && vacio.textContent.includes('No hay cuestionarios')) vacio.closest('tr')?.remove();
            tbody.insertBefore(frag, tbody.firstChild);
            this.totalCuestionarios++;
            this.actualizarPaginacion();
        } catch (error) {
            console.error('Error al insertar fila de cuestionario:', error);
        }
    },

    async mostrarCuestionarios(cuestionarios) {
        const tbody = document.getElementById('tabla-cuestionarios');
        if (!tbody) {
            console.error('No se encontró el elemento tabla-cuestionarios');
            return;
        }
        await this.cargarTematicasGestionadas();
        tbody.innerHTML = '';
        if (!Array.isArray(cuestionarios) || cuestionarios.length === 0) {
            const tr = document.createElement('tr');
            tr.innerHTML = '<td colspan="5" class="text-center">No hay cuestionarios</td>';
            tbody.appendChild(tr);
            return;
        }
        cuestionarios.forEach(c => this.renderCuestionarioEnTbody(tbody, c));
        tbody.querySelectorAll('.enlace-pregunta').forEach(a => {
            a.addEventListener('click', function(e) {
                e.preventDefault();
                const pid = this.dataset.id;
                if (pid) Utils.abrirEnNuevaPestana(`preguntas.html?id=${pid}`);
            });
        });
        if (!this.modoDestacado) {
            const params = new URLSearchParams(window.location.search);
            const idDestacado = params.get('id');
            if (idDestacado) {
                setTimeout(() => {
                    const fila = tbody.querySelector(`tr.fila-cuestionario[data-id='${idDestacado}']`);
                    if (fila) {
                        fila.classList.add('table-warning');
                        fila.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    } else {
                        this.cargarDestacado(parseInt(idDestacado, 10));
                    }
                }, 300);
            }
        }
        setTimeout(() => this.restoreScrollOrFocus(), 0);
    },

    getNivelColor(nivel) {
        if (["_2NLS", "_4NLS", "_5NLS", "2NLS", "4NLS"].includes(nivel)) return 'text-danger fw-bold';
        if (["_1LS", "_3LS", "_5LS", "1LS", "3LS"].includes(nivel)) return 'text-success fw-bold';
        return '';
    },
};

async function inicializarCuestionarios() {
    const idUrl = CuestionariosManager.obtenerIdDesdeUrl();
    if (idUrl) {
        CuestionariosManager.cargarTematicasGestionadas().catch(() => {});
        await CuestionariosManager.cargarDestacado(idUrl);
    } else {
        await CuestionariosManager.cargarCuestionarios();
    }
}

document.addEventListener('DOMContentLoaded', inicializarCuestionarios);

// IDs de los campos para las preguntas normales (solo niveles 1-4)
const normales = ['1LS','2NLS','3LS','4NLS'];

let selectorPreguntaContext = { nivel: null, factor: null, inputId: null, textoId: null };

async function mostrarFormularioCuestionario() {
    const form = document.getElementById('form-cuestionario');
    if (form) form.reset();
    const titulo = document.getElementById('modal-cuestionario-titulo');
    if (titulo) titulo.textContent = 'Nuevo Cuestionario';

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
        estadoSelect.value = 'borrador';
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

    ['buscador-id', 'buscador-texto', 'buscador-tematica-select', 'buscador-nivel-cuestionario', 'buscador-estado'].forEach(id => {
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
    const nivelSelect = document.getElementById('buscador-nivel-cuestionario');
    if (nivelSelect) nivelSelect.value = '';
    const estadoSelect = document.getElementById('buscador-estado');
    if (estadoSelect) estadoSelect.value = 'aprobada';
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
    const estadoSel = (document.getElementById('buscador-estado')?.value || 'todos').trim().toLowerCase();
    const filtroNivel = document.getElementById('buscador-nivel-cuestionario')?.value || '';

    try {
        const params = new URLSearchParams();
        let url = '';
        params.set('page', page);
        params.set('size', 20);

        if (id) {
            // Búsqueda exacta por ID
            params.set('id', id);
            if (tematica) params.set('tematica', tematica);
            if (filtroNivel) params.set('nivel', filtroNivel);
            url = `/api/preguntas/buscar?${params.toString()}`;
        } else {
            if (texto) params.set('texto', texto); // OR pregunta/respuesta
            if (tematica) params.set('tematica', tematica);
            if (filtroNivel) params.set('nivel', filtroNivel);
            // Estado: por defecto 'todos' (aprobada + verificada). Si se elige uno, aplicarlo.
            if (estadoSel === 'aprobada' || estadoSel === 'verificada') {
                params.set('estado', estadoSel);
            } else {
                // Enviar CSV para que el backend filtre ambos estados
                params.set('estado', 'aprobada,verificada');
            }
            url = `/api/preguntas/filtrar?${params.toString()}`;
        }

        console.log('[FRONT][CUEST] URL de búsqueda:', url);
        const resp = await fetch(url, { headers: authManager.getAuthHeaders() });
        if (!resp.ok) throw new Error('Error al buscar preguntas');
        const data = await resp.json();
        let preguntas = data.content || [];
        // Filtro por estado en cliente si se pide 'todos'
        if (estadoSel === 'todos') {
            preguntas = preguntas.filter(p => p.estado === 'aprobada' || p.estado === 'verificada');
        } else if (estadoSel === 'aprobada' || estadoSel === 'verificada') {
            preguntas = preguntas.filter(p => p.estado === estadoSel);
        }

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
                pregunta.subtema,
                pregunta.estado,
                pregunta.nivel
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

function seleccionarPreguntaModal(id, pregunta, tematica, respuesta, subtema, estado = null, nivel = null) {
    console.log('[FRONT] seleccionarPreguntaModal llamada con:', {id, pregunta, tematica, respuesta, subtema, estado, nivel, selectorPreguntaContext});
    // --- NUEVO: Si hay contexto de añadir pregunta a cuestionario, hacer petición AJAX ---
    if (window.contextoAnadirPregunta) {
        const { cuestionarioId, nivel: nivelEsperado } = window.contextoAnadirPregunta;
        const factorMultiplicacion = 1;

        // Función para añadir la pregunta al cuestionario
        const doAdd = async () => {
            await apiManager.post(`/api/cuestionarios/${cuestionarioId}/preguntas`, { preguntaId: id, factorMultiplicacion }, { headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' } });
            await CuestionariosManager.refrescarFila(cuestionarioId);
        };
        const undoDelete = async () => {
            await apiManager.delete(`/api/cuestionarios/${cuestionarioId}/preguntas/${id}`, { headers: authManager.getAuthHeaders() });
            await CuestionariosManager.refrescarFila(cuestionarioId);
        };

        const nivelActual = nivel || '';
        const slotToEnum = { '1LS': '_1LS', '2NLS': '_2NLS', '3LS': '_3LS', '4NLS': '_4NLS' };
        const nivelEsperadoEspecifico = slotToEnum[nivelEsperado] || nivelEsperado;

        // Si el nivel no coincide (incluye 5LS/5NLS), avisar y cambiar al del hueco elegido
        if (nivelActual !== nivelEsperadoEspecifico) {
            const mensajeNivel = 
                `Atención: La pregunta seleccionada tiene nivel "${Utils.formatearNivel(nivelActual)}" pero el hueco del cuestionario es "${Utils.formatearNivel(nivelEsperadoEspecifico)}".\n\n` +
                `Pregunta: "${pregunta}"\n\n` +
                `Si continúas, se cambiará automáticamente el nivel de esta pregunta a "${Utils.formatearNivel(nivelEsperadoEspecifico)}" para que sea consistente con el cuestionario.\n\n` +
                `¿Quieres continuar y cambiar el nivel?`;
            
            const continuar = window.confirm(mensajeNivel);
            if (!continuar) {
                // Usuario canceló, cerrar modal y limpiar contexto
                window.contextoAnadirPregunta = null;
                const modal = bootstrap.Modal.getInstance(document.getElementById('modal-selector-pregunta'));
                if (modal) modal.hide();
                return;
            }
            
            // Usuario aceptó, cambiar el nivel de la pregunta primero y luego añadirla
            (async () => {
                try {
                    await fetch(`/api/preguntas/${id}`, {
                        method: 'PUT',
                        headers: {
                            ...authManager.getAuthHeaders(),
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({
                            id: id,
                            nivel: nivelEsperadoEspecifico
                        })
                    });
                    Toastify({ 
                        text: `Nivel de la pregunta cambiado de "${Utils.formatearNivel(nivelActual)}" a "${Utils.formatearNivel(nivelEsperadoEspecifico)}"`, 
                        duration: 3000, 
                        close: true, 
                        gravity: 'top', 
                        position: 'right', 
                        style: Utils.estiloToastExito()
                    }).showToast();
                    
                    // Ahora añadir la pregunta
                    await doAdd();
                    
                    if (window.UndoManager) {
                        window.UndoManager.record({ do: doAdd, undo: undoDelete, label: `Añadir pregunta ${id} a cuestionario ${cuestionarioId}` });
                    }
                    Toastify({ text: 'Pregunta añadida al cuestionario', duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' } }).showToast();
                    if (estado && String(estado).toLowerCase() === 'verificada') {
                        Toastify({ text: 'Aviso: La pregunta estaba VERIFICADA y se ha marcado como APROBADA automáticamente.', duration: 4000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #ffc107, #ff9800)' } }).showToast();
                    }
                } catch (err) {
                    console.error('Error al cambiar nivel o añadir pregunta', id, err);
                    Toastify({
                        text: `Error: ${err.message || err}`,
                        duration: 4000,
                        close: true,
                        gravity: 'top',
                        position: 'right',
                        style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
                    }).showToast();
                } finally {
                    window.contextoAnadirPregunta = null;
                    const modal = bootstrap.Modal.getInstance(document.getElementById('modal-selector-pregunta'));
                    if (modal) modal.hide();
                }
            })();
            return;
        }

        // Nivel coincide, añadir directamente
        doAdd()
        .then(() => {
            if (window.UndoManager) {
                window.UndoManager.record({ do: doAdd, undo: undoDelete, label: `Añadir pregunta ${id} a cuestionario ${cuestionarioId}` });
            }
            Toastify({ text: 'Pregunta añadida al cuestionario', duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' } }).showToast();
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

function obtenerNombreAutorNota() {
    const nombreAuth = authManager?.currentUser?.nombre;
    if (nombreAuth && String(nombreAuth).trim()) return String(nombreAuth).trim();
    try {
        const guardado = JSON.parse(localStorage.getItem('usuario') || '{}');
        if (guardado?.nombre && String(guardado.nombre).trim()) return String(guardado.nombre).trim();
    } catch (_) {}
    return 'usuario';
}

function textoConPrefijoAutor(texto, autor) {
    const limpio = (texto || '').trim();
    if (!limpio) return '';
    if (/^\[[^\]]+\]\s*/.test(limpio)) return limpio;
    return `[${autor}] ${limpio}`;
}

function construirNotasDireccionParaGuardar(notasRaw, notasOriginales, esEdicion) {
    const actuales = notasRaw || '';
    const originales = notasOriginales || '';
    if (!esEdicion) return textoConPrefijoAutor(actuales, obtenerNombreAutorNota());

    if (actuales === originales) return actuales;
    if (!actuales.trim()) return '';

    const autor = obtenerNombreAutorNota();
    const originalesTrimEnd = originales.replace(/\s+$/, '');
    const actualesTrimEnd = actuales.replace(/\s+$/, '');

    if (!originalesTrimEnd) return textoConPrefijoAutor(actualesTrimEnd, autor);

    if (actualesTrimEnd.startsWith(originalesTrimEnd)) {
        const añadido = actualesTrimEnd.slice(originalesTrimEnd.length).trim();
        if (!añadido) return originalesTrimEnd;
        const añadidoConAutor = textoConPrefijoAutor(añadido, autor);
        return `${originalesTrimEnd}\n${añadidoConAutor}`;
    }

    return textoConPrefijoAutor(actualesTrimEnd, autor);
}

async function guardarCuestionario() {
    CuestionariosManager.rememberScroll();
    let preguntasNormales = [];
    const preguntasPorSlot = {};
    normales.forEach(nivel => {
        const element = document.getElementById(`pregunta-${nivel}`);
        const id = element ? element.value : '';
        if (id) {
            const numId = Number(id);
            preguntasNormales.push(numId);
            preguntasPorSlot[nivel] = numId;
        }
    });
    
    console.log('🔍 [FRONTEND] Preguntas seleccionadas:', preguntasNormales);

    // Bloquear guardado si hay preguntas repetidas en distintos niveles
    const slotsPorPregunta = new Map();
    Object.entries(preguntasPorSlot).forEach(([slot, preguntaId]) => {
        const key = Number(preguntaId);
        if (!slotsPorPregunta.has(key)) slotsPorPregunta.set(key, []);
        slotsPorPregunta.get(key).push(slot);
    });

    const repetidas = Array.from(slotsPorPregunta.entries())
        .filter(([, slots]) => slots.length > 1)
        .map(([id, slots]) => ({ id, slots }));

    if (repetidas.length > 0) {
        const detalle = repetidas
            .map((r) => {
                const niveles = r.slots.map((s) => Utils.formatearNivel(s)).join(' y ');
                return `Pregunta ${r.id}: ${niveles}`;
            })
            .join(' | ');

        Toastify({
            text: `Tienes preguntas iguales en la posición/nivel del cuestionario: ${detalle}`,
            duration: 5000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
        }).showToast();
        return;
    }
    
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
    
    // Antes de guardar: comprobar estado y nivel real de las preguntas seleccionadas
    try {
        const detallesPreguntas = await Promise.all(
            preguntasNormales.map(async (id) => {
                try {
                    const resp = await fetch(`/api/preguntas/${id}`, {
                        headers: authManager.getAuthHeaders()
                    });
                    if (!resp.ok) return null;
                    return await resp.json();
                } catch {
                    return null;
                }
            })
        );

        const detallesPorId = {};
        (detallesPreguntas || []).forEach(p => {
            if (p && p.id != null) {
                detallesPorId[p.id] = p;
            }
        });

        // 1) Aviso por preguntas en estado VERIFICADA (se promocionan a APROBADA)
        const preguntasVerificadas = (detallesPreguntas || []).filter(p =>
            p && typeof p.estado === 'string' &&
            String(p.estado).toLowerCase() === 'verificada'
        );

        if (preguntasVerificadas.length > 0) {
            const lista = preguntasVerificadas
                .map(p => `- ${p.id}: ${p.pregunta || '(sin texto)'}`)
                .join('\n');

            const mensajeConfirmacion =
                'Atención: vas a crear/editar un cuestionario que utiliza preguntas en estado VERIFICADA (no APROBADA todavía):\n\n' +
                lista +
                '\n\n' +
                'Si continúas, estas preguntas se marcarán automáticamente como APROBADAS y quedarán reservadas para este cuestionario.\n\n' +
                '¿Quieres continuar y cambiar su estado a APROBADA?';

            const continuar = window.confirm(mensajeConfirmacion);
            if (!continuar) {
                // El usuario ha cancelado explícitamente tras el aviso
                return;
            }
        }

        // 2) Aviso por preguntas cuyo NIVEL no coincide con el hueco del cuestionario
        const slotToNivelEnum = {
            '1LS': '_1LS',
            '2NLS': '_2NLS',
            '3LS': '_3LS',
            '4NLS': '_4NLS'
        };

        const desajustesNivel = [];
        Object.entries(preguntasPorSlot).forEach(([slot, id]) => {
            const detalle = detallesPorId[id];
            if (!detalle || !detalle.nivel) return;

            const targetNivel = slotToNivelEnum[slot] || null;
            const actualNivel = typeof detalle.nivel === 'string'
                ? detalle.nivel
                : (detalle.nivel.name || detalle.nivel);

            if (targetNivel && actualNivel && actualNivel !== targetNivel) {
                desajustesNivel.push({
                    slot,
                    id,
                    actualNivel,
                    targetNivel,
                    pregunta: detalle.pregunta || ''
                });
            }
        });

        if (desajustesNivel.length > 0) {
            const listaNiveles = desajustesNivel
                .map(m => `- Pregunta ${m.id}: "${m.pregunta}" (nivel actual: ${Utils.formatearNivel(m.actualNivel)}, hueco: ${Utils.formatearNivel(m.targetNivel)})`)
                .join('\n');

            const mensajeNiveles =
                'Atención: algunas preguntas no tienen el mismo NIVEL que el hueco del cuestionario donde las estás colocando:\n\n' +
                listaNiveles +
                '\n\n' +
                'Si continúas, se cambiará automáticamente el NIVEL de esas preguntas al del hueco indicado para que el cuestionario sea consistente.\n\n' +
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
                    console.error('Error al cambiar nivel de la pregunta', m.id, err);
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
        console.warn('No se pudo comprobar el estado de las preguntas antes de guardar el cuestionario:', e);
        // En caso de error en esta verificación previa, seguimos con el flujo normal
    }
    
    const cuestionarioIdElement = document.getElementById('cuestionario-id');
    const tematicaElement = document.getElementById('cuestionario-tematica');
    const estadoElement = document.getElementById('cuestionario-estado');
    const notasElement = document.getElementById('cuestionario-notas');
    
    const cuestionarioId = cuestionarioIdElement ? cuestionarioIdElement.value : '';
    const tematica = tematicaElement ? tematicaElement.value : '';
    const estadoSeleccionado = estadoElement ? estadoElement.value : '';
    const esEdicion = !!cuestionarioId;
    const notasOriginales = notasElement ? (notasElement.dataset.originalNotas || '') : '';
    const notasDireccion = notasElement
        ? construirNotasDireccionParaGuardar(notasElement.value, notasOriginales, esEdicion)
        : '';
    
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
        const idFinalGuardado = (data && (data.id || data.ID)) || (cuestionarioId || null);
        if (idFinalGuardado) await CuestionariosManager.insertarOActualizarFila(Number(idFinalGuardado));
        if (notasElement) notasElement.dataset.originalNotas = notasDireccion || '';
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
        await EditLockManager.tryAcquire('CUESTIONARIO', id);
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
                asignadoDiv.textContent = (cuestionario.estado === 'grabado')
                    ? `Grabado en jornada ${cuestionario.jornadaAsignada}`
                    : `Asignado a jornada ${cuestionario.jornadaAsignada}`;
                asignadoDiv.classList.remove('d-none');
            } else {
                asignadoDiv.classList.add('d-none');
            }
        }

        // Asignar temática actual si existe
        const tematicaSelect = document.getElementById('cuestionario-tematica');
        if (tematicaSelect) tematicaSelect.value = (cuestionario.tematica || '');
        const notasElement = document.getElementById('cuestionario-notas');
        if (notasElement) {
            const notasActuales = cuestionario.notasDireccion || '';
            notasElement.value = notasActuales;
            notasElement.dataset.originalNotas = notasActuales;
        }

        // Cambiar título del modal con verificación
        const tituloElement = document.getElementById('modal-cuestionario-titulo');
        if (tituloElement) tituloElement.innerText = 'Editar Cuestionario';
        
        // Mostrar modal
        const modalElement = document.getElementById('modal-cuestionario');
        if (modalElement) {
            const modal = new bootstrap.Modal(modalElement);
            modal.show();
            EditLockManager.startSession({
                entityType: 'CUESTIONARIO',
                entityId: id,
                modalSelector: '#modal-cuestionario',
                onExpire: () => guardarCuestionario()
            });
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
            throw new Error(await Utils.mensajeDesdeResponse(resp, 'eliminar cuestionarios'));
        }
        
        Toastify({ text: 'Cuestionario eliminado', duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' } }).showToast();
        CuestionariosManager.quitarDeListado(id);
        CuestionariosManager.restoreScrollOrFocus();
    } catch (e) {
        console.error('Error al eliminar cuestionario:', e);
        Toastify({ text: Utils.mensajeErrorApi(e, 'eliminar cuestionarios'), duration: 5000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' } }).showToast();
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
        CuestionariosManager.rememberScroll();
        CuestionariosManager.lastFocusCuestionarioId = cuestionarioId;
        // Preparar undo/redo: localizar pregunta en ese slot
        const cuest = CuestionariosManager.ultimoListado?.find(c => c.id === cuestionarioId);
        let preguntaId = null;
        if (cuest && Array.isArray(cuest.preguntas)) {
            const pc = cuest.preguntas.find(p => p.slot === slot);
            if (pc && pc.pregunta && pc.pregunta.id) preguntaId = pc.pregunta.id;
        }
        const doDelete = async () => {
            await apiManager.delete(`/api/cuestionarios/${cuestionarioId}/preguntas/slot/${slot}`, { headers: authManager.getAuthHeaders() });
            await CuestionariosManager.refrescarFila(cuestionarioId);
        };
        const undoAdd = async () => {
            if (preguntaId) {
                await apiManager.post(`/api/cuestionarios/${cuestionarioId}/preguntas`, { preguntaId, factorMultiplicacion: 1 }, { headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' } });
                await CuestionariosManager.refrescarFila(cuestionarioId);
            }
        };
        await doDelete();
        if (window.UndoManager) {
            window.UndoManager.record({ do: doDelete, undo: undoAdd, label: `Quitar pregunta slot ${slot} de cuestionario ${cuestionarioId}` });
        }
        Toastify({ text: 'Pregunta eliminada del cuestionario', duration: 3000, close: true, gravity: 'top', position: 'right', style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' } }).showToast();
        CuestionariosManager.restoreScrollOrFocus();
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
const _oldMostrar = CuestionariosManager.mostrarCuestionarios;
CuestionariosManager.mostrarCuestionarios = function(cuestionarios) {
    CuestionariosManager.ultimoListado = cuestionarios;
    _oldMostrar.call(this, cuestionarios);
    if (typeof SyncMonitor !== 'undefined' && Array.isArray(cuestionarios)) {
        SyncMonitor.resetFromVisible(cuestionarios.map(c => ({
            entityType: 'CUESTIONARIO',
            entityId: c.id,
            version: c.version || 0,
            label: `Cuestionario ${c.id}`
        })));
    }
}

// Funciones de filtrado
window.filtrarCuestionarios = async function(resetear = true) {
    console.log(`🔍 [FILTRAR] Iniciando filtrarCuestionarios con resetear=${resetear}`);
    console.log(`🔍 [FILTRAR] Estado actual: cargando=${CuestionariosManager.cargando}, paginaActual=${CuestionariosManager.paginaActual}, totalPaginas=${CuestionariosManager.totalPaginas}`);
    
    // No verificamos si está cargando cuando viene de cargarMasCuestionarios (resetear=false)
    // porque cargarMasCuestionarios ya establece cargando=true antes de llamar a esta función
    
    try {
        if (CuestionariosManager.modoDestacado) {
            CuestionariosManager.modoDestacado = false;
            CuestionariosManager.ocultarAvisoDestacado();
            CuestionariosManager.limpiarIdUrl();
        }
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
        // Si la búsqueda es numérica, usar 'id', sino usar 'texto' para buscar en preguntas/respuestas
        if (busqueda) {
            if (/^\d+$/.test(busqueda.trim())) {
                params.append('id', busqueda);
            } else {
                params.append('texto', busqueda);
            }
        }

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
    if (CuestionariosManager.modoDestacado) {
        CuestionariosManager.modoDestacado = false;
        CuestionariosManager.ocultarAvisoDestacado();
        CuestionariosManager.limpiarIdUrl();
    }
    document.getElementById('filtro-estado-cuestionario').value = '';
    document.getElementById('filtro-tematica-cuestionario').value = '';
    document.getElementById('buscar-cuestionario').value = '';
    CuestionariosManager.paginaActual = 0;
    CuestionariosManager.cargarCuestionarios(true);
};

window.actualizarNotasDireccion = async function(cuestionarioId, notas) {
    try {
        CuestionariosManager.rememberScroll();
        CuestionariosManager.lastFocusCuestionarioId = cuestionarioId;
        // Obtener valor anterior desde el listado
        const previo = (CuestionariosManager.ultimoListado || []).find(c => c.id === cuestionarioId);
        const notasPrevias = previo ? (previo.notasDireccion || '') : '';
        const notasConAutor = construirNotasDireccionParaGuardar(notas, notasPrevias, true);
        const doAction = async () => {
            await apiManager.put(`/api/cuestionarios/${cuestionarioId}/notas-direccion`, { notasDireccion: notasConAutor });
            CuestionariosManager.aplicarParcheEnMemoria(cuestionarioId, { notasDireccion: notasConAutor });
        };
        const undoAction = async () => {
            await apiManager.put(`/api/cuestionarios/${cuestionarioId}/notas-direccion`, { notasDireccion: notasPrevias });
            CuestionariosManager.aplicarParcheEnMemoria(cuestionarioId, { notasDireccion: notasPrevias });
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
        CuestionariosManager.restoreScrollOrFocus();
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

function cuestionarioCompletoParaAprobar(cuestionario) {
    const normales = ['1LS', '2NLS', '3LS', '4NLS'];
    const porSlot = CuestionariosManager.mapearPreguntasPorSlot(cuestionario || {});
    return normales.every(slot => !!porSlot[slot]);
}

function comboCompletoParaAprobar(combo) {
    const total = (combo?.preguntas && combo.preguntas.length) || 0;
    return total === 3;
}

window.cambiarEstadoCuestionario = async function(id, nuevoEstado) {
    try {
        CuestionariosManager.rememberScroll();
        CuestionariosManager.lastFocusCuestionarioId = id;
        const previo = (CuestionariosManager.ultimoListado || []).find(c => c.id === id);
        const estadoPrevio = previo ? previo.estado : null;

        if (nuevoEstado === 'aprobado' && !cuestionarioCompletoParaAprobar(previo)) {
            Toastify({
                text: 'No se puede aprobar: el cuestionario debe tener las 4 preguntas (1LS, 2NLS, 3LS, 4NLS)',
                duration: 4000,
                close: true,
                gravity: 'top',
                position: 'right',
                style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
            }).showToast();
            await CuestionariosManager.refrescarFila(id);
            return;
        }

        const doAction = async () => {
            await apiManager.put(`/api/cuestionarios/${id}/estado?nuevoEstado=${encodeURIComponent(nuevoEstado)}`, null);
            await CuestionariosManager.refrescarFila(id);
        };
        const undoAction = async () => {
            if (estadoPrevio) {
                await apiManager.put(`/api/cuestionarios/${id}/estado?nuevoEstado=${encodeURIComponent(estadoPrevio)}`, null);
                await CuestionariosManager.refrescarFila(id);
            }
        };
        await doAction();
        if (window.UndoManager) window.UndoManager.record({ do: doAction, undo: undoAction, label: `Estado cuestionario ${id}` });
        
        Toastify({
            text: `Estado cambiado a: ${nuevoEstado}`,
            duration: 3000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' }
        }).showToast();
        CuestionariosManager.restoreScrollOrFocus();
    } catch (error) {
        await CuestionariosManager.refrescarFila(id);
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
        CuestionariosManager.rememberScroll();
        CuestionariosManager.lastFocusCuestionarioId = id;
        const previo = (CuestionariosManager.ultimoListado || []).find(c => c.id === id);
        const tematicaPrevia = previo ? (previo.tematica || '') : '';
        const doAction = async () => {
            await apiManager.put(`/api/cuestionarios/${id}/tematica`, { tematica: nuevaTematica });
            await CuestionariosManager.refrescarFila(id);
        };
        const undoAction = async () => {
            await apiManager.put(`/api/cuestionarios/${id}/tematica`, { tematica: tematicaPrevia });
            await CuestionariosManager.refrescarFila(id);
        };
        await doAction();
        if (window.UndoManager) window.UndoManager.record({ do: doAction, undo: undoAction, label: `Temática cuestionario ${id}` });
        
        Toastify({
            text: `Temática cambiada a: ${nuevaTematica || 'Genérico'}`,
            duration: 3000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' }
        }).showToast();
        CuestionariosManager.restoreScrollOrFocus();
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
                let errorText = await response.text();
                if (response.status === 403) {
                    throw new Error('No tienes permisos para añadir temáticas en cuestionarios. Esta acción está permitida solo para los roles autorizados.');
                }
                try {
                    const parsed = JSON.parse(errorText);
                    errorText = parsed.message || parsed.error || errorText;
                } catch (parseError) {
                    console.debug('No se pudo parsear el error de añadir temática:', parseError);
                }
                throw new Error(errorText || 'No se pudo añadir la temática');
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