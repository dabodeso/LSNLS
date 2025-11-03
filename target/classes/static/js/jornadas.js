// Gestión de Jornadas - LSNLS
const JornadasManager = {
    jornadas: [],
    cuestionariosDisponibles: [],
    combosDisponibles: [],
    jornadaEditando: null,
    cuestionariosSeleccionados: [],
    combosSeleccionados: [],
    lastScrollY: 0,
    lastFocusJornadaId: null,
    reabrirEditarTrasSeleccion: false,
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
            console.log('✅ [JORNADAS] Inicialización completada');
        } catch (error) {
            console.error('❌ [JORNADAS] Error en inicialización:', error);
            Utils.showAlert('Error al cargar datos de jornadas', 'error');
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
    seleccionarCuestionariosDirecto(jornadaId) {
        this.jornadaEditando = this.jornadas.find(j => j.id === jornadaId);
        // Usar los mismos campos que editarJornada para consistencia
        this.cuestionariosSeleccionados = this.jornadaEditando.cuestionarioIds || [];
        this.seleccionarCuestionarios();
    },
    
    // Función para seleccionar combos directamente sin pasar por el editor
    seleccionarCombosDirecto(jornadaId) {
        this.jornadaEditando = this.jornadas.find(j => j.id === jornadaId);
        // Usar los mismos campos que editarJornada para consistencia
        this.combosSeleccionados = this.jornadaEditando.comboIds || [];
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
                sortDir: 'desc' // Ordenar por ID más alto primero
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

        // Nuevos filtros en modales (IDs actuales)
        const filtCuId = document.getElementById('filtroCuestId');
        const filtCuTem = document.getElementById('filtroCuestTematica');
        if (filtCuId) {
            filtCuId.addEventListener('keyup', () => this.mostrarCuestionariosDisponibles());
            filtCuId.addEventListener('change', () => this.mostrarCuestionariosDisponibles());
        }
        if (filtCuTem) {
            filtCuTem.addEventListener('change', () => this.mostrarCuestionariosDisponibles());
        }

        const filtCoId = document.getElementById('filtroComboId');
        const filtCoTipo = document.getElementById('filtroComboTipo');
        const filtCoTem = document.getElementById('filtroComboTematica');
        if (filtCoId) {
            filtCoId.addEventListener('keyup', () => this.mostrarCombosDisponibles());
            filtCoId.addEventListener('change', () => this.mostrarCombosDisponibles());
        }
        if (filtCoTipo) {
            filtCoTipo.addEventListener('change', () => this.mostrarCombosDisponibles());
        }
        if (filtCoTem) {
            filtCoTem.addEventListener('change', () => this.mostrarCombosDisponibles());
        }
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
        const infoPaginacion = document.getElementById('info-paginacion-jornadas');
        
        if (!paginacionContainer) {
            console.error('❌ [PAGINACION] Contenedor de paginación no encontrado');
            return;
        }

        // Actualizar información de paginación
        if (infoPaginacion) {
            const inicio = (this.paginaActual * this.tamanioPagina) + 1;
            const fin = Math.min((this.paginaActual + 1) * this.tamanioPagina, this.totalJornadas);
            infoPaginacion.textContent = `Mostrando ${inicio}-${fin} de ${this.totalJornadas} jornadas`;
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
        primeraPagina.innerHTML = `<a class="page-link" href="#" onclick="JornadasManager.irAPagina(0)">Primera</a>`;
        paginacionContainer.appendChild(primeraPagina);

        // Crear botón "Anterior"
        const paginaAnterior = document.createElement('li');
        paginaAnterior.className = `page-item ${this.paginaActual === 0 ? 'disabled' : ''}`;
        paginaAnterior.innerHTML = `<a class="page-link" href="#" onclick="JornadasManager.irAPagina(${this.paginaActual - 1})">Anterior</a>`;
        paginacionContainer.appendChild(paginaAnterior);

        // Calcular rango de páginas a mostrar
        const inicio = Math.max(0, this.paginaActual - 2);
        const fin = Math.min(this.totalPaginas - 1, this.paginaActual + 2);

        // Mostrar páginas en el rango
        for (let i = inicio; i <= fin; i++) {
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
    },

    generarCardJornada(jornada) {
        // Preparar los cuestionarios y combos (asegurar que existan arrays)
        const cuestionarios = jornada.cuestionarios || [];
        const combos = jornada.combos || [];
        
        // Generar slots de cuestionarios (6 en total)
        let cuestionariosHtml = '';
        for (let i = 0; i < 6; i++) {
            if (i < cuestionarios.length) {
                const c = cuestionarios[i];
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
                                ${esReutilizado ? `
                                    <button class="btn btn-outline-danger btn-sm" onclick="JornadasManager.quitarReutilizacionCuestionario(${c.id}, ${jornada.id})" title="Quitar reutilización">
                                        <i class="fas fa-undo"></i>
                                    </button>
                                ` : `
                                    <button class="btn btn-outline-success btn-sm" onclick="JornadasManager.reutilizarCuestionario(${c.id}, ${jornada.id})" title="Reutilizar cuestionario">
                                        <i class="fas fa-recycle"></i>
                                    </button>
                                `}
                            </div>
                        </div>
                        <small style="${esReutilizado ? 'color:#198754; font-weight:600;' : 'color:#6c757d;'}">${esReutilizado ? 'Reutilizado' : (c.tematica || 'Sin temática')}</small>
                    </div>
                `;
            } else {
                // Slot vacío con botón de añadir
                cuestionariosHtml += `
                    <div class="cuestionario-slot empty-slot p-2 border rounded" style="background-color: #f8f9fa; border-color: #e9ecef !important; border-style: dashed !important;">
                        <button class="btn btn-sm btn-outline-success w-100" 
                                onclick="JornadasManager.seleccionarCuestionariosDirecto(${jornada.id})">
                            <i class="fas fa-plus"></i> Añadir
                        </button>
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
            if (i < combos.length) {
                const c = combos[i];
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
                                ${esReutilizado ? `
                                    <button class="btn btn-outline-danger btn-sm" onclick="JornadasManager.quitarReutilizacionCombo(${c.id}, ${jornada.id})" title="Quitar reutilización">
                                        <i class="fas fa-undo"></i>
                                    </button>
                                ` : `
                                    <button class="btn btn-outline-success btn-sm" onclick="JornadasManager.reutilizarCombo(${c.id}, ${jornada.id})" title="Reutilizar combo">
                                        <i class="fas fa-recycle"></i>
                                    </button>
                                `}
                            </div>
                        </div>
                        <small style="${esReutilizado ? 'color:#198754; font-weight:600;' : 'color:#6c757d;'}">${esReutilizado ? 'Reutilizado' : tipoNombre}</small>
                    </div>
                `;
            } else {
                // Slot vacío con botón de añadir
                combosHtml += `
                    <div class="combo-slot empty-slot p-2 border rounded" style="background-color: #f8f9fa; border-color: #e9ecef !important; border-style: dashed !important;">
                        <button class="btn btn-sm btn-outline-success w-100" 
                                onclick="JornadasManager.seleccionarCombosDirecto(${jornada.id})">
                            <i class="fas fa-plus"></i> Añadir
                        </button>
                    </div>
                `;
            }
        }

        const estadoBadge = this.getEstadoBadge(jornada.estado);
        const fecha = jornada.fechaJornada ? new Date(jornada.fechaJornada).toLocaleDateString('es-ES') : 'Sin fecha';
        
        // Selector de estado (solo se muestra si el usuario puede gestionar el estado)
        const selectorEstado = this.puedeGestionarEstado(jornada) ? `
            <select class=\"form-select form-select-sm\" style=\"width: auto; min-width: 120px\" 
                    onchange=\"JornadasManager.cambiarEstado(${jornada.id}, this.value)\">\n\
                <option value=\"borrador\" ${jornada.estado === 'borrador' ? 'selected' : ''}>Borrador</option>\n\
                <option value=\"completa\" ${jornada.estado === 'completa' ? 'selected' : ''}>Completa</option>\n\
                <option value=\"grabada\" ${jornada.estado === 'grabada' ? 'selected' : ''}>Grabada</option>\n\
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
            </div>
        `;
    },

    getEstadoBadge(estado) {
        const badges = {
            'borrador': 'badge bg-secondary',
            'completa': 'badge bg-success',
            'grabada': 'badge bg-dark'
        };

        const nombres = {
            'borrador': 'Borrador',
            'completa': 'Completa',
            'grabada': 'Grabada'
        };

        return `<span class=\"${badges[estado] || 'badge bg-secondary'}\">${nombres[estado] || estado}</span>`;
    },

    puedeEditar(jornada) {
        return jornada.estado !== 'completada' && jornada.estado !== 'archivada';
    },

    puedeEliminar(jornada) {
        return jornada.estado !== 'en_grabacion';
    },

    puedeGestionarEstado(jornada) {
        // Asumiendo que solo ciertos roles pueden cambiar estado
        return true; // Implementar según roles del usuario
    },

    mostrarModalCrear() {
        this.jornadaEditando = null;
        this.cuestionariosSeleccionados = [];
        this.combosSeleccionados = [];
        
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
            const response = await apiManager.get(`/api/jornadas/${id}`);
            const jornada = response.datos;
            
            this.jornadaEditando = jornada;
            this.lastFocusJornadaId = id;
            this.cuestionariosSeleccionados = jornada.cuestionarioIds || [];
            this.combosSeleccionados = jornada.comboIds || [];
            
            document.getElementById('modalJornadaTitulo').textContent = 'Editar Jornada';
            document.getElementById('jornadaId').value = jornada.id;
            document.getElementById('jornadaNombre').value = jornada.nombre;
            document.getElementById('jornadaFecha').value = jornada.fechaJornada || '';
            document.getElementById('jornadaLugar').value = jornada.lugar || '';
            document.getElementById('jornadaNotas').value = jornada.notas || '';
            
            this.actualizarSlotsVisual();
            
            const modal = new bootstrap.Modal(document.getElementById('modalJornada'));
            modal.show();
            
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
        if (!confirm('¿Estás seguro de que quieres eliminar esta jornada?')) {
            return;
        }

        try {
            await apiManager.deleteUndoable(`/api/jornadas/${id}`, { label: `Eliminar jornada ${id}`, snapshotEndpoint: `/api/jornadas/${id}` });
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
            await apiManager.putUndoable(`/api/jornadas/${id}/estado`, { estado: nuevoEstado }, { label: `Estado jornada ${id}` , snapshotEndpoint: `/api/jornadas/${id}`});
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
            
            // Extraer mensaje de error de la respuesta JSON
            const mensajeError = this.extraerMensajeError(error.message);
            Utils.showAlert(mensajeError, 'error');
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

        // Listeners de filtros
        const inputId = document.getElementById('filtroCuestId');
        const selTem = document.getElementById('filtroCuestTematica');
        const trigger = () => this.mostrarCuestionariosDisponibles();
        if (inputId) {
            inputId.removeEventListener('keyup', inputId._h || (()=>{}));
            inputId._h = trigger;
            inputId.addEventListener('keyup', trigger);
            inputId.removeEventListener('change', inputId._hc || (()=>{}));
            inputId._hc = trigger;
            inputId.addEventListener('change', trigger);
        }
        if (selTem) {
            selTem.removeEventListener('change', selTem._h || (()=>{}));
            selTem._h = trigger;
            selTem.addEventListener('change', trigger);
        }

        this.mostrarCuestionariosDisponibles();
        const modal = new bootstrap.Modal(document.getElementById('modalSelectorCuestionarios'));
        modal.show();
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

        // Listeners de filtros
        const inputId = document.getElementById('filtroComboId');
        const selTipo = document.getElementById('filtroComboTipo');
        const selTem = document.getElementById('filtroComboTematica');
        const trigger = () => this.mostrarCombosDisponibles();
        if (inputId) {
            inputId.removeEventListener('keyup', inputId._h || (()=>{}));
            inputId._h = trigger;
            inputId.addEventListener('keyup', trigger);
            inputId.removeEventListener('change', inputId._hc || (()=>{}));
            inputId._hc = trigger;
            inputId.addEventListener('change', trigger);
        }
        if (selTipo) {
            selTipo.removeEventListener('change', selTipo._h || (()=>{}));
            selTipo._h = trigger;
            selTipo.addEventListener('change', trigger);
        }
        if (selTem) {
            selTem.removeEventListener('change', selTem._h || (()=>{}));
            selTem._h = trigger;
            selTem.addEventListener('change', trigger);
        }

        this.mostrarCombosDisponibles();
        const modal = new bootstrap.Modal(document.getElementById('modalSelectorCombos'));
        modal.show();
    },

    mostrarCuestionariosDisponibles() {
        const container = document.getElementById('listaCuestionarios');
        let html = '';

        const idFiltro = (document.getElementById('filtroCuestId')?.value || '').trim();
        const tematicaFiltro = document.getElementById('filtroCuestTematica')?.value || '';

        const lista = this.cuestionariosDisponibles.filter(cuestionario => {
            if (idFiltro && String(cuestionario.id) !== idFiltro) return false;
            if (tematicaFiltro && cuestionario.tematica !== tematicaFiltro) return false;
            return true;
        });

        lista.forEach(cuestionario => {
            const isSelected = this.cuestionariosSeleccionados.includes(cuestionario.id);
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
    },

    mostrarCombosDisponibles() {
        const container = document.getElementById('listaCombos');
        let html = '';

        // Leer filtros
        const idFiltro = (document.getElementById('filtroComboId')?.value || '').trim();
        const tipoFiltro = document.getElementById('filtroComboTipo')?.value || '';
        const tematicaFiltro = document.getElementById('filtroComboTematica')?.value || '';

        const lista = this.combosDisponibles.filter(combo => {
            if (idFiltro && String(combo.id) !== idFiltro) return false;
            if (tipoFiltro && combo.tipo !== tipoFiltro) return false;
            if (tematicaFiltro && combo.tematica !== tematicaFiltro) return false;
            return true;
        });

        lista.forEach(combo => {
            const isSelected = this.combosSeleccionados.includes(combo.id);
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
    },

    seleccionarCuestionarioDirecto(id) {
        if (!this.cuestionariosSeleccionados.includes(id)) {
            if (this.cuestionariosSeleccionados.length >= 6) {
                Utils.showAlert('Máximo 6 cuestionarios por jornada', 'error');
                return;
            }
            this.cuestionariosSeleccionados.push(id);
        }
        this.actualizarSlotsVisual();
        bootstrap.Modal.getInstance(document.getElementById('modalSelectorCuestionarios')).hide();
        if (this.jornadaEditando) {
            this.guardarCambiosCuestionarios();
        } else if (this.reabrirEditarTrasSeleccion) {
            // Si venimos de edición, reabrir el modal aunque sea nueva
            const jid = document.getElementById('jornadaId')?.value;
            if (jid && jid !== 'Auto') {
                this.editarJornada(Number(jid));
            } else {
                const modal = new bootstrap.Modal(document.getElementById('modalJornada'));
                modal.show();
                this.reabrirEditarTrasSeleccion = false;
            }
        }
    },

    seleccionarComboDirecto(id) {
        if (!this.combosSeleccionados.includes(id)) {
            if (this.combosSeleccionados.length >= 6) {
                Utils.showAlert('Máximo 6 combos por jornada', 'error');
                return;
            }
            this.combosSeleccionados.push(id);
        }
        this.actualizarSlotsVisual();
        bootstrap.Modal.getInstance(document.getElementById('modalSelectorCombos')).hide();
        if (this.jornadaEditando) {
            this.guardarCambiosCombos?.();
        } else if (this.reabrirEditarTrasSeleccion) {
            const jid = document.getElementById('jornadaId')?.value;
            if (jid && jid !== 'Auto') {
                this.editarJornada(Number(jid));
            } else {
                const modal = new bootstrap.Modal(document.getElementById('modalJornada'));
                modal.show();
                this.reabrirEditarTrasSeleccion = false;
            }
        }
    },

    toggleCuestionario(id) {
        const index = this.cuestionariosSeleccionados.indexOf(id);
        if (index > -1) {
            this.cuestionariosSeleccionados.splice(index, 1);
        } else {
            if (this.cuestionariosSeleccionados.length >= 6) {
                Utils.showAlert('Máximo 6 cuestionarios por jornada', 'error');
                return;
            }
            this.cuestionariosSeleccionados.push(id);
        }
        this.mostrarCuestionariosDisponibles();
    },

    toggleCombo(id) {
        const index = this.combosSeleccionados.indexOf(id);
        if (index > -1) {
            this.combosSeleccionados.splice(index, 1);
        } else {
            if (this.combosSeleccionados.length >= 6) {
                Utils.showAlert('Máximo 6 combos por jornada', 'error');
                return;
            }
            this.combosSeleccionados.push(id);
        }
        this.mostrarCombosDisponibles();
    },

    confirmarSeleccionCuestionarios() {
        this.actualizarSlotsVisual();
        bootstrap.Modal.getInstance(document.getElementById('modalSelectorCuestionarios')).hide();
        
        // Si estamos editando una jornada existente, guardar los cambios
        if (this.jornadaEditando) {
            this.guardarCambiosCuestionarios();
        }
    },

    confirmarSeleccionCombos() {
        this.actualizarSlotsVisual();
        bootstrap.Modal.getInstance(document.getElementById('modalSelectorCombos')).hide();
        
        // Si estamos editando una jornada existente, guardar los cambios
        if (this.jornadaEditando) {
            this.guardarCambiosCombos();
        }
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
                        <button type="button" class="btn btn-sm btn-outline-primary" onclick="JornadasManager.seleccionarCuestionarios()">
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
                        <button type="button" class="btn btn-sm btn-outline-primary" onclick="JornadasManager.seleccionarCombos()">
                            <i class="fas fa-plus"></i> Añadir combo
                        </button>
                    </div>
                `;
            }
        }

        container.innerHTML = html;
    },

    quitarCuestionario(id) {
        const index = this.cuestionariosSeleccionados.indexOf(id);
        if (index > -1) {
            this.cuestionariosSeleccionados.splice(index, 1);
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
            const prevLista = (this.jornadaEditando.cuestionarioIds || []).slice();
            const nuevaLista = (this.cuestionariosSeleccionados || []).slice();

            // Incluir todos los campos de la jornada actual para evitar que se pierdan datos
            const datos = {
                nombre: this.jornadaEditando.nombre,
                fechaJornada: this.jornadaEditando.fechaJornada,
                lugar: this.jornadaEditando.lugar,
                notas: this.jornadaEditando.notas,
                cuestionarioIds: this.cuestionariosSeleccionados,
                comboIds: this.jornadaEditando.comboIds || []
            };
            
            console.log('🔍 [JORNADAS] Datos a enviar:', datos);
            await apiManager.put(`/api/jornadas/${jid}`, datos);
            Utils.showAlert('Cuestionarios actualizados exitosamente', 'success');
            
            // Registrar acción de deshacer/rehacer para cuestionarios
            if (window.UndoManager) {
                const hacer = async () => {
                    const d = {
                        nombre: this.jornadaEditando.nombre,
                        fechaJornada: this.jornadaEditando.fechaJornada,
                        lugar: this.jornadaEditando.lugar,
                        notas: this.jornadaEditando.notas,
                        cuestionarioIds: nuevaLista,
                        comboIds: this.jornadaEditando.comboIds || []
                    };
                    await apiManager.put(`/api/jornadas/${jid}`, d);
                    await this.cargarDatos(true);
                    this.mostrarJornadas();
                };
                const deshacer = async () => {
                    const d = {
                        nombre: this.jornadaEditando.nombre,
                        fechaJornada: this.jornadaEditando.fechaJornada,
                        lugar: this.jornadaEditando.lugar,
                        notas: this.jornadaEditando.notas,
                        cuestionarioIds: prevLista,
                        comboIds: this.jornadaEditando.comboIds || []
                    };
                    await apiManager.put(`/api/jornadas/${jid}`, d);
                    await this.cargarDatos(true);
                    this.mostrarJornadas();
                };
                window.UndoManager.record({ do: hacer, undo: deshacer, label: `Jornada ${jid}: cuestionarios ${prevLista.join(',')}→${nuevaLista.join(',')}` });
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
        const index = this.combosSeleccionados.indexOf(id);
        if (index > -1) {
            this.combosSeleccionados.splice(index, 1);
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
            const prevLista = (this.jornadaEditando.comboIds || []).slice();
            const nuevaLista = (this.combosSeleccionados || []).slice();

            // Incluir todos los campos de la jornada actual para evitar que se pierdan datos
            const datos = {
                nombre: this.jornadaEditando.nombre,
                fechaJornada: this.jornadaEditando.fechaJornada,
                lugar: this.jornadaEditando.lugar,
                notas: this.jornadaEditando.notas,
                cuestionarioIds: this.jornadaEditando.cuestionarioIds || [],
                comboIds: this.combosSeleccionados
            };
            
            console.log('🔍 [JORNADAS] Datos a enviar:', datos);
            await apiManager.put(`/api/jornadas/${jid}`, datos);
            Utils.showAlert('Combos actualizados exitosamente', 'success');
            
            // Registrar acción de deshacer/rehacer para combos
            if (window.UndoManager) {
                const hacer = async () => {
                    const d = {
                        nombre: this.jornadaEditando.nombre,
                        fechaJornada: this.jornadaEditando.fechaJornada,
                        lugar: this.jornadaEditando.lugar,
                        notas: this.jornadaEditando.notas,
                        cuestionarioIds: this.jornadaEditando.cuestionarioIds || [],
                        comboIds: nuevaLista
                    };
                    await apiManager.put(`/api/jornadas/${jid}`, d);
                    await this.cargarDatos(true);
                    this.mostrarJornadas();
                };
                const deshacer = async () => {
                    const d = {
                        nombre: this.jornadaEditando.nombre,
                        fechaJornada: this.jornadaEditando.fechaJornada,
                        lugar: this.jornadaEditando.lugar,
                        notas: this.jornadaEditando.notas,
                        cuestionarioIds: this.jornadaEditando.cuestionarioIds || [],
                        comboIds: prevLista
                    };
                    await apiManager.put(`/api/jornadas/${jid}`, d);
                    await this.cargarDatos(true);
                    this.mostrarJornadas();
                };
                window.UndoManager.record({ do: hacer, undo: deshacer, label: `Jornada ${jid}: combos ${prevLista.join(',')}→${nuevaLista.join(',')}` });
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
                                        <p><strong>Estado:</strong> ${this.getEstadoBadge(jornada.estado)}</p>
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
                                                <strong>Combo #${c.id}</strong> - ${c.nivel} - <span class="badge ${Utils.getEstadoBadgeClass(c.estado, 'combo')}">${Utils.formatearEstadoCombo(c.estado)}</span>
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
        const modal = new bootstrap.Modal(document.getElementById('modalVerPreguntasCuestionario'));
        modal.show();
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
        const modal = new bootstrap.Modal(document.getElementById('modalVerPreguntasCombo'));
        modal.show();
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
            
            const response = await apiManager.post('/api/historial-jornadas/reaprovechar-combo', data);
            
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
            const response = await apiManager.post(`/api/jornadas/${jornadaId}/reutilizar-cuestionario/${cuestionarioId}`);
            
            if (response.exito) {
                Utils.showAlert(`Cuestionario ${cuestionarioId} reutilizado correctamente. Ahora está disponible para usar en otras jornadas.`, 'success');
                
                // Recargar datos y actualizar vista
                await this.cargarDatos();
                this.mostrarJornadas();
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
            const resp = await apiManager.post(`/api/jornadas/${jornadaId}/quitar-reutilizacion-cuestionario/${cuestionarioId}`);
            if (resp.exito) {
                Utils.showAlert('Reutilización de cuestionario quitada', 'success');
                await this.cargarDatos(true);
                this.mostrarJornadas();
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

    // Función para reutilizar un combo (ahora abre el modal de reciclaje)
    async reutilizarCombo(comboId, jornadaId) {
        try {
            console.log(`🔄 [JORNADAS] Abriendo modal de reciclaje para combo ${comboId} de jornada ${jornadaId}`);
            
            // Guardar información del combo actual
            this.comboReciclajeActual = comboId;
            this.jornadaReciclajeActual = jornadaId;
            this.preguntaSeleccionada = null;
            // Reset UI del modal para evitar arrastrar estado previo
            const modalEl = document.getElementById('modalReciclajeCombo');
            const paso1 = document.getElementById('pasoReciclaje');
            const paso2 = document.getElementById('pasoSeleccionPregunta');
            const btnConfirmar = document.getElementById('btnConfirmarReciclaje');
            const cont = document.getElementById('preguntasCombo');
            if (cont) cont.innerHTML = '';
            if (paso1) paso1.style.display = 'block';
            if (paso2) paso2.style.display = 'none';
            if (btnConfirmar) btnConfirmar.style.display = 'none';
            if (modalEl) {
                modalEl.querySelectorAll('.pregunta-card').forEach(card => {
                    card.classList.remove('border-primary', 'border-3');
                });
            }
            
            // Mostrar el modal de reciclaje
            const modal = new bootstrap.Modal(document.getElementById('modalReciclajeCombo'));
            modal.show();
            
        } catch (error) {
            console.error('❌ [JORNADAS] Error al abrir modal de reciclaje:', error);
            Utils.showAlert('Error al abrir modal de reciclaje', 'error');
        }
    },
    async quitarReutilizacionCombo(comboId, jornadaId) {
        try {
            const resp = await apiManager.post(`/api/jornadas/${jornadaId}/quitar-reutilizacion-combo/${comboId}`);
            if (resp.exito) {
                Utils.showAlert('Reutilización de combo quitada', 'success');
                await this.cargarDatos(true);
                this.mostrarJornadas();
            } else {
                Utils.showAlert(resp.mensaje || 'No se pudo quitar la reutilización', 'error');
            }
        } catch (error) {
            const msg = this.extraerMensajeError(error.message);
            Utils.showAlert(msg, 'error');
        }
    },

    // Reciclar combo entero (marcar como liberado)
    async reciclarComboEntero() {
        try {
            console.log(`🔄 [JORNADAS] Reciclando combo entero ${this.comboReciclajeActual} de jornada ${this.jornadaReciclajeActual}`);
            
            // Confirmar la acción
            const confirmacion = confirm(`¿Estás seguro de que quieres reciclar el combo ${this.comboReciclajeActual} completo?\n\nEsto marcará el combo como liberado.`);
            
            if (!confirmacion) {
                return;
            }

            // Llamar al endpoint para reciclar el combo entero
            const response = await apiManager.post(`/api/jornadas/${this.jornadaReciclajeActual}/reciclar-combo-entero/${this.comboReciclajeActual}`);
            
            if (response.exito) {
                Utils.showAlert(`Combo ${this.comboReciclajeActual} reciclado completamente. Marcado como liberado.`, 'success');
                
                // Cerrar modal y recargar datos
                bootstrap.Modal.getInstance(document.getElementById('modalReciclajeCombo')).hide();
                await this.cargarDatos();
                this.mostrarJornadas();
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
                    Utils.showAlert('El combo debe tener exactamente 3 preguntas para reciclaje parcial', 'error');
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
        // Ocultar paso 1 y mostrar paso 2
        document.getElementById('pasoReciclaje').style.display = 'none';
        document.getElementById('pasoSeleccionPregunta').style.display = 'block';
        document.getElementById('btnConfirmarReciclaje').style.display = 'block';
        
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
        
        try {
            console.log(`🔄 [JORNADAS] Confirmando reciclaje parcial del combo ${this.comboReciclajeActual} con pregunta usada ${this.preguntaSeleccionada}`);
            
            // Llamar al endpoint para reciclar el combo parcialmente
            const response = await apiManager.post(`/api/jornadas/${this.jornadaReciclajeActual}/reciclar-combo-parcial/${this.comboReciclajeActual}`, {
                preguntaUsadaId: this.preguntaSeleccionada
            });
            
            if (response.exito) {
                Utils.showAlert(`Combo reciclado parcialmente. Se creó un nuevo combo con las 2 preguntas restantes.`, 'success');
                
                // Cerrar modal y recargar datos
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
    extraerMensajeError(errorMessage) {
        try {
            // Si el mensaje contiene un JSON, intentar parsearlo
            if (errorMessage && errorMessage.includes('{')) {
                // Buscar el JSON en el mensaje (después del código de estado)
                const jsonMatch = errorMessage.match(/\{.*\}/);
                if (jsonMatch) {
                    const jsonStr = jsonMatch[0];
                    const errorObj = JSON.parse(jsonStr);
                    
                    // Si tiene un campo 'mensaje', usarlo
                    if (errorObj.mensaje) {
                        return errorObj.mensaje;
                    }
                    
                    // Si tiene un campo 'message', usarlo
                    if (errorObj.message) {
                        return errorObj.message;
                    }
                }
            }
            
            // Si no se puede extraer el mensaje del JSON, devolver el mensaje original
            return errorMessage || 'Error desconocido';
        } catch (parseError) {
            console.warn('No se pudo parsear el mensaje de error:', parseError);
            return errorMessage || 'Error desconocido';
        }
    }

}; 