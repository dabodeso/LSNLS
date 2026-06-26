// Módulo de gestión de preguntas
const PreguntasManager = {
    preguntas: [],
    paginaActual: 0,
    tamanioPagina: 25,
    totalPreguntas: 0,
    totalPaginas: 0,
    cargando: false,
    lastScrollY: 0,
    filtros: {
        tematica: '',
        nivel: '',
        estado: '',
        subtema: '',
        texto: '' // busca en pregunta y respuesta
    },
    orden: {
        columna: null,
        asc: false
    },

    /** Mismo orden que el filtro #filtro-estado en preguntas.html */
    ORDEN_ESTADOS_PREGUNTA: [
        'borrador', 'para_verificar', 'verificada', 'revisar', 'corregir',
        'para_aprobar', 'rechazada', 'aprobada', 'usada'
    ],

    formatearEstadoPregunta(estado) {
        if (estado === 'para_verificar') return 'Para verificar';
        if (estado === 'para_aprobar') return 'Para aprobar';
        if (!estado) return '';
        return estado.charAt(0).toUpperCase() + estado.slice(1);
    },

    ordenarEstadosPregunta(estados) {
        const unicos = [...new Set(estados)];
        return unicos.sort((a, b) => {
            const ia = this.ORDEN_ESTADOS_PREGUNTA.indexOf(a);
            const ib = this.ORDEN_ESTADOS_PREGUNTA.indexOf(b);
            return (ia === -1 ? 999 : ia) - (ib === -1 ? 999 : ib);
        });
    },

    leerFiltrosDesdeDom() {
        return {
            estado: document.getElementById('filtro-estado')?.value || '',
            autoria: document.getElementById('filtro-autoria')?.value || '',
            nivel: document.getElementById('filtro-nivel')?.value || '',
            tematica: document.getElementById('filtro-tematica')?.value || '',
            subtema: document.getElementById('filtro-subtema')?.value || '',
            texto: document.getElementById('filtro-texto')?.value?.trim() || '',
        };
    },

    tieneFiltrosActivos(filtros = this.filtros) {
        const f = filtros || {};
        return !!(f.estado || f.autoria || f.nivel || f.tematica || f.subtema || f.texto);
    },

    construirParamsFiltrar(filtros, page = this.paginaActual) {
        const params = new URLSearchParams({
            page: String(page),
            size: String(this.tamanioPagina),
            sortBy: this.orden.columna || 'id',
            sortDir: this.orden.asc ? 'asc' : 'desc',
        });
        if (filtros.estado) params.append('estado', filtros.estado);
        if (filtros.autoria) params.append('autoria', filtros.autoria);
        if (filtros.nivel) params.append('nivel', filtros.nivel);
        if (filtros.tematica) params.append('tematica', filtros.tematica);
        if (filtros.subtema) params.append('subtema', filtros.subtema);
        if (filtros.texto) params.append('texto', filtros.texto);
        return params;
    },

    async cargarPreguntas(resetear = true) {
        try {
            this.lastScrollY = window.scrollY || window.pageYOffset || 0;
            console.log('🔄 [CARGAR] Iniciando carga de preguntas, resetear:', resetear);
            console.log('🔄 [CARGAR] Estado actual - paginaActual:', this.paginaActual, 'preguntas.length:', this.preguntas.length);
            
            // Inicializar estado de ordenamiento si no existe
            if (!this.orden.columna) {
                this.orden.columna = 'id';
                this.orden.asc = false;
                console.log('🔄 [CARGAR] Inicializando ordenamiento por defecto - columna: id, asc: false (más recientes primero)');
            }
            
            if (!authManager.isAuthenticated()) {
                console.error('Usuario no autenticado');
                return;
            }

            if (resetear) {
                this.paginaActual = 0;
                this.preguntas = [];
                console.log('🔄 [CARGAR] Reset completado - paginaActual:', this.paginaActual, 'preguntas.length:', this.preguntas.length);
            }

            this.cargando = true;
            this.mostrarEstadoCarga();

            const params = new URLSearchParams({
                page: this.paginaActual,
                size: this.tamanioPagina,
                sortBy: this.orden.columna || 'id',
                sortDir: this.orden.asc ? 'asc' : 'desc'
            });

            console.log('🔄 [CARGAR] Parámetros de consulta:', params.toString());
            console.log('🔄 [CARGAR] URL:', `/api/preguntas?${params}`);

            const response = await fetch(`/api/preguntas?${params}`, {
                headers: authManager.getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error('Error al cargar las preguntas');
            }

            const data = await response.json();
            console.log('✅ [CARGAR] Respuesta del servidor:', data);
            console.log('✅ [CARGAR] Elementos recibidos:', data.content.length);
            console.log('✅ [CARGAR] Total elementos:', data.totalElements);
            console.log('✅ [CARGAR] Total páginas:', data.totalPages);
            console.log('✅ [CARGAR] Página actual:', data.number);
            
            // Log detallado de la primera pregunta para verificar codificación
            if (data.content.length > 0) {
                const primera = data.content[0];
                console.log('🔍 [CARGAR] Muestra primera pregunta recibida:');
                console.log('  - ID:', primera.id);
                console.log('  - Pregunta:', primera.pregunta);
                console.log('  - Autor:', primera.autor);
                console.log('  - Bytes de pregunta:', Array.from(primera.pregunta).map(c => c.charCodeAt(0)));
            }
            
            if (resetear) {
                this.preguntas = data.content;
                console.log('✅ [CARGAR] Preguntas reseteadas, nueva longitud:', this.preguntas.length);
            } else {
                const longitudAnterior = this.preguntas.length;
                this.preguntas = [...this.preguntas, ...data.content];
                console.log('✅ [CARGAR] Preguntas añadidas, longitud anterior:', longitudAnterior, 'nueva longitud:', this.preguntas.length);
            }
            
            this.totalPreguntas = data.totalElements;
            this.totalPaginas = data.totalPages;
            this.paginaActual = data.number;
            
            console.log('✅ [CARGAR] Estado finalizado - totalPreguntas:', this.totalPreguntas, 'totalPaginas:', this.totalPaginas, 'paginaActual:', this.paginaActual);
            
            this.mostrarPreguntas();
            setTimeout(() => { window.scrollTo({ top: this.lastScrollY || 0, behavior: 'auto' }); }, 0);
            
            // Actualizar indicadores visuales después de cargar
            if (typeof actualizarIndicadoresOrdenamientoPreguntas === 'function') {
                actualizarIndicadoresOrdenamientoPreguntas();
            }
        } catch (error) {
            if (error && error.message && error.message.startsWith('401')) {
                // No mostrar mensaje, la redirección ya ocurre en api.js
                return;
            }
            console.error('Error al cargar preguntas:', error);
            Toastify({
                text: `Error: ${error.message}`,
                duration: 3000,
                close: true,
                gravity: "top",
                position: "right",
                style: {
                    background: "linear-gradient(to right, #ff0000, #cc0000)",
                }
            }).showToast();
        } finally {
            this.cargando = false;
            this.ocultarEstadoCarga();
            // Actualizar paginación DESPUÉS de establecer cargando = false
            this.actualizarPaginacion();
        }
    },

    async irAPagina(pagina) {
        console.log('🔄 [PAGINACIÓN] Navegando a página:', pagina);
        
        if (this.cargando) {
            console.log('❌ [PAGINACIÓN] Ya está cargando, abortando...');
            return;
        }
        
        if (pagina < 0 || pagina >= this.totalPaginas) {
            console.log('❌ [PAGINACIÓN] Página inválida:', pagina);
            return;
        }
        
        this.paginaActual = pagina;
        this.cargando = true;
        this.mostrarEstadoCarga();
        
        try {
            // Determinar si hay filtros activos
            const hayFiltrosActivos = this.tieneFiltrosActivos(this.filtros);
            
            if (hayFiltrosActivos) {
                console.log('🔍 [PAGINACIÓN] Cargando página con filtros activos...');
                
                const params = this.construirParamsFiltrar(this.filtros, this.paginaActual);
                
                console.log('🔍 [PAGINACIÓN] Parámetros con ordenamiento:', params.toString());
                
                const response = await fetch(`/api/preguntas/filtrar?${params.toString()}`, {
                    headers: authManager.getAuthHeaders()
                });
                if (!response.ok) {
                    throw new Error('Error al cargar página con filtros');
                }
                const data = await response.json();
                console.log('🔍 [PAGINACIÓN] Datos recibidos con filtros:', data);
                this.preguntas = data.content || [];
                this.totalPreguntas = data.totalElements || 0;
                this.totalPaginas = data.totalPages || 1;
            } else {
                console.log('🔄 [PAGINACIÓN] Cargando página sin filtros...');
                
                const params = new URLSearchParams({
                    page: this.paginaActual,
                    size: this.tamanioPagina,
                    sortBy: this.orden.columna || 'id',
                    sortDir: this.orden.asc ? 'asc' : 'desc'
                });
                
                console.log('🔄 [PAGINACIÓN] Parámetros sin filtros:', params.toString());
                
                const response = await fetch(`/api/preguntas?${params.toString()}`, {
                    headers: authManager.getAuthHeaders()
                });
                if (!response.ok) {
                    throw new Error('Error al cargar página');
                }
                const data = await response.json();
                console.log('🔄 [PAGINACIÓN] Datos recibidos sin filtros:', data);
                this.preguntas = data.content || [];
                this.totalPreguntas = data.totalElements || 0;
                this.totalPaginas = data.totalPages || 1;
            }
            
            this.mostrarPreguntas();
            this.actualizarPaginacion();
            
        } catch (error) {
            console.error('❌ [PAGINACIÓN] Error al cargar página:', error);
            Toastify({
                text: `Error: ${error.message}`,
                duration: 3000,
                close: true,
                gravity: 'top',
                position: 'right',
                style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
            }).showToast();
        } finally {
            this.cargando = false;
            this.ocultarEstadoCarga();
        }
    },

    mostrarEstadoCarga() {
        const tbody = document.querySelector('#tabla-preguntas');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="14" class="text-center"><i class="fas fa-spinner fa-spin"></i> Cargando preguntas...</td></tr>';
        }
    },

    ocultarEstadoCarga() {
        // El estado de carga se oculta automáticamente cuando se muestran las preguntas
    },

    actualizarPaginacion() {
        console.log('🔄 [PAGINACION] Actualizando paginación con botones de páginas...');
        console.log('🔄 [PAGINACION] Estado - preguntas.length:', this.preguntas.length, 'totalPreguntas:', this.totalPreguntas, 'paginaActual:', this.paginaActual, 'totalPaginas:', this.totalPaginas);
        
        // Usar el contenedor de paginación del HTML
        const paginacionContainer = document.getElementById('paginacion-preguntas');
        const infoPaginacion = document.getElementById('info-paginacion');
        
        if (!paginacionContainer) {
            console.error('❌ [PAGINACION] Contenedor de paginación no encontrado');
            return;
        }

        // Actualizar información de paginación
        if (infoPaginacion) {
            const inicio = (this.paginaActual * this.tamanioPagina) + 1;
            const fin = Math.min((this.paginaActual + 1) * this.tamanioPagina, this.totalPreguntas);
            infoPaginacion.textContent = `Mostrando ${inicio}-${fin} de ${this.totalPreguntas} preguntas`;
        }

        // Limpiar botones existentes
        paginacionContainer.innerHTML = '';

        if (this.totalPaginas <= 1) {
            console.log('✅ [PAGINACION] Solo hay una página, no se muestran botones');
            return;
        }

        // Crear botón "Primera"
        const primeraPagina = document.createElement('li');
        primeraPagina.className = `page-item ${this.paginaActual === 0 ? 'disabled' : ''}`;
        primeraPagina.innerHTML = `<a class="page-link" href="#" onclick="PreguntasManager.irAPagina(0)">Primera</a>`;
        paginacionContainer.appendChild(primeraPagina);

        // Crear botón "Anterior"
        const paginaAnterior = document.createElement('li');
        paginaAnterior.className = `page-item ${this.paginaActual === 0 ? 'disabled' : ''}`;
        paginaAnterior.innerHTML = `<a class="page-link" href="#" onclick="PreguntasManager.irAPagina(${this.paginaActual - 1})">Anterior</a>`;
        paginacionContainer.appendChild(paginaAnterior);

        // Calcular rango de páginas a mostrar
        const inicio = Math.max(0, this.paginaActual - 2);
        const fin = Math.min(this.totalPaginas - 1, this.paginaActual + 2);

        // Mostrar páginas en el rango
        for (let i = inicio; i <= fin; i++) {
            const pagina = document.createElement('li');
            pagina.className = `page-item ${i === this.paginaActual ? 'active' : ''}`;
            pagina.innerHTML = `<a class="page-link" href="#" onclick="PreguntasManager.irAPagina(${i})">${i + 1}</a>`;
            paginacionContainer.appendChild(pagina);
        }

        // Crear botón "Siguiente"
        const paginaSiguiente = document.createElement('li');
        paginaSiguiente.className = `page-item ${this.paginaActual >= this.totalPaginas - 1 ? 'disabled' : ''}`;
        paginaSiguiente.innerHTML = `<a class="page-link" href="#" onclick="PreguntasManager.irAPagina(${this.paginaActual + 1})">Siguiente</a>`;
        paginacionContainer.appendChild(paginaSiguiente);

        // Crear botón "Última"
        const ultimaPagina = document.createElement('li');
        ultimaPagina.className = `page-item ${this.paginaActual >= this.totalPaginas - 1 ? 'disabled' : ''}`;
        ultimaPagina.innerHTML = `<a class="page-link" href="#" onclick="PreguntasManager.irAPagina(${this.totalPaginas - 1})">Última</a>`;
        paginacionContainer.appendChild(ultimaPagina);

        console.log('✅ [PAGINACION] Botones de paginación creados correctamente');
    },

    mostrarPreguntas() {
        const tbody = document.querySelector('#tabla-preguntas');
        if (!tbody) {
            console.error('No se encontró el elemento tabla-preguntas');
            return;
        }
        
        // Guardar la posición actual del scroll antes de modificar la tabla
        const scrollPosition = window.scrollY;
        const isAddingMore = this.preguntas.length > tbody.children.length;
        
        if (isAddingMore) {
            console.log('📍 [MOSTRAR] Añadiendo más preguntas, posición del scroll guardada:', scrollPosition);
        }
        
        tbody.innerHTML = '';

        // Las preguntas ya vienen filtradas y ordenadas del servidor
        this.preguntas.forEach((pregunta, index) => {
            // Log para verificar la codificación en el renderizado
            if (index === 0) {
                console.log('🎨 [MOSTRAR] Renderizando primera pregunta:');
                console.log('  - ID:', pregunta.id);
                console.log('  - Pregunta original:', pregunta.pregunta);
                console.log('  - Pregunta en HTML:', pregunta.pregunta ?? '');
                console.log('  - Bytes de pregunta:', Array.from(pregunta.pregunta || '').map(c => c.charCodeAt(0)));
            }
            const tr = document.createElement('tr');
            tr.setAttribute('data-id', pregunta.id);
            tr.setAttribute('oncontextmenu', `showContextMenu(event, ${pregunta.id}, 'pregunta')`);
            tr.innerHTML = `
                <td>${pregunta.id ?? ''}</td>
                <td style="background-color: #f8f9fa; font-style: italic;">${pregunta.autor ?? pregunta.creacionUsuarioNombre ?? ''}</td>
                <td ondblclick="PreguntasManager.editarCelda(${pregunta.id}, 'nivel', this)"><span class="${this.getNivelColor(pregunta.nivel)}">${Utils.formatearNivel(pregunta.nivel)}</span></td>
                <td ondblclick="PreguntasManager.editarCelda(${pregunta.id}, 'tematica', this)">${pregunta.tematica ?? ''}</td>
                <td ondblclick="PreguntasManager.editarCelda(${pregunta.id}, 'subtema', this)">${(pregunta.subtema ?? '').split(',').map(s => s.trim()).filter(Boolean).join(', ')}</td>
                <td ondblclick="PreguntasManager.editarCelda(${pregunta.id}, 'pregunta', this)" style="white-space:pre-line; word-break:break-word; max-width:300px;">${pregunta.pregunta ?? ''}</td>
                <td ondblclick="PreguntasManager.editarCelda(${pregunta.id}, 'respuesta', this)">${pregunta.respuesta ?? ''}</td>
                <td ondblclick="PreguntasManager.editarCelda(${pregunta.id}, 'datosExtra', this)">${pregunta.datosExtra ?? ''}</td>
                <td ondblclick="PreguntasManager.editarCelda(${pregunta.id}, 'fuentes', this)">${this.linkify(pregunta.fuentes ?? '')}</td>
                <td>${pregunta.verificacion ?? ''}</td>
                <td ondblclick="PreguntasManager.editarCelda(${pregunta.id}, 'notasVerificacion', this)">${pregunta.notasVerificacion ?? ''}</td>
                <td ondblclick="PreguntasManager.editarCelda(${pregunta.id}, 'notasDireccion', this)">${pregunta.notasDireccion ?? ''}</td>
                <td ondblclick="PreguntasManager.editarCelda(${pregunta.id}, 'estado', this)"><span class="badge ${this.getEstadoColor(pregunta.estado)}">${pregunta.estado ?? ''}</span></td>
                <td>
                    <button class="btn btn-sm btn-primary me-1" onclick="PreguntasManager.editarPregunta(${pregunta.id})" title="Editar pregunta">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-sm btn-info me-1" onclick="PreguntasManager.buscarAparicionesDesdeLista(${pregunta.id})" title="Buscar apariciones">
                        <i class="fas fa-search"></i>
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="PreguntasManager.eliminarPregunta(${pregunta.id})" title="Eliminar pregunta">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            `;
            tbody.appendChild(tr);
        });
        
        // Restaurar posición del scroll
        setTimeout(() => {
            window.scrollTo({ top: this.lastScrollY || scrollPosition || 0, behavior: 'auto' });
        }, 0);

        if (typeof SyncMonitor !== 'undefined') {
            SyncMonitor.resetFromVisible(this.preguntas.map(p => ({
                entityType: 'PREGUNTA',
                entityId: p.id,
                version: p.version || 0,
                label: `Pregunta ${p.id}`
            })));
        }
    },

    // Convertir URLs en enlaces clicables, manteniendo texto no-URL intacto
    linkify(textoFuentes) {
        try {
            if (!textoFuentes || typeof textoFuentes !== 'string') return '';
            const urlRegex = /((https?:\/\/|www\.)[^\s<>"'\)]+)(?![^<]*>)/gi;
            return textoFuentes.replace(urlRegex, (urlMatch) => {
                let href = urlMatch;
                if (href.toLowerCase().startsWith('www.')) {
                    href = 'https://' + href;
                }
                const hrefSafe = href.replace(/"/g, '&quot;');
                // Evitar que el clic/doble-clic sobre el enlace dispare la edición inline
                return `<a href="${hrefSafe}" target="_blank" rel="noopener noreferrer" onclick="event.stopPropagation();" ondblclick="event.stopPropagation();">${urlMatch}</a>`;
            });
        } catch (e) {
            console.warn('linkify error:', e);
            return textoFuentes || '';
        }
    },

    async filtrarPreguntas() {
        if (this.cargando) {
            return;
        }

        try {
            this.cargando = true;
            this.mostrarEstadoCarga();

            const filtros = this.leerFiltrosDesdeDom();
            this.filtros = filtros;

            if (!this.tieneFiltrosActivos(filtros)) {
                this.cargando = false;
                this.ocultarEstadoCarga();
                await this.cargarPreguntas(true);
                return;
            }

            this.paginaActual = 0;
            const params = this.construirParamsFiltrar(filtros, 0);
            console.log('🔍 [FILTRAR] Parámetros:', params.toString());

            const response = await fetch(`/api/preguntas/filtrar?${params.toString()}`, {
                headers: authManager.getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error('Error al filtrar preguntas');
            }

            const responseData = await response.json();
            console.log('🔍 [FILTRAR] Respuesta:', responseData.totalElements, 'resultados');

            if (responseData.content && Array.isArray(responseData.content)) {
                this.preguntas = responseData.content;
                this.totalPreguntas = responseData.totalElements || 0;
                this.totalPaginas = responseData.totalPages || 1;
                this.paginaActual = responseData.number ?? 0;
            } else {
                this.preguntas = Array.isArray(responseData) ? responseData : [];
                this.totalPreguntas = this.preguntas.length;
                this.totalPaginas = 1;
                this.paginaActual = 0;
            }

            this.mostrarPreguntas();
            this.actualizarPaginacion();

        } catch (error) {
            console.error('Error al filtrar preguntas:', error);
            Toastify({
                text: `Error al filtrar: ${error.message}`,
                duration: 3000,
                close: true,
                gravity: 'top',
                position: 'right',
                style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' },
            }).showToast();
        } finally {
            this.cargando = false;
            this.ocultarEstadoCarga();
        }
    },

    filtrarPreguntasClientSide() {
        const estado = document.getElementById('filtro-estado')?.value || '';
        const nivel = document.getElementById('filtro-nivel')?.value || '';
        const tematica = document.getElementById('filtro-tematica')?.value.toLowerCase() || '';
        const subtema = document.getElementById('filtro-subtema')?.value.toLowerCase() || '';
        const pregunta = document.getElementById('filtro-pregunta')?.value.toLowerCase() || '';
        const texto = document.getElementById('filtro-texto')?.value.toLowerCase() || '';
        
        const preguntasFiltradas = this.preguntas.filter(p => {
            const coincideEstado = !estado || p.estado === estado;
            const coincideNivel = !nivel || p.nivel === nivel;
            const coincideTematica = !tematica || (p.tematica && p.tematica.toLowerCase().includes(tematica));
            const coincideSubtema = !subtema || (p.subtema && p.subtema.toLowerCase().includes(subtema));
            const coincideTexto = !texto || ((p.pregunta && p.pregunta.toLowerCase().includes(texto)) || (p.respuesta && p.respuesta.toLowerCase().includes(texto)));
            
            return coincideEstado && coincideNivel && coincideTematica && 
                   coincideSubtema && coincideTexto;
        });
        
        // Actualizar preguntas filtradas y mostrar
        this.preguntas = preguntasFiltradas;
        this.mostrarPreguntas();
    },

    // Función para recargar manteniendo los filtros activos
    async recargarConFiltros() {
        try {
            this.filtros = this.leerFiltrosDesdeDom();
            console.log('🔄 [RECARGAR] Filtros activos:', this.filtros);

            if (this.tieneFiltrosActivos(this.filtros)) {
                await this.filtrarPreguntas();
            } else {
                await this.cargarPreguntas(true);
            }
        } catch (error) {
            console.error('❌ [RECARGAR] Error al recargar con filtros:', error);
            await this.cargarPreguntas(true);
        }
    },

    normalizarEnumSnapshot(valor) {
        if (valor == null) return null;
        if (typeof valor === 'string') return valor;
        if (typeof valor === 'object' && valor.name) return valor.name;
        return String(valor);
    },

    buildPayloadCrearDesdeSnapshot(snapshot) {
        const nivel = this.normalizarEnumSnapshot(snapshot?.nivel);
        return {
            nivel: nivel || '_0',
            tematica: snapshot?.tematica || 'General',
            pregunta: snapshot?.pregunta || '',
            respuesta: snapshot?.respuesta || '',
            datosExtra: snapshot?.datosExtra ?? null,
            fuentes: snapshot?.fuentes ?? null,
            subtema: snapshot?.subtema ?? null,
            notasVerificacion: snapshot?.notasVerificacion ?? null,
            notasDireccion: snapshot?.notasDireccion ?? null,
        };
    },

    buildPayloadPutDesdeSnapshot(snapshot, id, version) {
        const nivel = this.normalizarEnumSnapshot(snapshot?.nivel);
        const estado = this.normalizarEnumSnapshot(snapshot?.estado);
        const factor = this.normalizarEnumSnapshot(snapshot?.factor);
        return {
            id,
            version: version ?? snapshot?.version ?? 0,
            tematica: snapshot?.tematica ?? null,
            pregunta: snapshot?.pregunta ?? null,
            respuesta: snapshot?.respuesta ?? null,
            datosExtra: snapshot?.datosExtra ?? null,
            fuentes: snapshot?.fuentes ?? null,
            nivel: nivel ?? null,
            subtema: snapshot?.subtema ?? null,
            autor: snapshot?.autor ?? null,
            notas: snapshot?.notas ?? null,
            factor: factor ?? null,
            notasVerificacion: snapshot?.notasVerificacion ?? null,
            notasDireccion: snapshot?.notasDireccion ?? null,
            verificacion: snapshot?.verificacion ?? null,
            estado: estado ?? null,
        };
    },

    resaltarFilaPregunta(fila) {
        if (!fila) return;
        fila.classList.add('table-warning');
        fila.scrollIntoView({ behavior: 'smooth', block: 'center' });
    },

    async irAPreguntaPorId(idPregunta) {
        if (!idPregunta) return false;

        try {
            const resp = await fetch(`/api/preguntas/buscar?id=${encodeURIComponent(idPregunta)}&page=0&size=1`, {
                headers: authManager.getAuthHeaders()
            });
            if (!resp.ok) return false;
            const data = await resp.json();
            const encontrada = (data.content || []).find(p => p.id === Number(idPregunta));
            if (!encontrada) return false;

            this.preguntas = [encontrada];
            this.paginaActual = 0;
            this.totalPaginas = 1;
            this.totalPreguntas = 1;
            this.mostrarPreguntas();
            const fila = document.querySelector(`#tabla-preguntas tr[data-id='${idPregunta}']`);
            if (fila) {
                this.resaltarFilaPregunta(fila);
                return true;
            }
        } catch (e) {
            console.warn('[UNDO] No se pudo localizar la pregunta restaurada:', e);
        }
        return false;
    },

    async restaurarPreguntaEliminada(snapshot) {
        const payload = { ...snapshot };
        if (payload.nivel) payload.nivel = this.normalizarEnumSnapshot(payload.nivel);
        if (payload.estado) payload.estado = this.normalizarEnumSnapshot(payload.estado);
        if (payload.factor) payload.factor = this.normalizarEnumSnapshot(payload.factor);
        if (!payload.id) {
            throw new Error('No se puede restaurar la pregunta sin ID');
        }

        const crearResp = await fetch('/api/preguntas/restaurar', {
            method: 'POST',
            headers: {
                ...authManager.getAuthHeaders(),
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });
        if (!crearResp.ok) {
            let msg = 'No se pudo deshacer la eliminación';
            try {
                const errText = await crearResp.text();
                if (errText) msg = errText;
            } catch {}
            throw new Error(msg);
        }

        const creada = await crearResp.json();
        const idRestaurado = creada?.id ?? payload.id;
        if (!idRestaurado) {
            throw new Error('No se pudo obtener el ID de la pregunta restaurada');
        }
        return idRestaurado;
    },

    // Función para cargar los usuarios en el filtro de autoría
    async cargarUsuariosEnFiltro() {
        try {
            const response = await fetch('/api/usuarios', {
                headers: authManager.getAuthHeaders()
            });
            
            if (!response.ok) {
                console.error('Error al cargar usuarios para filtro de autoría');
                return;
            }
            
            const usuarios = await response.json();
            const selectAutoria = document.getElementById('filtro-autoria');
            
            if (!selectAutoria) return;
            
            // Limpiar opciones existentes excepto "Todos"
            selectAutoria.innerHTML = '<option value="">Todos</option>';
            
            // Agregar cada usuario como opción
            usuarios.forEach(usuario => {
                const option = document.createElement('option');
                option.value = usuario.nombre;
                option.textContent = usuario.nombre;
                selectAutoria.appendChild(option);
            });
            
            console.log('✅ [FILTRO] Cargados', usuarios.length, 'usuarios en filtro de autoría');
        } catch (error) {
            console.error('❌ [FILTRO] Error al cargar usuarios:', error);
        }
    },

    validarRequisitosParaVerificar(datos) {
        const faltantes = [];
        if (!datos?.tematica?.trim()) faltantes.push('temática');
        if (!datos?.pregunta?.trim()) faltantes.push('pregunta');
        if (!datos?.respuesta?.trim()) faltantes.push('respuesta');
        if (!datos?.fuentes?.trim()) faltantes.push('fuente');
        if (faltantes.length) {
            throw new Error(`Para pasar a "para verificar" son obligatorios: ${faltantes.join(', ')}`);
        }
    },

    async crearPregunta(event) {
        event.preventDefault();
        const formData = new FormData(event.target);
        const preguntaData = Object.fromEntries(formData.entries());
        
        // Verificar si es edición
        const editId = event.target.dataset.editId;
        const esEdicion = !!editId;
        
        console.log('💾 [GUARDAR] Iniciando guardado...', esEdicion ? 'EDICION' : 'CREACION');
        console.log('💾 [GUARDAR] ID de edición:', editId);
        console.log('💾 [GUARDAR] Datos del formulario inicial:', preguntaData);
        
        // Obtener subtemas seleccionados del picker
        const subtemasSeleccionadosList = SubtemasPicker.getSeleccionados();
        preguntaData.subtema = subtemasSeleccionadosList.join(',');
        console.log('💾 [GUARDAR] Subtemas procesados:', subtemasSeleccionadosList, '→', preguntaData.subtema);
        
        // IMPORTANTE: Verificar explícitamente el estado
        const estadoSelect = document.getElementById('estado-pregunta');
        if (estadoSelect) {
            preguntaData.estado = estadoSelect.value;
            console.log('💾 [GUARDAR] Estado seleccionado del select:', preguntaData.estado);
        } else if (!preguntaData.estado) {
            // Si no hay estado seleccionado (aunque debería haberlo), usar borrador por defecto
            preguntaData.estado = 'borrador';
            console.log('💾 [GUARDAR] Estado no especificado, usando por defecto:', preguntaData.estado);
        }
        
        console.log('💾 [GUARDAR] Datos finales a enviar:', preguntaData);

        if (preguntaData.estado === 'para_verificar') {
            try {
                this.validarRequisitosParaVerificar(preguntaData);
            } catch (validationError) {
                Toastify({
                    text: validationError.message,
                    duration: 5000,
                    close: true,
                    gravity: 'top',
                    position: 'right',
                    style: { background: 'linear-gradient(to right, #ff9966, #ff5e62)' }
                }).showToast();
                return;
            }
        }
        
        try {
            if (!authManager.isAuthenticated()) {
                throw new Error('Usuario no autenticado');
            }
            
            let response;
            if (esEdicion) {
                // Editar pregunta existente
                console.log('📤 [GUARDAR] Enviando PUT a /api/preguntas/' + editId);
                console.log('📤 [GUARDAR] Datos JSON:', JSON.stringify(preguntaData, null, 2));
                
                response = await fetch(`/api/preguntas/${editId}`, {
                    method: 'PUT',
                    headers: {
                        ...authManager.getAuthHeaders(),
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(preguntaData)
                });
                
                // Verificar respuesta específicamente para cambios de estado
                if (response.ok) {
                    const preguntaActualizada = await response.json();
                    console.log('✅ [GUARDAR] Pregunta actualizada exitosamente:', preguntaActualizada);
                    console.log('✅ [GUARDAR] Estado actualizado:', preguntaActualizada.estado);
                    
                    // Actualizar la pregunta en la lista local
                    const preguntaIndex = this.preguntas.findIndex(p => p.id === parseInt(editId));
                    if (preguntaIndex !== -1) {
                        this.preguntas[preguntaIndex] = preguntaActualizada;
                        console.log('✅ [GUARDAR] Pregunta actualizada en la lista local');
                    }
                } else {
                    const errorText = await response.text();
                    console.error('❌ [GUARDAR] Error del servidor:', errorText);
                    throw new Error('Error al editar la pregunta: ' + errorText);
                }
            } else {
                // Crear nueva pregunta
                console.log('📤 [GUARDAR] Enviando POST a /api/preguntas');
                response = await fetch('/api/preguntas', {
                    method: 'POST',
                    headers: {
                        ...authManager.getAuthHeaders(),
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(preguntaData)
                });
                
                if (!response.ok) {
                    const errorText = await response.text();
                    console.error('❌ [GUARDAR] Error del servidor:', errorText);
                    throw new Error('Error al crear la pregunta: ' + errorText);
                }
                
                // Registrar undo/redo para creación
                try {
                    const creada = await response.json();
                    const createdId = creada?.id;
                    if (window.UndoManager && createdId) {
                        const undoAction = async () => {
                            const del = await fetch(`/api/preguntas/${createdId}`, {
                                method: 'DELETE',
                                headers: authManager.getAuthHeaders()
                            });
                            if (!del.ok) throw new Error('No se pudo deshacer la creación');
                            await this.recargarConFiltros();
                        };
                        const redoAction = async () => {
                            const r = await fetch('/api/preguntas', {
                                method: 'POST',
                                headers: {
                                    ...authManager.getAuthHeaders(),
                                    'Content-Type': 'application/json'
                                },
                                body: JSON.stringify(preguntaData)
                            });
                            if (!r.ok) throw new Error('No se pudo rehacer la creación');
                            await this.recargarConFiltros();
                        };
                        window.UndoManager.record({ do: redoAction, undo: undoAction, label: `Crear pregunta ${createdId}` });
                    }
                } catch (e) {
                    console.warn('⚠️ [UNDO] No se pudo registrar undo para creación:', e);
                }
            }
            
            console.log('📥 [GUARDAR] Respuesta del servidor:', response.status, response.statusText);
            
            // Recargar las preguntas manteniendo filtros activos
            await this.recargarConFiltros();
            PreguntasManager.ocultarModalPregunta();
            
            Toastify({
                text: esEdicion ? "Pregunta editada exitosamente" : "Pregunta creada exitosamente",
                duration: 3000,
                close: true,
                gravity: "top",
                position: "right",
                style: {
                    background: "linear-gradient(to right, #00b09b, #96c93d)",
                }
            }).showToast();
            
            // Limpiar el dataset de edición
            delete event.target.dataset.editId;
            
        } catch (error) {
            console.error('Error al procesar pregunta:', error);
            Toastify({
                text: `Error: ${error.message}`,
                duration: 3000,
                close: true,
                gravity: "top",
                position: "right",
                style: {
                    background: "linear-gradient(to right, #ff0000, #cc0000)",
                }
            }).showToast();
        }
    },

    async aplicarFiltros() {
        await this.filtrarPreguntas();
    },

    async aplicarFiltrosConOrden() {
        await this.filtrarPreguntas();
    },

    async setOrden(columna) {
        console.log('🔄 [ORDEN] setOrden llamado con columna:', columna);

        if (typeof isTableResizing === 'function' && isTableResizing('tabla-preguntas-header')) {
            return;
        }

        if (this.cargando) {
            return;
        }

        if (this.orden.columna === columna) {
            this.orden.asc = !this.orden.asc;
        } else {
            this.orden.columna = columna;
            this.orden.asc = true;
        }

        this.paginaActual = 0;
        this.filtros = this.leerFiltrosDesdeDom();

        if (this.tieneFiltrosActivos(this.filtros)) {
            await this.filtrarPreguntas();
        } else {
            await this.cargarPreguntas(true);
        }

        if (typeof actualizarIndicadoresOrdenamientoPreguntas === 'function') {
            actualizarIndicadoresOrdenamientoPreguntas();
        }
    },

    getNivelColor(nivel) {
        if (nivel === '_0') return 'text-secondary fw-bold';
        if (["_2NLS", "_4NLS", "_5NLS"].includes(nivel)) return 'text-danger fw-bold';
        if (["_1LS", "_3LS", "_5LS"].includes(nivel)) return 'text-success fw-bold';
        return '';
    },

    getEstadoColor(estado) {
        if (estado === 'borrador') return 'bg-secondary text-white';
        if (estado === 'para_verificar') return 'bg-info text-white';
        if (estado === 'verificada') return 'bg-primary text-white';
        if (estado === 'revisar') return 'bg-warning text-dark';
        if (estado === 'corregir') return 'bg-warning text-dark';
        if (estado === 'rechazada') return 'bg-danger text-white';
        if (estado === 'aprobada') return 'bg-success text-white';
        if (estado === 'para_aprobar') return 'bg-info text-white';
        if (estado === 'usada') return 'bg-dark text-white';
        return 'bg-light text-dark';
    },
    
    // Determina qué estados están permitidos según el estado actual
    getEstadosPermitidos(estadoActual) {
        // Siempre incluir el estado actual como primera opción
        const estados = [estadoActual];
        
        // Añadir estados permitidos según el autómata
        switch (estadoActual) {
            case 'borrador':
                estados.push('para_verificar');
                break;
                
            case 'para_verificar':
                estados.push('verificada', 'revisar');
                break;
                
            case 'revisar':
                estados.push('para_verificar', 'para_aprobar', 'rechazada');
                break;
                
            case 'verificada':
                estados.push('corregir', 'rechazada', 'aprobada');
                break;
                
            case 'corregir':
                estados.push('para_aprobar', 'para_verificar');
                break;
                
            case 'para_aprobar':
                estados.push('aprobada', 'corregir', 'rechazada', 'para_verificar');
                break;
                
            case 'aprobada':
                estados.push('usada');
                break;
                
            case 'usada':
                estados.push('aprobada');
                break;
                
            case 'rechazada':
                // Estado final, no hay transiciones salientes
                break;
        }
        
        // Si el usuario es admin o dirección, permitir todos los estados (orden del filtro)
        const usuario = JSON.parse(localStorage.getItem('usuario'));
        if (usuario && (usuario.rol === 'ROLE_ADMIN' || usuario.rol === 'ROLE_DIRECCION')) {
            return this.ordenarEstadosPregunta(this.ORDEN_ESTADOS_PREGUNTA);
        }
        
        // Filtrar por rol para mostrar solo transiciones que el usuario puede ejecutar
        if (usuario && usuario.rol) {
            const rol = usuario.rol;
            const puedeTransicionar = (from, to, rol) => {
                switch (rol) {
                    case 'ROLE_GUION':
                        switch (from) {
                            case 'borrador': return to === 'para_verificar';
                            case 'para_verificar': return to === 'verificada' || to === 'revisar';
                            case 'revisar': return to === 'para_verificar' || to === 'para_aprobar' || to === 'rechazada';
                            case 'corregir': return to === 'para_aprobar' || to === 'para_verificar';
                            default: return false;
                        }
                    case 'ROLE_VERIFICACION':
                        switch (from) {
                            case 'para_verificar': return to === 'verificada' || to === 'revisar';
                            default: return false;
                        }
                    case 'ROLE_DIRECCION':
                        switch (from) {
                            case 'para_verificar': return to === 'verificada';
                            case 'verificada': return to === 'corregir' || to === 'rechazada' || to === 'aprobada';
                            case 'para_aprobar': return to === 'aprobada' || to === 'corregir' || to === 'rechazada' || to === 'para_verificar';
                            case 'aprobada': return to === 'usada';
                            case 'usada': return to === 'aprobada';
                            default: return false;
                        }
                    default:
                        return false;
                }
            };

            // Mantener el estado actual como primera opción y filtrar el resto
            const estadosFiltrados = estados.filter((estado, index) => index === 0 || puedeTransicionar(estadoActual, estado, rol));
            return this.ordenarEstadosPregunta(estadosFiltrados);
        }

        return this.ordenarEstadosPregunta(estados);
    },

    async editarCelda(id, campo, td) {
        // Evitar múltiples inputs
        if (td.querySelector('input,select')) return;
        
        // No permitir editar campos de autoría
        if (campo === 'creacionUsuario' || campo === 'creacionUsuarioNombre') {
            return;
        }
        const valorOriginal = td.innerText;
        let input;
        if (campo === 'nivel') {
            input = document.createElement('select');
            ['_0','_1LS','_2NLS','_3LS','_4NLS','_5LS','_5NLS'].forEach(opt => {
                const option = document.createElement('option');
                option.value = opt;
                option.text = opt === '_0' ? 'Sin nivel (0)' : Utils.formatearNivel(opt);
                if (valorOriginal === opt) option.selected = true;
                input.appendChild(option);
            });
        } else if (campo === 'tematica') {
            input = document.createElement('select');
            // Usar temas dinámicos si están disponibles, sino usar lista estática
            const temas = TemasManager.temas.length > 0 ? TemasManager.temas : ['GEOGRAFÍA','HISTORIA','DEPORTES','CIENCIA','ARTE'];
            temas.forEach(opt => {
                const option = document.createElement('option');
                option.value = opt;
                option.text = opt;
                if (valorOriginal === opt) option.selected = true;
                input.appendChild(option);
            });
        } else if (campo === 'subtema') {
            input = document.createElement('select');
            input.multiple = true;
            // Usar subtemas dinámicos si están disponibles, sino usar lista estática
            const subtemas = TemasManager.subtemas.length > 0 ? TemasManager.subtemas : ['GEOGRAFÍA','HISTORIA','DEPORTES','CIENCIA','ARTE'];
            const valoresActuales = valorOriginal.split(',').map(v => v.trim());
            subtemas.forEach(opt => {
                const option = document.createElement('option');
                option.value = opt;
                option.text = opt;
                if (valoresActuales.includes(opt)) option.selected = true;
                input.appendChild(option);
            });
        } else if (campo === 'estado') {
            input = document.createElement('select');
            
            // Obtener la pregunta actual para conocer su estado
            const pregunta = this.preguntas.find(p => p.id === id);
            if (!pregunta) return;
            
            const estadoActual = pregunta.estado;
            const estadosPermitidos = this.getEstadosPermitidos(estadoActual);
            
            // Añadir solo los estados permitidos al select
            estadosPermitidos.forEach(opt => {
                const option = document.createElement('option');
                option.value = opt;
                option.text = this.formatearEstadoPregunta(opt);
                if (valorOriginal === opt) option.selected = true;
                input.appendChild(option);
            });
        } else {
            input = document.createElement('input');
            input.type = 'text';
            input.value = valorOriginal;
        }
        input.className = 'form-control form-control-sm';
        td.innerHTML = '';
        td.appendChild(input);
        input.focus();
        if (campo === 'subtema') {
            // Guardar solo al perder el foco
            input.onblur = async () => await this.guardarCelda(id, campo, input, td, valorOriginal);
            input.onkeydown = async (e) => {
                if (e.key === 'Escape') {
                    td.innerHTML = valorOriginal;
                }
            };
        } else {
            input.onblur = async () => await this.guardarCelda(id, campo, input, td, valorOriginal);
            input.onkeydown = async (e) => {
                if (e.key === 'Enter') {
                    input.blur();
                } else if (e.key === 'Escape') {
                    td.innerHTML = valorOriginal;
                }
            };
        }
    },

    async guardarCelda(id, campo, input, td, valorOriginal) {
        let nuevoValor;
        if (input.tagName === 'SELECT' && input.multiple) {
            nuevoValor = Array.from(input.selectedOptions).map(opt => opt.value).join(',');
        } else if (input.tagName === 'SELECT') {
            nuevoValor = input.value;
        } else {
            nuevoValor = input.value;
        }
        // Permitir guardar aunque el valor original sea vacío o null
        if ((valorOriginal ?? '') === (nuevoValor ?? '')) {
            td.innerHTML = valorOriginal;
            return;
        }
        
        // CORRECCIÓN: Solo enviar el campo que se está editando
        const update = {};
        update[campo] = nuevoValor;
        
        console.log('📤 [FRONTEND] Actualizando campo:', campo, '→', nuevoValor);
        
        try {
            let response;
            
            if (campo === 'estado') {
                const prevEstadoObj = this.preguntas.find(p => p.id === id);
                const estadoAnterior = prevEstadoObj ? prevEstadoObj.estado : (valorOriginal || '');
                if (nuevoValor === 'para_verificar') {
                    try {
                        this.validarRequisitosParaVerificar(prevEstadoObj || {});
                    } catch (validationError) {
                        td.innerHTML = `<span class="badge ${this.getEstadoColor(estadoAnterior)}">${estadoAnterior}</span>`;
                        Toastify({
                            text: validationError.message,
                            duration: 5000,
                            close: true,
                            gravity: 'top',
                            position: 'right',
                            style: { background: 'linear-gradient(to right, #ff9966, #ff5e62)' }
                        }).showToast();
                        return;
                    }
                }
                // Usar endpoint especial para cambio de estado
                console.log('📤 [FRONTEND] Enviando cambio de estado a:', `/api/preguntas/${id}/estado?nuevoEstado=${nuevoValor}`);
                response = await fetch(`/api/preguntas/${id}/estado?nuevoEstado=${nuevoValor}`, {
                    method: 'PUT',
                    headers: authManager.getAuthHeaders()
                });
                
                // Verificar si se actualizó correctamente
                if (response.ok) {
                    const preguntaActualizada = await response.json();
                    console.log('✅ [FRONTEND] Estado actualizado:', preguntaActualizada.estado);
                    
                    // Actualizar el valor en la tabla con el estilo correcto
                    const estadoActual = preguntaActualizada.estado;
                    td.innerHTML = `<span class="badge ${this.getEstadoColor(estadoActual)}">${estadoActual}</span>`;
                    
                    // Actualizar la pregunta en la lista local
                    const preguntaIndex = this.preguntas.findIndex(p => p.id === id);
                    if (preguntaIndex !== -1) {
                        this.preguntas[preguntaIndex].estado = estadoActual;
                    }
                    
                    // Registrar acción de deshacer/rehacer
                    if (window.UndoManager) {
                        const hacer = async () => {
                            const r = await fetch(`/api/preguntas/${id}/estado?nuevoEstado=${nuevoValor}`, { method: 'PUT', headers: authManager.getAuthHeaders() });
                            if (r.ok) {
                                await this.recargarConFiltros();
                            } else {
                                throw new Error('No se pudo rehacer el cambio de estado');
                            }
                        };
                        const deshacer = async () => {
                            const r = await fetch(`/api/preguntas/${id}/estado?nuevoEstado=${estadoAnterior}`, { method: 'PUT', headers: authManager.getAuthHeaders() });
                            if (r.ok) {
                                await this.recargarConFiltros();
                            } else {
                                throw new Error('No se pudo deshacer el cambio de estado');
                            }
                        };
                        window.UndoManager.record({ do: hacer, undo: deshacer, label: `Estado ${estadoAnterior}→${nuevoValor}` });
                    }
                    
                    // Recargar la tabla manteniendo filtros activos
                    await this.recargarConFiltros();
                    return;
                }
            } else {
                response = await fetch(`/api/preguntas/${id}`, {
                    method: 'PUT',
                    headers: authManager.getAuthHeaders(),
                    body: JSON.stringify(update)
                });
            }
            
            if (!response.ok) {
                let errorMsg = 'Error al actualizar';
                try {
                    const data = await response.json();
                    if (data && data.message) errorMsg = data.message;
                    else if (typeof data === 'string') errorMsg = data;
                } catch {}
                throw new Error(errorMsg);
            }
            
            // Registrar acción de deshacer/rehacer para cualquier campo distinto de 'estado'
            if (campo !== 'estado' && window.UndoManager) {
                const doAction = async () => {
                    const r = await fetch(`/api/preguntas/${id}`, {
                        method: 'PUT',
                        headers: authManager.getAuthHeaders(),
                        body: JSON.stringify({ [campo]: nuevoValor })
                    });
                    if (!r.ok) throw new Error('No se pudo rehacer el cambio');
                    await this.recargarConFiltros();
                };
                const undoAction = async () => {
                    const r = await fetch(`/api/preguntas/${id}`, {
                        method: 'PUT',
                        headers: authManager.getAuthHeaders(),
                        body: JSON.stringify({ [campo]: valorOriginal })
                    });
                    if (!r.ok) throw new Error('No se pudo deshacer el cambio');
                    await this.recargarConFiltros();
                };
                window.UndoManager.record({ do: doAction, undo: undoAction, label: `Actualizar pregunta ${id} - ${campo}` });
            }
            
            console.log('✅ [FRONTEND] Campo actualizado, recargando tabla...');
            // CAMBIO: Usar recargarConFiltros() en lugar de cargarPreguntas() para mantener filtros
            await this.recargarConFiltros();
            
        } catch (e) {
            console.error('❌ [FRONTEND] Error:', e.message);
            td.innerHTML = valorOriginal;
            Toastify({
                text: 'Error al guardar: ' + e.message,
                duration: 3000,
                close: true,
                gravity: 'top',
                position: 'right',
                style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
            }).showToast();
        }
    },

    mostrarModalPregunta() {
        const el = document.getElementById('modal-pregunta');
        if (!el) return;
        bootstrap.Modal.getOrCreateInstance(el).show();
    },

    ocultarModalPregunta() {
        const el = document.getElementById('modal-pregunta');
        if (!el) return;
        const inst = bootstrap.Modal.getInstance(el);
        if (inst) inst.hide();
    },

    async editarPregunta(id) {
        try {
            await EditLockManager.tryAcquire('PREGUNTA', id);
            console.log('🔍 [EDITAR] Iniciando edición de pregunta ID:', id);
            
            const response = await fetch(`/api/preguntas/${id}`, {
                headers: authManager.getAuthHeaders()
            });
            if (!response.ok) {
                throw new Error('Error al cargar la pregunta');
            }
            const pregunta = await response.json();
            
            console.log('📥 [EDITAR] Pregunta cargada completa:', pregunta);
            console.log('📥 [EDITAR] Estado actual de la pregunta:', pregunta.estado);
            
            // Cambiar título del modal
            document.getElementById('modal-pregunta-titulo').textContent = 'Editar Pregunta';
            
            // CARGAR TEMAS Y SUBTEMAS DINÁMICOS ANTES DE RELLENAR
            // Asegurar que los temas estén cargados
            if (TemasManager.temas.length === 0) {
                console.log('🔄 [EDITAR] Cargando temas dinámicos...');
                await TemasManager.cargarTemas();
            }
            if (TemasManager.subtemas.length === 0) {
                console.log('🔄 [EDITAR] Cargando subtemas dinámicos...');
                await TemasManager.cargarSubtemas();
            }
            
            console.log('📋 [EDITAR] Temas disponibles:', TemasManager.temas);
            console.log('📋 [EDITAR] Subtemas disponibles:', TemasManager.subtemas);
            
            // Inicializar el picker de temática con el valor de la pregunta
            const tematicas = TemasManager.temas.length > 0 ? TemasManager.temas : ['Geografía','Historia','Deportes','Ciencia','Arte'];
            console.log('🎯 [EDITAR] Temática de la pregunta:', pregunta.tematica);
            TematicaPicker.init(tematicas, pregunta.tematica || null);
            console.log('✅ [EDITAR] Temática cargada en picker');
            
            // Inicializar el picker de subtemas con los valores de la pregunta
            const subtemas = TemasManager.subtemas.length > 0 ? TemasManager.subtemas : ['Geografía','Historia','Deportes','Ciencia','Arte'];
            const subtemasSeleccionados = pregunta.subtema
                ? pregunta.subtema.split(',').map(s => s.trim()).filter(Boolean)
                : [];
            console.log('📝 [EDITAR] Subtemas a seleccionar:', subtemasSeleccionados);
            SubtemasPicker.init(subtemas);
            SubtemasPicker.setSeleccionados(subtemasSeleccionados);
            console.log('✅ [EDITAR] Subtemas cargados en picker');
            
            // Rellenar el resto del formulario con los datos de la pregunta
            document.getElementById('nivel-pregunta').value = pregunta.nivel || '';
            document.getElementById('pregunta-pregunta').value = pregunta.pregunta || '';
            document.getElementById('respuesta-pregunta').value = pregunta.respuesta || '';
            document.getElementById('datos-extra-pregunta').value = pregunta.datosExtra || '';
            document.getElementById('fuentes-pregunta').value = pregunta.fuentes || '';
            document.getElementById('notas-verificacion-pregunta').value = pregunta.notasVerificacion || '';
            document.getElementById('notas-direccion-pregunta').value = pregunta.notasDireccion || '';
            
            // Rellenar el select de estado con los estados permitidos
            const selectEstado = document.getElementById('estado-pregunta');
            if (selectEstado) {
                console.log('🎯 [EDITAR] Llenando select de estado. Estado actual:', pregunta.estado);
                selectEstado.innerHTML = '';
                const estadosPermitidos = this.getEstadosPermitidos(pregunta.estado);
                console.log('📝 [EDITAR] Estados permitidos para este estado:', estadosPermitidos);
                
                estadosPermitidos.forEach(estado => {
                    const option = document.createElement('option');
                    option.value = estado;
                    option.text = this.formatearEstadoPregunta(estado);
                    if (pregunta.estado === estado) {
                        option.selected = true;
                        console.log('✅ [EDITAR] Opción seleccionada:', estado);
                    }
                    selectEstado.appendChild(option);
                });
                
                // Verificar que el estado se seleccionó correctamente
                console.log('🔍 [EDITAR] Estado seleccionado en el select:', selectEstado.value);
                console.log('🔍 [EDITAR] ¿Coincide con el estado de la pregunta?', selectEstado.value === pregunta.estado);
            } else {
                console.error('❌ [EDITAR] No se encontró el elemento estado-pregunta');
            }
            
            console.log('📝 [EDITAR] Formulario rellenado. Verificación original:', pregunta.verificacion);
            
            // Guardar el ID para la edición
            const form = document.getElementById('formCrearPregunta');
            form.dataset.editId = id;
            console.log('💾 [EDITAR] ID guardado en el formulario:', form.dataset.editId);
            
            // Mostrar el modal
            PreguntasManager.mostrarModalPregunta();
            EditLockManager.startSession({
                entityType: 'PREGUNTA',
                entityId: id,
                modalSelector: '#modal-pregunta',
                onExpire: async () => {
                    const form = document.getElementById('formCrearPregunta');
                    if (form) {
                        await PreguntasManager.crearPregunta({ preventDefault: () => {}, target: form });
                    }
                }
            });
            
            console.log('✅ [EDITAR] Modal abierto');
            
        } catch (error) {
            console.error('❌ [EDITAR] Error:', error);
            Toastify({
                text: 'Error al cargar pregunta: ' + error.message,
                duration: 3000,
                close: true,
                gravity: 'top',
                position: 'right',
                style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
            }).showToast();
        }
    },

    async eliminarPregunta(id) {
        if (!confirm('¿Seguro que quieres borrar esta pregunta?')) return;
        try {
            // Snapshot previo para permitir deshacer
            let snapshot = null;
            try {
                const snapResp = await fetch(`/api/preguntas/${id}`, {
                    headers: authManager.getAuthHeaders()
                });
                if (snapResp.ok) {
                    snapshot = await snapResp.json();
                }
            } catch (e) {
                console.warn('⚠️ [UNDO] No se pudo obtener snapshot previo de la pregunta:', e);
            }

            const response = await fetch(`/api/preguntas/${id}`, {
                method: 'DELETE',
                headers: authManager.getAuthHeaders()
            });
            if (!response.ok) {
                let msg = 'Error al borrar la pregunta';
                try {
                    const data = await response.json();
                    if (typeof data === 'string') msg = data;
                } catch {}
                throw new Error(msg);
            }

            // Registrar undo/redo para eliminación (restaurar y mostrar la pregunta)
            if (window.UndoManager && snapshot) {
                const estadoUndo = { recreatedId: null, idOriginal: id };

                const undoAction = async () => {
                    const idRestaurado = await PreguntasManager.restaurarPreguntaEliminada(snapshot);
                    estadoUndo.recreatedId = idRestaurado;
                    await PreguntasManager.recargarConFiltros();
                    const fila = document.querySelector(`#tabla-preguntas tr[data-id='${idRestaurado}']`);
                    if (fila) PreguntasManager.resaltarFilaPregunta(fila);
                    Toastify({
                        text: `Pregunta #${idRestaurado} restaurada`,
                        duration: 4000,
                        close: true,
                        gravity: 'top',
                        position: 'right',
                        style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' }
                    }).showToast();
                };

                const doAction = async () => {
                    let targetId = estadoUndo.recreatedId;
                    if (!targetId) {
                        try {
                            await this.recargarConFiltros();
                            const match = (this.preguntas || []).find(
                                p => p.pregunta === snapshot.pregunta && p.respuesta === snapshot.respuesta
                            );
                            if (match) targetId = match.id;
                        } catch {}
                    }
                    if (!targetId) return;
                    const r = await fetch(`/api/preguntas/${targetId}`, {
                        method: 'DELETE',
                        headers: authManager.getAuthHeaders()
                    });
                    if (!r.ok) throw new Error('No se pudo rehacer la eliminación');
                    estadoUndo.recreatedId = null;
                    await this.recargarConFiltros();
                };

                window.UndoManager.record({
                    do: doAction,
                    undo: undoAction,
                    label: `Eliminar pregunta ${id}`,
                    skipPageRefresh: true
                });
            } else if (!snapshot) {
                console.warn('⚠️ [UNDO] Sin snapshot previo; no se puede deshacer la eliminación');
            }

            await this.recargarConFiltros();
            Toastify({
                text: 'Pregunta eliminada',
                duration: 3000,
                close: true,
                gravity: 'top',
                position: 'right',
                style: { background: 'linear-gradient(to right, #ff5f6d, #ffc371)' }
            }).showToast();
        } catch (error) {
            Toastify({
                text: error.message,
                duration: 4000,
                close: true,
                gravity: 'top',
                position: 'right',
                style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
            }).showToast();
        }
    },

    async comprobarApariciones() {
        try {
            // Obtener el texto de la respuesta actual
            const respuestaInput = document.getElementById('respuesta-pregunta');
            if (!respuestaInput || !respuestaInput.value.trim()) {
                Toastify({
                    text: 'Ingresa una respuesta para buscar apariciones',
                    duration: 3000,
                    close: true,
                    gravity: 'top',
                    position: 'right',
                    style: { background: 'linear-gradient(to right, #ff9966, #ff5e62)' }
                }).showToast();
                return;
            }
            
            const textoRespuesta = respuestaInput.value.trim();
            console.log('🔍 [APARICIONES] Buscando apariciones para:', textoRespuesta);
            
            // Mostrar modal con indicador de carga
            const modalApariciones = new bootstrap.Modal(document.getElementById('modal-apariciones'));
            modalApariciones.show();
            
            document.getElementById('apariciones-resumen').innerHTML = `
                <i class="fas fa-spinner fa-spin"></i> Buscando apariciones para: <strong>${textoRespuesta}</strong>
            `;
            document.getElementById('tabla-apariciones').innerHTML = '';
            
            // Llamar al endpoint de búsqueda de apariciones
            const response = await fetch(`/api/preguntas/buscar-apariciones?texto=${encodeURIComponent(textoRespuesta)}`, {
                headers: authManager.getAuthHeaders()
            });
            
            if (!response.ok) {
                throw new Error('Error al buscar apariciones');
            }
            
            const resultado = await response.json();
            console.log('✅ [APARICIONES] Resultado:', resultado);
            
            // Actualizar el resumen
            const totalApariciones = resultado.totalApariciones;
            document.getElementById('apariciones-resumen').innerHTML = `
                <i class="fas fa-info-circle"></i> Se encontraron <strong>${totalApariciones}</strong> apariciones para: <strong>${textoRespuesta}</strong>
            `;
            
            // Llenar la tabla con las apariciones
            const tbody = document.getElementById('tabla-apariciones');
            tbody.innerHTML = '';
            
            if (totalApariciones === 0) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="5" class="text-center">No se encontraron apariciones</td>
                    </tr>
                `;
                return;
            }
            
            resultado.apariciones.forEach(pregunta => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${pregunta.id}</td>
                    <td><span class="${this.getNivelColor(pregunta.nivel)}">${pregunta.nivel || ''}</span></td>
                    <td>${this.resaltarTexto(pregunta.pregunta, textoRespuesta)}</td>
                    <td>${this.resaltarTexto(pregunta.respuesta, textoRespuesta)}</td>
                    <td><span class="badge ${this.getEstadoColor(pregunta.estado)}">${pregunta.estado || ''}</span></td>
                `;
                tbody.appendChild(tr);
            });
            
        } catch (error) {
            console.error('Error al buscar apariciones:', error);
            document.getElementById('apariciones-resumen').innerHTML = `
                <i class="fas fa-exclamation-triangle"></i> Error al buscar apariciones: ${error.message}
            `;
            document.getElementById('apariciones-resumen').className = 'alert alert-danger mb-3';
        }
    },
    
    // Método auxiliar para resaltar el texto buscado
    resaltarTexto(texto, busqueda) {
        if (!texto) return '';
        if (!busqueda) return texto;
        
        const regex = new RegExp(busqueda.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'gi');
        return texto.replace(regex, match => `<mark class="bg-warning">${match}</mark>`);
    },

    limpiarFiltros() {
        document.getElementById('filtro-estado').value = '';
        document.getElementById('filtro-nivel').value = '';
        document.getElementById('filtro-tematica').value = '';
        document.getElementById('filtro-subtema').value = '';
        document.getElementById('filtro-autoria') && (document.getElementById('filtro-autoria').value = '');
        document.getElementById('filtro-texto') && (document.getElementById('filtro-texto').value = '');
        const tematicaToggle = document.getElementById('filtro-tematica-toggle');
        if (tematicaToggle) tematicaToggle.textContent = 'Todos';
        const subtemaToggle = document.getElementById('filtro-subtema-toggle');
        if (subtemaToggle) subtemaToggle.textContent = 'Todos';
        this.filtros = { tematica: '', nivel: '', estado: '', subtema: '', texto: '', autoria: '' };
        this.cargarPreguntas();
    },

    // Método para buscar apariciones desde la lista de preguntas
    async buscarAparicionesDesdeLista(id) {
        try {
            // Obtener la pregunta actual
            const pregunta = this.preguntas.find(p => p.id === id);
            if (!pregunta || !pregunta.respuesta) {
                Toastify({
                    text: 'No se encontró la respuesta para esta pregunta',
                    duration: 3000,
                    close: true,
                    gravity: 'top',
                    position: 'right',
                    style: { background: 'linear-gradient(to right, #ff9966, #ff5e62)' }
                }).showToast();
                return;
            }
            
            const textoRespuesta = pregunta.respuesta.trim();
            console.log('🔍 [APARICIONES] Buscando apariciones para:', textoRespuesta);
            
            // Mostrar modal con indicador de carga
            const modalApariciones = new bootstrap.Modal(document.getElementById('modal-apariciones'));
            modalApariciones.show();
            
            document.getElementById('apariciones-resumen').innerHTML = `
                <i class="fas fa-spinner fa-spin"></i> Buscando apariciones para: <strong>${textoRespuesta}</strong>
            `;
            document.getElementById('tabla-apariciones').innerHTML = '';
            
            // Llamar al endpoint de búsqueda de apariciones
            const response = await fetch(`/api/preguntas/buscar-apariciones?texto=${encodeURIComponent(textoRespuesta)}`, {
                headers: authManager.getAuthHeaders()
            });
            
            if (!response.ok) {
                throw new Error('Error al buscar apariciones');
            }
            
            const resultado = await response.json();
            console.log('✅ [APARICIONES] Resultado:', resultado);
            
            // Actualizar el resumen
            const totalApariciones = resultado.totalApariciones;
            document.getElementById('apariciones-resumen').innerHTML = `
                <i class="fas fa-info-circle"></i> Se encontraron <strong>${totalApariciones}</strong> apariciones para: <strong>${textoRespuesta}</strong>
            `;
            
            // Llenar la tabla con las apariciones
            const tbody = document.getElementById('tabla-apariciones');
            tbody.innerHTML = '';
            
            if (totalApariciones === 0) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="5" class="text-center">No se encontraron apariciones</td>
                    </tr>
                `;
                return;
            }
            
            resultado.apariciones.forEach(pregunta => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${pregunta.id}</td>
                    <td><span class="${this.getNivelColor(pregunta.nivel)}">${pregunta.nivel || ''}</span></td>
                    <td>${this.resaltarTexto(pregunta.pregunta, textoRespuesta)}</td>
                    <td>${this.resaltarTexto(pregunta.respuesta, textoRespuesta)}</td>
                    <td><span class="badge ${this.getEstadoColor(pregunta.estado)}">${pregunta.estado || ''}</span></td>
                `;
                tbody.appendChild(tr);
            });
            
        } catch (error) {
            console.error('Error al buscar apariciones:', error);
            document.getElementById('apariciones-resumen').innerHTML = `
                <i class="fas fa-exclamation-triangle"></i> Error al buscar apariciones: ${error.message}
            `;
            document.getElementById('apariciones-resumen').className = 'alert alert-danger mb-3';
        }
    }
};

// Inicialización cuando el documento está listo
document.addEventListener('DOMContentLoaded', async () => {
    // Cargar usuarios para el filtro de autoría
    await PreguntasManager.cargarUsuariosEnFiltro();
    
    // Cargar preguntas directamente (la autenticación ya se verifica en auth.js)
    await PreguntasManager.cargarPreguntas();
    
    // Cargar temas y subtemas para tenerlos disponibles
    await TemasManager.cargarTemas();
    await TemasManager.cargarSubtemas();

    // Añadir eventos de ordenación a las cabeceras
        const headers = [
        { id: 'id', idx: 0 },
        { id: 'creacionUsuario', idx: 1 },
        { id: 'nivel', idx: 2 },
        { id: 'tematica', idx: 3 },
        { id: 'subtema', idx: 4 },
        { id: 'pregunta', idx: 5 },
        { id: 'respuesta', idx: 6 },
        { id: 'datosExtra', idx: 7 },
        { id: 'fuentes', idx: 8 },
        { id: 'verificacionUsuario', idx: 9 },
        { id: 'notasVerificacion', idx: 10 },
        { id: 'notasDireccion', idx: 11 },
        { id: 'estado', idx: 12 }
    ];
    const ths = document.querySelectorAll('table thead th');
    headers.forEach((h, i) => {
        const th = ths[h.idx];
        if (!th) return;

        th.addEventListener('click', (e) => {
            // Verificar si se está redimensionando
            if (typeof isTableResizing === 'function' && isTableResizing('tabla-preguntas-header')) {
                console.log('❌ [ORDEN] Click bloqueado - se está redimensionando');
                e.preventDefault();
                e.stopPropagation();
                return false;
            }
            
            // Verificar si el click fue en la zona de redimensionamiento
            const rect = e.target.getBoundingClientRect();
            const clickX = e.clientX - rect.left;
            const headerWidth = rect.width;
            
            // Si el click está en los últimos 30px (zona de redimensionamiento), no ordenar
            if (clickX > headerWidth - 30) {
                console.log('❌ [ORDEN] Click en zona de redimensionamiento, bloqueando ordenamiento');
                e.preventDefault();
                e.stopPropagation();
                return false;
            }
            
            PreguntasManager.setOrden(h.id);
        });
        th.classList.add('sortable');
        // Importante: no machacar otros estilos inline (anchos del TableResizer)
        th.style.cursor = 'pointer';
    });

    // Filtros (un solo camino: filtrarPreguntas)
    document.getElementById('filtro-estado')?.addEventListener('change', () => PreguntasManager.filtrarPreguntas());
    document.getElementById('filtro-nivel')?.addEventListener('change', () => PreguntasManager.filtrarPreguntas());
    document.getElementById('filtro-autoria')?.addEventListener('change', () => PreguntasManager.filtrarPreguntas());

    let filtroTextoTimeout;
    document.getElementById('filtro-texto')?.addEventListener('input', () => {
        clearTimeout(filtroTextoTimeout);
        filtroTextoTimeout = setTimeout(() => PreguntasManager.filtrarPreguntas(), 400);
    });

    // Event listener para el formulario de crear pregunta (si existe)
    document.querySelector('#formCrearPregunta')?.addEventListener('submit', (e) => PreguntasManager.crearPregunta(e));

    // --- NUEVO: Resaltar y hacer scroll a la pregunta si hay id en la URL ---
    const params = new URLSearchParams(window.location.search);
    const idDestacado = params.get('id');
    console.log(`🔍 [URL] URL completa: ${window.location.href}`);
    console.log(`🔍 [URL] Parámetros de búsqueda: ${window.location.search}`);
    console.log(`🔍 [URL] ID encontrado: ${idDestacado}`);
    
    if (idDestacado) {
        console.log(`🎯 [REDIRECT] Iniciando búsqueda de pregunta ${idDestacado}...`);
        setTimeout(async () => {
            console.log(`🔍 [REDIRECT] Buscando fila con data-id='${idDestacado}'...`);
            let fila = document.querySelector(`#tabla-preguntas tr[data-id='${idDestacado}']`);
            console.log(`🔍 [REDIRECT] Fila encontrada:`, fila);
            
            // Si no se encuentra en la página actual, buscar en todas las páginas
            if (!fila) {
                console.log(`🔍 [REDIRECT] Pregunta ${idDestacado} no encontrada en página actual, buscando en todas las páginas...`);
                await buscarPreguntaEnTodasLasPaginas(idDestacado);
            } else {
                console.log(`✅ [REDIRECT] Pregunta ${idDestacado} encontrada en página actual, resaltando...`);
                // Si se encuentra en la página actual, resaltarla
                fila.classList.add('table-warning');
                fila.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        }, 500);
    } else {
        console.log(`⚠️ [URL] No se encontró parámetro 'id' en la URL`);
    }

    // Función para buscar una pregunta en todas las páginas
    async function buscarPreguntaEnTodasLasPaginas(idPregunta) {
        try {
            console.log(`🔍 [BUSCAR] Buscando pregunta ${idPregunta} con consulta directa...`);
            
            // Primero verificar que la pregunta existe
            console.log(`🔍 [BUSCAR] Verificando existencia de pregunta ${idPregunta}...`);
            const response = await apiManager.get(`/api/preguntas/${idPregunta}`);
            console.log(`🔍 [BUSCAR] Respuesta de API:`, response);
            
            if (!response || !response.id) {
                console.error(`❌ [BUSCAR] Pregunta ${idPregunta} no encontrada en la base de datos`);
                return;
            }
            
            console.log(`✅ [BUSCAR] Pregunta ${idPregunta} existe, calculando página objetivo...`);
            console.log(`📊 [BUSCAR] Tamaño de página: ${PreguntasManager.tamanioPagina}`);
            console.log(`📊 [BUSCAR] Total páginas: ${PreguntasManager.totalPaginas}`);
            
            // Calcular la página que debería contener esta pregunta
            // Asumiendo que las preguntas están ordenadas por ID
            const paginaObjetivo = Math.floor((idPregunta - 1) / PreguntasManager.tamanioPagina);
            console.log(`📄 [BUSCAR] Pregunta ${idPregunta} debería estar en la página ${paginaObjetivo}`);
            console.log(`📄 [BUSCAR] Rango de IDs en página ${paginaObjetivo}: ${paginaObjetivo * PreguntasManager.tamanioPagina + 1} - ${(paginaObjetivo + 1) * PreguntasManager.tamanioPagina}`);
            
            // Cargar la página calculada
            console.log(`🔄 [BUSCAR] Estableciendo página objetivo: ${paginaObjetivo}`);
            PreguntasManager.paginaActual = paginaObjetivo;
            console.log(`🔄 [BUSCAR] Página actual antes de cargar: ${PreguntasManager.paginaActual}`);
            await PreguntasManager.cargarPreguntas(false); // NO resetear para mantener la página
            console.log(`🔄 [BUSCAR] Página actual después de cargar: ${PreguntasManager.paginaActual}`);
            
            // Esperar a que se cargue
            await new Promise(resolve => setTimeout(resolve, 1000));
            
            // Buscar la pregunta en esta página
            console.log(`🔍 [BUSCAR] Buscando pregunta ${idPregunta} en página ${paginaObjetivo}...`);
            const fila = document.querySelector(`#tabla-preguntas tr[data-id='${idPregunta}']`);
            
            // Mostrar qué preguntas están en esta página
            const todasLasFilas = document.querySelectorAll('#tabla-preguntas tr[data-id]');
            const idsEnPagina = Array.from(todasLasFilas).map(f => f.getAttribute('data-id'));
            console.log(`📋 [BUSCAR] IDs en página ${paginaObjetivo}:`, idsEnPagina);
            console.log(`🔍 [BUSCAR] Fila encontrada:`, fila);
            
            if (fila) {
                console.log(`✅ [BUSCAR] Pregunta ${idPregunta} encontrada en la página ${paginaObjetivo}`);
                
                // Resaltar la pregunta
                fila.classList.add('table-warning');
                fila.scrollIntoView({ behavior: 'smooth', block: 'center' });
                
                // Actualizar la paginación visual
                PreguntasManager.renderizarPaginacion();
                
                return;
            }
            
            // Si no se encuentra en la página calculada, buscar en páginas cercanas
            console.log(`⚠️ [BUSCAR] No encontrada en página calculada, buscando en páginas cercanas...`);
            
            const rangos = [
                { inicio: Math.max(0, paginaObjetivo - 2), fin: Math.min(PreguntasManager.totalPaginas - 1, paginaObjetivo + 2) },
                { inicio: 0, fin: Math.min(10, PreguntasManager.totalPaginas - 1) }, // Primeras 10 páginas
                { inicio: Math.max(0, PreguntasManager.totalPaginas - 10), fin: PreguntasManager.totalPaginas - 1 } // Últimas 10 páginas
            ];
            
            for (const rango of rangos) {
                console.log(`🔍 [BUSCAR] Buscando en rango ${rango.inicio}-${rango.fin}...`);
                
                for (let pagina = rango.inicio; pagina <= rango.fin; pagina++) {
                    console.log(`🔍 [BUSCAR] Buscando en página ${pagina}...`);
                    
                    // Cargar la página
                    console.log(`🔄 [BUSCAR] Estableciendo página en búsqueda: ${pagina}`);
                    PreguntasManager.paginaActual = pagina;
                    console.log(`🔄 [BUSCAR] Página actual antes de cargar: ${PreguntasManager.paginaActual}`);
                    await PreguntasManager.cargarPreguntas(false); // NO resetear para mantener la página
                    console.log(`🔄 [BUSCAR] Página actual después de cargar: ${PreguntasManager.paginaActual}`);
                    
                    // Esperar un momento para que se cargue
                    await new Promise(resolve => setTimeout(resolve, 500));
                    
                    // Buscar la pregunta en esta página
                    const fila = document.querySelector(`#tabla-preguntas tr[data-id='${idPregunta}']`);
                    
                    // Mostrar qué preguntas están en esta página
                    const todasLasFilas = document.querySelectorAll('#tabla-preguntas tr[data-id]');
                    const idsEnPagina = Array.from(todasLasFilas).map(f => f.getAttribute('data-id'));
                    console.log(`📋 [BUSCAR] IDs en página ${pagina}:`, idsEnPagina.slice(0, 5), idsEnPagina.length > 5 ? '...' : '');
                    
                    if (fila) {
                        console.log(`✅ [BUSCAR] Pregunta ${idPregunta} encontrada en la página ${pagina}`);
                        
                        // Resaltar la pregunta
                        fila.classList.add('table-warning');
                        fila.scrollIntoView({ behavior: 'smooth', block: 'center' });
                        
                        // Actualizar la paginación visual
                        PreguntasManager.renderizarPaginacion();
                        
                        return; // Salir cuando se encuentra
                    }
                }
            }
            
            console.error(`❌ [BUSCAR] Pregunta ${idPregunta} no encontrada en ninguna página`);
            
        } catch (error) {
            console.error(`❌ [BUSCAR] Error al buscar pregunta ${idPregunta}:`, error);
        }
    }

    // Auto-scroll por acercar el cursor a los bordes deshabilitado: se usará solo la barra personalizada
});

window.mostrarFormularioPregunta = function() {
    const modal = document.getElementById('modal-pregunta');
    if (modal) {
        document.getElementById('modal-pregunta-titulo').textContent = 'Nueva Pregunta';
        
        // Limpiar formulario
        const form = document.getElementById('formCrearPregunta');
        form.reset();
        delete form.dataset.editId;
        
        // Inicializar el picker de temática para nueva pregunta
        const tematicas = TemasManager.temas.length > 0 ? TemasManager.temas : ['Geografía','Historia','Deportes','Ciencia','Arte'];
        TematicaPicker.init(tematicas);
        // Inicializar el picker de subtemas para nueva pregunta
        const subtemas = TemasManager.subtemas.length > 0 ? TemasManager.subtemas : ['Geografía','Historia','Deportes','Ciencia','Arte'];
        SubtemasPicker.init(subtemas);

        // Inicializar el select de estado con "borrador" por defecto
        const selectEstado = document.getElementById('estado-pregunta');
        if (selectEstado) {
            selectEstado.innerHTML = '';
            // Para una nueva pregunta, solo permitir "borrador" y "para_verificar"
            const estadosPermitidos = PreguntasManager.ordenarEstadosPregunta(['borrador', 'para_verificar']);
            estadosPermitidos.forEach(estado => {
                const opt = document.createElement('option');
                opt.value = estado;
                opt.text = PreguntasManager.formatearEstadoPregunta(estado);
                if (estado === 'borrador') {
                    opt.selected = true;
                }
                selectEstado.appendChild(opt);
            });
        }
        
        PreguntasManager.mostrarModalPregunta();
    } else {
        alert('Funcionalidad de crear pregunta no implementada o modal no encontrado.');
    }
};

window.cambiarPassword = function() {
    document.getElementById('form-cambiar-password').reset();
    const modal = new bootstrap.Modal(document.getElementById('modal-cambiar-password'));
    modal.show();
};

window.limpiarFiltros = function() {
    PreguntasManager.limpiarFiltros();
};

window.filtrarPreguntas = function() {
    PreguntasManager.filtrarPreguntas();
};

// ─── Selector de subtemas con búsqueda y multi-selección ───────────────────
const SubtemasPicker = {
    _subtemas: [],
    _seleccionados: [],

    init(subtemas) {
        this._subtemas = subtemas || [];
        this._seleccionados = [];
        this._bindEvents();
        this._renderTags();
        this._renderOpciones('');
        const input = document.getElementById('subtemas-busqueda');
        const dropdown = document.getElementById('subtemas-dropdown');
        if (input) input.value = '';
        if (dropdown) dropdown.style.display = 'none';
    },

    _bindEvents() {
        const input = document.getElementById('subtemas-busqueda');
        const dropdown = document.getElementById('subtemas-dropdown');
        if (!input || !dropdown) return;

        input.oninput = () => {
            this._renderOpciones(input.value);
            dropdown.style.display = 'block';
        };
        input.onfocus = () => {
            this._renderOpciones(input.value);
            dropdown.style.display = 'block';
        };

        document._subtemasPickerOutsideHandler && document.removeEventListener('mousedown', document._subtemasPickerOutsideHandler);
        document._subtemasPickerOutsideHandler = (e) => {
            if (!input.contains(e.target) && !dropdown.contains(e.target)) {
                dropdown.style.display = 'none';
            }
        };
        document.addEventListener('mousedown', document._subtemasPickerOutsideHandler);
    },

    _renderOpciones(filtro) {
        const cont = document.getElementById('subtemas-opciones');
        if (!cont) return;
        const texto = (filtro || '').trim().toLowerCase();
        const filtrados = this._subtemas
            .filter(s => s.toLowerCase().includes(texto))
            .slice(0, 10);

        if (filtrados.length === 0) {
            cont.innerHTML = '<div class="px-3 py-2 text-muted small">Sin resultados</div>';
            return;
        }
        cont.innerHTML = filtrados.map(s => {
            const sel = this._seleccionados.includes(s);
            const sEsc = s.replace(/\\/g, '\\\\').replace(/'/g, "\\'");
            return `<div class="subtemas-picker-opcion px-3 py-1 d-flex align-items-center gap-2${sel ? ' selected' : ''}"
                         style="cursor:pointer;" onmousedown="event.preventDefault(); SubtemasPicker.toggle('${sEsc}')">
                        <i class="fas ${sel ? 'fa-check-square' : 'fa-square'}" style="font-size:0.85em; width:14px;"></i>
                        <span>${s}</span>
                    </div>`;
        }).join('');
    },

    _renderTags() {
        const cont = document.getElementById('subtemas-seleccionados');
        if (!cont) return;
        cont.innerHTML = this._seleccionados.map(s => {
            const sEsc = s.replace(/\\/g, '\\\\').replace(/'/g, "\\'");
            return `<span class="badge bg-primary d-inline-flex align-items-center gap-1" style="font-size:0.82em; padding:4px 8px;">
                        ${s}
                        <button type="button" class="btn-close btn-close-white ms-1"
                                style="font-size:0.6em;" onmousedown="event.preventDefault(); SubtemasPicker.toggle('${sEsc}')"></button>
                    </span>`;
        }).join('');
    },

    toggle(subtema) {
        const idx = this._seleccionados.indexOf(subtema);
        if (idx >= 0) {
            this._seleccionados.splice(idx, 1);
        } else {
            this._seleccionados.push(subtema);
        }
        this._renderTags();
        const input = document.getElementById('subtemas-busqueda');
        const dropdown = document.getElementById('subtemas-dropdown');
        this._renderOpciones(input ? input.value : '');
        // Mantener el dropdown abierto tras seleccionar (el re-render desvincula e.target del DOM
        // y el listener exterior lo cerraría erróneamente)
        if (dropdown) dropdown.style.display = 'block';
        if (input) input.focus();
    },

    getSeleccionados() {
        return [...this._seleccionados];
    },

    setSeleccionados(arr) {
        this._seleccionados = arr ? [...arr] : [];
        this._renderTags();
        const input = document.getElementById('subtemas-busqueda');
        this._renderOpciones(input ? input.value : '');
    }
};

// ─── Selector de temática con búsqueda (selección única) ───────────────────
const TematicaPicker = {
    _tematicas: [],
    _seleccionada: null,

    init(tematicas, valorInicial = null) {
        this._tematicas = tematicas || [];
        this._seleccionada = valorInicial || null;
        this._bindEvents();
        this._renderTag();
        this._renderOpciones('');
        const input = document.getElementById('tematica-busqueda');
        const dropdown = document.getElementById('tematica-dropdown');
        if (input) input.value = '';
        if (dropdown) dropdown.style.display = 'none';
        this._updateHidden();
    },

    _bindEvents() {
        const input = document.getElementById('tematica-busqueda');
        const dropdown = document.getElementById('tematica-dropdown');
        if (!input || !dropdown) return;

        input.oninput = () => {
            this._renderOpciones(input.value);
            dropdown.style.display = 'block';
        };
        input.onfocus = () => {
            this._renderOpciones(input.value);
            dropdown.style.display = 'block';
        };

        document._tematicaPickerOutsideHandler && document.removeEventListener('mousedown', document._tematicaPickerOutsideHandler);
        document._tematicaPickerOutsideHandler = (e) => {
            if (!input.contains(e.target) && !dropdown.contains(e.target)) {
                dropdown.style.display = 'none';
            }
        };
        document.addEventListener('mousedown', document._tematicaPickerOutsideHandler);
    },

    _renderOpciones(filtro) {
        const cont = document.getElementById('tematica-opciones');
        if (!cont) return;
        const texto = (filtro || '').trim().toLowerCase();
        const filtrados = this._tematicas
            .filter(t => t.toLowerCase().includes(texto))
            .slice(0, 10);

        if (filtrados.length === 0) {
            cont.innerHTML = '<div class="px-3 py-2 text-muted small">Sin resultados</div>';
            return;
        }
        cont.innerHTML = filtrados.map(t => {
            const sel = this._seleccionada === t;
            const tEsc = t.replace(/\\/g, '\\\\').replace(/'/g, "\\'");
            return `<div class="subtemas-picker-opcion px-3 py-1 d-flex align-items-center gap-2${sel ? ' selected' : ''}"
                         style="cursor:pointer;" onmousedown="event.preventDefault(); TematicaPicker.seleccionar('${tEsc}')">
                        <i class="fas ${sel ? 'fa-dot-circle' : 'fa-circle'}" style="font-size:0.85em; width:14px;"></i>
                        <span>${t}</span>
                    </div>`;
        }).join('');
    },

    _renderTag() {
        const cont = document.getElementById('tematica-seleccionada');
        if (!cont) return;
        if (this._seleccionada) {
            const tEsc = this._seleccionada.replace(/\\/g, '\\\\').replace(/'/g, "\\'");
            cont.innerHTML = `<span class="badge bg-success d-inline-flex align-items-center gap-1" style="font-size:0.82em; padding:4px 8px;">
                                  ${this._seleccionada}
                                  <button type="button" class="btn-close btn-close-white ms-1"
                                          style="font-size:0.6em;" onmousedown="event.preventDefault(); TematicaPicker.limpiar()"></button>
                              </span>`;
        } else {
            cont.innerHTML = '';
        }
    },

    _updateHidden() {
        const hidden = document.getElementById('tematica-pregunta');
        if (hidden) hidden.value = this._seleccionada || '';
    },

    seleccionar(tematica) {
        this._seleccionada = tematica;
        this._renderTag();
        this._updateHidden();
        const input = document.getElementById('tematica-busqueda');
        const dropdown = document.getElementById('tematica-dropdown');
        if (input) input.value = '';
        this._renderOpciones('');
        if (dropdown) dropdown.style.display = 'none';
    },

    limpiar() {
        this._seleccionada = null;
        this._renderTag();
        this._updateHidden();
        const input = document.getElementById('tematica-busqueda');
        const dropdown = document.getElementById('tematica-dropdown');
        this._renderOpciones('');
        if (input) { input.value = ''; input.focus(); }
        if (dropdown) dropdown.style.display = 'block';
    },

    getSeleccionada() {
        return this._seleccionada;
    }
};

// Gestión de Temas y Subtemas
const TemasManager = {
    temas: [],
    subtemas: [],

    async cargarTemas() {
        try {
            const response = await fetch('/api/temas', {
                headers: authManager.getAuthHeaders()
            });
            if (!response.ok) throw new Error('Error al cargar temas');
            this.temas = await response.json();
            this.mostrarTemas();
        } catch (error) {
            console.error('Error al cargar temas:', error);
            mostrarError('Error al cargar temas: ' + error.message);
        }
    },

    async cargarSubtemas() {
        try {
            const response = await fetch('/api/temas/subtemas', {
                headers: authManager.getAuthHeaders()
            });
            if (!response.ok) throw new Error('Error al cargar subtemas');
            this.subtemas = await response.json();
            this.mostrarSubtemas();
        } catch (error) {
            console.error('Error al cargar subtemas:', error);
            mostrarError('Error al cargar subtemas: ' + error.message);
        }
    },

    async cargarEstadisticas() {
        try {
            const response = await fetch('/api/temas/estadisticas', {
                headers: authManager.getAuthHeaders()
            });
            if (!response.ok) throw new Error('Error al cargar estadísticas');
            const stats = await response.json();
            
            // Actualizar contadores en ambos modales
            document.getElementById('total-temas').textContent = stats.totalTemas;
            document.getElementById('total-subtemas').textContent = stats.totalSubtemas;
            document.getElementById('total-temas-sub').textContent = stats.totalTemas;
            document.getElementById('total-subtemas-sub').textContent = stats.totalSubtemas;
        } catch (error) {
            console.error('Error al cargar estadísticas:', error);
        }
    },

    mostrarTemas() {
        const tbody = document.getElementById('lista-temas');
        if (!tbody) return;
        
        tbody.innerHTML = '';
        this.temas.forEach((tema, index) => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${index + 1}</td>
                <td>${tema}</td>
                <td>
                    <button class="btn btn-sm btn-danger" onclick="TemasManager.eliminarTema('${tema}')">
                        <i class="fas fa-trash"></i> Eliminar
                    </button>
                </td>
            `;
            tbody.appendChild(tr);
        });

        // Rellenar dropdown de filtros de temática con estética Bootstrap
        const cont = document.getElementById('filtro-tematica-options');
        const search = document.getElementById('filtro-tematica-search');
        const hidden = document.getElementById('filtro-tematica');
        const toggle = document.getElementById('filtro-tematica-toggle');
        if (cont && search && hidden && toggle) {
            const render = (filtro = '') => {
                cont.innerHTML = '';
                const items = ['(Todos)', ...this.temas].filter(t => t.toLowerCase().includes(filtro.toLowerCase()));
                items.forEach(t => {
                    const btn = document.createElement('button');
                    btn.type = 'button';
                    btn.className = 'dropdown-item';
                    btn.textContent = t;
                    btn.onclick = () => {
                        const valor = t === '(Todos)' ? '' : t;
                        hidden.value = valor;
                        toggle.textContent = valor || 'Todos';
                        // Cerrar dropdown
                        document.body.click();
                        if (typeof filtrarPreguntas === 'function') filtrarPreguntas();
                    };
                    cont.appendChild(btn);
                });
            };
            render('');
            search.oninput = () => render(search.value || '');
        }
    },

    mostrarSubtemas() {
        const tbody = document.getElementById('lista-subtemas');
        if (!tbody) return;
        
        tbody.innerHTML = '';
        this.subtemas.forEach((subtema, index) => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${index + 1}</td>
                <td>${subtema}</td>
                <td>
                    <button class="btn btn-sm btn-danger" onclick="TemasManager.eliminarSubtema('${subtema}')">
                        <i class="fas fa-trash"></i> Eliminar
                    </button>
                </td>
            `;
            tbody.appendChild(tr);
        });

        // Rellenar dropdown de filtros de subtema con estética Bootstrap
        const cont = document.getElementById('filtro-subtema-options');
        const search = document.getElementById('filtro-subtema-search');
        const hidden = document.getElementById('filtro-subtema');
        const toggle = document.getElementById('filtro-subtema-toggle');
        if (cont && search && hidden && toggle) {
            const render = (filtro = '') => {
                cont.innerHTML = '';
                const items = ['(Todos)', ...this.subtemas].filter(s => s.toLowerCase().includes(filtro.toLowerCase()));
                items.forEach(s => {
                    const btn = document.createElement('button');
                    btn.type = 'button';
                    btn.className = 'dropdown-item';
                    btn.textContent = s;
                    btn.onclick = () => {
                        const valor = s === '(Todos)' ? '' : s;
                        hidden.value = valor;
                        toggle.textContent = valor || 'Todos';
                        // Cerrar dropdown
                        document.body.click();
                        if (typeof filtrarPreguntas === 'function') filtrarPreguntas();
                    };
                    cont.appendChild(btn);
                });
            };
            render('');
            search.oninput = () => render(search.value || '');
        }
    },

    async añadirTema(nombreTema) {
        try {
            const response = await fetch('/api/temas', {
                method: 'POST',
                headers: {
                    ...authManager.getAuthHeaders(),
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ tema: nombreTema })
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            const result = await response.json();
            mostrarExito(result.mensaje);
            
            // Recargar datos
            await this.cargarTemas();
            await this.cargarEstadisticas();
            
            // Limpiar formulario
            document.getElementById('nuevo-tema').value = '';
            
        } catch (error) {
            mostrarError('Error al añadir tema: ' + error.message);
        }
    },

    async añadirSubtema(nombreSubtema) {
        try {
            const response = await fetch('/api/temas/subtemas', {
                method: 'POST',
                headers: {
                    ...authManager.getAuthHeaders(),
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ subtema: nombreSubtema })
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText);
            }

            const result = await response.json();
            mostrarExito(result.mensaje);
            
            // Recargar datos
            await this.cargarSubtemas();
            await this.cargarEstadisticas();
            
            // Limpiar formulario
            document.getElementById('nuevo-subtema').value = '';
            
        } catch (error) {
            mostrarError('Error al añadir subtema: ' + error.message);
        }
    },

    async eliminarTema(nombreTema) {
        if (!confirm(`¿Estás seguro de que quieres eliminar el tema "${nombreTema}"?`)) {
            return;
        }

        try {
            const response = await fetch(`/api/temas/${encodeURIComponent(nombreTema)}`, {
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
            await this.cargarTemas();
            await this.cargarEstadisticas();
            
        } catch (error) {
            mostrarError('Error al eliminar tema: ' + error.message);
        }
    },

    async eliminarSubtema(nombreSubtema) {
        if (!confirm(`¿Estás seguro de que quieres eliminar el subtema "${nombreSubtema}"?`)) {
            return;
        }

        try {
            const response = await fetch(`/api/temas/subtemas/${encodeURIComponent(nombreSubtema)}`, {
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
            await this.cargarSubtemas();
            await this.cargarEstadisticas();
            
        } catch (error) {
            mostrarError('Error al eliminar subtema: ' + error.message);
        }
    }
};

// Funciones globales para los botones
window.mostrarGestionTemasSubtemas = function() {
    const modal = new bootstrap.Modal(document.getElementById('modal-gestion-temas-subtemas'));
    modal.show();
    TemasManager.cargarTemas();
    TemasManager.cargarSubtemas();
    TemasManager.cargarEstadisticas();
};

// Event listeners para los formularios
document.addEventListener('DOMContentLoaded', function() {
    // Formulario añadir tema
    document.getElementById('form-añadir-tema')?.addEventListener('submit', function(e) {
        e.preventDefault();
        const nombreTema = document.getElementById('nuevo-tema').value.trim();
        if (nombreTema) {
            TemasManager.añadirTema(nombreTema);
        }
    });

    // Formulario añadir subtema
    document.getElementById('form-añadir-subtema')?.addEventListener('submit', function(e) {
        e.preventDefault();
        const nombreSubtema = document.getElementById('nuevo-subtema').value.trim();
        if (nombreSubtema) {
            TemasManager.añadirSubtema(nombreSubtema);
        }
    });

    // Event listeners para las pestañas
    document.getElementById('temas-tab')?.addEventListener('shown.bs.tab', function() {
        TemasManager.cargarTemas();
    });

    document.getElementById('subtemas-tab')?.addEventListener('shown.bs.tab', function() {
        TemasManager.cargarSubtemas();
    });
    
    // Configurar el desplazamiento automático de la tabla
    setupAutoScroll();
}); 

// Función para configurar el desplazamiento automático de la tabla
function setupAutoScroll() {
    const scrollRightZone = document.getElementById('scroll-right');
    const scrollLeftZone = document.getElementById('scroll-left');
    const tableContainer = document.querySelector('.table-responsive');
    
    if (!scrollRightZone || !scrollLeftZone || !tableContainer) return;
    
    let scrollInterval;
    const scrollSpeed = 10;
    
    // Desplazamiento hacia la derecha
    scrollRightZone.addEventListener('mouseenter', function() {
        scrollInterval = setInterval(function() {
            tableContainer.scrollLeft += scrollSpeed;
        }, 50);
    });
    
    scrollRightZone.addEventListener('mouseleave', function() {
        clearInterval(scrollInterval);
    });
    
    // Desplazamiento hacia la izquierda
    scrollLeftZone.addEventListener('mouseenter', function() {
        scrollInterval = setInterval(function() {
            tableContainer.scrollLeft -= scrollSpeed;
        }, 50);
    });
    
    scrollLeftZone.addEventListener('mouseleave', function() {
        clearInterval(scrollInterval);
    });
}