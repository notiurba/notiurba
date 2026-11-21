import test from 'node:test'
import assert from 'node:assert/strict'
import { textoAccionConfirmacion, textoProgresoEnvio } from './estadoEnvio.js'

const reporteGuardado = { codigoSeguimiento: 'NU-1' }

const acciones = [
  { guardado: null, permiteCorreo: true, esperado: 'Guardar y enviar' },
  { guardado: null, permiteCorreo: false, esperado: 'Guardar reporte' },
  { guardado: null, permiteCorreo: undefined, esperado: 'Guardar reporte' },
  { guardado: reporteGuardado, permiteCorreo: true, esperado: 'Reintentar' },
  { guardado: reporteGuardado, permiteCorreo: false, esperado: 'Reintentar' },
  { guardado: reporteGuardado, permiteCorreo: undefined, esperado: 'Reintentar' },
]

test('la acción depende de si el reporte ya se guardó y de si la entidad atiende por correo', () => {
  for (const { esperado, ...estado } of acciones) {
    assert.equal(textoAccionConfirmacion(estado), esperado, `guardado=${Boolean(estado.guardado)} permiteCorreo=${estado.permiteCorreo}`)
  }
})

const progresos = [
  { permiteCorreo: true, esperado: 'Guardando y enviando…' },
  { permiteCorreo: false, esperado: 'Guardando…' },
  { permiteCorreo: undefined, esperado: 'Guardando…' },
]

test('el progreso dice «enviando» solo si la entidad atiende por correo', () => {
  for (const { permiteCorreo, esperado } of progresos) {
    assert.equal(textoProgresoEnvio(permiteCorreo), esperado, String(permiteCorreo))
  }
})
