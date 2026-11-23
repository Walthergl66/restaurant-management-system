import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import ts from 'typescript';

const { outputText } = ts.transpileModule(readFileSync(new URL('./unir.ts', import.meta.url), 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 },
});
const { desdeSalon, desdeRetiro, unirListos, etiquetaEntrega } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`);

const salon = (codigo, mesaId, createdAt, lineas = []) => ({ codigo, mesaId, estado: 'LISTO', createdAt, lineas });
const retiro = (codigo, clienteNombre = 'Ana') => ({ codigo, estado: 'LISTO', entrega: 'RETIRAR', clienteNombre, lineas: [{ nombreProducto: 'Burger', cantidad: 2 }] });

test('desdeSalon toma el número de la mesa y las líneas del pedido', () => {
  const pedido = desdeSalon(
    salon('S-1', 7, '2026-11-21T10:00:00Z', [{ nombreProducto: 'Lomo', cantidad: 3, precioUnitario: 9 }]),
    new Map([[7, 12]]),
  );
  assert.equal(pedido.origen, 'SALON');
  assert.equal(pedido.mesaNumero, 12);
  assert.deepEqual(pedido.lineas, [{ nombreProducto: 'Lomo', cantidad: 3 }]);
});

test('desdeSalon sin la mesa en el mapa deja el número sin definir (no inventa uno)', () => {
  const pedido = desdeSalon(salon('S-2', 99, '2026-11-21T10:00:00Z'), new Map());
  assert.equal(pedido.mesaNumero, undefined);
});

test('desdeRetiro conserva el cliente y las líneas', () => {
  const pedido = desdeRetiro(retiro('R-1', 'Luis'));
  assert.equal(pedido.origen, 'RETIRO');
  assert.equal(pedido.clienteNombre, 'Luis');
  assert.deepEqual(pedido.lineas, [{ nombreProducto: 'Burger', cantidad: 2 }]);
});

test('unirListos pone primero los de salón por antigüedad y luego los de retiro en su orden', () => {
  const lista = unirListos(
    [
      desdeSalon(salon('S-nuevo', 1, '2026-11-21T11:00:00Z'), new Map()),
      desdeSalon(salon('S-viejo', 1, '2026-11-21T09:00:00Z'), new Map()),
    ],
    [desdeRetiro(retiro('R-1')), desdeRetiro(retiro('R-2'))],
  );
  assert.deepEqual(lista.map(p => p.codigo), ['S-viejo', 'S-nuevo', 'R-1', 'R-2']);
  assert.deepEqual(lista.map(p => p.origen), ['SALON', 'SALON', 'RETIRO', 'RETIRO']);
});

test('unirListos sin pedidos de salón (cocina) devuelve solo los de retiro', () => {
  const lista = unirListos([], [desdeRetiro(retiro('R-1'))]);
  assert.deepEqual(lista.map(p => p.codigo), ['R-1']);
});

test('unirListos con listas vacías devuelve una lista vacía y no modifica las originales', () => {
  assert.deepEqual(unirListos([], []), []);
  const originalSalon = [desdeSalon(salon('S-1', 1, '2026-11-21T09:00:00Z'), new Map())];
  unirListos(originalSalon, []);
  assert.equal(originalSalon.length, 1);
});

test('etiquetaEntrega: mesa del salón o retiro con el nombre del cliente', () => {
  assert.equal(etiquetaEntrega(desdeSalon(salon('S-1', 1, 'x'), new Map([[1, 5]]))), 'Mesa 5');
  assert.equal(etiquetaEntrega(desdeSalon(salon('S-2', 1, 'x'), new Map())), 'Salón');
  assert.equal(etiquetaEntrega(desdeRetiro(retiro('R-1', 'Ana'))), 'Retiro · Ana');
  assert.equal(etiquetaEntrega(desdeRetiro({ ...retiro('R-2'), clienteNombre: undefined })), 'Retiro');
});
