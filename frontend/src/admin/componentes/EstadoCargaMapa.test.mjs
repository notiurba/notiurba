import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { createServer } from 'vite'

let servidor
let EstadoCargaMapa
let ErrorApi

before(async () => {
  globalThis.window = { location: { pathname: '/admin/mapa' } }
  servidor = await createServer({ server: { middlewareMode: true, ws: false }, appType: 'custom' })
  ;({ default: EstadoCargaMapa } = await servidor.ssrLoadModule('/src/admin/componentes/EstadoCargaMapa.jsx'))
  ;({ ErrorApi } = await servidor.ssrLoadModule('/src/compartido/api/errorApi.js'))
})

after(async () => { await servidor?.close() })

const pintar = (error) => renderToStaticMarkup(
  React.createElement(EstadoCargaMapa, { error, recargar: () => {} }),
)

test('muestra el error seguro del API y permite reintentar', () => {
  const html = pintar(new ErrorApi({
    estado: 403,
    codigo: 'NO_AUTORIZADO',
    mensaje: 'No tiene permisos para consultar los reportes.',
  }))

  assert.match(html, /No pudimos cargar el mapa de reportes/)
  assert.match(html, /No tiene permisos para consultar los reportes/)
  assert.match(html, />Reintentar</)
  assert.doesNotMatch(html, /demostración|NU-00000[1-9]/i)
})

test('oculta el detalle de un error desconocido', () => {
  const html = pintar(new Error('SQLSTATE 42501: permiso denegado'))

  assert.match(html, /No pudimos consultar los reportes/)
  assert.doesNotMatch(html, /SQLSTATE|42501|permiso denegado/)
})
