import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import React from 'react'
import TestRenderer, { act } from 'react-test-renderer'
import { createServer } from 'vite'

globalThis.IS_REACT_ACT_ENVIRONMENT = true

let servidor
let LimiteErrores

before(async () => {
  servidor = await createServer({ server: { middlewareMode: true, ws: false }, appType: 'custom' })
  ;({ default: LimiteErrores } = await servidor.ssrLoadModule('/src/compartido/componentes/LimiteErrores.jsx'))
})

after(async () => { await servidor?.close() })

const texto = (nodo) => typeof nodo === 'string'
  ? nodo
  : Array.isArray(nodo)
    ? nodo.map(texto).join(' ')
    : texto(nodo?.children ?? [])

const estado = { falla: false }
function Hijo() {
  if (estado.falla) throw new Error('fallo al dibujar')
  return React.createElement('output', null, 'contenido sano')
}

async function sinRuidoDeReact(accion) {
  const original = console.error
  console.error = () => {}
  try { return await accion() } finally { console.error = original }
}

async function montar(clave = 'a') {
  let vista
  await act(async () => {
    vista = TestRenderer.create(React.createElement(LimiteErrores, { key: clave }, React.createElement(Hijo)))
  })
  return vista
}

test('con un hijo sano muestra el contenido tal cual', async () => {
  estado.falla = false
  const vista = await montar()

  assert.match(texto(vista.toJSON()), /contenido sano/)
  assert.equal(vista.root.findAllByProps({ role: 'alert' }).length, 0)
})

test('si el hijo falla al dibujarse muestra un aviso con «Reintentar» en vez de dejar la pantalla en blanco', async () => {
  estado.falla = true
  const vista = await sinRuidoDeReact(() => montar())

  const contenido = texto(vista.toJSON())
  assert.match(contenido, /No pudimos mostrar esta pantalla/)
  assert.match(contenido, /Reintentar/)
  assert.doesNotMatch(contenido, /contenido sano/)
  assert.equal(vista.root.findAllByProps({ role: 'alert' }).length > 0, true)
})

test('«Reintentar» vuelve a dibujar el contenido cuando el problema ya pasó', async () => {
  estado.falla = true
  const vista = await sinRuidoDeReact(() => montar())

  estado.falla = false
  await act(async () => { vista.root.findByType('button').props.onClick() })

  assert.match(texto(vista.toJSON()), /contenido sano/)
})

test('«Reintentar» con el problema vigente vuelve a mostrar el aviso sin romper nada', async () => {
  estado.falla = true
  const vista = await sinRuidoDeReact(() => montar())

  await sinRuidoDeReact(() => act(async () => { vista.root.findByType('button').props.onClick() }))

  assert.match(texto(vista.toJSON()), /No pudimos mostrar esta pantalla/)
})

test('con otra clave (otra pestaña) se reinicia y muestra el contenido nuevo', async () => {
  estado.falla = true
  const vista = await sinRuidoDeReact(() => montar('mapa'))
  assert.match(texto(vista.toJSON()), /No pudimos mostrar esta pantalla/)

  estado.falla = false
  await act(async () => {
    vista.update(React.createElement(LimiteErrores, { key: 'reportes' }, React.createElement(Hijo)))
  })

  assert.match(texto(vista.toJSON()), /contenido sano/)
})
