import assert from 'node:assert/strict'
import { readdirSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import { after, before, test } from 'node:test'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { createServer } from 'vite'

let servidor
let ResumenErrores
let Campo
let CampoPassword
let Selector
let AreaTexto

before(async () => {
  servidor = await createServer({ server: { middlewareMode: true, ws: false }, appType: 'custom' })
  const carga = async (ruta) => (await servidor.ssrLoadModule(ruta)).default
  ResumenErrores = await carga('/src/compartido/componentes/formulario/ResumenErrores.jsx')
  Campo = await carga('/src/compartido/componentes/formulario/Campo.jsx')
  CampoPassword = await carga('/src/compartido/componentes/formulario/CampoPassword.jsx')
  Selector = await carga('/src/compartido/componentes/formulario/Selector.jsx')
  AreaTexto = await carga('/src/compartido/componentes/formulario/AreaTexto.jsx')
})
after(async () => { await servidor?.close() })

const h = React.createElement
const campos = (error) => [
  h(Campo, { key: 'c', id: 'correo', etiqueta: 'Correo', error }),
  h(CampoPassword, { key: 'p', id: 'password', etiqueta: 'Contraseña', error }),
  h(Selector, { key: 's', id: 'tipo', etiqueta: 'Tipo', opciones: [], error }),
  h(AreaTexto, { key: 'a', id: 'descripcion', etiqueta: 'Descripción', error }),
]
const mensajeDe = (salida, id) => salida.match(new RegExp(`<p id="${id}-error"[^>]*>`))?.[0]

test('el resumen anuncia todos los errores con role="alert" y sin errores no dice nada', () => {
  const con = renderToStaticMarkup(h(ResumenErrores, { errores: { correo: 'Escriba su correo', password: 'Escriba su contraseña' } }))
  assert.match(con, /role="alert"/)
  assert.match(con, /Escriba su correo/)
  assert.match(con, /Escriba su contraseña/)
  assert.equal(renderToStaticMarkup(h(ResumenErrores, { errores: {} })), '')
})

test('dentro del resumen, el mensaje de cada campo se enlaza sin role y con aria-invalid y aria-describedby', () => {
  const salida = renderToStaticMarkup(h(ResumenErrores, { errores: { x: 'Error' } }, ...campos('Error')))
  for (const id of ['correo', 'password', 'tipo', 'descripcion']) {
    assert.match(salida, new RegExp(`id="${id}"[^>]*aria-invalid="true"`))
    assert.match(salida, new RegExp(`id="${id}"[^>]*aria-describedby="${id}-error"`))
    assert.doesNotMatch(mensajeDe(salida, id), /role=/)
  }
})

test('fuera del resumen, el mensaje de cada campo conserva role="alert"', () => {
  const salida = renderToStaticMarkup(h('div', null, ...campos('Error')))
  for (const id of ['correo', 'password', 'tipo', 'descripcion']) {
    assert.match(mensajeDe(salida, id), /role="alert"/)
  }
})

// Cada pantalla con campos debe estar clasificada: o tiene el resumen (y sus campos no llevan role)
// o conserva el role en el mensaje de cada campo. Una pantalla nueva sin clasificar rompe esta prueba.
const CON_RESUMEN = ['compartido/paginas/Login.jsx']
const CONSERVA_ROLE = [
  'app/componentes/HojaActualizarReporte.jsx',
  'app/reportar/PasoConfirmacion.jsx',
  'app/reportar/PasoDetalles.jsx',
  'app/reportar/PasoUbicacion.jsx',
  'compartido/paginas/NuevaContrasena.jsx',
  'compartido/paginas/RecuperarContrasena.jsx',
  'compartido/paginas/registro/PasoAcceso.jsx',
  'compartido/paginas/registro/PasoDatos.jsx',
  'app/paginas/EditarDatos.jsx',
  'compartido/paginas/CompletarPerfil.jsx',
]
const CON_CAMPOS = /<(Campo|CampoPassword|Selector|AreaTexto|CamposIdentidad|CamposResidencia|ConsentimientoRegistro|Casilla)\b/

function archivos(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((e) => {
    const ruta = join(dir, e.name)
    return e.isDirectory() ? archivos(ruta) : ruta.endsWith('.jsx') ? [ruta] : []
  })
}

test('todas las pantallas con campos están clasificadas respecto del resumen', () => {
  const raiz = new URL('../../../', import.meta.url).pathname
  const pantallas = archivos(raiz)
    .map((ruta) => ruta.slice(raiz.length))
    .filter((ruta) => !ruta.startsWith('compartido/componentes/formulario/'))
    .filter((ruta) => CON_CAMPOS.test(readFileSync(join(raiz, ruta), 'utf8')))
  const clasificadas = [...CON_RESUMEN, ...CONSERVA_ROLE].sort()
  assert.deepEqual(pantallas.sort(), clasificadas)
  for (const ruta of CON_RESUMEN) assert.match(readFileSync(join(raiz, ruta), 'utf8'), /<ResumenErrores\b/)
  for (const ruta of CONSERVA_ROLE) assert.doesNotMatch(readFileSync(join(raiz, ruta), 'utf8'), /<ResumenErrores\b/)
})
