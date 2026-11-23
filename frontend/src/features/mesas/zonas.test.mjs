import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import ts from 'typescript'

const { outputText } = ts.transpileModule(readFileSync(new URL('./zonas.ts', import.meta.url), 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 },
})
const { moverZona, agruparMesasPorZona } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

const zona = (id, nombre, orden, cantidadMesas = 0) => ({ id, nombre, orden, cantidadMesas })
const mesa = (id, numero, zonaId) => ({ id, numero, capacidad: 4, zonaId, estado: 'LIBRE', activo: true })

test('moverZona sube y baja una zona y devuelve la lista de ids completa', () => {
  const ids = [1, 2, 3]
  assert.deepEqual(moverZona(ids, 1, -1), [2, 1, 3])
  assert.deepEqual(moverZona(ids, 1, 1), [1, 3, 2])
})

test('moverZona no cambia nada en los extremos ni con índices inválidos', () => {
  const ids = [1, 2, 3]
  assert.deepEqual(moverZona(ids, 0, -1), [1, 2, 3])
  assert.deepEqual(moverZona(ids, 2, 1), [1, 2, 3])
  assert.deepEqual(moverZona(ids, 5, -1), [1, 2, 3])
  assert.deepEqual(moverZona(ids, -1, 1), [1, 2, 3])
})

test('moverZona no modifica la lista original', () => {
  const ids = [1, 2, 3]
  moverZona(ids, 0, 1)
  assert.deepEqual(ids, [1, 2, 3])
})

test('agruparMesasPorZona sigue el orden de las zonas e incluye las vacías', () => {
  const zonas = [zona(2, 'Terraza', 2), zona(1, 'Salón', 1), zona(3, 'Patio', 3)]
  const grupos = agruparMesasPorZona(zonas, [mesa(10, 5, 2), mesa(11, 1, 1), mesa(12, 3, 2)])
  assert.deepEqual(grupos.map(g => g.zona.nombre), ['Salón', 'Terraza', 'Patio'])
  assert.deepEqual(grupos[1].mesas.map(m => m.numero), [3, 5])
  assert.equal(grupos[2].mesas.length, 0)
})

test('agruparMesasPorZona pone aparte las mesas cuya zona no está en la lista', () => {
  const grupos = agruparMesasPorZona([zona(1, 'Salón', 1)], [mesa(10, 1, 1), mesa(11, 2, 99)])
  assert.equal(grupos.length, 2)
  assert.equal(grupos[1].zona.id, 0)
  assert.equal(grupos[1].zona.nombre, 'Sin zona')
  assert.deepEqual(grupos[1].mesas.map(m => m.numero), [2])
})

test('agruparMesasPorZona sin zonas ni mesas devuelve una lista vacía', () => {
  assert.deepEqual(agruparMesasPorZona([], []), [])
})
