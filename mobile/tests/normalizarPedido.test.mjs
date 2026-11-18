import test from 'node:test';
import assert from 'node:assert/strict';
import { desdeSpi, desdeRespuestaWeb } from '../src/features/pedidos/normalizarPedido.ts';

const spi = {
  codigo: 'PED-100', estado: 'EN_PREPARACION', metodoPago: ' tarjeta ', metodoEntrega: ' domicilio ',
  subtotal: 20, costoEntrega: 1.5, total: 21.5,
  creadoAt: '2026-11-17T10:00:00-05:00', actualizadoAt: '2026-11-17T15:05:00Z', version: 2,
  lineas: [{ productoId: 7, nombre: ' Hamburguesa ', precio: 10, cantidad: 2, subtotal: 20,
    extras: [{ extraId: 4, nombre: 'Queso', precio: 1.5 }] }],
};

test('P1: traduce el SPI del backend al modelo común', () => {
  const result = desdeSpi(spi);
  assert.equal(result.pago, 'TARJETA');
  assert.equal(result.entrega, 'DOMICILIO');
  assert.equal(result.lineas[0].precioUnitario, 10);
  assert.equal(result.lineas[0].extras[0].id, 4);
  assert.equal(result.creadoEn, '2026-11-17T15:00:00.000Z');
});

test('P2: respuesta web y SPI producen el mismo resultado normalizado', () => {
  const web = {
    codigo: spi.codigo, estado: spi.estado, metodoPago: 'TARJETA', metodoEntrega: 'DOMICILIO',
    costoEntrega: '1.50', total: '21.50', creadoAt: spi.creadoAt,
    actualizadoAt: spi.actualizadoAt,
    lineas: [{ productoId: 7, nombre: 'Hamburguesa', precioUnitario: '10.00', cantidad: 2,
      subtotal: '20.00', extras: [{ id: 4, nombre: 'Queso', precio: '1.50' }] }],
  };
  assert.deepEqual(desdeRespuestaWeb(web), desdeSpi(spi));
});

test('P3: rechaza total inconsistente y fecha sin zona horaria', () => {
  assert.throws(() => desdeSpi({ ...spi, total: 99 }), /no coincide/);
  assert.throws(() => desdeSpi({ ...spi, creadoAt: '2026-11-17T10:00:00' }), /ISO-8601/);
});
