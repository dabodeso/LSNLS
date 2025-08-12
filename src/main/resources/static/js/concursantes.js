// Variables globales
let concursantes = [];
let programas = [];
let concursanteActual = null;

// Configuración de columnas por rol
let configuracionColumnas = {
    esDireccion: false,
    columnasVisibles: {
        'numero-concur': true,
        'jornada': true,
        'dia-grabacion': true,
        'lugar': true,
        'nombre': true,
        'foto': true,
        'edad': true,
        'ocupacion': true,
        'rr-ss': true,
        'cuest': true,
        'combo': true,
        'x': true,
        'resultado': true,
        'notas-grabacion': true,
        'guionista': true,
        'valoracion-guionista': true,
        'estado': true,
        'momentos-destacados': false, // Solo dirección
        'duracion': true,
        'duracion-direccion': false, // Solo dirección
        'duracion-final': false, // Solo dirección
        'valoracion-final': false, // Solo dirección
        'numero-pgm': false, // Solo dirección
        'orden-escaleta': false, // Solo dirección
        'bonico': false // Solo dirección
    }
};

// Funciones de inicialización
async function inicializarConcursantes() {
    // Detectar rol del usuario
    detectarRolUsuario();
    
    await cargarProgramas();
    await cargarConcursantes();
    setupEventListeners();
    
    // Actualizar encabezados de la tabla según la configuración
    actualizarEncabezadosTabla();
}

function detectarRolUsuario() {
    try {
        const usuario = JSON.parse(localStorage.getItem('usuario'));
        console.log('🔍 [CONCURSANTES] Usuario detectado:', usuario);
        
        if (usuario && usuario.rol === 'ROLE_DIRECCION') {
            console.log('🔍 [CONCURSANTES] Usuario es DIRECCIÓN - activando columnas avanzadas');
            configuracionColumnas.esDireccion = true;
            
            // Mostrar botón de configuración de columnas
            const btnConfig = document.getElementById('btn-config-columnas');
            if (btnConfig) {
                btnConfig.style.display = 'block';
            }
            
            // Activar columnas de dirección por defecto
            configuracionColumnas.columnasVisibles['momentos-destacados'] = true;
            configuracionColumnas.columnasVisibles['duracion-direccion'] = true;
            configuracionColumnas.columnasVisibles['duracion-final'] = true;
            configuracionColumnas.columnasVisibles['valoracion-final'] = true;
            configuracionColumnas.columnasVisibles['numero-pgm'] = true;
            configuracionColumnas.columnasVisibles['orden-escaleta'] = true;
            configuracionColumnas.columnasVisibles['bonico'] = true;
            
            console.log('🔍 [CONCURSANTES] Configuración de columnas para dirección:', configuracionColumnas.columnasVisibles);
            
            // Cargar configuración guardada para dirección (puede sobrescribir los valores por defecto)
            cargarConfiguracionGuardada();
        } else {
            console.log('🔍 [CONCURSANTES] Usuario NO es dirección - aplicando configuración básica');
            // Usuario no es dirección - aplicar configuración básica
            aplicarConfiguracionBasica();
        }
    } catch (error) {
        console.error('Error al detectar rol:', error);
        aplicarConfiguracionBasica();
    }
}

