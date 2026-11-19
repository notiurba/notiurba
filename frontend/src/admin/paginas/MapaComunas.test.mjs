import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import React from 'react'
import TestRenderer, { act } from 'react-test-renderer'
import { createServer } from 'vite'

globalThis.IS_REACT_ACT_ENVIRONMENT = true

const soporte = (archivo) => new URL(`./soporte-pruebas/${archivo}`, import.meta.url).pathname

let servidor
let MapaComunas

before(async () => {
  const almacen = new Map()
  globalThis.localStorage = {
    getItem: (clave) => almacen.get(clave) ?? null,
    setItem: (clave, valor) => almacen.set(clave, String(valor)),
    removeItem: (clave) => almacen.delete(clave),
  }
  globalThis.window = { location: { pathname: '/admin/mapa', assign: () => {} }, dispatchEvent: () => true }
  servidor = await createServer({
    server: { middlewareMode: true, ws: false },
    appType: 'custom',
    define: { 'import.meta.env.VITE_USAR_MOCKS': '"false"' },
    resolve: {
      alias: [
        { find: /^leaflet$/, replacement: soporte('leaflet.mjs') },
        { find: /^leaflet\.heat$/, replacement: soporte('leaflet-heat.mjs') },
        { find: /^react-leaflet$/, replacement: soporte('react-leaflet.mjs') },
      ],
    },
  })
  ;({ default: MapaComunas } = await servidor.ssrLoadModule('/src/admin/paginas/MapaComunas.jsx'))
})

after(async () => { await servidor?.close() })

const texto = (nodo) => typeof nodo === 'string'
  ? nodo
  : Array.isArray(nodo)
    ? nodo.map(texto).join(' ')
    : texto(nodo?.children ?? [])

const reporte = (codigo, latitud, longitud) => ({
  codigoSeguimiento: codigo,
  categoria: 'Vías',
  descripcion: 'Hueco',
  latitud,
  longitud,
  creadoEn: '2026-11-16T19:20:00Z',
  estado: 'Enviado',
})

async function abrirMapa(reportes) {
  globalThis.fetch = async () => new Response(JSON.stringify(reportes), { status: 200 })
  let vista
  await act(async () => { vista = TestRenderer.create(React.createElement(MapaComunas)) })
  await act(async () => { await new Promise((resolver) => setTimeout(resolver, 50)) })
  return vista
}

test('con reportes con y sin ubicación dibuja solo los que la tienen y avisa cuántos faltan', async () => {
  const vista = await abrirMapa([reporte('A', 4.08, -76.19), reporte('B', null, null), reporte('C', null, null)])

  assert.equal(vista.root.findAllByType('marker').length, 1)
  assert.match(texto(vista.toJSON()), /2 reportes no tienen ubicación y no aparecen en el mapa/)
})

test('si ningún reporte tiene ubicación el mapa se muestra sin marcadores y con el aviso', async () => {
  const vista = await abrirMapa([reporte('A', null, null), reporte('B', null, null), reporte('C', null, null)])

  assert.equal(vista.root.findAllByType('marker').length, 0)
  assert.match(texto(vista.toJSON()), /3 reportes no tienen ubicación/)
})

test('si todos tienen ubicación dibuja todos y no muestra el aviso', async () => {
  const vista = await abrirMapa([reporte('A', 4.08, -76.19), reporte('B', 4.09, -76.2)])

  assert.equal(vista.root.findAllByType('marker').length, 2)
  assert.doesNotMatch(texto(vista.toJSON()), /no tienen ubicación|no tiene ubicación/)
})
