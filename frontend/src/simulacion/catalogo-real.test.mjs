import assert from 'node:assert/strict'
import { after, before, beforeEach, test } from 'node:test'
import { readFileSync } from 'node:fs'
import { createServer } from 'vite'

let server, peticion, buscarMock, guardarToken
const originalFetch = globalThis.fetch
const originalStorage = globalThis.localStorage
const memoria = new Map()
const evidencia = JSON.parse(readFileSync(new URL('../../../docs/validacion/evidencias-catalogo-barrios-20261107/chrome-resultado.json', import.meta.url), 'utf8'))

before(async () => {
  globalThis.localStorage = { getItem: k => memoria.get(k) ?? null, setItem: (k,v) => memoria.set(k,String(v)), removeItem: k => memoria.delete(k) }
  server = await createServer({ server: { middlewareMode: true, ws: false }, appType: 'custom', define: { 'import.meta.env.VITE_USAR_MOCKS': '"true"' } })
  ;({ peticion } = await server.ssrLoadModule('/src/compartido/api/cliente.js'))
  ;({ buscarMock } = await server.ssrLoadModule('/src/simulacion/index.js'))
  ;({ guardarToken } = await server.ssrLoadModule('/src/compartido/auth/sesion.js'))
})
beforeEach(() => { memoria.clear(); guardarToken('jwt-ficticio-no-enviar') })
after(async () => { globalThis.fetch = originalFetch; if (originalStorage === undefined) delete globalThis.localStorage; else globalThis.localStorage = originalStorage; await server?.close() })

test('catálogo moderno y antiguo usan GET público real sin JWT simulado ni fixture', async () => {
  const llamadas = []
  globalThis.fetch = async (url, options) => {
    llamadas.push(url)
    assert.ok(url.endsWith('/api/catalogos/barrios'))
    assert.equal(options.headers.Authorization, undefined)
    // Respuesta QA guardada: doble HTTP técnico, no catálogo aprobado ni prueba de producción.
    return new Response(JSON.stringify(evidencia.barrios), { status: 200 })
  }
  for (const ruta of ['/api/catalogos/barrios', '/api/reportes/opciones/barrios']) {
    assert.deepEqual(await peticion(ruta), evidencia.barrios)
  }
  assert.equal(llamadas.length, 2)
})
test('con mocks activos un catálogo real vacío permanece vacío', async () => {
  globalThis.fetch = async () => new Response('[]', { status: 200 })
  assert.deepEqual(await peticion('/api/catalogos/barrios'), [])
})
test('errores HTTP y falta de conexión no restauran los barrios de ejemplo', async () => {
  for (const status of [401,403,404,500]) {
    globalThis.fetch = async () => new Response('{}', { status })
    await assert.rejects(() => peticion('/api/catalogos/barrios'), e => e.estado === status)
  }
  globalThis.fetch = async () => { throw new Error('sin red') }
  await assert.rejects(() => peticion('/api/catalogos/barrios'), e => e.codigo === 'SIN_CONEXION')
})
test('el simulador no expone ninguno de sus catálogos de barrios como opción ciudadana', () => {
  assert.equal(buscarMock('GET','/api/catalogos/barrios'), null)
  assert.equal(buscarMock('GET','/api/reportes/opciones/barrios'), null)
})
test('el script de ejemplos marca las nuevas filas internas y mantiene los IDs existentes', () => {
  const sql = readFileSync(new URL('../../../db/datos_ejemplo.sql', import.meta.url), 'utf8')
  assert.match(sql, /INSERT INTO barrio \(id_barrio, id_comuna, nombre, interno\)/)
  assert.match(sql, /SELECT g, g, 'Barrio ejemplo ' \|\| g, TRUE FROM generate_series\(1,10\) g/)
  assert.match(sql, /ON CONFLICT \(id_barrio\) DO NOTHING/)
})
