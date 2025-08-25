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
        
        // Generar slots de cuestionarios (5 en total)
        let cuestionariosHtml = '';
        for (let i = 0; i < 5; i++) {
            if (i < cuestionarios.length) {
                const c = cuestionarios[i];
                cuestionariosHtml += `
                    <div class="cuestionario-slot">
                        <div class="d-flex justify-content-between align-items-center">
                            <span style="font-weight: bold; color: #0066cc;">Cuestionario ${c.id}</span>
                            <div class="btn-group btn-group-sm">
                                <button class="btn btn-outline-info" onclick="JornadasManager.verPreguntasCuestionario(${c.id})" title="Ver preguntas">
                                    <i class="fas fa-eye"></i>
                                </button>
                                <button class="btn btn-outline-secondary" onclick="JornadasManager.mostrarHistorialCuestionario(${c.id})" title="Ver historial">
                                    <i class="fas fa-history"></i>
                                </button>
                                <button class="btn btn-outline-success" onclick="JornadasManager.reutilizarCuestionario(${c.id}, ${jornada.id})" title="Reutilizar cuestionario">
                                    <i class="fas fa-recycle"></i>
                                </button>
                            </div>
                        </div>
                        <small>${c.tematica || 'Sin temática'}</small>
                    </div>
                `;
            } else {
                // Slot vacío con botón de añadir
                cuestionariosHtml += `
                    <div class="cuestionario-slot empty-slot">
                        <button class="btn btn-sm btn-success w-100" 
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
        
        // Generar slots de combos (5 en total)
        let combosHtml = '';
        for (let i = 0; i < 5; i++) {
            if (i < combos.length) {
                const c = combos[i];
                // Obtener el nombre completo del tipo o usar el valor original
                const tipoNombre = tipoComboNombres[c.tipo] || c.tipo || 'Sin tipo';
                
                combosHtml += `
                    <div class="combo-slot">
                        <div class="d-flex justify-content-between align-items-center">
                            <span style="font-weight: bold; color: #0066cc;">Combo ${c.id}</span>
                            <div class="btn-group btn-group-sm">
                                <button class="btn btn-outline-info" onclick="JornadasManager.verPreguntasCombo(${c.id})" title="Ver preguntas">
                                    <i class="fas fa-eye"></i>
                                </button>
                                <button class="btn btn-outline-secondary" onclick="JornadasManager.mostrarHistorialCombo(${c.id})" title="Ver historial">
                                    <i class="fas fa-history"></i>
                                </button>
                                <button class="btn btn-outline-success" onclick="JornadasManager.reutilizarCombo(${c.id}, ${jornada.id})" title="Reutilizar combo">
                                    <i class="fas fa-recycle"></i>
                                </button>
                            </div>
                        </div>
                        <small>${tipoNombre}</small>
                    </div>
                `;
            } else {
                // Slot vacío con botón de añadir
                combosHtml += `
                    <div class="combo-slot empty-slot">
                        <button class="btn btn-sm btn-success w-100" 
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
            <select class="form-select form-select-sm" style="width: auto; min-width: 120px" 
                    onchange="JornadasManager.cambiarEstado(${jornada.id}, this.value)">
                <option value="preparacion" ${jornada.estado === 'preparacion' ? 'selected' : ''}>Preparación</option>
                <option value="lista" ${jornada.estado === 'lista' ? 'selected' : ''}>Lista</option>
                <option value="en_grabacion" ${jornada.estado === 'en_grabacion' ? 'selected' : ''}>En Grabación</option>
                <option value="completada" ${jornada.estado === 'completada' ? 'selected' : ''}>Completada</option>
                <option value="archivada" ${jornada.estado === 'archivada' ? 'selected' : ''}>Archivada</option>
            </select>
        ` : estadoBadge;
        
        return `
            <div class="jornada-card" data-id="${jornada.id}">
                <div class="mb-3">
                    <table class="table">
                        <tr>
                            <td style="font-weight: bold; font-size: 1.1em; color: #0066cc; padding-right: 20px; width: 10%;">${jornada.id}</td>
                            <td style="width: 20%;">${jornada.nombre}</td>
                            <td style="width: 25%;">${selectorEstado}</td>
                            <td style="width: 15%;">${fecha}</td>
                            <td style="width: 15%;">${jornada.lugar || 'No especificado'}</td>
                            <td style="width: 15%;">
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
                            </td>
                        </tr>
                    </table>
                </div>
                
                <!-- Línea divisoria con texto "CUESTIONARIOS" -->
                <div class="combos-divider">
                    <span class="combos-text">CUESTIONARIOS</span>
                </div>
                
                <!-- Cuestionarios -->
                <div class="cuestionarios-container mt-3">
                    <div class="cuestionarios-grid">
                        ${cuestionariosHtml}
                    </div>
                </div>
                
                <!-- Línea divisoria con texto "COMBOS" -->
                <div class="combos-divider">
                    <span class="combos-text">COMBOS</span>
                </div>
                
                <!-- Combos -->
                <div class="combos-container mt-2">
                    <div class="combos-grid">
                        ${combosHtml}
                    </div>
                </div>
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
            
            // Manejar específicamente errores de autenticación
            if (error.message && error.message.includes('UNAUTHORIZED')) {
                Utils.showAlert('Sesión expirada. Por favor, inicia sesión nuevamente.', 'error');
                // Redirigir a login después de un breve delay
                setTimeout(() => {
                    window.location.href = 'login.html';
                }, 2000);
                return;
            }
            
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
            
            // Manejar específicamente errores de autenticación
            if (error.message && error.message.includes('UNAUTHORIZED')) {
                Utils.showAlert('Sesión expirada. Por favor, inicia sesión nuevamente.', 'error');
                setTimeout(() => {
                    window.location.href = 'login.html';
                }, 2000);
                return;
            }
            
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
            
            // Manejar específicamente errores de autenticación
            if (error.message && error.message.includes('UNAUTHORIZED')) {
                Utils.showAlert('Sesión expirada. Por favor, inicia sesión nuevamente.', 'error');
                setTimeout(() => {
                    window.location.href = 'login.html';
                }, 2000);
                return;
            }
            
            Utils.showAlert('Error al cambiar el estado: ' + (error.message || 'Error desconocido'), 'error');
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
            
            // Si estamos editando una jornada existente, guardar los cambios
            if (this.jornadaEditando) {
                this.guardarCambiosCuestionarios();
            }
        }
    },

    async guardarCambiosCuestionarios() {
        try {
            console.log('🔍 [JORNADAS] Guardando cambios de cuestionarios para jornada:', this.jornadaEditando.id);
            console.log('🔍 [JORNADAS] Cuestionarios seleccionados:', this.cuestionariosSeleccionados);
            console.log('🔍 [JORNADAS] Jornada editando:', this.jornadaEditando);
            
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

            await apiManager.put(`/api/jornadas/${this.jornadaEditando.id}`, datos);
            Utils.showAlert('Cuestionarios actualizados exitosamente', 'success');
            
            // Recargar datos para mostrar los cambios
            await this.cargarDatos();
            this.mostrarJornadas();
            
        } catch (error) {
            console.error('❌ [JORNADAS] Error al guardar cambios de cuestionarios:', error);
            Utils.showAlert('Error al actualizar cuestionarios: ' + (error.message || 'Error desconocido'), 'error');
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
            console.log('🔍 [JORNADAS] Guardando cambios de combos para jornada:', this.jornadaEditando.id);
            console.log('🔍 [JORNADAS] Combos seleccionados:', this.combosSeleccionados);
            console.log('🔍 [JORNADAS] Jornada editando:', this.jornadaEditando);
            
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

            await apiManager.put(`/api/jornadas/${this.jornadaEditando.id}`, datos);
            Utils.showAlert('Combos actualizados exitosamente', 'success');
            
            // Recargar datos para mostrar los cambios
            await this.cargarDatos();
            this.mostrarJornadas();
            
        } catch (error) {
            console.error('❌ [JORNADAS] Error al guardar cambios de combos:', error);
            Utils.showAlert('Error al actualizar combos: ' + (error.message || 'Error desconocido'), 'error');
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
                            <td><span class="badge bg-info">${factorStr}</span></td>
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
            await this.cargarDatos();
            this.mostrarJornadas();
            
        } catch (error) {
            console.error('Error al marcar elementos como no usados:', error);
            Utils.showAlert('Error al marcar elementos como no usados', 'error');
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
            await this.cargarDatos();
            this.mostrarJornadas();
            
        } catch (error) {
            console.error('Error al reaprovechar combo:', error);
            Utils.showAlert('Error al reaprovechar combo', 'error');
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
            Utils.showAlert('Error al reutilizar cuestionario', 'error');
        }
    },

    // Función para reutilizar un combo
    async reutilizarCombo(comboId, jornadaId) {
        try {
            console.log(`🔄 [JORNADAS] Reutilizando combo ${comboId} de jornada ${jornadaId}`);
            
            // Confirmar la acción
            const confirmacion = confirm(`¿Estás seguro de que quieres reutilizar el combo ${comboId}?\n\nEsto hará que:\n- El combo vuelva a estar disponible\n- Se actualice el historial\n- Se pueda usar en otras jornadas`);
            
            if (!confirmacion) {
                return;
            }

            // Llamar al endpoint para reutilizar el combo
            const response = await apiManager.post(`/api/jornadas/${jornadaId}/reutilizar-combo/${comboId}`);
            
            if (response.exito) {
                Utils.showAlert(`Combo ${comboId} reutilizado correctamente. Ahora está disponible para usar en otras jornadas.`, 'success');
                
                // Recargar datos y actualizar vista
                await this.cargarDatos();
                this.mostrarJornadas();
            } else {
                Utils.showAlert(`Error al reutilizar combo: ${response.mensaje}`, 'error');
            }
            
        } catch (error) {
            console.error('❌ [JORNADAS] Error al reutilizar combo:', error);
            Utils.showAlert('Error al reutilizar combo', 'error');
        }
    }

}; 