function aplicarConfiguracionBasica() {
    // Para usuarios no dirección, ocultar columnas avanzadas
    configuracionColumnas.columnasVisibles['momentos-destacados'] = false;
    configuracionColumnas.columnasVisibles['duracion-direccion'] = false;
    configuracionColumnas.columnasVisibles['duracion-final'] = false;
    configuracionColumnas.columnasVisibles['valoracion-final'] = false;
    configuracionColumnas.columnasVisibles['numero-pgm'] = false;
    configuracionColumnas.columnasVisibles['orden-escaleta'] = false;
    configuracionColumnas.columnasVisibles['bonico'] = false;
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
    console.log('🔍 [CONCURSANTES] Mostrando concursantes con configuración:', configuracionColumnas.columnasVisibles);
    const lista = concursantesFiltrados || concursantes;
    const tbody = document.getElementById('tabla-concursantes');
    
    tbody.innerHTML = lista.map(concursante => {
        const celdas = [];
        
        // Nº CONCUR
        if (configuracionColumnas.columnasVisibles['numero-concur']) {
            celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'numeroConcursante', this)">${concursante.numeroConcursante || ''}</td>`);
        }
        
        // JORNADA
        if (configuracionColumnas.columnasVisibles['jornada']) {
            celdas.push(`<td onclick="abrirSelectorJornadaParaConcursante(${concursante.id})" style="cursor: pointer; background-color: #f8f9fa;" title="Click para seleccionar jornada">
                ${concursante.jornadaNombre ? `<span class="badge bg-success">${concursante.jornadaNombre}</span>` : '<em class="text-muted">Sin asignar</em>'}
            </td>`);
        }
        
        // DÍA GRABACIÓN
        if (configuracionColumnas.columnasVisibles['dia-grabacion']) {
            celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'diaGrabacion', this)">${formatearFecha(concursante.diaGrabacion)}</td>`);
        }
        
        // LUGAR
        if (configuracionColumnas.columnasVisibles['lugar']) {
            celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'lugar', this)">${concursante.lugar || ''}</td>`);
        }
        
        // NOMBRE
        if (configuracionColumnas.columnasVisibles['nombre']) {
            celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'nombre', this)">${concursante.nombre || ''}</td>`);
        }
        
        // FOTO
        if (configuracionColumnas.columnasVisibles['foto']) {
            celdas.push(`<td onclick="abrirExploradorFoto(${concursante.id}, event)" style="cursor: pointer;">
                ${concursante.foto ? 
                    `<img src="/uploads/${concursante.foto}" class="foto-concursante" alt="Foto del concursante">` : 
                    `<div class="campo-foto-vacio" onclick="abrirExploradorFoto(${concursante.id}, event)">
                        <i class="fas fa-camera"></i>
                        <span>Añadir foto</span>
                    </div>`
                }
            </td>`);
        }
        
        // EDAD
        if (configuracionColumnas.columnasVisibles['edad']) {
            celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'edad', this)">${concursante.edad || ''}</td>`);
        }
        
        // OCUPACIÓN
        if (configuracionColumnas.columnasVisibles['ocupacion']) {
            celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'ocupacion', this)">${concursante.ocupacion || ''}</td>`);
        }
        
        // RR SS
        if (configuracionColumnas.columnasVisibles['rr-ss']) {
            celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'redesSociales', this)">${concursante.redesSociales || ''}</td>`);
        }
        
        // CUEST
        if (configuracionColumnas.columnasVisibles['cuest']) {
            celdas.push(`<td onclick="abrirSelectorCuestionarioParaConcursante(${concursante.id})" style="cursor: pointer; background-color: #f8f9fa;" title="Click para seleccionar cuestionario">
                ${concursante.cuestionarioId ? `<span class="badge bg-primary">${concursante.cuestionarioId}</span>` : '<em class="text-muted">Sin asignar</em>'}
            </td>`);
        }
        
        // COMBO
        if (configuracionColumnas.columnasVisibles['combo']) {
            celdas.push(`<td onclick="abrirSelectorComboParaConcursante(${concursante.id})" style="cursor: pointer; background-color: #f8f9fa;" title="Click para seleccionar combo">
                ${concursante.comboId ? `<span class="badge bg-warning">${concursante.comboId}</span>` : '<em class="text-muted">Sin asignar</em>'}
            </td>`);
        }
        
        // X
        if (configuracionColumnas.columnasVisibles['x']) {
            celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'factorX', this)">${concursante.factorX || ''}</td>`);
        }
        
        // RESULTADO
        if (configuracionColumnas.columnasVisibles['resultado']) {
            celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'resultado', this)">${concursante.resultado !== null && concursante.resultado !== undefined ? concursante.resultado : ''}</td>`);
        }
        
        // NOTAS GRABACIÓN
        if (configuracionColumnas.columnasVisibles['notas-grabacion']) {
            celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'notasGrabacion', this)">${concursante.notasGrabacion || ''}</td>`);
        }
        
        // GUIONISTA
        if (configuracionColumnas.columnasVisibles['guionista']) {
            celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'guionista', this)">${concursante.guionista || ''}</td>`);
        }
        
        // VALORACIÓN GUIONISTA
        if (configuracionColumnas.columnasVisibles['valoracion-guionista']) {
            celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'valoracionGuionista', this)">${concursante.valoracionGuionista || ''}</td>`);
        }
        
        // ESTADO
        if (configuracionColumnas.columnasVisibles['estado']) {
            celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'estado', this)">${concursante.estado || ''}</td>`);
        }
        
        // MOMENTOS DESTACADOS
        if (configuracionColumnas.columnasVisibles['momentos-destacados']) {
            celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'momentosDestacados', this)">${concursante.momentosDestacados || ''}</td>`);
        }
        
        // DURACIÓN
        if (configuracionColumnas.columnasVisibles['duracion']) {
            celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'duracion', this)">${concursante.duracion || ''}</td>`);
        }
        
        // DUR. DIRECCIÓN
        if (configuracionColumnas.columnasVisibles['duracion-direccion']) {
            console.log('🔍 [CONCURSANTES] Renderizando columna DUR. DIRECCIÓN para concursante:', concursante.id);
            celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'duracionDireccion', this)">${concursante.duracionDireccion || ''}</td>`);
        }
        
        // DUR. FINAL
        if (configuracionColumnas.columnasVisibles['duracion-final']) {
            console.log('🔍 [CONCURSANTES] Renderizando columna DUR. FINAL para concursante:', concursante.id);
            celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'duracionFinal', this)">${concursante.duracionFinal || ''}</td>`);
        }
        
        // VALORACIÓN FINAL
        if (configuracionColumnas.columnasVisibles['valoracion-final']) {
            celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'valoracionFinal', this)">${concursante.valoracionFinal || ''}</td>`);
        }
        
        // Nº PGM
        if (configuracionColumnas.columnasVisibles['numero-pgm']) {
            celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'numeroPrograma', this)">${concursante.numeroPrograma || ''}</td>`);
        }
        
        // ORDEN ESCALETA
        if (configuracionColumnas.columnasVisibles['orden-escaleta']) {
            celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'ordenEscaleta', this)">${concursante.ordenEscaleta || ''}</td>`);
        }
        
        // BONICO
        if (configuracionColumnas.columnasVisibles['bonico']) {
            celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'bonico', this)">${concursante.bonico || ''}</td>`);
        }
        
        // ACCIONES (siempre visible)
        celdas.push(`<td>
            <button class="btn btn-sm btn-primary" onclick="editarConcursante(${concursante.id})">
                <i class="fas fa-edit"></i>
            </button>
            <button class="btn btn-sm btn-danger" onclick="eliminarConcursante(${concursante.id})">
                <i class="fas fa-trash"></i>
            </button>
        </td>`);
        
        return `<tr data-id="${concursante.id}" oncontextmenu="showContextMenu(event, ${concursante.id}, 'concursante')">
            ${celdas.join('')}
        </tr>`;
    }).join('');
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
    const modal = new bootstrap.Modal(document.getElementById('modal-concursante'));
    modal.show();
}

