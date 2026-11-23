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

const conZona = (id, numero, zonaId, zonaOrden, ubicacion) => ({ id, numero, capacidad: 4, ubicacion, zonaId, zonaOrden, estado: 'LIBRE', activo: true });

test('las zonas siguen el orden del administrador aunque las mesas lleguen mezcladas', () => {
  const zonas = agruparMesasPorZona([
    conZona(1, 1, 20, 2, 'Terraza'),
    conZona(2, 2, 10, 1, 'Salón'),
    conZona(3, 3, 30, 3, 'Patio'),
  ]);
  assert.deepEqual(zonas.map(z => z.zona), ['Salón', 'Terraza', 'Patio']);
});

test('las mesas con el mismo zonaId forman una sola zona y se ordenan por número', () => {
  const zonas = agruparMesasPorZona([
    conZona(1, 5, 10, 1, 'Salón'),
    conZona(2, 2, 10, 1, 'Salón'),
  ]);
  assert.equal(zonas.length, 1);
  assert.deepEqual(zonas[0].mesas.map(m => m.numero), [2, 5]);
});

test('una zona renombrada se muestra con el nombre actual del backend', () => {
  const zonas = agruparMesasPorZona([conZona(1, 1, 10, 1, 'Salón principal'), conZona(2, 2, 10, 1, 'Salón principal')]);
  assert.deepEqual(zonas.map(z => z.zona), ['Salón principal']);
});

test('con el mismo orden las zonas se desempatan por nombre', () => {
  const zonas = agruparMesasPorZona([conZona(1, 1, 20, 1, 'Terraza'), conZona(2, 2, 10, 1, 'Barra')]);
  assert.deepEqual(zonas.map(z => z.zona), ['Barra', 'Terraza']);
});

test('las mesas sin zonaId (backend antiguo) se agrupan por nombre y quedan al final', () => {
  const zonas = agruparMesasPorZona([
    mesa(1, 1, 'terraza'),
    conZona(2, 2, 10, 1, 'Salón'),
    mesa(3, 3, 'Terraza '),
  ]);
  assert.deepEqual(zonas.map(z => z.zona), ['Salón', 'terraza']);
  assert.deepEqual(zonas[1].mesas.map(m => m.numero), [1, 3]);
});
