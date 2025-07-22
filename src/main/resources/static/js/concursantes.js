// Variables globales
let concursantes = [];
let programas = [];
let concursanteActual = null;

// Funciones de inicialización
async function inicializarConcursantes() {
    await cargarProgramas();
    await cargarConcursantes();
    setupEventListeners();
}

// Carga de datos
async function cargarConcursantes() {
    try {
        concursantes = await apiManager.get('/api/concursantes');
        mostrarConcursantes();
    } catch (error) {
        if (error && error.message && error.message.startsWith('401')) {
            // No mostrar mensaje, la redirección ya ocurre en api.js
            return;
        }
        mostrarError('Error al cargar concursantes: ' + error.message);
    }
}

async function cargarProgramas() {
    try {
        programas = await apiManager.get('/api/programas');
        actualizarSelectProgramas();
    } catch (error) {
        mostrarError('Error al cargar programas: ' + error.message);
    }
}

// Funciones de UI
function setupEventListeners() {
    document.getElementById('filtro-estado-concursante').addEventListener('change', filtrarConcursantes);
    document.getElementById('filtro-programa').addEventListener('change', filtrarConcursantes);
    document.getElementById('buscar-concursante').addEventListener('keyup', filtrarConcursantes);
}

function actualizarSelectProgramas() {
    const selectPrograma = document.getElementById('programa-id');
    const selectFiltro = document.getElementById('filtro-programa');
    
    const options = programas.map(programa => 
        `<option value="${programa.id}">Programa ${programa.id} - ${programa.fechaEmision || 'Sin fecha'}</option>`
    );
    
    // Solo actualizar si el elemento existe (para evitar errores en diferentes páginas)
    if (selectPrograma) {
        selectPrograma.innerHTML = '<option value="">Seleccione un programa...</option>' + options.join('');
    }
    if (selectFiltro) {
        selectFiltro.innerHTML = '<option value="">Todos</option>' + options.join('');
    }
}

