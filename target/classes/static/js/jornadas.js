// Gestión de Jornadas - LSNLS
const JornadasManager = {
    jornadas: [],
    cuestionariosDisponibles: [],
    combosDisponibles: [],
    jornadaEditando: null,
    cuestionariosSeleccionados: [],
    combosSeleccionados: [],
    slotDestinoCuestionario: null,
    slotDestinoCombo: null,
    lastScrollY: 0,
    lastFocusJornadaId: null,
    reabrirEditarTrasSeleccion: false,
    selectorCuestPagina: 0,
    selectorComboPagina: 0,
    selectorPorPagina: 10,

    normalizeId(id) {
        const n = Number(id);
        return Number.isFinite(n) ? n : id;
    },

    idEnLista(lista, id) {
        const target = this.normalizeId(id);
        return (lista || []).findIndex(x => x != null && this.normalizeId(x) === target);
    },

    incluyeId(lista, id) {
        return this.idEnLista(lista, id) >= 0;
    },

    slotsVacios() {
        return [null, null, null, null, null, null];
    },

    normalizarSlots(lista) {
        const slots = this.slotsVacios();
        if (!Array.isArray(lista)) {
            return slots;
        }
        const posicional = lista.length === 6 || lista.some(x => x == null);
        if (posicional) {
            for (let i = 0; i < 6; i++) {
                slots[i] = lista[i] != null ? lista[i] : null;
            }
            return slots;
        }
        let j = 0;
        for (const item of lista) {
            if (item != null && j < 6) {
                slots[j++] = item;
            }
        }
        return slots;
    },

    contarOcupados(lista) {
        return (lista || []).filter(x => x != null).length;
    },

    colocarEnHueco(lista, id, desde) {
        const slots = this.normalizarSlots(lista);
        if (this.incluyeId(slots, id)) {
            return slots;
        }
        const start = Number.isInteger(desde) ? desde : 0;
        for (let i = start; i < 6; i++) {
            if (slots[i] == null) {
                slots[i] = this.normalizeId(id);
                return slots;
            }
        }
        for (let i = 0; i < start; i++) {
            if (slots[i] == null) {
                slots[i] = this.normalizeId(id);
                return slots;
            }
        }
        return null;
    },

    quitarDeSlots(lista, id) {
        const slots = this.normalizarSlots(lista);
        const idx = this.idEnLista(slots, id);
        if (idx >= 0) {
            slots[idx] = null;
        }
        return slots;
    },

    reabrirModalJornadaTrasSeleccion() {
        if (!this.reabrirEditarTrasSeleccion) return;
        const jid = document.getElementById('jornadaId')?.value;
        if (jid && jid !== 'Auto') {
            this.editarJornada(Number(jid));
        } else {
            const modalEl = document.getElementById('modalJornada');
            if (modalEl) {
                const inst = bootstrap.Modal.getInstance(modalEl);
                if (inst) inst.show();
                else new bootstrap.Modal(modalEl).show();
            }
        }
        this.reabrirEditarTrasSeleccion = false;
    },

    // Variables de paginación
    paginaActual: 0,
    tamanioPagina: 10,
    totalJornadas: 0,
    totalPaginas: 0,
    cargando: false,

    async init() {
        console.log('🚀 [JORNADAS] Inicializando gestión de jornadas');
        try {
            await this.cargarDatos(true);
            this.configurarEventos();
            this.configurarPermisos();
            console.log('✅ [JORNADAS] Inicialización completada');
        } catch (error) {
            console.error('❌ [JORNADAS] Error en inicialización:', error);
            Utils.showAlert('Error al cargar datos de jornadas', 'error');
        }
    },

    async recargarConFiltros() {
        await this.cargarDatos(false);
    },

    esAdminODireccion() {
        const usuario = JSON.parse(localStorage.getItem('usuario') || '{}');
        return usuario.rol === 'ROLE_ADMIN' || usuario.rol === 'ROLE_DIRECCION';
    },

    configurarPermisos() {
        // Crear jornada: solo ADMIN / DIRECCION (alineado con POST /api/jornadas)
        const btnNuevaJornada = document.querySelector('button[onclick*="mostrarModalCrear"]');
        if (btnNuevaJornada && !this.esAdminODireccion()) {
            btnNuevaJornada.style.display = 'none';
        }
    },

    rememberScroll() {
        this.lastScrollY = window.scrollY || window.pageYOffset || 0;
    },

    restoreScrollOrFocus() {
        // Intentar centrar la jornada de interés si la conocemos
        if (this.lastFocusJornadaId) {
            const card = document.querySelector(`.jornada-card[data-id="${this.lastFocusJornadaId}"]`);
            if (card) {
                card.scrollIntoView({ block: 'start', behavior: 'auto' });
                // Ajuste por cabecera fija si aplica
                try { window.scrollBy(0, -80); } catch (e) {}
                return;
            }
        }
        // Si no, restaurar posición previa
        if (typeof this.lastScrollY === 'number') {
            window.scrollTo({ top: this.lastScrollY, behavior: 'auto' });
        }
    },
    
    // Función para seleccionar cuestionarios directamente sin pasar por el editor
    seleccionarCuestionariosDirecto(jornadaId, slotIndex) {
        this.jornadaEditando = this.jornadas.find(j => j.id === jornadaId);
        if (!this.puedeEditar(this.jornadaEditando)) {
            Utils.showAlert('Esta jornada está bloqueada por estado y no se puede editar.', 'error');
            return;
        }
        this.slotDestinoCuestionario = Number.isInteger(slotIndex) ? slotIndex : 0;
        this.cuestionariosSeleccionados = this.normalizarSlots(this.jornadaEditando.cuestionarioIds);
        this.seleccionarCuestionarios();
    },

    seleccionarCombosDirecto(jornadaId, slotIndex) {
        this.jornadaEditando = this.jornadas.find(j => j.id === jornadaId);
        if (!this.puedeEditar(this.jornadaEditando)) {
            Utils.showAlert('Esta jornada está bloqueada por estado y no se puede editar.', 'error');
            return;
        }
        this.slotDestinoCombo = Number.isInteger(slotIndex) ? slotIndex : 0;
        this.combosSeleccionados = this.normalizarSlots(this.jornadaEditando.comboIds);
        this.seleccionarCombos();
    },

    async cargarDatos(resetear = false) {
        console.log('📡 [JORNADAS] Cargando datos...');
        try {
            if (resetear) {
                this.paginaActual = 0;
                this.jornadas = [];
            }

            this.cargando = true;
            this.mostrarEstadoCarga();

            // Obtener filtros del formulario
            const estado = document.getElementById('filtroEstado')?.value || '';
            const fechaDesde = document.getElementById('filtroFechaDesde')?.value || '';
            const fechaHasta = document.getElementById('filtroFechaHasta')?.value || '';
            const buscar = document.getElementById('filtroBuscar')?.value || '';

            const params = new URLSearchParams({
                page: this.paginaActual,
                size: this.tamanioPagina,
                sortBy: 'id',
                sortDir: 'desc'
            });

            // Agregar filtros a los parámetros si tienen valor
            if (estado) params.append('estado', estado);
            if (fechaDesde) params.append('fechaDesde', fechaDesde);
            if (fechaHasta) params.append('fechaHasta', fechaHasta);
            if (buscar) params.append('buscar', buscar);

            const [jornadasRes, cuestionariosRes, combosRes] = await Promise.all([
                apiManager.get(`/api/jornadas?${params}`),
                apiManager.get('/api/jornadas/cuestionarios-disponibles'),
                apiManager.get('/api/jornadas/combos-disponibles')
            ]);

            // Siempre reemplazar las jornadas con las de la página actual
            this.jornadas = jornadasRes.datos?.content || [];

            this.totalJornadas = jornadasRes.datos?.totalElements || 0;
            this.totalPaginas = jornadasRes.datos?.totalPages || 0;
            this.paginaActual = jornadasRes.datos?.number || 0;

            this.cuestionariosDisponibles = cuestionariosRes.datos || [];
            this.combosDisponibles = combosRes.datos || [];

            console.log(`✅ [JORNADAS] Datos cargados: ${this.jornadas.length} jornadas, ${this.cuestionariosDisponibles.length} cuestionarios, ${this.combosDisponibles.length} combos`);
            
            // Mostrar las jornadas después de cargar los datos
            this.mostrarJornadas();
        } catch (error) {
            console.error('❌ [JORNADAS] Error al cargar datos:', error);
            throw error;
        } finally {
            this.cargando = false;
            this.ocultarEstadoCarga();
            this.actualizarPaginacion();
        }
    },

    configurarEventos() {
        // Configurar eventos solo si los elementos existen (ids antiguos o nuevos)
        const inpC = document.getElementById('buscarCuestionarios');
        if (inpC) {
            inpC.addEventListener('input', (e) => this.filtrarCuestionarios(e.target.value));
        }

        const inpCb = document.getElementById('buscarCombos');
        if (inpCb) {
            inpCb.addEventListener('input', (e) => this.filtrarCombos(e.target.value));
        }

        const filtCuTem = document.getElementById('filtroCuestTematica');
        if (filtCuTem) {
            filtCuTem.addEventListener('change', () => this.buscarCuestionariosSelector());
        }

        const filtCoTipo = document.getElementById('filtroComboTipo');
        const filtCoTem = document.getElementById('filtroComboTematica');
        if (filtCoTipo) {
            filtCoTipo.addEventListener('change', () => this.buscarCombosSelector());
        }
        if (filtCoTem) {
            filtCoTem.addEventListener('change', () => this.buscarCombosSelector());
        }

        this.configurarTeclasSelectores();
        this.configurarReaperturaSelectores();
    },

    esTeclaConfirmarSelector(e) {
        return e.key === 'Enter' || e.key === ' ' || e.key === 'Spacebar' || e.code === 'Space';
    },

    debeIgnorarTeclaSelector(e) {
        const tag = (e.target && e.target.tagName) ? e.target.tagName.toUpperCase() : '';
        if (tag === 'SELECT' || tag === 'BUTTON' || tag === 'TEXTAREA' || tag === 'A') return true;
        if (e.target && e.target.getAttribute && e.target.getAttribute('data-bs-dismiss')) return true;
        return false;
    },

    configurarTeclasSelectores() {
        const enlazar = (modalId, confirmarFn) => {
            const modal = document.getElementById(modalId);
            if (!modal || modal.dataset.teclasSelector) return;
            modal.dataset.teclasSelector = '1';
            modal.addEventListener('keydown', (e) => {
                if (!this.esTeclaConfirmarSelector(e) || this.debeIgnorarTeclaSelector(e)) return;
                e.preventDefault();
                e.stopPropagation();
                confirmarFn.call(this);
            });
        };
        enlazar('modalSelectorCuestionarios', this.confirmarSelectorCuestionariosConTeclado);
        enlazar('modalSelectorCombos', this.confirmarSelectorCombosConTeclado);
    },

    configurarReaperturaSelectores() {
        const enlazar = (id) => {
            const el = document.getElementById(id);
            if (!el || el.dataset.reabrirTrasHide) return;
            el.dataset.reabrirTrasHide = '1';
            el.addEventListener('hidden.bs.modal', () => {
                if (this.reabrirEditarTrasSeleccion) {
                    this.reabrirModalJornadaTrasSeleccion();
                }
            });
        };
        enlazar('modalSelectorCuestionarios');
        enlazar('modalSelectorCombos');
    },


    mostrarEstadoCarga() {
        const container = document.getElementById('listaJornadas');
        if (container) {
            container.innerHTML = `
                <div class="text-center py-5">
                    <i class="fas fa-spinner fa-spin fa-2x"></i>
                    <p class="mt-2">Cargando jornadas...</p>
                </div>
            `;
        }
    },

    ocultarEstadoCarga() {
        // El estado de carga se oculta automáticamente cuando se muestran las jornadas
    },

    actualizarPaginacion() {
        console.log('🔄 [PAGINACION] Actualizando paginación con botones de páginas...');
        console.log('🔄 [PAGINACION] Estado - jornadas.length:', this.jornadas.length, 'totalJornadas:', this.totalJornadas, 'paginaActual:', this.paginaActual, 'totalPaginas:', this.totalPaginas);
        
        // Usar el contenedor de paginación del HTML
        const paginacionContainer = document.getElementById('paginacion-jornadas');
        const infosPaginacion = [
            document.getElementById('info-paginacion-jornadas'),
            document.getElementById('info-paginacion-jornadas-top')
        ].filter(Boolean);
        
        if (!paginacionContainer) {
            console.error('❌ [PAGINACION] Contenedor de paginación no encontrado');
            return;
        }

        // Actualizar información de paginación (arriba y abajo)
        const inicio = (this.paginaActual * this.tamanioPagina) + 1;
        const fin = Math.min((this.paginaActual + 1) * this.tamanioPagina, this.totalJornadas);
        const textoInfo = `Mostrando ${inicio}-${fin} de ${this.totalJornadas} jornadas`;
        infosPaginacion.forEach(el => { el.textContent = textoInfo; });

        // Limpiar botones existentes
        paginacionContainer.innerHTML = '';

        if (this.totalPaginas <= 1) {
            console.log('✅ [PAGINACION] Solo hay una página, no se muestran botones');
            return;
        }

        // Crear botón "Primera"
        const primeraPagina = document.createElement('li');
        primeraPagina.className = `page-item ${this.paginaActual === 0 ? 'disabled' : ''}`;
        primeraPagina.innerHTML = `<a class="page-link" href="#" onclick="JornadasManager.irAPagina(0)">Primera</a>`;
        paginacionContainer.appendChild(primeraPagina);

        // Crear botón "Anterior"
        const paginaAnterior = document.createElement('li');
        paginaAnterior.className = `page-item ${this.paginaActual === 0 ? 'disabled' : ''}`;
        paginaAnterior.innerHTML = `<a class="page-link" href="#" onclick="JornadasManager.irAPagina(${this.paginaActual - 1})">Anterior</a>`;
        paginacionContainer.appendChild(paginaAnterior);

        // Calcular rango de páginas a mostrar
        const inicioRango = Math.max(0, this.paginaActual - 2);
        const finRango = Math.min(this.totalPaginas - 1, this.paginaActual + 2);

        // Mostrar páginas en el rango
        for (let i = inicioRango; i <= finRango; i++) {
            const pagina = document.createElement('li');
            pagina.className = `page-item ${i === this.paginaActual ? 'active' : ''}`;
            pagina.innerHTML = `<a class="page-link" href="#" onclick="JornadasManager.irAPagina(${i})">${i + 1}</a>`;
            paginacionContainer.appendChild(pagina);
        }

        // Crear botón "Siguiente"
        const paginaSiguiente = document.createElement('li');
        paginaSiguiente.className = `page-item ${this.paginaActual >= this.totalPaginas - 1 ? 'disabled' : ''}`;
        paginaSiguiente.innerHTML = `<a class="page-link" href="#" onclick="JornadasManager.irAPagina(${this.paginaActual + 1})">Siguiente</a>`;
        paginacionContainer.appendChild(paginaSiguiente);

        // Crear botón "Última"
        const ultimaPagina = document.createElement('li');
        ultimaPagina.className = `page-item ${this.paginaActual >= this.totalPaginas - 1 ? 'disabled' : ''}`;
        ultimaPagina.innerHTML = `<a class="page-link" href="#" onclick="JornadasManager.irAPagina(${this.totalPaginas - 1})">Última</a>`;
        paginacionContainer.appendChild(ultimaPagina);

        console.log('✅ [PAGINACION] Botones de paginación creados correctamente');
    },

    irAPagina(pagina) {
        if (pagina < 0 || pagina >= this.totalPaginas || pagina === this.paginaActual) {
            return;
        }
        
        console.log(`🔄 [PAGINACION] Navegando a página ${pagina + 1}`);
        this.paginaActual = pagina;
        this.cargarDatos(); // Cargar datos de la nueva página
    },

    cambiarTamanioPagina() {
        const select = document.getElementById('tamanio-pagina-jornadas');
        if (select) {
            this.tamanioPagina = parseInt(select.value);
            console.log(`🔄 [PAGINACION] Cambiando tamaño de página a ${this.tamanioPagina}`);
            this.paginaActual = 0;
            this.cargarDatos();
        }
    },

    mostrarJornadas() {
        const container = document.getElementById('listaJornadas');
        
        if (this.jornadas.length === 0) {
            container.innerHTML = `
                <div class="text-center py-5">
                    <i class="fas fa-calendar-plus fa-3x text-muted"></i>
                    <h5 class="mt-3 text-muted">No hay jornadas registradas</h5>
                    <p class="text-muted">Crea la primera jornada para comenzar</p>
                    <button class="btn btn-primary" onclick="JornadasManager.mostrarModalCrear()">
                        <i class="fas fa-plus"></i> Nueva Jornada
                    </button>
                </div>
            `;
            return;
        }

        // No mostramos tabla general, iremos directamente a las tarjetas con la información
        let html = ``;
        
        // Agregar sección de tarjetas con cuestionarios y combos
        html += `
            <div class="mt-4">
        `;
        
        // Agregar las cards para cada jornada
        this.jornadas.forEach(jornada => {
            html += this.generarCardJornada(jornada);
        });
        
        html += `</div>`;
        
        container.innerHTML = html;

        if (typeof SyncMonitor !== 'undefined') {
            SyncMonitor.resetFromVisible(this.jornadas.map(j => ({
                entityType: 'JORNADA',
                entityId: j.id,
                version: j.version || 0,
                label: j.nombre || `Jornada ${j.id}`
            })));
        }
    },

    generarCardJornada(jornada) {
        // Preparar los cuestionarios y combos (asegurar que existan arrays)
        const cuestionarios = this.normalizarSlots(jornada.cuestionarios);
        const combos = this.normalizarSlots(jornada.combos);
        // Mapear estado backend -> etiquetas del front
        const estadoVista = jornada.estado || 'preparacion';
        
        // Generar slots de cuestionarios (6 en total)
        let cuestionariosHtml = '';
        for (let i = 0; i < 6; i++) {
            const c = cuestionarios[i];
            if (c && c.id) {
                const esReutilizado = !!c.reutilizado;
                cuestionariosHtml += `
                    <div class="cuestionario-slot p-2 border rounded ${esReutilizado ? 'bg-success bg-opacity-10' : ''}" style="${esReutilizado ? 'border-color:#28a745 !important;' : 'background-color:#ffffff; border-color:#e9ecef !important;'}">
                        <div class="d-flex justify-content-between align-items-center">
                            <span style="font-weight: 500; color: #495057;">Cuestionario ${c.id}</span>
                            <div class="btn-group btn-group-sm">
                                <button class="btn btn-outline-info btn-sm" onclick="JornadasManager.verPreguntasCuestionario(${c.id})" title="Ver preguntas">
                                    <i class="fas fa-eye"></i>
                                </button>
                                <button class="btn btn-outline-secondary btn-sm" onclick="JornadasManager.mostrarHistorialCuestionario(${c.id})" title="Ver historial">
                                    <i class="fas fa-history"></i>
                                </button>
                                ${JornadasManager.puedeEditar(jornada) ? (esReutilizado ? `
                                    <button class="btn btn-outline-danger btn-sm" onclick="JornadasManager.quitarReutilizacionCuestionario(${c.id}, ${jornada.id})" title="Quitar reutilización">
                                        <i class="fas fa-undo"></i>
                                    </button>
                                ` : `
                                    <button class="btn btn-outline-success btn-sm" onclick="JornadasManager.reutilizarCuestionario(${c.id}, ${jornada.id})" title="Reutilizar cuestionario">
                                        <i class="fas fa-recycle"></i>
                                    </button>
                                `) : ''}
                                ${JornadasManager.puedeEditar(jornada) ? `
                                    <button class="btn btn-outline-danger btn-sm" onclick="JornadasManager.eliminarCuestionarioDeJornada(${jornada.id}, ${c.id})" title="Borrar del slot">
                                        <i class="fas fa-trash"></i>
                                    </button>
                                ` : ``}
                            </div>
                        </div>
                        <small style="${esReutilizado ? 'color:#198754; font-weight:600;' : 'color:#6c757d;'}">${esReutilizado ? 'Reutilizado' : (c.tematica || 'Sin temática')}</small>
                    </div>
                `;
            } else {
                // Slot vacío con botón de añadir
                cuestionariosHtml += `
                    <div class="cuestionario-slot empty-slot p-2 border rounded" style="background-color: #f8f9fa; border-color: #e9ecef !important; border-style: dashed !important;">
                        ${JornadasManager.puedeEditar(jornada) ? `
                            <button class="btn btn-sm btn-outline-success w-100" 
                                    onclick="JornadasManager.seleccionarCuestionariosDirecto(${jornada.id}, ${i})">
                                <i class="fas fa-plus"></i> Añadir
                            </button>
                        ` : `<div class="text-muted small text-center">Solo lectura</div>`}
                    </div>
                `;
            }
        }
        
        // Mapeo de tipos de combo a nombres completos
        const tipoComboNombres = {
            'P': 'Premio',
            'A': 'Asequible',
            'D': 'Difícil',
            'R': 'Rescate'
        };
        
        // Generar slots de combos (6 en total)
        let combosHtml = '';
        for (let i = 0; i < 6; i++) {
            const c = combos[i];
            if (c && c.id) {
                const esReutilizado = !!c.reutilizado;
                // Obtener el nombre completo del tipo o usar el valor original
                const tipoNombre = tipoComboNombres[c.tipo] || c.tipo || 'Sin tipo';
                
                combosHtml += `
                    <div class="combo-slot p-2 border rounded ${esReutilizado ? 'bg-success bg-opacity-10' : ''}" style="${esReutilizado ? 'border-color:#28a745 !important;' : 'background-color:#ffffff; border-color:#e9ecef !important;'}">
                        <div class="d-flex justify-content-between align-items-center">
                            <span style="font-weight: 500; color: #495057;">Combo ${c.id}</span>
                            <div class="btn-group btn-group-sm">
                                <button class="btn btn-outline-info btn-sm" onclick="JornadasManager.verPreguntasCombo(${c.id})" title="Ver preguntas">
                                    <i class="fas fa-eye"></i>
                                </button>
                                <button class="btn btn-outline-secondary btn-sm" onclick="JornadasManager.mostrarHistorialCombo(${c.id})" title="Ver historial">
                                    <i class="fas fa-history"></i>
                                </button>
                                ${JornadasManager.puedeEditar(jornada) ? (esReutilizado ? `
                                    <button class="btn btn-outline-danger btn-sm" onclick="JornadasManager.quitarReutilizacionCombo(${c.id}, ${jornada.id})" title="Quitar reutilización">
                                        <i class="fas fa-undo"></i>
                                    </button>
                                ` : `
                                    <button class="btn btn-outline-success btn-sm" onclick="JornadasManager.reutilizarCombo(${c.id}, ${jornada.id})" title="Reutilizar combo">
                                        <i class="fas fa-recycle"></i>
                                    </button>
                                `) : ''}
                                ${JornadasManager.puedeEditar(jornada) ? `
                                    <button class="btn btn-outline-danger btn-sm" onclick="JornadasManager.eliminarComboDeJornada(${jornada.id}, ${c.id})" title="Borrar del slot">
                                        <i class="fas fa-trash"></i>
                                    </button>
                                ` : ``}
                            </div>
                        </div>
                        <small style="${esReutilizado ? 'color:#198754; font-weight:600;' : 'color:#6c757d;'}">${esReutilizado ? 'Reutilizado' : tipoNombre}</small>
                    </div>
                `;
            } else {
                // Slot vacío con botón de añadir
                combosHtml += `
                    <div class="combo-slot empty-slot p-2 border rounded" style="background-color: #f8f9fa; border-color: #e9ecef !important; border-style: dashed !important;">
                        ${JornadasManager.puedeEditar(jornada) ? `
                            <button class="btn btn-sm btn-outline-success w-100" 
                                    onclick="JornadasManager.seleccionarCombosDirecto(${jornada.id}, ${i})">
                                <i class="fas fa-plus"></i> Añadir
                            </button>
                        ` : `<div class="text-muted small text-center">Solo lectura</div>`}
                    </div>
                `;
            }
        }

        const estadoBadge = this.getEstadoBadge(estadoVista);
        const fecha = jornada.fechaJornada ? new Date(jornada.fechaJornada).toLocaleDateString('es-ES') : 'Sin fecha';
        
        // Selector de estado (solo se muestra si el usuario puede gestionar el estado)
        const selectorEstado = this.puedeGestionarEstado(jornada) ? `
            <select class=\"form-select form-select-sm\" style=\"width: auto; min-width: 130px\" 
                    onchange=\"JornadasManager.cambiarEstado(${jornada.id}, this.value)\">\n\
                <option value=\"preparacion\" ${estadoVista === 'preparacion' ? 'selected' : ''}>Preparación</option>\n\
                <option value=\"lista\" ${estadoVista === 'lista' ? 'selected' : ''}>Lista</option>\n\
                <option value=\"en_grabacion\" ${estadoVista === 'en_grabacion' ? 'selected' : ''}>En Grabación</option>\n\
                <option value=\"completada\" ${estadoVista === 'completada' ? 'selected' : ''}>Completada</option>\n\
                <option value=\"archivada\" ${estadoVista === 'archivada' ? 'selected' : ''}>Archivada</option>\n\
            </select>
        ` : estadoBadge;
        
        return `
            <div class="jornada-card mb-4 p-3 border rounded shadow-sm" data-id="${jornada.id}" style="background-color: #f8f9fa; border-left: 4px solid #0066cc !important;">
                <div class="mb-3">
                    <div class="row align-items-center">
                        <div class="col-md-1">
                            <span style="font-weight: bold; font-size: 1.2em; color: #0066cc;">${jornada.id}</span>
                        </div>
                        <div class="col-md-2">
                            <span style="font-weight: 500; color: #495057;">${jornada.nombre}</span>
                        </div>
                        <div class="col-md-2">
                            ${selectorEstado}
                        </div>
                        <div class="col-md-2">
                            <span style="color: #6c757d; font-size: 0.9em;">${fecha}</span>
                        </div>
                        <div class="col-md-2">
                            <span style="color: #6c757d; font-size: 0.9em;">${jornada.lugar || 'No especificado'}</span>
                        </div>
                        <div class="col-md-3">
                            <div class="btn-group">
                                <button class="btn btn-outline-primary btn-sm" onclick="JornadasManager.verDetalle(${jornada.id})" title="Ver detalle">
                                    <i class="fas fa-eye"></i>
                                </button>
                                <button class="btn btn-outline-success btn-sm" onclick="JornadasManager.exportarExcel(${jornada.id})" title="Exportar Excel">
                                    <i class="fas fa-file-excel"></i>
                                </button>
                                ${this.puedeEditar(jornada) ? `
                                    <button class="btn btn-outline-warning btn-sm" onclick="JornadasManager.editarJornada(${jornada.id})" title="Editar">
                                        <i class="fas fa-edit"></i>
                                    </button>
                                ` : ''}
                                ${this.puedeEliminar(jornada) ? `
                                    <button class="btn btn-outline-danger btn-sm" onclick="JornadasManager.eliminarJornada(${jornada.id})" title="Eliminar">
                                        <i class="fas fa-trash"></i>
                                    </button>
                                ` : ''}
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- Línea divisoria con texto "CUESTIONARIOS" -->
                <div class="mt-4 mb-3">
                    <hr style="border-color: #dee2e6; margin: 0;">
                    <div class="text-center">
                        <span class="badge bg-light text-dark px-3 py-1" style="font-size: 0.8em; font-weight: 500;">CUESTIONARIOS</span>
                    </div>
                </div>
                
                <!-- Cuestionarios -->
                <div class="cuestionarios-container">
                    <div class="cuestionarios-grid">
                        ${cuestionariosHtml}
                    </div>
                </div>
                
                <!-- Notas de dirección de cuestionarios -->
                <div class="mt-3 mb-3">
                    <label class="form-label fw-bold mb-2" style="font-size: 0.9em;">Notas de dirección (Cuestionarios):</label>
                    <div class="cuestionarios-grid">
                        ${this.generarNotasDireccionCuestionariosVista3(cuestionarios, jornada)}
                    </div>
                </div>
                
                <!-- Línea divisoria con texto "COMBOS" -->
                <div class="mt-4 mb-3">
                    <hr style="border-color: #dee2e6; margin: 0;">
                    <div class="text-center">
                        <span class="badge bg-light text-dark px-3 py-1" style="font-size: 0.8em; font-weight: 500;">COMBOS</span>
                    </div>
                </div>
                
                <!-- Combos -->
                <div class="combos-container">
                    <div class="combos-grid">
                        ${combosHtml}
                    </div>
                </div>
                
                <!-- Notas de dirección de combos -->
                <div class="mt-3 mb-3">
                    <label class="form-label fw-bold mb-2" style="font-size: 0.9em;">Notas de dirección (Combos):</label>
                    <div class="combos-grid">
                        ${this.generarNotasDireccionCombosVista3(combos, jornada)}
                    </div>
                </div>
            </div>
        `;
    },
    
    generarNotasDireccionCuestionariosVista3(cuestionarios, jornada) {
        // Generar recuadros del mismo tamaño que los slots de cuestionarios
        let html = '';
        for (let i = 0; i < 6; i++) {
            const c = cuestionarios[i];
            if (c && c.id) {
                const notas = c.notasDireccion || '';
                html += `
                    <div class="cuestionario-slot p-2 border rounded" style="background-color:#ffffff; border-color:#e9ecef !important;">
                        <label class="form-label small fw-bold mb-1">Cuestionario ${c.id}:</label>
                        <textarea 
                            class="form-control form-control-sm border-0 p-0" 
                            id="notas-direccion-cuestionario-${jornada.id}-${c.id}" 
                            rows="6" 
                            placeholder="Notas de dirección..."
                            style="resize: both; overflow: auto; background: transparent; min-height: 120px;"
                            ${this.puedeEditar(jornada) ? '' : 'readonly'}
                            onblur="JornadasManager.guardarNotasDireccionCuestionario(${c.id}, this.value)"
                        >${notas}</textarea>
                    </div>
                `;
            } else {
                // Slot vacío
                html += `
                    <div class="cuestionario-slot empty-slot p-2 border rounded" style="background-color: #f8f9fa; border-color:#e9ecef !important; border-style: dashed !important;">
                        <div class="text-muted small text-center">-</div>
                    </div>
                `;
            }
        }
        return html;
    },
    
    generarNotasDireccionCombosVista3(combos, jornada) {
        // Vista 3: Generar recuadros del mismo tamaño que los slots de combos
        let html = '';
        for (let i = 0; i < 6; i++) {
            const c = combos[i];
            if (c && c.id) {
                const notas = c.notasDireccion || '';
                html += `
                    <div class="combo-slot p-2 border rounded" style="background-color:#ffffff; border-color:#e9ecef !important;">
                        <label class="form-label small fw-bold mb-1">Combo ${c.id}:</label>
                        <textarea 
                            class="form-control form-control-sm border-0 p-0" 
                            id="notas-direccion-combo-${jornada.id}-${c.id}" 
                            rows="6" 
                            placeholder="Notas de dirección..."
                            style="resize: both; overflow: auto; background: transparent; min-height: 120px;"
                            ${this.puedeEditar(jornada) ? '' : 'readonly'}
                            onblur="JornadasManager.guardarNotasDireccionCombo(${c.id}, this.value)"
                        >${notas}</textarea>
                    </div>
                `;
            } else {
                // Slot vacío
                html += `
                    <div class="combo-slot empty-slot p-2 border rounded" style="background-color: #f8f9fa; border-color:#e9ecef !important; border-style: dashed !important;">
                        <div class="text-muted small text-center">-</div>
                    </div>
                `;
            }
        }
        return html;
    },
    
    async guardarNotasDireccionCuestionario(cuestionarioId, notas) {
        try {
            await apiManager.put(`/api/cuestionarios/${cuestionarioId}/notas-direccion`, {
                notasDireccion: notas
            });
            
            Toastify({
                text: 'Notas de dirección actualizadas',
                duration: 2000,
                close: true,
                gravity: 'top',
                position: 'right',
                style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' }
            }).showToast();
        } catch (error) {
            console.error('Error al guardar notas de dirección:', error);
            Toastify({
                text: 'Error al guardar notas: ' + (error.message || 'Error desconocido'),
                duration: 3000,
                close: true,
                gravity: 'top',
                position: 'right',
                style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
            }).showToast();
        }
    },
    
    async guardarNotasDireccionCombo(comboId, notas) {
        try {
            await apiManager.put(`/api/combos/${comboId}`, {
                notasDireccion: notas
            });
            
            Toastify({
                text: 'Notas de dirección actualizadas',
                duration: 2000,
                close: true,
                gravity: 'top',
                position: 'right',
                style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' }
            }).showToast();
        } catch (error) {
            console.error('Error al guardar notas de dirección:', error);
            Toastify({
                text: 'Error al guardar notas: ' + (error.message || 'Error desconocido'),
                duration: 3000,
                close: true,
                gravity: 'top',
                position: 'right',
                style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
            }).showToast();
        }
    },

    async eliminarCuestionarioDeJornada(jornadaId, cuestionarioId) {
        try {
            this.rememberScroll();
            const response = await apiManager.get(`/api/jornadas/${jornadaId}`);
            const jornada = response?.datos || response;
            if (!jornada) throw new Error('No se pudo cargar la jornada');
            
            // Capturar SOLO los campos editables (sin version, creacion_usuario_id, etc.)
            const prevCuestionarioIds = this.normalizarSlots(jornada.cuestionarioIds);
            const nuevaCuestionarioIds = this.quitarDeSlots(prevCuestionarioIds, cuestionarioId);
            
            const doAction = async () => {
                console.log('[UNDO][do][eliminar-cuestionario] Eliminando cuestionario:', { jornadaId, cuestionarioId, nuevaCuestionarioIds });
                // Leer estado actual y modificar solo cuestionarioIds
                const jActual = (await apiManager.get(`/api/jornadas/${jornadaId}`))?.datos || {};
                await apiManager.put(`/api/jornadas/${jornadaId}`, {
                    nombre: jActual.nombre,
                    fechaJornada: jActual.fechaJornada || null,
                    lugar: jActual.lugar || '',
                    notas: jActual.notas || '',
                    cuestionarioIds: nuevaCuestionarioIds,
                    comboIds: jActual.comboIds || []
                });
            };
            const undoAction = async () => {
                console.log('[UNDO][undo][eliminar-cuestionario] Restaurando cuestionario:', { jornadaId, cuestionarioId, prevCuestionarioIds });
                // Si el PUT falla (concurrencia, permisos...), el error se propaga
                // para que el UndoManager no dé el undo por hecho
                const jActual = (await apiManager.get(`/api/jornadas/${jornadaId}`))?.datos || {};
                await apiManager.put(`/api/jornadas/${jornadaId}`, {
                    nombre: jActual.nombre,
                    fechaJornada: jActual.fechaJornada || null,
                    lugar: jActual.lugar || '',
                    notas: jActual.notas || '',
                    cuestionarioIds: prevCuestionarioIds,
                    comboIds: jActual.comboIds || []
                });
            };
            const doWrapped = async () => {
                await doAction();
                await this.cargarDatos(true);
                this.mostrarJornadas();
                this.restoreScrollOrFocus();
            };
            const undoWrapped = async () => {
                await undoAction();
                await this.cargarDatos(true);
                this.mostrarJornadas();
                this.restoreScrollOrFocus();
            };
            await doWrapped();
            if (window.UndoManager) window.UndoManager.record({ do: doWrapped, undo: undoWrapped, label: `Quitar cuestionario ${cuestionarioId} de jornada ${jornadaId}` });
            Utils.showAlert(`Cuestionario ${cuestionarioId} eliminado de la jornada`, 'success');
        } catch (e) {
            console.error('❌ [JORNADAS] Error eliminando cuestionario de jornada:', e);
            Utils.showAlert(Utils.mensajeErrorApi(e, 'eliminar cuestionarios de la jornada'), 'error');
        }
    },

    async eliminarComboDeJornada(jornadaId, comboId) {
        try {
            this.rememberScroll();
            const response = await apiManager.get(`/api/jornadas/${jornadaId}`);
            const jornada = response?.datos || response;
            if (!jornada) throw new Error('No se pudo cargar la jornada');
            
            // Capturar SOLO los campos editables (sin version, creacion_usuario_id, etc.)
            const prevComboIds = this.normalizarSlots(jornada.comboIds);
            const nuevaComboIds = this.quitarDeSlots(prevComboIds, comboId);
            
            const doAction = async () => {
                console.log('[UNDO][do][eliminar-combo] Eliminando combo:', { jornadaId, comboId, nuevaComboIds });
                // Leer estado actual y modificar solo comboIds
                const jActual = (await apiManager.get(`/api/jornadas/${jornadaId}`))?.datos || {};
                await apiManager.put(`/api/jornadas/${jornadaId}`, {
                    nombre: jActual.nombre,
                    fechaJornada: jActual.fechaJornada || null,
                    lugar: jActual.lugar || '',
                    notas: jActual.notas || '',
                    cuestionarioIds: jActual.cuestionarioIds || [],
                    comboIds: nuevaComboIds
                });
            };
            const undoAction = async () => {
                console.log('[UNDO][undo][eliminar-combo] Restaurando combo:', { jornadaId, comboId, prevComboIds });
                // Si el PUT falla (concurrencia, permisos...), el error se propaga
                // para que el UndoManager no dé el undo por hecho
                const jActual = (await apiManager.get(`/api/jornadas/${jornadaId}`))?.datos || {};
                await apiManager.put(`/api/jornadas/${jornadaId}`, {
                    nombre: jActual.nombre,
                    fechaJornada: jActual.fechaJornada || null,
                    lugar: jActual.lugar || '',
                    notas: jActual.notas || '',
                    cuestionarioIds: jActual.cuestionarioIds || [],
                    comboIds: prevComboIds
                });
            };
            const doWrapped = async () => {
                await doAction();
                await this.cargarDatos(true);
                this.mostrarJornadas();
                this.restoreScrollOrFocus();
            };
            const undoWrapped = async () => {
                await undoAction();
                await this.cargarDatos(true);
                this.mostrarJornadas();
                this.restoreScrollOrFocus();
            };
            await doWrapped();
            if (window.UndoManager) window.UndoManager.record({ do: doWrapped, undo: undoWrapped, label: `Quitar combo ${comboId} de jornada ${jornadaId}` });
            Utils.showAlert(`Combo ${comboId} eliminado de la jornada`, 'success');
        } catch (e) {
            console.error('❌ [JORNADAS] Error eliminando combo de jornada:', e);
            Utils.showAlert(Utils.mensajeErrorApi(e, 'eliminar combos de la jornada'), 'error');
        }
    },

    getEstadoBadge(estado) {
        const badges = {
            'preparacion':  'badge bg-secondary',
            'lista':        'badge bg-info text-dark',
            'en_grabacion': 'badge bg-warning text-dark',
            'completada':   'badge bg-success',
            'archivada':    'badge bg-dark'
        };
        const nombres = {
            'preparacion':  'Preparación',
            'lista':        'Lista',
            'en_grabacion': 'En Grabación',
            'completada':   'Completada',
            'archivada':    'Archivada'
        };
        return `<span class=\"${badges[estado] || 'badge bg-secondary'}\">${nombres[estado] || estado}</span>`;
    },

    puedeEditar(jornada) {
        // Solo ADMIN / DIRECCION; bloqueado en completada / archivada (alineado con PUT /api/jornadas)
        if (!this.esAdminODireccion()) {
            return false;
        }
        return jornada.estado !== 'completada' && jornada.estado !== 'archivada';
    },

    puedeEliminar(jornada) {
        // Solo ADMIN / DIRECCION; bloqueado en completada / archivada (alineado con DELETE)
        if (!this.esAdminODireccion()) {
            return false;
        }
        return jornada.estado !== 'completada' && jornada.estado !== 'archivada';
    },

    puedeGestionarEstado(jornada) {
        // Solo ADMIN / DIRECCION (alineado con PUT /api/jornadas/{id}/estado)
        return this.esAdminODireccion();
    },

    mostrarModalCrear() {
        this.jornadaEditando = null;
        this.cuestionariosSeleccionados = this.slotsVacios();
        this.combosSeleccionados = this.slotsVacios();
        
        document.getElementById('modalJornadaTitulo').textContent = 'Nueva Jornada';
        document.getElementById('formJornada').reset();
        
        // El ID se asigna automáticamente, mostramos el texto apropiado
        document.getElementById('jornadaId').value = 'Auto';
        
        this.actualizarSlotsVisual();
        
        const modal = new bootstrap.Modal(document.getElementById('modalJornada'));
        modal.show();
    },

    async editarJornada(id) {
        try {
            await EditLockManager.tryAcquire('JORNADA', id);
            const response = await apiManager.get(`/api/jornadas/${id}`);
            const jornada = response.datos;
            
            this.jornadaEditando = jornada;
            this.lastFocusJornadaId = id;
            this.cuestionariosSeleccionados = this.normalizarSlots(jornada.cuestionarioIds);
            this.combosSeleccionados = this.normalizarSlots(jornada.comboIds);
            
            document.getElementById('modalJornadaTitulo').textContent = 'Editar Jornada';
            document.getElementById('jornadaId').value = jornada.id;
            document.getElementById('jornadaNombre').value = jornada.nombre;
            document.getElementById('jornadaFecha').value = jornada.fechaJornada || '';
            document.getElementById('jornadaLugar').value = jornada.lugar || '';
            document.getElementById('jornadaNotas').value = jornada.notas || '';
            
            this.actualizarSlotsVisual();
            
            const modal = new bootstrap.Modal(document.getElementById('modalJornada'));
            modal.show();
            EditLockManager.startSession({
                entityType: 'JORNADA',
                entityId: id,
                modalSelector: '#modalJornada',
                onExpire: () => this.guardarJornada()
            });
            
        } catch (error) {
            console.error('❌ [JORNADAS] Error al cargar jornada:', error);
            Utils.showAlert('Error al cargar los datos de la jornada', 'error');
        }
    },

    async guardarJornada() {
        try {
            this.rememberScroll();
            const datos = {
                nombre: document.getElementById('jornadaNombre').value.trim(),
                fechaJornada: document.getElementById('jornadaFecha').value || null,
                lugar: document.getElementById('jornadaLugar').value.trim(),
                notas: document.getElementById('jornadaNotas').value.trim(),
                cuestionarioIds: this.cuestionariosSeleccionados,
                comboIds: this.combosSeleccionados
            };

            if (!datos.nombre) {
                Utils.showAlert('El nombre es obligatorio', 'error');
                return;
            }

            if (this.cuestionariosSeleccionados.length > 6) {
                Utils.showAlert('Máximo 6 cuestionarios por jornada', 'error');
                return;
            }

            if (this.combosSeleccionados.length > 6) {
                Utils.showAlert('Máximo 6 combos por jornada', 'error');
                return;
            }

            let response;
            if (this.jornadaEditando) {
                response = await apiManager.putUndoable(`/api/jornadas/${this.jornadaEditando.id}`, datos, { label: `Actualizar jornada ${this.jornadaEditando.id}` });
            } else {
                response = await apiManager.postUndoable('/api/jornadas', datos, { label: 'Crear jornada', idExtractor: (r) => r?.id || r?.datos?.id, deleteEndpointBuilder: (id) => `/api/jornadas/${id}` });
            }

            Utils.showAlert('Jornada guardada exitosamente', 'success');
            bootstrap.Modal.getInstance(document.getElementById('modalJornada')).hide();
            
            await this.cargarDatos(true);
            this.mostrarJornadas();
            this.restoreScrollOrFocus();
            
        } catch (error) {
            console.error('❌ [JORNADAS] Error al guardar:', error);
            
            // Manejar específicamente errores de autenticación
            if (error.message && error.message.includes('UNAUTHORIZED')) {
                Utils.showAlert('Sesión expirada. Por favor, inicia sesión nuevamente.', 'error');
                // Redirigir a login después de un breve delay
                setTimeout(() => {
                    window.location.href = 'login.html';
                }, 2000);
                return;
            }
            
            // Extraer mensaje de error de la respuesta JSON
            const mensajeError = this.extraerMensajeError(error.message);
            Utils.showAlert(mensajeError, 'error');
        }
    },

    async eliminarJornada(id) {
        if (!confirm('¿Estás seguro de que quieres eliminar esta jornada?\n\nPodrás deshacerlo con Ctrl+Z durante la próxima hora.')) {
            return;
        }

        try {
            await apiManager.deleteUndoable(`/api/jornadas/${id}`, { label: `Eliminar jornada ${id}` });
            Utils.showAlert('Jornada eliminada exitosamente', 'success');
            
            await this.cargarDatos(true);
            this.mostrarJornadas();
            
        } catch (error) {
            console.error('❌ [JORNADAS] Error al eliminar:', error);
            
            // Manejar específicamente errores de autenticación
            if (error.message && error.message.includes('UNAUTHORIZED')) {
                Utils.showAlert('Sesión expirada. Por favor, inicia sesión nuevamente.', 'error');
                setTimeout(() => {
                    window.location.href = 'login.html';
                }, 2000);
                return;
            }
            
            // Extraer mensaje de error de la respuesta JSON
            const mensajeError = this.extraerMensajeError(error.message);
            Utils.showAlert(mensajeError, 'error');
        }
    },

    async cambiarEstado(id, nuevoEstado) {
        if (!nuevoEstado) return;

        try {
            const respPrev = await apiManager.get(`/api/jornadas/${id}`);
            const jornadaPrev = respPrev?.datos || respPrev;
            const estadoAnterior = jornadaPrev?.estado || null;

            await apiManager.put(`/api/jornadas/${id}/estado`, { estado: nuevoEstado });

            // Undo respaldado por backend: revierte también la cascada de estados
            // de cuestionarios/combos. Si no está disponible, revertir solo el
            // estado de la jornada por el mismo endpoint.
            const label = `Estado jornada ${id}: ${estadoAnterior || '?'} → ${nuevoEstado}`;
            const registrado = apiManager.registrarUndoBackend({
                label,
                redo: async () => { await apiManager.put(`/api/jornadas/${id}/estado`, { estado: nuevoEstado }); }
            });
            if (!registrado && window.UndoManager && estadoAnterior && estadoAnterior !== nuevoEstado) {
                window.UndoManager.record({
                    do: async () => { await apiManager.put(`/api/jornadas/${id}/estado`, { estado: nuevoEstado }); },
                    undo: async () => { await apiManager.put(`/api/jornadas/${id}/estado`, { estado: estadoAnterior }); },
                    label
                });
            }

            Utils.showAlert('Estado actualizado exitosamente', 'success');
            
            await this.cargarDatos(true);
            this.mostrarJornadas();
            
        } catch (error) {
            console.error('❌ [JORNADAS] Error al cambiar estado:', error);
            
            // Manejar específicamente errores de autenticación
            if (error.message && error.message.includes('UNAUTHORIZED')) {
                Utils.showAlert('Sesión expirada. Por favor, inicia sesión nuevamente.', 'error');
                setTimeout(() => {
                    window.location.href = 'login.html';
                }, 2000);
                return;
            }

            const msg = (error.message || '').toLowerCase();
            if (msg.includes('403') || msg.includes('permiso') || msg.includes('forbidden')) {
                Utils.showAlert('No tienes permiso para cambiar el estado', 'error');
                await this.cargarDatos(true);
                this.mostrarJornadas();
                return;
            }
            
            // Extraer mensaje de error de la respuesta JSON
            const mensajeError = this.extraerMensajeError(error.message, 'cambiar el estado');
            Utils.showAlert(mensajeError, 'error');
            await this.cargarDatos(true);
            this.mostrarJornadas();
        }
    },

    async exportarExcel(id) {
        try {
            console.log('📊 [JORNADAS] Exportando Excel para jornada:', id);
            
            const response = await fetch(`/api/jornadas/${id}/exportar-excel`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('token')}`,
                    'X-Excel-Cambiar-Columna-ID-PREGUNTA': 'MULT',     // Cambiar el encabezado ID PREGUNTA a MULT solo en Excel
                    'X-Excel-Mostrar-Factor-Multiplicacion': 'true',    // Mostrar factor de multiplicación en columna MULT
                    'X-Excel-Ordenar-Cuestionarios-Por-Nivel': 'true',  // Ordenar preguntas de cuestionarios por nivel (1,2,3,4)
                    'X-Excel-Ordenar-Combos-Por-Factor': 'true'         // Ordenar preguntas de combos por factor de multiplicación
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.style.display = 'none';
            a.href = url;
            
            // Obtener nombre del archivo desde la respuesta
            const contentDisposition = response.headers.get('content-disposition');
            let filename = `jornada_${id}.xlsx`;
            if (contentDisposition) {
                const filenameMatch = contentDisposition.match(/filename="(.+)"/);
                if (filenameMatch) {
                    filename = filenameMatch[1];
                }
            }
            
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
            
            Utils.showAlert('Excel exportado exitosamente', 'success');
            
        } catch (error) {
            console.error('❌ [JORNADAS] Error al exportar Excel:', error);
            const mensajeError = this.extraerMensajeError(error.message);
            Utils.showAlert(mensajeError, 'error');
        }
    },

    abrirSelectorCuestionarioEnSlot(slotIndex) {
        this.slotDestinoCuestionario = Number.isInteger(slotIndex) ? slotIndex : 0;
        return this.seleccionarCuestionarios();
    },

    abrirSelectorComboEnSlot(slotIndex) {
        this.slotDestinoCombo = Number.isInteger(slotIndex) ? slotIndex : 0;
        return this.seleccionarCombos();
    },

    async seleccionarCuestionarios() {
        // Si el modal de edición está abierto, lo ocultamos y marcamos que reabriremos luego
        const modalEditEl = document.getElementById('modalJornada');
        const modalEditInst = modalEditEl ? bootstrap.Modal.getInstance(modalEditEl) : null;
        if (modalEditEl && modalEditEl.classList.contains('show')) {
            this.reabrirEditarTrasSeleccion = true;
            modalEditInst?.hide();
        }
        // Cargar temáticas al abrir
        try {
            const selTem = document.getElementById('filtroCuestTematica');
            if (selTem) {
                const resp = await fetch('/api/cuestionarios/tematicas', { headers: authManager.getAuthHeaders() });
                if (resp.ok) {
                    const tems = await resp.json();
                    selTem.innerHTML = '<option value="">Temática (todas)</option>' + tems.map(t => `<option value="${t}">${t}</option>`).join('');
                }
            }
        } catch (e) { console.warn('[JORNADAS] No se pudieron cargar temáticas de cuestionarios', e); }

        this.selectorCuestPagina = 0;
        this.mostrarCuestionariosDisponibles();
        const modalEl = document.getElementById('modalSelectorCuestionarios');
        const modal = new bootstrap.Modal(modalEl);
        modal.show();
        setTimeout(() => document.getElementById('filtroCuestId')?.focus(), 200);
    },

    async seleccionarCombos() {
        // Si el modal de edición está abierto, lo ocultamos y marcamos que reabriremos luego
        const modalEditEl = document.getElementById('modalJornada');
        const modalEditInst = modalEditEl ? bootstrap.Modal.getInstance(modalEditEl) : null;
        if (modalEditEl && modalEditEl.classList.contains('show')) {
            this.reabrirEditarTrasSeleccion = true;
            modalEditInst?.hide();
        }
        // Cargar temáticas para el select (de combos disponibles)
        try {
            const selTem = document.getElementById('filtroComboTematica');
            if (selTem) {
                const resp = await fetch('/api/tematicas', { headers: authManager.getAuthHeaders() });
                if (resp.ok) {
                    const tematicasData = await resp.json();
                    const nombres = tematicasData.map(t => t.nombre);
                    selTem.innerHTML = '<option value="">Temática (todas)</option>' + nombres.map(t => `<option value="${t}">${t}</option>`).join('');
                }
            }
        } catch (e) { console.warn('[JORNADAS] No se pudieron cargar temáticas de combos', e); }

        this.selectorComboPagina = 0;
        this.mostrarCombosDisponibles();
        const modalEl = document.getElementById('modalSelectorCombos');
        const modal = new bootstrap.Modal(modalEl);
        modal.show();
        setTimeout(() => document.getElementById('filtroComboId')?.focus(), 200);
    },

    listaCuestionariosFiltrada() {
        const idFiltro = (document.getElementById('filtroCuestId')?.value || '').trim();
        const tematicaFiltro = document.getElementById('filtroCuestTematica')?.value || '';
        return (this.cuestionariosDisponibles || []).filter(cuestionario => {
            if (idFiltro && String(cuestionario.id) !== idFiltro) return false;
            if (tematicaFiltro && cuestionario.tematica !== tematicaFiltro) return false;
            return true;
        });
    },

    listaCombosFiltrada() {
        const idFiltro = (document.getElementById('filtroComboId')?.value || '').trim();
        const tipoFiltro = document.getElementById('filtroComboTipo')?.value || '';
        const tematicaFiltro = document.getElementById('filtroComboTematica')?.value || '';
        return (this.combosDisponibles || []).filter(combo => {
            if (idFiltro && String(combo.id) !== idFiltro) return false;
            if (tipoFiltro && combo.tipo !== tipoFiltro) return false;
            if (tematicaFiltro && combo.tematica !== tematicaFiltro) return false;
            return true;
        });
    },

    paginarListaSelector(lista, pagina) {
        const total = lista.length;
        const totalPag = Math.max(1, Math.ceil(total / this.selectorPorPagina) || 1);
        const p = Math.min(Math.max(0, pagina), totalPag - 1);
        const start = p * this.selectorPorPagina;
        return {
            slice: lista.slice(start, start + this.selectorPorPagina),
            pagina: p,
            total,
            totalPag,
            inicio: total === 0 ? 0 : start + 1,
            fin: Math.min(start + this.selectorPorPagina, total)
        };
    },

    renderPaginacionSelector(ulId, pagina, totalPag, irAFnName) {
        const ul = document.getElementById(ulId);
        if (!ul) return;
        ul.innerHTML = '';
        if (totalPag <= 1) return;

        const addItem = (label, target, disabled, active) => {
            const li = document.createElement('li');
            li.className = `page-item${disabled ? ' disabled' : ''}${active ? ' active' : ''}`;
            li.innerHTML = `<a class="page-link" href="#">${label}</a>`;
            if (!disabled && !active) {
                li.querySelector('a').addEventListener('click', (e) => {
                    e.preventDefault();
                    this[irAFnName](target);
                });
            } else {
                li.querySelector('a').addEventListener('click', (e) => e.preventDefault());
            }
            ul.appendChild(li);
        };

        addItem('Anterior', pagina - 1, pagina === 0, false);
        const inicio = Math.max(0, pagina - 2);
        const fin = Math.min(totalPag - 1, pagina + 2);
        for (let i = inicio; i <= fin; i++) {
            addItem(String(i + 1), i, false, i === pagina);
        }
        addItem('Siguiente', pagina + 1, pagina >= totalPag - 1, false);
    },

    buscarCuestionariosSelector() {
        this.selectorCuestPagina = 0;
        this.mostrarCuestionariosDisponibles();
        document.getElementById('filtroCuestId')?.focus();
    },

    buscarCombosSelector() {
        this.selectorComboPagina = 0;
        this.mostrarCombosDisponibles();
        document.getElementById('filtroComboId')?.focus();
    },

    irAPaginaSelectorCuestionarios(pagina) {
        this.selectorCuestPagina = pagina;
        this.mostrarCuestionariosDisponibles();
    },

    irAPaginaSelectorCombos(pagina) {
        this.selectorComboPagina = pagina;
        this.mostrarCombosDisponibles();
    },

    confirmarSelectorCuestionariosConTeclado() {
        const idFiltro = (document.getElementById('filtroCuestId')?.value || '').trim();
        if (idFiltro) {
            const encontrado = (this.cuestionariosDisponibles || []).find(c => String(c.id) === idFiltro);
            if (encontrado) {
                this.seleccionarCuestionarioDirecto(encontrado.id);
                return;
            }
            Utils.showAlert(`No hay un cuestionario disponible con ID ${idFiltro}`, 'error');
            return;
        }
        this.confirmarSeleccionCuestionarios();
    },

    confirmarSelectorCombosConTeclado() {
        const idFiltro = (document.getElementById('filtroComboId')?.value || '').trim();
        if (idFiltro) {
            const encontrado = (this.combosDisponibles || []).find(c => String(c.id) === idFiltro);
            if (encontrado) {
                this.seleccionarComboDirecto(encontrado.id);
                return;
            }
            Utils.showAlert(`No hay un combo disponible con ID ${idFiltro}`, 'error');
            return;
        }
        this.confirmarSeleccionCombos();
    },

    mostrarCuestionariosDisponibles() {
        const container = document.getElementById('listaCuestionarios');
        if (!container) return;
        const lista = this.listaCuestionariosFiltrada();
        const pagina = this.paginarListaSelector(lista, this.selectorCuestPagina);
        this.selectorCuestPagina = pagina.pagina;

        if (!pagina.slice.length) {
            container.innerHTML = '<div class="list-group-item text-muted">No hay cuestionarios para mostrar</div>';
        } else {
            let html = '';
            pagina.slice.forEach(cuestionario => {
                const isSelected = this.incluyeId(this.cuestionariosSeleccionados, cuestionario.id);
                html += `
                <div class="list-group-item ${isSelected ? 'active' : ''}" 
                     onclick="JornadasManager.toggleCuestionario(${cuestionario.id})"
                     data-id="${cuestionario.id}">
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <h6 class="mb-1">Cuestionario #${cuestionario.id}</h6>
                            <p class="mb-1">Nivel: ${cuestionario.nivel} | Estado: <span class="badge ${Utils.getEstadoBadgeClass(cuestionario.estado, 'cuestionario')}">${Utils.formatearEstadoCuestionario(cuestionario.estado)}</span></p>
                        <small>${cuestionario.tematica || 'Sin temática'}</small>
                        </div>
                        <div class="d-flex align-items-center gap-2">
                            <button class="btn btn-sm btn-success" 
                                    onclick="event.stopPropagation(); JornadasManager.seleccionarCuestionarioDirecto(${cuestionario.id})"
                                    title="Seleccionar">
                                <i class="fas fa-plus"></i>
                            </button>
                            <button class="btn btn-sm btn-outline-secondary" 
                                    onclick="event.stopPropagation(); JornadasManager.verPreguntasCuestionario(${cuestionario.id})"
                                    title="Ver preguntas">
                                <i class="fas fa-eye"></i>
                            </button>
                            <span class="badge bg-info">${cuestionario.totalPreguntas} preguntas</span>
                            ${isSelected ? '<i class="fas fa-check text-white"></i>' : ''}
                        </div>
                    </div>
                </div>
            `;
            });
            container.innerHTML = html;
        }

        const info = document.getElementById('info-paginacion-selector-cuestionarios');
        if (info) {
            info.textContent = pagina.total === 0
                ? 'Mostrando 0 de 0'
                : `Mostrando ${pagina.inicio}-${pagina.fin} de ${pagina.total}`;
        }
        this.renderPaginacionSelector('paginacion-selector-cuestionarios', pagina.pagina, pagina.totalPag, 'irAPaginaSelectorCuestionarios');
    },

    mostrarCombosDisponibles() {
        const container = document.getElementById('listaCombos');
        if (!container) return;
        const lista = this.listaCombosFiltrada();
        const pagina = this.paginarListaSelector(lista, this.selectorComboPagina);
        this.selectorComboPagina = pagina.pagina;

        if (!pagina.slice.length) {
            container.innerHTML = '<div class="list-group-item text-muted">No hay combos para mostrar</div>';
        } else {
            let html = '';
            pagina.slice.forEach(combo => {
                const isSelected = this.incluyeId(this.combosSeleccionados, combo.id);
                html += `
                <div class="list-group-item ${isSelected ? 'active' : ''}" 
                     onclick="JornadasManager.toggleCombo(${combo.id})"
                     data-id="${combo.id}">
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <h6 class="mb-1">Combo #${combo.id}</h6>
                            <p class="mb-1">Nivel: ${combo.nivel} | Estado: <span class="badge ${Utils.getEstadoBadgeClass(combo.estado, 'combo')}">${Utils.formatearEstadoCombo(combo.estado)}</span></p>
                            <small>Tipo: ${combo.tipo || 'No especificado'} ${combo.tematica ? `| Temática: ${combo.tematica}` : ''}</small>
                        </div>
                        <div class="d-flex align-items-center gap-2">
                            <button class="btn btn-sm btn-success" 
                                    onclick="event.stopPropagation(); JornadasManager.seleccionarComboDirecto(${combo.id})"
                                    title="Seleccionar">
                                <i class="fas fa-plus"></i>
                            </button>
                            <button class="btn btn-sm btn-outline-secondary" 
                                    onclick="event.stopPropagation(); JornadasManager.verPreguntasCombo(${combo.id})"
                                    title="Ver preguntas">
                                <i class="fas fa-eye"></i>
                            </button>
                            <span class="badge bg-info">${combo.totalPreguntas} preguntas</span>
                            ${isSelected ? '<i class="fas fa-check text-white"></i>' : ''}
                        </div>
                    </div>
                </div>
            `;
            });
            container.innerHTML = html;
        }

        const info = document.getElementById('info-paginacion-selector-combos');
        if (info) {
            info.textContent = pagina.total === 0
                ? 'Mostrando 0 de 0'
                : `Mostrando ${pagina.inicio}-${pagina.fin} de ${pagina.total}`;
        }
        this.renderPaginacionSelector('paginacion-selector-combos', pagina.pagina, pagina.totalPag, 'irAPaginaSelectorCombos');
    },

    seleccionarCuestionarioDirecto(id) {
        console.log('🎯 [AÑADIR] seleccionarCuestionarioDirecto llamado con id:', id);
        console.log('🎯 [AÑADIR] jornadaEditando:', this.jornadaEditando);
        console.log('🎯 [AÑADIR] jornadaEditando.id:', this.jornadaEditando?.id);
        
        // Límite visual
        if (this.contarOcupados(this.cuestionariosSeleccionados) >= 6 && !this.incluyeId(this.cuestionariosSeleccionados, id)) {
            Utils.showAlert('Máximo 6 cuestionarios por jornada', 'error');
            return;
        }

        // Si estamos en edición de jornada, hacemos PUT con snapshot y registramos undo/redo
        if (this.jornadaEditando && this.jornadaEditando.id) {
            console.log('✅ [AÑADIR] Entrando en bloque de edición de jornada existente');
            const modalInst = bootstrap.Modal.getInstance(document.getElementById('modalSelectorCuestionarios'));
            modalInst && modalInst.hide();

            const jid = this.jornadaEditando.id;
            const prevLista = this.normalizarSlots(this.jornadaEditando.cuestionarioIds);
            const nuevaLista = this.colocarEnHueco(prevLista, id, this.slotDestinoCuestionario);
            if (!nuevaLista) {
                Utils.showAlert('Máximo 6 cuestionarios por jornada', 'error');
                return;
            }

            // Los errores del PUT se propagan: el UndoManager necesita saberlos
            // para no dar por hechas acciones que fallaron
            const doAction = async () => {
                console.log('[UNDO][do][add-cuestionario] DO Añadir:', { jid, id, nuevaLista });
                this.rememberScroll();
                const jActual = (await apiManager.get(`/api/jornadas/${jid}`))?.datos || {};
                await apiManager.put(`/api/jornadas/${jid}`, {
                    nombre: jActual.nombre,
                    fechaJornada: jActual.fechaJornada || null,
                    lugar: jActual.lugar || '',
                    notas: jActual.notas || '',
                    cuestionarioIds: nuevaLista,
                    comboIds: jActual.comboIds || []
                });
                await this.cargarDatos(true);
                this.mostrarJornadas();
                this.restoreScrollOrFocus();
            };
            const undoAction = async () => {
                console.log('[UNDO][undo][add-cuestionario] UNDO Quitar recién añadido:', { jid, id, prevLista });
                this.rememberScroll();
                const jActual = (await apiManager.get(`/api/jornadas/${jid}`))?.datos || {};
                await apiManager.put(`/api/jornadas/${jid}`, {
                    nombre: jActual.nombre,
                    fechaJornada: jActual.fechaJornada || null,
                    lugar: jActual.lugar || '',
                    notas: jActual.notas || '',
                    cuestionarioIds: prevLista,
                    comboIds: jActual.comboIds || []
                });
                await this.cargarDatos(true);
                this.mostrarJornadas();
                this.restoreScrollOrFocus();
            };

            (async () => {
                try {
                    await doAction();
                } catch (e) {
                    console.error('[AÑADIR] Error al añadir cuestionario:', e);
                    Utils.showAlert(Utils.mensajeErrorApi(e, 'añadir el cuestionario a la jornada'), 'error');
                    await this.cargarDatos(true);
                    this.mostrarJornadas();
                    return;
                }
                if (window.UndoManager) {
                    window.UndoManager.record({ 
                        do: doAction, 
                        undo: undoAction, 
                        label: `Añadir cuestionario ${id} a jornada ${jid}` 
                    });
                }
                Utils.showAlert(`Cuestionario ${id} añadido`, 'success');
            })();
            return;
        }

        // Flujo en creación de jornada (modal de crear/editar abierto pero sin id persistido)
        id = this.normalizeId(id);
        if (!this.incluyeId(this.cuestionariosSeleccionados, id)) {
            const colocados = this.colocarEnHueco(this.cuestionariosSeleccionados, id, this.slotDestinoCuestionario);
            if (!colocados) {
                Utils.showAlert('Máximo 6 cuestionarios por jornada', 'error');
                return;
            }
            this.cuestionariosSeleccionados = colocados;
        }
        this.actualizarSlotsVisual();
        const modalInst = bootstrap.Modal.getInstance(document.getElementById('modalSelectorCuestionarios'));
        modalInst && modalInst.hide();
        this.reabrirModalJornadaTrasSeleccion();
        Utils.showAlert(`Cuestionario ${id} añadido`, 'success');
    },

    seleccionarComboDirecto(id) {
        console.log('🎯 [AÑADIR] seleccionarComboDirecto llamado con id:', id);
        console.log('🎯 [AÑADIR] jornadaEditando:', this.jornadaEditando);
        console.log('🎯 [AÑADIR] jornadaEditando.id:', this.jornadaEditando?.id);
        
        // Límite visual
        if (this.contarOcupados(this.combosSeleccionados) >= 6 && !this.incluyeId(this.combosSeleccionados, id)) {
            Utils.showAlert('Máximo 6 combos por jornada', 'error');
            return;
        }

        // Si estamos en edición de jornada, hacemos PUT con snapshot y registramos undo/redo
        if (this.jornadaEditando && this.jornadaEditando.id) {
            console.log('✅ [AÑADIR] Entrando en bloque de edición de jornada existente');
            const modalInst = bootstrap.Modal.getInstance(document.getElementById('modalSelectorCombos'));
            modalInst && modalInst.hide();

            const jid = this.jornadaEditando.id;
            const prevLista = this.normalizarSlots(this.jornadaEditando.comboIds);
            const nuevaLista = this.colocarEnHueco(prevLista, id, this.slotDestinoCombo);
            if (!nuevaLista) {
                Utils.showAlert('Máximo 6 combos por jornada', 'error');
                return;
            }

            // Los errores del PUT se propagan: el UndoManager necesita saberlos
            // para no dar por hechas acciones que fallaron
            const doAction = async () => {
                console.log('[UNDO][do][add-combo] DO Añadir:', { jid, id, nuevaLista });
                this.rememberScroll();
                const jActual = (await apiManager.get(`/api/jornadas/${jid}`))?.datos || {};
                await apiManager.put(`/api/jornadas/${jid}`, {
                    nombre: jActual.nombre,
                    fechaJornada: jActual.fechaJornada || null,
                    lugar: jActual.lugar || '',
                    notas: jActual.notas || '',
                    cuestionarioIds: jActual.cuestionarioIds || [],
                    comboIds: nuevaLista
                });
                await this.cargarDatos(true);
                this.mostrarJornadas();
                this.restoreScrollOrFocus();
            };
            const undoAction = async () => {
                console.log('[UNDO][undo][add-combo] UNDO Quitar recién añadido:', { jid, id, prevLista });
                this.rememberScroll();
                const jActual = (await apiManager.get(`/api/jornadas/${jid}`))?.datos || {};
                await apiManager.put(`/api/jornadas/${jid}`, {
                    nombre: jActual.nombre,
                    fechaJornada: jActual.fechaJornada || null,
                    lugar: jActual.lugar || '',
                    notas: jActual.notas || '',
                    cuestionarioIds: jActual.cuestionarioIds || [],
                    comboIds: prevLista
                });
                await this.cargarDatos(true);
                this.mostrarJornadas();
                this.restoreScrollOrFocus();
            };

            (async () => {
                try {
                    await doAction();
                } catch (e) {
                    console.error('[AÑADIR] Error al añadir combo:', e);
                    Utils.showAlert(Utils.mensajeErrorApi(e, 'añadir el combo a la jornada'), 'error');
                    await this.cargarDatos(true);
                    this.mostrarJornadas();
                    return;
                }
                if (window.UndoManager) {
                    window.UndoManager.record({ 
                        do: doAction, 
                        undo: undoAction, 
                        label: `Añadir combo ${id} a jornada ${jid}` 
                    });
                }
                Utils.showAlert(`Combo ${id} añadido`, 'success');
            })();
            return;
        }

        // Flujo en creación de jornada (modal de crear/editar abierto pero sin id persistido)
        id = this.normalizeId(id);
        if (!this.incluyeId(this.combosSeleccionados, id)) {
            const colocados = this.colocarEnHueco(this.combosSeleccionados, id, this.slotDestinoCombo);
            if (!colocados) {
                Utils.showAlert('Máximo 6 combos por jornada', 'error');
                return;
            }
            this.combosSeleccionados = colocados;
        }
        this.actualizarSlotsVisual();
        const modalInst2 = bootstrap.Modal.getInstance(document.getElementById('modalSelectorCombos'));
        modalInst2 && modalInst2.hide();
        this.reabrirModalJornadaTrasSeleccion();
        Utils.showAlert(`Combo ${id} añadido`, 'success');
    },

    toggleCuestionario(id) {
        id = this.normalizeId(id);
        if (this.incluyeId(this.cuestionariosSeleccionados, id)) {
            this.cuestionariosSeleccionados = this.quitarDeSlots(this.cuestionariosSeleccionados, id);
        } else {
            const colocados = this.colocarEnHueco(this.cuestionariosSeleccionados, id, this.slotDestinoCuestionario);
            if (!colocados) {
                Utils.showAlert('Máximo 6 cuestionarios por jornada', 'error');
                return;
            }
            this.cuestionariosSeleccionados = colocados;
        }
        this.mostrarCuestionariosDisponibles();
    },

    toggleCombo(id) {
        id = this.normalizeId(id);
        if (this.incluyeId(this.combosSeleccionados, id)) {
            this.combosSeleccionados = this.quitarDeSlots(this.combosSeleccionados, id);
        } else {
            const colocados = this.colocarEnHueco(this.combosSeleccionados, id, this.slotDestinoCombo);
            if (!colocados) {
                Utils.showAlert('Máximo 6 combos por jornada', 'error');
                return;
            }
            this.combosSeleccionados = colocados;
        }
        this.mostrarCombosDisponibles();
    },

    confirmarSeleccionCuestionarios() {
        const count = this.contarOcupados(this.cuestionariosSeleccionados);
        this.actualizarSlotsVisual();
        bootstrap.Modal.getInstance(document.getElementById('modalSelectorCuestionarios'))?.hide();

        if (this.jornadaEditando?.id) {
            this.guardarCambiosCuestionarios();
        } else {
            this.reabrirModalJornadaTrasSeleccion();
            if (count > 0) {
                Utils.showAlert(`${count} cuestionario(s) seleccionado(s)`, 'success');
            }
        }
    },

    confirmarSeleccionCombos() {
        const count = this.contarOcupados(this.combosSeleccionados);
        this.actualizarSlotsVisual();
        bootstrap.Modal.getInstance(document.getElementById('modalSelectorCombos'))?.hide();

        if (this.jornadaEditando?.id) {
            this.guardarCambiosCombos();
        } else {
            this.reabrirModalJornadaTrasSeleccion();
            if (count > 0) {
                Utils.showAlert(`${count} combo(s) seleccionado(s)`, 'success');
            }
        }
    },

    cerrarSelectorCuestionarios() {
        bootstrap.Modal.getInstance(document.getElementById('modalSelectorCuestionarios'))?.hide();
        this.reabrirModalJornadaTrasSeleccion();
    },

    cerrarSelectorCombos() {
        bootstrap.Modal.getInstance(document.getElementById('modalSelectorCombos'))?.hide();
        this.reabrirModalJornadaTrasSeleccion();
    },

    actualizarSlotsVisual() {
        // Render avanzado con preguntas y respuestas
        this.actualizarSlotsCuestionarios();
        this.actualizarSlotsCombos();
    },

    async actualizarSlotsCuestionarios() {
        const container = document.getElementById('cuestionariosSeleccionados');
        let html = '';

        for (let i = 0; i < 6; i++) {
            const cuestionarioId = this.cuestionariosSeleccionados[i];
            if (cuestionarioId) {
                // Cargar detalle para vista avanzada (niveles/pregunta/respuesta)
                try {
                    const detalle = await apiManager.get(`/api/cuestionarios/${cuestionarioId}`);
                    if (detalle) {
                        const preguntas = Array.isArray(detalle.preguntas) ? detalle.preguntas : [];
                        // Ordenar por nivel numérico 1..4 si está disponible
                        const ordenadas = [...preguntas].sort((a,b) => {
                            const na = parseInt(String(a.pregunta?.nivel || '').replace(/\D/g,'')) || 0;
                            const nb = parseInt(String(b.pregunta?.nivel || '').replace(/\D/g,'')) || 0;
                            return na - nb;
                        });
                        let tabla = '';
                        ordenadas.forEach(pq => {
                            const p = pq.pregunta || {};
                            const nivel = (p.nivel ? String(p.nivel).replace(/^_/, '') : '') || '';
                            tabla += `
                                <tr>
                                    <td style="width:60px"><span class="badge bg-light text-secondary fw-bold">${nivel}</span></td>
                                    <td>${p.pregunta || ''}</td>
                                    <td><strong>${p.respuesta || ''}</strong></td>
                                </tr>
                            `;
                        });
                        html += `
                            <div class="item-slot item-filled">
                                <div class="d-flex justify-content-between align-items-start">
                                    <div>
                                        <strong>Cuestionario #${detalle.id}</strong><br>
                                        <small>${detalle.nivel || ''}</small><br>
                                        <small>${detalle.tematica || 'Sin temática'}</small>
                                    </div>
                                    <div>
                                        <button class="btn btn-sm btn-outline-danger" onclick="JornadasManager.quitarCuestionario(${cuestionarioId})" title="Quitar">
                                            <i class="fas fa-times"></i>
                                        </button>
                                    </div>
                                </div>
                                <div class="table-responsive mt-2">
                                    <table class="table table-sm table-bordered mb-0">
                                        <thead>
                                            <tr><th>Nivel</th><th>Pregunta</th><th>Respuesta</th></tr>
                                        </thead>
                                        <tbody>
                                            ${tabla || `<tr><td colspan="3" class="text-muted text-center">Sin preguntas</td></tr>`}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        `;
                    }
                } catch (e) {
                    html += `
                        <div class="item-slot item-filled">
                            <div>
                                <strong>Cuestionario #${cuestionarioId}</strong>
                                <div class="text-danger small">Error al cargar detalle</div>
                                <button type="button" class="btn btn-sm btn-outline-danger mt-1" onclick="JornadasManager.quitarCuestionario(${cuestionarioId})"><i class="fas fa-times"></i></button>
                            </div>
                        </div>
                    `;
                }
            } else {
                html += `
                    <div class="item-slot text-center">
                        <div class="text-muted mb-2">Slot ${i + 1} vacío</div>
                        <button type="button" class="btn btn-sm btn-outline-primary" onclick="JornadasManager.abrirSelectorCuestionarioEnSlot(${i})">
                            <i class="fas fa-plus"></i> Añadir cuestionario
                        </button>
                    </div>
                `;
            }
        }

        container.innerHTML = html;
    },

    async actualizarSlotsCombos() {
        const container = document.getElementById('combosSeleccionados');
        let html = '';

        for (let i = 0; i < 6; i++) {
            const comboId = this.combosSeleccionados[i];
            if (comboId) {
                // Cargar detalle para vista avanzada (factor/pregunta/respuesta)
                try {
                    const detalle = await apiManager.get(`/api/combos/${comboId}`);
                    if (detalle) {
                        const preguntas = Array.isArray(detalle.preguntas) ? detalle.preguntas : [];
                        // Ordenar por multiplicador 1..3 según valor numérico extraído
                        const ordenadas = [...preguntas].sort((a,b) => {
                            const na = parseInt(String(a.factor || a.factorMultiplicacion || '').replace(/\D/g,'')) || 0;
                            const nb = parseInt(String(b.factor || b.factorMultiplicacion || '').replace(/\D/g,'')) || 0;
                            return na - nb;
                        });
                        let tabla = '';
                        ordenadas.forEach(pq => {
                            const p = pq.pregunta || {};
                            let factorStr = pq.factorMultiplicacion || pq.factor || '';
                            const num = parseInt(factorStr);
                            if (!isNaN(num)) factorStr = num === 0 ? 'x' : `x${num}`;
                            tabla += `
                                <tr>
                                    <td style=\"width:80px\">
                                        <input class=\"form-control form-control-sm\"
                                               value=\"${factorStr || ''}\"
                                               onblur=\"JornadasManager.actualizarFactorDesdeModal(${detalle.id}, ${p.id}, this.value)\"
                                               title=\"Editar multiplicador (p.ej. X, X2, X3)\">
                                    </td>
                                    <td>${p.pregunta || ''}</td>
                                    <td><strong>${p.respuesta || ''}</strong></td>
                                </tr>
                            `;
                        });
                        html += `
                            <div class="item-slot item-filled">
                                <div class="d-flex justify-content-between align-items-start">
                                    <div>
                                        <strong>Combo #${detalle.id}</strong><br>
                                        <small>${detalle.nivel || ''}</small><br>
                                        <small>Tipo: ${detalle.tipo || 'N/A'}</small>
                                    </div>
                                    <div>
                                        <button type="button" class="btn btn-sm btn-outline-danger" onclick="JornadasManager.quitarCombo(${comboId})" title="Quitar">
                                            <i class="fas fa-times"></i>
                                        </button>
                                    </div>
                                </div>
                                <div class="table-responsive mt-2">
                                    <table class="table table-sm table-bordered mb-0">
                                        <thead>
                                            <tr><th>MULT</th><th>Pregunta</th><th>Respuesta</th></tr>
                                        </thead>
                                        <tbody>
                                            ${tabla || `<tr><td colspan=\"3\" class=\"text-muted text-center\">Sin preguntas</td></tr>`}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        `;
                    }
                } catch (e) {
                    html += `
                        <div class=\"item-slot item-filled\">\n\
                            <div>\n\
                                <strong>Combo #${comboId}</strong>\n\
                                <div class=\"text-danger small\">Error al cargar detalle</div>\n\
                                <button class=\"btn btn-sm btn-outline-danger mt-1\" onclick=\"JornadasManager.quitarCombo(${comboId})\"><i class=\"fas fa-times\"></i></button>\n\
                            </div>\n\
                        </div>
                    `;
                }
            } else {
                html += `
                    <div class="item-slot text-center">
                        <div class="text-muted mb-2">Slot ${i + 1} vacío</div>
                        <button type="button" class="btn btn-sm btn-outline-primary" onclick="JornadasManager.abrirSelectorComboEnSlot(${i})">
                            <i class="fas fa-plus"></i> Añadir combo
                        </button>
                    </div>
                `;
            }
        }

        container.innerHTML = html;
    },

    quitarCuestionario(id) {
        const index = this.idEnLista(this.cuestionariosSeleccionados, id);
        if (index > -1) {
            this.cuestionariosSeleccionados = this.quitarDeSlots(this.cuestionariosSeleccionados, id);
            this.actualizarSlotsVisual();
            
            // Si estamos editando una jornada existente, guardar los cambios
            if (this.jornadaEditando) {
                this.guardarCambiosCuestionarios();
            }
        }
    },

    async guardarCambiosCuestionarios() {
        try {
            this.rememberScroll();
            console.log('🔍 [JORNADAS] Guardando cambios de cuestionarios para jornada:', this.jornadaEditando.id);
            console.log('🔍 [JORNADAS] Cuestionarios seleccionados:', this.cuestionariosSeleccionados);
            console.log('🔍 [JORNADAS] Jornada editando:', this.jornadaEditando);
            
            const jid = this.jornadaEditando.id;
            // Leer el estado ACTUAL desde el backend para comparar correctamente
            const jornadaActual = (await apiManager.get(`/api/jornadas/${jid}`))?.datos || {};
            const prevLista = (jornadaActual.cuestionarioIds || []).slice();
            const nuevaLista = (this.cuestionariosSeleccionados || []).slice();
            
            console.log('🔍 [JORNADAS] prevLista (desde backend):', prevLista);
            console.log('🔍 [JORNADAS] nuevaLista (seleccionados):', nuevaLista);

            // Incluir todos los campos de la jornada actual (desde backend) para evitar que se pierdan datos
            const datos = {
                nombre: jornadaActual.nombre,
                fechaJornada: jornadaActual.fechaJornada || null,
                lugar: jornadaActual.lugar || '',
                notas: jornadaActual.notas || '',
                cuestionarioIds: this.cuestionariosSeleccionados,
                comboIds: jornadaActual.comboIds || []
            };
            
            console.log('🔍 [JORNADAS] Datos a enviar:', datos);
            await apiManager.put(`/api/jornadas/${jid}`, datos);
            Utils.showAlert('Cuestionarios actualizados exitosamente', 'success');
            
            // Registrar acción de deshacer/rehacer para cuestionarios
            if (window.UndoManager) {
                // Capturar un snapshot estático usando el estado actual del backend
                const snapshot = {
                    id: jid,
                    nombre: jornadaActual.nombre,
                    fechaJornada: jornadaActual.fechaJornada,
                    lugar: jornadaActual.lugar,
                    notas: jornadaActual.notas,
                    comboIds: (jornadaActual.comboIds || []).slice()
                };
                const label = `Jornada ${jid}: cuestionarios ${prevLista.join(',')}→${nuevaLista.join(',')}`;

                // Si no hay cambios reales, no registrar
                if ((prevLista || []).join(',') === (nuevaLista || []).join(',')) {
                    console.log('[UNDO][skip][cuestionarios] Sin cambios reales, no se registra acción:', label);
                } else {
                    const hacer = async () => {
                        console.log('[UNDO][do][cuestionarios] Ejecutando DO:', label, { jid: snapshot.id, nuevaLista, snapshot });
                        // Basar en estado actual para evitar conflictos
                        const jAct = (await apiManager.get(`/api/jornadas/${snapshot.id}`))?.datos || {};
                        const d = {
                            nombre: jAct?.nombre || snapshot.nombre,
                            fechaJornada: jAct?.fechaJornada ?? snapshot.fechaJornada,
                            lugar: jAct?.lugar ?? snapshot.lugar,
                            notas: jAct?.notas ?? snapshot.notas,
                            cuestionarioIds: nuevaLista,
                            comboIds: Array.isArray(jAct?.comboIds) ? jAct.comboIds : snapshot.comboIds
                        };
                        await apiManager.put(`/api/jornadas/${snapshot.id}`, d);
                        await this.cargarDatos(true);
                        this.mostrarJornadas();
                        this.restoreScrollOrFocus();
                    };
                    const deshacer = async () => {
                        console.log('[UNDO][undo][cuestionarios] Ejecutando UNDO:', label, { jid: snapshot.id, prevLista, snapshot });
                        const jAct = (await apiManager.get(`/api/jornadas/${snapshot.id}`))?.datos || {};
                        const d = {
                            nombre: jAct?.nombre || snapshot.nombre,
                            fechaJornada: jAct?.fechaJornada ?? snapshot.fechaJornada,
                            lugar: jAct?.lugar ?? snapshot.lugar,
                            notas: jAct?.notas ?? snapshot.notas,
                            cuestionarioIds: prevLista,
                            comboIds: Array.isArray(jAct?.comboIds) ? jAct.comboIds : snapshot.comboIds
                        };
                        await apiManager.put(`/api/jornadas/${snapshot.id}`, d);
                        await this.cargarDatos(true);
                        this.mostrarJornadas();
                        this.restoreScrollOrFocus();
                    };
                    console.log('[UNDO][record][cuestionarios] Registrando acción:', label, { prevLista, nuevaLista, snapshot, hasUndoManager: !!window.UndoManager });
                    window.UndoManager.record({ do: hacer, undo: deshacer, label });
                }
            }

            // Recargar datos para mostrar los cambios
            await this.cargarDatos(true);
            this.mostrarJornadas();
            this.restoreScrollOrFocus();
            if (this.reabrirEditarTrasSeleccion) {
                this.editarJornada(jid);
                this.reabrirEditarTrasSeleccion = false;
            }
            
        } catch (error) {
            console.error('❌ [JORNADAS] Error al guardar cambios de cuestionarios:', error);
            const mensajeError = this.extraerMensajeError(error.message);
            Utils.showAlert(mensajeError, 'error');
        }
    },

    quitarCombo(id) {
        const index = this.idEnLista(this.combosSeleccionados, id);
        if (index > -1) {
            this.combosSeleccionados = this.quitarDeSlots(this.combosSeleccionados, id);
            this.actualizarSlotsVisual();
            
            // Si estamos editando una jornada existente, guardar los cambios
            if (this.jornadaEditando) {
                this.guardarCambiosCombos();
            }
        }
    },

    async guardarCambiosCombos() {
        try {
            this.rememberScroll();
            console.log('🔍 [JORNADAS] Guardando cambios de combos para jornada:', this.jornadaEditando.id);
            console.log('🔍 [JORNADAS] Combos seleccionados:', this.combosSeleccionados);
            console.log('🔍 [JORNADAS] Jornada editando:', this.jornadaEditando);
            
            const jid = this.jornadaEditando.id;
            // Leer el estado ACTUAL desde el backend para comparar correctamente
            const jornadaActual = (await apiManager.get(`/api/jornadas/${jid}`))?.datos || {};
            const prevLista = (jornadaActual.comboIds || []).slice();
            const nuevaLista = (this.combosSeleccionados || []).slice();
            
            console.log('🔍 [JORNADAS] prevLista (desde backend):', prevLista);
            console.log('🔍 [JORNADAS] nuevaLista (seleccionados):', nuevaLista);

            // Incluir todos los campos de la jornada actual (desde backend) para evitar que se pierdan datos
            const datos = {
                nombre: jornadaActual.nombre,
                fechaJornada: jornadaActual.fechaJornada || null,
                lugar: jornadaActual.lugar || '',
                notas: jornadaActual.notas || '',
                cuestionarioIds: jornadaActual.cuestionarioIds || [],
                comboIds: this.combosSeleccionados
            };
            
            console.log('🔍 [JORNADAS] Datos a enviar:', datos);
            await apiManager.put(`/api/jornadas/${jid}`, datos);
            Utils.showAlert('Combos actualizados exitosamente', 'success');
            
            // Registrar acción de deshacer/rehacer para combos
            if (window.UndoManager) {
                // Capturar un snapshot estático usando el estado actual del backend
                const snapshot = {
                    id: jid,
                    nombre: jornadaActual.nombre,
                    fechaJornada: jornadaActual.fechaJornada,
                    lugar: jornadaActual.lugar,
                    notas: jornadaActual.notas,
                    cuestionarioIds: (jornadaActual.cuestionarioIds || []).slice()
                };
                const label = `Jornada ${jid}: combos ${prevLista.join(',')}→${nuevaLista.join(',')}`;
                if ((prevLista || []).join(',') === (nuevaLista || []).join(',')) {
                    console.log('[UNDO][skip][combos] Sin cambios reales, no se registra acción:', label);
                } else {
                    const hacer = async () => {
                        console.log('[UNDO][do][combos] Ejecutando DO:', label, { jid: snapshot.id, nuevaLista, snapshot });
                        const jAct = (await apiManager.get(`/api/jornadas/${snapshot.id}`))?.datos || {};
                        const d = {
                            nombre: jAct?.nombre || snapshot.nombre,
                            fechaJornada: jAct?.fechaJornada ?? snapshot.fechaJornada,
                            lugar: jAct?.lugar ?? snapshot.lugar,
                            notas: jAct?.notas ?? snapshot.notas,
                            cuestionarioIds: Array.isArray(jAct?.cuestionarioIds) ? jAct.cuestionarioIds : snapshot.cuestionarioIds,
                            comboIds: nuevaLista
                        };
                        await apiManager.put(`/api/jornadas/${snapshot.id}`, d);
                        await this.cargarDatos(true);
                        this.mostrarJornadas();
                        this.restoreScrollOrFocus();
                    };
                    const deshacer = async () => {
                        console.log('[UNDO][undo][combos] Ejecutando UNDO:', label, { jid: snapshot.id, prevLista, snapshot });
                        const jAct = (await apiManager.get(`/api/jornadas/${snapshot.id}`))?.datos || {};
                        const d = {
                            nombre: jAct?.nombre || snapshot.nombre,
                            fechaJornada: jAct?.fechaJornada ?? snapshot.fechaJornada,
                            lugar: jAct?.lugar ?? snapshot.lugar,
                            notas: jAct?.notas ?? snapshot.notas,
                            cuestionarioIds: Array.isArray(jAct?.cuestionarioIds) ? jAct.cuestionarioIds : snapshot.cuestionarioIds,
                            comboIds: prevLista
                        };
                        await apiManager.put(`/api/jornadas/${snapshot.id}`, d);
                        await this.cargarDatos(true);
                        this.mostrarJornadas();
                        this.restoreScrollOrFocus();
                    };
                    console.log('[UNDO][record][combos] Registrando acción:', label, { prevLista, nuevaLista, snapshot, hasUndoManager: !!window.UndoManager });
                    window.UndoManager.record({ do: hacer, undo: deshacer, label });
                }
            }

            // Recargar datos para mostrar los cambios
            await this.cargarDatos(true);
            this.mostrarJornadas();
            this.restoreScrollOrFocus();
            if (this.reabrirEditarTrasSeleccion) {
                this.editarJornada(jid);
                this.reabrirEditarTrasSeleccion = false;
            }
            
        } catch (error) {
            console.error('❌ [JORNADAS] Error al guardar cambios de combos:', error);
            const mensajeError = this.extraerMensajeError(error.message);
            Utils.showAlert(mensajeError, 'error');
        }
    },

    filtrarCuestionarios(texto) {
        const items = document.querySelectorAll('#listaCuestionarios .list-group-item');
        items.forEach(item => {
            const content = item.textContent.toLowerCase();
            if (content.includes(texto.toLowerCase())) {
                item.style.display = 'block';
            } else {
                item.style.display = 'none';
            }
        });
    },

    filtrarCombos(texto) {
        const items = document.querySelectorAll('#listaCombos .list-group-item');
        items.forEach(item => {
            const content = item.textContent.toLowerCase();
            if (content.includes(texto.toLowerCase())) {
                item.style.display = 'block';
            } else {
                item.style.display = 'none';
            }
        });
    },

    async aplicarFiltros() {
        console.log('🔍 [JORNADAS] Aplicando filtros...');
        // Resetear paginación y recargar datos
        await this.cargarDatos(true);
    },

    async verDetalle(id) {
        try {
            const response = await apiManager.get(`/api/jornadas/${id}`);
            const jornada = response.datos;
            const estadoVista = jornada.estado || 'preparacion';
            
            let detalleHtml = `
                <div class="modal fade" id="modalDetalle" tabindex="-1">
                    <div class="modal-dialog modal-lg">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title">Detalle de Jornada - ${jornada.nombre}</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body">
                                <div class="alert alert-secondary">
                                    <h5 class="d-flex align-items-center">
                                        <span style="font-weight: bold; font-size: 1.1em; color: #0066cc;" class="me-2">ID: ${jornada.id}</span>
                                        ${jornada.nombre}
                                    </h5>
                                </div>
                                <div class="row">
                                    <div class="col-md-6">
                                        <p><strong>Fecha:</strong> ${jornada.fechaJornada ? new Date(jornada.fechaJornada).toLocaleDateString('es-ES') : 'Sin fecha'}</p>
                                        <p><strong>Lugar:</strong> ${jornada.lugar || 'No especificado'}</p>
                                        <p><strong>Estado:</strong> ${this.getEstadoBadge(estadoVista)}</p>
                                    </div>
                                    <div class="col-md-6">
                                        <p><strong>Creada por:</strong> ${jornada.creacionUsuarioNombre}</p>
                                        <p><strong>Fecha creación:</strong> ${new Date(jornada.fechaCreacion).toLocaleDateString('es-ES')}</p>
                                    </div>
                                </div>
                                ${jornada.notas ? `<p><strong>Notas:</strong> ${jornada.notas}</p>` : ''}
                                
                                <h6 class="mt-4">Cuestionarios (${jornada.cuestionarios ? jornada.cuestionarios.length : 0})</h6>
                                <div class="list-group">
                                    ${jornada.cuestionarios ? jornada.cuestionarios.map(c => `
                                        <div class="list-group-item d-flex justify-content-between align-items-center">
                                            <div>
                                                <strong>Cuestionario #${c.id}</strong> - ${c.nivel} - ${c.estado}
                                                ${c.tematica ? `<br><small>Temática: ${c.tematica}</small>` : ''}
                                            </div>
                                            <button class="btn btn-sm btn-outline-info" onclick="JornadasManager.verPreguntasCuestionario(${c.id})">
                                                <i class="fas fa-eye"></i> Ver preguntas
                                            </button>
                                        </div>
                                    `).join('') : '<p class="text-muted">No hay cuestionarios asignados</p>'}
                                </div>
                                
                                <h6 class="mt-4">Combos (${jornada.combos ? jornada.combos.length : 0})</h6>
                                <div class="list-group">
                                    ${jornada.combos ? jornada.combos.map(c => `
                                        <div class="list-group-item d-flex justify-content-between align-items-center">
                                            <div>
                                                <strong>Combo #${c.id}</strong> - ${c.nivel} - ${Utils.formatearEstadoCombo(c.estado)}
                                                ${c.tipo ? `<br><small>Tipo: ${c.tipo}</small>` : ''}
                                            </div>
                                            <button class="btn btn-sm btn-outline-info" onclick="JornadasManager.verPreguntasCombo(${c.id})">
                                                <i class="fas fa-eye"></i> Ver preguntas
                                            </button>
                                        </div>
                                    `).join('') : '<p class="text-muted">No hay combos asignados</p>'}
                                </div>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cerrar</button>
                                <button type="button" class="btn btn-success" onclick="JornadasManager.exportarExcel(${id})">
                                    <i class="fas fa-file-excel"></i> Exportar Excel
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            `;
            
            // Eliminar modal anterior si existe
            const modalExistente = document.getElementById('modalDetalle');
            if (modalExistente) {
                modalExistente.remove();
            }
            
            document.body.insertAdjacentHTML('beforeend', detalleHtml);
            const modal = new bootstrap.Modal(document.getElementById('modalDetalle'));
            modal.show();
            
            // Limpiar modal al cerrarse
            document.getElementById('modalDetalle').addEventListener('hidden.bs.modal', function() {
                this.remove();
            });
            
        } catch (error) {
            console.error('❌ [JORNADAS] Error al cargar detalle:', error);
            Utils.showAlert('Error al cargar el detalle de la jornada', 'error');
        }
    },

    // Funciones para ver preguntas
    async verPreguntasCuestionario(cuestionarioId) {
        try {
            console.log(`📋 [JORNADAS] Cargando preguntas del cuestionario ${cuestionarioId}`);
            const cuestionario = await apiManager.get(`/api/cuestionarios/${cuestionarioId}`);
            
            if (cuestionario) {
                this.mostrarModalPreguntasCuestionario(cuestionario);
            } else {
                Utils.showAlert('Error al cargar las preguntas del cuestionario', 'error');
            }
        } catch (error) {
            console.error('❌ [JORNADAS] Error al cargar preguntas del cuestionario:', error);
            Utils.showAlert('Error al cargar las preguntas del cuestionario', 'error');
        }
    },

    async verPreguntasCombo(comboId) {
        try {
            console.log(`🔗 [JORNADAS] Cargando preguntas del combo ${comboId}`);
            const combo = await apiManager.get(`/api/combos/${comboId}`);
            
            if (combo) {
                this.mostrarModalPreguntasCombo(combo);
            } else {
                Utils.showAlert('Error al cargar las preguntas del combo', 'error');
            }
        } catch (error) {
            console.error('❌ [JORNADAS] Error al cargar preguntas del combo:', error);
            Utils.showAlert('Error al cargar las preguntas del combo', 'error');
        }
    },

    mostrarModalPreguntasCuestionario(cuestionario) {
        const titulo = document.getElementById('modalVerPreguntasCuestionarioTitulo');
        const tbody = document.getElementById('tablaPreguntasCuestionario');
        
        titulo.textContent = `Preguntas del Cuestionario #${cuestionario.id} (${cuestionario.tematica || 'Sin temática'})`;
        
        let html = '';
        if (cuestionario.preguntas && cuestionario.preguntas.length > 0) {
            const preguntasParaOrdenar = [...cuestionario.preguntas];
            // Ordenar por nivel de pregunta (1,2,3,4)
            const preguntasOrdenadas = preguntasParaOrdenar.sort((a, b) => {
                if (a.pregunta && b.pregunta) {
                    // Extraer solo los dígitos del nivel
                    const nivelA = parseInt(String(a.pregunta.nivel).replace(/\D/g, '')) || 0;
                    const nivelB = parseInt(String(b.pregunta.nivel).replace(/\D/g, '')) || 0;
                    return nivelA - nivelB;
                }
                return 0;
            });
            preguntasOrdenadas.forEach((preguntaCuestionario, index) => {
                const pregunta = preguntaCuestionario.pregunta;
                if (!pregunta) return;
                // Mostrar solo el nivel real de la pregunta
                let nivel = pregunta.nivel ? String(pregunta.nivel).replace(/^_/, '') : '';
                html += `
                    <tr>
                        <td><span class="badge bg-light text-secondary fw-bold">${nivel}</span></td>
                        <td>${pregunta.pregunta || 'Sin texto'}</td>
                        <td><strong>${pregunta.respuesta || 'Sin respuesta'}</strong></td>
                    </tr>
                `;
            });
        } else {
            html = '<tr><td colspan="3" class="text-center text-muted">No hay preguntas disponibles</td></tr>';
        }
        tbody.innerHTML = html;
        const modalEl = document.getElementById('modalVerPreguntasCuestionario');
        const modal = new bootstrap.Modal(modalEl);
        this._mostrarModalSobreOtro(modalEl, modal);
    },

    mostrarModalPreguntasCombo(combo) {
        const titulo = document.getElementById('modalVerPreguntasComboTitulo');
        const tbody = document.getElementById('tablaPreguntasCombo');
        
        titulo.textContent = `Preguntas del Combo #${combo.id} (Tipo: ${combo.tipo || 'No especificado'})`;
        
        // Actualizar las columnas para mostrar MULT en vez de Factor
        const thFactorElement = document.querySelector('#modalVerPreguntasCombo thead th:first-child');
        if (thFactorElement) {
            thFactorElement.textContent = 'MULT';
        }
        
        let html = '';
        if (combo.preguntas && combo.preguntas.length > 0) {
            const preguntasParaOrdenar = [...combo.preguntas];
            // Ordenar por factor de multiplicación
            const preguntasOrdenadas = preguntasParaOrdenar.sort((a, b) => {
                if (a.pregunta && b.pregunta) {
                    const factorA = parseInt(String(a.factorMultiplicacion).replace(/\D/g, '')) || 0;
                    const factorB = parseInt(String(b.factorMultiplicacion).replace(/\D/g, '')) || 0;
                    return factorA - factorB;
                }
                return 0;
            });
            preguntasOrdenadas.forEach((preguntaSlot, index) => {
                if (preguntaSlot.pregunta) {
                    const pregunta = preguntaSlot.pregunta;
                    // Mostrar el factor de multiplicación exactamente como está
                    let factorStr = preguntaSlot.factorMultiplicacion || '';
                    
                    // Intentar formatear para casos comunes
                    const factorNum = parseInt(factorStr);
                    if (!isNaN(factorNum)) {
                        if (factorNum === 2) factorStr = 'x2';
                        else if (factorNum === 3) factorStr = 'x3';
                        else if (factorNum === 0) factorStr = 'x';
                        else factorStr = `x${factorNum}`;
                    }
                    
                    // Mostrar 5LS o 5NLS según el tipo de pregunta
                    const nivelText = pregunta.nivel?.includes('LS') ? '5LS' : '5NLS';
                    
                    html += `
                        <tr>
                            <td style="width:80px;">
                                <input class="form-control form-control-sm" 
                                       value="${factorStr}"
                                       onblur="JornadasManager.actualizarFactorDesdeModal(${combo.id}, ${pregunta.id}, this.value)"
                                       title="Editar multiplicador (p.ej. X, X2, X3)">
                            </td>
                            <td>${pregunta.pregunta || 'Sin texto'}</td>
                            <td><strong>${pregunta.respuesta || 'Sin respuesta'}</strong></td>
                        </tr>
                    `;
                }
            });
        } else {
            html = '<tr><td colspan="3" class="text-center text-muted">No hay preguntas disponibles</td></tr>';
        }
        tbody.innerHTML = html;
        const modalEl = document.getElementById('modalVerPreguntasCombo');
        const modal = new bootstrap.Modal(modalEl);
        this._mostrarModalSobreOtro(modalEl, modal);
    },

    /** Abre un modal por encima de cualquier otro modal ya abierto, elevando su z-index y el backdrop */
    _mostrarModalSobreOtro(modalEl, bsModal) {
        // Elevar el modal por encima de los demás (Bootstrap usa 1055 por defecto)
        modalEl.style.zIndex = '1075';
        bsModal.show();
        // En cuanto el backdrop nuevo esté en el DOM, subirlo también
        modalEl.addEventListener('shown.bs.modal', function handler() {
            const backdrops = document.querySelectorAll('.modal-backdrop');
            if (backdrops.length > 0) {
                backdrops[backdrops.length - 1].style.zIndex = '1070';
            }
            modalEl.removeEventListener('shown.bs.modal', handler);
        });
        // Al cerrarse, restablecer el z-index para usos futuros
        modalEl.addEventListener('hidden.bs.modal', function resetZ() {
            modalEl.style.zIndex = '';
            modalEl.removeEventListener('hidden.bs.modal', resetZ);
        });
    },

    async actualizarFactorDesdeModal(comboId, preguntaId, valor) {
        try {
            const factor = (valor || '').trim();
            const body = { factorMultiplicacion: factor };
            await apiManager.putUndoable(`/api/combos/${comboId}/preguntas/${preguntaId}/factor`, body, { label: `Factor combo ${comboId}` , snapshotEndpoint: `/api/combos/${comboId}`});
            Toastify({
                text: 'Multiplicador actualizado',
                duration: 2000,
                close: true,
                gravity: 'top',
                position: 'right',
                style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' }
            }).showToast();
        } catch (e) {
            console.error('❌ [JORNADAS] Error al actualizar multiplicador:', e);
            const msg = this.extraerMensajeError?.(e.message) || 'Error al actualizar multiplicador';
            Utils.showAlert(msg, 'error');
        }
    },

    // ========================================
    // NUEVAS FUNCIONALIDADES DE HISTORIAL
    // ========================================

    // Mostrar modal de elementos no usados
    mostrarElementosNoUsados() {
        const select = document.getElementById('selectJornadaNoUsados');
        select.innerHTML = '<option value="">Selecciona una jornada...</option>';
        
        this.jornadas.forEach(jornada => {
            const option = document.createElement('option');
            option.value = jornada.id;
            option.textContent = `${jornada.nombre} (${jornada.fechaJornada || 'Sin fecha'})`;
            select.appendChild(option);
        });
        
        const modal = new bootstrap.Modal(document.getElementById('modalElementosNoUsados'));
        modal.show();
    },

    // Cargar elementos no usados de una jornada
    async cargarElementosNoUsados() {
        const jornadaId = document.getElementById('selectJornadaNoUsados').value;
        const container = document.getElementById('elementosNoUsadosContainer');
        
        if (!jornadaId) {
            container.innerHTML = '<div class="text-center py-3"><p class="text-muted">Selecciona una jornada para ver los elementos no usados</p></div>';
            return;
        }
        
        try {
            container.innerHTML = '<div class="text-center py-3"><i class="fas fa-spinner fa-spin"></i> Cargando...</div>';
            
            const response = await apiManager.get(`/api/historial-jornadas/jornada/${jornadaId}/no-usados`);
            const elementos = response.datos || [];
            
            if (elementos.length === 0) {
                container.innerHTML = '<div class="text-center py-3"><p class="text-success">No hay elementos no usados en esta jornada</p></div>';
                return;
            }
            
            let html = '<div class="row">';
            html += '<div class="col-12 mb-3">';
            html += '<button class="btn btn-warning btn-sm" onclick="JornadasManager.mostrarModalMarcarNoUsados()">';
            html += '<i class="fas fa-check"></i> Marcar Elementos como No Usados</button>';
            html += '</div>';
            html += '</div>';
            
            const cuestionarios = elementos.filter(e => e.tipoAsignacion === 'CUESTIONARIO');
            const combos = elementos.filter(e => e.tipoAsignacion === 'COMBO');
            
            if (cuestionarios.length > 0) {
                html += '<div class="mb-4">';
                html += '<h6><i class="fas fa-list"></i> Cuestionarios No Usados</h6>';
                cuestionarios.forEach(elemento => {
                    html += `<div class="elemento-no-usado">`;
                    html += `<strong>Cuestionario #${elemento.cuestionarioId}</strong> - Asignado el ${new Date(elemento.fechaAsignacion).toLocaleDateString()}`;
                    html += `<br><small class="text-muted">Estado: ${elemento.estadoAsignacion}</small>`;
                    html += `</div>`;
                });
                html += '</div>';
            }
            
            if (combos.length > 0) {
                html += '<div class="mb-4">';
                html += '<h6><i class="fas fa-cube"></i> Combos No Usados</h6>';
                combos.forEach(elemento => {
                    html += `<div class="elemento-no-usado">`;
                    html += `<strong>Combo #${elemento.comboId}</strong> - Asignado el ${new Date(elemento.fechaAsignacion).toLocaleDateString()}`;
                    if (elemento.preguntaUsadaId) {
                        html += `<br><small class="text-success">Pregunta usada: #${elemento.preguntaUsadaId}</small>`;
                    }
                    html += `<br><small class="text-muted">Estado: ${elemento.estadoAsignacion}</small>`;
                    html += `</div>`;
                });
                html += '</div>';
            }
            
            container.innerHTML = html;
            
        } catch (error) {
            console.error('Error al cargar elementos no usados:', error);
            container.innerHTML = '<div class="text-center py-3"><p class="text-danger">Error al cargar elementos no usados</p></div>';
        }
    },

    // Mostrar modal para marcar elementos como no usados
    mostrarModalMarcarNoUsados() {
        const jornadaId = document.getElementById('selectJornadaNoUsados').value;
        const jornada = this.jornadas.find(j => j.id == jornadaId);
        
        if (!jornada) {
            Utils.showAlert('Selecciona una jornada primero', 'error');
            return;
        }
        
        document.getElementById('jornadaNoUsadosNombre').value = jornada.nombre;
        document.getElementById('jornadaNoUsadosId').value = jornada.id;
        
        // Cargar elementos no usados para selección
        this.cargarElementosParaMarcar(jornadaId);
        
        const modal = new bootstrap.Modal(document.getElementById('modalMarcarNoUsados'));
        modal.show();
    },

    // Cargar elementos para marcar como no usados
    async cargarElementosParaMarcar(jornadaId) {
        try {
            const response = await apiManager.get(`/api/historial-jornadas/jornada/${jornadaId}/no-usados`);
            const elementos = response.datos || [];
            
            const cuestionarios = elementos.filter(e => e.tipoAsignacion === 'CUESTIONARIO');
            const combos = elementos.filter(e => e.tipoAsignacion === 'COMBO');
            
            // Llenar cuestionarios
            let htmlCuestionarios = '';
            if (cuestionarios.length > 0) {
                cuestionarios.forEach(elemento => {
                    htmlCuestionarios += `
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" value="${elemento.cuestionarioId}" 
                                   id="cuestionario_${elemento.cuestionarioId}" checked>
                            <label class="form-check-label" for="cuestionario_${elemento.cuestionarioId}">
                                Cuestionario #${elemento.cuestionarioId} - ${new Date(elemento.fechaAsignacion).toLocaleDateString()}
                            </label>
                        </div>
                    `;
                });
            } else {
                htmlCuestionarios = '<p class="text-muted">No hay cuestionarios no usados</p>';
            }
            document.getElementById('cuestionariosNoUsadosList').innerHTML = htmlCuestionarios;
            
            // Llenar combos
            let htmlCombos = '';
            if (combos.length > 0) {
                combos.forEach(elemento => {
                    htmlCombos += `
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" value="${elemento.comboId}" 
                                   id="combo_${elemento.comboId}" checked>
                            <label class="form-check-label" for="combo_${elemento.comboId}">
                                Combo #${elemento.comboId} - ${new Date(elemento.fechaAsignacion).toLocaleDateString()}
                                ${elemento.preguntaUsadaId ? `<br><small class="text-success">Pregunta usada: #${elemento.preguntaUsadaId}</small>` : ''}
                            </label>
                        </div>
                    `;
                });
            } else {
                htmlCombos = '<p class="text-muted">No hay combos no usados</p>';
            }
            document.getElementById('combosNoUsadosList').innerHTML = htmlCombos;
            
        } catch (error) {
            console.error('Error al cargar elementos para marcar:', error);
            Utils.showAlert('Error al cargar elementos', 'error');
        }
    },

    // Marcar elementos como no usados
    async marcarElementosNoUsados() {
        const jornadaId = document.getElementById('jornadaNoUsadosId').value;
        const motivo = document.getElementById('motivoNoUsados').value;
        
        if (!motivo.trim()) {
            Utils.showAlert('Debes especificar un motivo', 'error');
            return;
        }
        
        // Obtener elementos seleccionados
        const cuestionarioIds = Array.from(document.querySelectorAll('#cuestionariosNoUsadosList input:checked'))
            .map(cb => parseInt(cb.value));
        const comboIds = Array.from(document.querySelectorAll('#combosNoUsadosList input:checked'))
            .map(cb => parseInt(cb.value));
        
        if (cuestionarioIds.length === 0 && comboIds.length === 0) {
            Utils.showAlert('Debes seleccionar al menos un elemento', 'error');
            return;
        }
        
        try {
            const data = {
                jornadaId: parseInt(jornadaId),
                cuestionarioIds: cuestionarioIds,
                comboIds: comboIds,
                motivo: motivo
            };
            
            await apiManager.post('/api/historial-jornadas/marcar-no-usados', data);
            
            Utils.showAlert('Elementos marcados como no usados correctamente', 'success');
            
            // Cerrar modal y recargar datos
            bootstrap.Modal.getInstance(document.getElementById('modalMarcarNoUsados')).hide();
            await this.cargarDatos(true);
            this.mostrarJornadas();
            
        } catch (error) {
            console.error('Error al marcar elementos como no usados:', error);
            const mensajeError = this.extraerMensajeError(error.message);
            Utils.showAlert(mensajeError, 'error');
        }
    },

    // Mostrar modal de reaprovechar combo
    mostrarModalReaprovechar() {
        const select = document.getElementById('selectComboOriginal');
        select.innerHTML = '<option value="">Selecciona un combo...</option>';
        
        // Cargar combos disponibles (que no estén reaprovechados)
        this.combosDisponibles.forEach(combo => {
            if (combo.estado !== 'reaprovechado') {
                const option = document.createElement('option');
                option.value = combo.id;
                option.textContent = `Combo #${combo.id} - ${combo.tipo || 'Sin tipo'} (${combo.nivel})`;
                select.appendChild(option);
            }
        });
        
        const modal = new bootstrap.Modal(document.getElementById('modalReaprovecharCombo'));
        modal.show();
    },

    // Cargar detalles del combo seleccionado
    async cargarDetallesCombo() {
        const comboId = document.getElementById('selectComboOriginal').value;
        const container = document.getElementById('detallesComboOriginal');
        
        if (!comboId) {
            container.style.display = 'none';
            return;
        }
        
        try {
            const combo = this.combosDisponibles.find(c => c.id == comboId);
            if (!combo) {
                container.style.display = 'none';
                return;
            }
            
            let html = `<h6>Combo #${combo.id}</h6>`;
            html += `<p><strong>Tipo:</strong> ${combo.tipo || 'No especificado'}</p>`;
            html += `<p><strong>Nivel:</strong> ${combo.nivel}</p>`;
            html += `<p><strong>Estado:</strong> <span class="badge bg-${this.getBadgeColor(combo.estado)}">${combo.estado}</span></p>`;
            
            if (combo.preguntas && combo.preguntas.length > 0) {
                html += '<h6>Preguntas:</h6>';
                combo.preguntas.forEach((preguntaCombo, index) => {
                    const pregunta = preguntaCombo.pregunta;
                    if (pregunta) {
                        html += `
                            <div class="pregunta-combo">
                                <strong>Pregunta ${index + 1}:</strong> ${pregunta.pregunta}<br>
                                <strong>Respuesta:</strong> ${pregunta.respuesta}<br>
                                <strong>Factor:</strong> ${preguntaCombo.factorMultiplicacion || 'Sin factor'}
                            </div>
                        `;
                    }
                });
            }
            
            container.innerHTML = html;
            container.style.display = 'block';
            
            // Llenar select de pregunta usada
            this.llenarSelectPreguntaUsada(combo);
            
        } catch (error) {
            console.error('Error al cargar detalles del combo:', error);
            container.innerHTML = '<p class="text-danger">Error al cargar detalles del combo</p>';
        }
    },

    // Llenar select de pregunta usada
    llenarSelectPreguntaUsada(combo) {
        const select = document.getElementById('selectPreguntaUsada');
        select.innerHTML = '<option value="">Selecciona la pregunta usada...</option>';
        
        if (combo.preguntas && combo.preguntas.length > 0) {
            combo.preguntas.forEach((preguntaCombo, index) => {
                const pregunta = preguntaCombo.pregunta;
                if (pregunta) {
                    const option = document.createElement('option');
                    option.value = pregunta.id;
                    option.textContent = `Pregunta ${index + 1}: ${pregunta.pregunta.substring(0, 50)}...`;
                    select.appendChild(option);
                }
            });
        }
    },

    // Actualizar preguntas no usadas cuando se selecciona pregunta usada
    actualizarPreguntasNoUsadas() {
        const preguntaUsadaId = document.getElementById('selectPreguntaUsada').value;
        const comboId = document.getElementById('selectComboOriginal').value;
        
        if (!preguntaUsadaId || !comboId) return;
        
        const combo = this.combosDisponibles.find(c => c.id == comboId);
        if (!combo || !combo.preguntas) return;
        
        // Las preguntas no usadas son todas excepto la seleccionada
        const preguntasNoUsadas = combo.preguntas
            .filter(pc => pc.pregunta && pc.pregunta.id != preguntaUsadaId)
            .map(pc => pc.pregunta.id);
        
        console.log('Preguntas no usadas:', preguntasNoUsadas);
        
        // Cargar preguntas disponibles para el nuevo combo
        this.cargarPreguntasDisponibles();
    },

    // Cargar preguntas disponibles para el nuevo combo
    async cargarPreguntasDisponibles() {
        try {
            const response = await apiManager.get('/api/preguntas?estado=aprobada&estadoDisponibilidad=disponible&nivel=_5LS,_5NLS');
            const preguntas = response.datos?.content || [];
            
            const select = document.getElementById('selectNuevaPregunta');
            select.innerHTML = '<option value="">Selecciona una nueva pregunta...</option>';
            
            preguntas.forEach(pregunta => {
                const option = document.createElement('option');
                option.value = pregunta.id;
                option.textContent = `#${pregunta.id}: ${pregunta.pregunta.substring(0, 60)}...`;
                select.appendChild(option);
            });
            
        } catch (error) {
            console.error('Error al cargar preguntas disponibles:', error);
            Utils.showAlert('Error al cargar preguntas disponibles', 'error');
        }
    },

    // Reaprovechar combo
    async reaprovecharCombo() {
        const comboOriginalId = document.getElementById('selectComboOriginal').value;
        const preguntaUsadaId = document.getElementById('selectPreguntaUsada').value;
        const nuevaPreguntaId = document.getElementById('selectNuevaPregunta').value;
        const factorMultiplicacion = document.getElementById('selectFactorMultiplicacion').value;
        const notas = document.getElementById('notasReaprovechar').value;
        
        if (!comboOriginalId || !preguntaUsadaId || !nuevaPreguntaId) {
            Utils.showAlert('Debes completar todos los campos obligatorios', 'error');
            return;
        }
        
        try {
            const combo = this.combosDisponibles.find(c => c.id == comboOriginalId);
            const preguntasNoUsadas = combo.preguntas
                .filter(pc => pc.pregunta && pc.pregunta.id != preguntaUsadaId)
                .map(pc => pc.pregunta.id);
            
            const data = {
                comboOriginalId: parseInt(comboOriginalId),
                preguntaUsadaId: parseInt(preguntaUsadaId),
                preguntasNoUsadasIds: preguntasNoUsadas,
                nuevaPreguntaId: parseInt(nuevaPreguntaId),
                factorMultiplicacion: factorMultiplicacion,
                notas: notas
            };
            
            const response = await apiManager.postUndoableBackend('/api/historial-jornadas/reaprovechar-combo', data, {
                label: `Reaprovechar combo ${comboOriginalId}`
            });
            
            Utils.showAlert(`Combo reaprovechado correctamente. Nuevo combo ID: ${response.datos.id}`, 'success');
            
            // Cerrar modal y recargar datos
            bootstrap.Modal.getInstance(document.getElementById('modalReaprovecharCombo')).hide();
            await this.cargarDatos(true);
            this.mostrarJornadas();
            
        } catch (error) {
            console.error('Error al reaprovechar combo:', error);
            const mensajeError = this.extraerMensajeError(error.message);
            Utils.showAlert(mensajeError, 'error');
        }
    },

    // Mostrar historial de un cuestionario
    async mostrarHistorialCuestionario(cuestionarioId) {
        try {
            const response = await apiManager.get(`/api/historial-jornadas/cuestionario/${cuestionarioId}`);
            const historial = response.datos || [];
            
            document.getElementById('modalHistorialTitulo').innerHTML = '<i class="fas fa-history"></i> Historial del Cuestionario';
            this.mostrarHistorial(historial, 'cuestionario');
            
        } catch (error) {
            console.error('Error al cargar historial del cuestionario:', error);
            Utils.showAlert('Error al cargar historial', 'error');
        }
    },

    // Mostrar historial de un combo
    async mostrarHistorialCombo(comboId) {
        try {
            const response = await apiManager.get(`/api/historial-jornadas/combo/${comboId}`);
            const historial = response.datos || [];
            
            document.getElementById('modalHistorialTitulo').innerHTML = '<i class="fas fa-history"></i> Historial del Combo';
            this.mostrarHistorial(historial, 'combo');
            
        } catch (error) {
            console.error('Error al cargar historial del combo:', error);
            Utils.showAlert('Error al cargar historial', 'error');
        }
    },

    // Mostrar historial en modal
    mostrarHistorial(historial, tipo) {
        const container = document.getElementById('historialContainer');
        
        if (historial.length === 0) {
            container.innerHTML = '<div class="text-center py-3"><p class="text-muted">No hay historial disponible</p></div>';
        } else {
            let html = '';
            historial.forEach(item => {
                const estadoClass = this.getEstadoClass(item.estadoAsignacion);
                const fechaAsignacion = new Date(item.fechaAsignacion).toLocaleDateString();
                const fechaUso = item.fechaUso ? new Date(item.fechaUso).toLocaleDateString() : 'No usado';
                
                html += `
                    <div class="historial-item ${estadoClass}">
                        <div class="d-flex justify-content-between align-items-start">
                            <div>
                                <h6>Jornada: ${item.jornadaNombre}</h6>
                                <p><strong>Estado:</strong> <span class="badge badge-estado bg-${this.getBadgeColor(item.estadoAsignacion)}">${item.estadoAsignacion}</span></p>
                                <p><strong>Asignado:</strong> ${fechaAsignacion}</p>
                                ${item.fechaUso ? `<p><strong>Usado:</strong> ${fechaUso}</p>` : ''}
                                ${item.preguntaUsadaId ? `<p><strong>Pregunta usada:</strong> #${item.preguntaUsadaId}</p>` : ''}
                                ${item.notas ? `<p><strong>Notas:</strong> ${item.notas}</p>` : ''}
                            </div>
                        </div>
                    </div>
                `;
            });
            container.innerHTML = html;
        }
        
        const modal = new bootstrap.Modal(document.getElementById('modalHistorial'));
        modal.show();
    },

    // Utilidades para colores de badges
    getBadgeColor(estado) {
        const colores = {
            'asignado': 'primary',
            'usado': 'success',
            'no_usado': 'warning',
            'reaprovechado': 'info'
        };
        return colores[estado] || 'secondary';
    },

    getEstadoClass(estado) {
        const clases = {
            'asignado': 'asignado',
            'usado': 'usado',
            'no_usado': 'no-usado',
            'reaprovechado': 'reaprovechado'
        };
        return clases[estado] || '';
    },

    // Función para reutilizar un cuestionario
    async reutilizarCuestionario(cuestionarioId, jornadaId) {
        try {
            console.log(`🔄 [JORNADAS] Reutilizando cuestionario ${cuestionarioId} de jornada ${jornadaId}`);
            
            // Confirmar la acción
            const confirmacion = confirm(`¿Estás seguro de que quieres reutilizar el cuestionario ${cuestionarioId}?\n\nEsto hará que:\n- El cuestionario vuelva a estar disponible\n- Se actualice el historial\n- Se pueda usar en otras jornadas`);
            
            if (!confirmacion) {
                return;
            }

            // Llamar al endpoint para reutilizar el cuestionario
            const doAction = async () => await apiManager.post(`/api/jornadas/${jornadaId}/reutilizar-cuestionario/${cuestionarioId}`);
            const undoAction = async () => await apiManager.post(`/api/jornadas/${jornadaId}/quitar-reutilizacion-cuestionario/${cuestionarioId}`);
            const doWrapped = async () => {
                const r = await doAction();
                await this.cargarDatos();
                this.mostrarJornadas();
                return r;
            };
            const undoWrapped = async () => {
                await undoAction();
                await this.cargarDatos();
                this.mostrarJornadas();
            };
            const response = await doWrapped();
            
            if (response.exito) {
                if (window.UndoManager) window.UndoManager.record({ do: doWrapped, undo: undoWrapped, label: `Reutilizar cuestionario ${cuestionarioId}` });
                Utils.showAlert(`Cuestionario ${cuestionarioId} reutilizado correctamente. Ahora está disponible para usar en otras jornadas.`, 'success');
                
                // Recargar datos y actualizar vista
                // Ya recargado por doWrapped
            } else {
                Utils.showAlert(`Error al reutilizar cuestionario: ${response.mensaje}`, 'error');
            }
            
        } catch (error) {
            console.error('❌ [JORNADAS] Error al reutilizar cuestionario:', error);
            const mensajeError = this.extraerMensajeError(error.message);
            Utils.showAlert(mensajeError, 'error');
        }
    },
    async quitarReutilizacionCuestionario(cuestionarioId, jornadaId) {
        try {
            const doAction = async () => await apiManager.post(`/api/jornadas/${jornadaId}/quitar-reutilizacion-cuestionario/${cuestionarioId}`);
            const undoAction = async () => await apiManager.post(`/api/jornadas/${jornadaId}/reutilizar-cuestionario/${cuestionarioId}`);
            const doWrapped = async () => {
                const r = await doAction();
                await this.cargarDatos(true);
                this.mostrarJornadas();
                return r;
            };
            const undoWrapped = async () => {
                await undoAction();
                await this.cargarDatos(true);
                this.mostrarJornadas();
            };
            const resp = await doWrapped();
            if (resp.exito) {
                if (window.UndoManager) window.UndoManager.record({ do: doWrapped, undo: undoWrapped, label: `Quitar reutilización cuestionario ${cuestionarioId}` });
                Utils.showAlert('Reutilización de cuestionario quitada', 'success');
                // UI ya refrescada por doWrapped
            } else {
                Utils.showAlert(resp.mensaje || 'No se pudo quitar la reutilización', 'error');
            }
        } catch (error) {
            const msg = this.extraerMensajeError(error.message);
            Utils.showAlert(msg, 'error');
        }
    },

    // Variables para el reciclaje de combos
    comboReciclajeActual: null,
    jornadaReciclajeActual: null,
    preguntaSeleccionada: null,
    preguntasReciclajeCount: 0,

    // Función para reutilizar un combo (modal de reciclaje según nº de preguntas)
    async reutilizarCombo(comboId, jornadaId) {
        try {
            console.log(`🔄 [JORNADAS] Reciclaje combo ${comboId} de jornada ${jornadaId}`);
            
            this.comboReciclajeActual = comboId;
            this.jornadaReciclajeActual = jornadaId;
            this.preguntaSeleccionada = null;

            const response = await apiManager.get(`/api/combos/${comboId}/preguntas`);
            if (!response.exito || !Array.isArray(response.datos)) {
                Utils.showAlert('No se pudieron cargar las preguntas del combo', 'error');
                return;
            }

            const preguntas = response.datos;
            if (preguntas.length !== 3) {
                Utils.showAlert('Solo se pueden reciclar combos con exactamente 3 preguntas', 'error');
                return;
            }

            this.resetModalReciclaje();

            const modal = new bootstrap.Modal(document.getElementById('modalReciclajeCombo'));
            modal.show();
            
        } catch (error) {
            console.error('❌ [JORNADAS] Error al abrir reciclaje:', error);
            Utils.showAlert('Error al abrir reciclaje de combo', 'error');
        }
    },

    resetModalReciclaje() {
        const paso1 = document.getElementById('pasoReciclaje');
        const paso2 = document.getElementById('pasoSeleccionPregunta');
        const btnConfirmar = document.getElementById('btnConfirmarReciclaje');
        const cont = document.getElementById('preguntasCombo');
        if (cont) cont.innerHTML = '';
        if (paso1) paso1.style.display = 'block';
        if (paso2) paso2.style.display = 'none';
        if (btnConfirmar) btnConfirmar.style.display = 'none';
        const modalEl = document.getElementById('modalReciclajeCombo');
        if (modalEl) {
            modalEl.querySelectorAll('.pregunta-card').forEach(card => {
                card.classList.remove('border-primary', 'border-3');
            });
        }
    },
    async quitarReutilizacionCombo(comboId, jornadaId) {
        try {
            const doAction = async () => await apiManager.post(`/api/jornadas/${jornadaId}/quitar-reutilizacion-combo/${comboId}`);
            const undoAction = async () => await apiManager.post(`/api/jornadas/${jornadaId}/reciclar-combo-entero/${comboId}`);
            const doWrapped = async () => {
                const r = await doAction();
                await this.cargarDatos(true);
                this.mostrarJornadas();
                return r;
            };
            const undoWrapped = async () => {
                await undoAction();
                await this.cargarDatos(true);
                this.mostrarJornadas();
            };
            const resp = await doWrapped();
            if (resp.exito) {
                if (window.UndoManager) window.UndoManager.record({ do: doWrapped, undo: undoWrapped, label: `Quitar reutilización combo ${comboId}` });
                Utils.showAlert('Reutilización de combo quitada', 'success');
                // UI ya refrescada por doWrapped
            } else {
                Utils.showAlert(resp.mensaje || 'No se pudo quitar la reutilización', 'error');
            }
        } catch (error) {
            const msg = this.extraerMensajeError(error.message);
            Utils.showAlert(msg, 'error');
        }
    },

    // Reciclar combo entero (reaprovechado completo)
    async reciclarComboEntero() {
        const confirmacion = confirm(
            `¿Reciclar el combo ${this.comboReciclajeActual} completo?\n\nQuedará reaprovechado y disponible para otras jornadas.`
        );
        if (!confirmacion) return;
        await this.ejecutarReciclajeEntero();
    },

    async ejecutarReciclajeEntero() {
        const comboId = this.comboReciclajeActual;
        const jornadaId = this.jornadaReciclajeActual;
        try {
            console.log(`🔄 [JORNADAS] Reciclando combo entero ${comboId} de jornada ${jornadaId}`);

            const response = await apiManager.postUndoableBackend(
                `/api/jornadas/${jornadaId}/reciclar-combo-entero/${comboId}`,
                {},
                { label: `Reciclar combo ${comboId}` }
            );
            const modalInst = bootstrap.Modal.getInstance(document.getElementById('modalReciclajeCombo'));
            if (modalInst) modalInst.hide();
            await this.cargarDatos();
            this.mostrarJornadas();
            
            if (response.exito) {
                Utils.showAlert(`Combo ${comboId} reaprovechado correctamente.`, 'success');
            } else {
                Utils.showAlert(`Error al reciclar combo: ${response.mensaje}`, 'error');
            }
            
        } catch (error) {
            console.error('❌ [JORNADAS] Error al reciclar combo entero:', error);
            const mensajeError = this.extraerMensajeError(error.message);
            Utils.showAlert(mensajeError, 'error');
        }
    },

    // Reciclar combo parcial (seleccionar pregunta usada)
    async reciclarComboParcial() {
        try {
            console.log(`🔄 [JORNADAS] Cargando preguntas del combo ${this.comboReciclajeActual} para reciclaje parcial`);
            // Reset selección y contenedor por seguridad
            this.preguntaSeleccionada = null;
            const contPrev = document.getElementById('preguntasCombo');
            if (contPrev) contPrev.innerHTML = '';
            
            // Obtener las preguntas del combo
            const response = await apiManager.get(`/api/combos/${this.comboReciclajeActual}/preguntas`);
            
            if (response.exito && response.datos) {
                const preguntas = response.datos;
                
                if (preguntas.length !== 3) {
                    Utils.showAlert('Solo se pueden reciclar combos con exactamente 3 preguntas', 'error');
                    return;
                }
                
                // Mostrar las preguntas para selección
                this.mostrarPreguntasParaSeleccion(preguntas);
                
            } else {
                Utils.showAlert(`Error al cargar preguntas del combo: ${response.mensaje}`, 'error');
            }
            
        } catch (error) {
            console.error('❌ [JORNADAS] Error al cargar preguntas del combo:', error);
            Utils.showAlert('Error al cargar preguntas del combo', 'error');
        }
    },

    // Mostrar preguntas para selección
    mostrarPreguntasParaSeleccion(preguntas) {
        this.preguntasReciclajeCount = preguntas.length;
        // Ocultar paso 1 y mostrar paso 2
        document.getElementById('pasoReciclaje').style.display = 'none';
        document.getElementById('pasoSeleccionPregunta').style.display = 'block';
        document.getElementById('btnConfirmarReciclaje').style.display = 'block';

        const textoRestantes = 'Se creará un nuevo combo con las 2 preguntas restantes.';
        const alertInfo = document.querySelector('#pasoSeleccionPregunta .alert-info');
        if (alertInfo) {
            alertInfo.innerHTML = `
                <i class="fas fa-info-circle"></i>
                <strong>Selecciona la pregunta que se usó en este combo:</strong><br>
                ${textoRestantes}
            `;
        }
        
        // Generar HTML para las preguntas
        const container = document.getElementById('preguntasCombo');
        let html = '';
        
        preguntas.forEach((pregunta, index) => {
            html += `
                <div class="col-md-4 mb-3">
                    <div class="card pregunta-card" onclick="JornadasManager.seleccionarPregunta(${pregunta.id}, this)">
                        <div class="card-body text-center">
                            <h6 class="card-title">Pregunta ${index + 1}</h6>
                            <p class="card-text">${pregunta.pregunta}</p>
                            <div class="mt-2">
                                <span class="badge bg-primary">${pregunta.nivel}</span>
                                <span class="badge bg-secondary">${pregunta.tematica}</span>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        });
        
        container.innerHTML = html;
    },

    // Seleccionar una pregunta
    seleccionarPregunta(preguntaId, elemento) {
        // Remover selección anterior
        document.querySelectorAll('#modalReciclajeCombo .pregunta-card').forEach(card => {
            card.classList.remove('border-primary', 'border-3');
        });
        
        // Seleccionar nueva pregunta
        elemento.classList.add('border-primary', 'border-3');
        this.preguntaSeleccionada = preguntaId;
        
        console.log(`✅ [JORNADAS] Pregunta seleccionada: ${preguntaId}`);
    },

    // Confirmar reciclaje parcial
    async confirmarReciclajeParcial() {
        if (!this.preguntaSeleccionada) {
            Utils.showAlert('Debes seleccionar una pregunta antes de continuar', 'warning');
            return;
        }

        const comboId = this.comboReciclajeActual;
        const jornadaId = this.jornadaReciclajeActual;
        const preguntaUsadaId = this.preguntaSeleccionada;
        
        try {
            console.log(`🔄 [JORNADAS] Confirmando reciclaje parcial del combo ${comboId} con pregunta usada ${preguntaUsadaId}`);
            
            const response = await apiManager.postUndoableBackend(
                `/api/jornadas/${jornadaId}/reciclar-combo-parcial/${comboId}`,
                { preguntaUsadaId },
                { label: `Reciclaje parcial combo ${comboId}` }
            );
            
            if (response.exito) {
                Utils.showAlert('Combo reciclado parcialmente. Se creó un nuevo combo con las 2 preguntas restantes.', 'success');
                
                bootstrap.Modal.getInstance(document.getElementById('modalReciclajeCombo')).hide();
                await this.cargarDatos();
                this.mostrarJornadas();
            } else {
                Utils.showAlert(`Error al reciclar combo parcialmente: ${response.mensaje}`, 'error');
            }
            
        } catch (error) {
            console.error('❌ [JORNADAS] Error al confirmar reciclaje parcial:', error);
            const mensajeError = this.extraerMensajeError(error.message);
            Utils.showAlert(mensajeError, 'error');
        }
    },

    // Función para extraer el mensaje de error de la respuesta JSON
    extraerMensajeError(errorMessage, accion = 'realizar esta acción') {
        return Utils.mensajeErrorApi({ message: errorMessage }, accion);
    }

};

// Los atajos de teclado Ctrl+Z / Ctrl+Y están manejados globalmente por undo-manager.js
// No es necesario duplicar los listeners aquí