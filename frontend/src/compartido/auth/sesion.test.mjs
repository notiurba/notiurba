import assert from 'node:assert/strict'
import { beforeEach, test } from 'node:test'
import { cerrarSesion, estadoSesion, guardarToken, obtenerRol, obtenerToken } from './sesion.js'

// Node no tiene localStorage: se simula con un objeto que guarda pares clave-valor.
const almacen = new Map()
globalThis.localStorage = {
  getItem: (clave) => (almacen.has(clave) ? almacen.get(clave) : null),
  setItem: (clave, valor) => almacen.set(clave, String(valor)),
  removeItem: (clave) => almacen.delete(clave),
}

// Arma un JWT de mentira: encabezado.datos.firma, con los datos en base64url.
function crearToken(datos) {
  const base64url = (objeto) => Buffer.from(JSON.stringify(objeto)).toString('base64url')
  return `${base64url({ alg: 'HS256' })}.${base64url(datos)}.firma`
}

const ahora = () => Math.floor(Date.now() / 1000)

beforeEach(() => almacen.clear())

test('sin token: sesión «ninguna» y rol null', () => {
  assert.equal(estadoSesion(), 'ninguna')
  assert.equal(obtenerRol(), null)
})

test('token vigente de CIUDADANO: sesión «activa» y rol CIUDADANO', () => {
  guardarToken(crearToken({ rol: 'CIUDADANO', exp: ahora() + 3600 }))
  assert.equal(estadoSesion(), 'activa')
  assert.equal(obtenerRol(), 'CIUDADANO')
})

test('token vigente de ADMIN: rol ADMIN', () => {
  guardarToken(crearToken({ rol: 'ADMIN', exp: ahora() + 3600 }))
  assert.equal(obtenerRol(), 'ADMIN')
})

test('token vencido: sesión «vencida»', () => {
  guardarToken(crearToken({ rol: 'CIUDADANO', exp: ahora() - 10 }))
  assert.equal(estadoSesion(), 'vencida')
})

test('token dañado: se trata como «ninguna», no revienta', () => {
  guardarToken('esto-no-es-un-jwt')
  assert.equal(estadoSesion(), 'ninguna')
  assert.equal(obtenerRol(), null)
})

test('cerrar sesión borra el token', () => {
  guardarToken(crearToken({ rol: 'CIUDADANO', exp: ahora() + 3600 }))
  cerrarSesion()
  assert.equal(obtenerToken(), null)
  assert.equal(estadoSesion(), 'ninguna')
})
