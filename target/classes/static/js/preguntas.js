// Módulo de gestión de preguntas
const PreguntasManager = {
    preguntas: [],
    paginaActual: 0,
    tamanioPagina: 25,
    totalPreguntas: 0,
    totalPaginas: 0,
    cargando: false,
    filtros: {
        tematica: '',
        nivel: '',
        estado: ''
    },
    orden: {
        columna: null,
        asc: true
    },

    async cargarPreguntas(resetear = true) {
        try {
            console.log('🔄 [CARGAR] Iniciando carga de preguntas, resetear:', resetear);
            console.log('🔄 [CARGAR] Estado actual - paginaActual:', this.paginaActual, 'preguntas.length:', this.preguntas.length);
            
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

    async cargarMasPreguntas() {
        console.log('🔄 [CARGAR MÁS] Iniciando carga de más preguntas...');
        console.log('🔄 [CARGAR MÁS] Estado actual - cargando:', this.cargando, 'paginaActual:', this.paginaActual, 'totalPaginas:', this.totalPaginas);
        
        if (this.cargando) {
            console.log('❌ [CARGAR MÁS] Ya está cargando, abortando...');
            return;
        }
        
        if (this.paginaActual >= this.totalPaginas - 1) {
            console.log('❌ [CARGAR MÁS] Ya estamos en la última página, abortando...');
            return;
        }
        
        // Guardar la posición actual del scroll
        const scrollPosition = window.scrollY;
        console.log('📍 [CARGAR MÁS] Posición del scroll guardada:', scrollPosition);
        
        this.paginaActual++;
        console.log('✅ [CARGAR MÁS] Página incrementada a:', this.paginaActual);
        await this.cargarPreguntas(false);
        
        // Restaurar la posición del scroll después de cargar las preguntas
        setTimeout(() => {
            window.scrollTo(0, scrollPosition);
            console.log('📍 [CARGAR MÁS] Posición del scroll restaurada:', scrollPosition);
        }, 100);
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
        console.log('🔄 [PAGINACION] Actualizando paginación...');
        console.log('🔄 [PAGINACION] Estado - preguntas.length:', this.preguntas.length, 'totalPreguntas:', this.totalPreguntas, 'paginaActual:', this.paginaActual, 'totalPaginas:', this.totalPaginas);
        
        let paginacionContainer = document.getElementById('paginacion-preguntas');
        if (!paginacionContainer) {
            console.log('🔄 [PAGINACION] Contenedor no encontrado, creando...');
            // Crear el contenedor si no existe
            const tablaContainer = document.querySelector('.table-responsive');
            if (tablaContainer) {
                console.log('✅ [PAGINACION] Tabla encontrada, creando contenedor...');
                const paginacionDiv = document.createElement('div');
                paginacionDiv.id = 'paginacion-preguntas';
                paginacionDiv.className = 'mt-3 d-flex justify-content-between align-items-center';
                tablaContainer.parentNode.insertBefore(paginacionDiv, tablaContainer.nextSibling);
                // Obtener la referencia al contenedor recién creado
                paginacionContainer = document.getElementById('paginacion-preguntas');
                console.log('✅ [PAGINACION] Contenedor creado:', paginacionContainer);
            } else {
                console.error('❌ [PAGINACION] No se encontró la tabla .table-responsive');
            }
        } else {
            console.log('✅ [PAGINACION] Contenedor existente encontrado');
        }

        if (paginacionContainer) {
            const infoPagina = document.createElement('div');
            infoPagina.innerHTML = `Mostrando ${this.preguntas.length} de ${this.totalPreguntas} preguntas (Página ${this.paginaActual + 1} de ${this.totalPaginas})`;

            // Crear el botón con un enfoque más robusto
            const botonCargarMas = document.createElement('button');
            botonCargarMas.className = 'btn btn-primary';
            botonCargarMas.innerHTML = '<i class="fas fa-plus"></i> Cargar más preguntas';
            botonCargarMas.type = 'button';
            botonCargarMas.id = 'btn-cargar-mas-preguntas';
            
            // Estilos inline para asegurar que sea clickeable
            Object.assign(botonCargarMas.style, {
                cursor: 'pointer',
                pointerEvents: 'auto',
                position: 'relative',
                zIndex: '1000',
                display: 'inline-block',
                userSelect: 'none',
                WebkitUserSelect: 'none',
                MozUserSelect: 'none',
                msUserSelect: 'none'
            });
            
            // Verificar si el botón debe estar deshabilitado
            const botonDeshabilitado = this.cargando || this.paginaActual >= this.totalPaginas - 1;
            
            if (botonDeshabilitado) {
                botonCargarMas.disabled = true;
                botonCargarMas.style.opacity = '0.6';
                botonCargarMas.style.cursor = 'not-allowed';
            } else {
                botonCargarMas.disabled = false;
                botonCargarMas.style.opacity = '1';
                botonCargarMas.style.cursor = 'pointer';
                
                // Añadir event listener solo si el botón está habilitado
                botonCargarMas.addEventListener('click', (e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    console.log('🔄 [BOTON] Botón "Cargar más" clickeado');
                    this.cargarMasPreguntas();
                });
                
                // Añadir eventos adicionales para debug
                botonCargarMas.addEventListener('mouseenter', () => {
                    console.log('🔄 [BOTON] Mouse enter en botón');
                });
                
                botonCargarMas.addEventListener('mouseleave', () => {
                    console.log('🔄 [BOTON] Mouse leave en botón');
                });
                
                botonCargarMas.addEventListener('mousedown', () => {
                    console.log('🔄 [BOTON] Mouse down en botón');
                });
                
                botonCargarMas.addEventListener('mouseup', () => {
                    console.log('🔄 [BOTON] Mouse up en botón');
                });
            }
            
            console.log('✅ [PAGINACION] Botón creado, deshabilitado:', botonDeshabilitado);
            console.log('✅ [PAGINACION] Estado del botón - cargando:', this.cargando, 'paginaActual >= totalPaginas-1:', this.paginaActual >= this.totalPaginas - 1);
            console.log('✅ [PAGINACION] Total páginas:', this.totalPaginas, 'Página actual:', this.paginaActual);

            paginacionContainer.innerHTML = '';
            paginacionContainer.appendChild(infoPagina);
            paginacionContainer.appendChild(botonCargarMas);
            
            console.log('✅ [PAGINACION] Paginación actualizada correctamente');
            console.log('✅ [PAGINACION] Botón añadido al DOM:', botonCargarMas);
            console.log('✅ [PAGINACION] Botón visible:', botonCargarMas.offsetParent !== null);
            console.log('✅ [PAGINACION] Botón habilitado:', !botonCargarMas.disabled);
            console.log('✅ [PAGINACION] Botón clickeable:', botonCargarMas.style.pointerEvents !== 'none');
            
            // Debug adicional: verificar elementos superpuestos
            const rect = botonCargarMas.getBoundingClientRect();
            const elementosEnPosicion = document.elementsFromPoint(
                rect.left + rect.width / 2, 
                rect.top + rect.height / 2
            );
            console.log('🔍 [PAGINACION] Elementos en la posición del botón:', elementosEnPosicion);
            console.log('🔍 [PAGINACION] ¿El botón está en la parte superior?', elementosEnPosicion[0] === botonCargarMas);
            

        } else {
            console.error('❌ [PAGINACION] No se pudo crear o encontrar el contenedor de paginación');
        }
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
                <td ondblclick="PreguntasManager.editarCelda(${pregunta.id}, 'nivel', this)"><span class="${this.getNivelColor(pregunta.nivel)}">${pregunta.nivel ?? ''}</span></td>
                <td ondblclick="PreguntasManager.editarCelda(${pregunta.id}, 'tematica', this)">${pregunta.tematica ?? ''}</td>
                <td ondblclick="PreguntasManager.editarCelda(${pregunta.id}, 'subtema', this)">${(pregunta.subtema ?? '').split(',').map(s => s.trim()).filter(Boolean).join(', ')}</td>
                <td ondblclick="PreguntasManager.editarCelda(${pregunta.id}, 'pregunta', this)" style="white-space:pre-line; word-break:break-word; max-width:300px;">${pregunta.pregunta ?? ''}</td>
                <td ondblclick="PreguntasManager.editarCelda(${pregunta.id}, 'respuesta', this)">${pregunta.respuesta ?? ''}</td>
                <td ondblclick="PreguntasManager.editarCelda(${pregunta.id}, 'datosExtra', this)">${pregunta.datosExtra ?? ''}</td>
                <td ondblclick="PreguntasManager.editarCelda(${pregunta.id}, 'fuentes', this)">${pregunta.fuentes ?? ''}</td>
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
        
        // Si estamos añadiendo más preguntas, restaurar la posición del scroll
        if (isAddingMore) {
            setTimeout(() => {
                window.scrollTo(0, scrollPosition);
                console.log('📍 [MOSTRAR] Posición del scroll restaurada:', scrollPosition);
            }, 50);
        }
    },

    async filtrarPreguntas() {
        try {
            // Obtener valores de todos los filtros
            const estado = document.getElementById('filtro-estado')?.value || '';
            const nivel = document.getElementById('filtro-nivel')?.value || '';
            const tematica = document.getElementById('filtro-tematica')?.value || '';
            const subtema = document.getElementById('filtro-subtema')?.value || '';
            const pregunta = document.getElementById('filtro-pregunta')?.value || '';
            const respuesta = document.getElementById('filtro-respuesta')?.value || '';

            // Si no hay filtros, cargar todas las preguntas
            const hayFiltros = estado || nivel || tematica || subtema || pregunta || respuesta;
            
            if (!hayFiltros) {
                await this.cargarPreguntas();
                return;
            }

            // Construir parámetros de consulta
            const params = new URLSearchParams();
            if (estado) params.append('estado', estado);
            if (nivel) params.append('nivel', nivel);
            if (tematica) params.append('tematica', tematica);
            if (subtema) params.append('subtema', subtema);
            if (pregunta) params.append('pregunta', pregunta);
            if (respuesta) params.append('respuesta', respuesta);

            // Llamar al endpoint de filtrado del backend
            const response = await fetch(`/api/preguntas/filtrar?${params.toString()}`, {
                headers: authManager.getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error('Error al filtrar preguntas');
            }

            const responseData = await response.json();
            
            // El backend devuelve una respuesta paginada, extraer el contenido
            if (responseData.content && Array.isArray(responseData.content)) {
                this.preguntas = responseData.content;
                this.totalPreguntas = responseData.totalElements || responseData.content.length;
                this.totalPaginas = responseData.totalPages || 1;
                this.paginaActual = 0; // Resetear a la primera página
            } else {
                // Si no es paginado, usar directamente
                this.preguntas = Array.isArray(responseData) ? responseData : [];
                this.totalPreguntas = this.preguntas.length;
                this.totalPaginas = 1;
                this.paginaActual = 0;
            }
            
            this.mostrarPreguntas();

        } catch (error) {
            console.error('Error al filtrar preguntas:', error);
            // En caso de error, usar filtro client-side como fallback
            this.filtrarPreguntasClientSide();
        }
    },

    filtrarPreguntasClientSide() {
        const estado = document.getElementById('filtro-estado')?.value || '';
        const nivel = document.getElementById('filtro-nivel')?.value || '';
        const tematica = document.getElementById('filtro-tematica')?.value.toLowerCase() || '';
        const subtema = document.getElementById('filtro-subtema')?.value.toLowerCase() || '';
        const pregunta = document.getElementById('filtro-pregunta')?.value.toLowerCase() || '';
        const respuesta = document.getElementById('filtro-respuesta')?.value.toLowerCase() || '';
        
        const preguntasFiltradas = this.preguntas.filter(p => {
            const coincideEstado = !estado || p.estado === estado;
            const coincideNivel = !nivel || p.nivel === nivel;
            const coincideTematica = !tematica || (p.tematica && p.tematica.toLowerCase().includes(tematica));
            const coincideSubtema = !subtema || (p.subtema && p.subtema.toLowerCase().includes(subtema));
            const coincidePregunta = !pregunta || (p.pregunta && p.pregunta.toLowerCase().includes(pregunta));
            const coincideRespuesta = !respuesta || (p.respuesta && p.respuesta.toLowerCase().includes(respuesta));
            
            return coincideEstado && coincideNivel && coincideTematica && 
                   coincideSubtema && coincidePregunta && coincideRespuesta;
        });
        
        // Actualizar preguntas filtradas y mostrar
        this.preguntas = preguntasFiltradas;
        this.mostrarPreguntas();
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
        
        // Obtener subtemas seleccionados como array
        const subtemasSelect = document.getElementById('subtemas-pregunta');
        if (subtemasSelect) {
            const subtemas = Array.from(subtemasSelect.selectedOptions).map(opt => opt.value);
            preguntaData.subtema = subtemas.join(',');
            console.log('💾 [GUARDAR] Subtemas procesados:', subtemas, '→', preguntaData.subtema);
        }
        
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
            }
            
            console.log('📥 [GUARDAR] Respuesta del servidor:', response.status, response.statusText);
            
            // Recargar las preguntas para reflejar los cambios
            await this.cargarPreguntas();
            $('#modal-pregunta').modal('hide');
            
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
        try {
            if (!authManager.isAuthenticated()) {
                console.error('Usuario no autenticado');
                return;
            }

            // Resetear paginación al aplicar filtros
            this.paginaActual = 0;
            this.preguntas = [];
            this.cargando = true;
            this.mostrarEstadoCarga();

            // Obtener valores de los filtros
            const tematica = document.getElementById('filtro-tematica')?.value || '';
            const nivel = document.getElementById('filtro-nivel')?.value || '';
            const estado = document.getElementById('filtro-estado')?.value || '';
            const pregunta = document.getElementById('buscar-pregunta')?.value || '';

            const params = new URLSearchParams({
                page: this.paginaActual,
                size: this.tamanioPagina
            });

            // Añadir filtros solo si tienen valor
            if (tematica) params.append('tematica', tematica);
            if (nivel) params.append('nivel', nivel);
            if (estado) params.append('estado', estado);
            if (pregunta) params.append('pregunta', pregunta);

            const response = await fetch(`/api/preguntas/filtrar?${params}`, {
                headers: authManager.getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error('Error al filtrar las preguntas');
            }

            const data = await response.json();
            this.preguntas = data.content;
            this.totalPreguntas = data.totalElements;
            this.totalPaginas = data.totalPages;
            this.paginaActual = data.number;

            this.mostrarPreguntas();
            this.actualizarPaginacion();
        } catch (error) {
            console.error('Error al filtrar preguntas:', error);
            Toastify({
                text: `Error al filtrar: ${error.message}`,
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
        }
    },

    async setOrden(columna) {
        if (this.orden.columna === columna) {
            this.orden.asc = !this.orden.asc;
        } else {
            this.orden.columna = columna;
            this.orden.asc = true;
        }
        
        // Recargar preguntas con el nuevo orden desde el servidor
        this.paginaActual = 0;
        await this.cargarPreguntas(true);
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
                estados.push('para_verificar', 'rechazada');
                break;
                
            case 'verificada':
                estados.push('corregir', 'rechazada', 'aprobada');
                break;
                
            case 'corregir':
                estados.push('para_aprobar', 'para_verificar');
                break;
                
            case 'para_aprobar':
                estados.push('aprobada', 'corregir', 'rechazada');
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
        
        // Si el usuario es admin, permitir todos los estados
        const usuario = JSON.parse(localStorage.getItem('usuario'));
        if (usuario && usuario.rol === 'ROLE_ADMIN') {
            return ['borrador', 'para_verificar', 'verificada', 'revisar', 'corregir', 
                   'rechazada', 'aprobada', 'para_aprobar', 'usada'];
        }
        
        return estados;
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
                option.text = opt === '_0' ? 'Sin nivel (0)' : opt;
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
                option.text = opt === 'para_verificar' ? 'Para verificar' : 
                             opt === 'para_aprobar' ? 'Para aprobar' : 
                             opt.charAt(0).toUpperCase() + opt.slice(1);
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
                    
                    // Recargar la tabla completa para asegurar consistencia
                    await this.cargarPreguntas();
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
            
            console.log('✅ [FRONTEND] Campo actualizado, recargando tabla...');
            // CAMBIO: Usar cargarPreguntas() en lugar de aplicarFiltros() para forzar recarga completa
            await this.cargarPreguntas();
            
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

    async editarPregunta(id) {
        try {
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
            
            // Rellenar select de temática con temas dinámicos
            const tematicas = TemasManager.temas.length > 0 ? TemasManager.temas : ['Geografía','Historia','Deportes','Ciencia','Arte'];
            const selectTematica = document.getElementById('tematica-pregunta');
            if (selectTematica) {
                console.log('🎯 [EDITAR] Llenando select de temática...');
                selectTematica.innerHTML = '';
                
                // Añadir opción vacía por defecto
                const optionVacia = document.createElement('option');
                optionVacia.value = '';
                optionVacia.textContent = 'Seleccionar temática';
                selectTematica.appendChild(optionVacia);
                
                // Añadir todas las temáticas
                tematicas.forEach(t => {
                    const opt = document.createElement('option');
                    opt.value = t;
                    opt.textContent = t;
                    selectTematica.appendChild(opt);
                });
                
                console.log('📝 [EDITAR] Opciones creadas. Intentando seleccionar:', pregunta.tematica);
                
                // DESPUÉS de añadir todas las opciones, seleccionar la correcta
                if (pregunta.tematica) {
                    selectTematica.value = pregunta.tematica;
                    console.log('✅ [EDITAR] Valor asignado. Select.value ahora es:', selectTematica.value);
                    console.log('🔍 [EDITAR] ¿Coincide?', selectTematica.value === pregunta.tematica);
                } else {
                    console.log('⚠️ [EDITAR] pregunta.tematica está vacía o es null');
                }
            } else {
                console.error('❌ [EDITAR] No se encontró el elemento tematica-pregunta');
            }
            
            // Rellenar select de subtemas con subtemas dinámicos
            const subtemas = TemasManager.subtemas.length > 0 ? TemasManager.subtemas : ['Geografía','Historia','Deportes','Ciencia','Arte'];
            const selectSubtemas = document.getElementById('subtemas-pregunta');
            if (selectSubtemas) {
                console.log('🎯 [EDITAR] Llenando select de subtemas...');
                selectSubtemas.innerHTML = '';
                
                // Obtener subtemas seleccionados de la pregunta
                const subtemasSeleccionados = pregunta.subtema ? 
                    pregunta.subtema.split(',').map(s => s.trim()) : [];
                
                console.log('📝 [EDITAR] Subtemas a seleccionar:', subtemasSeleccionados);
                
                subtemas.forEach(t => {
                    const opt = document.createElement('option');
                    opt.value = t;
                    opt.textContent = t;
                    selectSubtemas.appendChild(opt);
                });
                
                // DESPUÉS de añadir todas las opciones, seleccionar las correctas
                if (subtemasSeleccionados.length > 0) {
                    Array.from(selectSubtemas.options).forEach(option => {
                        option.selected = subtemasSeleccionados.includes(option.value);
                    });
                    console.log('✅ [EDITAR] Subtemas seleccionados');
                }
            }
            
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
                    option.text = estado === 'para_verificar' ? 'Para verificar' : 
                                 estado === 'para_aprobar' ? 'Para aprobar' : 
                                 estado.charAt(0).toUpperCase() + estado.slice(1);
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
            $('#modal-pregunta').modal('show');
            
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
            await this.cargarPreguntas();
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
                        <td colspan="6" class="text-center">No se encontraron apariciones</td>
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
                    <td>
                        <button class="btn btn-sm btn-primary" onclick="PreguntasManager.editarPregunta(${pregunta.id}); $('#modal-apariciones').modal('hide');">
                            <i class="fas fa-edit"></i>
                        </button>
                    </td>
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
        document.getElementById('filtro-pregunta').value = '';
        document.getElementById('filtro-respuesta').value = '';
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
                        <td colspan="6" class="text-center">No se encontraron apariciones</td>
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
                    <td>
                        <button class="btn btn-sm btn-primary" onclick="PreguntasManager.editarPregunta(${pregunta.id}); $('#modal-apariciones').modal('hide');">
                            <i class="fas fa-edit"></i>
                        </button>
                    </td>
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
        ths[h.idx]?.addEventListener('click', () => PreguntasManager.setOrden(h.id));
        ths[h.idx]?.classList.add('sortable');
        ths[h.idx]?.setAttribute('style', 'cursor:pointer');
    });

    // Filtros
    document.getElementById('filtro-estado')?.addEventListener('change', () => PreguntasManager.aplicarFiltros());
    document.getElementById('filtro-nivel')?.addEventListener('change', () => PreguntasManager.aplicarFiltros());
    
    // Debounce para el campo de búsqueda para evitar múltiples llamadas
    let searchTimeout;
    document.getElementById('buscar-pregunta')?.addEventListener('input', () => {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            PreguntasManager.aplicarFiltros();
        }, 500); // Esperar 500ms después de que el usuario deje de escribir
    });

    // Event listener para el formulario de crear pregunta (si existe)
    document.querySelector('#formCrearPregunta')?.addEventListener('submit', (e) => PreguntasManager.crearPregunta(e));

    // --- NUEVO: Resaltar y hacer scroll a la pregunta si hay id en la URL ---
    const params = new URLSearchParams(window.location.search);
    const idDestacado = params.get('id');
    if (idDestacado) {
        setTimeout(() => {
            const fila = document.querySelector(`#tabla-preguntas tr[data-id='${idDestacado}']`);
            if (fila) {
                fila.classList.add('table-warning');
                fila.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        }, 500);
    }

    // --- AUTO-SCROLL HORIZONTAL EN TABLA DE PREGUNTAS ---
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

window.mostrarFormularioPregunta = function() {
    // Si hay un modal de Bootstrap para crear pregunta, mostrarlo
    const modal = document.getElementById('modal-pregunta');
    if (modal && typeof $ !== 'undefined') {
        // Resetear título para nueva pregunta
        document.getElementById('modal-pregunta-titulo').textContent = 'Nueva Pregunta';
        
        // Limpiar formulario
        const form = document.getElementById('formCrearPregunta');
        form.reset();
        delete form.dataset.editId;
        
        // Rellenar select de temática con temas dinámicos
        const tematicas = TemasManager.temas.length > 0 ? TemasManager.temas : ['Geografía','Historia','Deportes','Ciencia','Arte'];
        const selectTematica = document.getElementById('tematica-pregunta');
        if (selectTematica) {
            selectTematica.innerHTML = '';
            tematicas.forEach(t => {
                const opt = document.createElement('option');
                opt.value = t;
                opt.textContent = t;
                selectTematica.appendChild(opt);
            });
        }
        // Rellenar select de subtemas con subtemas dinámicos
        const subtemas = TemasManager.subtemas.length > 0 ? TemasManager.subtemas : ['Geografía','Historia','Deportes','Ciencia','Arte'];
        const selectSubtemas = document.getElementById('subtemas-pregunta');
        if (selectSubtemas) {
            selectSubtemas.innerHTML = '';
            subtemas.forEach(t => {
                const opt = document.createElement('option');
                opt.value = t;
                opt.textContent = t;
                selectSubtemas.appendChild(opt);
            });
        }
        
        // Inicializar el select de estado con "borrador" por defecto
        const selectEstado = document.getElementById('estado-pregunta');
        if (selectEstado) {
            selectEstado.innerHTML = '';
            // Para una nueva pregunta, solo permitir "borrador" y "para_verificar"
            const estadosPermitidos = ['borrador', 'para_verificar'];
            estadosPermitidos.forEach(estado => {
                const opt = document.createElement('option');
                opt.value = estado;
                opt.text = estado === 'para_verificar' ? 'Para verificar' : 
                          estado.charAt(0).toUpperCase() + estado.slice(1);
                if (estado === 'borrador') {
                    opt.selected = true;
                }
                selectEstado.appendChild(opt);
            });
        }
        
        $(modal).modal('show');
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
}); 