import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import ts from 'typescript'

const source = readFileSync(new URL('./mesaCode.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 },
})
const { mesaCode, siguienteNumeroMesa, etiquetaMesa } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

test('el código conserva los números existentes y rellena los de un dígito', () => {
  assert.equal(mesaCode(1), 'M01')
  assert.equal(mesaCode(12), 'M12')
  assert.equal(mesaCode(100), 'M100')
})

test('sugiere el primer número libre y reserva también los deshabilitados', () => {
  assert.equal(siguienteNumeroMesa([1, 3]), 2)
  assert.equal(siguienteNumeroMesa(Array.from({ length: 99 }, (_, i) => i + 1)), null)
})

test('resuelve el código del pedido desde el ID persistente', () => {
  assert.equal(etiquetaMesa(42, [{ id: 42, numero: 1 }]), 'Mesa M01')
  assert.equal(etiquetaMesa(42, []), 'Mesa (ID 42)')
})
