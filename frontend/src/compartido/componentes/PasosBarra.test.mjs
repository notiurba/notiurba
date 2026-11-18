import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { createServer } from 'vite'

let servidor
let PasosBarra

before(async () => {
  servidor = await createServer({ server: { middlewareMode: true, ws: false }, appType: 'custom' })
  ;({ default: PasosBarra } = await servidor.ssrLoadModule('/src/compartido/componentes/PasosBarra.jsx'))
})
after(async () => { await servidor?.close() })

test('el paso se anuncia en un encabezado al que se le puede llevar el foco', () => {
  const salida = renderToStaticMarkup(
    React.createElement(PasosBarra, { paso: 2, total: 2, etiqueta: 'Ubicación y acceso' }),
  )
  assert.match(salida, /<h2[^>]*tabindex="-1"[^>]*>.*Paso 2 de 2.*Ubicación y acceso.*<\/h2>/)
})