function mostrarConcursantes(concursantesFiltrados = null) {
    const lista = concursantesFiltrados || concursantes;
    const tbody = document.getElementById('tabla-concursantes');
    tbody.innerHTML = lista.map(concursante => `
        <tr data-id="${concursante.id}">
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'numeroConcursante', this)">${concursante.numeroConcursante || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'jornada', this)">${concursante.jornada || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'diaGrabacion', this)">${formatearFecha(concursante.diaGrabacion)}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'lugar', this)">${concursante.lugar || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'nombre', this)">${concursante.nombre || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'edad', this)">${concursante.edad || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'ocupacion', this)">${concursante.ocupacion || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'redesSociales', this)">${concursante.redesSociales || ''}</td>
            <td onclick="abrirSelectorCuestionarioParaConcursante(${concursante.id})" style="cursor: pointer; background-color: #f8f9fa;" title="Click para seleccionar cuestionario">
                ${concursante.cuestionarioId ? `<span class="badge bg-primary">${concursante.cuestionarioId}</span>` : '<em class="text-muted">Sin asignar</em>'}
            </td>
            <td onclick="abrirSelectorComboParaConcursante(${concursante.id})" style="cursor: pointer; background-color: #f8f9fa;" title="Click para seleccionar combo">
                ${concursante.comboId ? `<span class="badge bg-warning">${concursante.comboId}</span>` : '<em class="text-muted">Sin asignar</em>'}
            </td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'factorX', this)">${concursante.factorX || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'resultado', this)">${concursante.resultado || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'notasGrabacion', this)">${concursante.notasGrabacion || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'guionista', this)">${concursante.guionista || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'valoracionGuionista', this)">${concursante.valoracionGuionista || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'concursantesPorJornada', this)">${concursante.concursantesPorJornada || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'estado', this)">${concursante.estado || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'momentosDestacados', this)">${concursante.momentosDestacados || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'duracion', this)">${concursante.duracion || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'valoracionFinal', this)">${concursante.valoracionFinal || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'numeroPrograma', this)">${concursante.numeroPrograma || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'ordenEscaleta', this)">${concursante.ordenEscaleta || ''}</td>
            <td>
                <button class="btn btn-sm btn-primary" onclick="editarConcursante(${concursante.id})">
                    <i class="fas fa-edit"></i>
                </button>
                <button class="btn btn-sm btn-danger" onclick="eliminarConcursante(${concursante.id})">
                    <i class="fas fa-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
    // Resaltado y scroll si hay id en la URL
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
}

function filtrarConcursantes() {
    const estado = document.getElementById('filtro-estado-concursante').value;
    const programaId = document.getElementById('filtro-programa').value;
    const busqueda = document.getElementById('buscar-concursante').value.toLowerCase();
    
    const filtrados = concursantes.filter(concursante => {
        const cumpleEstado = !estado || concursante.estado === estado;
        const cumplePrograma = !programaId || (concursante.programa && concursante.programa.id.toString() === programaId);
        const cumpleBusqueda = !busqueda || 
            concursante.nombre.toLowerCase().includes(busqueda) ||
            concursante.numeroConcursante.toLowerCase().includes(busqueda);
        
        return cumpleEstado && cumplePrograma && cumpleBusqueda;
    });
    
    mostrarConcursantes(filtrados);
}

function mostrarFormularioConcursante() {
    concursanteActual = null;
    document.getElementById('modal-concursante-titulo').textContent = 'Nuevo Concursante';
    document.getElementById('form-concursante').reset();
    document.getElementById('concursante-id').value = '';
    document.getElementById('cuestionario-id').value = '';
    document.getElementById('combo-id').value = '';
    // El número de concursante se genera automáticamente
    document.getElementById('numero-concursante').value = '';
    document.getElementById('numero-concursante').placeholder = 'Se generará automáticamente';
    const modal = new bootstrap.Modal(document.getElementById('modal-concursante'));
    modal.show();
}

async function editarConcursante(id) {
    try {
        concursanteActual = await apiManager.get(`/api/concursantes/${id}`);
        document.getElementById('modal-concursante-titulo').textContent = 'Editar Concursante';
        const form = document.getElementById('form-concursante');
        form.reset();
        document.getElementById('concursante-id').value = concursanteActual.id;
        // Mostrar el número de concursante existente en modo edición
        document.getElementById('numero-concursante').value = concursanteActual.numeroConcursante || '';
        document.getElementById('numero-concursante').placeholder = concursanteActual.numeroConcursante ? 
            'Número asignado: ' + concursanteActual.numeroConcursante : 'Sin número asignado';
        document.getElementById('jornada').value = concursanteActual.jornada || '';
        document.getElementById('dia-grabacion').value = concursanteActual.diaGrabacion || '';
        document.getElementById('lugar-concursante').value = concursanteActual.lugar || '';
        document.getElementById('nombre-concursante').value = concursanteActual.nombre || '';
        document.getElementById('edad-concursante').value = concursanteActual.edad || '';
        document.getElementById('ocupacion').value = concursanteActual.ocupacion || '';
        document.getElementById('redes-sociales').value = concursanteActual.redesSociales || '';
        document.getElementById('cuestionario-id').value = concursanteActual.cuestionario ? concursanteActual.cuestionario.id : '';
        document.getElementById('combo-id').value = concursanteActual.combo ? concursanteActual.combo.id : '';
        document.getElementById('factor-x').value = concursanteActual.factorX || '';
        document.getElementById('resultado').value = concursanteActual.resultado || '';
        document.getElementById('notas-grabacion').value = concursanteActual.notasGrabacion || '';
        document.getElementById('guionista').value = concursanteActual.guionista || '';
        document.getElementById('valoracion-guionista').value = concursanteActual.valoracionGuionista || '';
        document.getElementById('concursantes-por-jornada').value = concursanteActual.concursantesPorJornada || '';
        document.getElementById('momentos-destacados').value = concursanteActual.momentosDestacados || '';
        document.getElementById('duracion').value = concursanteActual.duracion || '';
        document.getElementById('valoracion-final').value = concursanteActual.valoracionFinal || '';
        document.getElementById('numero-programa').value = concursanteActual.numeroPrograma || '';
        document.getElementById('orden-escaleta').value = concursanteActual.ordenEscaleta || '';
        const modal = new bootstrap.Modal(document.getElementById('modal-concursante'));
        modal.show();
    } catch (error) {
        mostrarError('Error al cargar concursante: ' + error.message);
    }
}

// Guardar concursante con gestión de errores mejorada
async function guardarConcursante() {
    const form = document.getElementById('form-concursante');
    // Recoge todos los campos del formulario
    const datosConcursante = {
        id: document.getElementById('concursante-id').value || null,
        // Solo incluir numeroConcursante si estamos editando (id existe)
        numeroConcursante: document.getElementById('concursante-id').value ? 
            (document.getElementById('numero-concursante').value || null) : null,
        jornada: document.getElementById('jornada').value || null,
        diaGrabacion: document.getElementById('dia-grabacion').value || null,
        lugar: document.getElementById('lugar-concursante').value || null,
        nombre: document.getElementById('nombre-concursante').value,
        edad: document.getElementById('edad-concursante').value || null,
        ocupacion: document.getElementById('ocupacion').value || null,
        redesSociales: document.getElementById('redes-sociales').value || null,
        cuestionarioId: document.getElementById('cuestionario-id').value || null,
        comboId: document.getElementById('combo-id').value || null,
        factorX: document.getElementById('factor-x').value || null,
        resultado: document.getElementById('resultado').value || null,
        notasGrabacion: document.getElementById('notas-grabacion').value || null,
        guionista: document.getElementById('guionista').value || null,
        valoracionGuionista: document.getElementById('valoracion-guionista').value || null,
        concursantesPorJornada: document.getElementById('concursantes-por-jornada').value || null,
        momentosDestacados: document.getElementById('momentos-destacados').value || null,
        duracion: document.getElementById('duracion').value || null,
        valoracionFinal: document.getElementById('valoracion-final').value || null,
        numeroPrograma: document.getElementById('numero-programa').value || null,
        ordenEscaleta: document.getElementById('orden-escaleta').value || null,
        estado: null // El estado se gestiona aparte
    };

    // Enviar como JSON usando apiManager o fetch
    try {
        const token = localStorage.getItem('token');
        const response = await fetch('/api/concursantes', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': token ? (token.startsWith('Bearer ') ? token : 'Bearer ' + token) : ''
            },
            body: JSON.stringify(datosConcursante)
        });
        if (response.ok) {
            mostrarExito('Concursante guardado correctamente');
            $('#modal-concursante').modal('hide');
            await cargarConcursantes();
        } else {
            let mensaje = 'Error desconocido al guardar concursante.';
            let errorText = '';
            try {
                errorText = await response.text();
                const errorJson = JSON.parse(errorText);
                if (errorJson && errorJson.message) mensaje = errorJson.message;
                else if (errorJson && errorJson.error) mensaje = errorJson.error;
            } catch (e) {
                mensaje = errorText || mensaje;
            }
            if (response.status === 415) {
                mensaje = 'El servidor no acepta el formato de datos enviado. Contacta con el administrador.';
            } else if (response.status === 400) {
                mensaje = mensaje || 'Datos inválidos.';
            } else if (response.status === 401) {
                mensaje = 'No tienes permisos para realizar esta acción.';
            } else if (response.status === 500) {
                mensaje = 'Error interno del servidor.';
            }
            mostrarError('Error al guardar concursante: ' + mensaje);
        }
    } catch (err) {
        mostrarError('Error de red o inesperado: ' + err);
    }
}

// Manejar cambio de estado desde el select en la tabla
$(document).on('change', '.estado-select', async function() {
    const id = $(this).data('id');
    const nuevoEstado = $(this).val();
    // Lógica para actualizar el estado en el backend
    await fetch(`/api/concursantes/${id}/estado`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ estado: nuevoEstado })
    });
    // Recargar o actualizar la fila si es necesario
});

async function eliminarConcursante(id) {
    if (!confirm('¿Está seguro de que desea eliminar este concursante?')) {
        return;
    }

    try {
        await apiManager.delete(`/api/concursantes/${id}`);
        await cargarConcursantes();
        mostrarExito('Concursante eliminado correctamente');
    } catch (error) {
        mostrarError('Error al eliminar concursante: ' + error.message);
    }
}

async function editarCeldaConcursante(id, campo, td) {
    if (td.querySelector('input,select')) return;
    const valorOriginal = td.innerText;
    let input;
    
    // Todos los campos usan input de texto (incluyendo estado)
    {
        // Input normal para otros campos
        input = document.createElement('input');
        input.type = 'text';
        input.value = valorOriginal;
        input.className = 'form-control form-control-sm';
        
        // Configurar input según el tipo de campo
        if (campo === 'duracion') {
            input.placeholder = 'MM:SS (ej: 25:08)';
            input.type = 'text';
            input.pattern = '\\d{1,3}:\\d{2}';
        } else if (['numeroConcursante', 'edad', 'concursantesPorJornada', 'numeroPrograma', 'ordenEscaleta'].includes(campo)) {
            input.placeholder = 'Ingrese un número';
            input.type = 'number';
        }
    }
    
    td.innerHTML = '';
    td.appendChild(input);
    input.focus();
    
    input.addEventListener('blur', async function() {
        await guardarCeldaConcursante(id, campo, input, td, valorOriginal);
    });
    
    input.addEventListener('keydown', async function(e) {
        if (e.key === 'Enter') {
            await guardarCeldaConcursante(id, campo, input, td, valorOriginal);
        } else if (e.key === 'Escape') {
            td.innerHTML = valorOriginal;
        }
    });
    

}

async function guardarCeldaConcursante(id, campo, input, td, valorOriginal) {
    const nuevoValor = input.value.trim();
    if (nuevoValor === valorOriginal) {
        td.innerHTML = valorOriginal;
        return;
    }
    
    try {
        const concursante = concursantes.find(c => c.id === id);
        if (!concursante) return;
        
        // Validar y convertir el valor según el tipo de campo
        let valorConvertido = nuevoValor;
        
        // Campo duracion - validar formato MM:SS
        if (campo === 'duracion') {
            if (nuevoValor === '' || nuevoValor === null) {
                valorConvertido = null;
            } else {
                // Validar formato MM:SS
                const formatoValido = /^\d{1,3}:\d{2}$/.test(nuevoValor);
                if (!formatoValido) {
                    throw new Error('La duración debe tener formato MM:SS (ej: 25:08)');
                }
                // Validar que los segundos sean válidos (00-59)
                const [minutos, segundos] = nuevoValor.split(':');
                if (parseInt(segundos) > 59) {
                    throw new Error('Los segundos deben estar entre 00 y 59');
                }
                valorConvertido = nuevoValor; // Mantener como string
            }
        }
        // Campos numéricos enteros (excluyendo duracion)
        else if (['numeroConcursante', 'edad', 'concursantesPorJornada', 'numeroPrograma', 'ordenEscaleta'].includes(campo)) {
            if (nuevoValor === '' || nuevoValor === null) {
                valorConvertido = null;
            } else {
                const numero = parseInt(nuevoValor);
                if (isNaN(numero)) {
                    throw new Error(`El valor "${nuevoValor}" no es un número válido para el campo ${campo}`);
                }
                valorConvertido = numero;
            }
        }
        
        // Campo estado - mantener como string libre
        if (campo === 'estado') {
            valorConvertido = nuevoValor || null;
        }
        
        // Asignar el valor convertido
        concursante[campo] = valorConvertido;
        
        // Si el campo es programa, buscar el objeto programa
        if (campo === 'programa') {
            const prog = programas.find(p => p.id == valorConvertido);
            concursante.programa = prog ? { id: prog.id } : null;
        }
        
        await apiManager.put(`/api/concursantes/${id}`, concursante);
        await cargarConcursantes();
        mostrarExito('Campo actualizado correctamente');
    } catch (error) {
        mostrarError('Error al guardar el cambio: ' + error.message);
        td.innerHTML = valorOriginal;
    }
}

// Funciones de utilidad
function mostrarError(mensaje) {
    Toastify({
        text: mensaje,
        duration: 3000,
        close: true,
        gravity: "top",
        position: "right",
        backgroundColor: "#dc3545"
    }).showToast();
}

function mostrarExito(mensaje) {
    Toastify({
        text: mensaje,
        duration: 3000,
        close: true,
        gravity: "top",
        position: "right",
        backgroundColor: "#28a745"
    }).showToast();
}

// Modal selector de cuestionario
function abrirSelectorCuestionario() {
    concursanteParaAsignar = null; // Limpiar para uso en formulario
    buscarCuestionariosModal();
    const modal = new bootstrap.Modal(document.getElementById('modal-selector-cuestionario'));
    modal.show();
}

async function buscarCuestionariosModal() {
    const filtro = document.getElementById('buscador-cuestionario').value.trim().toLowerCase();
    const nivelFiltro = document.getElementById('filtro-nivel-cuestionario').value;
    
    try {
        let cuestionarios = await apiManager.get('/api/cuestionarios/para-asignar');
        
        // Aplicar filtros
        if (nivelFiltro) {
            cuestionarios = cuestionarios.filter(c => c.nivel === nivelFiltro);
        }
        
        if (filtro) {
            cuestionarios = cuestionarios.filter(c => {
                // Buscar por ID
                if (c.id.toString().includes(filtro)) return true;
                
                // Buscar por nivel
                if (c.nivel && c.nivel.toLowerCase().includes(filtro)) return true;
                
                // Buscar en texto de preguntas
                if (c.preguntas && c.preguntas.length > 0) {
                    return c.preguntas.some(p => 
                        (p.pregunta && p.pregunta.toLowerCase().includes(filtro)) ||
                        (p.respuesta && p.respuesta.toLowerCase().includes(filtro)) ||
                        (p.tematica && p.tematica.toLowerCase().includes(filtro))
                    );
                }
                
                return false;
            });
        }
        
        const tbody = document.getElementById('tabla-selector-cuestionario');
        tbody.innerHTML = '';
        
        if (!cuestionarios.length) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center">No hay cuestionarios disponibles</td></tr>';
            return;
        }
        
        cuestionarios.forEach(c => {
            const tr = document.createElement('tr');
            
            // Crear resumen de preguntas
            let preguntasResumen = '';
            if (c.preguntas && c.preguntas.length > 0) {
                preguntasResumen = c.preguntas.map(p => {
                    const preguntaCorta = p.pregunta ? (p.pregunta.length > 50 ? p.pregunta.substring(0, 50) + '...' : p.pregunta) : '';
                    const nivel = p.nivel ? p.nivel.replace('_', '') : '';
                    return `${nivel} ${preguntaCorta}`;
                }).join('<br>');
            } else {
                preguntasResumen = '<em>Sin preguntas</em>';
            }
            
            tr.innerHTML = `
                <td><strong>${c.id}</strong></td>
                <td><span class="badge bg-info">${c.nivel || 'N/A'}</span></td>
                <td><span class="badge ${Utils.getEstadoBadgeClass(c.estado, 'cuestionario')}">${Utils.formatearEstadoCuestionario(c.estado)}</span></td>
                <td>${c.fechaCreacion ? Utils.formatearFecha(c.fechaCreacion) : ''}</td>
                <td style="max-width: 300px; font-size: 0.85em;">${preguntasResumen}</td>
                <td><button class="btn btn-sm btn-success" onclick="seleccionarCuestionarioModal(${c.id})">Seleccionar</button></td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        mostrarError('Error al buscar cuestionarios: ' + e.message);
    }
}

