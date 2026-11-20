import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import React from 'react'
import TestRenderer, { act } from 'react-test-renderer'
import { createServer } from 'vite'

globalThis.IS_REACT_ACT_ENVIRONMENT = true

let servidor
let Estadisticas

before(async () => {
  globalThis.localStorage = { getItem: () => null, setItem() {}, removeItem() {} }
  globalThis.window = { location: { pathname: '/admin/estadisticas', assign() {} }, dispatchEvent: () => true }
  servidor = await createServer({ server: { middlewareMode: true, ws: false }, appType: 'custom', define: { 'import.meta.env.VITE_USAR_MOCKS': '"false"' } })
  ;({ default: Estadisticas } = await servidor.ssrLoadModule('/src/admin/paginas/Estadisticas.jsx'))
})

after(async () => { await servidor?.close() })

const texto = (nodo) => typeof nodo === 'string' ? nodo : Array.isArray(nodo) ? nodo.map(texto).join(' ') : texto(nodo?.children ?? [])

test('presenta totales, porcentaje y desgloses entregados por estadísticas', async () => {
  globalThis.fetch = async (url) => new Response(JSON.stringify(String(url).includes('/api/categorias')
    ? [{ idCategoria: 1, nombre: 'Vías' }]
    : { vigencia: 'todos', total: 4, atendidos: 1, porcentajeAtendidos: 25.0, sinComuna: 1, porEstado: [{ estado: 'Recibido', total: 3 }], porCategoria: [{ idCategoria: 1, categoria: 'Vías', total: 4 }], porComuna: [{ comuna: 1, total: 4 }] }), { status: 200 })
  let vista
  await act(async () => { vista = TestRenderer.create(React.createElement(Estadisticas)) })
  await act(async () => { await new Promise((resolver) => setTimeout(resolver, 30)) })
  const salida = texto(vista.toJSON())
  assert.match(salida, /Total de reportes 4/)
  assert.match(salida, /Reportes atendidos 1/)
  assert.match(salida, /Porcentaje de atención 25.0\s+%/)
  assert.match(salida, /Vías 4/)
})
