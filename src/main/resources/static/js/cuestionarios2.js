// Cuestionarios Vista 2 - Formato Tarjetas
let cuestionarios = [];
let cuestionariosFiltrados = [];

// Cargar cuestionarios al iniciar
document.addEventListener('DOMContentLoaded', function() {
    cargarCuestionarios();
});

async function cargarCuestionarios() {
    try {
        const response = await fetch('/api/cuestionarios', {
            headers: getAuthHeaders()
        });
        
        if (response.ok) {
            cuestionarios = await response.json();
            cuestionariosFiltrados = [...cuestionarios];
            renderizarCuestionarios();
            actualizarEstadisticas();
        } else {
            mostrarError('Error al cargar cuestionarios');
        }
    } catch (error) {
        console.error('Error:', error);
        mostrarError('Error de conexión al cargar cuestionarios');
    }
}

function renderizarCuestionarios() {
    const grid = document.getElementById('cuestionarios-grid');
    
    if (cuestionariosFiltrados.length === 0) {
        grid.innerHTML = `
            <div class="col-12">
                <div class="text-center py-5">
                    <i class="fas fa-inbox fa-4x text-muted mb-3"></i>
                    <h4 class="text-muted">No se encontraron cuestionarios</h4>
                    <p class="text-muted">Intenta modificar los filtros de búsqueda</p>
                </div>
            </div>
        `;
        return;
    }

    let html = '';
    cuestionariosFiltrados.forEach(cuestionario => {
        html += crearTarjetaCuestionario(cuestionario);
    });
    
    grid.innerHTML = html;
}

function crearTarjetaCuestionario(cuestionario) {
    const estadoClass = `estado-${cuestionario.estado || 'borrador'}`;
    const estadoTexto = Utils.formatearEstadoCuestionario(cuestionario.estado || 'borrador');
    const fechaCreacion = cuestionario.fechaCreacion ? 
        new Date(cuestionario.fechaCreacion).toLocaleDateString('es-ES') : 
        'No especificada';
    
    const totalPreguntas = cuestionario.preguntas ? cuestionario.preguntas.length : 0;
    const tematica = cuestionario.tematica || 'Genérico';

    return `
        <div class="cuestionario-card">
            <div class="card-header-custom">
                <div class="estado-badge ${estadoClass}">${estadoTexto}</div>
                <div class="cuestionario-numero">#${cuestionario.id}</div>
                
                <div class="tematica-title">${tematica}</div>
                
                <div class="preguntas-info">
                    <i class="fas fa-question-circle fa-2x"></i>
                    <div>
                        <div style="font-size: 1.2rem; font-weight: bold;">${totalPreguntas} preguntas</div>
                        <div class="fecha-info">
                            <i class="fas fa-calendar-alt"></i> ${fechaCreacion}
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="acciones-card">
                <div class="d-flex flex-wrap gap-1">
                    ${generarBotonesAcciones(cuestionario)}
                </div>
                <div>
                    <button class="btn btn-outline-primary btn-accion" onclick="verDetallesCuestionario(${cuestionario.id})" title="Ver detalles">
                        <i class="fas fa-eye"></i>
                    </button>
                </div>
            </div>
        </div>
    `;
}

function generarBotonesAcciones(cuestionario) {
    let botones = '';
    
    // Botón editar - siempre disponible para borradores
    if (cuestionario.estado === 'borrador' || !cuestionario.estado) {
        botones += `
            <button class="btn btn-outline-warning btn-accion" onclick="editarCuestionario(${cuestionario.id})" title="Editar">
                <i class="fas fa-edit"></i>
            </button>
        `;
    }
    
    // Botones de cambio de estado
    switch(cuestionario.estado) {
        case 'borrador':
            botones += `
                <button class="btn btn-success btn-accion" onclick="cambiarEstado(${cuestionario.id}, 'creado')" title="Marcar como Creado">
                    <i class="fas fa-check"></i> Crear
                </button>
            `;
            break;
        case 'creado':
            botones += `
                <button class="btn btn-info btn-accion" onclick="cambiarEstado(${cuestionario.id}, 'adjudicado')" title="Adjudicar">
                    <i class="fas fa-user-tag"></i> Adjudicar
                </button>
            `;
            break;
        case 'adjudicado':
            botones += `
                <button class="btn btn-purple btn-accion" onclick="cambiarEstado(${cuestionario.id}, 'grabado')" title="Marcar como Grabado">
                    <i class="fas fa-video"></i> Grabar
                </button>
            `;
            break;
        case 'asignado_jornada':
            // Solo mostrar información, sin botones de cambio (es automático)
            botones += `
                <span class="badge bg-warning text-dark" title="Asignado automáticamente a una jornada">
                    <i class="fas fa-calendar"></i> En Jornada
                </span>
            `;
            break;
        case 'asignado_concursantes':
            // Solo mostrar información, sin botones de cambio (es automático)
            botones += `
                <span class="badge bg-dark" title="Asignado automáticamente a concursantes">
                    <i class="fas fa-users"></i> Con Concursantes
                </span>
            `;
            break;
    }
    
    // Botón eliminar para borradores
    if (cuestionario.estado === 'borrador' || !cuestionario.estado) {
        botones += `
            <button class="btn btn-outline-danger btn-accion" onclick="eliminarCuestionario(${cuestionario.id})" title="Eliminar">
                <i class="fas fa-trash"></i>
            </button>
        `;
    }
    
    return botones;
}

