import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { createServer } from 'vite'

let servidor
let Configuracion

before(async () => {
  servidor = await createServer({ server: { middlewareMode: true, ws: false }, appType: 'custom' })
  ;({ default: Configuracion } = await servidor.ssrLoadModule('/src/admin/paginas/Configuracion.jsx'))
})

after(async () => { await servidor?.close() })

test('mientras carga la configuración lo dice en palabras', () => {
  const html = renderToStaticMarkup(React.createElement(Configuracion))
  assert.match(html, /Cargando la configuración/)
})
