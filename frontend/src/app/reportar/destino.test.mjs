import test from 'node:test'
import assert from 'node:assert/strict'
import { destinoTrasCategoria, redireccionPorCategoria, RUTA_CANALES } from './destino.js'

const conCorreo = { idCategoria: 1, permiteCorreo: true }
const sinCorreo = { idCategoria: 14, permiteCorreo: false }

test('tras el paso 1, gas, energía y aseo van directo a sus canales; el resto sigue a la ubicación', () => {
  assert.equal(destinoTrasCategoria(sinCorreo), RUTA_CANALES)
  assert.equal(destinoTrasCategoria(conCorreo), '/reportar/ubicacion')
  assert.equal(destinoTrasCategoria({ idCategoria: 10 }), '/reportar/ubicacion')
})

test('quien llega por URL a los pasos 2 a 4 con una categoría sin correo va a los canales', () => {
  for (const paso of ['/reportar/ubicacion', '/reportar/detalles', '/reportar/confirmacion']) {
    assert.equal(redireccionPorCategoria(paso, sinCorreo, null), RUTA_CANALES)
    assert.equal(redireccionPorCategoria(paso, conCorreo, null), null)
  }
})

test('la pantalla de canales solo es para categorías sin correo y no toca un reporte ya guardado', () => {
  assert.equal(redireccionPorCategoria(RUTA_CANALES, conCorreo, null), '/reportar')
  assert.equal(redireccionPorCategoria(RUTA_CANALES, sinCorreo, null), null)
  assert.equal(redireccionPorCategoria('/reportar/confirmacion', sinCorreo, { codigoSeguimiento: 'NU-000001' }), null)
  assert.equal(redireccionPorCategoria('/reportar', sinCorreo, null), null)
  assert.equal(redireccionPorCategoria('/reportar/ubicacion', undefined, null), null)
})
