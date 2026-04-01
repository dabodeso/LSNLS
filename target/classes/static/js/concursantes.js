// Variables globales
let concursantes = [];
let programas = [];
let concursanteActual = null;

// Solo ADMIN y DIRECCION pueden editar concursantes
let puedeEditarConcursantes = false;

// Valoraciones permitidas para guionista y dirección (solo front, no BBDD)
const VALORACIONES_PERMITIDAS = ['1', '1+', '2-', '2', '2+', '3-', '3', '3+'];

function setSelectValoracion(selectId, value) {
    const sel = document.getElementById(selectId);
    if (!sel || sel.tagName !== 'SELECT') return;
    const v = (value || '').trim();
    // Quitar opciones "extra" que no están en la lista (de ediciones anteriores con valor legacy)
    Array.from(sel.options).forEach(opt => {
        if (opt.value !== '' && !VALORACIONES_PERMITIDAS.includes(opt.value)) opt.remove();
    });
    if (v && !VALORACIONES_PERMITIDAS.includes(v)) {
        const opt = document.createElement('option');
        opt.value = v;
        opt.textContent = v;
        sel.appendChild(opt);
    }
    sel.value = v;
}

// Variables de paginación
let paginaActual = 0;
let tamanioPagina = 25;
let totalConcursantes = 0;
let totalPaginas = 0;
let cargando = false;
let lastScrollYConcursantes = 0;
// Jornada activa para filtrar cuestionarios/combos en los selectores (si procede)
let jornadaFiltroSeleccion = null;

// Estado de ordenación (server-side) — por defecto ID ascendente
let sortByConcursantes = 'id';
let sortAscConcursantes = true;

// Paginación de selectores (cuestionario/combo)
let modalCuestPagina = 1;
const modalCuestPorPagina = 25;
let modalComboPagina = 1;
const modalComboPorPagina = 25;

// Configuración de columnas por rol
let configuracionColumnas = {
esDireccion: true,
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
'xusoker': true,
'x': true,
'resultado': true,
'notas-grabacion': true,
'guionista': true,
'valoracion-guionista': true,
'estado': true,
'momentos-destacados': true,
'duracion': true,
'duracion-direccion': true,
'duracion-final': true,
'valoracion-final': true,
'numero-pgm': true,
'orden-escaleta': true,
'bonico': true
}
};

// Funciones de inicialización
async function inicializarConcursantes() {
// Detectar rol del usuario (esto también carga la configuración guardada)
detectarRolUsuario();

// Asegurar que la configuración se haya cargado antes de continuar
// cargarConfiguracionGuardada() ya se llama dentro de detectarRolUsuario(),
// pero la llamamos de nuevo aquí para asegurarnos
cargarConfiguracionGuardada();

await cargarProgramas();
    await cargarConcursantes(true);
setupEventListeners();

// Actualizar encabezados de la tabla según la configuración
actualizarEncabezadosTabla();
}

function detectarRolUsuario() {
// Obtener usuario actual
const usuario = authManager.currentUser;
const rol = usuario && usuario.rol ? usuario.rol.replace('ROLE_', '').toLowerCase() : null;

// Solo ADMIN y DIRECCION pueden editar
puedeEditarConcursantes = (rol === 'admin' || rol === 'direccion');

// Ocultar "Nuevo Concursante" si no puede editar
const btnNuevo = document.getElementById('btn-nuevo-concursante');
if (btnNuevo) btnNuevo.style.display = puedeEditarConcursantes ? '' : 'none';

// Mostrar siempre el botón de configuración
const btnConfig = document.getElementById('btn-config-columnas');
if (btnConfig) {
btnConfig.style.display = 'block';
btnConfig.querySelector('i')?.classList.add('me-1');
}

// Si el usuario es GUION, aplicar configuración de columnas predeterminada
if (rol === 'guion') {
    // Verificar si ya hay configuración guardada
    const configGuardada = localStorage.getItem('configuracionColumnasConcursantes');
    
    // Solo aplicar configuración predeterminada si no hay configuración guardada
    if (!configGuardada) {
        // Configuración predeterminada para GUION: ocultar columnas avanzadas
        configuracionColumnas.columnasVisibles = {
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
            'xusoker': true,
            'x': true,
            'resultado': true,
            'notas-grabacion': true,
            'guionista': true,
            'valoracion-guionista': true,
            'estado': true,
            'momentos-destacados': false,  // Ocultar por defecto
            'duracion': true,
            'duracion-direccion': false,   // Ocultar por defecto
            'duracion-final': false,        // Ocultar por defecto
            'valoracion-final': false,      // Ocultar por defecto
            'numero-pgm': false,            // Ocultar por defecto
            'orden-escaleta': false,        // Ocultar por defecto
            'bonico': false                 // Ocultar por defecto
        };
        // Guardar la configuración predeterminada
        localStorage.setItem('configuracionColumnasConcursantes', JSON.stringify(configuracionColumnas));
    }
}

// Cargar configuración guardada para cualquier usuario
cargarConfiguracionGuardada();
}

function aplicarConfiguracionBasica() { /* sin uso, mantenido por compatibilidad */ }

// Función para limitar estados del formulario según el rol
function limitarEstadosSegunRol() {
    const usuario = authManager.currentUser;
    const rol = usuario && usuario.rol ? usuario.rol.replace('ROLE_', '').toLowerCase() : null;
    const estadoSelect = document.getElementById('estado');
    
    if (!estadoSelect) return;
    
    // Si es GUION, limitar solo a 'borrador' y 'grabado'
    if (rol === 'guion') {
        // Guardar el valor actual
        const valorActual = estadoSelect.value;
        
        // Limpiar opciones existentes
        estadoSelect.innerHTML = '';
        
        // Agregar solo las opciones permitidas
        const opcionVacia = document.createElement('option');
        opcionVacia.value = '';
        opcionVacia.textContent = 'Seleccionar estado';
        estadoSelect.appendChild(opcionVacia);
        
        const opcionBorrador = document.createElement('option');
        opcionBorrador.value = 'borrador';
        opcionBorrador.textContent = 'Borrador';
        estadoSelect.appendChild(opcionBorrador);
        
        const opcionGrabado = document.createElement('option');
        opcionGrabado.value = 'grabado';
        opcionGrabado.textContent = 'Grabado';
        estadoSelect.appendChild(opcionGrabado);
        
        // Restaurar valor si es permitido, sino establecer borrador
        if (valorActual === 'borrador' || valorActual === 'grabado') {
            estadoSelect.value = valorActual;
        } else {
            estadoSelect.value = 'borrador';
        }
    }
}

// Carga de datos
async function cargarConcursantes(resetear = true) {
try {
        lastScrollYConcursantes = window.scrollY || window.pageYOffset || 0;
        // Log de entrada
        console.info('📄 [CONCURSANTES] Cargando', { paginaActual, tamanioPagina, resetear });
        
        if (!authManager.isAuthenticated()) {
            console.error('Usuario no autenticado');
            return;
        }

        if (resetear) {
            // no alterar paginaActual aquí; puede venir fijada por navegación
            concursantes = [];
        }

        cargando = true;
        mostrarEstadoCarga();

        // Obtener filtros del formulario
        const estado = document.getElementById('filtro-estado-concursante')?.value || '';
        const jornadaFiltro = document.getElementById('filtro-jornada-text')?.value || '';
        const lugar = document.getElementById('filtro-lugar')?.value || '';
        const numeroProgramaTxt = document.getElementById('filtro-numero-pgm')?.value || '';
        const duracionFinalMin = document.getElementById('filtro-duracion-final-min')?.value || '';
        const duracionFinalMax = document.getElementById('filtro-duracion-final-max')?.value || '';
        const valoracionFinal = document.getElementById('filtro-valoracion-final')?.value || '';
        const bonico = document.getElementById('filtro-bonico')?.value || '';
        const busqueda = document.getElementById('buscar-concursante')?.value || '';

        const params = new URLSearchParams({
            page: paginaActual,
            size: tamanioPagina,
            sortBy: sortByConcursantes || 'id',
            sortDir: sortAscConcursantes ? 'asc' : 'desc'
        });

        // Agregar filtros a los parámetros si tienen valor
        if (estado) params.append('estado', estado);
        if (jornadaFiltro) params.append('jornada', jornadaFiltro);
        if (lugar) params.append('lugar', lugar);
        if (numeroProgramaTxt) params.append('numeroPrograma', numeroProgramaTxt);
        if (duracionFinalMin) params.append('duracionFinalMin', duracionFinalMin);
        if (duracionFinalMax) params.append('duracionFinalMax', duracionFinalMax);
        if (valoracionFinal) params.append('valoracionFinal', valoracionFinal);
        if (bonico) params.append('bonico', bonico);
        if (busqueda) params.append('busqueda', busqueda);

        const url = `/api/concursantes?${params}`;
        console.info('📄 [CONCURSANTES] Fetch', url, {
            filtros: {
                estado,
                jornadaFiltro,
                lugar,
                numeroProgramaTxt,
                duracionFinalMin,
                duracionFinalMax,
                valoracionFinal,
                bonico,
                busqueda
            },
            sortBy: sortByConcursantes,
            sortDir: sortAscConcursantes ? 'asc' : 'desc'
        });
        const response = await fetch(url, {
            headers: authManager.getAuthHeaders()
        });

        if (!response.ok) {
            let errorText = '';
            try { errorText = await response.text(); } catch {}
            console.error('❌ [CONCURSANTES] HTTP', response.status, errorText);
            throw new Error('Error al cargar los concursantes');
        }

        const data = await response.json();
        console.info('📄 [CONCURSANTES] Resumen respuesta', {
            number: data.number,
            size: data.size,
            totalPages: data.totalPages,
            totalElements: data.totalElements,
            pageContent: Array.isArray(data.content) ? data.content.length : 0
        });
        
        // Para paginación por páginas, siempre mostramos solo la página actual
        concursantes = data.content;
        try {
            console.info('📄 [CONCURSANTES] IDs en página', (concursantes || []).map(c => c && c.id).slice(0, 25));
        } catch {}
        
        totalConcursantes = data.totalElements;
        totalPaginas = data.totalPages;
        paginaActual = data.number;
        
mostrarConcursantes();
        setTimeout(() => { window.scrollTo({ top: lastScrollYConcursantes || 0, behavior: 'auto' }); }, 0);
} catch (error) {
if (error && error.message && error.message.startsWith('401')) {
// No mostrar mensaje, la redirección ya ocurre en api.js
return;
}
        console.error('❌ [CONCURSANTES] Error en carga', error);
        mostrarError('Error al cargar concursantes: ' + error.message);
    } finally {
        cargando = false;
        ocultarEstadoCarga();
        // Actualizar paginación
        actualizarPaginacion();
    }
}

async function cargarMasConcursantes() {
    if (cargando) {
        return;
    }
    
    if (paginaActual >= totalPaginas - 1) {
        return;
    }
    
    // Guardar la posición actual del scroll
    const scrollPosition = window.scrollY;
    
    paginaActual++;
    await cargarConcursantes(false);
    
    // Restaurar la posición del scroll después de cargar los concursantes
    setTimeout(() => {
        window.scrollTo(0, scrollPosition);
    }, 100);
}

function mostrarEstadoCarga() {
    const tbody = document.querySelector('#tabla-concursantes');
    if (tbody) {
        tbody.innerHTML = '<tr><td colspan="26" class="text-center"><i class="fas fa-spinner fa-spin"></i> Cargando concursantes...</td></tr>';
    }
}

function ocultarEstadoCarga() {
    // El estado de carga se oculta automáticamente cuando se muestran los concursantes
}

