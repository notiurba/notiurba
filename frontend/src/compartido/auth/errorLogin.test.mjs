import assert from 'node:assert/strict'
import { test } from 'node:test'
import { mensajeErrorLogin } from './errorLogin.js'

test('cada estado del login tiene un mensaje distinto y no expone el cuerpo del servidor', () => {
  const mensajes = [0, 400, 401, 403, 429, 500, 200].map((estado) => mensajeErrorLogin({ estado, mensaje: 'dato técnico privado' }))
  assert.equal(new Set(mensajes).size, mensajes.length)
  for (const mensaje of mensajes) assert.doesNotMatch(mensaje, /privado|incorrect|no encontramos/i)
  assert.match(mensajes[4], /Espere/)
  assert.match(mensajes[5], /no está disponible/)
})
