import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import ts from 'typescript';

const transpilar = (archivo) => ts.transpileModule(readFileSync(new URL(archivo, import.meta.url), 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 },
}).outputText;
const comoModulo = (codigo) => `data:text/javascript;base64,${Buffer.from(codigo).toString('base64')}`;

// rutaInicial.ts importa ./permisos: se enlaza la versión transpilada para no duplicar las reglas.
const permisosUrl = comoModulo(transpilar('./permisos.ts'));
const { grupoInicio } = await import(comoModulo(transpilar('./rutaInicial.ts').replace("'./permisos'", `'${permisosUrl}'`)));

const usuario = (...permisos) => ({ id: 1, username: 'u', nombre: 'U', rol: 'X', permisos });

test('repartidor va a delivery', () => {
  assert.equal(grupoInicio(usuario('delivery:mis-entregas')), '/(delivery)');
});

test('cocinero va a cocina aunque conserve permisos de mesero', () => {
  assert.equal(grupoInicio(usuario('pedidos:estado-preparacion', 'pedidos:crear')), '/(kitchen)');
});

test('mesero va a la pantalla de mesero', () => {
  assert.equal(grupoInicio(usuario('pedidos:crear', 'mesas:ver')), '/(waiter)');
});

test('el repartidor tiene prioridad sobre cocina y mesero', () => {
  assert.equal(grupoInicio(usuario('delivery:mis-entregas', 'pedidos:estado-preparacion', 'pedidos:crear')), '/(delivery)');
});

test('cliente y sesión ausente van a las pestañas', () => {
  assert.equal(grupoInicio(usuario('clientes:menu-ver')), '/(tabs)');
  assert.equal(grupoInicio(null), '/(tabs)');
  assert.equal(grupoInicio({ id: 1, username: 'u', nombre: 'U', rol: 'CLIENTE' }), '/(tabs)');
});