function actualizarPaginacion() {
    const infoEl = document.getElementById('info-paginacion-concursantes');
    const paginacionEl = document.getElementById('paginacion-concursantes');
    if (!paginacionEl || !infoEl) return;

    // Info "Mostrando X-Y de Z"
    const inicio = totalConcursantes === 0 ? 0 : (paginaActual * tamanioPagina + 1);
    const fin = Math.min((paginaActual + 1) * tamanioPagina, totalConcursantes);
    infoEl.textContent = `Mostrando ${inicio}-${fin} de ${totalConcursantes} concursantes`;

    paginacionEl.innerHTML = '';
    if (totalPaginas <= 1) return;

    // Primera
    const primera = document.createElement('li');
    primera.className = `page-item ${paginaActual === 0 ? 'disabled' : ''}`;
    primera.innerHTML = `<a class="page-link" href="#" onclick="irAPaginaConcursantes(0)">Primera</a>`;
    paginacionEl.appendChild(primera);

    // Anterior
    const anterior = document.createElement('li');
    anterior.className = `page-item ${paginaActual === 0 ? 'disabled' : ''}`;
    anterior.innerHTML = `<a class="page-link" href="#" onclick="irAPaginaConcursantes(${paginaActual - 1})">Anterior</a>`;
    paginacionEl.appendChild(anterior);

    // Rango central (como en preguntas)
const inicioR = Math.max(0, paginaActual - 2);
const finR = Math.min(totalPaginas - 1, paginaActual + 2);
    for (let i = inicioR; i <= finR; i++) {
        const li = document.createElement('li');
        li.className = `page-item ${i === paginaActual ? 'active' : ''}`;
        li.innerHTML = `<a class="page-link" href="#" onclick="irAPaginaConcursantes(${i}); return false;">${i + 1}</a>`;
        paginacionEl.appendChild(li);
    }

    // Siguiente
    const siguiente = document.createElement('li');
    siguiente.className = `page-item ${paginaActual === totalPaginas - 1 ? 'disabled' : ''}`;
    siguiente.innerHTML = `<a class="page-link" href="#" onclick="irAPaginaConcursantes(${paginaActual + 1})">Siguiente</a>`;
    paginacionEl.appendChild(siguiente);

    // Última
    const ultima = document.createElement('li');
    ultima.className = `page-item ${paginaActual === totalPaginas - 1 ? 'disabled' : ''}`;
    ultima.innerHTML = `<a class="page-link" href="#" onclick="irAPaginaConcursantes(${totalPaginas - 1})">Última</a>`;
    paginacionEl.appendChild(ultima);
}

function irAPaginaConcursantes(pagina) {
    if (pagina < 0 || pagina >= totalPaginas || pagina === paginaActual) return;
    paginaActual = pagina;
    cargarConcursantes(true);
}

async function cargarProgramas() {
try {
programas = await apiManager.get('/api/programas');
actualizarSelectProgramas();
actualizarSelectJornadasFiltro();
} catch (error) {
mostrarError('Error al cargar programas: ' + error.message);
}
}

