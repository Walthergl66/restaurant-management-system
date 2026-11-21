import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import ts from 'typescript';

const { outputText } = ts.transpileModule(readFileSync(new URL('./zonas.ts', import.meta.url), 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 },
});
const { agruparMesasPorZona } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`);

const mesa = (id, numero, ubicacion) => ({ id, numero, capacidad: 4, ubicacion, estado: 'LIBRE', activo: true });

test('agrupa por ubicación, respeta el orden de aparición y ordena por número', () => {
  const zonas = agruparMesasPorZona([mesa(1, 3, 'Terraza'), mesa(2, 2, 'Salón'), mesa(3, 1, 'Terraza')]);
  assert.deepEqual(zonas.map(z => z.zona), ['Terraza', 'Salón']);
  assert.deepEqual(zonas[0].mesas.map(m => m.numero), [1, 3]);
});

test('ubicación vacía o null cae en la zona Salón', () => {
  const zonas = agruparMesasPorZona([mesa(1, 1, null), mesa(2, 2, ''), mesa(3, 3, '   ')]);
  assert.equal(zonas.length, 1);
  assert.equal(zonas[0].zona, 'Salón');
  assert.equal(zonas[0].mesas.length, 3);
});

test('"salon" y "Salón " son la misma zona y se muestra la primera grafía', () => {
  const zonas = agruparMesasPorZona([mesa(1, 1, 'Salón'), mesa(2, 2, 'salon'), mesa(3, 3, ' SALÓN ')]);
  assert.equal(zonas.length, 1);
  assert.equal(zonas[0].zona, 'Salón');
});

test('sin mesas no hay zonas', () => {
  assert.deepEqual(agruparMesasPorZona([]), []);
});
