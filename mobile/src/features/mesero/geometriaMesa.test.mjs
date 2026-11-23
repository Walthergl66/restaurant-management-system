import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import ts from 'typescript';

const { outputText } = ts.transpileModule(readFileSync(new URL('./geometriaMesa.ts', import.meta.url), 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 },
});
const { disenarMesa, MAX_SILLAS } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`);

const dentro = (d, s) => s.x >= 0 && s.y >= 0 && s.x + s.w <= d.ancho && s.y + s.h <= d.alto;

test('la forma depende de la capacidad', () => {
  assert.equal(disenarMesa(1).forma, 'cuadrada');
  assert.equal(disenarMesa(2).forma, 'cuadrada');
  assert.equal(disenarMesa(3).forma, 'redonda');
  assert.equal(disenarMesa(4).forma, 'redonda');
  assert.equal(disenarMesa(5).forma, 'alargada');
  assert.equal(disenarMesa(8).forma, 'alargada');
});

test('hay una silla por comensal hasta el máximo dibujable', () => {
  for (const capacidad of [1, 2, 3, 4, 5, 6, 7, 8, 10, 12]) {
    assert.equal(disenarMesa(capacidad).sillas.length, capacidad, `capacidad ${capacidad}`);
  }
  assert.equal(disenarMesa(20).sillas.length, MAX_SILLAS);
  assert.equal(disenarMesa(50).sillas.length, MAX_SILLAS);
});

test('todas las sillas y el cuerpo caben dentro del recuadro de la mesa', () => {
  for (const capacidad of [1, 2, 3, 4, 5, 6, 7, 8, 12, 50]) {
    const d = disenarMesa(capacidad);
    d.sillas.forEach(s => assert.ok(dentro(d, s), `silla fuera con capacidad ${capacidad}`));
    assert.ok(dentro(d, d.cuerpo), `cuerpo fuera con capacidad ${capacidad}`);
  }
});

test('las sillas de un mismo lado no se superponen', () => {
  for (const capacidad of [5, 6, 7, 8, 12]) {
    const d = disenarMesa(capacidad);
    const arriba = d.sillas.filter(s => s.y < d.cuerpo.y).sort((a, b) => a.x - b.x);
    const abajo = d.sillas.filter(s => s.y >= d.cuerpo.y + d.cuerpo.h).sort((a, b) => a.x - b.x);
    for (const fila of [arriba, abajo]) {
      for (let i = 1; i < fila.length; i++) {
        assert.ok(fila[i].x >= fila[i - 1].x + fila[i - 1].w, `superposición con capacidad ${capacidad}`);
      }
    }
  }
});

test('la mesa alargada reparte las sillas entre los dos lados largos', () => {
  const d = disenarMesa(7);
  const arriba = d.sillas.filter(s => s.y < d.cuerpo.y).length;
  const abajo = d.sillas.filter(s => s.y >= d.cuerpo.y + d.cuerpo.h).length;
  assert.equal(arriba, 4);
  assert.equal(abajo, 3);
});

test('una capacidad no válida se trata como una silla', () => {
  assert.equal(disenarMesa(0).sillas.length, 1);
  assert.equal(disenarMesa(-3).sillas.length, 1);
  assert.equal(disenarMesa(Number.NaN).sillas.length, 1);
});
