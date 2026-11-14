import assert from 'node:assert/strict'
import test from 'node:test'
import { hayPantallaAnterior, subirAlInicio } from './navegacion.js'

test('si la ruta se abrió directo no hay pantalla anterior; si se llegó desde otra, sí', () => {
  assert.equal(hayPantallaAnterior({ key: 'default' }), false)
  assert.equal(hayPantallaAnterior({ key: 'a1b2c3' }), true)
})

test('subir al inicio lleva el scroll a la esquina de arriba', () => {
  const llamadas = []
  subirAlInicio({ scrollTo: (x, y) => llamadas.push([x, y]) })
  assert.deepEqual(llamadas, [[0, 0]])
  assert.doesNotThrow(() => subirAlInicio(undefined))
})