async function editarConcursante(id) {
    try {
        concursanteActual = await apiManager.get(`/api/concursantes/${id}`);
        document.getElementById('modal-concursante-titulo').textContent = 'Editar Concursante';
        const form = document.getElementById('form-concursante');
        form.reset();
        
        // Campos básicos
        document.getElementById('concursante-id').value = concursanteActual.id;
        
        // CORREGIR: usar jornadaNombre en lugar de jornada
        document.getElementById('jornada').value = concursanteActual.jornadaNombre || '';
        
        // CORREGIR: formatear fecha correctamente para input type="date"
        if (concursanteActual.diaGrabacion) {
            // Si viene como string "YYYY-MM-DD" o como array [año, mes, día]
            let fechaFormateada = '';
            if (Array.isArray(concursanteActual.diaGrabacion)) {
                // Formato array [2024, 1, 15] -> "2024-01-15"
                const [año, mes, día] = concursanteActual.diaGrabacion;
                fechaFormateada = `${año}-${mes.toString().padStart(2, '0')}-${día.toString().padStart(2, '0')}`;
            } else if (typeof concursanteActual.diaGrabacion === 'string') {
                // Ya viene en formato correcto "YYYY-MM-DD"
                fechaFormateada = concursanteActual.diaGrabacion;
            }
            document.getElementById('dia-grabacion').value = fechaFormateada;
        }
        
        document.getElementById('lugar-concursante').value = concursanteActual.lugar || '';
        document.getElementById('nombre-concursante').value = concursanteActual.nombre || '';
        document.getElementById('edad-concursante').value = concursanteActual.edad || '';
        document.getElementById('ocupacion').value = concursanteActual.ocupacion || '';
        document.getElementById('redes-sociales').value = concursanteActual.redesSociales || '';
        
        // CORREGIR: usar cuestionarioId en lugar de cuestionario.id
        document.getElementById('cuestionario-id').value = concursanteActual.cuestionarioId || '';
        
        // CORREGIR: usar comboId en lugar de combo.id
        document.getElementById('combo-id').value = concursanteActual.comboId || '';
        
        document.getElementById('factor-x').value = concursanteActual.factorX || '';
        document.getElementById('resultado').value = concursanteActual.resultado || '';
        document.getElementById('notas-grabacion').value = concursanteActual.notasGrabacion || '';
        document.getElementById('guionista').value = concursanteActual.guionista || '';
        document.getElementById('valoracion-guionista').value = concursanteActual.valoracionGuionista || '';
        document.getElementById('momentos-destacados').value = concursanteActual.momentosDestacados || '';
        document.getElementById('duracion').value = concursanteActual.duracion || '';
        document.getElementById('duracion-direccion').value = concursanteActual.duracionDireccion || '';
        document.getElementById('duracion-final').value = concursanteActual.duracionFinal || '';
        document.getElementById('valoracion-final').value = concursanteActual.valoracionFinal || '';
        document.getElementById('numero-programa').value = concursanteActual.numeroPrograma || '';
        document.getElementById('orden-escaleta').value = concursanteActual.ordenEscaleta || '';
        document.getElementById('bonico').value = concursanteActual.bonico || '';
        
        // AÑADIR: campos faltantes
        // Estado (si existe el campo en el formulario)
        const estadoElement = document.getElementById('estado');
        if (estadoElement) {
            estadoElement.value = concursanteActual.estado || '';
        }
        
        // Premio (si existe el campo en el formulario)
        const premioElement = document.getElementById('premio');
        if (premioElement) {
            premioElement.value = concursanteActual.premio || '';
        }
        
        // Foto (si existe el campo en el formulario)
        const fotoElement = document.getElementById('foto');
        if (fotoElement) {
            fotoElement.value = concursanteActual.foto || '';
        }
        
        // Créditos especiales (si existe el campo en el formulario)
        const creditosElement = document.getElementById('creditos-especiales');
        if (creditosElement) {
            creditosElement.value = concursanteActual.creditosEspeciales || '';
        }
        
        const modal = new bootstrap.Modal(document.getElementById('modal-concursante'));
        modal.show();
    } catch (error) {
        mostrarError('Error al cargar concursante: ' + error.message);
    }
}

