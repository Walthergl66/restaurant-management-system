import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import ts from 'typescript';

const { outputText } = ts.transpileModule(readFileSync(new URL('./agrupar.ts', import.meta.url), 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 },
});
const { agruparPorEstado, etiquetaOrigen, accionesDe } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`);

const comanda = (id, numeroComanda, estado, extra = {}) => ({
  id, numeroComanda, estado, tipo: 'ORDEN', canal: 'SALON', pedidoCodigo: `P-${id}`, areaId: 1, areaNombre: 'Cocina', lineas: [], ...extra,
});

test('agruparPorEstado separa por estado y ordena por número de comanda', () => {
  const grupos = agruparPorEstado([
    comanda(1, 30, 'PENDIENTE'),
    comanda(2, 10, 'PENDIENTE'),
    comanda(3, 20, 'EN_PREPARACION'),
    comanda(4, 5, 'LISTO'),
  ]);
  assert.deepEqual(grupos.PENDIENTE.map(c => c.id), [2, 1]);
  assert.deepEqual(grupos.EN_PREPARACION.map(c => c.id), [3]);
  assert.deepEqual(grupos.LISTO.map(c => c.id), [4]);
});

test('las cancelaciones pendientes van primero', () => {
  const grupos = agruparPorEstado([
    comanda(1, 10, 'PENDIENTE'),
    comanda(2, 50, 'PENDIENTE', { tipo: 'CANCELACION' }),
    comanda(3, 20, 'PENDIENTE'),
  ]);
  assert.deepEqual(grupos.PENDIENTE.map(c => c.id), [2, 1, 3]);
});

test('una lista vacía devuelve los tres grupos vacíos', () => {
  assert.deepEqual(agruparPorEstado([]), { PENDIENTE: [], EN_PREPARACION: [], LISTO: [] });
});

test('etiquetaOrigen: mesa del salón', () => {
  assert.equal(etiquetaOrigen(comanda(1, 1, 'PENDIENTE', { mesaNumero: 5 })), 'Mesa 5');
});

test('etiquetaOrigen: pedidos de la app según su forma de entrega', () => {
  assert.equal(etiquetaOrigen(comanda(1, 1, 'PENDIENTE', { canal: 'APP', entrega: 'RETIRAR' })), 'Retiro');
  assert.equal(etiquetaOrigen(comanda(1, 1, 'PENDIENTE', { canal: 'APP', entrega: 'DOMICILIO' })), 'Domicilio');
});

test('etiquetaOrigen nunca muestra "Mesa undefined"', () => {
  assert.equal(etiquetaOrigen(comanda(1, 1, 'PENDIENTE', { canal: 'APP' })), 'Pedido app');
  assert.equal(etiquetaOrigen(comanda(1, 1, 'PENDIENTE', { canal: 'SALON' })), 'Salón');
});

test('accionesDe: una orden pendiente se puede preparar o terminar de una vez', () => {
  assert.deepEqual(accionesDe(comanda(1, 1, 'PENDIENTE')), ['PREPARAR', 'LISTO']);
});

test('accionesDe: una orden en preparación solo se puede terminar', () => {
  assert.deepEqual(accionesDe(comanda(1, 1, 'EN_PREPARACION')), ['LISTO']);
});

test('accionesDe: una orden lista no tiene acciones', () => {
  assert.deepEqual(accionesDe(comanda(1, 1, 'LISTO')), []);
});

test('accionesDe: una cancelación solo se da por enterada, mientras no esté lista', () => {
  assert.deepEqual(accionesDe(comanda(1, 1, 'PENDIENTE', { tipo: 'CANCELACION' })), ['ENTERADO']);
  assert.deepEqual(accionesDe(comanda(1, 1, 'EN_PREPARACION', { tipo: 'CANCELACION' })), ['ENTERADO']);
  assert.deepEqual(accionesDe(comanda(1, 1, 'LISTO', { tipo: 'CANCELACION' })), []);
});
