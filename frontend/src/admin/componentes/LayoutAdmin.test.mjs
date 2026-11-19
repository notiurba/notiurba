import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import React from 'react'
import TestRenderer, { act } from 'react-test-renderer'
import { MemoryRouter, Route, Routes, useNavigate } from 'react-router-dom'
import { createServer } from 'vite'

globalThis.IS_REACT_ACT_ENVIRONMENT = true

let servidor
let LayoutAdmin

before(async () => {
  const almacen = new Map()
  globalThis.localStorage = {
    getItem: (clave) => almacen.get(clave) ?? null,
    setItem: (clave, valor) => almacen.set(clave, String(valor)),
    removeItem: (clave) => almacen.delete(clave),
  }
  globalThis.window = { location: { pathname: '/admin', assign: () => {} }, dispatchEvent: () => true }
  servidor = await createServer({ server: { middlewareMode: true, ws: false }, appType: 'custom' })
  ;({ default: LayoutAdmin } = await servidor.ssrLoadModule('/src/admin/componentes/LayoutAdmin.jsx'))
})

after(async () => { await servidor?.close() })

const texto = (nodo) => typeof nodo === 'string'
  ? nodo
  : Array.isArray(nodo)
    ? nodo.map(texto).join(' ')
    : texto(nodo?.children ?? [])

const Sano = () => React.createElement('output', null, 'reportes sanos')
const Roto = () => { throw new Error('mapa roto') }

let navegar
function CapturaNavegacion() {
  navegar = useNavigate()
  return null
}

async function montar(rutaInicial) {
  let vista
  const original = console.error
  console.error = () => {}
  try {
    await act(async () => {
      vista = TestRenderer.create(
        React.createElement(MemoryRouter, { initialEntries: [rutaInicial] },
          React.createElement(CapturaNavegacion),
          React.createElement(Routes, null,
            React.createElement(Route, { element: React.createElement(LayoutAdmin) },
              React.createElement(Route, { path: '/admin', element: React.createElement(Sano) }),
              React.createElement(Route, { path: '/admin/mapa', element: React.createElement(Roto) })))),
      )
    })
  } finally { console.error = original }
  return vista
}

test('si una pantalla del panel falla, la cabecera y las pestañas siguen visibles y se ve el aviso', async () => {
  const vista = await montar('/admin/mapa')

  const contenido = texto(vista.toJSON())
  assert.match(contenido, /Panel administrativo/)
  assert.match(contenido, /Mapa de calor/)
  assert.match(contenido, /No pudimos mostrar esta pantalla/)
})

test('al cambiar de pestaña el aviso no se queda pegado y se ve la pantalla nueva', async () => {
  const vista = await montar('/admin/mapa')

  await act(async () => { navegar('/admin') })

  const contenido = texto(vista.toJSON())
  assert.match(contenido, /reportes sanos/)
  assert.doesNotMatch(contenido, /No pudimos mostrar esta pantalla/)
})
