import assert from 'node:assert/strict'
import { after, before, beforeEach, test } from 'node:test'
import { createServer } from 'vite'

let server
let peticion
let ErrorApi
let guardarToken
let obtenerToken
let iniciarSesion
let iniciarSesionConGoogle
let completarPerfilGoogle
let obtenerBarrios
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
  // Estas pruebas ejercitan HTTP, incluso si Amelia activa el simulador en .env.local.
  server = await createServer({ server: { middlewareMode: true, ws: false }, appType: 'custom',
    define: { 'import.meta.env.VITE_USAR_MOCKS': '"false"' } })
  const cliente = await server.ssrLoadModule('/src/compartido/api/cliente.js')
  peticion = cliente.peticion
  ErrorApi = cliente.ErrorApi
  const sesion = await server.ssrLoadModule('/src/compartido/auth/sesion.js')
  guardarToken = sesion.guardarToken
  obtenerToken = sesion.obtenerToken
  ;({ iniciarSesion, iniciarSesionConGoogle, completarPerfilGoogle } = await server.ssrLoadModule('/src/compartido/api/auth.js'))
  ;({ obtenerBarrios } = await server.ssrLoadModule('/src/compartido/api/catalogos.js'))
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

for (const estado of [400, 401, 403, 429, 500]) {
  test(`HTTP ${estado} sin cuerpo del contrato conserva el estado y oculta detalles`, async () => {
    globalThis.fetch = async () => new Response('detalle técnico privado', { status: estado })
    await assert.rejects(() => iniciarSesion('qa@example.invalid', 'solo-prueba-local'), (e) => {
      assert.equal(e.estado, estado)
      assert.doesNotMatch(e.mensaje, /privado|técnico/)
      return true
    })
  })
}

test('un 500 con mensaje técnico JSON tampoco expone su contenido', async () => {
  globalThis.fetch = async () => respuestaJson(500, { codigo: 'SQL', mensaje: 'detalle privado' })
  await assert.rejects(() => iniciarSesion('qa@example.invalid', 'solo-prueba-local'), (e) => e.estado === 500 && !e.mensaje.includes('privado'))
})

test('el estado HTTP no puede ser reemplazado por el cuerpo', async () => {
  globalThis.fetch = async () => respuestaJson(403, { estado: 401, codigo: 'OTRO', mensaje: 'No permitido' })
  await assert.rejects(() => peticion('/api/auth/login', { publica: true }), (e) => e.estado === 403)
})

test('login rechaza JSON inválido y éxitos sin sesión antes de guardar un token', async () => {
  for (const cuerpo of [null, {}, { token: '', rol: 'CIUDADANO' }]) {
    globalThis.fetch = async () => respuestaJson(200, cuerpo)
    await assert.rejects(() => iniciarSesion('qa@example.invalid', 'solo-prueba-local'), (e) => e.codigo === 'RESPUESTA_INESPERADA')
  }
  globalThis.fetch = async () => new Response('<html>proxy</html>', { status: 200 })
  await assert.rejects(() => iniciarSesion('qa@example.invalid', 'solo-prueba-local'), (e) => e.codigo === 'RESPUESTA_INESPERADA')
  assert.equal(obtenerToken(), null)
})

test('barrios vacíos siguen vacíos y errores 401/403/404 no usan el barrio interno', async () => {
  globalThis.fetch = async () => respuestaJson(200, [])
  assert.deepEqual(await obtenerBarrios(), [])
  for (const estado of [401, 403, 404]) {
    globalThis.fetch = async () => respuestaJson(estado, {})
    await assert.rejects(obtenerBarrios, (e) => e.estado === estado)
  }
})

test('login válido es público y mantiene correo/password del contrato', async () => {
  guardarToken('sesion-local-sintetica')
  globalThis.fetch = async (_url, opciones) => {
    assert.equal(opciones.headers.Authorization, undefined)
    assert.deepEqual(JSON.parse(opciones.body), { correo: 'qa@example.invalid', password: 'solo-prueba-local' })
    return respuestaJson(200, { token: 'token-sintetico', rol: 'CIUDADANO' })
  }
  assert.equal((await iniciarSesion('qa@example.invalid', 'solo-prueba-local')).rol, 'CIUDADANO')
})


test('Google rechaza respuestas vacías y sesiones incompletas sin guardar tokens', async () => {
  for (const cuerpo of [{}, null, { token: '', rol: 'CIUDADANO' },
    { token: 'sesion', rol: '' }, { estado: 'PERFIL_INCOMPLETO', correo: 'qa@example.test' }]) {
    globalThis.fetch = async () => respuestaJson(200, cuerpo)
    await assert.rejects(() => iniciarSesionConGoogle('credencial-prueba'), e => e.codigo === 'RESPUESTA_INESPERADA')
    assert.equal(obtenerToken(), null)
  }
})

test('Google distingue sesión de perfil incompleto y no almacena credenciales Google', async () => {
  const perfil = { estado: 'PERFIL_INCOMPLETO', tokenRegistro: 'registro-prueba', correo: 'qa@example.test' }
  globalThis.fetch = async () => respuestaJson(200, perfil)
  assert.deepEqual(await iniciarSesionConGoogle('credencial-prueba'), perfil)
  assert.equal(obtenerToken(), null)
  const sesion = { token: 'sesion-prueba', rol: 'CIUDADANO' }
  globalThis.fetch = async () => respuestaJson(200, sesion)
  assert.deepEqual(await iniciarSesionConGoogle('credencial-prueba'), sesion)
  assert.deepEqual(await completarPerfilGoogle({}), sesion)
  globalThis.fetch = async () => respuestaJson(200, {})
  await assert.rejects(() => completarPerfilGoogle({}), e => e.codigo === 'RESPUESTA_INESPERADA')
})

test('Google conserva el error de asociación y nunca redirige por un 409 público', async () => {
  globalThis.fetch = async () => respuestaJson(409, { codigo: 'CONFLICTO', mensaje: 'Confirme su cuenta existente' })
  await assert.rejects(() => iniciarSesionConGoogle('credencial-prueba'), e => e.estado === 409)
  assert.equal(llamadas.redireccion, null)
  assert.equal(obtenerToken(), null)
})
