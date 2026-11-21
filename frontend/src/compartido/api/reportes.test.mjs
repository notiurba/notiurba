import assert from 'node:assert/strict'
import { after, afterEach, beforeEach, test } from 'node:test'
import { createServer } from 'vite'

let servidor
let listarTodosLosReportes
let ultimaRuta
let descriptorSize

beforeEach(async () => {
  globalThis.localStorage = { getItem: () => null, setItem() {}, removeItem() {} }
  globalThis.window = { location: { pathname: '/admin', assign() {} }, dispatchEvent: () => true }
  ultimaRuta = ''
  globalThis.fetch = async (ruta) => {
    ultimaRuta = String(ruta)
    return new Response(JSON.stringify([]), { status: 200 })
  }
  if (!servidor) {
    servidor = await createServer({ server: { middlewareMode: true, ws: false }, appType: 'custom', define: { 'import.meta.env.VITE_USAR_MOCKS': '"false"' } })
    ;({ listarTodosLosReportes } = await servidor.ssrLoadModule('/src/compartido/api/reportes.js'))
  }
})

test('construye la URL exacta con ambos límites de fecha', async () => {
  descriptorSize = Object.getOwnPropertyDescriptor(URLSearchParams.prototype, 'size')
  Object.defineProperty(URLSearchParams.prototype, 'size', { get: () => undefined, configurable: true })
  await listarTodosLosReportes({ desde: '2026-11-01' })
  assert.match(ultimaRuta, /desde=2026-11-01/)
  await listarTodosLosReportes({ hasta: '2026-11-19' })
  assert.match(ultimaRuta, /hasta=2026-11-19/)
  await listarTodosLosReportes({ desde: '2026-11-01', hasta: '2026-11-19' })
  assert.ok(ultimaRuta.endsWith('/api/admin/reportes?desde=2026-11-01&hasta=2026-11-19'))
})

test('combina fechas con comuna, categoría y estado sin descartar filtros', async () => {
  await listarTodosLosReportes({ desde: '2026-11-01', hasta: '2026-11-19', comuna: 4, idCategoria: 2, estado: 'EN_TRAMITE' })
  assert.match(ultimaRuta, /desde=2026-11-01/)
  assert.match(ultimaRuta, /hasta=2026-11-19/)
  assert.match(ultimaRuta, /comuna=4/)
  assert.match(ultimaRuta, /idCategoria=2/)
  assert.match(ultimaRuta, /estado=EN_TRAMITE/)
})

test('omite filtros vacíos al limpiar las fechas', async () => {
  await listarTodosLosReportes({ desde: '', hasta: '' })
  assert.ok(ultimaRuta.endsWith('/api/admin/reportes'))
  assert.ok(!ultimaRuta.endsWith('?'))
})

afterEach(() => {
  if (descriptorSize) Object.defineProperty(URLSearchParams.prototype, 'size', descriptorSize)
  else delete URLSearchParams.prototype.size
  descriptorSize = undefined
})

after(async () => { await servidor?.close() })
