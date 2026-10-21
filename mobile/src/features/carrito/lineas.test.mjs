import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import ts from 'typescript';

const { outputText } = ts.transpileModule(readFileSync(new URL('./lineas.ts', import.meta.url), 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 },
});
const { claveLinea, aLineaPedido } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`);
const base = { producto: { id: 2 }, cantidad: 1, extras: [{ id: 1 }, { id: 3 }], ingredientesRemovidos: [] };

test('separa el producto normal del producto sin ingredientes', () => {
  assert.notEqual(claveLinea(base), claveLinea({ ...base, ingredientesRemovidos: ['Cebolla'] }));
});
test('el orden de selección no crea combinaciones distintas', () => {
  const item = { ...base, ingredientesRemovidos: ['Cebolla', 'Tomate'] };
  assert.equal(claveLinea(item), claveLinea({ ...item, extras: [...item.extras].reverse(), ingredientesRemovidos: ['Tomate', 'Cebolla'] }));
});
test('transmite ingredientes sin inventar precios en el contrato', () => {
  assert.deepEqual(aLineaPedido({ ...base, ingredientesRemovidos: ['Cebolla'] }), {
    productoId: 2, cantidad: 1, extraIds: [1, 3], ingredientesRemovidos: ['Cebolla'], observaciones: undefined,
  });
});
