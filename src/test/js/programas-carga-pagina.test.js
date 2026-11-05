/**
 * Un guardado de programa no pide concursantes del catálogo.
 * La apertura pide la página 1 (5) y solo los concursantes de esa página.
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

function programa(id) {
    return {
        id,
        estado: 'borrador',
        temporada: 1,
        codigo: String(id),
        duracionObjetivo: '45m',
        fechaEmision: '2026-10-05',
        creditosEspeciales: '',
        notas: '',
        repetido: false
    };
}

const apiManager = {
    async get(url) {
        calls.push('GET ' + url);
        if (url.startsWith('/api/programas/pagina')) {
            return {
                programas: [1, 2, 3, 4, 5].map(programa),
                currentPage: 0,
                totalItems: 448,
                totalPages: 90
            };
        }
        if (url === '/api/programas' || url === '/api/programas/repetidos') {
            return [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map(programa);
        }
        if (String(url).includes('/concursantes')) {
            return [];
        }
        const id = Number(String(url).split('/').pop());
        return programa(Number.isFinite(id) ? id : 7);
    },
    async patch(url, data) {
        calls.push('PATCH ' + url + ' ' + JSON.stringify(data));
        const id = Number(String(url).match(/(\d+)/)?.[1] || 7);
        return Object.assign({ id }, data || {});
    },
    async patchUndoable(endpoint, data, opciones = {}) {
        let previous = null;
        if (opciones.snapshotEndpoint) {
            try {
                previous = await this.get(opciones.snapshotEndpoint);
            } catch (error) {
                previous = null;
            }
        }
        const result = await this.patch(endpoint, data);
        if (opciones.onAfter) {
            await opciones.onAfter(data, previous, 'initial', result);
        }
        return result;
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
    parseInt,
    isNaN,
    isFinite: Number.isFinite,
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
    actualizarEstadoPrograma,
    actualizarDuracionObjetivoPrograma,
    actualizarTemporadaPrograma,
    actualizarFechaEmision,
    actualizarCodigoPrograma,
    actualizarNotasPrograma,
    actualizarCreditosEspecialesPrograma,
    alternarEdicionPrograma,
    cargarProgramas,
    cargarConcursantesParaProgramasFiltrados,
    get programas() { return programas; },
    set programas(valor) { programas = valor; },
    get programasFiltrados() { return programasFiltrados; },
    set programasFiltrados(valor) { programasFiltrados = valor; },
    get paginaActual() { return paginaActual; },
    set paginaActual(valor) { paginaActual = valor; },
    get tamañoPagina() { return tamañoPagina; },
    get aplicandoFiltros() { return aplicandoFiltros; },
    set aplicandoFiltros(valor) { aplicandoFiltros = valor; },
    set rolUsuarioActual(valor) { rolUsuarioActual = valor; },
    get concursantesPorPrograma() { return concursantesPorPrograma; },
    programasEnEdicion
};
`, sandbox);

const api = sandbox.__api;
assert.strictEqual(api.tamañoPagina, 5, 'página 1 = 5 programas');

function llamadasDesde(indice) {
    return calls.slice(indice);
}

function esListadoCompleto(llamada) {
    return llamada === 'GET /api/programas' || llamada === 'GET /api/programas/repetidos';
}

function esConcursantes(llamada) {
    return llamada.includes('/concursantes');
}

(async () => {
    calls.length = 0;
    await api.cargarProgramas();
    const apertura = calls.slice();
    const paginas = apertura.filter(llamada => llamada.startsWith('GET /api/programas/pagina'));
    assert.strictEqual(paginas.length, 1, 'al abrir hay una sola página');
    assert.ok(paginas[0].includes('page=0'), 'al abrir se pide la página 1');
    assert.ok(paginas[0].includes('size=5'), 'al abrir el tamaño es 5');
    assert.ok(!apertura.some(esListadoCompleto), 'al abrir no se pide el catálogo entero');
    const concursantesApertura = apertura.filter(esConcursantes);
    assert.strictEqual(concursantesApertura.length, 5, 'al abrir solo hay concursantes de la página 1');
    assert.deepStrictEqual(
        concursantesApertura,
        [1, 2, 3, 4, 5].map(id => 'GET /api/concursantes/programa/' + id)
    );

    const catalogo = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map(id => programa(id));
    api.aplicandoFiltros = true;
    api.programasFiltrados = catalogo;
    api.paginaActual = 0;
    calls.length = 0;
    await api.cargarConcursantesParaProgramasFiltrados();
    const filtrados = calls.filter(esConcursantes);
    assert.strictEqual(filtrados.length, 5, 'un filtro no pide concursantes de los 12');
    assert.ok(filtrados.every(llamada => /\/programa\/([1-5])$/.test(llamada)));

    api.paginaActual = 1;
    calls.length = 0;
    await api.cargarConcursantesParaProgramasFiltrados();
    const segunda = calls.filter(esConcursantes);
    assert.strictEqual(segunda.length, 5, 'la página 2 pide otros 5, no el resto del catálogo');
    assert.ok(segunda.every(llamada => /\/programa\/(6|7|8|9|10)$/.test(llamada)));

    const editado = programa(7);
    api.programas = [editado];
    api.programasFiltrados = [editado, programa(8), programa(9), programa(10), programa(11)];
    api.aplicandoFiltros = true;
    api.paginaActual = 0;
    api.programasEnEdicion.add('n:7');

    const campos = [
        () => api.actualizarEstadoPrograma(7, 'grabado', false),
        () => api.actualizarTemporadaPrograma(7, 3, false),
        () => api.actualizarFechaEmision(7, '2026-10-05', false),
        () => api.actualizarCodigoPrograma(7, '107', false),
        () => api.actualizarCreditosEspecialesPrograma(7, 'crédito', false),
        () => api.actualizarNotasPrograma(7, 'nota interna', false),
        () => api.actualizarDuracionObjetivoPrograma(7, '50m')
    ];

    for (const guardar of campos) {
        const desde = calls.length;
        await guardar();
        const hechas = llamadasDesde(desde);
        assert.strictEqual(hechas.filter(esConcursantes).length, 0, 'guardar no pide concursantes: ' + hechas.join(' | '));
        assert.ok(!hechas.some(llamada => llamada.startsWith('GET /api/programas/pagina')), 'guardar no recarga la página');
        assert.ok(!hechas.some(esListadoCompleto), 'guardar no recarga el catálogo');
    }

    assert.strictEqual(api.programas[0].estado, 'grabado');
    assert.strictEqual(api.programas[0].temporada, 3);
    assert.strictEqual(api.programas[0].codigo, '107');
    assert.strictEqual(api.programas[0].duracionObjetivo, '50m');
    assert.strictEqual(api.programas[0].notas, 'nota interna');
    assert.ok(api.programasEnEdicion.has('n:7'), 'Listo sigue en edición sin recargar');

    api.concursantesPorPrograma[7] = [
        { numeroConcursante: 22, estado: 'emitido' },
        { numeroConcursante: 15, estado: 'grabado' },
        { numeroConcursante: 8, estado: 'editado' }
    ];
    calls.length = 0;
    let rechazoEditado = null;
    try {
        await api.actualizarEstadoPrograma(7, 'editado', false);
    } catch (error) {
        rechazoEditado = error;
    }
    assert.ok(rechazoEditado, 'editado se rechaza si hay concursantes sin editar');
    assert.ok(String(rechazoEditado.message).includes('15'), rechazoEditado && rechazoEditado.message);
    assert.ok(String(rechazoEditado.message).includes('22'), rechazoEditado && rechazoEditado.message);
    assert.strictEqual(calls.length, 0, 'el rechazo no llama al servidor');
    delete api.concursantesPorPrograma[7];

    calls.length = 0;
    api.rolUsuarioActual = 'ROLE_DIRECCION';
    api.alternarEdicionPrograma(7, false);
    assert.strictEqual(calls.length, 0, 'el botón Listo no pide nada');

    console.log('OK programas pagina');
})().catch(error => {
    console.error(error);
    process.exit(1);
});
