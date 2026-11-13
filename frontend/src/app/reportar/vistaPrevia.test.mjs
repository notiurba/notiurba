import test from 'node:test'
import assert from 'node:assert/strict'
import { estadoVistaPrevia, explicarErrorVistaPrevia } from './vistaPrevia.js'

test('sin correo o con reporte guardado no hay vista previa ni carga', () => {
  assert.deepEqual(estadoVistaPrevia({ clave: 'a', datos: { texto: 'x' } }, null), { datos: null, error: null, cargando: false })
})

test('al cambiar los datos se muestra «cargando» hasta que llegue la vista previa de esos datos', () => {
  assert.equal(estadoVistaPrevia(null, '{"id":1}').cargando, true)
  const vieja = { clave: '{"id":1}', datos: { texto: 'viejo' }, error: null }
  assert.deepEqual(estadoVistaPrevia(vieja, '{"id":2}'), { datos: null, error: null, cargando: true })
  assert.deepEqual(estadoVistaPrevia(vieja, '{"id":1}'), { datos: { texto: 'viejo' }, error: null, cargando: false })
})

test('un error del borrador se explica y no ofrece reintentar; uno de red sí', () => {
  assert.deepEqual(explicarErrorVistaPrevia({ estado: 422, mensaje: 'El punto está fuera de Tuluá' }),
    { mensaje: 'El punto está fuera de Tuluá', reintentar: false })
  assert.equal(explicarErrorVistaPrevia({ estado: 503 }).reintentar, true)
  assert.equal(explicarErrorVistaPrevia(new Error('red')).reintentar, true)
})
