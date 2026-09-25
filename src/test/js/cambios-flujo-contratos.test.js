/**
 * Contratos del flujo concursantes/programas/jornadas (UI + consultas).
 * Se ejecuta con Node desde CambiosFlujoJsContratosTest.
 */
const assert = require('assert');
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const root = path.resolve(__dirname, '../..');
const staticDir = path.join(root, 'main/resources/static');
const jsDir = path.join(staticDir, 'js');

function leer(rel) {
  return fs.readFileSync(path.join(root, rel), 'utf8');
}

const jornadasJs = fs.readFileSync(path.join(jsDir, 'jornadas.js'), 'utf8');
const programasJs = fs.readFileSync(path.join(jsDir, 'programas.js'), 'utf8');
const concursantesJs = fs.readFileSync(path.join(jsDir, 'concursantes.js'), 'utf8');
const combosJs = fs.readFileSync(path.join(jsDir, 'combos.js'), 'utf8');
const programasHtml = fs.readFileSync(path.join(staticDir, 'programas.html'), 'utf8');
const jornadasHtml = fs.readFileSync(path.join(staticDir, 'jornadas.html'), 'utf8');
const concursantesHtml = fs.readFileSync(path.join(staticDir, 'concursantes.html'), 'utf8');
const stylesCss = fs.readFileSync(path.join(staticDir, 'css/styles.css'), 'utf8');
const repoJava = leer('main/java/com/lsnls/repository/ConcursanteRepository.java');
const schemaSql = leer('main/resources/schema.sql');

assert.ok(
  repoJava.includes("LOWER(c.estado) IN ('grabado', 'editado', 'emitido')"),
  'disponibles: grabado, editado o emitido'
);
assert.ok(
  repoJava.includes('c.numeroPrograma IS NULL OR c.numeroPrograma <> :programaId'),
  'disponibles: se puede reasignar un emitido de otro programa'
);
assert.ok(schemaSql.includes('pregunta_usada_id'), 'schema guarda la pregunta usada del combo');

assert.ok(programasJs.includes("'GRABADO', 'EDITADO', 'EMITIDO'"), 'se pueden asignar emitidos');
assert.ok(programasJs.includes("filtroEstado.value = 'editado'"), 'el filtro de disponibles empieza en editado');
assert.ok(programasJs.includes('&programaId='), 'disponibles envía programaId');
assert.ok(programasJs.includes('esProgramaEmitido(programaId)'), 'programa emitido bloquea añadir/quitar');
assert.ok(
  programasHtml.includes('<option value="editado" selected>'),
  'HTML del filtro disponibles arranca en editado'
);
assert.ok(programasHtml.includes('value="emitido"'), 'el filtro incluye emitido');

assert.ok(
  /#modalJornada \.cuestionarios-grid[\s\S]*grid-template-columns:\s*1fr/.test(jornadasHtml),
  'edición de jornada vertical (un slot por fila)'
);
assert.ok(jornadasJs.includes('refrescarModalEdicionJornada'), 'el modal se refresca al añadir varios');
assert.ok(
  jornadasJs.includes('No se puede reciclar: el cuestionario está grabado'),
  'cuestionario asignado: reciclar apagado'
);
assert.ok(
  jornadasJs.includes('No se puede borrar: el cuestionario está asignado a un concursante'),
  'cuestionario asignado: borrar apagado'
);
assert.ok(
  jornadasJs.includes('No se puede borrar: el combo está asignado a un concursante'),
  'combo asignado: borrar apagado'
);
assert.ok(
  jornadasJs.includes('onclick="JornadasManager.reutilizarCombo(${c.id}, ${jornada.id})"'),
  'combo asignado: sí se puede reciclar'
);
assert.ok(jornadasJs.includes('pregunta-combo-usada'), 'pregunta reciclada en rojo en jornadas');
assert.ok(combosJs.includes('pregunta-combo-usada'), 'pregunta reciclada en rojo en combos');
assert.ok(stylesCss.includes('.pregunta-combo-usada'), 'estilo rojo de pregunta usada');
assert.ok(concursantesJs.includes('pregunta-combo-usada'), 'pregunta usada en rojo al ver el combo');

assert.ok(concursantesHtml.includes('id="btn-ver-cuestionario"'), 'botón ver preguntas del cuestionario');
assert.ok(concursantesHtml.includes('id="btn-ver-combo"'), 'botón ver preguntas del combo');
assert.ok(
  !concursantesHtml.includes('id="orden-escaleta"'),
  'orden de escaleta no sale en el formulario de editar'
);
assert.ok(
  concursantesJs.includes("btn.style.display = reciclado ? 'none' : 'inline-block'"),
  'si el combo ya está reciclado se oculta el botón de reciclar'
);
assert.ok(
  /CAMPOS_TABLA_SOLO_DIRECCION[\s\S]*duracionDireccion[\s\S]*duracionFinal[\s\S]*valoracionFinal[\s\S]*numeroPrograma[\s\S]*bonico/.test(concursantesJs),
  'campos de dirección solo los toca Dirección'
);
assert.ok(
  concursantesJs.includes("estadosPosibles = [estadoActual]"),
  'con programa asignado el estado no se toca'
);
assert.ok(
  concursantesJs.includes("['grabado', 'editado', 'archivado']"),
  'sin programa, desde grabado se puede archivar'
);
assert.ok(
  concursantesJs.includes("puedeCrearConcursante = (rol === 'admin' || rol === 'guion' || rol === 'direccion')"),
  'Verificación no crea concursantes'
);
assert.ok(
  /function puedeEditarConcursanteSegunEstado[\s\S]*rol === 'guion'/.test(concursantesJs),
  'Guión solo edita grabado; Verificación no edita'
);

