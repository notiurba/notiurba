import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import React from 'react'
import TestRenderer, { act } from 'react-test-renderer'
import { createServer } from 'vite'

globalThis.IS_REACT_ACT_ENVIRONMENT = true

let servidor
let CargaReportesMapa
let ErrorApi

before(async () => {
  globalThis.window = { location: { pathname: '/admin/mapa' } }
  servidor = await createServer({ server: { middlewareMode: true, ws: false }, appType: 'custom' })
  ;({ default: CargaReportesMapa } = await servidor.ssrLoadModule('/src/admin/componentes/CargaReportesMapa.jsx'))
  ;({ ErrorApi } = await servidor.ssrLoadModule('/src/compartido/api/errorApi.js'))
})

after(async () => { await servidor?.close() })

const vaciarMicrotareas = () => new Promise((resolve) => setTimeout(resolve, 0))
const texto = (nodo) => typeof nodo === 'string'
  ? nodo
  : Array.isArray(nodo)
    ? nodo.map(texto).join(' ')
    : texto(nodo?.children ?? [])

async function montar(cargador, mapasDibujados) {
  let vista
  await act(async () => {
    vista = TestRenderer.create(React.createElement(CargaReportesMapa, { cargador }, (reportes) => {
      mapasDibujados.push(reportes)
      return React.createElement('output', null, reportes.map((reporte) => reporte.id).join(','))
    }))
    await vaciarMicrotareas()
  })
  return vista
}

test('mientras carga anuncia el estado sin un segundo <main> dentro del panel', async () => {
  const mapas = []
  const vista = await montar(() => new Promise(() => {}), mapas)

  const estado = vista.root.findByProps({ role: 'status' })
  assert.notEqual(estado.type, 'main')
  assert.match(texto(vista.toJSON()), /Cargando mapa de comunas/)
  assert.equal(mapas.length, 0)
})

test('ante un error no dibuja el mapa y «Reintentar» muestra solo la respuesta nueva del API', async () => {
  let llamadas = 0
  let resolverSegundoIntento
  const cargador = () => {
    llamadas += 1
    if (llamadas === 1) {
      return Promise.reject(new ErrorApi({
        estado: 0,
        codigo: 'SIN_CONEXION',
        mensaje: 'No hay conexión a internet.',
      }))
    }
    return new Promise((resolve) => { resolverSegundoIntento = resolve })
  }
  const mapas = []
  const vista = await montar(cargador, mapas)

  assert.match(texto(vista.toJSON()), /No pudimos cargar el mapa de reportes/)
  assert.match(texto(vista.toJSON()), /No hay conexión a internet/)
  assert.notEqual(vista.root.findByProps({ role: 'alert' }).type, 'main')
  assert.equal(mapas.length, 0, 'con error no se dibuja ningún mapa ni dato')

  await act(async () => {
    vista.root.findByType('button').props.onClick()
    await vaciarMicrotareas()
  })
  assert.equal(llamadas, 2)
  assert.match(texto(vista.toJSON()), /Cargando mapa de comunas/)
  assert.equal(mapas.length, 0)

  await act(async () => {
    resolverSegundoIntento([{ id: 'NU-API-NUEVA' }])
    await vaciarMicrotareas()
  })

  assert.deepEqual(mapas.at(-1), [{ id: 'NU-API-NUEVA' }])
  assert.equal(vista.root.findByType('output').children.join(''), 'NU-API-NUEVA')
  assert.doesNotMatch(texto(vista.toJSON()), /NU-00000[1-9]|demostración/i)
})