function actualizarEstadisticas() {
    const stats = {
        total: cuestionarios.length,
        borrador: cuestionarios.filter(c => !c.estado || c.estado === 'borrador').length,
        creado: cuestionarios.filter(c => c.estado === 'creado').length,
        adjudicado: cuestionarios.filter(c => c.estado === 'adjudicado').length,
        grabado: cuestionarios.filter(c => c.estado === 'grabado').length
    };
    
    document.getElementById('total-cuestionarios').textContent = stats.total;
    document.getElementById('cuestionarios-borrador').textContent = stats.borrador;
    document.getElementById('cuestionarios-creado').textContent = stats.creado;
    document.getElementById('cuestionarios-adjudicado').textContent = stats.adjudicado;
    document.getElementById('cuestionarios-grabado').textContent = stats.grabado;
}

function filtrarCuestionarios() {
    const busqueda = document.getElementById('buscar-cuestionario').value.toLowerCase();
    const estado = document.getElementById('filtro-estado-cuestionario').value;
    const tematica = document.getElementById('filtro-tematica-cuestionario').value;
    
    cuestionariosFiltrados = cuestionarios.filter(cuestionario => {
        const coincideBusqueda = !busqueda || 
            cuestionario.id.toString().includes(busqueda) ||
            (cuestionario.tematica && cuestionario.tematica.toLowerCase().includes(busqueda)) ||
            (cuestionario.estado && cuestionario.estado.toLowerCase().includes(busqueda));
            
        const coincideEstado = !estado || 
            (cuestionario.estado || 'borrador') === estado;
            
        const coincideTematica = !tematica || 
            (cuestionario.tematica || '') === tematica;
            
        return coincideBusqueda && coincideEstado && coincideTematica;
    });
    
    renderizarCuestionarios();
}

function limpiarFiltrosCuestionarios() {
    document.getElementById('buscar-cuestionario').value = '';
    document.getElementById('filtro-estado-cuestionario').value = '';
    document.getElementById('filtro-tematica-cuestionario').value = '';
    cuestionariosFiltrados = [...cuestionarios];
    renderizarCuestionarios();
}

async function cambiarEstado(id, nuevoEstado) {
    if (!confirm(`¿Estás seguro de cambiar el estado a "${nuevoEstado}"?`)) {
        return;
    }
    
    try {
        const response = await fetch(`/api/cuestionarios/${id}/estado`, {
            method: 'PUT',
            headers: {
                ...getAuthHeaders(),
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ estado: nuevoEstado })
        });
        
        if (response.ok) {
            mostrarExito(`Estado cambiado a ${nuevoEstado}`);
            cargarCuestionarios();
        } else {
            const errorText = await response.text();
            mostrarError(`Error al cambiar estado: ${errorText}`);
        }
    } catch (error) {
        console.error('Error:', error);
        mostrarError('Error de conexión al cambiar estado');
    }
}

async function eliminarCuestionario(id) {
    if (!confirm('¿Estás seguro de que quieres eliminar este cuestionario?')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/cuestionarios/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });
        
        if (response.ok) {
            mostrarExito('Cuestionario eliminado correctamente');
            cargarCuestionarios();
        } else {
            const errorText = await response.text();
            mostrarError(`Error al eliminar: ${errorText}`);
        }
    } catch (error) {
        console.error('Error:', error);
        mostrarError('Error de conexión al eliminar cuestionario');
    }
}

function verDetallesCuestionario(id) {
    // Abrir modal o página de detalles
    window.open(`cuestionarios.html?id=${id}`, '_blank');
}

function editarCuestionario(id) {
    // Redirigir a la vista de edición
    window.location.href = `cuestionarios.html?edit=${id}`;
}

function mostrarFormularioCuestionario() {
    // Redirigir a la vista original para crear
    window.location.href = 'cuestionarios.html?new=true';
}

// Función auxiliar para obtener headers de autenticación
function getAuthHeaders() {
    const token = localStorage.getItem('token');
    return {
        'Authorization': token ? (token.startsWith('Bearer ') ? token : 'Bearer ' + token) : '',
        'Content-Type': 'application/json'
    };
} 