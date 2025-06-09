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
        // Sumar duración de concursantes en minutos
        let totalMin = 0;
        concursantes.forEach(c => {
            const val = parseInt(c.duracion);
            if (!isNaN(val)) totalMin += val;
        });
        // Formato horas y minutos
        const horas = Math.floor(totalMin / 60);
        const minutos = totalMin % 60;
        let duracionStr = '';
        if (horas > 0) duracionStr += horas + 'h ';
        duracionStr += minutos + 'm';
        // Formatear fecha
        const fechaEmision = formatearFecha(programa.fechaEmision);
        return `
            <tr class="table-primary" data-id="${programa.id}">
                <td>${programa.id}</td>
                <td>${fechaEmision || '-'}</td>
                <td>${programa.estado || '-'}</td>
                <td>${concursantes.length}</td>
                <td>${duracionStr}</td>
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

function mostrarFormularioPrograma() {
    document.getElementById('modal-programa-titulo').textContent = 'Nuevo Programa';
    document.getElementById('form-programa').reset();
    document.getElementById('programa-id').value = '';
    // Eliminar tabla anterior si existe
    const anterior = document.getElementById('tabla-concursantes-nuevo-programa');
    if (anterior) anterior.remove();
    const anteriorSpan = document.getElementById('duracion-total-nuevo-programa');
    if (anteriorSpan) anteriorSpan.remove();
    const anteriorInput = document.getElementById('input-duracion-total-programa');
    if (anteriorInput) anteriorInput.parentElement.remove();
    // Campo de duración total editable
    document.getElementById('modal-programa').querySelector('.modal-body').insertAdjacentHTML('afterbegin', `
        <div class="mb-3" id="bloque-duracion-total-programa">
            <label class="form-label">Duración total del programa (ej: 1h 5m):</label>
            <input type="text" class="form-control" id="input-duracion-total-programa" placeholder="Ej: 1h 5m">
        </div>
    `);
    // Tabla de concursantes en blanco
    document.getElementById('modal-programa').querySelector('.modal-body').insertAdjacentHTML('beforeend', `
        <div id="tabla-concursantes-nuevo-programa" class="mt-4">
            <h5>Concursantes</h5>
            <table class="table table-excel table-preguntas">
                <thead>
                    <tr>
                        <th>Nombre</th>
                        <th>Edad</th>
                        <th>Duración (min)</th>
                        <th>Acciones</th>
                    </tr>
                </thead>
                <tbody id="tbody-concursantes-nuevo-programa">
                </tbody>
            </table>
            <button class="btn btn-outline-primary" type="button" onclick="agregarFilaConcursanteNuevoPrograma()"><i class="fas fa-plus"></i> Añadir concursante</button>
        </div>
    `);
    // Inicialmente una fila vacía
    agregarFilaConcursanteNuevoPrograma();
    // Evento para recalcular diferencia al cambiar la duración total
    document.getElementById('input-duracion-total-programa').addEventListener('input', calcularDuracionTotalNuevoPrograma);
    const modal = new bootstrap.Modal(document.getElementById('modal-programa'));
    modal.show();
}

function agregarFilaConcursanteNuevoPrograma() {
    const tbody = document.getElementById('tbody-concursantes-nuevo-programa');
    const tr = document.createElement('tr');
    tr.innerHTML = `
        <td><input type="text" class="form-control" placeholder="Nombre"></td>
        <td><input type="number" class="form-control" placeholder="Edad"></td>
        <td><input type="number" class="form-control duracion-concursante" placeholder="Duración (min)"></td>
        <td><button class="btn btn-danger btn-sm" type="button" onclick="this.closest('tr').remove();calcularDuracionTotalNuevoPrograma()"><i class="fas fa-trash"></i></button></td>
    `;
    tbody.appendChild(tr);
    // Recalcular duración al cambiar cualquier input de duración
    tr.querySelector('.duracion-concursante').addEventListener('input', calcularDuracionTotalNuevoPrograma);
    calcularDuracionTotalNuevoPrograma();
}

function calcularDuracionTotalNuevoPrograma() {
    const inputs = document.querySelectorAll('.duracion-concursante');
    let totalMin = 0;
    inputs.forEach(input => {
        const val = parseInt(input.value);
        if (!isNaN(val)) totalMin += val;
    });
    // Formato horas y minutos
    const horas = Math.floor(totalMin / 60);
    const minutos = totalMin % 60;
    let texto = '';
    if (horas > 0) texto += horas + 'h ';
    texto += minutos + 'm';
    // Mostrar suma
    let span = document.getElementById('duracion-total-nuevo-programa');
    if (!span) return;
    span.textContent = 'Duración sumada concursantes: ' + texto;
    // Calcular diferencia con la duración total del programa
    const inputTotal = document.getElementById('input-duracion-total-programa');
    let totalProgramaMin = 0;
    if (inputTotal && inputTotal.value.trim()) {
        // Parsear formato libre tipo "1h 5m", "90m", "2h"
        const regex = /(?:(\d+)\s*h)?\s*(\d+)?\s*m?/i;
        const match = inputTotal.value.trim().match(regex);
        if (match) {
            const h = parseInt(match[1]) || 0;
            const m = parseInt(match[2]) || 0;
            totalProgramaMin = h * 60 + m;
        }
    }
    let diff = totalMin - totalProgramaMin;
    let diffTexto = '';
    if (totalProgramaMin > 0) {
        if (diff > 0) diffTexto = `(+${diff} min)`;
        else if (diff < 0) diffTexto = `(${diff} min)`;
        else diffTexto = '(OK)';
    }
    span.textContent += ' ' + diffTexto;
} 