// Funciones para asignar desde la tabla
let concursanteParaAsignar = null;

function abrirSelectorCuestionarioParaConcursante(concursanteId) {
    concursanteParaAsignar = concursanteId;
    buscarCuestionariosModal();
    const modal = new bootstrap.Modal(document.getElementById('modal-selector-cuestionario'));
    modal.show();
}

function abrirSelectorComboParaConcursante(concursanteId) {
    concursanteParaAsignar = concursanteId;
    buscarCombosModal();
    const modal = new bootstrap.Modal(document.getElementById('modal-selector-combo'));
    modal.show();
}

async function seleccionarCuestionarioModal(id) {
    if (concursanteParaAsignar) {
        // Asignar directamente al concursante
        try {
            const concursante = concursantes.find(c => c.id === concursanteParaAsignar);
            if (concursante) {
                concursante.cuestionarioId = id;
                await apiManager.put(`/api/concursantes/${concursanteParaAsignar}`, concursante);
                await cargarConcursantes();
                mostrarExito('Cuestionario asignado correctamente');
            }
        } catch (error) {
            mostrarError('Error al asignar cuestionario: ' + error.message);
        }
        concursanteParaAsignar = null;
    } else {
        // Asignar al formulario
        document.getElementById('cuestionario-id').value = id;
    }
    const modal = bootstrap.Modal.getInstance(document.getElementById('modal-selector-cuestionario'));
    modal.hide();
}

