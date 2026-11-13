/**
 * Buscar 389 no debe devolver también el programa cuyo id interno es 389.
 * En un programa normal el tiempo admite ±30 segundos.
 */
const assert = require('assert');
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const jsPath = path.resolve(__dirname, '../../main/resources/static/js/programas.js');
const codigo = fs.readFileSync(jsPath, 'utf8');

function fakeEl() {
    return {
        style: {},
        textContent: '',
        innerHTML: '',
        value: '',
        className: '',
        dataset: {},
        classList: { add() {}, remove() {}, contains() { return false; } },
        setAttribute() {},
        appendChild() {},
        querySelector() { return null; },
        querySelectorAll() { return []; },
        addEventListener() {}
    };
}

const elements = {};
function elemento(id) {
    if (!elements[id]) elements[id] = fakeEl();
    return elements[id];
}

const sandbox = {
    console,
    JSON,
    Math,
    Number,
    String,
    Boolean,
    Array,
    Object,
    Set,
    Map,
    Date,
    RegExp,
    Promise,
    parseInt,
    isNaN,
    isFinite: Number.isFinite,
    setTimeout,
    clearTimeout,
    document: {
        addEventListener() {},
        getElementById: elemento,
        querySelector() { return null; },
        querySelectorAll() { return []; },
        createElement: fakeEl
    },
    mostrarExito() {},
    mostrarError() {},
    Utils: { mensajeErrorApi() { return 'error'; }, escapeHtml(v) { return v; } }
};
sandbox.window = sandbox;

vm.createContext(sandbox);
vm.runInContext(codigo + `
this.__api = { aplicarFiltros, filtroTiempoDesdeTexto, duracionCabeEnFiltroTiempo, textoAyudaFiltroTiempoDisponibles };
`, sandbox);

const api = sandbox.__api;
elemento('filtro-estado-programa').value = '';
elemento('filtro-temporada').value = '';
elemento('filtro-fecha-emision').value = '';
elemento('filtro-programa-id').value = '389';

const lista = api.aplicarFiltros([
    { id: 400, codigo: '389', estado: 'programado', temporada: 6 },
    { id: 389, codigo: '388', estado: 'programado', temporada: 6 },
    { id: 10, codigo: '', estado: 'programado', temporada: 6 }
]);

assert.deepStrictEqual(lista.map(p => p.codigo || String(p.id)), ['389']);

elemento('filtro-programa-id').value = 'PGM 112';
const conPrefijo = api.aplicarFiltros([
    { id: 112, codigo: '111' },
    { id: 300, codigo: '112' }
]);
assert.deepStrictEqual(conPrefijo.map(p => p.id), [300]);

elemento('filtro-programa-id').value = '10';
const sinCodigo = api.aplicarFiltros([
    { id: 10, codigo: '' },
    { id: 11, codigo: '10' }
]);
assert.deepStrictEqual(sinCodigo.map(p => p.id), [10, 11]);

const filtro = api.filtroTiempoDesdeTexto('20:00');
assert.strictEqual(filtro.min, 20 * 60 - 30);
assert.strictEqual(filtro.max, 20 * 60 + 30);
assert.strictEqual(api.duracionCabeEnFiltroTiempo('19:30', filtro), true);
assert.strictEqual(api.duracionCabeEnFiltroTiempo('20:30', filtro), true);
assert.strictEqual(api.duracionCabeEnFiltroTiempo('19:29', filtro), false);
assert.ok(api.textoAyudaFiltroTiempoDisponibles().startsWith('Margen de ±30 segundos'));

console.log('OK codigo visible y margen 30s');
