import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import React from 'react'
import TestRenderer, { act } from 'react-test-renderer'
import { createServer } from 'vite'

globalThis.IS_REACT_ACT_ENVIRONMENT = true
let servidor
let SinConexion
const navegadorOriginal = Object.getOwnPropertyDescriptor(globalThis, 'navigator')

before(async () => {
  servidor = await createServer({ server: { middlewareMode: true, ws: false }, appType: 'custom' })
  ;({ default: SinConexion } = await servidor.ssrLoadModule('/src/compartido/componentes/SinConexion.jsx'))
})

after(async () => {
  await servidor?.close()
  if (navegadorOriginal) Object.defineProperty(globalThis, 'navigator', navegadorOriginal)
  else delete globalThis.navigator
  delete globalThis.window
})

async function montar(enLinea) {
  const eventos = new EventTarget()
  const navegador = { onLine: enLinea }
  let recargas = 0
  let desmontajes = 0
  globalThis.window = eventos
  eventos.location = { reload: () => { recargas += 1 } }
  Object.defineProperty(globalThis, 'navigator', { configurable: true, value: navegador })
  const dialogo = {
    open: false,
    showModal() { this.open = true },
    close() { this.open = false },
  }
  function Formulario() {
    const [valor, cambiar] = React.useState('')
    React.useEffect(() => () => { desmontajes += 1 }, [])
    return React.createElement('input', { value: valor, onChange: cambiar })
  }
  let vista
  await act(async () => {
    vista = TestRenderer.create(React.createElement(React.Fragment, null,
      React.createElement(SinConexion), React.createElement(Formulario)), {
      createNodeMock: elemento => elemento.type === 'dialog' ? dialogo : null,
    })
  })
  return { vista, dialogo, navegador, eventos, recargas: () => recargas, desmontajes: () => desmontajes }
}

test('al abrir sin red muestra un diálogo modal identificado y no permite cerrarlo con Escape', async () => {
  const { vista, dialogo } = await montar(false)
  try {
    assert.equal(dialogo.open, true)
    const modal = vista.root.findByType('dialog')
    assert.ok(modal.props['aria-labelledby'])
    assert.ok(modal.props['aria-describedby'])
    let cancelado = false
    modal.props.onCancel({ preventDefault: () => { cancelado = true } })
    assert.equal(cancelado, true)
  } finally { await act(async () => vista.unmount()) }
})

test('desconectar, reintentar y reconectar conserva el formulario sin recargar', async () => {
  const estado = await montar(true)
  const { vista, dialogo, navegador, eventos } = estado
  try {
    assert.equal(dialogo.open, false)
    await act(async () => { vista.root.findByType('input').props.onChange('Texto pendiente') })
    await act(async () => {
      navegador.onLine = false
      eventos.dispatchEvent(new Event('offline'))
    })
    await act(async () => vista.root.findByType('button').props.onClick())
    assert.equal(estado.recargas(), 0, 'reintentar sin red no debe recargar la página')
    assert.equal(dialogo.open, true)
    await act(async () => {
      navegador.onLine = true
      eventos.dispatchEvent(new Event('online'))
    })
    assert.equal(dialogo.open, false)
    assert.equal(vista.root.findByType('input').props.value, 'Texto pendiente')
    assert.equal(estado.desmontajes(), 0)
  } finally { await act(async () => vista.unmount()) }
})

test('reintentar detecta la reconexión aunque todavía no haya llegado el evento online', async () => {
  const { vista, dialogo, navegador } = await montar(false)
  try {
    navegador.onLine = true
    await act(async () => vista.root.findByType('button').props.onClick())
    assert.equal(dialogo.open, false)
  } finally { await act(async () => vista.unmount()) }
})