const crearCols = concursantesJs.match(/function crearColumnasVisiblesPorDefecto\([\s\S]*?\n\}/);
const colsSoloDir = concursantesJs.match(/const COLUMNAS_SOLO_DIRECCION = \[[\s\S]*?\];/);
assert.ok(crearCols && colsSoloDir, 'existen defaults de columnas de Dirección');
const sandboxCols = {};
vm.createContext(sandboxCols);
vm.runInContext(colsSoloDir[0] + '\n' + crearCols[0], sandboxCols);
assert.strictEqual(sandboxCols.crearColumnasVisiblesPorDefecto(false)['numero-pgm'], false);
assert.strictEqual(sandboxCols.crearColumnasVisiblesPorDefecto(false)['nombre'], true);
assert.strictEqual(sandboxCols.crearColumnasVisiblesPorDefecto(true)['numero-pgm'], true);

const aplicarRest = concursantesJs.match(/function aplicarRestriccionColumnasDireccion\([\s\S]*?\n\}/);
assert.ok(aplicarRest, 'existe aplicarRestriccionColumnasDireccion');
assert.ok(aplicarRest[0].includes('return false'), 'la restricción ya no oculta columnas de Dirección');
assert.ok(!aplicarRest[0].includes('columnasVisibles'), 'la restricción no pisa la visibilidad elegida');

const cargarModal = concursantesJs.match(/function cargarConfiguracionEnModal\([\s\S]*?\n\}/);
assert.ok(cargarModal, 'existe cargarConfiguracionEnModal');
assert.ok(cargarModal[0].includes("wrapper.style.display = ''"), 'el modal muestra también las columnas de Dirección');
assert.ok(!cargarModal[0].includes('COLUMNAS_SOLO_DIRECCION'), 'el modal no oculta columnas de Dirección');

