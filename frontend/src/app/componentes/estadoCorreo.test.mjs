import test from 'node:test'
import assert from 'node:assert/strict'
import { presentacionCorreo, enlaceCanal } from './estadoCorreo.js'

test('guardar un reporte no equivale a correo enviado', () => {
  assert.equal(presentacionCorreo(null).etiqueta, 'Sin enviar')
  assert.equal(presentacionCorreo({ estado: 'PENDIENTE' }).titulo, 'Reporte guardado')
  assert.equal(presentacionCorreo({ estado: 'PENDIENTE' }).etiqueta, 'En camino')
})
test('solo cuando el correo salió se muestra «enviado» y no se promete lectura', () => {
  const e = presentacionCorreo({ estado: 'ENVIADO' })
  assert.equal(e.titulo, 'Reporte enviado')
  assert.match(e.mensaje, /Enviamos su reporte/)
  assert.doesNotMatch(e.mensaje, /Alcaldía recibió|leyó|radicado/)
})
test('fallo inicial conserva el reporte; rebote tiene aviso propio sin retroceso', () => {
  assert.match(presentacionCorreo({ estado: 'FALLIDO' }).mensaje, /quedó guardado/)
  assert.equal(presentacionCorreo({ estado: 'FALLIDO', rebote: true }).clase, 'rebote')
  assert.match(presentacionCorreo({ estado: 'FALLIDO', rebote: true }).mensaje, /sigue guardado/)
})
test('canal externo y gas 164 no simulan envío exitoso', () => {
  const ruta = { canal: 'Teléfono', acceso: '164', mensaje: 'Emergencia de gas: llame al 164' }
  assert.equal(enlaceCanal(ruta), 'tel:164')
  assert.equal(presentacionCorreo({ estado: 'NO_ENVIADO_CORREO', ruta }).etiqueta, 'Otro canal')
  assert.equal(presentacionCorreo({ estado: 'NO_ENVIADO_CORREO' }).titulo, 'Reporte guardado')
})
test('los enlaces de canales rechazan scripts, HTTP y credenciales', () => {
  for (const acceso of ['javascript:alert(1)', 'http://celsia.com', 'https://clave@celsia.com']) assert.equal(enlaceCanal({ acceso }), null)
  assert.equal(enlaceCanal({ acceso: 'https://www.celsia.com/es/zona-clientes/' }), 'https://www.celsia.com/es/zona-clientes/')
})
