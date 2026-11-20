import test from 'node:test'
import assert from 'node:assert/strict'
import { textoAccionConfirmacion, textoProgresoEnvio } from './estadoEnvio.js'

const casos = [
  { nombre: 'ruta con correo antes de enviar', permiteCorreo: true, guardado: null, enviando: false, esperado: 'Guardar y enviar' },
  { nombre: 'ruta sin correo antes de guardar', permiteCorreo: false, guardado: null, enviando: false, esperado: 'Guardar reporte' },
  { nombre: 'ruta nula antes de guardar', permiteCorreo: undefined, guardado: null, enviando: false, esperado: 'Guardar reporte' },
  { nombre: 'envío a entidad con correo', permiteCorreo: true, guardado: null, enviando: true, esperado: 'Guardando y enviando…' },
  { nombre: 'guardado para entidad sin correo', permiteCorreo: false, guardado: { codigoSeguimiento: 'NU-1' }, enviando: true, esperado: 'Guardando…' },
  { nombre: 'error recuperable tras guardar', permiteCorreo: true, guardado: { codigoSeguimiento: 'NU-1' }, enviando: false, esperado: 'Reintentar' },
]

test('calcula la acción según el estado real del envío', () => {
  for (const caso of casos) {
    assert.equal(textoAccionConfirmacion(caso), caso.esperado, caso.nombre)
  }
})

test('centraliza el progreso para rutas con y sin correo', () => {
  assert.equal(textoProgresoEnvio(true), 'Guardando y enviando…')
  assert.equal(textoProgresoEnvio(false), 'Guardando…')
})
