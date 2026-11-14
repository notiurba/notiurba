import assert from 'node:assert/strict'
import test from 'node:test'
import { ESTADOS_REPORTE, estadosSiguientes, identificadorEstado, lugarDelReporte, puedeActualizar, textoLugar } from './reportes.js'

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

test('la API recibe el identificador del estado, no la etiqueta de la pantalla', () => {
  assert.equal(identificadorEstado('En trámite'), 'EN_TRAMITE')
  assert.equal(identificadorEstado('Resuelto'), 'RESUELTO')
  assert.equal(identificadorEstado('Recibido'), 'RECIBIDO')
  assert.equal(identificadorEstado('Enviado'), 'ENVIADO')
})

test('el administrador solo puede avanzar y nunca marcar «Enviado» a mano', () => {
  assert.deepEqual(estadosSiguientes('Recibido'), ['En trámite', 'Resuelto'])
  assert.deepEqual(estadosSiguientes('Enviado'), ['En trámite', 'Resuelto'])
  assert.deepEqual(estadosSiguientes('En trámite'), ['Resuelto'])
  assert.deepEqual(estadosSiguientes('Resuelto'), [])
})

test('un reporte sin lugar escrito dice si hay punto en el mapa o que no tiene ubicación', () => {
  assert.equal(lugarDelReporte({ barrio: 'Salesiano', idComuna: 5 }), 'Salesiano · Comuna 5')
  assert.equal(lugarDelReporte({ latitud: 4.08, longitud: -76.19 }), 'Punto marcado en el mapa')
  assert.equal(lugarDelReporte({ latitud: null, longitud: null }), 'Sin ubicación')
  assert.equal(lugarDelReporte({}), 'Sin ubicación')
})

