/**
 * Contrato: no salir de borrador sin 4 preguntas / 3 PM (salvo admin).
 * Se ejecuta con Node desde BorradorCompletoJsContratosTest.
 */
const assert = require('assert');
const fs = require('fs');
const path = require('path');

const jsDir = path.resolve(__dirname, '../../main/resources/static/js');
const combos = fs.readFileSync(path.join(jsDir, 'combos.js'), 'utf8');
const cuestionarios = fs.readFileSync(path.join(jsDir, 'cuestionarios.js'), 'utf8');

assert.ok(
  combos.includes('sin las 3 preguntas multiplicadoras (PM1, PM2, PM3)'),
  'combos.js debe bloquear salir de borrador sin PM1/PM2/PM3'
);
assert.ok(
  /estadoCombo !== 'borrador' && !esAdminCombo/.test(combos)
    || /nuevoEstado !== 'borrador' && !esAdminCombo/.test(combos),
  'combos.js debe exigir las 3 PM si no es admin y el estado no es borrador'
);
assert.ok(
  combos.includes("authManager.hasRole('ROLE_ADMIN')"),
  'combos.js debe exceptuar a ROLE_ADMIN'
);

assert.ok(
  cuestionarios.includes('sin las 4 preguntas (1LS, 2NLS, 3LS, 4NLS)'),
  'cuestionarios.js debe bloquear salir de borrador sin 1LS/2NLS/3LS/4NLS'
);
assert.ok(
  /estadoSeleccionadoPrevio !== 'borrador' && !esAdminCuestionario/.test(cuestionarios)
    || /nuevoEstado !== 'borrador' && !esAdminCuestionario/.test(cuestionarios),
  'cuestionarios.js debe exigir las 4 preguntas si no es admin y el estado no es borrador'
);
assert.ok(
  cuestionarios.includes("authManager.hasRole('ROLE_ADMIN')"),
  'cuestionarios.js debe exceptuar a ROLE_ADMIN'
);

console.log('OK borrador completo contratos');
