import assert from 'node:assert/strict'
import test from 'node:test'
import { formatearFecha } from './fechas.js'

test('formatearFecha da día/mes/año con ceros', () => {
  assert.equal(formatearFecha(new Date(2026, 9, 7, 12).toISOString()), '07/10/2026')
})

test('formatearFecha devuelve texto vacío si la fecha no sirve', () => {
  assert.equal(formatearFecha('no es una fecha'), '')
})