async function seleccionarComboModal(id) {
    if (concursanteParaAsignar) {
        // Asignar directamente al concursante
        try {
            const concursante = concursantes.find(c => c.id === concursanteParaAsignar);
            if (concursante) {
                concursante.comboId = id;
                await apiManager.put(`/api/concursantes/${concursanteParaAsignar}`, concursante);
                await cargarConcursantes();
                mostrarExito('Combo asignado correctamente');
            }
        } catch (error) {
            mostrarError('Error al asignar combo: ' + error.message);
        }
        concursanteParaAsignar = null;
    } else {
        // Asignar al formulario
        document.getElementById('combo-id').value = id;
    }
    const modal = bootstrap.Modal.getInstance(document.getElementById('modal-selector-combo'));
    modal.hide();
}

function limpiarSelectorCuestionario() {
    document.getElementById('cuestionario-id').value = '';
}

// Funciones para selector de combos
function abrirSelectorCombo() {
    concursanteParaAsignar = null; // Limpiar para uso en formulario
    buscarCombosModal();
    const modal = new bootstrap.Modal(document.getElementById('modal-selector-combo'));
    modal.show();
}

async function buscarCombosModal() {
    const filtro = document.getElementById('buscador-combo').value.trim().toLowerCase();
    
    try {
        let combos = await apiManager.get('/api/combos/para-asignar');
        
        // Aplicar filtro de búsqueda
        if (filtro) {
            combos = combos.filter(c => {
                // Buscar por ID
                if (c.id.toString().includes(filtro)) return true;
                
                // Buscar en texto de preguntas multiplicadoras
                if (c.preguntas && c.preguntas.length > 0) {
                    return c.preguntas.some(p => 
                        (p.pregunta && p.pregunta.toLowerCase().includes(filtro)) ||
                        (p.respuesta && p.respuesta.toLowerCase().includes(filtro)) ||
                        (p.tematica && p.tematica.toLowerCase().includes(filtro))
                    );
                }
                
                return false;
            });
        }
        
        const tbody = document.getElementById('tabla-selector-combo');
        tbody.innerHTML = '';
        
        if (!combos.length) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center">No hay combos disponibles</td></tr>';
            return;
        }
        
        combos.forEach(c => {
            const tr = document.createElement('tr');
            
            // Crear resumen de preguntas multiplicadoras
            let preguntasResumen = '';
            if (c.preguntas && c.preguntas.length > 0) {
                preguntasResumen = c.preguntas.map(p => {
                    const preguntaCorta = p.pregunta ? (p.pregunta.length > 40 ? p.pregunta.substring(0, 40) + '...' : p.pregunta) : '';
                    const nivel = p.nivel ? p.nivel.replace('_', '') : '';
                    const factor = p.factor ? `<span class="badge bg-warning">x${p.factor}</span>` : '';
                    return `${factor} ${nivel} ${preguntaCorta}`;
                }).join('<br>');
            } else {
                preguntasResumen = '<em>Sin preguntas</em>';
            }
            
            tr.innerHTML = `
                <td><strong>${c.id}</strong></td>
                <td><span class="badge ${Utils.getEstadoBadgeClass(c.estado, 'combo')}">${Utils.formatearEstadoCombo(c.estado)}</span></td>
                <td>${c.fechaCreacion ? Utils.formatearFecha(c.fechaCreacion) : ''}</td>
                <td style="max-width: 350px; font-size: 0.85em;">${preguntasResumen}</td>
                <td><button class="btn btn-sm btn-success" onclick="seleccionarComboModal(${c.id})">Seleccionar</button></td>
            `;
            tbody.appendChild(tr);
        });
    } catch (error) {
        mostrarError('Error al cargar combos: ' + error.message);
    }
}

