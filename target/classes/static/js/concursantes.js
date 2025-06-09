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
    
    selectPrograma.innerHTML = '<option value="">Seleccione un programa...</option>' + options.join('');
    selectFiltro.innerHTML = '<option value="">Todos</option>' + options.join('');
}

function mostrarConcursantes(concursantesFiltrados = null) {
    const lista = concursantesFiltrados || concursantes;
    const tbody = document.getElementById('tabla-concursantes');
    tbody.innerHTML = lista.map(concursante => `
        <tr data-id="${concursante.id}">
            <td>${concursante.id}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'nombre', this)">${concursante.nombre || ''}</td>
            <td ondblclick="abrirSelectorImagen(${concursante.id})">${concursante.imagen ? `<img src='${concursante.imagen}' alt='Imagen' style='max-width:100px;max-height:100px;object-fit:cover;cursor:pointer;'/>` : ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'edad', this)">${concursante.edad || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'datosInteres', this)">${concursante.datosInteres || ''}</td>
            <td>
                ${concursante.cuestionarioId ? `<a href="cuestionarios.html?id=${concursante.cuestionarioId}">${concursante.cuestionarioId}</a>` : ''}
            </td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'fecha', this)">${concursante.fecha || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'lugar', this)">${concursante.lugar || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'guionista', this)">${concursante.guionista || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'resultado', this)">${concursante.resultado || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'notasGrabacion', this)">${concursante.notasGrabacion || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'editor', this)">${concursante.editor || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'notasEdicion', this)">${concursante.notasEdicion || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'duracion', this)">${concursante.duracion || ''}</td>
            <td>
                ${concursante.programa && concursante.programa.id ? `<a href="programas.html?id=${concursante.programa.id}">${concursante.programa.id}</a>` : ''}
            </td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'ordenPrograma', this)">${concursante.ordenPrograma || ''}</td>
            <td ondblclick="editarCeldaConcursante(${concursante.id}, 'estado', this)">${concursante.estado || ''}</td>
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
        document.getElementById('nombre-concursante').value = concursanteActual.nombre;
        document.getElementById('edad-concursante').value = concursanteActual.edad || '';
        document.getElementById('fecha-concursante').value = concursanteActual.fecha || '';
        document.getElementById('lugar-concursante').value = concursanteActual.lugar || '';
        document.getElementById('datos-interes').value = concursanteActual.datosInteres || '';
        document.getElementById('cuestionario-id').value = concursanteActual.cuestionarioId || '';
        document.getElementById('guionista').value = concursanteActual.guionista || '';
        document.getElementById('resultado').value = concursanteActual.resultado || '';
        document.getElementById('notas-grabacion').value = concursanteActual.notasGrabacion || '';
        document.getElementById('editor').value = concursanteActual.editor || '';
        document.getElementById('duracion').value = concursanteActual.duracion || '';
        document.getElementById('notas-edicion').value = concursanteActual.notasEdicion || '';
        document.getElementById('programa-id').value = concursanteActual.programa ? concursanteActual.programa.id : '';
        document.getElementById('orden-programa').value = concursanteActual.ordenPrograma || '';
        const modal = new bootstrap.Modal(document.getElementById('modal-concursante'));
        modal.show();
    } catch (error) {
        mostrarError('Error al cargar concursante: ' + error.message);
    }
}

// Previsualización de imagen en el formulario
$(document).on('change', '#imagen-concursante', function(e) {
    const input = e.target;
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = function(ev) {
            $('#preview-imagen-concursante').attr('src', ev.target.result).show();
        };
        reader.readAsDataURL(input.files[0]);
    } else {
        $('#preview-imagen-concursante').hide();
    }
});

// Drag&Drop sobre la previsualización de imagen
$(document).ready(function() {
    const preview = document.getElementById('preview-imagen-concursante');
    const fileInput = document.getElementById('imagen-concursante');
    if (preview) {
        preview.addEventListener('dragover', function(e) {
            e.preventDefault();
            preview.style.border = '2px dashed #007bff';
        });
        preview.addEventListener('dragleave', function(e) {
            e.preventDefault();
            preview.style.border = '';
        });
        preview.addEventListener('drop', function(e) {
            e.preventDefault();
            preview.style.border = '';
            if (e.dataTransfer.files && e.dataTransfer.files[0]) {
                const file = e.dataTransfer.files[0];
                if (!file.type.startsWith('image/')) {
                    alert('Solo se permiten imágenes');
                    return;
                }
                // Asignar archivo al input file
                fileInput.files = e.dataTransfer.files;
                // Lanzar evento change para actualizar previsualización
                const event = new Event('change', { bubbles: true });
                fileInput.dispatchEvent(event);
            }
        });
    }
});

// Guardar concursante con imagen en base64 y gestión de errores mejorada
async function guardarConcursante() {
    const form = document.getElementById('form-concursante');
    const fileInput = document.getElementById('imagen-concursante');
    // Recoge todos los campos del formulario
    const datosConcursante = {
        id: document.getElementById('concursante-id').value || null,
        nombre: document.getElementById('nombre-concursante').value,
        edad: document.getElementById('edad-concursante').value || null,
        fecha: document.getElementById('fecha-concursante').value || null,
        lugar: document.getElementById('lugar-concursante').value || null,
        datosInteres: document.getElementById('datos-interes').value || null,
        cuestionarioId: document.getElementById('cuestionario-id').value || null,
        guionista: document.getElementById('guionista').value || null,
        resultado: document.getElementById('resultado').value || null,
        notasGrabacion: document.getElementById('notas-grabacion').value || null,
        editor: document.getElementById('editor').value || null,
        duracion: document.getElementById('duracion').value || null,
        notasEdicion: document.getElementById('notas-edicion').value || null,
        programaId: document.getElementById('programa-id').value || null,
        ordenPrograma: document.getElementById('orden-programa').value || null,
        estado: null, // El estado se gestiona aparte
        imagen: null
    };
    // Si hay imagen, conviértela a base64
    if (fileInput && fileInput.files && fileInput.files[0]) {
        const file = fileInput.files[0];
        datosConcursante.imagen = await new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = function(ev) {
                resolve(ev.target.result);
            };
            reader.onerror = function() {
                reject('Error al leer la imagen');
            };
            reader.readAsDataURL(file);
        });
    }
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
    let input = document.createElement('input');
    input.type = 'text';
    input.value = valorOriginal;
    input.className = 'form-control form-control-sm';
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
    const nuevoValor = input.value;
    if (nuevoValor === valorOriginal) {
        td.innerHTML = valorOriginal;
        return;
    }
    try {
        const concursante = concursantes.find(c => c.id === id);
        if (!concursante) return;
        concursante[campo] = nuevoValor;
        // Si el campo es programa, buscar el objeto programa
        if (campo === 'programa') {
            const prog = programas.find(p => p.id == nuevoValor);
            concursante.programa = prog ? { id: prog.id } : null;
        }
        await apiManager.put(`/api/concursantes/${id}`, concursante);
        await cargarConcursantes();
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
    buscarCuestionariosModal();
    const modal = new bootstrap.Modal(document.getElementById('modal-selector-cuestionario'));
    modal.show();
}

async function buscarCuestionariosModal() {
    const filtro = document.getElementById('buscador-cuestionario').value.trim();
    let cuestionarios = [];
    try {
        cuestionarios = await apiManager.get('/api/cuestionarios/por-estado/creado');
    } catch (e) {
        mostrarError('Error al buscar cuestionarios: ' + e.message);
        return;
    }
    if (filtro) {
        cuestionarios = cuestionarios.filter(c => c.id.toString().includes(filtro));
    }
    const tbody = document.getElementById('tabla-selector-cuestionario');
    tbody.innerHTML = '';
    if (!cuestionarios.length) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center">No hay cuestionarios disponibles</td></tr>';
        return;
    }
    cuestionarios.forEach(c => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${c.id}</td>
            <td>${c.estado}</td>
            <td>${c.fechaCreacion ? Utils.formatearFecha(c.fechaCreacion) : ''}</td>
            <td><button class="btn btn-sm btn-success" onclick="seleccionarCuestionarioModal(${c.id})">Seleccionar</button></td>
        `;
        tbody.appendChild(tr);
    });
}

