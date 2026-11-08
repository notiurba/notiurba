import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { createServer } from 'vite'
import { MemoryRouter } from 'react-router-dom'

let servidor
let CamposResidencia
let LimiteDireccionContexto
let ContenidoAvisoPrivacidad
let AvisoPrivacidad

const datos = { idBarrio: '', direccionCorrespondencia: 'D'.repeat(100) }
const props = { datos, errores: {}, cambiar: () => () => {}, barrios: [] }

before(async () => {
  servidor = await createServer({ server: { middlewareMode: true }, appType: 'custom' })
  ;({ CamposResidencia } = await servidor.ssrLoadModule('/src/compartido/componentes/formulario/CamposPerfil.jsx'))
  ;({ LimiteDireccionContexto } = await servidor.ssrLoadModule('/src/compartido/componentes/formulario/limiteDireccionContexto.js'))
  ;({ default: ContenidoAvisoPrivacidad } = await servidor.ssrLoadModule('/src/compartido/componentes/avisos/ContenidoAvisoPrivacidad.jsx'))
  ;({ default: AvisoPrivacidad } = await servidor.ssrLoadModule('/src/compartido/paginas/AvisoPrivacidad.jsx'))
})

after(async () => { await servidor?.close() })

const campo = (p = props) => React.createElement(CamposResidencia, p)
const registro = (p = props) => renderToStaticMarkup(React.createElement(LimiteDireccionContexto, { value: 100 }, campo(p)))

test('dirección de registro renderiza límite y contador 100; otras pantallas conservan 150', () => {
  assert.match(registro(), /maxLength="100"/)
  assert.match(registro(), /100\/100/)
  assert.match(renderToStaticMarkup(campo()), /maxLength="150"/)
})

test('dirección anterior de 101 se conserva completa y comunica el exceso sin recortarla', () => {
  const html = registro({ ...props, datos: { ...datos, direccionCorrespondencia: 'D'.repeat(101) } })
  assert.ok(html.includes(`value="${'D'.repeat(101)}"`))
  assert.match(html, /no se ha recortado/)
  assert.match(html, /aria-invalid="true"/)
})

test('catálogo vacío deshabilita el selector y explica la falta de barrios sin inventarlos', () => {
  const html = registro()
  assert.match(html, /<select[^>]*disabled=""/)
  assert.match(html, /No hay barrios disponibles/)
  assert.doesNotMatch(html, /Administración NotiUrba/)
})

test('la ruta independiente reutiliza exactamente el contenido del aviso del diálogo', () => {
  const contenido = renderToStaticMarkup(React.createElement(ContenidoAvisoPrivacidad))
  const pagina = renderToStaticMarkup(React.createElement(MemoryRouter, null, React.createElement(AvisoPrivacidad)))
  assert.ok(pagina.includes(contenido))
  assert.match(pagina, /href="\/"/)
})
