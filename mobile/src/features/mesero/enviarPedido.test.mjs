import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import ts from 'typescript';

const { outputText } = ts.transpileModule(readFileSync(new URL('./enviarPedido.ts', import.meta.url), 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 },
});
const { enviarPedidoMesa } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`);

const lineas = [
  { productoId: 1, cantidad: 2, extraIds: [] },
  { productoId: 2, cantidad: 1, extraIds: [5] },
];

/** API falsa: registra las llamadas y falla en el paso indicado. */
function apiFalsa({ falla, fallaCancelar = false } = {}) {
  const llamadas = [];
  const paso = (nombre, fn = () => {}) => async (...args) => {
    llamadas.push(nombre);
    if (falla === nombre || (falla === 'segundaLinea' && nombre === 'agregarLinea' && llamadas.filter(l => l === 'agregarLinea').length === 2)) {
      throw new Error(`falla ${falla}`);
    }
    return fn(...args);
  };
  return {
    llamadas,
    api: {
      crear: paso('crear'),
      agregarLinea: paso('agregarLinea'),
      confirmar: paso('confirmar'),
      cancelarBorrador: async () => {
        llamadas.push('cancelarBorrador');
        if (fallaCancelar) throw new Error('falla cancelar');
      },
    },
  };
}

test('camino feliz: crea, agrega cada línea y confirma sin cancelar', async () => {
  const { api, llamadas } = apiFalsa();
  await enviarPedidoMesa(api, 3, 'MOB-1', lineas, 'sin sal');
  assert.deepEqual(llamadas, ['crear', 'agregarLinea', 'agregarLinea', 'confirmar']);
});

test('si falla la segunda línea cancela el borrador y relanza el error original', async () => {
  const { api, llamadas } = apiFalsa({ falla: 'segundaLinea' });
  await assert.rejects(() => enviarPedidoMesa(api, 3, 'MOB-1', lineas), /falla segundaLinea/);
  assert.deepEqual(llamadas, ['crear', 'agregarLinea', 'agregarLinea', 'cancelarBorrador']);
});

test('si falla confirmar cancela el borrador', async () => {
  const { api, llamadas } = apiFalsa({ falla: 'confirmar' });
  await assert.rejects(() => enviarPedidoMesa(api, 3, 'MOB-1', lineas), /falla confirmar/);
  assert.equal(llamadas.at(-1), 'cancelarBorrador');
});

test('si falla crear no hay borrador que cancelar', async () => {
  const { api, llamadas } = apiFalsa({ falla: 'crear' });
  await assert.rejects(() => enviarPedidoMesa(api, 3, 'MOB-1', lineas), /falla crear/);
  assert.deepEqual(llamadas, ['crear']);
});

test('si cancelar también falla se propaga el error original', async () => {
  const { api } = apiFalsa({ falla: 'confirmar', fallaCancelar: true });
  await assert.rejects(() => enviarPedidoMesa(api, 3, 'MOB-1', lineas), /falla confirmar/);
});

test('pasa a la API la mesa, el código y las notas', async () => {
  const recibido = {};
  const api = {
    crear: async (codigo, mesaId, notas) => { Object.assign(recibido, { codigo, mesaId, notas }); },
    agregarLinea: async () => {},
    confirmar: async (codigo) => { recibido.confirmado = codigo; },
    cancelarBorrador: async () => {},
  };
  await enviarPedidoMesa(api, 7, 'MOB-9', lineas, 'sin sal');
  assert.deepEqual(recibido, { codigo: 'MOB-9', mesaId: 7, notas: 'sin sal', confirmado: 'MOB-9' });
});
