import test from 'node:test'
import assert from 'node:assert/strict'
import { textoAccionConfirmacion, textoProgresoEnvio } from './estadoEnvio.js'

test('el texto de progreso no anuncia un correo para una entidad sin correo', () => {
  assert.equal(textoProgresoEnvio(true), 'Guardando y enviando…')
  assert.equal(textoProgresoEnvio(false), 'Guardando…')
  assert.equal(textoProgresoEnvio(undefined), 'Guardando…')
})

test('la acción de reintento solo aparece cuando ya existe un reporte guardado', () => {
  assert.equal(textoAccionConfirmacion({ guardado: null, permiteCorreo: true }), 'Guardar y enviar')
  assert.equal(textoAccionConfirmacion({ guardado: null, permiteCorreo: false }), 'Guardar reporte')
  assert.equal(textoAccionConfirmacion({ guardado: { codigoSeguimiento: 'NU-1' }, permiteCorreo: true }), 'Reintentar')
})
