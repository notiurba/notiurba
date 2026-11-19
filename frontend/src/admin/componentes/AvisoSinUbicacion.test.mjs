import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import React from 'react'
import TestRenderer, { act } from 'react-test-renderer'
import { createServer } from 'vite'

globalThis.IS_REACT_ACT_ENVIRONMENT = true

let servidor
let AvisoSinUbicacion

before(async () => {
  servidor = await createServer({ server: { middlewareMode: true, ws: false }, appType: 'custom' })
  ;({ default: AvisoSinUbicacion } = await servidor.ssrLoadModule('/src/admin/componentes/AvisoSinUbicacion.jsx'))
})

after(async () => { await servidor?.close() })

const texto = (nodo) => typeof nodo === 'string'
  ? nodo
  : Array.isArray(nodo)
    ? nodo.map(texto).join(' ')
    : texto(nodo?.children ?? [])

async function dibujar(cantidad) {
  let vista
  await act(async () => {
    vista = TestRenderer.create(React.createElement(AvisoSinUbicacion, { cantidad }))
  })
  return vista.toJSON()
}

test('sin reportes fuera del mapa no muestra ningún aviso', async () => {
  assert.equal(await dibujar(0), null)
})

test('con un reporte usa el singular', async () => {
  assert.match(texto(await dibujar(1)), /1 reporte no tiene ubicación y no aparece en el mapa/)
})

test('con varios reportes usa el plural', async () => {
  assert.match(texto(await dibujar(4)), /4 reportes no tienen ubicación y no aparecen en el mapa/)
})
