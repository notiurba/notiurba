import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { createServer } from 'vite'

let servidor
let CamposIdentidad

before(async () => {
  servidor = await createServer({ server: { middlewareMode: true, ws: false }, appType: 'custom' })
  ;({ CamposIdentidad } = await servidor.ssrLoadModule('/src/compartido/componentes/formulario/CamposPerfil.jsx'))
})
after(async () => { await servidor?.close() })

const tiposDocumento = [
  { idTipoDocumento: 1, codigo: 'CC', nombre: 'Cédula de ciudadanía' },
  { idTipoDocumento: 2, codigo: 'CE', nombre: 'Cédula de extranjería' },
  { idTipoDocumento: 4, codigo: 'PAS', nombre: 'Pasaporte' },
]

test('el selector de documento muestra el código y el nombre, y conserva el id y el valor', () => {
  const datos = { nombres: '', apellidos: '', idTipoDocumento: '', numeroDocumento: '', telefono: '' }
  const salida = renderToStaticMarkup(
    React.createElement(CamposIdentidad, { datos, errores: {}, cambiar: () => () => {}, tiposDocumento }),
  )
  assert.match(salida, /<select id="idTipoDocumento"/)
  assert.match(salida, /<option value="1">CC — Cédula de ciudadanía<\/option>/)
  assert.match(salida, /<option value="2">CE — Cédula de extranjería<\/option>/)
  assert.match(salida, /<option value="4">PAS — Pasaporte<\/option>/)
})
