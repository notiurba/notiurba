import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  soloErrores,
  validarConfirmacion,
  validarCorreo,
  validarObligatorio,
  validarPasswordNueva,
  validarTelefono,
} from './campos.js'

test('obligatorio: vacío o solo espacios da error; con texto no', () => {
  assert.equal(validarObligatorio('', 'Falta'), 'Falta')
  assert.equal(validarObligatorio('   ', 'Falta'), 'Falta')
  assert.equal(validarObligatorio('Ana', 'Falta'), '')
})

test('correo: vacío, sin arroba y válido', () => {
  assert.notEqual(validarCorreo(''), '')
  assert.notEqual(validarCorreo('ana.correo.com'), '')
  assert.notEqual(validarCorreo('ana@correo'), '')
  assert.equal(validarCorreo(' ana@correo.com '), '')
})

test('teléfono: opcional, pero si se escribe son 10 dígitos exactos', () => {
  assert.equal(validarTelefono(''), '')
  assert.equal(validarTelefono('3001234567'), '')
  assert.notEqual(validarTelefono('30012'), '') // corto (caso del prototipo)
  assert.notEqual(validarTelefono('30012345678'), '') // 11 dígitos
  assert.notEqual(validarTelefono('300 123 4567'), '') // con espacios
  assert.notEqual(validarTelefono('30012345ab'), '') // letras
})

test('contraseña nueva: 8+ caracteres con letra y número', () => {
  assert.notEqual(validarPasswordNueva(''), '')
  assert.notEqual(validarPasswordNueva('abc123'), '') // 6 caracteres
  assert.notEqual(validarPasswordNueva('abcdefgh'), '') // sin número
  assert.notEqual(validarPasswordNueva('12345678'), '') // sin letra
  assert.equal(validarPasswordNueva('Clave1234'), '')
  assert.equal(validarPasswordNueva('clave-1234!'), '') // admite símbolos
})

test('confirmación: debe coincidir', () => {
  assert.notEqual(validarConfirmacion('Clave1234', ''), '')
  assert.notEqual(validarConfirmacion('Clave1234', 'Clave12345'), '')
  assert.equal(validarConfirmacion('Clave1234', 'Clave1234'), '')
})

test('soloErrores: descarta los campos sin error', () => {
  assert.deepEqual(soloErrores({ a: '', b: 'Mal', c: '' }), { b: 'Mal' })
  assert.deepEqual(soloErrores({ a: '' }), {})
})
