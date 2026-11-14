import assert from 'node:assert/strict'
import { after, before, beforeEach, test } from 'node:test'
import { createServer } from 'vite'

let servidor
let SoloSinSesion
let guardarToken
let cerrarSesion
const memoria = new Map()
const almacenOriginal = globalThis.localStorage

// Un JWT de mentira: solo importa el payload (rol y vencimiento), igual que lo lee la app.
const token = (payload) => `cabecera.${Buffer.from(JSON.stringify(payload)).toString('base64url')}.firma`
const futuro = () => Math.floor(Date.now() / 1000) + 3600
const pasado = () => Math.floor(Date.now() / 1000) - 60

before(async () => {
  globalThis.localStorage = {
    getItem: (k) => memoria.get(k) ?? null,
    setItem: (k, v) => memoria.set(k, String(v)),
    removeItem: (k) => memoria.delete(k),
  }
  servidor = await createServer({ server: { middlewareMode: true, ws: false }, appType: 'custom' })
  ;({ default: SoloSinSesion } = await servidor.ssrLoadModule('/src/compartido/auth/SoloSinSesion.jsx'))
  ;({ guardarToken, cerrarSesion } = await servidor.ssrLoadModule('/src/compartido/auth/sesion.js'))
})
beforeEach(() => cerrarSesion())
after(async () => {
  await servidor?.close()
  if (almacenOriginal === undefined) delete globalThis.localStorage
  else globalThis.localStorage = almacenOriginal
})

const pantalla = 'PANTALLA DE ENTRAR'
const resultado = () => SoloSinSesion({ children: pantalla })

test('sin sesión se muestra la pantalla de entrar', () => {
  assert.equal(resultado(), pantalla)
})

test('un ciudadano con sesión va directo a /app, sin dejar la pantalla en el historial', () => {
  guardarToken(token({ rol: 'CIUDADANO', exp: futuro() }))
  const r = resultado()
  assert.equal(r.props.to, '/app')
  assert.equal(r.props.replace, true)
})

test('un administrador con sesión va a /admin', () => {
  guardarToken(token({ rol: 'ADMIN', exp: futuro() }))
  assert.equal(resultado().props.to, '/admin')
})

test('con la sesión vencida sí se muestra la pantalla de entrar', () => {
  guardarToken(token({ rol: 'CIUDADANO', exp: pasado() }))
  assert.equal(resultado(), pantalla)
})

test('un token dañado no cuenta como sesión', () => {
  guardarToken('no-es-un-jwt')
  assert.equal(resultado(), pantalla)
})
