import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter } from 'react-router-dom'
import { createServer } from 'vite'

let servidor
let AvisoIngreso
let BotonEsperando
let avisoDeIngreso

before(async () => {
  servidor = await createServer({ server: { middlewareMode: true, ws: false }, appType: 'custom' })
  ;({ default: AvisoIngreso } = await servidor.ssrLoadModule('/src/compartido/componentes/acceso/AvisoIngreso.jsx'))
  ;({ default: BotonEsperando } = await servidor.ssrLoadModule('/src/compartido/componentes/acceso/BotonEsperando.jsx'))
  ;({ avisoDeIngreso } = await servidor.ssrLoadModule('/src/compartido/auth/errorLogin.js'))
})

after(async () => { await servidor?.close() })

const pintar = (elemento) => renderToStaticMarkup(React.createElement(MemoryRouter, null, elemento))
const aviso = (error) => pintar(React.createElement(AvisoIngreso, { aviso: avisoDeIngreso(error), correo: 'ana@example.com' }))

test('sin aviso no se pinta nada', () => {
  assert.equal(pintar(React.createElement(AvisoIngreso, { aviso: null })), '')
})

test('el bloqueo dice hasta qué hora y ofrece restablecer la contraseña', () => {
  const html = aviso({ estado: 429, codigo: 'INTENTOS_EXCEDIDOS',
    datos: { esperarSegundos: 300, bloqueadoHasta: '2026-11-13T20:42:00Z' } })
  assert.match(html, /bloqueamos el ingreso con este correo durante 5 minutos/)
  assert.match(html, /3:42 p\. m\./)
  assert.match(html, /href="\/recuperar"[^>]*>Restablecer mi contraseña</)
  assert.match(html, /aviso--alerta/)
})

test('correo sin confirmar ofrece reenviar el enlace', () => {
  const html = aviso({ estado: 403, codigo: 'CORREO_NO_VERIFICADO' })
  assert.match(html, /Falta confirmar su correo/)
  assert.match(html, /href="\/verificar-correo"[^>]*>Reenviar enlace</)
})

test('datos incorrectos con un solo intento restante lo advierte', () => {
  const html = aviso({ estado: 401, codigo: 'CREDENCIALES_INVALIDAS', datos: { intentosRestantes: 1 } })
  assert.match(html, /no coinciden/)
  assert.match(html, /Le queda 1 intento/)
  assert.match(html, /role="alert"/)
})

test('un fallo del servicio no muestra el texto técnico que trae', () => {
  const html = aviso({ estado: 500, codigo: 'ERROR_INTERNO', mensaje: 'PSQLException: connection refused' })
  assert.doesNotMatch(html, /PSQL|connection|servidor/i)
  assert.match(html, /NotiUrba no está respondiendo/)
})

test('mientras dura el bloqueo el botón está apagado y muestra la cuenta regresiva', () => {
  const html = pintar(React.createElement(BotonEsperando, { fin: Date.now() + 272_000, alTerminar: () => {} }))
  assert.match(html, /disabled=""/)
  assert.match(html, /Podrá intentarlo en 4:3[12]/)
  assert.match(html, /type="submit"/)
})
