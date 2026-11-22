/**
 * La lista de jornadas no arrastra los catálogos.
 * Esos catálogos se piden al abrir el selector.
 */
const assert = require('assert');
const fs = require('fs');
const path = require('path');

const jsPath = path.resolve(__dirname, '../../main/resources/static/js/jornadas.js');
const jornadasJs = fs.readFileSync(jsPath, 'utf8');

const cargarDatos = jornadasJs.slice(
  jornadasJs.indexOf('async cargarDatos'),
  jornadasJs.indexOf('async cargarCuestionariosDisponibles')
);
assert.ok(cargarDatos.includes('/api/jornadas?'), 'la lista sigue pidiendo las jornadas');
assert.ok(!cargarDatos.includes('cuestionarios-disponibles'), 'la lista no baja el catálogo de cuestionarios');
assert.ok(!cargarDatos.includes('combos-disponibles'), 'la lista no baja el catálogo de combos');

const abrirCuestionarios = jornadasJs.slice(
  jornadasJs.indexOf('async seleccionarCuestionarios'),
  jornadasJs.indexOf('async seleccionarCombos')
);
const abrirCombos = jornadasJs.slice(
  jornadasJs.indexOf('async seleccionarCombos'),
  jornadasJs.indexOf('listaCuestionariosFiltrada')
);
assert.ok(
  abrirCuestionarios.indexOf('cargarCuestionariosDisponibles') >= 0
    && abrirCuestionarios.indexOf('cargarCuestionariosDisponibles')
      < abrirCuestionarios.indexOf('mostrarCuestionariosDisponibles'),
  'el selector de cuestionarios carga el catálogo antes de pintarlo'
);
assert.ok(
  abrirCombos.indexOf('cargarCombosDisponibles') >= 0
    && abrirCombos.indexOf('cargarCombosDisponibles') < abrirCombos.indexOf('mostrarCombosDisponibles'),
  'el selector de combos carga el catálogo antes de pintarlo'
);

console.log('OK jornadas carga lista');