function limpiarSelectorCombo() {
    document.getElementById('combo-id').value = '';
}

// --- ORDENACIÓN POR COLUMNA ---
function ordenarTablaConcursantes(colIndex, tipo = 'string') {
    const tabla = document.getElementById('tabla-concursantes-principal');
    const tbody = tabla.querySelector('tbody');
    const filas = Array.from(tbody.querySelectorAll('tr'));
    const asc = tabla.dataset.ordenCol == colIndex ? tabla.dataset.ordenAsc !== 'true' : true;
    filas.sort((a, b) => {
        let va = a.children[colIndex].innerText.trim();
        let vb = b.children[colIndex].innerText.trim();
        if (tipo === 'number') {
            va = parseFloat(va.replace(/[^\d.\-]/g, '')) || 0;
            vb = parseFloat(vb.replace(/[^\d.\-]/g, '')) || 0;
        }
        return asc ? va.localeCompare(vb, undefined, {numeric: tipo==='number'}) : vb.localeCompare(va, undefined, {numeric: tipo==='number'});
    });
    filas.forEach(f => tbody.appendChild(f));
    tabla.dataset.ordenCol = colIndex;
    tabla.dataset.ordenAsc = asc;
}

// Añadir listeners a los th
setTimeout(() => {
    const tabla = document.getElementById('tabla-concursantes-principal');
    if (tabla) {
        tabla.querySelectorAll('thead th').forEach((th, idx) => {
            th.style.cursor = 'pointer';
            th.onclick = () => ordenarTablaConcursantes(idx, th.dataset.tipo || 'string');
        });
    }
}, 500);

