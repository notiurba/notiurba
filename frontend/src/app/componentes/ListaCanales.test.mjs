import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { createServer } from 'vite'

let servidor
let ListaCanales

before(async () => {
  servidor = await createServer({ server: { middlewareMode: true, ws: false }, appType: 'custom' })
  ;({ default: ListaCanales } = await servidor.ssrLoadModule('/src/app/componentes/ListaCanales.jsx'))
})
after(async () => { await servidor?.close() })

const html = (canales) => renderToStaticMarkup(React.createElement(ListaCanales, { canales }))

test('cada canal tiene su enlace seguro y el primero se marca como recomendado', () => {
  const salida = html([
    { tipo: 'WhatsApp', acceso: '317 372 5078', finalidad: 'Radicar PQR', detalle: 'Elija «Residuos»' },
    { tipo: 'Web', acceso: 'https://www.gdo.com.co/Paginas/Registro-de-pqrs.aspx', finalidad: 'Radicar PQR' },
    { tipo: 'Presencial', acceso: 'Calle 25 #32A-31, Tuluá', finalidad: 'Radicar PQR', horario: 'Lunes a viernes' },
  ])
  assert.match(salida, /href="https:\/\/wa\.me\/573173725078" target="_blank" rel="noopener noreferrer"/)
  assert.match(salida, /href="https:\/\/www\.gdo\.com\.co\/Paginas\/Registro-de-pqrs\.aspx" target="_blank" rel="noopener noreferrer"/)
  assert.match(salida, /\(se abre en otra pestaña\)/)
  assert.match(salida, />gdo\.com\.co</)
  assert.equal(salida.match(/Recomendado/g).length, 1)
  assert.match(salida, /Calle 25 #32A-31, Tuluá/)
  assert.match(salida, /Lunes a viernes/)
})

test('un acceso peligroso se muestra como texto, nunca como enlace', () => {
  const salida = html([{ tipo: 'Web', acceso: 'javascript:alert(1)', finalidad: 'Radicar PQR' },
    { tipo: 'Teléfono', acceso: '(602) 233 9300', finalidad: 'Información' }])
  assert.doesNotMatch(salida, /href="javascript/)
  assert.match(salida, /javascript:alert\(1\)/)
  assert.match(salida, /href="tel:6022339300"(?! target)/)
})

test('sin canales no se dibuja nada', () => {
  assert.equal(html([]), '')
  assert.equal(html(undefined), '')
})
