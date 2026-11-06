import { createRequire } from 'node:module'
import { readFileSync } from 'node:fs'
import { pathToFileURL } from 'node:url'
import assert from 'node:assert/strict'

// El adaptador de Link es exclusivamente de prueba: permite renderizar sin navegador ni servidor de API.
const require = createRequire(process.env.NOTIURBA_NODE_MODULES ? `${process.env.NOTIURBA_NODE_MODULES}/package.json` : new URL('../package.json', import.meta.url))
const { transformWithOxc } = await import(pathToFileURL(require.resolve('vite')))
const React = require('react')
const { renderToStaticMarkup } = require('react-dom/server')
const estadoURL = new URL('../src/app/componentes/estadoCorreo.js', import.meta.url)
const componentURL = new URL('../src/app/componentes/ResultadoCorreo.jsx', import.meta.url)
const source = readFileSync(componentURL, 'utf8').replace(/^import (\w+) from '(.+\.svg)'$/gm, (_, name, path) => {
  const url = new URL(path, componentURL)
  const asset = readFileSync(url, 'utf8')
  assert.ok(asset.length > 0 && /<svg\b/.test(asset))
  return `const ${name} = ${JSON.stringify(url.href)}`
})
let { code } = await transformWithOxc(source, 'ResultadoCorreo.jsx', { jsx: { runtime: 'automatic' } })
code = code.replace('import { Link } from "react-router-dom";', 'const Link = ({ to, children, ...props }) => _jsx("a", { ...props, href: to, children });')
code = code.replace('"./estadoCorreo"', JSON.stringify(estadoURL.href))
for (const name of ['react', 'react/jsx-runtime']) code = code.replaceAll(`from "${name}"`, `from ${JSON.stringify(pathToFileURL(require.resolve(name)).href)}`)
const { default: Resultado } = await import(`data:text/javascript;base64,${Buffer.from(code).toString('base64')}`)
const reporte = { codigoSeguimiento: 'NU-000042', estado: 'Recibido', categoria: { nombre: 'Vías' }, barrio: { nombre: 'Barrio sintético' }, comuna: 5 }
for (const estado of ['PENDIENTE', 'FALLIDO', 'ENVIADO', 'NO_ENVIADO_CORREO']) {
  const envio = { estado, reintentable: estado === 'FALLIDO', ruta: { mensaje: 'Canal oficial, sin radicación simulada', entidad: 'Entidad sintética', canal: 'Teléfono', acceso: '164', permiteCorreo: false } }
  const markup = renderToStaticMarkup(React.createElement(Resultado, { reporte, envio, onEnviar() {} }))
  assert.ok(markup.includes('NU-000042'))
  assert.ok(markup.includes('CORREO · ' + estado))
  assert.ok(markup.includes('/app/mis-reportes'))
  if (estado === 'FALLIDO') assert.ok(markup.includes('Reintentar envío'))
  if (estado === 'NO_ENVIADO_CORREO') assert.ok(markup.includes('tel:164'))
}
const rebote = renderToStaticMarkup(React.createElement(Resultado, { reporte: { ...reporte, estado: 'Enviado' }, envio: { estado: 'FALLIDO', rebote: true }, onEnviar() {} }))
assert.ok(rebote.includes('FALLIDO (rebote)') && rebote.includes('Enviado'))
assert.ok(!rebote.includes('Reintentar envío'))
const avanzado = renderToStaticMarkup(React.createElement(Resultado, { reporte: { ...reporte, estado: 'En trámite' }, envio: { estado: 'FALLIDO', rebote: true }, onEnviar() {} }))
assert.ok(avanzado.includes('El reporte conserva su estado «En trámite»'))
assert.ok(!avanzado.includes('El reporte conserva su estado «Enviado»'))
console.log('OK: Oxc compila el componente real; React SSR verifica los cinco resultados. Esto no sustituye la inspección visual en móvil/escritorio.')
