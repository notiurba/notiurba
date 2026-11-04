import assert from 'node:assert/strict'
import { after, before, beforeEach, test } from 'node:test'
import { createServer } from 'vite'

let server
let peticion
let ErrorApi
let guardarToken
let obtenerToken
let llamadas

const respuestaJson = (estado, cuerpo) =>
  new Response(cuerpo === undefined ? null : JSON.stringify(cuerpo), { status: estado })

before(async () => {
  const almacen = new Map()
  globalThis.localStorage = {
    getItem: (k) => almacen.get(k) ?? null,
    setItem: (k, v) => almacen.set(k, String(v)),
    removeItem: (k) => almacen.delete(k),
  }
  globalThis.window = { location: { pathname: '/app', assign: (ruta) => { llamadas.redireccion = ruta } } }
  server = await createServer({ server: { middlewareMode: true }, appType: 'custom' })
  const cliente = await server.ssrLoadModule('/src/compartido/api/cliente.js')
  peticion = cliente.peticion
  ErrorApi = cliente.ErrorApi
  const sesion = await server.ssrLoadModule('/src/compartido/auth/sesion.js')
  guardarToken = sesion.guardarToken
  obtenerToken = sesion.obtenerToken
})

after(async () => { await server?.close() })

beforeEach(() => {
  llamadas = { redireccion: null, opciones: null }
  localStorage.removeItem('notiurba.token')
})

test('un error del contrato llega como ErrorApi con codigo, mensaje y campo', async () => {
  globalThis.fetch = async () => respuestaJson(409, { codigo: 'CORREO_YA_REGISTRADO', mensaje: 'Ya existe una cuenta con este correo', campo: 'correo' })
  await assert.rejects(() => peticion('/api/auth/google/completar', { method: 'POST', body: '{}' }), (e) => {
    assert.ok(e instanceof ErrorApi)
    assert.equal(e.estado, 409)
    assert.equal(e.codigo, 'CORREO_YA_REGISTRADO')
    assert.equal(e.campo, 'correo')
    assert.equal(e.message, 'Ya existe una cuenta con este correo')
    return true
  })
})

test('una respuesta sin el formato del contrato da un mensaje genérico en español', async () => {
  globalThis.fetch = async () => new Response('<html>Bad gateway</html>', { status: 502 })
  await assert.rejects(() => peticion('/api/health'), (e) => {
    assert.equal(e.codigo, 'RESPUESTA_INESPERADA')
    assert.match(e.message, /inesperado/)
    return true
  })
})

test('sin red se lanza SIN_CONEXION', async () => {
  globalThis.fetch = async () => { throw new TypeError('Failed to fetch') }
  await assert.rejects(() => peticion('/api/health'), (e) => e.codigo === 'SIN_CONEXION' && e.estado === 0)
})

test('con FormData no se fija Content-Type; con JSON sí', async () => {
  globalThis.fetch = async (_url, opciones) => { llamadas.opciones = opciones; return respuestaJson(200, {}) }
  await peticion('/api/reportes/NU-000001/fotos', { method: 'POST', body: new FormData() })
  assert.equal(llamadas.opciones.headers['Content-Type'], undefined)
  await peticion('/api/auth/login', { method: 'POST', body: '{}' })
  assert.equal(llamadas.opciones.headers['Content-Type'], 'application/json')
})

test('NO_AUTENTICADO con sesión cierra la sesión y lleva al ingreso', async () => {
  guardarToken('token-viejo')
  globalThis.fetch = async () => respuestaJson(401, { codigo: 'NO_AUTENTICADO', mensaje: 'Tu sesión no es válida o venció. Inicia sesión de nuevo.', campo: null })
  await assert.rejects(() => peticion('/api/categorias'), (e) => e.codigo === 'NO_AUTENTICADO')
  assert.equal(obtenerToken(), null)
  assert.equal(llamadas.redireccion, '/app/ingresar')
})

test('credenciales inválidas en el login no cierran sesión ni redirigen', async () => {
  globalThis.fetch = async () => respuestaJson(401, { codigo: 'CREDENCIALES_INVALIDAS', mensaje: 'Correo o contraseña incorrectos.', campo: null })
  await assert.rejects(() => peticion('/api/auth/login', { method: 'POST', body: '{}' }), (e) => e.codigo === 'CREDENCIALES_INVALIDAS')
  assert.equal(llamadas.redireccion, null)
})

test('204 devuelve null', async () => {
  globalThis.fetch = async () => new Response(null, { status: 204 })
  assert.equal(await peticion('/api/auth/restablecer', { method: 'POST', body: '{}' }), null)
})
