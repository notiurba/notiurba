import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import React from 'react'
import TestRenderer, { act } from 'react-test-renderer'
import { createServer } from 'vite'

let servidor
let opcionesConSeleccion
let Reportes

before(async () => {
  servidor = await createServer({ server: { middlewareMode: true, ws: false }, appType: 'custom' })
  ;({ opcionesConSeleccion } = await servidor.ssrLoadModule('/src/admin/paginas/opcionesFiltros.js'))
  ;({ default: Reportes } = await servidor.ssrLoadModule('/src/admin/paginas/Reportes.jsx'))
})

after(async () => { await servidor?.close() })

test('conserva Comuna 3 seleccionada cuando las fechas ya no devuelven reportes', () => {
  assert.deepEqual(opcionesConSeleccion([], '3', 'Todas'), ['3'])
  assert.deepEqual(opcionesConSeleccion(['Vías'], 'Aseo', 'Todos'), ['Vías', 'Aseo'])
})

test('la pantalla conserva visualmente Comuna 3 tras filtrar una fecha vacía', async () => {
  globalThis.localStorage = { getItem: () => null, setItem() {}, removeItem() {} }
  globalThis.window = { location: { pathname: '/admin', assign() {} }, dispatchEvent: () => true }
  globalThis.fetch = async (url) => new Response(JSON.stringify(String(url).includes('desde=') ? [] : [{ codigoSeguimiento: 'NU-3', categoria: 'Vías', idComuna: 3, estado: 'Recibido', creadoEn: '2026-11-01T00:00:00Z', historial: [] }]), { status: 200 })
  let vista
  await act(async () => { vista = TestRenderer.create(React.createElement(Reportes)) })
  await act(async () => { await new Promise((r) => setTimeout(r, 30)) })
  const selects = vista.root.findAllByType('select')
  await act(async () => { selects[0].props.onChange({ target: { value: '3' } }) })
  const fecha = vista.root.findAllByType('input').find((n) => n.props.type === 'date')
  await act(async () => { fecha.props.onChange({ target: { value: '2026-11-19' } }); await new Promise((r) => setTimeout(r, 30)) })
  assert.equal(vista.root.findAllByType('select')[0].props.value, '3')
  assert.ok(vista.root.findAllByType('select')[0].findAllByType('option').some((n) => n.props.value === '3'))
  await act(async () => { vista.root.findAllByType('select')[0].props.onChange({ target: { value: 'Todas' } }) })
  assert.ok(!vista.root.findAllByType('select')[0].findAllByType('option').some((n) => n.props.value === '3'))
})
