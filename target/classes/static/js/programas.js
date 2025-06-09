// programas.js

let programas = [];
let concursantesPorPrograma = {};

async function inicializarProgramas() {
    await cargarProgramas();
}

async function cargarProgramas() {
    try {
        programas = await apiManager.get('/api/programas');
        await cargarConcursantesPorPrograma();
        mostrarProgramas();
    } catch (error) {
        if (error && error.message && error.message.startsWith('401')) {
            // No mostrar mensaje, la redirección ya ocurre en api.js
            return;
        }
        mostrarError('Error al cargar programas: ' + error.message);
    }
}

async function cargarConcursantesPorPrograma() {
    concursantesPorPrograma = {};
    for (const programa of programas) {
        try {
            const concursantes = await apiManager.get(`/api/concursantes/programa/${programa.id}`);
            concursantesPorPrograma[programa.id] = concursantes;
        } catch (e) {
            concursantesPorPrograma[programa.id] = [];
        }
    }
}

function mostrarProgramas() {
    const tbody = document.getElementById('tabla-programas');
    tbody.innerHTML = programas.map(programa => {
        const concursantes = concursantesPorPrograma[programa.id] || [];
        return `
            <tr class="table-primary" data-id="${programa.id}">
                <td>${programa.id}</td>
                <td>${programa.fechaEmision || '-'}</td>
                <td>${programa.estado || '-'}</td>
                <td>${concursantes.length}</td>
                <td>${programa.duracionAcumulada || '-'}</td>
                <td><!-- Acciones futuras --></td>
            </tr>
            <tr><td colspan="6">
                <div class="table-responsive">
                    <table class="table table-excel table-preguntas mb-0">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Nombre</th>
                                <th>Imagen</th>
                                <th>Edad</th>
                                <th>Datos de Interés</th>
                                <th>Nº Cuestionario</th>
                                <th>Fecha</th>
                                <th>Lugar</th>
                                <th>Guionista</th>
                                <th>Resultado</th>
                                <th>Notas Grabación</th>
                                <th>Editor</th>
                                <th>Notas Edición</th>
                                <th>Duración</th>
                                <th>Programa</th>
                                <th>Orden</th>
                                <th>Estado</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${concursantes.map(c => `
                                <tr class="concursante-row" data-id="${c.id}" style="cursor:pointer;">
                                    <td>${c.id ?? ''}</td>
                                    <td>${c.nombre ?? ''}</td>
                                    <td>${c.imagen ? `<img src='${c.imagen}' alt='Imagen' style='max-width:100px;max-height:100px;object-fit:cover;'/>` : ''}</td>
                                    <td>${c.edad ?? ''}</td>
                                    <td>${c.datosInteres ?? ''}</td>
                                    <td>${c.cuestionarioId ? `<a href='cuestionarios.html?id=${c.cuestionarioId}'>${c.cuestionarioId}</a>` : ''}</td>
                                    <td>${c.fecha ?? ''}</td>
                                    <td>${c.lugar ?? ''}</td>
                                    <td>${c.guionista ?? ''}</td>
                                    <td>${c.resultado ?? ''}</td>
                                    <td>${c.notasGrabacion ?? ''}</td>
                                    <td>${c.editor ?? ''}</td>
                                    <td>${c.notasEdicion ?? ''}</td>
                                    <td>${c.duracion ?? ''}</td>
                                    <td>${c.programa && c.programa.id ? `<a href='programas.html?id=${c.programa.id}'>${c.programa.id}</a>` : ''}</td>
                                    <td>${c.ordenPrograma ?? ''}</td>
                                    <td>${c.estado ?? ''}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                </div>
            </td></tr>
        `;
    }).join('');
    // Añadir evento click a cada fila de concursante
    document.querySelectorAll('.concursante-row').forEach(row => {
        row.addEventListener('click', function() {
            const id = this.getAttribute('data-id');
            window.location.href = `concursantes.html?id=${id}`;
        });
    });
    // Resaltado y scroll si hay id en la URL
    const params = new URLSearchParams(window.location.search);
    const idDestacado = params.get('id');
    if (idDestacado) {
        setTimeout(() => {
            const fila = document.querySelector(`.concursante-row[data-id='${idDestacado}']`);
            if (fila) {
                fila.classList.add('table-warning');
                fila.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        }, 500);
    }
}

async function verConcursantesPrograma(programaId) {
    try {
        const concursantes = await apiManager.get(`/api/concursantes/programa/${programaId}`);
        const lista = document.getElementById('lista-concursantes');
        mostrarConcursantesPrograma(concursantes);
        const modal = new bootstrap.Modal(document.getElementById('modal-ver-concursantes'));
        modal.show();
    } catch (error) {
        mostrarError('Error al cargar concursantes del programa: ' + error.message);
    }
}

// Renderizar concursantes en el modal de programa
function mostrarConcursantesPrograma(concursantes) {
    const html = `
        <div class="table-responsive">
            <table class="table table-excel table-preguntas">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Nombre</th>
                        <th>Imagen</th>
                        <th>Edad</th>
                        <th>Datos de Interés</th>
                        <th>Nº Cuestionario</th>
                        <th>Fecha</th>
                        <th>Lugar</th>
                        <th>Guionista</th>
                        <th>Resultado</th>
                        <th>Notas Grabación</th>
                        <th>Editor</th>
                        <th>Notas Edición</th>
                        <th>Duración</th>
                        <th>Programa</th>
                        <th>Orden</th>
                        <th>Estado</th>
                    </tr>
                </thead>
                <tbody>
                    ${concursantes.map(c => `
                        <tr>
                            <td>${c.id ?? ''}</td>
                            <td>${c.nombre ?? ''}</td>
                            <td>${c.imagen ? `<img src='${c.imagen}' alt='Imagen' style='max-width:100px;max-height:100px;object-fit:cover;'/>` : ''}</td>
                            <td>${c.edad ?? ''}</td>
                            <td>${c.datosInteres ?? ''}</td>
                            <td>${c.cuestionarioId ? `<a href='cuestionarios.html?id=${c.cuestionarioId}'>${c.cuestionarioId}</a>` : ''}</td>
                            <td>${c.fecha ?? ''}</td>
                            <td>${c.lugar ?? ''}</td>
                            <td>${c.guionista ?? ''}</td>
                            <td>${c.resultado ?? ''}</td>
                            <td>${c.notasGrabacion ?? ''}</td>
                            <td>${c.editor ?? ''}</td>
                            <td>${c.notasEdicion ?? ''}</td>
                            <td>${c.duracion ?? ''}</td>
                            <td>${c.programa && c.programa.id ? `<a href='programas.html?id=${c.programa.id}'>${c.programa.id}</a>` : ''}</td>
                            <td>${c.ordenPrograma ?? ''}</td>
                            <td>${c.estado ?? ''}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;
    document.getElementById('lista-concursantes').innerHTML = html;
}

// --- ORDENACIÓN POR COLUMNA ---
function ordenarTablaProgramas(colIndex, tipo = 'string') {
    const tabla = document.getElementById('tabla-programas-principal');
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
    const tabla = document.getElementById('tabla-programas-principal');
    if (tabla) {
        tabla.querySelectorAll('thead th').forEach((th, idx) => {
            th.style.cursor = 'pointer';
            th.onclick = () => ordenarTablaProgramas(idx, th.dataset.tipo || 'string');
        });
    }
}, 500);

// --- AUTO-SCROLL HORIZONTAL EN TABLA DE PROGRAMAS ---
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