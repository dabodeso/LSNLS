// Módulo de gestión de preguntas
const PreguntasManager = {
    preguntas: [],
    filtros: {
        tematica: '',
        nivel: '',
        estado: ''
    },
    orden: {
        columna: null,
        asc: true
    },

    async cargarPreguntas() {
        try {
            if (!authManager.isAuthenticated()) {
                console.error('Usuario no autenticado');
                return;
            }

            const response = await fetch('/api/preguntas', {
                headers: authManager.getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error('Error al cargar las preguntas');
            }

            this.preguntas = await response.json();
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
        }
    },

    mostrarPreguntas() {
        const tbody = document.querySelector('#tabla-preguntas');
        if (!tbody) {
            console.error('No se encontró el elemento tabla-preguntas');
            return;
        }
        tbody.innerHTML = '';

        let preguntasFiltradas = this.preguntas;

        // Ordenar
        if (this.orden.columna) {
            preguntasFiltradas = preguntasFiltradas.slice().sort((a, b) => {
                let valA = a[this.orden.columna];
                let valB = b[this.orden.columna];
                if (typeof valA === 'string') valA = valA.toLowerCase();
                if (typeof valB === 'string') valB = valB.toLowerCase();
                if (valA === undefined || valA === null) return 1;
                if (valB === undefined || valB === null) return -1;
                if (valA < valB) return this.orden.asc ? -1 : 1;
                if (valA > valB) return this.orden.asc ? 1 : -1;
                return 0;
            });
        }

        preguntasFiltradas.forEach(pregunta => {
            const tr = document.createElement('tr');
            tr.setAttribute('data-id', pregunta.id);
            tr.innerHTML = `
                <td>${pregunta.id ?? ''}</td>
                <td style="background-color: #f8f9fa; font-style: italic;">${pregunta.creacionUsuarioNombre ?? ''}</td>
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
                <td><button class="btn btn-sm btn-danger" onclick="PreguntasManager.eliminarPregunta(${pregunta.id})"><i class="fas fa-trash"></i></button></td>
            `;
            tbody.appendChild(tr);
        });
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

            this.preguntas = await response.json();
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
        // Obtener subtemas seleccionados como array
        const subtemasSelect = document.getElementById('subtemas-pregunta');
        if (subtemasSelect) {
            const subtemas = Array.from(subtemasSelect.selectedOptions).map(opt => opt.value);
            preguntaData.subtema = subtemas.join(',');
        }
        // No enviar campo 'estado', el backend lo pone por defecto
        try {
            if (!authManager.isAuthenticated()) {
                throw new Error('Usuario no autenticado');
            }
            const response = await fetch('/api/preguntas', {
                method: 'POST',
                headers: authManager.getAuthHeaders(),
                body: JSON.stringify(preguntaData)
            });
            if (!response.ok) {
                throw new Error('Error al crear la pregunta');
            }
            await this.cargarPreguntas();
            $('#modal-pregunta').modal('hide');
            Toastify({
                text: "Pregunta creada exitosamente",
                duration: 3000,
                close: true,
                gravity: "top",
                position: "right",
                style: {
                    background: "linear-gradient(to right, #00b09b, #96c93d)",
                }
            }).showToast();
        } catch (error) {
            console.error('Error al crear pregunta:', error);
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

    aplicarFiltros() {
        this.mostrarPreguntas();
    },

    setOrden(columna) {
        if (this.orden.columna === columna) {
            this.orden.asc = !this.orden.asc;
        } else {
            this.orden.columna = columna;
            this.orden.asc = true;
        }
        this.mostrarPreguntas();
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
        return 'bg-light text-dark';
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
            ['borrador','para_verificar','verificada','revisar','corregir','rechazada','aprobada'].forEach(opt => {
                const option = document.createElement('option');
                option.value = opt;
                option.text = opt === 'para_verificar' ? 'Para verificar' : opt.charAt(0).toUpperCase() + opt.slice(1);
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
        // Construir el objeto de actualización
        const update = {};
        update[campo] = nuevoValor;
        // Siempre enviar los tres campos obligatorios (pregunta, respuesta, tematica)
        const preguntaActual = this.preguntas.find(p => p.id == id);
        if (preguntaActual) {
            if (campo !== "pregunta") update["pregunta"] = preguntaActual.pregunta ?? "";
            if (campo !== "respuesta") update["respuesta"] = preguntaActual.respuesta ?? "";
            if (campo !== "tematica") update["tematica"] = preguntaActual.tematica ?? "";
        }
        // Normalizar los campos obligatorios
        if (update["pregunta"]) {
            let p = update["pregunta"];
            p = p.replace(/[\r\n]+/g, ' ');
            p = p.replace(/\s+/g, ' ').trim();
            if (p.length > 150) p = p.substring(0, 150).trim();
            update["pregunta"] = p;
        }
        if (update["respuesta"]) {
            let r = update["respuesta"];
            r = r.replace(/[\r\n]+/g, ' ');
            r = r.replace(/\s+/g, ' ').trim();
            if (r.length > 50) r = r.substring(0, 50).trim();
            update["respuesta"] = r;
        }
        if (update["tematica"]) {
            let t = update["tematica"];
            t = t.replace(/[\r\n]+/g, ' ');
            t = t.replace(/\s+/g, ' ').trim();
            if (t.length > 100) t = t.substring(0, 100).trim();
            update["tematica"] = t;
        }
        try {
            if (campo === 'estado') {
                // Usar endpoint especial para cambio de estado
                const response = await fetch(`/api/preguntas/${id}/estado?nuevoEstado=${nuevoValor}`, {
                    method: 'PUT',
                    headers: authManager.getAuthHeaders()
                });
                if (!response.ok) {
                    let errorMsg = 'Error al actualizar estado';
                    try {
                        const data = await response.json();
                        if (data && data.message) errorMsg = data.message;
                        else if (typeof data === 'string') errorMsg = data;
                    } catch {}
                    throw new Error(errorMsg);
                }
            } else {
                const response = await fetch(`/api/preguntas/${id}`, {
                    method: 'PUT',
                    headers: authManager.getAuthHeaders(),
                    body: JSON.stringify(update)
                });
                if (!response.ok) {
                    let errorMsg = 'Error al actualizar';
                    try {
                        const data = await response.json();
                        if (data && data.message) errorMsg = data.message;
                        else if (typeof data === 'string') errorMsg = data;
                    } catch {}
                    throw new Error(errorMsg);
                }
            }
            await this.cargarPreguntas();
        } catch (e) {
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

    limpiarFiltros() {
        document.getElementById('filtro-estado').value = '';
        document.getElementById('filtro-nivel').value = '';
        document.getElementById('filtro-tematica').value = '';
        document.getElementById('filtro-subtema').value = '';
        document.getElementById('filtro-pregunta').value = '';
        document.getElementById('filtro-respuesta').value = '';
        this.cargarPreguntas();
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
    document.getElementById('buscar-pregunta')?.addEventListener('keyup', () => PreguntasManager.aplicarFiltros());

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
        // Rellenar select de temática con temas dinámicos
        const tematicas = TemasManager.temas.length > 0 ? TemasManager.temas : ['GEOGRAFÍA','HISTORIA','DEPORTES','CIENCIA','ARTE'];
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
        const subtemas = TemasManager.subtemas.length > 0 ? TemasManager.subtemas : ['GEOGRAFÍA','HISTORIA','DEPORTES','CIENCIA','ARTE'];
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