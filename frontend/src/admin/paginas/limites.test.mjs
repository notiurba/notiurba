import assert from 'node:assert/strict'
import { test } from 'node:test'
import { aFormulario, aSolicitud, validarLimites } from './limites.js'

const delServicio = {
  login: { intentosMaximos: 3, ventanaMinutos: 5, bloqueoMinutos: 5 },
  reportesMaximosPorDia: 2,
  duplicados: { ventanaDias: 30, categorias: [
    { idCategoria: 1, nombre: 'Vías, huecos, andenes e infraestructura', metros: 5 },
    { idCategoria: 12, nombre: 'Gas natural', metros: 15 }] },
}

test('los valores del servicio van y vuelven sin cambiar', () => {
  const formulario = aFormulario(delServicio)
  assert.deepEqual(validarLimites(formulario), {})
  const solicitud = aSolicitud(formulario)
  assert.deepEqual(solicitud.login, delServicio.login)
  assert.equal(solicitud.reportesMaximosPorDia, 2)
  assert.deepEqual(solicitud.duplicados.categorias, [{ idCategoria: 1, metros: 5 }, { idCategoria: 12, metros: 15 }])
})

test('cada campo fuera de su rango tiene su mensaje junto al campo', () => {
  const f = { ...aFormulario(delServicio), intentosMaximos: '0', ventanaMinutos: '61', bloqueoMinutos: '60',
    reportesMaximosPorDia: '51', ventanaDias: '31' }
  f.categorias = [{ ...f.categorias[0], metros: '4' }, { ...f.categorias[1], metros: '500' }]
  const errores = validarLimites(f)
  assert.equal(errores.intentosMaximos, 'Use un valor entre 1 y 10.')
  assert.equal(errores.ventanaMinutos, 'Use un valor entre 1 y 60 minutos.')
  assert.equal(errores.bloqueoMinutos, undefined)
  assert.equal(errores.reportesMaximosPorDia, 'Use un valor entre 1 y 50.')
  assert.equal(errores.ventanaDias, 'Use un valor entre 1 y 30 días.')
  assert.equal(errores['categorias.1.metros'], 'Use un valor entre 5 y 500 metros.')
  assert.equal(errores['categorias.12.metros'], undefined)
})

test('vacío, decimales o letras piden un número entero', () => {
  for (const valor of ['', '2.5', 'dos', '-1', ' ']) {
    assert.equal(validarLimites({ ...aFormulario(delServicio), reportesMaximosPorDia: valor }).reportesMaximosPorDia,
      'Escriba un número entero.', JSON.stringify(valor))
  }
  assert.deepEqual(validarLimites({ ...aFormulario(delServicio), reportesMaximosPorDia: ' 7 ' }), {})
})
