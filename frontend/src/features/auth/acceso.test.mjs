import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import ts from 'typescript'

const { outputText } = ts.transpileModule(readFileSync(new URL('./acceso.ts', import.meta.url), 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 },
})
const { puedeUsarWeb, MENSAJE_SOLO_MOVIL, SoloMovilError } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

test('el administrador y el cajero pueden usar la web', () => {
  assert.equal(puedeUsarWeb({ rol: 'ADMIN' }), true)
  assert.equal(puedeUsarWeb({ rol: 'CAJERO' }), true)
})

test('mesero, cocina, repartidor y cliente usan la app móvil', () => {
  for (const rol of ['MESERO', 'COCINA', 'DELIVERY', 'CLIENTE']) {
    assert.equal(puedeUsarWeb({ rol }), false, rol)
  }
})

test('un mesero no entra a la web aunque tenga permisos de administración marcados', () => {
  assert.equal(puedeUsarWeb({ rol: 'MESERO', permisos: ['usuarios:ver', 'reportes:ver', 'caja:cierre'] }), false)
})

test('sin usuario, sin rol o con un rol desconocido no se permite', () => {
  assert.equal(puedeUsarWeb(null), false)
  assert.equal(puedeUsarWeb(undefined), false)
  assert.equal(puedeUsarWeb({}), false)
  assert.equal(puedeUsarWeb({ rol: null }), false)
  assert.equal(puedeUsarWeb({ rol: 'SUPERVISOR' }), false)
})

test('el mensaje indica usar la aplicación móvil y el error lo lleva', () => {
  assert.match(MENSAJE_SOLO_MOVIL, /aplicación móvil/)
  const error = new SoloMovilError()
  assert.equal(error.message, MENSAJE_SOLO_MOVIL)
  assert.ok(error instanceof Error)
})
