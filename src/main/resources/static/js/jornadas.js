// Gestión de Jornadas - LSNLS
const JornadasManager = {
    jornadas: [],
    cuestionariosDisponibles: [],
    combosDisponibles: [],
    jornadaEditando: null,
    cuestionariosSeleccionados: [],
    combosSeleccionados: [],

    async init() {
        console.log('🚀 [JORNADAS] Inicializando gestión de jornadas');
        try {
            await this.cargarDatos();
            this.mostrarJornadas();
            this.configurarEventos();
            console.log('✅ [JORNADAS] Inicialización completada');
        } catch (error) {
            console.error('❌ [JORNADAS] Error en inicialización:', error);
            Utils.showAlert('Error al cargar datos de jornadas', 'error');
        }
    },

    async cargarDatos() {
        console.log('📡 [JORNADAS] Cargando datos...');
        try {
            const [jornadasRes, cuestionariosRes, combosRes] = await Promise.all([
                apiManager.get('/api/jornadas'),
                apiManager.get('/api/jornadas/cuestionarios-disponibles'),
                apiManager.get('/api/jornadas/combos-disponibles')
            ]);

            this.jornadas = jornadasRes.datos || [];
            this.cuestionariosDisponibles = cuestionariosRes.datos || [];
            this.combosDisponibles = combosRes.datos || [];

            console.log(`✅ [JORNADAS] Datos cargados: ${this.jornadas.length} jornadas, ${this.cuestionariosDisponibles.length} cuestionarios, ${this.combosDisponibles.length} combos`);
        } catch (error) {
            console.error('❌ [JORNADAS] Error al cargar datos:', error);
            throw error;
        }
    },

    configurarEventos() {
        // Configurar eventos de los modales
        document.getElementById('buscarCuestionarios').addEventListener('input', (e) => {
            this.filtrarCuestionarios(e.target.value);
        });

        document.getElementById('buscarCombos').addEventListener('input', (e) => {
            this.filtrarCombos(e.target.value);
        });
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

        let html = '';
        this.jornadas.forEach(jornada => {
            html += this.generarCardJornada(jornada);
        });

        container.innerHTML = html;
    },

    generarCardJornada(jornada) {
        const estadoBadge = this.getEstadoBadge(jornada.estado);
        const fecha = jornada.fechaJornada ? new Date(jornada.fechaJornada).toLocaleDateString('es-ES') : 'Sin fecha';
        const cuestionariosCount = jornada.cuestionarios ? jornada.cuestionarios.length : 0;
        const combosCount = jornada.combos ? jornada.combos.length : 0;

        return `
            <div class="jornada-card" data-id="${jornada.id}">
                <div class="jornada-header">
                    <div>
                        <h5 class="mb-1">${jornada.nombre}</h5>
                        <small class="text-muted">Creada por: ${jornada.creacionUsuarioNombre}</small>
                    </div>
                    <div class="d-flex gap-2 align-items-center">
                        ${estadoBadge}
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
                
                <div class="row">
                    <div class="col-md-6">
                        <small class="text-muted"><i class="fas fa-calendar"></i> Fecha:</small> ${fecha}<br>
                        <small class="text-muted"><i class="fas fa-map-marker-alt"></i> Lugar:</small> ${jornada.lugar || 'No especificado'}<br>
                        <small class="text-muted"><i class="fas fa-clipboard-list"></i> Cuestionarios:</small> ${cuestionariosCount}/5<br>
                        <small class="text-muted"><i class="fas fa-layer-group"></i> Combos:</small> ${combosCount}/5
                    </div>
                    <div class="col-md-6">
                        ${jornada.notas ? `
                            <small class="text-muted"><i class="fas fa-sticky-note"></i> Notas:</small>
                            <p class="small mb-0">${jornada.notas}</p>
                        ` : ''}
                    </div>
                </div>

                ${this.puedeGestionarEstado(jornada) ? `
                    <div class="mt-3">
                        <small class="text-muted">Cambiar estado:</small>
                        <select class="form-select form-select-sm d-inline-block w-auto ms-2" onchange="JornadasManager.cambiarEstado(${jornada.id}, this.value)">
                            <option value="">-- Seleccionar --</option>
                            <option value="preparacion" ${jornada.estado === 'preparacion' ? 'selected' : ''}>Preparación</option>
                            <option value="lista" ${jornada.estado === 'lista' ? 'selected' : ''}>Lista</option>
                            <option value="en_grabacion" ${jornada.estado === 'en_grabacion' ? 'selected' : ''}>En Grabación</option>
                            <option value="completada" ${jornada.estado === 'completada' ? 'selected' : ''}>Completada</option>
                            <option value="archivada" ${jornada.estado === 'archivada' ? 'selected' : ''}>Archivada</option>
                        </select>
                    </div>
                ` : ''}
            </div>
        `;
    },

    getEstadoBadge(estado) {
        const badges = {
            'preparacion': 'badge bg-secondary',
            'lista': 'badge bg-info',
            'en_grabacion': 'badge bg-warning text-dark',
            'completada': 'badge bg-success',
            'archivada': 'badge bg-dark'
        };

        const nombres = {
            'preparacion': 'Preparación',
            'lista': 'Lista',
            'en_grabacion': 'En Grabación',
            'completada': 'Completada',
            'archivada': 'Archivada'
        };

        return `<span class="${badges[estado] || 'badge bg-secondary'}">${nombres[estado] || estado}</span>`;
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
        
        this.actualizarSlotsVisual();
        
        const modal = new bootstrap.Modal(document.getElementById('modalJornada'));
        modal.show();
    },

    async editarJornada(id) {
        try {
            const response = await apiManager.get(`/api/jornadas/${id}`);
            const jornada = response.datos;
            
            this.jornadaEditando = jornada;
            this.cuestionariosSeleccionados = jornada.cuestionarioIds || [];
            this.combosSeleccionados = jornada.comboIds || [];
            
            document.getElementById('modalJornadaTitulo').textContent = 'Editar Jornada';
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

            if (this.cuestionariosSeleccionados.length > 5) {
                Utils.showAlert('Máximo 5 cuestionarios por jornada', 'error');
                return;
            }

            if (this.combosSeleccionados.length > 5) {
                Utils.showAlert('Máximo 5 combos por jornada', 'error');
                return;
            }

            let response;
            if (this.jornadaEditando) {
                response = await apiManager.put(`/api/jornadas/${this.jornadaEditando.id}`, datos);
            } else {
                response = await apiManager.post('/api/jornadas', datos);
            }

            Utils.showAlert('Jornada guardada exitosamente', 'success');
            bootstrap.Modal.getInstance(document.getElementById('modalJornada')).hide();
            
            await this.cargarDatos();
            this.mostrarJornadas();
            
        } catch (error) {
            console.error('❌ [JORNADAS] Error al guardar:', error);
            Utils.showAlert('Error al guardar la jornada: ' + (error.message || 'Error desconocido'), 'error');
        }
    },

    async eliminarJornada(id) {
        if (!confirm('¿Estás seguro de que quieres eliminar esta jornada?')) {
            return;
        }

        try {
            await apiManager.delete(`/api/jornadas/${id}`);
            Utils.showAlert('Jornada eliminada exitosamente', 'success');
            
            await this.cargarDatos();
            this.mostrarJornadas();
            
        } catch (error) {
            console.error('❌ [JORNADAS] Error al eliminar:', error);
            Utils.showAlert('Error al eliminar la jornada: ' + (error.message || 'Error desconocido'), 'error');
        }
    },

    async cambiarEstado(id, nuevoEstado) {
        if (!nuevoEstado) return;

        try {
            await apiManager.put(`/api/jornadas/${id}/estado`, { estado: nuevoEstado });
            Utils.showAlert('Estado actualizado exitosamente', 'success');
            
            await this.cargarDatos();
            this.mostrarJornadas();
            
        } catch (error) {
            console.error('❌ [JORNADAS] Error al cambiar estado:', error);
            Utils.showAlert('Error al cambiar el estado: ' + (error.message || 'Error desconocido'), 'error');
        }
    },

    async exportarExcel(id) {
        try {
            console.log('📊 [JORNADAS] Exportando Excel para jornada:', id);
            
            const response = await fetch(`/api/jornadas/${id}/exportar-excel`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('token')}`
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
            Utils.showAlert('Error al exportar Excel: ' + (error.message || 'Error desconocido'), 'error');
        }
    },

    seleccionarCuestionarios() {
        this.mostrarCuestionariosDisponibles();
        const modal = new bootstrap.Modal(document.getElementById('modalSelectorCuestionarios'));
        modal.show();
    },

    seleccionarCombos() {
        this.mostrarCombosDisponibles();
        const modal = new bootstrap.Modal(document.getElementById('modalSelectorCombos'));
        modal.show();
    },

    mostrarCuestionariosDisponibles() {
        const container = document.getElementById('listaCuestionarios');
        let html = '';

        this.cuestionariosDisponibles.forEach(cuestionario => {
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

        this.combosDisponibles.forEach(combo => {
            const isSelected = this.combosSeleccionados.includes(combo.id);
            html += `
                <div class="list-group-item ${isSelected ? 'active' : ''}" 
                     onclick="JornadasManager.toggleCombo(${combo.id})"
                     data-id="${combo.id}">
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <h6 class="mb-1">Combo #${combo.id}</h6>
                            <p class="mb-1">Nivel: ${combo.nivel} | Estado: <span class="badge ${Utils.getEstadoBadgeClass(combo.estado, 'combo')}">${Utils.formatearEstadoCombo(combo.estado)}</span></p>
                            <small>Tipo: ${combo.tipo || 'No especificado'}</small>
                        </div>
                        <div class="d-flex align-items-center gap-2">
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

    toggleCuestionario(id) {
        const index = this.cuestionariosSeleccionados.indexOf(id);
        if (index > -1) {
            this.cuestionariosSeleccionados.splice(index, 1);
        } else {
            if (this.cuestionariosSeleccionados.length >= 5) {
                Utils.showAlert('Máximo 5 cuestionarios por jornada', 'error');
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
            if (this.combosSeleccionados.length >= 5) {
                Utils.showAlert('Máximo 5 combos por jornada', 'error');
                return;
            }
            this.combosSeleccionados.push(id);
        }
        this.mostrarCombosDisponibles();
    },

    confirmarSeleccionCuestionarios() {
        this.actualizarSlotsVisual();
        bootstrap.Modal.getInstance(document.getElementById('modalSelectorCuestionarios')).hide();
    },

    confirmarSeleccionCombos() {
        this.actualizarSlotsVisual();
        bootstrap.Modal.getInstance(document.getElementById('modalSelectorCombos')).hide();
    },

    actualizarSlotsVisual() {
        this.actualizarSlotsCuestionarios();
        this.actualizarSlotsCombos();
    },

    actualizarSlotsCuestionarios() {
        const container = document.getElementById('cuestionariosSeleccionados');
        let html = '';

        for (let i = 0; i < 5; i++) {
            const cuestionarioId = this.cuestionariosSeleccionados[i];
            if (cuestionarioId) {
                const cuestionario = this.cuestionariosDisponibles.find(c => c.id === cuestionarioId);
                if (cuestionario) {
                    html += `
                        <div class="item-slot item-filled">
                            <div>
                                <strong>Cuestionario #${cuestionario.id}</strong><br>
                                <small>${cuestionario.nivel}</small><br>
                                <small>${cuestionario.tematica || 'Sin temática'}</small>
                                <button class="btn btn-sm btn-outline-danger mt-1" 
                                        onclick="JornadasManager.quitarCuestionario(${cuestionarioId})">
                                    <i class="fas fa-times"></i>
                                </button>
                            </div>
                        </div>
                    `;
                }
            } else {
                html += `
                    <div class="item-slot">
                        <span class="text-muted">Slot ${i + 1} vacío</span>
                    </div>
                `;
            }
        }

        container.innerHTML = html;
    },

    actualizarSlotsCombos() {
        const container = document.getElementById('combosSeleccionados');
        let html = '';

        for (let i = 0; i < 5; i++) {
            const comboId = this.combosSeleccionados[i];
            if (comboId) {
                const combo = this.combosDisponibles.find(c => c.id === comboId);
                if (combo) {
                    html += `
                        <div class="item-slot item-filled">
                            <div>
                                <strong>Combo #${combo.id}</strong><br>
                                <small>${combo.nivel}</small><br>
                                <small>Tipo: ${combo.tipo || 'N/A'}</small>
                                <button class="btn btn-sm btn-outline-danger mt-1" 
                                        onclick="JornadasManager.quitarCombo(${comboId})">
                                    <i class="fas fa-times"></i>
                                </button>
                            </div>
                        </div>
                    `;
                }
            } else {
                html += `
                    <div class="item-slot">
                        <span class="text-muted">Slot ${i + 1} vacío</span>
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
        }
    },

    quitarCombo(id) {
        const index = this.combosSeleccionados.indexOf(id);
        if (index > -1) {
            this.combosSeleccionados.splice(index, 1);
            this.actualizarSlotsVisual();
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

    aplicarFiltros() {
        // Implementar filtros de fecha, estado, etc.
        console.log('🔍 [JORNADAS] Aplicando filtros...');
        // Esta funcionalidad se puede expandir según necesidades
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
                                        <div class="list-group-item">
                                            <strong>Cuestionario #${c.id}</strong> - ${c.nivel} - ${c.estado}
                                            ${c.tematica ? `<br><small>Temática: ${c.tematica}</small>` : ''}
                                        </div>
                                    `).join('') : '<p class="text-muted">No hay cuestionarios asignados</p>'}
                                </div>
                                
                                <h6 class="mt-4">Combos (${jornada.combos ? jornada.combos.length : 0})</h6>
                                <div class="list-group">
                                    ${jornada.combos ? jornada.combos.map(c => `
                                        <div class="list-group-item">
                                            <strong>Combo #${c.id}</strong> - ${c.nivel} - <span class="badge ${Utils.getEstadoBadgeClass(c.estado, 'combo')}">${Utils.formatearEstadoCombo(c.estado)}</span>
                                            ${c.tipo ? `<br><small>Tipo: ${c.tipo}</small>` : ''}
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
        
        console.log('=== DEBUG CUESTIONARIO ===');
        console.log('Cuestionario completo:', cuestionario);
        console.log('Preguntas:', cuestionario.preguntas);
        
        let html = '';
        if (cuestionario.preguntas && cuestionario.preguntas.length > 0) {
            // Crear una copia del array antes de ordenar
            const preguntasParaOrdenar = [...cuestionario.preguntas];
            
            // Ordenar las preguntas por factor de multiplicación
            const preguntasOrdenadas = preguntasParaOrdenar.sort((a, b) => {
                const factorA = parseInt(a.factorMultiplicacion) || 0;
                const factorB = parseInt(b.factorMultiplicacion) || 0;
                console.log(`Comparando factores: ${factorA} vs ${factorB}`);
                return factorA - factorB;
            });

            console.log('Preguntas ordenadas:', preguntasOrdenadas);

            preguntasOrdenadas.forEach((preguntaCuestionario, index) => {
                const pregunta = preguntaCuestionario.pregunta;
                if (!pregunta) {
                    console.log(`Pregunta ${index} es null o undefined`);
                    return;
                }
                
                // Usar factorMultiplicacion en lugar de pregunta.nivel
                const factor = parseInt(preguntaCuestionario.factorMultiplicacion) || 0;
                console.log(`Procesando pregunta ${index}: factor="${preguntaCuestionario.factorMultiplicacion}" -> parseado=${factor}`);
                
                // Determinar tipo LS/NLS y color del texto
                let tipoTexto = '';
                let colorTexto = 'text-secondary'; // gris por defecto
                
                if (factor === 1 || factor === 3) {
                    tipoTexto = `${factor}LS`;
                    colorTexto = 'text-success'; // texto verde para LS
                    console.log(`✅ Aplicando VERDE (LS) para factor ${factor}`);
                } else if (factor === 2 || factor === 4) {
                    tipoTexto = `${factor}NLS`;
                    colorTexto = 'text-danger'; // texto rojo para NLS
                    console.log(`❌ Aplicando ROJO (NLS) para factor ${factor}`);
                } else {
                    tipoTexto = `${factor}`;
                    console.log(`⚠️ Factor ${factor} no reconocido, usando gris`);
                }
                
                html += `
                    <tr>
                        <td><span class="badge bg-light ${colorTexto} fw-bold">${tipoTexto}</span></td>
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
        
        console.log('=== DEBUG COMBO ===');
        console.log('Combo completo:', combo);
        console.log('Preguntas:', combo.preguntas);
        
        let html = '';
        if (combo.preguntas && combo.preguntas.length > 0) {
            // Crear una copia del array antes de ordenar
            const preguntasParaOrdenar = [...combo.preguntas];
            
            // Ordenar las preguntas por factorMultiplicacion
            const preguntasOrdenadas = preguntasParaOrdenar.sort((a, b) => {
                if (a.pregunta && b.pregunta) {
                    const factorA = parseInt(a.factorMultiplicacion) || 0;
                    const factorB = parseInt(b.factorMultiplicacion) || 0;
                    console.log(`Combo - Comparando factores: ${factorA} vs ${factorB}`);
                    return factorA - factorB;
                }
                return 0;
            });

            console.log('Combo - Preguntas ordenadas:', preguntasOrdenadas);

            preguntasOrdenadas.forEach((preguntaSlot, index) => {
                // Manejar la estructura del ComboService donde cada slot tiene: {slot, pregunta, factorMultiplicacion}
                if (preguntaSlot.pregunta) {
                    const pregunta = preguntaSlot.pregunta;
                    let multiplicador = '';
                    
                    // Determinar el multiplicador según el slot
                    if (preguntaSlot.slot === 'PM1') {
                        multiplicador = 'X2';
                    } else if (preguntaSlot.slot === 'PM2') {
                        multiplicador = 'X3';
                    } else if (preguntaSlot.slot === 'PM3') {
                        multiplicador = 'Variable';
                    } else {
                        multiplicador = `Factor ${preguntaSlot.factorMultiplicacion || 'N/A'}`;
                    }
                    
                    // Usar factorMultiplicacion en lugar de pregunta.nivel
                    const factor = parseInt(preguntaSlot.factorMultiplicacion) || 0;
                    console.log(`Combo - Procesando pregunta ${index}: factor="${preguntaSlot.factorMultiplicacion}" -> parseado=${factor}`);
                    
                    // Determinar tipo LS/NLS y color del texto
                    let tipoTexto = '';
                    let colorTexto = 'text-secondary'; // gris por defecto
                    
                    if (factor === 1 || factor === 3) {
                        tipoTexto = `${factor}LS`;
                        colorTexto = 'text-success'; // texto verde para LS
                        console.log(`Combo - ✅ Aplicando VERDE (LS) para factor ${factor}`);
                    } else if (factor === 2 || factor === 4) {
                        tipoTexto = `${factor}NLS`;
                        colorTexto = 'text-danger'; // texto rojo para NLS
                        console.log(`Combo - ❌ Aplicando ROJO (NLS) para factor ${factor}`);
                    } else {
                        tipoTexto = `${factor}`;
                        console.log(`Combo - ⚠️ Factor ${factor} no reconocido, usando gris`);
                    }
                    
                    html += `
                        <tr>
                            <td><span class="badge bg-light ${colorTexto} fw-bold">${tipoTexto}</span></td>
                            <td>${pregunta.pregunta || 'Sin texto'}</td>
                            <td><strong>${pregunta.respuesta || 'Sin respuesta'}</strong></td>
                            <td><span class="badge bg-info">${multiplicador}</span></td>
                        </tr>
                    `;
                }
            });
        } else {
            html = '<tr><td colspan="4" class="text-center text-muted">No hay preguntas disponibles</td></tr>';
        }
        
        tbody.innerHTML = html;
        
        const modal = new bootstrap.Modal(document.getElementById('modalVerPreguntasCombo'));
        modal.show();
    }

}; 