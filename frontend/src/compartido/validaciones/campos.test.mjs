import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  documentoEsNumerico,
  limpiarNumeroDocumento,
  soloErrores,
  validarDireccion,
  validarNombre,
  validarNumeroDocumento,
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
  assert.equal(validarPasswordNueva('Contraseña1'), '') // letras con tilde o ñ cuentan, igual que en el backend
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

test('documento CC: solo 6 a 10 dígitos', () => {
  assert.equal(validarNumeroDocumento('1112345678', 1), '')
  assert.equal(validarNumeroDocumento('123456', 1), '')
  assert.notEqual(validarNumeroDocumento('12345', 1), '') // corto
  assert.notEqual(validarNumeroDocumento('12345678901', 1), '') // 11 dígitos
  assert.notEqual(validarNumeroDocumento('11123A5678', 1), '') // con letra
  assert.notEqual(validarNumeroDocumento('', 1), '')
})

test('documento CE: dígitos de 6 a 10', () => {
  assert.equal(validarNumeroDocumento('1234567', 2), '')
  assert.notEqual(validarNumeroDocumento('AB12345', 2), '')
})

test('documento pasaporte: letras y números de 6 a 15', () => {
  assert.equal(validarNumeroDocumento('AB1234567', 4), '')
  assert.notEqual(validarNumeroDocumento('AB-12345', 4), '') // símbolo
  assert.notEqual(validarNumeroDocumento('AB12', 4), '') // corto
  assert.notEqual(validarNumeroDocumento('A'.repeat(16), 4), '') // largo
})

test('limpiar documento mientras se escribe', () => {
  assert.equal(limpiarNumeroDocumento('1.112-345 abc678', 1), '1112345678')
  assert.equal(limpiarNumeroDocumento('12345678901234', 2), '1234567890') // máximo 10
  assert.equal(limpiarNumeroDocumento('ab-12 34', 4), 'AB1234') // pasaporte: mayúsculas, sin símbolos
})

test('nombre: letras con tildes y ñ; sin números ni exceso de largo', () => {
  assert.equal(validarNombre('María Camila', 'Falta'), '')
  assert.equal(validarNombre("D'Angelo-Muñoz", 'Falta'), '')
  assert.equal(validarNombre('  ', 'Falta'), 'Falta')
  assert.notEqual(validarNombre('Ana123', 'Falta'), '')
  assert.notEqual(validarNombre('A'.repeat(61), 'Falta'), '')
  assert.notEqual(validarNombre('A', 'Falta'), '') // al menos 2 letras
  assert.equal(validarNombre('Jo', 'Falta'), '')
  assert.notEqual(validarNombre('Ana@correo.com', 'Falta'), '')
})

test('dirección: de 5 a 150 caracteres', () => {
  assert.equal(validarDireccion('Cra 26 #28-40'), '')
  assert.notEqual(validarDireccion(''), '')
  assert.notEqual(validarDireccion('Cra'), '')
  assert.notEqual(validarDireccion('x'.repeat(151)), '')
})

test('correo y contraseña demasiado largos se rechazan', () => {
  assert.notEqual(validarCorreo(`${'a'.repeat(250)}@correo.com`), '')
  assert.notEqual(validarPasswordNueva(`Clave1${'x'.repeat(80)}`), '')
})

test('documentoEsNumerico: las cédulas son números; el pasaporte admite letras', () => {
  assert.equal(documentoEsNumerico(1), true)
  assert.equal(documentoEsNumerico('2'), true)
  assert.equal(documentoEsNumerico('4'), false)
  assert.equal(documentoEsNumerico(''), false)
})
