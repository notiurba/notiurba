import assert from 'node:assert/strict'
import { test } from 'node:test'
import { decidirFalloEnvio } from './falloEnvio.js'

const parecido = { esPropio: false, cantidad: 1, estadoExistente: 'Enviado' }

test('antes de guardar, un reporte parecido abre su hoja con los datos base', () => {
  const r = decidirFalloEnvio({ codigo: 'POSIBLE_DUPLICADO', datos: parecido }, null)
  assert.deepEqual(r.parecido, parecido)
  assert.equal(r.error, '')
  assert.equal(r.limite, null)
})

test('antes de guardar, el límite del día abre su hoja y no es un error', () => {
  const r = decidirFalloEnvio({ codigo: 'LIMITE_DIARIO_REPORTES', datos: { maximo: 2 } }, null)
  assert.deepEqual(r.limite, { maximo: 2 })
  assert.equal(r.error, '')
})

test('antes de guardar, la falta de evidencia vuelve obligatoria la foto', () => {
  const r = decidirFalloEnvio({ codigo: 'EVIDENCIA_REQUERIDA' }, null)
  assert.equal(r.fotoObligatoria, true)
  assert.equal(r.error, '')
})

test('sin datos en la respuesta las hojas igual se abren', () => {
  assert.deepEqual(decidirFalloEnvio({ codigo: 'POSIBLE_DUPLICADO' }, null).parecido, {})
  assert.deepEqual(decidirFalloEnvio({ codigo: 'LIMITE_DIARIO_REPORTES' }, null).limite, {})
})

test('si el reporte ya quedó guardado, ninguna hoja se abre: se reintenta sobre el mismo código', () => {
  for (const codigo of ['POSIBLE_DUPLICADO', 'LIMITE_DIARIO_REPORTES', 'EVIDENCIA_REQUERIDA', 'ERROR_INTERNO']) {
    const r = decidirFalloEnvio({ codigo }, { codigoSeguimiento: 'NU-000001' })
    assert.equal(r.parecido, null)
    assert.equal(r.limite, null)
    assert.equal(r.fotoObligatoria, false)
    assert.match(r.error, /Su reporte está guardado/)
  }
})

test('fuera de cobertura y los demás errores se explican', () => {
  assert.match(decidirFalloEnvio({ codigo: 'FUERA_DE_COBERTURA' }, null).error, /fuera de la cobertura/)
  assert.match(decidirFalloEnvio({ codigo: 'SIN_CONEXION' }, null).error, /Sus datos siguen aquí/)
})
