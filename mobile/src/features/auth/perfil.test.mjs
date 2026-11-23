import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import ts from 'typescript';

const { outputText } = ts.transpileModule(readFileSync(new URL('./perfil.ts', import.meta.url), 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 },
});
const { iniciales, etiquetaRol } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`);

test('iniciales: primera letra de las dos primeras palabras, en mayúscula', () => {
  assert.equal(iniciales('maykel josue menendez'), 'MJ');
  assert.equal(iniciales('Ana'), 'A');
  assert.equal(iniciales('  luis   pérez  '), 'LP');
});

test('iniciales: vacío o nulo devuelve un marcador', () => {
  assert.equal(iniciales(''), '?');
  assert.equal(iniciales('   '), '?');
  assert.equal(iniciales(null), '?');
  assert.equal(iniciales(undefined), '?');
});

test('etiquetaRol: nombres legibles para cada rol del sistema', () => {
  assert.equal(etiquetaRol('ADMIN'), 'Administrador');
  assert.equal(etiquetaRol('MESERO'), 'Mesero');
  assert.equal(etiquetaRol('CAJERO'), 'Cajero');
  assert.equal(etiquetaRol('COCINA'), 'Cocina');
  assert.equal(etiquetaRol('DELIVERY'), 'Repartidor');
  assert.equal(etiquetaRol('CLIENTE'), 'Cliente');
});

test('etiquetaRol: un rol desconocido se muestra tal cual y el nulo queda vacío', () => {
  assert.equal(etiquetaRol('SUPERVISOR'), 'SUPERVISOR');
  assert.equal(etiquetaRol(null), '');
});
