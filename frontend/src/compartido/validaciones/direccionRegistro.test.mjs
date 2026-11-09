import assert from 'node:assert/strict'
import { test } from 'node:test'
import { validarDireccionRegistro } from './direccionRegistro.js'
import { LIMITES, validarDireccion } from './campos.js'

test('registro admite exactamente 100 caracteres y rechaza 101', () => {
  assert.equal(validarDireccionRegistro('D'.repeat(100)), '')
  assert.match(validarDireccionRegistro('D'.repeat(101)), /100/)
})

test('la restricción de registro no altera el límite 150 de otras pantallas', () => {
  assert.equal(LIMITES.direccion, 150)
  assert.equal(validarDireccion('D'.repeat(150)), '')
  assert.match(validarDireccionRegistro('D'.repeat(150)), /no se ha recortado/)
})

test('el valor anterior largo no se modifica y los espacios también cuentan', () => {
  const direccion = `Calle ${'A'.repeat(145)}`
  assert.match(validarDireccionRegistro(direccion), /100/)
  assert.equal(direccion.length, 151)
  assert.match(validarDireccionRegistro('D'.repeat(100) + ' '), /100/)
})

test('registro conserva los requisitos de presencia y dirección completa', () => {
  assert.ok(validarDireccionRegistro(''))
  assert.ok(validarDireccionRegistro('Cra'))
  assert.equal(validarDireccionRegistro('Cra 26 #28-40'), '')
})
