import test from 'node:test'
import assert from 'node:assert/strict'
import { textoAccionConfirmacion } from './estadoEnvio.js'

test('mantiene el progreso mientras conservar el reporte actualiza guardado', () => {
  const rutaConCorreo = { permiteCorreo: true }

  assert.equal(textoAccionConfirmacion({ guardado: null, enviando: false, permiteCorreo: rutaConCorreo.permiteCorreo }), 'Guardar y enviar')

  // guardarConEvidencias conserva el código antes de subir fotos y solicitar
  // el envío. Esta transición era la que cambiaba brevemente a «Reintentar».
  const guardado = { codigoSeguimiento: 'NU-000042' }
  assert.equal(textoAccionConfirmacion({ guardado, enviando: true, permiteCorreo: rutaConCorreo.permiteCorreo }), 'Guardando y enviando…')

  // Solo después de un fallo que devuelve al formulario es válida la acción
  // de recuperación sobre el mismo código, sin crear otro reporte.
  assert.equal(textoAccionConfirmacion({ guardado, enviando: false, permiteCorreo: rutaConCorreo.permiteCorreo }), 'Reintentar')
})
