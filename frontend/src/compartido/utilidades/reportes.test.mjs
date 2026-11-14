import assert from 'node:assert/strict'
import test from 'node:test'
import { ESTADOS_REPORTE, avisoDelCiudadano, comunasConReportes, estadoElegido, estadosConReportes, etiquetaEstado, estadosSiguientes, identificadorEstado, lugarDelReporte, puedeActualizar, textoLugar } from './reportes.js'

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

test('los filtros del panel solo ofrecen lo que tiene reportes', () => {
  const reportes = [{ idComuna: 7 }, { idComuna: 4 }, { idComuna: 7 }, { idComuna: null }]
  assert.deepEqual(comunasConReportes(reportes), [4, 7])
  const cuentas = { Todos: 6, Recibido: 3, Enviado: 3, 'En trámite': 0, Resuelto: 0 }
  const cuenta = (e) => cuentas[e]
  assert.deepEqual(estadosConReportes(['Todos', ...ESTADOS_REPORTE], cuenta, 'Todos'), ['Todos', 'Recibido', 'Enviado'])
  assert.deepEqual(estadosConReportes(['Todos', ...ESTADOS_REPORTE], cuenta, 'Resuelto'), ['Todos', 'Recibido', 'Enviado', 'Resuelto'])
})

test('de identificador a etiqueta y de vuelta, para los cuatro estados', () => {
  for (const estado of ESTADOS_REPORTE) assert.equal(etiquetaEstado(identificadorEstado(estado)), estado)
  assert.equal(etiquetaEstado('EN_TRAMITE'), 'En trámite')
  assert.equal(etiquetaEstado('OTRO'), 'OTRO')
})

test('el selector del panel no se queda en un estado que ya no se puede elegir', () => {
  assert.equal(estadoElegido(['En trámite', 'Resuelto'], ''), 'En trámite')
  assert.equal(estadoElegido(['En trámite', 'Resuelto'], 'Resuelto'), 'Resuelto')
  // Tras pasar a «En trámite» solo queda «Resuelto»: el valor viejo no se reenvía.
  assert.equal(estadoElegido(['Resuelto'], 'En trámite'), 'Resuelto')
  assert.equal(estadoElegido([], 'En trámite'), '')
})

test('el aviso del ciudadano se ve hasta que el administrador responde o el reporte se resuelve', () => {
  const del = (delCiudadano, texto = 'nota') => ({ estado: 'Enviado', observacion: texto, delCiudadano })
  const conAviso = { estado: 'Enviado', historial: [del(false), del(true, 'El ciudadano avisó que su reporte ya se resolvió.')] }
  assert.equal(avisoDelCiudadano(conAviso).observacion, 'El ciudadano avisó que su reporte ya se resolvió.')
  assert.equal(avisoDelCiudadano({ ...conAviso, historial: [...conAviso.historial, del(false)] }), null)
  assert.equal(avisoDelCiudadano({ ...conAviso, estado: 'Resuelto' }), null)
  assert.equal(avisoDelCiudadano({ estado: 'Recibido', historial: [] }), null)
  assert.equal(avisoDelCiudadano({ estado: 'Recibido' }), null)
})