assert.ok(
  /function seleccionarTodasColumnas\(\) \{[\s\S]*MAPEO_COLUMNAS_A_CHECKBOX/.test(concursantesJs),
  'Seleccionar todas incluye las columnas de Dirección'
);
assert.ok(
  concursantesJs.includes('CAMPOS_TABLA_SOLO_DIRECCION.has(campo) && !puedeVerColumnasDireccion()'),
  'editar celdas de Dirección sigue bloqueado para el resto de roles'
);
assert.ok(
  /function actualizarMomentosDestacados[\s\S]*puedeVerColumnasDireccion\(\)/.test(concursantesJs),
  'momentos destacados solo los escribe Dirección'
);
assert.ok(
  concursantesJs.includes('momentos-destacados-textarea'),
  'momentos destacados se pintan en un textarea'
);
assert.ok(
  concursantesHtml.includes('.momentos-destacados-textarea'),
  'momentos destacados tienen caja con scroll como notas'
);
assert.ok(
  /#tabla-concursantes-header th\.col-acciones[\s\S]*min-width:\s*120px/.test(concursantesHtml),
  'la columna de acciones tiene ancho propio'
);
assert.ok(!/nth-of-type\(\s*20\s*\)/.test(concursantesHtml), 'ya no se aplastan columnas por nth-of-type 20');
assert.ok(!/nth-of-type\(\s*21\s*\)/.test(concursantesHtml), 'ya no se aplastan columnas por nth-of-type 21');
assert.ok(!/nth-of-type\(\s*22\s*\)/.test(concursantesHtml), 'ya no se aplastan columnas por nth-of-type 22');
assert.ok(
  /\.table-preguntas \{[\s\S]*table-layout:\s*auto/.test(concursantesHtml),
  'la tabla de concursantes usa anchos naturales'
);
assert.ok(
  /#tabla-concursantes-body-wrapper \{[\s\S]*overflow-x:\s*auto/.test(concursantesHtml),
  'la tabla de concursantes hace scroll horizontal'
);

assert.ok(
  /#modalJornada \.item-slot table \{[\s\S]*table-layout:\s*fixed/.test(jornadasHtml),
  'tablas del modal de jornada con anchos fijos'
);
assert.ok(
  /th\.col-nivel-jornada[\s\S]*width:\s*80px/.test(jornadasHtml),
  'la columna Nivel cabe en una línea'
);
assert.ok(
  /getNivelColor\([\s\S]*text-danger[\s\S]*text-success/.test(jornadasJs),
  'en jornadas LS va en verde y NLS en rojo'
);
assert.ok(
  /#modalJornada \.col-pregunta-jornada[\s\S]*width:\s*48%/.test(jornadasHtml),
  'pregunta del modal no se come toda la fila'
);
assert.ok(
  /#modalJornada \.col-respuesta-jornada \{[\s\S]*width:\s*24%/.test(jornadasHtml),
  'respuesta del modal tiene ancho propio'
);
assert.ok(
  /#modalJornada \.col-datos-jornada \{[\s\S]*width:\s*28%/.test(jornadasHtml),
  'datos extra del modal no se comen la respuesta'
);
assert.ok(
  jornadasJs.includes('col-respuesta-jornada') && jornadasJs.includes('col-datos-jornada'),
  'el JS del modal marca respuesta y datos extra'
);
assert.ok(
  jornadasJs.includes("Utils.showAlert(error.message || 'Error al cargar los datos de la jornada'"),
  'si la jornada está bloqueada se muestra el motivo real'
);
assert.ok(
  !/#modalJornada \.item-slot table td \{[\s\S]*word-break:\s*break-word/.test(jornadasHtml),
  'las celdas del modal no parten la respuesta letra a letra'
);

const comboParece = concursantesJs.match(/function comboPareceReciclado\([\s\S]*?\n\}/);
assert.ok(comboParece, 'existe comboPareceReciclado');
const sandboxFn = { concursanteActual: null };
vm.createContext(sandboxFn);
vm.runInContext(comboParece[0], sandboxFn);
assert.strictEqual(sandboxFn.comboPareceReciclado({ preguntaUsadaId: 9 }, '1'), true);
assert.strictEqual(sandboxFn.comboPareceReciclado({ preguntaUsadaId: null, notasDireccion: '' }, '1'), false);

const localStorageData = { usuario: JSON.stringify({ rol: 'ROLE_DIRECCION' }) };
const sandbox = {
  console,
  JSON,
  Number,
  String,
  Boolean,
  Array,
  Object,
  Set,
  Map,
  Date,
  Math,
  parseInt,
  isNaN,
  isFinite: Number.isFinite,
  localStorage: {
    getItem(key) { return localStorageData[key] || null; },
    setItem(key, value) { localStorageData[key] = String(value); },
    removeItem(key) { delete localStorageData[key]; }
  },
  document: {
    getElementById() { return null; },
    querySelector() { return null; },
    addEventListener() {}
  },
  Utils: { formatearNivel(n) { return n; }, mensajeErrorApi() { return ''; }, showAlert() {} }
};
sandbox.window = sandbox;
sandbox.window.scrollY = 0;
vm.createContext(sandbox);
vm.runInContext(jornadasJs + '\nthis.JornadasManager = JornadasManager;', sandbox);
const JM = sandbox.JornadasManager;
assert.ok(JM, 'JornadasManager no se cargó');

const jornada = { id: 1, estado: 'lista' };
const cuestionarioGrabado = { id: 20, estado: 'grabado', asignadoAConcursante: true };
const cuestionarioLibre = { id: 21, estado: 'adjudicado', asignadoAConcursante: false };
const comboAsignado = { id: 30, estado: 'grabado', asignadoAConcursante: true };
const comboLibre = { id: 31, estado: 'adjudicado', asignadoAConcursante: false };

const reciclarCuestGrabado = JM.htmlBotonReciclarCuestionario(cuestionarioGrabado, jornada);
assert.ok(reciclarCuestGrabado.includes('disabled'), 'no se recicla un cuestionario grabado');
assert.ok(!reciclarCuestGrabado.includes('reutilizarCuestionario'), 'el reciclar del cuestionario grabado no tiene onclick');

const reciclarCuestLibre = JM.htmlBotonReciclarCuestionario(cuestionarioLibre, jornada);
assert.ok(reciclarCuestLibre.includes('reutilizarCuestionario'), 'cuestionario libre sí se recicla');

const borrarCuest = JM.htmlBotonBorrarCuestionario(cuestionarioGrabado, jornada);
assert.ok(borrarCuest.includes('disabled'), 'no se borra un cuestionario asignado');
const borrarCuestLibre = JM.htmlBotonBorrarCuestionario(cuestionarioLibre, jornada);
assert.ok(borrarCuestLibre.includes('eliminarCuestionarioDeJornada'), 'cuestionario libre sí se quita');

const borrarCombo = JM.htmlBotonBorrarCombo(comboAsignado, jornada);
assert.ok(borrarCombo.includes('disabled'), 'no se borra un combo asignado');
const borrarComboLibre = JM.htmlBotonBorrarCombo(comboLibre, jornada);
assert.ok(borrarComboLibre.includes('eliminarComboDeJornada'), 'combo libre sí se quita');

console.log('OK cambios flujo contratos');