function seleccionarCuestionarioModal(id) {
    document.getElementById('cuestionario-id').value = id;
    const modal = bootstrap.Modal.getInstance(document.getElementById('modal-selector-cuestionario'));
    modal.hide();
}

function limpiarSelectorCuestionario() {
    document.getElementById('cuestionario-id').value = '';
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

// Función para abrir el selector de archivos al hacer doble click en la celda de imagen
function abrirSelectorImagen(id) {
    // Crear un input file temporal
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.style.display = 'none';
    document.body.appendChild(input);
    input.addEventListener('change', async function(e) {
        if (input.files && input.files[0]) {
            const file = input.files[0];
            const reader = new FileReader();
            reader.onload = async function(ev) {
                // Actualizar la imagen del concursante
                const concursante = concursantes.find(c => c.id === id);
                if (concursante) {
                    concursante.imagen = ev.target.result;
                    await apiManager.put(`/api/concursantes/${id}`, concursante);
                    await cargarConcursantes();
                }
            };
            reader.readAsDataURL(file);
        }
        document.body.removeChild(input);
    });
    input.click();
}

window.cambiarPassword = function() {
    document.getElementById('form-cambiar-password').reset();
    const modal = new bootstrap.Modal(document.getElementById('modal-cambiar-password'));
    modal.show();
}; 