// Guardar concursante con gestión de errores mejorada
async function guardarConcursante() {
    const form = document.getElementById('form-concursante');
    
    // Detectar si es edición por la presencia de ID
    const esEdicion = document.getElementById('concursante-id').value;
    
    // Recoge todos los campos del formulario
    const datosConcursante = {
        id: document.getElementById('concursante-id').value || null,
        // CORREGIR: mantener jornadaId original en edición, null en creación
        jornadaId: esEdicion && concursanteActual ? concursanteActual.jornadaId : null,
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
        momentosDestacados: document.getElementById('momentos-destacados').value || null,
        duracion: document.getElementById('duracion').value || null,
        duracionDireccion: document.getElementById('duracion-direccion').value || null,
        duracionFinal: document.getElementById('duracion-final').value || null,
        valoracionFinal: document.getElementById('valoracion-final').value || null,
        numeroPrograma: document.getElementById('numero-programa').value || null,
        ordenEscaleta: document.getElementById('orden-escaleta').value || null,
        bonico: document.getElementById('bonico').value || null,
        // CORREGIR: enviar estado del formulario
        estado: document.getElementById('estado') ? document.getElementById('estado').value || null : null,
        // AÑADIR: campos faltantes si existen en el formulario
        premio: document.getElementById('premio') ? document.getElementById('premio').value || null : null,
        foto: document.getElementById('foto') ? document.getElementById('foto').value || null : null,
        creditosEspeciales: document.getElementById('creditos-especiales') ? document.getElementById('creditos-especiales').value || null : null
    };

    // Enviar como JSON usando apiManager o fetch
    try {
        const token = localStorage.getItem('token');
        let response;
        
        if (esEdicion) {
            // Editar concursante existente
            response = await fetch(`/api/concursantes/${datosConcursante.id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': token ? (token.startsWith('Bearer ') ? token : 'Bearer ' + token) : ''
                },
                body: JSON.stringify(datosConcursante)
            });
        } else {
            // Crear nuevo concursante
            response = await fetch('/api/concursantes', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': token ? (token.startsWith('Bearer ') ? token : 'Bearer ' + token) : ''
                },
                body: JSON.stringify(datosConcursante)
            });
        }
        
        if (response.ok) {
            mostrarExito(esEdicion ? 'Concursante editado correctamente' : 'Concursante guardado correctamente');
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
        
        // Campos de duración - validar formato MM:SS
        if (['duracion', 'duracionDireccion', 'duracionFinal'].includes(campo)) {
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
        else if (['numeroConcursante', 'edad', 'concursantesPorJornada', 'numeroPrograma', 'ordenEscaleta', 'resultado'].includes(campo)) {
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
        // Campo fecha - validar formato DD/MM/YYYY y convertir a ISO
        else if (campo === 'diaGrabacion') {
            if (nuevoValor === '' || nuevoValor === null) {
                valorConvertido = null;
            } else {
                // Validar formato DD/MM/YYYY
                const formatoValido = /^\d{2}\/\d{2}\/\d{4}$/.test(nuevoValor);
                if (!formatoValido) {
                    throw new Error('La fecha debe tener formato DD/MM/YYYY (ej: 11/08/2025)');
                }
                
                // Convertir DD/MM/YYYY a YYYY-MM-DD (formato ISO)
                const [dia, mes, año] = nuevoValor.split('/');
                const fechaISO = `${año}-${mes}-${dia}`;
                
                // Validar que la fecha sea válida
                const fechaObj = new Date(fechaISO);
                if (isNaN(fechaObj.getTime())) {
                    throw new Error('La fecha ingresada no es válida');
                }
                
                valorConvertido = fechaISO;
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

// Variables para gestión de jornadas
let jornadas = [];
let concursanteParaAsignarJornada = null;

async function cargarJornadas() {
    try {
        const response = await apiManager.get('/api/jornadas');
        jornadas = response.datos || [];
    } catch (error) {
        console.error('Error al cargar jornadas:', error);
    }
}

function abrirSelectorJornadaParaConcursante(concursanteId) {
    concursanteParaAsignarJornada = concursanteId;
    mostrarModalSelectorJornada();
}

function mostrarModalSelectorJornada() {
    const modal = document.getElementById('modal-selector-jornada');
    if (!modal) {
        crearModalSelectorJornada();
    }
    
    cargarJornadas().then(() => {
        const tbody = document.getElementById('tabla-jornadas-selector');
        tbody.innerHTML = jornadas.map(jornada => `
            <tr>
                <td>${jornada.nombre}</td>
                <td>${jornada.estado}</td>
                <td>${jornada.fechaJornada ? new Date(jornada.fechaJornada).toLocaleDateString('es-ES') : 'Sin fecha'}</td>
                <td>
                    <button class="btn btn-sm btn-success" onclick="seleccionarJornadaModal(${jornada.id})">
                        <i class="fas fa-check"></i> Seleccionar
                    </button>
                </td>
            </tr>
        `).join('');
        
        const modalInstance = new bootstrap.Modal(document.getElementById('modal-selector-jornada'));
        modalInstance.show();
    });
}

function crearModalSelectorJornada() {
    const modalHTML = `
        <div class="modal fade" id="modal-selector-jornada" tabindex="-1">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">Seleccionar Jornada</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <div class="table-responsive">
                            <table class="table table-striped">
                                <thead>
                                    <tr>
                                        <th>Nombre</th>
                                        <th>Estado</th>
                                        <th>Fecha</th>
                                        <th>Acciones</th>
                                    </tr>
                                </thead>
                                <tbody id="tabla-jornadas-selector">
                                </tbody>
                            </table>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                        <button type="button" class="btn btn-danger" onclick="desasignarJornadaModal()">
                            <i class="fas fa-times"></i> Desasignar
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
    document.body.insertAdjacentHTML('beforeend', modalHTML);
}

async function seleccionarJornadaModal(jornadaId) {
    if (concursanteParaAsignarJornada) {
        try {
            await apiManager.post(`/api/concursantes/${concursanteParaAsignarJornada}/asignar-jornada/${jornadaId}`);
            await cargarConcursantes();
            mostrarExito('Jornada asignada correctamente');
        } catch (error) {
            mostrarError('Error al asignar jornada: ' + error.message);
        }
        concursanteParaAsignarJornada = null;
    }
    
    const modal = bootstrap.Modal.getInstance(document.getElementById('modal-selector-jornada'));
    modal.hide();
}

async function desasignarJornadaModal() {
    if (concursanteParaAsignarJornada) {
        try {
            await apiManager.delete(`/api/concursantes/${concursanteParaAsignarJornada}/desasignar-jornada`);
            await cargarConcursantes();
            mostrarExito('Jornada desasignada correctamente');
        } catch (error) {
            mostrarError('Error al desasignar jornada: ' + error.message);
        }
        concursanteParaAsignarJornada = null;
    }
    
    const modal = bootstrap.Modal.getInstance(document.getElementById('modal-selector-jornada'));
    modal.hide();
}

// Funciones para manejo de fotos
function abrirExploradorFoto(concursanteId, event) {
    // Detener la propagación del evento para evitar que se active el click del row
    if (event) {
        event.stopPropagation();
        event.preventDefault();
    }
    
    // Crear input file dinámicamente
    const inputFile = document.createElement('input');
    inputFile.type = 'file';
    inputFile.accept = 'image/*';
    inputFile.style.display = 'none';
    
    inputFile.onchange = function(event) {
        const file = event.target.files[0];
        if (file) {
            subirFotoConcursante(concursanteId, file);
        }
        // Limpiar el input después de usar
        document.body.removeChild(inputFile);
    };
    
    // Añadir al DOM y hacer click
    document.body.appendChild(inputFile);
    inputFile.click();
}

async function subirFotoConcursante(concursanteId, file) {
    try {
        // Mostrar indicador de carga
        mostrarExito('Subiendo foto...');
        
        // Crear FormData para enviar el archivo
        const formData = new FormData();
        formData.append('foto', file);
        
        // Subir la foto
        const response = await fetch(`/api/concursantes/${concursanteId}/foto`, {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            },
            body: formData
        });
        
        if (!response.ok) {
            throw new Error('Error al subir la foto');
        }
        
        const resultado = await response.json();
        
        // Actualizar la vista
        await cargarConcursantes();
        mostrarExito('Foto subida correctamente');
        
    } catch (error) {
        console.error('Error al subir foto:', error);
        mostrarError('Error al subir la foto: ' + error.message);
    }
}

// Funciones para el formulario de fotos
function limpiarFoto() {
    document.getElementById('foto-concursante').value = '';
    document.getElementById('foto-nombre').value = '';
    document.getElementById('foto-preview').style.display = 'none';
}

// Funciones para configuración de columnas
function mostrarConfiguracionColumnas() {
    // Cargar configuración actual en los checkboxes
    cargarConfiguracionEnModal();
    const modal = new bootstrap.Modal(document.getElementById('modal-config-columnas'));
    modal.show();
}

function cargarConfiguracionEnModal() {
    // Mapear configuración a IDs de checkboxes
    const mapeoColumnas = {
        'numero-concur': 'col-numero-concur',
        'jornada': 'col-jornada',
        'dia-grabacion': 'col-dia-grabacion',
        'lugar': 'col-lugar',
        'nombre': 'col-nombre',
        'foto': 'col-foto',
        'edad': 'col-edad',
        'ocupacion': 'col-ocupacion',
        'rr-ss': 'col-rr-ss',
        'cuest': 'col-cuest',
        'combo': 'col-combo',
        'x': 'col-x',
        'resultado': 'col-resultado',
        'notas-grabacion': 'col-notas-grabacion',
        'guionista': 'col-guionista',
        'valoracion-guionista': 'col-valoracion-guionista',
        'estado': 'col-estado',
        'momentos-destacados': 'col-momentos-destacados',
        'duracion': 'col-duracion',
        'duracion-direccion': 'col-duracion-direccion',
        'duracion-final': 'col-duracion-final',
        'valoracion-final': 'col-valoracion-final',
        'numero-pgm': 'col-numero-pgm',
        'orden-escaleta': 'col-orden-escaleta',
        'bonico': 'col-bonico'
    };
    
    // Aplicar configuración a los checkboxes
    Object.keys(mapeoColumnas).forEach(columna => {
        const checkboxId = mapeoColumnas[columna];
        const checkbox = document.getElementById(checkboxId);
        if (checkbox) {
            checkbox.checked = configuracionColumnas.columnasVisibles[columna] || false;
        }
    });
}

function aplicarConfiguracionColumnas() {
    // Mapear IDs de checkboxes a configuración
    const mapeoColumnas = {
        'col-numero-concur': 'numero-concur',
        'col-jornada': 'jornada',
        'col-dia-grabacion': 'dia-grabacion',
        'col-lugar': 'lugar',
        'col-nombre': 'nombre',
        'col-foto': 'foto',
        'col-edad': 'edad',
        'col-ocupacion': 'ocupacion',
        'col-rr-ss': 'rr-ss',
        'col-cuest': 'cuest',
        'col-combo': 'combo',
        'col-x': 'x',
        'col-resultado': 'resultado',
        'col-notas-grabacion': 'notas-grabacion',
        'col-guionista': 'guionista',
        'col-valoracion-guionista': 'valoracion-guionista',
        'col-estado': 'estado',
        'col-momentos-destacados': 'momentos-destacados',
        'col-duracion': 'duracion',
        'col-duracion-direccion': 'duracion-direccion',
        'col-duracion-final': 'duracion-final',
        'col-valoracion-final': 'valoracion-final',
        'col-numero-pgm': 'numero-pgm',
        'col-orden-escaleta': 'orden-escaleta',
        'col-bonico': 'bonico'
    };
    
    // Aplicar configuración desde los checkboxes
    Object.keys(mapeoColumnas).forEach(checkboxId => {
        const columna = mapeoColumnas[checkboxId];
        const checkbox = document.getElementById(checkboxId);
        if (checkbox) {
            configuracionColumnas.columnasVisibles[columna] = checkbox.checked;
        }
    });
    
    // Guardar configuración en localStorage
    localStorage.setItem('configuracionColumnasConcursantes', JSON.stringify(configuracionColumnas));
    
    // Actualizar tabla
    actualizarEncabezadosTabla();
    mostrarConcursantes();
    
    // Cerrar modal
    bootstrap.Modal.getInstance(document.getElementById('modal-config-columnas')).hide();
    
    mostrarExito('Configuración de columnas aplicada correctamente');
}

function actualizarEncabezadosTabla() {
    const thead = document.querySelector('#tabla-concursantes-principal thead tr');
    if (!thead) return;
    
    const encabezados = [];
    
    // Mapear columnas a encabezados
    const mapeoEncabezados = {
        'numero-concur': 'Nº CONCUR',
        'jornada': 'JORNADA',
        'dia-grabacion': 'DÍA GRABACIÓN',
        'lugar': 'LUGAR',
        'nombre': 'NOMBRE',
        'foto': 'FOTO',
        'edad': 'EDAD',
        'ocupacion': 'OCUPACIÓN',
        'rr-ss': 'RR SS',
        'cuest': 'CUEST',
        'combo': 'COMBO',
        'x': 'X',
        'resultado': 'RESULTADO',
        'notas-grabacion': 'NOTAS GRABACIÓN',
        'guionista': 'GUIONISTA',
        'valoracion-guionista': 'VALORACIÓN GUIONISTA',
        'estado': 'ESTADO',
        'momentos-destacados': 'MOMENTOS DESTACADOS',
        'duracion': 'DURACIÓN',
        'duracion-direccion': 'DUR. DIRECCIÓN',
        'duracion-final': 'DUR. FINAL',
        'valoracion-final': 'VALORACIÓN FINAL',
        'numero-pgm': 'Nº PGM',
        'orden-escaleta': 'ORDEN ESCALETA',
        'bonico': 'BONICO'
    };
    
    // Construir encabezados visibles
    Object.keys(mapeoEncabezados).forEach(columna => {
        if (configuracionColumnas.columnasVisibles[columna]) {
            encabezados.push(`<th>${mapeoEncabezados[columna]}</th>`);
        }
    });
    
    // Añadir encabezado de acciones
    encabezados.push('<th>ACCIONES</th>');
    
    thead.innerHTML = encabezados.join('');
}

function seleccionarTodasColumnas() {
    const checkboxes = document.querySelectorAll('#modal-config-columnas input[type="checkbox"]');
    checkboxes.forEach(checkbox => {
        checkbox.checked = true;
    });
}

function deseleccionarTodasColumnas() {
    const checkboxes = document.querySelectorAll('#modal-config-columnas input[type="checkbox"]');
    checkboxes.forEach(checkbox => {
        checkbox.checked = false;
    });
}

// Cargar configuración guardada al inicializar
function cargarConfiguracionGuardada() {
    try {
        const configGuardada = localStorage.getItem('configuracionColumnasConcursantes');
        if (configGuardada) {
            const config = JSON.parse(configGuardada);
            // Solo cargar si el usuario es dirección y hay configuración válida
            if (configuracionColumnas.esDireccion && config && config.columnasVisibles) {
                // Aplicar configuración guardada solo para las columnas que están en la configuración guardada
                Object.keys(config.columnasVisibles).forEach(columna => {
                    if (configuracionColumnas.columnasVisibles.hasOwnProperty(columna)) {
                        configuracionColumnas.columnasVisibles[columna] = config.columnasVisibles[columna];
                    }
                });
            }
        }
    } catch (error) {
        console.error('Error al cargar configuración:', error);
    }
}

// Event listener para el input de foto en el formulario
document.addEventListener('DOMContentLoaded', function() {
    const fotoInput = document.getElementById('foto-concursante');
    if (fotoInput) {
        fotoInput.addEventListener('change', function(event) {
            const file = event.target.files[0];
            if (file) {
                document.getElementById('foto-nombre').value = file.name;
                
                // Mostrar preview
                const reader = new FileReader();
                reader.onload = function(e) {
                    document.getElementById('foto-preview-img').src = e.target.result;
                    document.getElementById('foto-preview').style.display = 'block';
                };
                reader.readAsDataURL(file);
            }
        });
    }
}); 