// Funciones de UI
function setupEventListeners() {
const fe = document.getElementById('filtro-estado-concursante');
const fjText = document.getElementById('filtro-jornada-text');
const fl = document.getElementById('filtro-lugar');
const fpgm = document.getElementById('filtro-numero-pgm');
const fdfmin = document.getElementById('filtro-duracion-final-min');
const fdfmax = document.getElementById('filtro-duracion-final-max');
const fvalfin = document.getElementById('filtro-valoracion-final');
const fbon = document.getElementById('filtro-bonico');
const fb = document.getElementById('buscar-concursante');
if (fe) fe.addEventListener('change', filtrarConcursantes);
if (fjText) fjText.addEventListener('keyup', filtrarConcursantes);
if (fl) fl.addEventListener('keyup', filtrarConcursantes);
if (fpgm) fpgm.addEventListener('keyup', filtrarConcursantes);
if (fdfmin) fdfmin.addEventListener('keyup', filtrarConcursantes);
if (fdfmax) fdfmax.addEventListener('keyup', filtrarConcursantes);
if (fvalfin) fvalfin.addEventListener('change', filtrarConcursantes);
if (fbon) fbon.addEventListener('change', filtrarConcursantes);
if (fb) fb.addEventListener('keyup', filtrarConcursantes);
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

async function actualizarSelectJornadasFiltro() {
const select = document.getElementById('filtro-jornada');
if (!select) return;
try {
const data = await apiManager.get('/api/jornadas');
const jornadas = (data && data.datos) ? data.datos : (Array.isArray(data) ? data : []);
select.innerHTML = '<option value="">Todas</option>' +
jornadas.map(j => `<option value="${j.id}">${j.nombre || ('Jornada ' + j.id)}</option>`).join('');
} catch (e) {
console.warn('No se pudieron cargar jornadas para el filtro:', e);
}
}

function mostrarConcursantes(concursantesFiltrados = null) {
const lista = concursantesFiltrados || concursantes;
const tbody = document.getElementById('tabla-concursantes');

    const htmlGenerado = lista.map(concursante => {
const celdas = [];

// ID
if (configuracionColumnas.columnasVisibles['numero-concur']) {
celdas.push(`<td>${concursante.id || ''}</td>`);
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
const cuéstOnclick = concursante.cuestionarioId
    ? `verCuestionario(${concursante.cuestionarioId}, ${concursante.id})`
    : (puedeEditarConcursantes ? `abrirSelectorCuestionarioParaConcursante(${concursante.id})` : '');
const cuéstTitle = concursante.cuestionarioId ? 'Ver cuestionario' : (puedeEditarConcursantes ? 'Seleccionar cuestionario' : '');
celdas.push(`<td ${cuéstOnclick ? `onclick="${cuéstOnclick}"` : ''} style="cursor: ${cuéstOnclick ? 'pointer' : 'default'}; background-color: #f8f9fa;" title="${cuéstTitle}">
               ${concursante.cuestionarioId && concursante.cuestionarioId !== 0 ? `<span class=\"badge bg-primary\">${concursante.cuestionarioId}</span>` : '<em class=\"text-muted\">Sin asignar</em>'}
           </td>`);
}

// COMBO
if (configuracionColumnas.columnasVisibles['combo']) {
    const badgeClass = concursante.comboReciclado ? 'bg-success' : 'bg-warning';
    const comboOnclick = concursante.comboId
        ? `verCombo(${concursante.comboId}, ${concursante.id})`
        : (puedeEditarConcursantes ? `abrirSelectorComboParaConcursante(${concursante.id})` : '');
    const comboTitle = concursante.comboId ? (concursante.comboReciclado ? 'Combo reciclado' : 'Ver combo') : (puedeEditarConcursantes ? 'Seleccionar combo' : '');
    celdas.push(`<td ${comboOnclick ? `onclick="${comboOnclick}"` : ''} style="cursor: ${comboOnclick ? 'pointer' : 'default'}; background-color: #f8f9fa;" title="${comboTitle}">
               ${concursante.comboId && concursante.comboId !== 0 ? `<span class=\"badge ${badgeClass}\">${concursante.comboId}</span>` : '<em class=\"text-muted\">Sin asignar</em>'}
           </td>`);
}

// XUSÓKER
if (configuracionColumnas.columnasVisibles['xusoker']) {
    const opcionesXusoker = [
        '',
        'NO USÓ',
        'CONTINÚE',
        'AL VERRÉS',
        'RECICLA',
        'LLAMADA'
    ];
    const valorActualXusoker = concursante.xusoker || '';
    const htmlOpcionesXusoker = opcionesXusoker.map(v => {
        const selected = v === valorActualXusoker ? ' selected' : '';
        const label = v === '' ? '' : v;
        return `<option value="${v}"${selected}>${label}</option>`;
    }).join('');
    celdas.push(
        `<td>
            <select class="form-select form-select-sm xusoker-select" data-id="${concursante.id}"${puedeEditarConcursantes ? '' : ' disabled'}>
                ${htmlOpcionesXusoker}
            </select>
        </td>`
    );
}

// X
if (configuracionColumnas.columnasVisibles['x']) {
celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'factorX', this)">${concursante.factorX || ''}</td>`);
}

// RESULTADO
if (configuracionColumnas.columnasVisibles['resultado']) {
celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'resultado', this)">${(concursante.resultado !== null && concursante.resultado !== undefined) ? formatEuro(concursante.resultado) : ''}</td>`);
}

// NOTAS GRABACIÓN
if (configuracionColumnas.columnasVisibles['notas-grabacion']) {
            const notas = concursante.notasGrabacion || '';
            const contenidoTrunc = `<div class=\"notas-trunc\">${notas}</div>`;
            celdas.push(`<td ondblclick=\"editarCeldaConcursante(${concursante.id}, 'notasGrabacion', this)\" data-notas=\"${encodeURIComponent(notas)}\" title=\"Doble click para editar\">${contenidoTrunc}</td>`);
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
    const estadosPosibles = ['', 'borrador','grabado','editado','programado','emitido','archivado'];
    const estadoActual = (concursante.estado || '').toLowerCase();
    const opcionesEstado = estadosPosibles.map(e => {
        const selected = e === estadoActual ? ' selected' : '';
        const label = e === '' 
            ? 'Seleccionar estado'
            : e.charAt(0).toUpperCase() + e.slice(1);
        return `<option value="${e}"${selected}>${label}</option>`;
    }).join('');
    celdas.push(
        `<td>
            <select class="form-select form-select-sm estado-select" data-id="${concursante.id}"${puedeEditarConcursantes ? '' : ' disabled'}>
                ${opcionesEstado}
            </select>
        </td>`
    );
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
celdas.push(`<td ondblclick="editarCeldaConcursante(${concursante.id}, 'duracionDireccion', this)">${concursante.duracionDireccion || ''}</td>`);
}

// DUR. FINAL
if (configuracionColumnas.columnasVisibles['duracion-final']) {
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
           ${puedeEditarConcursantes ? `
           <button class="btn btn-sm btn-primary" onclick="editarConcursante(${concursante.id})">
               <i class="fas fa-edit"></i>
           </button>
           <button class="btn btn-sm btn-danger" onclick="eliminarConcursante(${concursante.id})">
               <i class="fas fa-trash"></i>
           </button>` : ''}
       </td>`);

return `<tr data-id="${concursante.id}" oncontextmenu="showContextMenu(event, ${concursante.id}, 'concursante')">
           ${celdas.join('')}
       </tr>`;
    }).join('');
    
    tbody.innerHTML = htmlGenerado;
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
    // Reiniciar paginación y solicitar datos filtrados al backend
    paginaActual = 0;
    cargarConcursantes(true);
}

function limpiarFiltrosConcursantes() {
['filtro-estado-concursante','filtro-programa','filtro-jornada','filtro-valoracion','filtro-lugar','buscar-concursante']
.forEach(id => { const el = document.getElementById(id); if (el) el.value = ''; });
    mostrarConcursantes();
    
    // Reiniciar paginación y cargar desde el servidor
    paginaActual = 0;
    cargarConcursantes(true);
}

function mostrarFormularioConcursante() {
if (!puedeEditarConcursantes) return;
concursanteActual = null;
document.getElementById('modal-concursante-titulo').textContent = 'Nuevo Concursante';
document.getElementById('form-concursante').reset();
document.getElementById('concursante-id').value = '';
document.getElementById('cuestionario-id').value = '';
document.getElementById('combo-id').value = '';
document.getElementById('combo-id').dataset.comboId = '';
// Ocultar botón de reciclar
const btnReciclar = document.getElementById('btn-reciclar-combo');
if (btnReciclar) {
    btnReciclar.style.display = 'none';
    btnReciclar.dataset.comboId = '';
}
// Establecer estado predeterminado a 'borrador'
const estadoElement = document.getElementById('estado');
if (estadoElement) {
    estadoElement.value = 'borrador';
}
// Limitar estados según rol
limitarEstadosSegunRol();
// Cargar jornadas en el desplegable
try { cargarJornadas(); } catch {}
const modal = new bootstrap.Modal(document.getElementById('modal-concursante'));
modal.show();
}

async function editarConcursante(id) {
if (!puedeEditarConcursantes) return;
try {
concursanteActual = await apiManager.get(`/api/concursantes/${id}`);
document.getElementById('modal-concursante-titulo').textContent = 'Editar Concursante';
const form = document.getElementById('form-concursante');
form.reset();

// Campos básicos
document.getElementById('concursante-id').value = concursanteActual.id;

// Asignar jornada al select si existe (cargar solo 5 y seleccionar la del concursante si está)
await cargarJornadas();
const jornadaSel = document.getElementById('jornada-select');
if (jornadaSel) {
    if (concursanteActual.jornadaId) {
        // Si no está en los 5 últimos, intentar cargarla por ID y añadirla
        if (!Array.from(jornadaSel.options).some(o => String(o.value) === String(concursanteActual.jornadaId))) {
            try {
                const j = await apiManager.get(`/api/jornadas/${concursanteActual.jornadaId}`);
                if (j && j.id) {
                    const opt = document.createElement('option');
                    opt.value = j.id;
                    opt.textContent = `${j.nombre || ('Jornada ' + j.id)}${j.fechaJornada ? ' - ' + new Date(j.fechaJornada).toLocaleDateString('es-ES') : ''}`;
                    jornadaSel.insertBefore(opt, jornadaSel.options[1] || null);
                }
            } catch {}
        }
        jornadaSel.value = concursanteActual.jornadaId;
    } else {
        jornadaSel.value = '';
    }
}

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
const comboId = concursanteActual.comboId || '';
if (comboId) {
    // Cargar información del combo para mostrar nombre
    try {
        const comboDetalle = await apiManager.get(`/api/combos/${comboId}`);
        if (comboDetalle && comboDetalle.datos) {
            const combo = comboDetalle.datos;
            document.getElementById('combo-id').value = `Combo #${comboId}`;
            document.getElementById('combo-id').dataset.comboId = comboId;
            // Mostrar botón de reciclar
            const btnReciclar = document.getElementById('btn-reciclar-combo');
            if (btnReciclar) {
                btnReciclar.style.display = 'inline-block';
                btnReciclar.dataset.comboId = comboId;
            }
        } else {
            document.getElementById('combo-id').value = `Combo #${comboId}`;
            document.getElementById('combo-id').dataset.comboId = comboId;
            const btnReciclar = document.getElementById('btn-reciclar-combo');
            if (btnReciclar) {
                btnReciclar.style.display = 'inline-block';
                btnReciclar.dataset.comboId = comboId;
            }
        }
    } catch (e) {
        console.warn('No se pudo cargar detalle del combo:', e);
        document.getElementById('combo-id').value = `Combo #${comboId}`;
        document.getElementById('combo-id').dataset.comboId = comboId;
        const btnReciclar = document.getElementById('btn-reciclar-combo');
        if (btnReciclar) {
            btnReciclar.style.display = 'inline-block';
            btnReciclar.dataset.comboId = comboId;
        }
    }
} else {
    document.getElementById('combo-id').value = '';
    document.getElementById('combo-id').dataset.comboId = '';
    const btnReciclar = document.getElementById('btn-reciclar-combo');
    if (btnReciclar) {
        btnReciclar.style.display = 'none';
        btnReciclar.dataset.comboId = '';
    }
}

document.getElementById('xusoker').value = concursanteActual.xusoker || '';
document.getElementById('factor-x').value = concursanteActual.factorX || '';
document.getElementById('resultado').value = concursanteActual.resultado || '';
document.getElementById('notas-grabacion').value = concursanteActual.notasGrabacion || '';
document.getElementById('guionista').value = concursanteActual.guionista || '';
setSelectValoracion('valoracion-guionista', concursanteActual.valoracionGuionista);
document.getElementById('momentos-destacados').value = concursanteActual.momentosDestacados || '';
        document.getElementById('duracion').value = concursanteActual.duracion || '';
        document.getElementById('duracion-direccion').value = concursanteActual.duracionDireccion || '';
        document.getElementById('duracion-final').value = concursanteActual.duracionFinal || '';
                console.log('⏱️ DEBUG - Valor de duración en modal:', concursanteActual.duracion);
                document.getElementById('duracion').value = concursanteActual.duracion || '';
                document.getElementById('duracion-direccion').value = concursanteActual.duracionDireccion || '';
                document.getElementById('duracion-final').value = concursanteActual.duracionFinal || '';
setSelectValoracion('valoracion-final', concursanteActual.valoracionFinal);
document.getElementById('numero-programa').value = concursanteActual.numeroPrograma || '';
document.getElementById('orden-escaleta').value = concursanteActual.ordenEscaleta || '';
document.getElementById('bonico').value = concursanteActual.bonico || '';

// AÑADIR: campos faltantes
// Estado (si existe el campo en el formulario)
const estadoElement = document.getElementById('estado');
if (estadoElement) {
estadoElement.value = concursanteActual.estado || '';
}

// Limitar estados según rol
limitarEstadosSegunRol();

// Premio (si existe el campo en el formulario)
const premioElement = document.getElementById('premio');
if (premioElement) {
premioElement.value = concursanteActual.premio || '';
}

// Foto (mostrar preview si existe)
if (concursanteActual.foto) {
    const fotoPreview = document.getElementById('foto-preview-img');
    const fotoPreviewDiv = document.getElementById('foto-preview');
    if (fotoPreview) {
        fotoPreview.src = `/uploads/${concursanteActual.foto}`;
        if (fotoPreviewDiv) {
            fotoPreviewDiv.style.display = 'block';
        }
    }
    document.getElementById('foto-nombre').value = concursanteActual.foto;
} else {
    const fotoPreviewDiv = document.getElementById('foto-preview');
    if (fotoPreviewDiv) {
        fotoPreviewDiv.style.display = 'none';
    }
    document.getElementById('foto-nombre').value = '';
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
// Tomar jornada seleccionada del desplegable (prioritario)
jornadaId: (document.getElementById('jornada-select') && document.getElementById('jornada-select').value) ? document.getElementById('jornada-select').value : (esEdicion && concursanteActual ? concursanteActual.jornadaId : null),
diaGrabacion: document.getElementById('dia-grabacion').value || null,
lugar: document.getElementById('lugar-concursante').value || null,
nombre: document.getElementById('nombre-concursante').value,
edad: document.getElementById('edad-concursante').value || null,
ocupacion: document.getElementById('ocupacion').value || null,
redesSociales: document.getElementById('redes-sociales').value || null,
    cuestionarioId: document.getElementById('cuestionario-id').value || null,
    comboId: (document.getElementById('combo-id')?.dataset?.comboId || document.getElementById('combo-id')?.value?.replace(/[^0-9]/g, '')) || null,
    xusoker: document.getElementById('xusoker').value || null,
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

// Debug: payload enviado
try {
    console.info('📝 [GUARDAR] Payload concursante', JSON.parse(JSON.stringify(datosConcursante)));
} catch {}

// Enviar como JSON usando apiManager o fetch
try {
const token = localStorage.getItem('token');
let response;
    const selectedJornadaId = (document.getElementById('jornada-select') && document.getElementById('jornada-select').value) ? document.getElementById('jornada-select').value : '';
    console.info('🔧 [GUARDAR] selectedJornadaId', selectedJornadaId);

    if (esEdicion) {
        // Editar concursante existente (manual do/undo con refresco UI)
        console.info('📤 [GUARDAR] PUT (manual do/undo)', `/api/concursantes/${datosConcursante.id}`);
        const snapshotPrevio = await apiManager.get(`/api/concursantes/${datosConcursante.id}`);
        const doAction = async () => { await apiManager.put(`/api/concursantes/${datosConcursante.id}`, datosConcursante); await cargarConcursantes(true); };
        const undoAction = async () => { await apiManager.put(`/api/concursantes/${datosConcursante.id}`, snapshotPrevio); await cargarConcursantes(true); };
        await doAction();
        if (window.UndoManager) window.UndoManager.record({ do: doAction, undo: undoAction, label: `Actualizar concursante ${datosConcursante.id}` });
        // Simular objeto Response OK para flujo existente
        response = new Response(JSON.stringify({ ok: true }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    } else {
        // Crear nuevo concursante (undoable)
        console.info('📤 [GUARDAR] POST-undoable', '/api/concursantes');
        const creado = await apiManager.postUndoable('/api/concursantes', datosConcursante, {
            label: 'Crear concursante',
            idExtractor: (r) => (r && (r.id || r?.datos?.id || r?.data?.id)) ? (r.id || r?.datos?.id || r?.data?.id) : null,
            deleteEndpointBuilder: (id) => `/api/concursantes/${id}`
        });
        // Construir Response-like para seguir flujo
        response = new Response(JSON.stringify(creado || {}), { status: 200, headers: { 'Content-Type': 'application/json' } });
    }

if (response.ok) {
        console.info('✅ [GUARDAR] Respuesta OK', response.status);
        // Sincronizar jornada explícitamente con endpoints dedicados
        if (esEdicion) {
            const prevJornada = (concursanteActual && concursanteActual.jornadaId) ? String(concursanteActual.jornadaId) : '';
            const nuevaJornada = String(selectedJornadaId || '');
            if (nuevaJornada !== prevJornada) {
                try {
                    if (nuevaJornada) {
                        console.info('🔗 [GUARDAR] Asignando jornada (edición)', { id: datosConcursante.id, nuevaJornada });
                        await apiManager.postUndoable(`/api/concursantes/${datosConcursante.id}/asignar-jornada/${nuevaJornada}`, {}, {
                            label: `Asignar jornada a concursante ${datosConcursante.id}`,
                            idExtractor: () => datosConcursante.id,
                            deleteEndpointBuilder: () => `/api/concursantes/${datosConcursante.id}/desasignar-jornada`
                        });
                    } else {
                        console.info('🔗 [GUARDAR] Desasignando jornada (edición)', { id: datosConcursante.id });
                        await apiManager.deleteUndoable(`/api/concursantes/${datosConcursante.id}/desasignar-jornada`, { label: `Desasignar jornada de concursante ${datosConcursante.id}`, snapshotEndpoint: `/api/concursantes/${datosConcursante.id}` });
                    }
                } catch (e) {
                    console.warn('No se pudo sincronizar la jornada del concursante:', e);
                }
            }
        } else {
            // Creación: obtener ID creado y asignar jornada si procede
            let creadoId = null;
            try {
                const body = await response.clone().json();
                console.info('✅ [GUARDAR] Cuerpo creación', body);
                creadoId = (body && (body.id || body?.datos?.id || body?.data?.id)) ? (body.id || body?.datos?.id || body?.data?.id) : null;
            } catch (e) {
                console.warn('⚠️ [GUARDAR] No se pudo parsear el cuerpo de creación', e);
            }
            if (!creadoId) console.warn('⚠️ [GUARDAR] No se pudo determinar el ID creado');
            if (selectedJornadaId && creadoId) {
                try {
                    console.info('🔗 [GUARDAR] Asignando jornada tras crear', { creadoId, selectedJornadaId });
                    await apiManager.postUndoable(`/api/concursantes/${creadoId}/asignar-jornada/${selectedJornadaId}`, {}, {
                        label: `Asignar jornada a concursante ${creadoId}`,
                        idExtractor: () => creadoId,
                        deleteEndpointBuilder: () => `/api/concursantes/${creadoId}/desasignar-jornada`
                    });
                } catch (e) {
                    console.warn('⚠️ [GUARDAR] Error asignando jornada tras crear', e);
                }
            }
            // Mantener orden por ID; ir a primera página para ver el recién creado
            paginaActual = 0;
            // Guardar el id creado para resaltarlo después
            datosConcursante.id = creadoId;
        }

        // Subir foto si hay un archivo seleccionado
        const fotoInput = document.getElementById('foto-concursante');
        const idParaFoto = datosConcursante.id || (concursanteActual && concursanteActual.id) || null;
        if (fotoInput && fotoInput.files && fotoInput.files.length > 0 && idParaFoto) {
            try {
                await subirFotoConcursante(idParaFoto, fotoInput.files[0]);
                // Limpiar el input de foto después de subir
                fotoInput.value = '';
                document.getElementById('foto-nombre').value = '';
                document.getElementById('foto-preview').style.display = 'none';
            } catch (e) {
                console.warn('Error al subir foto:', e);
                // No bloquear el guardado si falla la foto
            }
        }
        
        mostrarExito(esEdicion ? 'Concursante editado correctamente' : 'Concursante guardado correctamente');
        $('#modal-concursante').modal('hide');
        const idEditado = datosConcursante.id || (concursanteActual && concursanteActual.id) || idParaFoto;
        const paginaAntes = esEdicion ? paginaActual : 0;
        console.info('🔄 [GUARDAR] Recargando lista', { idEditado, paginaAntes, sortByConcursantes, sortAscConcursantes });
        // Recargar y posicionar
        await cargarConcursantes(true);
        paginaActual = paginaAntes;
        setTimeout(() => {
            try {
                const fila = document.querySelector(`#tabla-concursantes tr[data-id='${idEditado}']`);
                if (fila) {
                    fila.classList.add('table-warning');
                    fila.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    setTimeout(() => fila.classList.remove('table-warning'), 2000);
                } else {
                    console.warn('🔍 [GUARDAR] Fila no encontrada tras recarga', { idEditado });
                    // Fallback: obtener por ID e inyectar al inicio para que sea visible
                    (async () => {
                        try {
                            if (!idEditado) return;
                            const creado = await apiManager.get(`/api/concursantes/${idEditado}`);
                            if (creado && creado.id) {
                                // Prepend y re-render
                                concursantes = [creado, ...concursantes.filter(c => c && c.id !== creado.id)];
                                mostrarConcursantes();
                                const fila2 = document.querySelector(`#tabla-concursantes tr[data-id='${idEditado}']`);
                                if (fila2) {
                                    fila2.classList.add('table-warning');
                                    fila2.scrollIntoView({ behavior: 'smooth', block: 'center' });
                                    setTimeout(() => fila2.classList.remove('table-warning'), 2000);
                                }
                            }
                        } catch (e) {
                            console.warn('⚠️ [GUARDAR] Fallback por ID falló', e);
                        }
                    })();
                }
            } catch {}
        }, 100);
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
        console.error('❌ [GUARDAR] Error HTTP', { status: response.status, body: errorText });
mostrarError('Error al guardar concursante: ' + mensaje);
}
} catch (err) {
console.error('❌ [GUARDAR] Excepción', err);
mostrarError('Error de red o inesperado: ' + err);
}
}

// Manejar cambio de estado desde el select en la tabla
$(document).on('change', '.estado-select', async function() {
    const id = $(this).data('id');
    const nuevoEstado = $(this).val();
    const select = this;
    try {
        const snapshot = await apiManager.get(`/api/concursantes/${id}`);
        const estadoPrevio = snapshot && snapshot.estado ? snapshot.estado : null;
        // Usamos el endpoint genérico de actualización de campo
        const doAction = async () => {
            await apiManager.patch(`/api/concursantes/${id}/campo`, { estado: nuevoEstado || null });
            await cargarConcursantes(true);
        };
        const undoAction = async () => {
            if (estadoPrevio !== null) {
                await apiManager.patch(`/api/concursantes/${id}/campo`, { estado: estadoPrevio });
                await cargarConcursantes(true);
            }
        };
        await doAction();
        if (window.UndoManager) window.UndoManager.record({ do: doAction, undo: undoAction, label: `Cambiar estado concursante ${id}` });
    } catch (e) {
        console.error('Error al cambiar estado del concursante:', e);
        // Restaurar valor previo en el select si lo conocemos
        if (typeof estadoPrevio !== 'undefined' && estadoPrevio !== null && select) {
            select.value = (estadoPrevio || '').toLowerCase();
        }
        // Mensaje más explicativo según tipo de error
        const msg = (e && e.message) ? e.message : '';
        if (msg.startsWith('404')) {
            mostrarError('No se ha encontrado el concursante o el servicio de cambio de estado. Es posible que haya sido eliminado o que la URL del servicio haya cambiado.');
        } else if (msg.startsWith('403')) {
            mostrarError('No tienes permisos para cambiar el estado de este concursante.');
        } else {
            mostrarError('Error al cambiar el estado del concursante: ' + msg);
        }
    }
});

// Manejar cambio de XUSÓKER desde el select en la tabla
$(document).on('change', '.xusoker-select', async function() {
    const id = $(this).data('id');
    const nuevoValor = $(this).val() || null;
    const select = this;
    try {
        const snapshot = await apiManager.get(`/api/concursantes/${id}`);
        const valorPrevio = snapshot && snapshot.xusoker ? snapshot.xusoker : null;

        const doAction = async () => {
            await apiManager.patch(`/api/concursantes/${id}/campo`, { xusoker: nuevoValor });
            await cargarConcursantes(true);
        };
        const undoAction = async () => {
            await apiManager.patch(`/api/concursantes/${id}/campo`, { xusoker: valorPrevio });
            await cargarConcursantes(true);
        };

        await doAction();
        if (window.UndoManager) {
            window.UndoManager.record({
                do: doAction,
                undo: undoAction,
                label: `Cambiar XUSÓKER concursante ${id}`
            });
        }
    } catch (e) {
        console.error('Error al cambiar XUSÓKER del concursante:', e);
        if (typeof valorPrevio !== 'undefined' && select) {
            select.value = valorPrevio || '';
        }
        const msg = (e && e.message) ? e.message : '';
        if (msg.startsWith('403')) {
            mostrarError('No tienes permisos para cambiar el XUSÓKER de este concursante.');
        } else {
            mostrarError('Error al cambiar el XUSÓKER del concursante: ' + msg);
        }
    }
});

async function eliminarConcursante(id) {
    if (!puedeEditarConcursantes) return;
    try {
        const c = (concursantes || []).find(x => x && x.id === id);
        if (c && c.jornadaId) {
            mostrarError('No se puede eliminar un concursante con jornada asignada. Desasigna la jornada primero.');
            return;
        }
        if (!confirm('¿Está seguro de que desea eliminar este concursante?')) {
            return;
        }

        // Snapshot previo
        const previo = await apiManager.get(`/api/concursantes/${id}`);
        let idOriginal = id;
        let idRecreado = null;

        const doDelete = async () => {
            const objetivo = idRecreado || idOriginal;
            await apiManager.delete(`/api/concursantes/${objetivo}`);
            await cargarConcursantes(true);
        };

        const undoCreate = async () => {
            try {
                // Reconstruir payload compatible con POST creación
                const payload = {
                    jornadaId: previo?.jornadaId || null,
                    diaGrabacion: previo?.diaGrabacion || null,
                    lugar: previo?.lugar || null,
                    nombre: previo?.nombre || null,
                    edad: previo?.edad || null,
                    ocupacion: previo?.ocupacion || null,
                    redesSociales: previo?.redesSociales || null,
                    cuestionarioId: previo?.cuestionarioId || null,
                    comboId: previo?.comboId || null,
                    factorX: previo?.factorX || null,
                    resultado: previo?.resultado || null,
                    notasGrabacion: previo?.notasGrabacion || null,
                    guionista: previo?.guionista || null,
                    valoracionGuionista: previo?.valoracionGuionista || null,
                    momentosDestacados: previo?.momentosDestacados || null,
                    duracion: previo?.duracion || null,
                    duracionDireccion: previo?.duracionDireccion || null,
                    duracionFinal: previo?.duracionFinal || null,
                    valoracionFinal: previo?.valoracionFinal || null,
                    numeroPrograma: previo?.numeroPrograma || null,
                    ordenEscaleta: previo?.ordenEscaleta || null,
                    bonico: previo?.bonico || null,
                    estado: previo?.estado || null,
                    premio: previo?.premio || null,
                    foto: previo?.foto || null,
                    creditosEspeciales: previo?.creditosEspeciales || null,
                    numeroConcursante: previo?.numeroConcursante || null
                };
                const creado = await apiManager.post('/api/concursantes', payload);
                const nuevoId = (creado && (creado.id || creado?.datos?.id || creado?.data?.id)) ? (creado.id || creado?.datos?.id || creado?.data?.id) : null;
                if (payload.jornadaId && nuevoId) {
                    try { await apiManager.post(`/api/concursantes/${nuevoId}/asignar-jornada/${payload.jornadaId}`, {}); } catch {}
                }
                idRecreado = nuevoId; // Para que REDO elimine el recreado
                await cargarConcursantes(true);
            } catch (e) {
                mostrarError('No se pudo restaurar el concursante: ' + e.message);
            }
        };

        await doDelete();
        if (window.UndoManager) window.UndoManager.record({ do: doDelete, undo: undoCreate, label: `Eliminar concursante ${id}` });
        mostrarExito('Concursante eliminado correctamente');
    } catch (error) {
        mostrarError('Error al eliminar concursante: ' + error.message);
    }
}

function crearSelectValoracion(valorActual) {
    const select = document.createElement('select');
    select.className = 'form-control form-control-sm';
    const optVacio = document.createElement('option');
    optVacio.value = '';
    optVacio.textContent = '—';
    select.appendChild(optVacio);
    VALORACIONES_PERMITIDAS.forEach(v => {
        const opt = document.createElement('option');
        opt.value = v;
        opt.textContent = v;
        if (v === valorActual) opt.selected = true;
        select.appendChild(opt);
    });
    if (!VALORACIONES_PERMITIDAS.includes(valorActual) && valorActual) {
        const optActual = document.createElement('option');
        optActual.value = valorActual;
        optActual.textContent = valorActual;
        optActual.selected = true;
        select.appendChild(optActual);
    }
    return select;
}

async function editarCeldaConcursante(id, campo, td) {
if (!puedeEditarConcursantes) return;
if (td.querySelector('input,select')) return;
const valorOriginal = (td.innerText || '').trim();
let input;

if (campo === 'valoracionGuionista' || campo === 'valoracionFinal') {
    input = crearSelectValoracion(valorOriginal);
} else {
// Input normal para otros campos
input = document.createElement('input');
input.type = 'text';
input.value = valorOriginal;
input.className = 'form-control form-control-sm';

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

if (input.tagName === 'SELECT') {
    input.addEventListener('change', async function() {
        await guardarCeldaConcursante(id, campo, input, td, valorOriginal);
    });
}
}

async function guardarCeldaConcursante(id, campo, input, td, valorOriginal) {
const nuevoValor = (input.tagName === 'SELECT' ? input.value : input.value.trim());
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
            console.log('⏱️ DEBUG - Guardando duración:', campo, 'valor original:', valorOriginal, 'nuevo valor:', nuevoValor);
            
if (nuevoValor === '' || nuevoValor === null) {
valorConvertido = null;
                console.log('⏱️ DEBUG - Duración vacía, estableciendo a null');
} else {
// Validar formato MM:SS
const formatoValido = /^\d{1,3}:\d{2}$/.test(nuevoValor);
if (!formatoValido) {
                    console.log('⏱️ DEBUG - Formato de duración inválido:', nuevoValor);
throw new Error('La duración debe tener formato MM:SS (ej: 25:08)');
}
// Validar que los segundos sean válidos (00-59)
const [minutos, segundos] = nuevoValor.split(':');
if (parseInt(segundos) > 59) {
                    console.log('⏱️ DEBUG - Segundos inválidos:', segundos);
throw new Error('Los segundos deben estar entre 00 y 59');
}
valorConvertido = nuevoValor; // Mantener como string
                console.log('⏱️ DEBUG - Duración validada correctamente:', valorConvertido);
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

        // Construir payloads prev/next para do/undo y refrescar UI sin F5
        const snapshot = await apiManager.get(`/api/concursantes/${id}`);
        const buildPayload = (src) => ({
            id: id,
            jornadaId: src?.jornadaId ?? null,
            diaGrabacion: src?.diaGrabacion ?? null,
            lugar: src?.lugar ?? null,
            nombre: src?.nombre ?? null,
            edad: src?.edad ?? null,
            ocupacion: src?.ocupacion ?? null,
            redesSociales: src?.redesSociales ?? null,
            cuestionarioId: src?.cuestionarioId ?? null,
            comboId: src?.comboId ?? null,
            xusoker: src?.xusoker ?? null,
            factorX: src?.factorX ?? null,
            resultado: src?.resultado ?? null,
            notasGrabacion: src?.notasGrabacion ?? null,
            guionista: src?.guionista ?? null,
            valoracionGuionista: src?.valoracionGuionista ?? null,
            momentosDestacados: src?.momentosDestacados ?? null,
            duracion: src?.duracion ?? null,
            duracionDireccion: src?.duracionDireccion ?? null,
            duracionFinal: src?.duracionFinal ?? null,
            valoracionFinal: src?.valoracionFinal ?? null,
            numeroPrograma: src?.numeroPrograma ?? null,
            ordenEscaleta: src?.ordenEscaleta ?? null,
            bonico: src?.bonico ?? null,
            estado: src?.estado ?? null,
            premio: src?.premio ?? null,
            foto: src?.foto ?? null,
            creditosEspeciales: src?.creditosEspeciales ?? null,
            numeroConcursante: src?.numeroConcursante ?? null
        });

        const prevPayload = buildPayload(snapshot);
        const nextSource = { ...snapshot, [campo]: valorConvertido };
        const nextPayload = buildPayload(nextSource);

        const doAction = async () => { await apiManager.put(`/api/concursantes/${id}`, nextPayload); await cargarConcursantes(true); };
        const undoAction = async () => { await apiManager.put(`/api/concursantes/${id}`, prevPayload); await cargarConcursantes(true); };

        await doAction();
        if (window.UndoManager) window.UndoManager.record({ do: doAction, undo: undoAction, label: `Actualizar concursante ${id} - ${campo}` });
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

// Formateo de euros con separador de miles (punto)
function formatEuro(num) {
    try {
        const n = Number(num);
        if (isNaN(n)) return '';
        return n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.') + ' €';
    } catch { return ''; }
}

// Fecha flexible: acepta string ISO o array [yyyy, mm, dd, HH?, MM?, SS?]
function formatFechaFlexible(f) {
    try {
        if (!f) return '';
        if (typeof f === 'string') {
            return (typeof Utils !== 'undefined' && Utils.formatearFecha) ? Utils.formatearFecha(f) : f;
        }
        if (Array.isArray(f)) {
            const y = f[0];
            const m = (f[1] || 1);
            const d = (f[2] || 1);
            const hh = f[3] || 0;
            const mm = f[4] || 0;
            const ss = f[5] || 0;
            const fecha = new Date(y, m - 1, d, hh, mm, ss);
            if (isNaN(fecha.getTime())) return '';
            const dd = String(fecha.getDate()).padStart(2, '0');
            const mm2 = String(fecha.getMonth() + 1).padStart(2, '0');
            const yyyy = fecha.getFullYear();
            return `${dd}/${mm2}/${yyyy}`;
        }
        return '';
    } catch {
        return '';
    }
}

// Vista previa de Cuestionario
let concursanteParaReemplazo = null;

async function verCuestionario(id, concursanteId) {
    try {
        if (concursanteId) concursanteParaReemplazo = concursanteId;
        console.info('[PREVIEW] Cargando cuestionario', { id, concursanteId });
        const data = await apiManager.get(`/api/cuestionarios/${id}`);
        console.info('[PREVIEW] Cuestionario recibido', { keys: Object.keys(data || {}), preguntasLen: Array.isArray(data?.preguntas) ? data.preguntas.length : 'N/A' });
        document.getElementById('preview-cuestionario-id').textContent = `#${id}`;
        const cont = document.getElementById('preview-cuestionario-contenido');
        if (cont) {
            const items = (data && data.preguntas) ? Array.from(data.preguntas) : [];
            console.info('[PREVIEW] Items cuestionario', items.slice(0, 3));
            const filas = items
                .map(item => item && (item.pregunta ? item.pregunta : (item.pregunta?.pregunta ? item.pregunta : item)))
                .filter(q => q && typeof q.pregunta === 'string' && q.pregunta.trim().length > 0)
                .map(q => {
                    const nivel = (q.nivel && typeof q.nivel === 'string') ? q.nivel : (q.nivel || '');
                    const texto = q.pregunta;
                    const resp = (q.respuesta && typeof q.respuesta === 'string') ? q.respuesta : (q.respuesta || '');
                    return `<tr><td>${nivel || ''}</td><td>${texto}</td><td>${resp || ''}</td></tr>`;
                })
                .join('');
            cont.innerHTML = `
                <table class="table table-sm table-striped">
                    <thead>
                        <tr><th>Nivel</th><th>Pregunta</th><th>Respuesta</th></tr>
                    </thead>
                    <tbody>${filas}</tbody>
                </table>`;
        }
        new bootstrap.Modal(document.getElementById('modal-preview-cuestionario')).show();
    } catch (e) {
        console.error('[PREVIEW] Error cargando cuestionario', e);
        mostrarError('No se pudo cargar el cuestionario: ' + e);
    }
}

// Vista previa de Combo
async function verCombo(id, concursanteId) {
    try {
        if (concursanteId) concursanteParaReemplazo = concursanteId;
        console.info('[PREVIEW] Cargando combo', { id, concursanteId });
        const data = await apiManager.get(`/api/combos/${id}`);
        console.info('[PREVIEW] Combo recibido', { keys: Object.keys(data || {}), preguntasLen: Array.isArray(data?.preguntas) ? data.preguntas.length : 'N/A' });
        document.getElementById('preview-combo-id').textContent = `#${id}`;
        const cont = document.getElementById('preview-combo-contenido');
        if (cont) {
            const items = (data && data.preguntas) ? Array.from(data.preguntas) : [];
            console.info('[PREVIEW] Items combo', items.slice(0, 3));
            const filas = items.map(item => {
                const q = item.pregunta ? item.pregunta : (item.pregunta?.pregunta ? item.pregunta : item);
                const nivel = (q.nivel && typeof q.nivel === 'string') ? q.nivel : (q.nivel || '');
                const texto = (q.pregunta && typeof q.pregunta === 'string') ? q.pregunta : (q.pregunta || '');
                const resp = (q.respuesta && typeof q.respuesta === 'string') ? q.respuesta : (q.respuesta || '');
                const factor = item.factorMultiplicacion || item.factor || '';
                return `<tr><td>${nivel || ''}</td><td>${texto || ''}</td><td>${resp || ''}</td><td>${factor || ''}</td></tr>`;
            }).join('');
            cont.innerHTML = `
                <table class="table table-sm table-striped">
                    <thead>
                        <tr><th>Nivel</th><th>Pregunta</th><th>Respuesta</th><th>Factor</th></tr>
                    </thead>
                    <tbody>${filas}</tbody>
                </table>`;
        }
        new bootstrap.Modal(document.getElementById('modal-preview-combo')).show();
    } catch (e) {
        console.error('[PREVIEW] Error cargando combo', e);
        mostrarError('No se pudo cargar el combo: ' + e);
    }
}

// Reciclaje parcial desde vista previa (Concursantes)
async function iniciarReciclajeParcialDesdePreview() {
    try {
        const idTxt = document.getElementById('preview-combo-id')?.textContent || '';
        const comboId = idTxt.replace('#','').trim();
        if (!comboId) {
            mostrarError('No se pudo determinar el combo');
            return;
        }
        // Cargar preguntas del combo
        const resp = await apiManager.get(`/api/combos/${comboId}/preguntas`);
        if (!(resp && resp.exito && Array.isArray(resp.datos))) {
            mostrarError('No se pudieron cargar las preguntas del combo');
            return;
        }
        const preguntas = resp.datos;
        // Construir un modal ligero para seleccionar la usada
        const html = `
            <div class="modal fade" id="modal-reciclar-desde-preview" tabindex="-1">
              <div class="modal-dialog">
                <div class="modal-content">
                  <div class="modal-header">
                    <h5 class="modal-title">Reciclar combo #${comboId}</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                  </div>
                  <div class="modal-body">
                    <p>¿Qué pregunta se usó?</p>
                    ${preguntas.map((p,i)=>`
                      <div class="form-check">
                        <input class="form-check-input" type="radio" name="preguntaUsada" id="pregUsada${p.id}" value="${p.id}">
                        <label class="form-check-label" for="pregUsada${p.id}">
                          ${i+1}. ${p.pregunta}
                        </label>
                      </div>
                    `).join('')}
                  </div>
                  <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                    <button type="button" class="btn btn-warning" id="btn-confirmar-reciclaje-preview">Confirmar</button>
                  </div>
                </div>
              </div>
            </div>`;
        // Insertar si no existe
        let cont = document.getElementById('modal-reciclar-desde-preview');
        if (cont) cont.parentElement.removeChild(cont);
        const div = document.createElement('div');
        div.innerHTML = html;
        document.body.appendChild(div.firstElementChild);
        const modal = new bootstrap.Modal(document.getElementById('modal-reciclar-desde-preview'));
        modal.show();

        document.getElementById('btn-confirmar-reciclaje-preview').onclick = async () => {
            try {
                const sel = document.querySelector('input[name="preguntaUsada"]:checked');
                if (!sel) { mostrarError('Selecciona la pregunta usada'); return; }
                const preguntaUsadaId = sel.value;
                // Necesitamos la jornada del concursante para el endpoint
                let jornadaId = null;
                if (concursanteActual && concursanteActual.jornadaId) {
                    jornadaId = concursanteActual.jornadaId;
                } else if (concursanteParaReemplazo) {
                    // Si no estamos en edición, obtener el concursante por su ID
                    try {
                        const concursante = await apiManager.get(`/api/concursantes/${concursanteParaReemplazo}`);
                        if (concursante && concursante.jornadaId) {
                            jornadaId = concursante.jornadaId;
                        }
                    } catch (e) {
                        console.error('Error al obtener concursante:', e);
                    }
                }
                if (!jornadaId) { mostrarError('Este concursante no tiene jornada asignada'); return; }
                const r = await apiManager.post(`/api/jornadas/${jornadaId}/reciclar-combo-parcial/${comboId}`, { preguntaUsadaId });
                if (r && r.exito) {
                    Utils.showAlert('Reciclaje parcial aplicado. Otras dos preguntas quedan reciclables.', 'success');
                    bootstrap.Modal.getInstance(document.getElementById('modal-reciclar-desde-preview')).hide();
                    // Cerrar preview y recargar lista para refrescar estados
                    const prev = bootstrap.Modal.getInstance(document.getElementById('modal-preview-combo'));
                    if (prev) prev.hide();
                    await cargarConcursantes(true);
                } else {
                    mostrarError(r?.mensaje || 'No se pudo reciclar el combo');
                }
            } catch (e) {
                mostrarError('Error al reciclar: ' + e.message);
            }
        };
    } catch (e) {
        mostrarError('No se pudo iniciar el reciclaje: ' + e.message);
    }
}

// Reemplazar desde la vista previa
function abrirSelectorCuestionarioDesdePreview() {
    const modal = bootstrap.Modal.getInstance(document.getElementById('modal-preview-cuestionario'));
    if (modal) modal.hide();
    if (concursanteParaReemplazo) {
        abrirSelectorCuestionarioParaConcursante(concursanteParaReemplazo);
    } else {
        abrirSelectorCuestionario();
    }
}

function abrirSelectorComboDesdePreview() {
    const modal = bootstrap.Modal.getInstance(document.getElementById('modal-preview-combo'));
    if (modal) modal.hide();
    if (concursanteParaReemplazo) {
        abrirSelectorComboParaConcursante(concursanteParaReemplazo);
    } else {
        abrirSelectorCombo();
    }
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
// Fijar jornada desde el select del formulario si hay valor
try {
    const sel = document.getElementById('jornada-select');
    jornadaFiltroSeleccion = (sel && sel.value) ? sel.value : null;
} catch { jornadaFiltroSeleccion = null; }
buscarCuestionariosModal();
const modal = new bootstrap.Modal(document.getElementById('modal-selector-cuestionario'));
modal.show();
}

async function buscarCuestionariosModal() {
const filtro = document.getElementById('buscador-cuestionario').value.trim().toLowerCase();
const nivelFiltro = document.getElementById('filtro-nivel-cuestionario').value;

try {
let cuestionarios;
// Si el concursante tiene jornada asignada, limitar a los cuestionarios de esa jornada
if (jornadaFiltroSeleccion) {
    try {
        const jornadaResp = await apiManager.get(`/api/jornadas/${jornadaFiltroSeleccion}`);
        const jornada = (jornadaResp && jornadaResp.datos) ? jornadaResp.datos : jornadaResp;
        const ids = (jornada && Array.isArray(jornada.cuestionarioIds)) ? jornada.cuestionarioIds : [];
        const detalles = [];
        for (const id of ids) {
            try { detalles.push(await apiManager.get(`/api/cuestionarios/${id}`)); } catch {}
        }
        cuestionarios = detalles;
    } catch (e) {
        // Fallback: usar disponibles aprobados
        try {
            cuestionarios = await apiManager.get('/api/cuestionarios/para-asignar?estado=aprobado');
        } catch (_) {
            cuestionarios = await apiManager.get('/api/cuestionarios/para-asignar');
        }
    }
} else {
    try {
        cuestionarios = await apiManager.get('/api/cuestionarios/para-asignar?estado=aprobado');
    } catch (_) {
        cuestionarios = await apiManager.get('/api/cuestionarios/para-asignar');
    }
}

// Solo aprobados si NO estamos filtrando por jornada asignada
if (!jornadaFiltroSeleccion) {
    cuestionarios = cuestionarios.filter(c => c.estado && c.estado.toLowerCase() === 'aprobado');
}

console.info('[SELECTOR][CUEST] Lista base recibida', { total: cuestionarios.length, sample: cuestionarios.slice(0,3) });

// Aplicar filtros
if (nivelFiltro) {
cuestionarios = cuestionarios.filter(c => c.nivel === nivelFiltro);
}

if (filtro) {
cuestionarios = cuestionarios.filter(c => {
if (c.id && c.id.toString().includes(filtro)) return true;
if (c.nivel && c.nivel.toLowerCase().includes(filtro)) return true;
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

// Paginación
const total = cuestionarios.length;
const totalPag = Math.max(1, Math.ceil(total / modalCuestPorPagina));
modalCuestPagina = Math.min(modalCuestPagina, totalPag);
const start = (modalCuestPagina - 1) * modalCuestPorPagina;
const pageItems = cuestionarios.slice(start, start + modalCuestPorPagina);

const tbody = document.getElementById('tabla-selector-cuestionario');
tbody.innerHTML = '';

if (!pageItems.length) {
tbody.innerHTML = '<tr><td colspan="6" class="text-center">No hay cuestionarios disponibles</td></tr>';
} else {
pageItems.forEach(c => {
const tr = document.createElement('tr');
// Cargaremos las preguntas de forma diferida por rendimiento
const resumenId = `resumen-cuestionario-${c.id}`;
const preguntasResumen = `<em id="${resumenId}">Cargando…</em>`;
tr.innerHTML = `
               <td><strong>${c.id}</strong></td>
               <td><span class="badge bg-info">${c.nivel || 'N/A'}</span></td>
               <td><span class="badge ${Utils.getEstadoBadgeClass(c.estado, 'cuestionario')}">${Utils.formatearEstadoCuestionario(c.estado)}</span></td>
               <td>${c.fechaCreacion ? formatFechaFlexible(c.fechaCreacion) : ''}</td>
               <td style="max-width: 300px; font-size: 0.85em;">${preguntasResumen}</td>
               <td><button class="btn btn-sm btn-success" onclick="seleccionarCuestionarioModal(${c.id})">Seleccionar</button></td>
           `;
tbody.appendChild(tr);

// Cargar resumen de preguntas (primeras 3) asíncronamente con logs
setTimeout(async () => {
    try {
        console.info('[SELECTOR][CUEST] Cargando detalle', c.id);
        const det = await apiManager.get(`/api/cuestionarios/${c.id}`);
        const items = Array.isArray(det?.preguntas) ? det.preguntas : [];
        console.info('[SELECTOR][CUEST] Detalle recibido', { id: c.id, len: items.length });
        const nivelOrder = (niv) => {
            if (!niv) return 999;
            const s = String(niv);
            const m = s.match(/(\d+)/);
            return m ? parseInt(m[1], 10) : 999;
        };
        const textos = items
            .map(it => {
                const q = it.pregunta ? it.pregunta : (it.pregunta?.pregunta ? it.pregunta : it);
                return {
                    nivel: q?.nivel,
                    texto: (typeof q?.pregunta === 'string') ? q.pregunta.trim() : ''
                };
            })
            .filter(row => row.texto.length > 0)
            .sort((a,b) => nivelOrder(a.nivel) - nivelOrder(b.nivel))
            .slice(0,4)
            .map(row => {
                const nivelNum = nivelOrder(row.nivel);
                const corta = row.texto.length > 50 ? row.texto.substring(0,50) + '…' : row.texto;
                return corta ? `${isFinite(nivelNum) && nivelNum !== 999 ? nivelNum : ''} ${corta}`.trim() : '';
            })
            .filter(Boolean);
        const el = document.getElementById(resumenId);
        if (el) el.innerHTML = textos.length ? textos.join('<br>') : '<em>Sin preguntas</em>';
    } catch (e) {
        console.error('[SELECTOR][CUEST] Error cargando detalle', c.id, e);
        const el = document.getElementById(resumenId);
        if (el) el.innerHTML = '<em>Error al cargar</em>';
    }
}, 0);
});
}

// Render paginación modal
let pagEl = document.getElementById('paginacion-selector-cuestionario');
if (!pagEl) {
    pagEl = document.createElement('nav');
    pagEl.id = 'paginacion-selector-cuestionario';
    pagEl.className = 'mt-2';
    tbody.parentElement.parentElement.appendChild(pagEl);
}
pagEl.innerHTML = '';
if (totalPag > 1) {
    const ul = document.createElement('ul');
    ul.className = 'pagination pagination-sm mb-0';
    const prev = document.createElement('li');
    prev.className = `page-item ${modalCuestPagina === 1 ? 'disabled' : ''}`;
    prev.innerHTML = `<a class="page-link" href="#" onclick="modalCuestPagina=Math.max(1, modalCuestPagina-1); buscarCuestionariosModal(); return false;">Anterior</a>`;
    ul.appendChild(prev);
    const inicio = Math.max(1, modalCuestPagina - 2);
    const fin = Math.min(totalPag, modalCuestPagina + 2);
    for (let i = inicio; i <= fin; i++) {
        const li = document.createElement('li');
        li.className = `page-item ${i === modalCuestPagina ? 'active' : ''}`;
        li.innerHTML = `<a class="page-link" href="#" onclick="modalCuestPagina=${i}; buscarCuestionariosModal(); return false;">${i}</a>`;
        ul.appendChild(li);
    }
    const next = document.createElement('li');
    next.className = `page-item ${modalCuestPagina === totalPag ? 'disabled' : ''}`;
    next.innerHTML = `<a class="page-link" href="#" onclick="modalCuestPagina=Math.min(${totalPag}, modalCuestPagina+1); buscarCuestionariosModal(); return false;">Siguiente</a>`;
    ul.appendChild(next);
    pagEl.appendChild(ul);
}
} catch (e) {
mostrarError('Error al buscar cuestionarios: ' + e.message);
}
}

// Funciones para asignar desde la tabla
let concursanteParaAsignar = null;

function abrirSelectorCuestionarioParaConcursante(concursanteId) {
concursanteParaAsignar = concursanteId;
modalCuestPagina = 1;
    // Fijar jornada para filtrar según el concursante seleccionado
    try {
        const c = concursantes.find(x => x && x.id === concursanteId);
        jornadaFiltroSeleccion = c && c.jornadaId ? c.jornadaId : null;
    } catch { jornadaFiltroSeleccion = null; }
buscarCuestionariosModal();
const modal = new bootstrap.Modal(document.getElementById('modal-selector-cuestionario'));
modal.show();
}

function abrirSelectorComboParaConcursante(concursanteId) {
concursanteParaAsignar = concursanteId;
modalComboPagina = 1;
    // Fijar jornada para filtrar según el concursante seleccionado
    try {
        const c = concursantes.find(x => x && x.id === concursanteId);
        jornadaFiltroSeleccion = c && c.jornadaId ? c.jornadaId : null;
    } catch { jornadaFiltroSeleccion = null; }
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
await apiManager.putUndoable(`/api/concursantes/${concursanteParaAsignar}`, concursante, { label: `Actualizar concursante ${concursanteParaAsignar}` });
                // Refrescar fila en pantalla sin perder contexto
                const paginaAntes = paginaActual;
                await cargarConcursantes(true);
                paginaActual = paginaAntes;
mostrarExito('Cuestionario asignado correctamente');
}
} catch (error) {
mostrarError('Error al asignar cuestionario: ' + error.message);
}
concursanteParaAsignar = null;
concursanteParaReemplazo = null;
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
await apiManager.putUndoable(`/api/concursantes/${concursanteParaAsignar}`, concursante, { label: `Actualizar concursante ${concursanteParaAsignar}` });
                // Refrescar fila en pantalla sin perder contexto
                const paginaAntes = paginaActual;
                await cargarConcursantes(true);
                paginaActual = paginaAntes;
mostrarExito('Combo asignado correctamente');
}
} catch (error) {
mostrarError('Error al asignar combo: ' + error.message);
}
concursanteParaAsignar = null;
concursanteParaReemplazo = null;
} else {
// Asignar al formulario
document.getElementById('combo-id').value = id;
// Obtener información del combo para mostrar nombre y habilitar botón reciclar
try {
    const comboDetalle = await apiManager.get(`/api/combos/${id}`);
    if (comboDetalle && comboDetalle.datos) {
        const combo = comboDetalle.datos;
        const nombreCombo = `Combo #${id}`;
        document.getElementById('combo-id').value = nombreCombo;
        document.getElementById('combo-id').dataset.comboId = id;
        // Mostrar botón de reciclar
        const btnReciclar = document.getElementById('btn-reciclar-combo');
        if (btnReciclar) {
            btnReciclar.style.display = 'inline-block';
            btnReciclar.dataset.comboId = id;
        }
    }
} catch (e) {
    console.warn('No se pudo cargar detalle del combo:', e);
    // Mostrar botón de reciclar de todas formas
    const btnReciclar = document.getElementById('btn-reciclar-combo');
    if (btnReciclar) {
        btnReciclar.style.display = 'inline-block';
        btnReciclar.dataset.comboId = id;
    }
}
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
// Fijar jornada desde el select del formulario si hay valor
try {
    const sel = document.getElementById('jornada-select');
    jornadaFiltroSeleccion = (sel && sel.value) ? sel.value : null;
} catch { jornadaFiltroSeleccion = null; }
buscarCombosModal();
const modal = new bootstrap.Modal(document.getElementById('modal-selector-combo'));
modal.show();
}

async function buscarCombosModal() {
const filtro = document.getElementById('buscador-combo').value.trim().toLowerCase();

try {
let combos;
// Si el concursante tiene jornada asignada, limitar a los combos de esa jornada
if (jornadaFiltroSeleccion) {
    try {
        const jornadaResp = await apiManager.get(`/api/jornadas/${jornadaFiltroSeleccion}`);
        const jornada = (jornadaResp && jornadaResp.datos) ? jornadaResp.datos : jornadaResp;
        const ids = (jornada && Array.isArray(jornada.comboIds)) ? jornada.comboIds : [];
        const detalles = [];
        for (const id of ids) {
            try { detalles.push(await apiManager.get(`/api/combos/${id}`)); } catch {}
        }
        combos = detalles;
    } catch (e) {
        // Fallback: usar disponibles aprobados
        try {
            combos = await apiManager.get('/api/combos/para-asignar?estado=aprobado');
        } catch (_) {
            combos = await apiManager.get('/api/combos/para-asignar');
        }
    }
} else {
    try {
        combos = await apiManager.get('/api/combos/para-asignar?estado=aprobado');
    } catch (_) {
        combos = await apiManager.get('/api/combos/para-asignar');
    }
}

// Solo aprobados si NO estamos filtrando por jornada asignada
if (!jornadaFiltroSeleccion) {
    combos = combos.filter(c => c.estado && c.estado.toLowerCase() === 'aprobado');
}
console.info('[SELECTOR][COMBO] Lista base recibida', { total: combos.length, sample: combos.slice(0,3) });

// Filtro de búsqueda
if (filtro) {
combos = combos.filter(c => {
if (c.id && c.id.toString().includes(filtro)) return true;
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

// Paginación
const total = combos.length;
const totalPag = Math.max(1, Math.ceil(total / modalComboPorPagina));
modalComboPagina = Math.min(modalComboPagina, totalPag);
const start = (modalComboPagina - 1) * modalComboPorPagina;
const pageItems = combos.slice(start, start + modalComboPorPagina);

const tbody = document.getElementById('tabla-selector-combo');
tbody.innerHTML = '';

if (!pageItems.length) {
tbody.innerHTML = '<tr><td colspan="5" class="text-center">No hay combos disponibles</td></tr>';
} else {
pageItems.forEach(c => {
const tr = document.createElement('tr');
let preguntasResumen = '';
// Diferido
const resumenId = `resumen-combo-${c.id}`;
preguntasResumen = `<em id="${resumenId}">Cargando…</em>`;
tr.innerHTML = `
               <td><strong>${c.id}</strong></td>
               <td><span class="badge ${Utils.getEstadoBadgeClass(c.estado, 'combo')}">${Utils.formatearEstadoCombo(c.estado)}</span></td>
               <td>${c.fechaCreacion ? formatFechaFlexible(c.fechaCreacion) : ''}</td>
               <td style="max-width: 350px; font-size: 0.85em;">${preguntasResumen}</td>
               <td><button class="btn btn-sm btn-success" onclick="seleccionarComboModal(${c.id})">Seleccionar</button></td>
           `;
tbody.appendChild(tr);

// Cargar resumen de preguntas del combo (primeras 3)
setTimeout(async () => {
    try {
        console.info('[SELECTOR][COMBO] Cargando detalle', c.id);
        const det = await apiManager.get(`/api/combos/${c.id}`);
        const items = Array.isArray(det?.preguntas) ? det.preguntas : [];
        console.info('[SELECTOR][COMBO] Detalle recibido', { id: c.id, len: items.length });
        const textos = items.slice(0,3).map(it => {
            const q = it.pregunta ? it.pregunta : (it.pregunta?.pregunta ? it.pregunta : it);
            const nivel = q?.nivel ? String(q.nivel).replace('_','') : '';
            const texto = q?.pregunta || '';
            const corta = texto.length > 40 ? texto.substring(0,40) + '…' : texto;
            const factor = it.factorMultiplicacion || it.factor || '';
            const fx = factor ? `<span class=\"badge bg-warning\">x${factor}</span> ` : '';
            return `${fx}${nivel} ${corta}`.trim();
        }).filter(Boolean);
        const el = document.getElementById(resumenId);
        if (el) el.innerHTML = textos.length ? textos.join('<br>') : '<em>Sin preguntas</em>';
    } catch (e) {
        console.error('[SELECTOR][COMBO] Error cargando detalle', c.id, e);
        const el = document.getElementById(resumenId);
        if (el) el.innerHTML = '<em>Error al cargar</em>';
    }
}, 0);
});
}

// Render paginación modal
let pagEl = document.getElementById('paginacion-selector-combo');
if (!pagEl) {
    pagEl = document.createElement('nav');
    pagEl.id = 'paginacion-selector-combo';
    pagEl.className = 'mt-2';
    tbody.parentElement.parentElement.appendChild(pagEl);
}
pagEl.innerHTML = '';
if (totalPag > 1) {
    const ul = document.createElement('ul');
    ul.className = 'pagination pagination-sm mb-0';
    const prev = document.createElement('li');
    prev.className = `page-item ${modalComboPagina === 1 ? 'disabled' : ''}`;
    prev.innerHTML = `<a class="page-link" href="#" onclick="modalComboPagina=Math.max(1, modalComboPagina-1); buscarCombosModal(); return false;">Anterior</a>`;
    ul.appendChild(prev);
    const inicio = Math.max(1, modalComboPagina - 2);
    const fin = Math.min(totalPag, modalComboPagina + 2);
    for (let i = inicio; i <= fin; i++) {
        const li = document.createElement('li');
        li.className = `page-item ${i === modalComboPagina ? 'active' : ''}`;
        li.innerHTML = `<a class="page-link" href="#" onclick="modalComboPagina=${i}; buscarCombosModal(); return false;">${i}</a>`;
        ul.appendChild(li);
    }
    const next = document.createElement('li');
    next.className = `page-item ${modalComboPagina === totalPag ? 'disabled' : ''}`;
    next.innerHTML = `<a class="page-link" href="#" onclick="modalComboPagina=Math.min(${totalPag}, modalComboPagina+1); buscarCombosModal(); return false;">Siguiente</a>`;
    ul.appendChild(next);
    pagEl.appendChild(ul);
}
} catch (error) {
mostrarError('Error al cargar combos: ' + error.message);
}
}

function limpiarSelectorCombo() {
document.getElementById('combo-id').value = '';
document.getElementById('combo-id').dataset.comboId = '';
const btnReciclar = document.getElementById('btn-reciclar-combo');
if (btnReciclar) {
    btnReciclar.style.display = 'none';
    btnReciclar.dataset.comboId = '';
}
}

// Función para iniciar reciclaje de combo desde el formulario
async function iniciarReciclajeComboDesdeFormulario() {
    const comboIdInput = document.getElementById('combo-id');
    const comboId = comboIdInput?.dataset?.comboId || comboIdInput?.value?.replace(/[^0-9]/g, '') || '';
    
    if (!comboId) {
        mostrarError('No hay un combo seleccionado');
        return;
    }
    
    // Verificar que hay una jornada seleccionada
    let jornadaId = document.getElementById('jornada-select')?.value;
    
    // Si no hay jornada seleccionada en el formulario, intentar obtenerla del concursante actual
    if (!jornadaId) {
        if (concursanteActual && concursanteActual.jornadaId) {
            jornadaId = concursanteActual.jornadaId;
        } else {
            // Si estamos creando un nuevo concursante, necesitamos la jornada
            mostrarError('Debe seleccionar una jornada antes de reciclar el combo');
            return;
        }
    }
    
    try {
        // Cargar preguntas del combo
        const response = await apiManager.get(`/api/combos/${comboId}/preguntas`);
        
        if (!response || !response.exito || !Array.isArray(response.datos)) {
            mostrarError('No se pudieron cargar las preguntas del combo');
            return;
        }
        
        const preguntas = response.datos;
        
        if (preguntas.length !== 3) {
            mostrarError('El combo debe tener exactamente 3 preguntas para reciclaje parcial');
            return;
        }
        
        // Crear y mostrar modal para seleccionar pregunta usada
        mostrarModalReciclajeCombo(comboId, preguntas, jornadaId);
        
    } catch (error) {
        console.error('Error al cargar preguntas del combo:', error);
        mostrarError('Error al cargar preguntas del combo: ' + error.message);
    }
}

// Función para mostrar modal de reciclaje de combo
function mostrarModalReciclajeCombo(comboId, preguntas, jornadaId) {
    // Eliminar modal anterior si existe
    const modalAnterior = document.getElementById('modal-reciclar-combo-formulario');
    if (modalAnterior) {
        modalAnterior.remove();
    }
    
    // Crear modal
    const modalHtml = `
        <div class="modal fade" id="modal-reciclar-combo-formulario" tabindex="-1">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">
                            <i class="fas fa-recycle"></i> Reciclar Combo #${comboId}
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <div class="alert alert-info">
                            <i class="fas fa-info-circle"></i>
                            <strong>Selecciona la pregunta que se usó en este combo:</strong><br>
                            Se creará un nuevo combo con las 2 preguntas restantes.
                        </div>
                        <div id="preguntas-combo-reciclar" class="row">
                            ${preguntas.map((p, i) => {
                                const preguntaTexto = p.pregunta?.pregunta || p.pregunta || 'Sin texto';
                                const respuesta = p.pregunta?.respuesta || p.respuesta || '';
                                return `
                                    <div class="col-md-12 mb-3">
                                        <div class="form-check">
                                            <input class="form-check-input" type="radio" name="preguntaUsadaReciclar" id="pregUsada${p.pregunta?.id || p.id}" value="${p.pregunta?.id || p.id}">
                                            <label class="form-check-label" for="pregUsada${p.pregunta?.id || p.id}">
                                                <strong>Pregunta ${i + 1}:</strong> ${preguntaTexto.substring(0, 100)}${preguntaTexto.length > 100 ? '...' : ''}
                                                ${respuesta ? `<br><small class="text-muted">Respuesta: ${respuesta}</small>` : ''}
                                            </label>
                                        </div>
                                    </div>
                                `;
                            }).join('')}
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                        <button type="button" class="btn btn-warning" id="btn-confirmar-reciclaje-formulario" onclick="confirmarReciclajeComboDesdeFormulario('${comboId}', '${jornadaId}')">
                            <i class="fas fa-check"></i> Confirmar Reciclaje
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    // Insertar modal en el body
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    
    // Mostrar modal
    const modal = new bootstrap.Modal(document.getElementById('modal-reciclar-combo-formulario'));
    modal.show();
    
    // Limpiar al cerrar
    document.getElementById('modal-reciclar-combo-formulario').addEventListener('hidden.bs.modal', function() {
        this.remove();
    });
}

// Función para confirmar reciclaje de combo desde formulario
async function confirmarReciclajeComboDesdeFormulario(comboId, jornadaId) {
    const preguntaSeleccionada = document.querySelector('input[name="preguntaUsadaReciclar"]:checked');
    
    if (!preguntaSeleccionada) {
        mostrarError('Debe seleccionar una pregunta usada');
        return;
    }
    
    const preguntaUsadaId = preguntaSeleccionada.value;
    
    try {
        // Obtener usuario actual
        const usuario = JSON.parse(localStorage.getItem('usuario') || '{}');
        const usuarioId = usuario.id;
        
        if (!usuarioId) {
            mostrarError('No se pudo identificar al usuario');
            return;
        }
        
        // Llamar al endpoint de reciclaje parcial
        const response = await apiManager.post(`/api/jornadas/${jornadaId}/reciclar-combo-parcial/${comboId}`, {
            preguntaUsadaId: preguntaUsadaId
        });
        
        if (response && response.exito) {
            mostrarExito('Combo reciclado correctamente. Se ha creado un nuevo combo con las 2 preguntas restantes.');
            
            // Cerrar modal
            const modal = bootstrap.Modal.getInstance(document.getElementById('modal-reciclar-combo-formulario'));
            if (modal) modal.hide();
            
            // Opcional: actualizar el combo asignado al nuevo combo creado
            // Por ahora, mantenemos el combo original asignado
        } else {
            mostrarError(response?.mensaje || 'Error al reciclar el combo');
        }
    } catch (error) {
        console.error('Error al reciclar combo:', error);
        mostrarError('Error al reciclar el combo: ' + (error.message || error));
    }
}

// --- ORDENACIÓN POR COLUMNA con indicadores ---
function actualizarIndicadoresOrdenamientoConcursantes() {
    const tabla = document.getElementById('tabla-concursantes-header');
    if (!tabla) return;
    const headers = tabla.querySelectorAll('thead th');
    headers.forEach((th, idx) => {
        const indicator = th.querySelector('.sort-indicator');
        if (!indicator) return;
        indicator.className = 'sort-indicator inactive';
        if (tabla.dataset.ordenCol == idx) {
            const isAsc = tabla.dataset.ordenAsc === 'true';
            indicator.className = `sort-indicator active ${isAsc ? 'asc' : 'desc'}`;
        }
    });
}

function ordenarTablaConcursantes(colIndex, tipo = 'string') {
const tabla = document.getElementById('tabla-concursantes-header');
if (!tabla) return;
const asc = tabla.dataset.ordenCol == colIndex ? tabla.dataset.ordenAsc !== 'true' : true;

// Mapear índice a sortBy del backend (encabezados en la tabla de cabecera)
const headerMap = Array.from(tabla.querySelectorAll('thead th')).map(th => th.textContent.trim());
const header = headerMap[colIndex] || '';
const mapSortBy = {
    'ID': 'id',
    'JORNADA': 'jornadaNombre',
    'DÍA GRABACIÓN': 'diaGrabacion',
    'LUGAR': 'lugar',
    'NOMBRE': 'nombre',
    'EDAD': 'edad',
    'OCUPACIÓN': 'ocupacion',
    'RR SS': 'redesSociales',
    'CUEST': 'cuestionarioId',
    'COMBO': 'comboId',
    'X': 'factorX',
    'RESULTADO': 'resultado',
    'NOTAS GRABACIÓN': 'notasGrabacion',
    'GUIONISTA': 'guionista',
    'VALORACIÓN GUIONISTA': 'valoracionGuionista',
    'ESTADO': 'estado',
    'MOMENTOS DESTACADOS': 'momentosDestacados',
    'DURACIÓN': 'duracion',
    'DUR. DIRECCIÓN': 'duracionDireccion',
    'DUR. FINAL': 'duracionFinal',
    'VALORACIÓN FINAL': 'valoracionFinal',
    'Nº PGM': 'numeroPrograma',
    'ORDEN ESCALETA': 'ordenEscaleta',
    'BONICO': 'bonico'
};

sortByConcursantes = mapSortBy[header] || 'id';
sortAscConcursantes = asc;

// Guardar estado visual
tabla.dataset.ordenCol = colIndex;
tabla.dataset.ordenAsc = asc;
actualizarIndicadoresOrdenamientoConcursantes();

// Recargar desde servidor con nuevo sort
paginaActual = 0;
cargarConcursantes(true);
}

// Añadir listeners a los th (cabecera está en tabla-concursantes-header)
setTimeout(() => {
const tabla = document.getElementById('tabla-concursantes-header');
if (tabla) {
tabla.querySelectorAll('thead th').forEach((th, idx) => {
th.style.cursor = 'pointer';
th.onclick = (e) => {
    // Evitar conflicto si se está redimensionando
    if (typeof isTableResizing === 'function' && isTableResizing('tabla-concursantes-header')) return;
    ordenarTablaConcursantes(idx, th.dataset.tipo || 'string');
};
});
actualizarIndicadoresOrdenamientoConcursantes();
}
}, 500);

// --- AUTO-SCROLL HORIZONTAL EN TABLA DE CONCURSANTES ---
function inicializarScrollbarPersonalizadaConcursantes() {
    const container = document.getElementById('tabla-concursantes-body-wrapper');
    const scrollbar = document.getElementById('scrollbar-concursantes');
    const thumb = document.getElementById('thumb-concursantes');
    if (!container || !scrollbar || !thumb) return;

    function actualizarThumb() {
        const scrollWidth = container.scrollWidth;
        const clientWidth = container.clientWidth;
        const maxScrollLeft = Math.max(1, scrollWidth - clientWidth);
        const scrollLeft = container.scrollLeft;
        const thumbWidthPx = Math.max(20, (clientWidth / Math.max(scrollWidth, 1)) * scrollbar.clientWidth);
        const maxThumbLeft = Math.max(0, scrollbar.clientWidth - thumbWidthPx);
        const thumbLeft = (scrollLeft / maxScrollLeft) * maxThumbLeft;
        thumb.style.width = `${thumbWidthPx}px`;
        thumb.style.left = `${thumbLeft}px`;
    }

    function verificarVisibilidad() {
        const rect = container.getBoundingClientRect();
        const isVisible = rect.top < window.innerHeight && rect.bottom > 0;
        scrollbar.style.display = isVisible ? 'block' : 'none';
        if (isVisible) {
            actualizarThumb();
        }
    }

    container.addEventListener('scroll', actualizarThumb);
    window.addEventListener('resize', actualizarThumb);
    window.addEventListener('scroll', verificarVisibilidad);

    // Drag del thumb
    let dragging = false;
    let startX = 0;
    let startLeft = 0;
    thumb.addEventListener('mousedown', function(e) {
        dragging = true;
        startX = e.clientX;
        startLeft = Number.parseFloat(thumb.style.left || '0');
        e.preventDefault();
    });
    document.addEventListener('mousemove', function(e) {
        if (!dragging) return;
        const delta = e.clientX - startX;
        const newLeft = startLeft + delta;
        const maxLeft = Math.max(0, scrollbar.clientWidth - thumb.clientWidth);
        const clampedLeft = Math.max(0, Math.min(newLeft, maxLeft));
        thumb.style.left = `${clampedLeft}px`;

        const maxScrollLeft = Math.max(0, container.scrollWidth - container.clientWidth);
        container.scrollLeft = maxLeft > 0 ? (clampedLeft / maxLeft) * maxScrollLeft : 0;
    });
    document.addEventListener('mouseup', function() { dragging = false; });

    // Click en track
    scrollbar.addEventListener('click', function(e) {
        if (e.target === thumb) return;
        const rect = scrollbar.getBoundingClientRect();
        const clickX = e.clientX - rect.left;
        const targetLeft = clickX - (thumb.clientWidth / 2);
        const maxLeft = Math.max(0, scrollbar.clientWidth - thumb.clientWidth);
        const clampedLeft = Math.max(0, Math.min(targetLeft, maxLeft));
        thumb.style.left = `${clampedLeft}px`;

        const maxScrollLeft = Math.max(0, container.scrollWidth - container.clientWidth);
        container.scrollLeft = maxLeft > 0 ? (clampedLeft / maxLeft) * maxScrollLeft : 0;
    });

    document.body.style.paddingBottom = '16px';
    verificarVisibilidad();
    actualizarThumb();
}

function inicializarCabeceraFlotanteConcursantes() {
    const container = document.getElementById('tabla-concursantes-body-wrapper');
    const table = document.getElementById('tabla-concursantes-header');
    const thead = table ? table.querySelector('thead') : null;
    const floating = document.getElementById('tabla-concursantes-header-floating');
    if (!container || !table || !thead || !floating) return;

    const debugCabecera = (...args) => console.debug('[CABECERA CONCURSANTES]', ...args);

    const cloneTable = document.createElement('table');
    cloneTable.className = table.className;
    const cloneHead = thead.cloneNode(true);
    cloneTable.appendChild(cloneHead);
    floating.innerHTML = '';
    floating.appendChild(cloneTable);

    function getTopOffset() {
        const navbar = document.querySelector('.navbar');
        if (!navbar) {
            debugCabecera('Navbar no encontrada, topOffset=0');
            return 0;
        }
        const rect = navbar.getBoundingClientRect();
        // Si la navbar ya salió de la vista, pegar la cabecera arriba del todo.
        if (rect.bottom <= 0 || rect.top < 0) {
            debugCabecera('Navbar fuera de vista, topOffset=0', { top: rect.top, bottom: rect.bottom });
            return 0;
        }
        const topOffset = Math.max(0, rect.bottom - 2);
        debugCabecera('Navbar visible, topOffset calculado', { top: rect.top, bottom: rect.bottom, topOffset });
        return topOffset;
    }

    function syncWidthsAndPosition() {
        const originalTh = thead.querySelectorAll('th');
        const cloneTh = cloneHead.querySelectorAll('th');
        if (!originalTh.length || originalTh.length !== cloneTh.length) return;

        const containerRect = container.getBoundingClientRect();
        floating.style.left = `${containerRect.left}px`;
        floating.style.width = `${container.clientWidth}px`;
        floating.style.top = `${getTopOffset()}px`;

        debugCabecera('syncWidthsAndPosition', {
            containerLeft: containerRect.left,
            containerTop: containerRect.top,
            containerWidth: container.clientWidth,
            containerScrollLeft: container.scrollLeft,
            floatingTop: floating.style.top
        });

        originalTh.forEach((th, i) => {
            const width = th.getBoundingClientRect().width;
            cloneTh[i].style.width = `${width}px`;
            cloneTh[i].style.minWidth = `${width}px`;
            cloneTh[i].style.maxWidth = `${width}px`;
        });

        cloneTable.style.width = `${table.scrollWidth}px`;
        cloneTable.style.transform = `translateX(${-container.scrollLeft}px)`;
    }

    function updateVisibility() {
        const tableRect = table.getBoundingClientRect();
        const topOffset = getTopOffset();
        const headHeight = thead.getBoundingClientRect().height || 0;
        const mustShow = tableRect.top < topOffset && tableRect.bottom > (topOffset + headHeight + 4);
        debugCabecera('updateVisibility', {
            tableTop: tableRect.top,
            tableBottom: tableRect.bottom,
            topOffset,
            headHeight,
            mustShow,
            scrollY: window.scrollY
        });
        floating.style.display = mustShow ? 'block' : 'none';
        if (mustShow) syncWidthsAndPosition();
    }

    container.addEventListener('scroll', () => {
        debugCabecera('scroll horizontal contenedor', { scrollLeft: container.scrollLeft });
        syncWidthsAndPosition();
    });
    window.addEventListener('resize', () => {
        debugCabecera('window resize');
        syncWidthsAndPosition();
        updateVisibility();
    });
    window.addEventListener('scroll', () => {
        debugCabecera('window scroll', { scrollY: window.scrollY });
        updateVisibility();
    });

    if (typeof ResizeObserver !== 'undefined') {
        const ro = new ResizeObserver(() => {
            debugCabecera('ResizeObserver disparado');
            syncWidthsAndPosition();
            updateVisibility();
        });
        ro.observe(container);
        ro.observe(table);
        ro.observe(thead);
    }

    syncWidthsAndPosition();
    updateVisibility();
    setTimeout(() => {
        debugCabecera('recalculo diferido 150ms');
        syncWidthsAndPosition();
        updateVisibility();
    }, 150);
    setTimeout(() => {
        debugCabecera('recalculo diferido 600ms');
        syncWidthsAndPosition();
        updateVisibility();
    }, 600);
}

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
// Cargar solo 5 jornadas más recientes
const response = await apiManager.get('/api/jornadas?page=0&size=5&sortBy=id&sortDir=desc');
let lista = [];
// Normalizar posibles formatos de respuesta
if (Array.isArray(response)) {
    lista = response;
} else if (response && Array.isArray(response.datos)) {
    lista = response.datos;
} else if (response && response.datos && Array.isArray(response.datos.content)) {
    lista = response.datos.content;
} else if (response && Array.isArray(response.content)) {
    lista = response.content;
} else if (response && Array.isArray(response.jornadas)) {
    lista = response.jornadas;
}
if (!Array.isArray(lista)) lista = [];
jornadas = lista;
const sel = document.getElementById('jornada-select');
if (sel) {
    const valorPrevio = sel.value;
    sel.innerHTML = '<option value="">Sin asignar</option>' +
        jornadas.map(j => `<option value="${j.id}">${j.nombre || ('Jornada ' + j.id)}${j.fechaJornada ? ' - ' + new Date(j.fechaJornada).toLocaleDateString('es-ES') : ''}</option>`).join('');
    if (valorPrevio) sel.value = valorPrevio;
}
} catch (error) {
console.error('Error al cargar jornadas:', error);
}
}

// Buscar una jornada concreta por ID y colocarla en el select
async function buscarJornadaPorId() {
    try {
        const input = document.getElementById('jornada-buscar-id');
        const sel = document.getElementById('jornada-select');
        if (!input || !sel) return;
        const id = (input.value || '').trim();
        if (!id) return;
        const j = await apiManager.get(`/api/jornadas/${id}`);
        if (!j || !j.id) {
            mostrarError('Jornada no encontrada');
            return;
        }
        // Si no está en el select, añadirla al principio
        if (!Array.from(sel.options).some(o => String(o.value) === String(j.id))) {
            const texto = `${j.nombre || ('Jornada ' + j.id)}${j.fechaJornada ? ' - ' + new Date(j.fechaJornada).toLocaleDateString('es-ES') : ''}`;
            const opt = document.createElement('option');
            opt.value = j.id;
            opt.textContent = texto;
            sel.insertBefore(opt, sel.options[1] || null); // tras "Sin asignar"
        }
        sel.value = j.id;
    } catch (e) {
        mostrarError('No se pudo cargar la jornada indicada');
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
await apiManager.postUndoable(`/api/concursantes/${concursanteParaAsignarJornada}/asignar-jornada/${jornadaId}`, {}, {
    label: `Asignar jornada a concursante ${concursanteParaAsignarJornada}`,
    idExtractor: () => concursanteParaAsignarJornada,
    deleteEndpointBuilder: () => `/api/concursantes/${concursanteParaAsignarJornada}/desasignar-jornada`
});
            await cargarConcursantes();
            // Reiniciar paginación y cargar desde el servidor
            paginaActual = 0;
            await cargarConcursantes(true);
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
await apiManager.deleteUndoable(`/api/concursantes/${concursanteParaAsignarJornada}/desasignar-jornada`, { label: `Desasignar jornada de concursante ${concursanteParaAsignarJornada}`, snapshotEndpoint: `/api/concursantes/${concursanteParaAsignarJornada}` });
            await cargarConcursantes();
            // Reiniciar paginación y cargar desde el servidor
            paginaActual = 0;
            await cargarConcursantes(true);
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

// Actualizar la vista de la foto en el formulario si estamos editando
if (concursanteActual && concursanteActual.id === concursanteId) {
    // Actualizar la URL de la foto en el preview
    const fotoPreview = document.getElementById('foto-preview-img');
    const fotoPreviewDiv = document.getElementById('foto-preview');
    if (fotoPreview && resultado && resultado.foto) {
        fotoPreview.src = `/uploads/${resultado.foto}`;
        fotoPreviewDiv.style.display = 'block';
    }
}

// Actualizar la vista de la tabla
        await cargarConcursantes();
        // Reiniciar paginación y cargar desde el servidor
        paginaActual = 0;
        await cargarConcursantes(true);
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
    'xusoker': 'col-xusoker',
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
    'col-xusoker': 'xusoker',
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
const thead = document.querySelector('#tabla-concursantes-header thead tr');
if (!thead) return;

const encabezados = [];

// Mapear columnas a encabezados
const mapeoEncabezados = {
'numero-concur': 'ID',
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
'xusoker': 'XUSÓKER',
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

// Construir encabezados visibles (ordenables con indicador)
Object.keys(mapeoEncabezados).forEach(columna => {
if (configuracionColumnas.columnasVisibles[columna]) {
const esNumero = ['numero-concur','edad','resultado','numero-pgm','orden-escaleta','cuest','combo'].includes(columna);
const attrTipo = esNumero ? ' data-tipo="number"' : '';
encabezados.push(`<th class="sortable-header"${attrTipo}>${mapeoEncabezados[columna]}<span class="sort-indicator inactive"></span></th>`);
}
});

// Añadir encabezado de acciones
encabezados.push('<th>ACCIONES</th>');

thead.innerHTML = encabezados.join('');

// Reasignar listeners de orden y re-inicializar el resizer en la tabla de cabecera
const tablaHeader = document.getElementById('tabla-concursantes-header');
if (tablaHeader) {
tablaHeader.querySelectorAll('thead th').forEach((th, idx) => {
th.style.cursor = 'pointer';
th.onclick = () => {
    if (typeof isTableResizing === 'function' && isTableResizing('tabla-concursantes-header')) return;
    ordenarTablaConcursantes(idx, th.dataset.tipo || 'string');
};
});
if (typeof actualizarIndicadoresOrdenamientoConcursantes === 'function') {
actualizarIndicadoresOrdenamientoConcursantes();
}
try {
    if (typeof TableResizer !== 'undefined') {
        new TableResizer('tabla-concursantes-header', { minWidth: 50, maxWidth: 600 });
    }
} catch (e) {
    console.error('Error al re-inicializar TableResizer en concursantes:', e);
}
}
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
        console.log('⏱️ DEBUG - Configuración guardada en localStorage:', configGuardada);
        
if (configGuardada) {
const config = JSON.parse(configGuardada);
            console.log('⏱️ DEBUG - Configuración parseada:', config);
            
// Cargar configuración guardada si existe
if (config && config.columnasVisibles) {
                console.log('⏱️ DEBUG - Columnas visibles antes de aplicar configuración:', JSON.stringify(configuracionColumnas.columnasVisibles));
                
// Aplicar configuración guardada solo para las columnas que están en la configuración guardada
Object.keys(config.columnasVisibles).forEach(columna => {
if (configuracionColumnas.columnasVisibles.hasOwnProperty(columna)) {
configuracionColumnas.columnasVisibles[columna] = config.columnasVisibles[columna];
}
});
                
                console.log('⏱️ DEBUG - Columnas visibles después de aplicar configuración:', JSON.stringify(configuracionColumnas.columnasVisibles));
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
    
    // Añadir función para mostrar notas completas
    window.mostrarNotasCompletas = function(event, texto) {
        // Evitar que se active el evento de doble clic
        event.preventDefault();
        
        // Si el usuario está haciendo doble clic, no mostrar el modal
        if (event.detail > 1) {
            return;
        }
        
        // Si el usuario está haciendo clic en un input o está editando, no mostrar el modal
        if (event.target.tagName === 'INPUT' || event.target.classList.contains('editing')) {
            return;
        }
        
        // Mostrar el modal con el texto completo
        const contenidoNotas = document.getElementById('contenido-notas');
        if (contenidoNotas) {
            contenidoNotas.textContent = texto;
            const modal = new bootstrap.Modal(document.getElementById('modal-ver-notas'));
            modal.show();
        }
    };

    // Click en icono de expandir (pseudoelemento): delegar al TD completo
    const tablaNotas = document.getElementById('tabla-concursantes');
    if (tablaNotas) {
        tablaNotas.addEventListener('click', function(e) {
            const td = e.target.closest('td');
            if (!td) return;
            const tr = td.parentElement;
            const notasAttr = td.getAttribute('data-notas');
            if (notasAttr && e.offsetX >= td.clientWidth - 24 && e.offsetY >= td.clientHeight - 24) {
                try {
                    const txt = decodeURIComponent(notasAttr);
                    const contenidoNotas = document.getElementById('contenido-notas');
                    if (contenidoNotas) {
                        contenidoNotas.textContent = txt;
                        const modal = new bootstrap.Modal(document.getElementById('modal-ver-notas'));
                        modal.show();
                    }
                } catch (err) {
                    console.error('Error decodificando notas:', err);
                }
            }
        });
    }
}); 