// --- AUTO-SCROLL HORIZONTAL EN TABLA DE CONCURSANTES ---
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

window.cambiarPassword = function() {
    document.getElementById('form-cambiar-password').reset();
    const modal = new bootstrap.Modal(document.getElementById('modal-cambiar-password'));
    modal.show();
};

// Función para formatear fechas al formato DD/MM/YYYY
function formatearFecha(fecha) {
    if (!fecha) return '';
    
    try {
        let fechaObj;
        
        // Si la fecha viene en formato ISO (YYYY-MM-DD)
        if (fecha.includes('-')) {
            const partes = fecha.split('-');
            if (partes.length === 3) {
                const año = parseInt(partes[0]);
                const mes = parseInt(partes[1]) - 1; // Los meses en JavaScript van de 0-11
                const dia = parseInt(partes[2]);
                fechaObj = new Date(año, mes, dia);
            } else {
                fechaObj = new Date(fecha);
            }
        } else {
            fechaObj = new Date(fecha);
        }
        
        // Verificar si la fecha es válida
        if (isNaN(fechaObj.getTime())) {
            return fecha; // Devolver la fecha original si no se puede parsear
        }
        
        const dia = fechaObj.getDate().toString().padStart(2, '0');
        const mes = (fechaObj.getMonth() + 1).toString().padStart(2, '0');
        const año = fechaObj.getFullYear();
        
        return `${dia}/${mes}/${año}`;
    } catch (error) {
        console.error('Error al formatear fecha:', fecha, error);
        return fecha; // Devolver la fecha original en caso de error
    }
} 