import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'

const css = (ruta) => readFileSync(new URL(ruta, import.meta.url), 'utf8')

// Valor en px de una propiedad dentro de la primera regla que empieza con ese selector.
function px(hoja, selector, propiedad) {
  const regla = hoja.match(new RegExp(`(?:^|\\n)${selector.replace('.', '\\.')}\\s*\\{([^}]*)\\}`))
  const valor = regla?.[1].match(new RegExp(`(?:^|[\\s;])${propiedad}:\\s*(\\d+)px`))
  return valor ? Number(valor[1]) : 0
}

test('el botón de icono mide al menos 44×44 px', () => {
  const hoja = css('./BotonIcono.css')
  assert.ok(px(hoja, '.boton-icono', 'width') >= 44)
  assert.ok(px(hoja, '.boton-icono', 'height') >= 44)
})

test('el filtro mide al menos 44 px de alto', () => {
  const hoja = css('./Filtro.css')
  assert.ok(Math.max(px(hoja, '.filtro', 'height'), px(hoja, '.filtro', 'min-height')) >= 44)
})

// La barra de acciones del registro (paso 2) apila 3 botones de 48 px: unos 175 px, más el área segura.
test('al enfocar un campo del formulario queda por encima de la barra fija de acciones', () => {
  const hoja = css('./formulario/FormularioPantalla.css')
  const regla = hoja.match(/\.formulario-pantalla input,[^{]*\{([^}]*)\}/)?.[1] ?? ''
  assert.ok(Number(regla.match(/scroll-margin-bottom:\s*(\d+)px/)?.[1] ?? 0) >= 200)
  assert.match(hoja, /\.formulario-pantalla \.campo\b[^{]*\{[^}]*scroll-margin-bottom:\s*(?:2\d\d|[3-9]\d\d)px/)
})
