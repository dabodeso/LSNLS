/**
 * Contratos de Utils (estados y mensajes al usuario).
 * Se ejecuta con Node desde UtilsJsContratosTest.
 */
const assert = require('assert');
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const utilsPath = path.resolve(__dirname, '../../main/resources/static/js/utils.js');
const code = fs.readFileSync(utilsPath, 'utf8');

const documentStub = {
  readyState: 'loading',
  addEventListener() {},
  getElementById() { return null; },
  createElement() {
    return { style: {}, className: '', innerHTML: '', appendChild() {} };
  },
  body: { appendChild() {} }
};

const sandbox = {
  console,
  document: documentStub,
  localStorage: { getItem() { return null; }, setItem() {}, removeItem() {} },
  Toastify: undefined,
  setTimeout,
  clearTimeout,
  JSON,
  String,
  Number,
  Boolean,
  Date,
  Array,
  Object,
  Set,
  Map,
  Error,
  Function,
  Promise,
  RegExp,
  Math,
  parseInt,
  isNaN
};
sandbox.window = sandbox;
sandbox.window.location = { pathname: '/preguntas.html' };
sandbox.window.alert = function () {};
vm.createContext(sandbox);
vm.runInContext(code + '\nthis.Utils = Utils;', sandbox);
const Utils = sandbox.Utils;
assert.ok(Utils, 'Utils no se cargó');

assert.strictEqual(Utils.formatearEstadoCuestionario('grabado'), 'Grabado');
assert.strictEqual(Utils.formatearEstadoCuestionario('borrador'), 'Borrador');
assert.strictEqual(Utils.formatearEstadoCombo('adjudicado'), 'Adjudicado');
assert.strictEqual(Utils.formatearNivel('_1LS'), '1LS');
assert.strictEqual(Utils.formatearNivel('_5NLS'), '5NLS');
assert.strictEqual(Utils.formatearNivel('_0'), '0');
assert.strictEqual(Utils.formatearRol('ROLE_ADMIN'), 'ADMIN');
assert.strictEqual(Utils.formatearRol('ROLE_GUION'), 'GUION');
assert.strictEqual(Utils.truncateText('abcdefghij', 5), 'abcde...');

assert.strictEqual(true, Utils.esMensajeTecnico('could not execute statement SQLException'));
assert.strictEqual(true, Utils.esMensajeTecnico('Failed to fetch'));
assert.ok(!Utils.esMensajeTecnico('El nombre es obligatorio'));

const oculto = Utils.prepararTextoUsuario(
  'Error interno: could not execute statement; nested exception is java.sql.SQLException',
  'guardar'
);
assert.ok(!/sql/i.test(oculto), 'El usuario no debe ver SQL: ' + oculto);
assert.ok(!oculto.includes('java.'), 'El usuario no debe ver java.: ' + oculto);

assert.strictEqual(
  Utils.mensajeErrorHttp(401, '', 'editar'),
  'Tu sesión ha expirado. Vuelve a iniciar sesión.'
);
assert.strictEqual(
  Utils.mensajeErrorHttp(403, '', 'borrar el concursante'),
  'No tienes permisos para borrar el concursante.'
);
assert.ok(!Utils.mensajeErrorApi(new Error('SQLException: Duplicate entry'), 'guardar').toLowerCase().includes('sql'));

const detalleJsonString = Utils.extraerDetalleErrorCuerpo(
  '"Error al crear concursante: El cuestionario 19 ya está asignado al concursante 19 (ALICIA). Debe desasignarlo primero antes de asignarlo a otro concursante."'
);
assert.ok(detalleJsonString.includes('cuestionario 19'), detalleJsonString);
assert.ok(detalleJsonString.includes('ALICIA'), detalleJsonString);

const toastUsuario = Utils.mensajeErrorHttp(400, detalleJsonString, 'completar la petición');
assert.ok(toastUsuario.includes('cuestionario 19'), toastUsuario);
assert.ok(!/inténtalo de nuevo/i.test(toastUsuario), toastUsuario);

console.log('OK utils contratos');
