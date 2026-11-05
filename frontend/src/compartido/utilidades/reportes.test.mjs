import assert from 'node:assert/strict'
import test from 'node:test'
import { ESTADOS_REPORTE, puedeActualizar, textoLugar } from './reportes.js'

test('solo se puede actualizar un reporte enviado o en trámite', () => {
  assert.equal(puedeActualizar({ estado: 'Enviado' }), true)
  assert.equal(puedeActualizar({ estado: 'En trámite' }), true)
  assert.equal(puedeActualizar({ estado: 'Recibido' }), false)
  assert.equal(puedeActualizar({ estado: 'Resuelto' }), false)
})

test('hay cuatro estados y el primero es Recibido', () => {
  assert.equal(ESTADOS_REPORTE.length, 4)
  assert.equal(ESTADOS_REPORTE[0], 'Recibido')
})

test('textoLugar une dirección, barrio y comuna sin los datos que faltan', () => {
  assert.equal(textoLugar({ direccion: 'Cra 26 #28-40', barrio: 'Salesiano', idComuna: 5 }), 'Cra 26 #28-40 · Salesiano · Comuna 5')
  assert.equal(textoLugar({ barrio: 'Salesiano', idComuna: 5 }), 'Salesiano · Comuna 5')
  assert.equal(textoLugar({ idComuna: 3 }), 'Comuna 3')
  assert.equal(textoLugar({}), '')
})
