import assert from 'node:assert/strict'
import test from 'node:test'
import { prepararCategorias, ORDEN_REPORTE, esEmergencia } from './catalogo.js'

test('el adaptador conserva las 15 opciones del servidor y sus flags sin modificar la respuesta', () => {
  const rutas = Array.from({ length: 15 }, (_, i) => ({ idCategoria: i + 1, categoria: 'Nombre API ' + (i + 1), requiereUbicacion: i < 5, admiteMatricula: i === 10, permiteCorreo: i !== 12 }))
  const antes = JSON.stringify(rutas)
  const opciones = prepararCategorias(rutas)
  assert.deepEqual(opciones.map(c => c.idCategoria), [...ORDEN_REPORTE.filter(id => id !== 10), 15, 10])
  assert.equal(opciones.find(c => c.idCategoria === 15).etiqueta, 'Nombre API 15')
  assert.equal(opciones.find(c => c.idCategoria === 11).admiteMatricula, true)
  assert.equal(opciones.find(c => c.idCategoria === 13).permiteCorreo, false)
  assert.equal(opciones.find(c => c.idCategoria === 1).nombre, 'Nombre API 1')
  assert.equal(JSON.stringify(rutas), antes)
})

test('conserva una categoría nueva con el nombre y la descripción de la API', () => {
  const opcion = { idCategoria: 99, nombre: 'Categoría nueva del servidor', descripcion: 'Ayuda del servidor', permiteCorreo: false }
  assert.deepEqual(prepararCategorias([opcion])[0], { ...opcion, etiqueta: opcion.nombre })
})

test('no inventa opciones ausentes y el orden de IDs no depende del orden numérico', () => {
  assert.deepEqual(prepararCategorias([]), [])
  assert.deepEqual(prepararCategorias([{ idCategoria: 10 }, { idCategoria: 11 }, { idCategoria: 1 }]).map(c => c.idCategoria), [1, 11, 10])
})

test('gas y emergencia explícita reevaluan el canal; otras descripciones no inventan emergencia', () => {
  assert.equal(esEmergencia({ descripcion: 'Hay olor a gas' }, { esGas: true }), true)
  assert.equal(esEmergencia({ descripcion: 'Sin servicio de gas' }, { esGas: true }), false)
  assert.equal(esEmergencia({ emergencia: true, descripcion: '' }, { esGas: false }), true)
  assert.equal(esEmergencia({ descripcion: 'Hueco en la vía' }, { esGas: false }), false)
})
