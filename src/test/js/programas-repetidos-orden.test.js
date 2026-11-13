/**
 * Un repetido de octubre o noviembre con id bajo sale en Repetidos
 * por fecha de emisión, no queda detrás de los de agosto por el id.
 * La página sigue siendo de 5.
 */
const assert = require('assert');
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const jsPath = path.resolve(__dirname, '../../main/resources/static/js/programas.js');
const codigo = fs.readFileSync(jsPath, 'utf8');

const calls = [];
const elements = {};

function fakeEl(tag) {
    return {
        tag: tag || 'div',
        style: {},
        textContent: '',
        innerHTML: '',
        value: '',
        className: '',
        dataset: {},
        children: [],
        classList: {
            add() {},
            remove() {},
            contains() { return false; }
        },
        setAttribute() {},
        getAttribute() { return null; },
        appendChild(child) { this.children.push(child); return child; },
        querySelector() { return null; },
        querySelectorAll() { return []; },
        addEventListener() {}
    };
}

function elemento(id) {
    if (!elements[id]) elements[id] = fakeEl(id);
    return elements[id];
}

function repetido(id, fechaEmision) {
    return {
        id,
        estado: 'programado',
        temporada: 2,
        codigo: String(id),
        duracionObjetivo: '45m',
        fechaEmision,
        creditosEspeciales: '',
        notas: '',
        repetido: true
    };
}

// Ids altos en agosto y julio; octubre y noviembre con id bajo (el caso de Raúl).
const catalogo = [
    repetido(90, '2026-08-28'),
    repetido(80, '2026-08-21'),
    repetido(70, '2026-08-14'),
    repetido(60, '2026-08-07'),
    repetido(50, '2026-07-30'),
    repetido(12, '2026-10-19'),
    repetido(8, '2026-11-02'),
    repetido(3, '2026-11-02'),
    repetido(7, '2026-10-05'),
    repetido(4, [2026, 11, 16])
];

const ordenEsperado = [4, 8, 3, 12, 7, 90, 80, 70, 60, 50];

const apiManager = {
    async get(url) {
        calls.push('GET ' + url);
        if (url === '/api/programas/repetidos') {
            return catalogo.map(programa => ({ ...programa, fechaEmision: programa.fechaEmision }));
        }
        if (url === '/api/programas') {
            return [
                { id: 8, fechaEmision: '2026-11-02', estado: 'programado', temporada: 2, codigo: '8' },
                { id: 90, fechaEmision: '2026-08-28', estado: 'emitido', temporada: 2, codigo: '90' }
            ];
        }
        if (String(url).includes('/concursantes')) {
            return [];
        }
        return [];
    }
};

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
    parseInt,
    isNaN,
    isFinite: Number.isFinite,
    Promise,
    setTimeout,
    clearTimeout,
    requestAnimationFrame(fn) { return setTimeout(fn, 0); },
    apiManager,
    localStorage: { getItem() { return null; }, setItem() {}, removeItem() {} },
    document: {
        addEventListener() {},
        getElementById: elemento,
        querySelector() { return null; },
        querySelectorAll() { return []; },
        createElement: fakeEl
    },
    mostrarExito() {},
    mostrarError(mensaje) { throw new Error(mensaje || 'error en programas'); },
    Utils: { mensajeErrorApi() { return 'error'; }, abrirEnNuevaPestana() {} }
};
sandbox.window = sandbox;
sandbox.window.scrollY = 0;
sandbox.window.pageYOffset = 0;
sandbox.window.scrollTo = () => {};
sandbox.window.UndoManager = { record() {} };

vm.createContext(sandbox);
vm.runInContext(codigo + `
this.__api = {
    cargarProgramasFiltrados,
    cambiarPaginaFiltrada,
    get programas() { return programas; },
    get programasFiltrados() { return programasFiltrados; },
    get paginaActual() { return paginaActual; },
    get tamañoPagina() { return tamañoPagina; },
    get totalPaginas() { return totalPaginas; },
    get totalItems() { return totalItems; }
};
`, sandbox);

const api = sandbox.__api;

function ids(lista) {
    return lista.map(programa => programa.id);
}

(async () => {
    assert.strictEqual(api.tamañoPagina, 5, 'la página de repetidos sigue siendo de 5');

    elemento('filtro-listado-programa').value = 'repetidos';
    elemento('filtro-estado-programa').value = '';
    elemento('filtro-temporada').value = '';
    elemento('filtro-programa-id').value = '';
    elemento('filtro-fecha-emision').value = '';

    await api.cargarProgramasFiltrados();

    assert.deepStrictEqual(ids(api.programasFiltrados), ordenEsperado,
        'octubre y noviembre (id bajo) van delante de agosto');
    assert.strictEqual(api.totalItems, catalogo.length, 'no se pierde ningún repetido al ordenar');
    assert.strictEqual(api.totalPaginas, 2, '10 repetidos caben en 2 páginas de 5');
    assert.strictEqual(api.paginaActual, 0);
    assert.deepStrictEqual(ids(api.programas), ordenEsperado.slice(0, 5),
        'la primera página muestra los cinco más recientes');
    assert.ok(api.programas.every(programa => programa.repetido === true),
        'el listado conserva el flag de repetido');
    assert.ok(api.programas.every(programa => programa.estado === 'programado'),
        'el listado conserva el estado');
    assert.ok(ids(api.programas).includes(4) && ids(api.programas).includes(7),
        'noviembre y octubre están en la primera página');
    assert.ok(!ids(api.programas).includes(90),
        'el 28 de agosto ya no tapa la primera página');

    const info = elemento('info-paginacion-programas').textContent;
    assert.strictEqual(info, 'Mostrando 1-5 de 10 programas');

    calls.length = 0;
    await api.cambiarPaginaFiltrada(1);
    assert.strictEqual(api.paginaActual, 1);
    assert.deepStrictEqual(ids(api.programas), ordenEsperado.slice(5),
        'la segunda página sigue siendo de los 5 siguientes');
    assert.strictEqual(api.programas.length, 5);
    assert.strictEqual(calls.filter(llamada => llamada.includes('/concursantes')).length, 5,
        'al cambiar de página solo se piden los concursantes de esos 5');
    assert.strictEqual(elemento('info-paginacion-programas').textContent, 'Mostrando 6-10 de 10 programas');

    await api.cambiarPaginaFiltrada(0);
    elemento('filtro-listado-programa').value = 'conjunto';
    await api.cargarProgramasFiltrados();
    assert.strictEqual(api.totalItems, catalogo.length + 2, 'la vista conjunta suma programas y repetidos');
    assert.deepStrictEqual(
        api.programas.map(programa => ({ id: programa.id, repetido: programa.repetido })),
        [
            { id: 4, repetido: true },
            { id: 8, repetido: false },
            { id: 8, repetido: true },
            { id: 3, repetido: true },
            { id: 12, repetido: true }
        ],
        'en la vista conjunta mandan la fecha y se distinguen el programa y el repetido con el mismo id'
    );
    assert.ok(api.programasFiltrados.some(programa => programa.id === 90 && programa.repetido === false));
    assert.ok(api.programasFiltrados.some(programa => programa.id === 90 && programa.repetido === true));
    assert.ok(!api.programas.some(programa => programa.fechaEmision === '2026-08-28' || (Array.isArray(programa.fechaEmision) && programa.fechaEmision[1] === 8)),
        'agosto no tapa la primera página de la vista conjunta');

    console.log('OK repetidos por fecha');
})().catch(error => {
    console.error(error);
    process.exit(1);
});
