/**
 * La celda de una pregunta no se abre ni se guarda si otra persona la tiene reservada.
 */
const assert = require('assert');
const fs = require('fs');
const path = require('path');

const jsPath = path.resolve(__dirname, '../../main/resources/static/js/preguntas.js');
const preguntasJs = fs.readFileSync(jsPath, 'utf8');

const aviso = preguntasJs.slice(
  preguntasJs.indexOf('async avisoSiPreguntaBloqueada'),
  preguntasJs.indexOf('avisarCeldaBloqueada')
);
assert.ok(aviso.includes('/api/edit-locks/status?entityType=PREGUNTA&entityId='), 'consulta el candado de la pregunta');
assert.ok(aviso.includes('estado.locked'), 'distingue si hay candado');
assert.ok(aviso.includes('otra ventana'), 'avisa cuando la misma persona ya la tiene abierta');

const editar = preguntasJs.slice(
  preguntasJs.indexOf('async editarCelda'),
  preguntasJs.indexOf('async guardarCelda')
);
assert.ok(
  editar.indexOf('avisoSiPreguntaBloqueada') >= 0
    && editar.indexOf('avisoSiPreguntaBloqueada') < editar.indexOf('document.createElement'),
  'el doble clic consulta el candado antes de abrir el campo'
);

const guardar = preguntasJs.slice(
  preguntasJs.indexOf('async guardarCelda'),
  preguntasJs.indexOf('mostrarModalPregunta')
);
const primerPut = guardar.indexOf("method: 'PUT'");
assert.ok(
  guardar.indexOf('avisoSiPreguntaBloqueada') >= 0 && guardar.indexOf('avisoSiPreguntaBloqueada') < primerPut,
  'el guardado vuelve a consultar el candado antes de escribir'
);

console.log('OK pregunta celda candado');
