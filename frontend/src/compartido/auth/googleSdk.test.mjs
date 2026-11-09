import assert from 'node:assert/strict'
import { test } from 'node:test'
import { crearGoogleSdk } from './googleSdk.js'

function entorno(plazo) {
  const ventana = {}
  const scripts = []
  const documento = {
    createElement: () => ({ remove() { this.retirado = true } }),
    head: { appendChild: script => scripts.push(script) },
  }
  return { ventana, scripts, sdk: crearGoogleSdk(ventana, documento, plazo) }
}

test('montajes concurrentes comparten la carga e inicializan GIS una sola vez', async () => {
  const { ventana, scripts, sdk } = entorno()
  const primera = sdk.cargar()
  assert.equal(sdk.cargar(), primera)
  assert.equal(scripts.length, 1)
  let inicializaciones = 0
  let callback
  ventana.google = { accounts: { id: { initialize: opciones => {
    inicializaciones++; callback = opciones.callback
  } } } }
  scripts[0].onload()
  await primera
  let antiguo = 0
  let nuevo = 0
  const salirAntiguo = sdk.conectar('cliente-qa', () => antiguo++)
  const salirNuevo = sdk.conectar('cliente-qa', () => nuevo++)
  salirAntiguo()
  callback({ credential: 'solo-prueba' })
  assert.equal(antiguo, 0)
  assert.equal(nuevo, 1)
  assert.equal(inicializaciones, 1)
  salirNuevo()
  callback({})
  assert.equal(nuevo, 1)
  assert.throws(() => sdk.conectar('otro-cliente', () => {}), /inconsistente/)
})

test('un fallo de red retira el script y permite una carga posterior', async () => {
  const { scripts, sdk } = entorno()
  const carga = sdk.cargar()
  scripts[0].onerror()
  await assert.rejects(carga, /cargar/)
  assert.equal(scripts[0].retirado, true)
  const reintento = sdk.cargar()
  assert.equal(scripts.length, 2)
  scripts[1].onerror()
  await assert.rejects(reintento)
})

test('una carga sin eventos termina con error en vez de dejar el botón esperando', async () => {
  const { scripts, sdk } = entorno(5)
  await assert.rejects(sdk.cargar(), /respondió/)
  assert.equal(scripts[0].retirado, true)
})
