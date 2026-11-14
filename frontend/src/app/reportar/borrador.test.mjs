import assert from 'node:assert/strict'
import { test } from 'node:test'
import { aplicarCambio } from './borrador.js'

const base = { idCategoria: '1', latitud: 4.08, longitud: -76.19, direccion: 'Cra 1', idBarrio: '5', descripcion: 'Hueco',
  matricula: '123', emergencia: true, fotoObligatoria: true }

test('otra categoría reinicia el lugar, la matrícula, la emergencia y la foto obligatoria', () => {
  const r = aplicarCambio(base, { idCategoria: '2' })
  assert.equal(r.idCategoria, '2')
  assert.equal(r.latitud, null)
  assert.equal(r.matricula, '')
  assert.equal(r.emergencia, false)
  assert.equal(r.fotoObligatoria, false)
  assert.equal(r.descripcion, 'Hueco')
})

test('otro punto en el mapa deja de exigir la foto; la misma categoría y el mismo punto la conservan', () => {
  assert.equal(aplicarCambio(base, { latitud: 4.09 }).fotoObligatoria, false)
  assert.equal(aplicarCambio(base, { longitud: -76.2 }).fotoObligatoria, false)
  assert.equal(aplicarCambio(base, { latitud: 4.08, longitud: -76.19 }).fotoObligatoria, true)
  assert.equal(aplicarCambio(base, { descripcion: 'Otro texto' }).fotoObligatoria, true)
  assert.equal(aplicarCambio(base, { idCategoria: '1' }).fotoObligatoria, true)
})

test('marcar la foto como obligatoria se respeta', () => {
  assert.equal(aplicarCambio({ ...base, fotoObligatoria: false }, { fotoObligatoria: true }).fotoObligatoria, true)
})
