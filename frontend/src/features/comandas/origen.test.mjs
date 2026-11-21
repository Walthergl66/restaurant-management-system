import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import ts from 'typescript'

const transpilar = (archivo) => ts.transpileModule(readFileSync(new URL(archivo, import.meta.url), 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 },
}).outputText
const comoModulo = (codigo) => `data:text/javascript;base64,${Buffer.from(codigo).toString('base64')}`

// origen.ts importa el alias @/shared/lib/mesaCode: se enlaza la versión transpilada para no duplicar la regla.
const mesaCodeUrl = comoModulo(transpilar('../../shared/lib/mesaCode.ts'))
const { etiquetaOrigen } = await import(comoModulo(transpilar('./origen.ts').replace("'@/shared/lib/mesaCode'", `'${mesaCodeUrl}'`)))

test('una comanda de salón muestra el código de la mesa', () => {
  assert.equal(etiquetaOrigen({ canal: 'SALON', mesaNumero: 5 }), 'Mesa M05')
  assert.equal(etiquetaOrigen({ canal: 'SALON', mesaNumero: 12 }), 'Mesa M12')
})

test('un pedido de la app muestra si es de retiro o domicilio', () => {
  assert.equal(etiquetaOrigen({ canal: 'APP', entrega: 'RETIRAR' }), 'Retiro')
  assert.equal(etiquetaOrigen({ canal: 'APP', entrega: 'DOMICILIO' }), 'Domicilio')
})

test('nunca muestra "Mesa undefined": sin datos usa una etiqueta genérica', () => {
  assert.equal(etiquetaOrigen({ canal: 'APP' }), 'Pedido app')
  assert.equal(etiquetaOrigen({ canal: 'SALON' }), 'Salón')
  assert.equal(etiquetaOrigen({}), 'Salón')
})
