/**
 * El temporizador no se reinicia, la jornada no pisa lo escrito
 * y las celdas de programa y concursante reservan antes de abrirse.
 */
const assert = require('assert');
const fs = require('fs');
const path = require('path');

function leer(rel) {
  return fs.readFileSync(path.resolve(__dirname, rel), 'utf8');
}

const candado = leer('../../main/resources/static/js/editLockManager.js');
const renovar = candado.slice(candado.indexOf('this.renewId = setInterval'), candado.indexOf('if (modalEl && !modalEl._editLockBound)'));
assert.ok(!renovar.includes('this.active.remaining = data.ttlSeconds'), 'renovar no reinicia la cuenta atrás');

const jornadas = leer('../../main/resources/static/js/jornadas.js');
const refrescar = jornadas.slice(jornadas.indexOf('async refrescarModalEdicionJornada'), jornadas.indexOf('await this.actualizarSlotsVisualAsync()'));
assert.ok(refrescar.includes('el.value !== base'), 'no reescribe un campo que el usuario ya cambió');

const programas = leer('../../main/resources/static/js/programas.js');
const editarPrograma = programas.slice(programas.indexOf('async function editarCampoPrograma'), programas.indexOf('async function guardarCampoProgramaEditado'));
assert.ok(editarPrograma.indexOf('tryAcquire') < editarPrograma.indexOf('crearControlCampoPrograma'), 'reserva el programa antes de abrir la celda');
assert.ok(editarPrograma.includes('PROGRAMA_REPETIDO'), 'el repetido usa su propio candado');

const concursantes = leer('../../main/resources/static/js/concursantes.js');
const editarConcursante = concursantes.slice(concursantes.indexOf('async function editarCeldaConcursante'), concursantes.indexOf('async function guardarCeldaConcursante'));
assert.ok(editarConcursante.indexOf("tryAcquire('CONCURSANTE'") < editarConcursante.indexOf('td.appendChild'), 'reserva el concursante antes de abrir la celda');

const preguntas = leer('../../main/resources/static/js/preguntas.js');
const guardar = preguntas.slice(preguntas.indexOf('async guardarCelda'), preguntas.indexOf('mostrarModalPregunta'));
assert.ok(guardar.includes('update.version = previa.version'), 'la celda manda la versión que tenía al abrir');

console.log('OK concurrencia restos');
