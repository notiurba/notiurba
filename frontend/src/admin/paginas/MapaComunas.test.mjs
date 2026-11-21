import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import React from 'react'
import TestRenderer, { act } from 'react-test-renderer'
import { createServer } from 'vite'

globalThis.IS_REACT_ACT_ENVIRONMENT = true

const soporte = (archivo) => new URL(`./soporte-pruebas/${archivo}`, import.meta.url).pathname

let servidor
let MapaComunas
let paginaCriticosAnterior
let paginaCriticosSiguiente

before(async () => {
  const almacen = new Map()
  globalThis.localStorage = {
    getItem: (clave) => almacen.get(clave) ?? null,
    setItem: (clave, valor) => almacen.set(clave, String(valor)),
    removeItem: (clave) => almacen.delete(clave),
  }
  globalThis.window = { location: { pathname: '/admin/mapa', assign: () => {} }, dispatchEvent: () => true }
  servidor = await createServer({
    server: { middlewareMode: true, ws: false },
    appType: 'custom',
    define: { 'import.meta.env.VITE_USAR_MOCKS': '"false"' },
    resolve: {
      alias: [
        { find: /^leaflet$/, replacement: soporte('leaflet.mjs') },
        { find: /^leaflet\.heat$/, replacement: soporte('leaflet-heat.mjs') },
        { find: /^react-leaflet$/, replacement: soporte('react-leaflet.mjs') },
      ],
    },
  })
  ;({ default: MapaComunas } = await servidor.ssrLoadModule('/src/admin/paginas/MapaComunas.jsx'))
  ;({ paginaCriticosAnterior, paginaCriticosSiguiente } = await servidor.ssrLoadModule('/src/admin/paginas/mapaPaginacion.js'))
})

after(async () => { await servidor?.close() })

const texto = (nodo) => typeof nodo === 'string'
  ? nodo
  : Array.isArray(nodo)
    ? nodo.map(texto).join(' ')
    : texto(nodo?.children ?? [])

const reporte = (codigo, latitud, longitud) => ({
  codigoSeguimiento: codigo,
  categoria: { idCategoria: 1, nombre: 'Vías' },
  barrio: { idBarrio: 1, nombre: 'Centro' },
  latitud,
  longitud,
  creadoEn: '2026-11-16T19:20:00Z',
  estado: 'Enviado',
})

async function abrirMapa(reportes, { total = reportes.length, sinComuna = 0 } = {}) {
  const solicitudes = []
  globalThis.fetch = async (url) => {
    solicitudes.push(String(url))
    if (String(url).includes('/api/categorias')) return new Response(JSON.stringify([]), { status: 200 })
    if (String(url).includes('/api/admin/mapa/calor')) return new Response(JSON.stringify({ totalReportes: 3, totalPuntos: 2, truncado: false, sinComuna: 0, puntos: [{ latitud: 4.08, longitud: -76.19, peso: 2 }], comunasCriticas: { pagina: 1, tamano: 5, totalComunas: 1, totalPaginas: 1, items: [{ posicion: 1, comuna: 1, total: 3, categoriaPrincipal: { idCategoria: 1, nombre: 'Vías' } }] } }), { status: 200 })
    return new Response(JSON.stringify({ total, sinComuna, truncado: false, reportes }), { status: 200 })
  }
  let vista
  await act(async () => { vista = TestRenderer.create(React.createElement(MapaComunas)) })
  await act(async () => { await new Promise((resolver) => setTimeout(resolver, 50)) })
  return { vista, solicitudes }
}

test('consume el endpoint especializado y dibuja sus marcadores vigentes', async () => {
  const { vista, solicitudes } = await abrirMapa([reporte('A', 4.08, -76.19), reporte('B', 4.09, -76.2)], { total: 2, sinComuna: 1 })

  assert.equal(vista.root.findAllByType('marker').length, 2)
  assert.match(solicitudes[0], /\/api\/admin\/mapa$/)
  assert.match(texto(vista.toJSON()), /1\s+sin comuna asignada/)
})

test('muestra un mapa vacío cuando la API no devuelve vigentes con ubicación', async () => {
  const { vista } = await abrirMapa([], { total: 0 })

  assert.equal(vista.root.findAllByType('marker').length, 0)
  assert.match(texto(vista.toJSON()), /solo muestra reportes vigentes con ubicación válida/)
})

test('conserva los filtros de fecha y evita consultas de rango invertido', async () => {
  const { vista, solicitudes } = await abrirMapa([reporte('A', 4.08, -76.19)])
  const entradas = vista.root.findAllByType('input').filter((nodo) => nodo.props.type === 'date')
  await act(async () => { entradas[0].props.onChange({ target: { value: '2026-11-20' } }) })
  await act(async () => { entradas[1].props.onChange({ target: { value: '2026-11-19' } }) })
  assert.match(texto(vista.toJSON()), /fecha inicial no puede ser posterior/)
  assert.equal(solicitudes.filter((url) => url.includes('/api/admin/mapa')).length, 2)
})

test('carga calor y puntos críticos desde el endpoint especializado', async () => {
  const { vista, solicitudes } = await abrirMapa([reporte('A', 4.08, -76.19)])
  const capas = vista.root.findAllByType('input').filter((nodo) => nodo.props.type === 'checkbox')
  await act(async () => { capas[2].props.onChange({ target: { checked: true } }) })
  await act(async () => { await new Promise((resolver) => setTimeout(resolver, 30)) })
  assert.ok(solicitudes.some((url) => url.includes('/api/admin/mapa/calor?pagina=1&tamano=5')))
  assert.match(texto(vista.toJSON()), /Puntos críticos por comuna/)
})

test('limita la paginación del ranking ante clics consecutivos', () => {
  assert.equal(paginaCriticosAnterior(1, 2), 1)
  assert.equal(paginaCriticosSiguiente(1, 2), 2)
  assert.equal(paginaCriticosSiguiente(paginaCriticosSiguiente(1, 2), 2), 2)
})

test('la vista limita clics rápidos y conserva controles ante una respuesta de menos páginas', async () => {
  globalThis.window.location.pathname = '/admin/puntos-criticos'
  const solicitudes = []
  globalThis.fetch = async (url) => {
    const textoUrl = String(url); solicitudes.push(textoUrl)
    if (textoUrl.includes('/api/categorias')) return new Response(JSON.stringify([]), { status: 200 })
    if (textoUrl.includes('/api/admin/mapa/calor')) {
      const pagina = new URL(textoUrl).searchParams.get('pagina')
      return new Response(JSON.stringify({ totalReportes: 3, totalPuntos: 1, truncado: false, sinComuna: 0, puntos: [], comunasCriticas: { pagina: Number(pagina), tamano: 5, totalComunas: pagina === '2' ? 1 : 2, totalPaginas: pagina === '2' ? 1 : 2, items: pagina === '2' ? [] : [{ posicion: 1, comuna: 3, total: 3, categoriaPrincipal: { nombre: 'Vías' } }] } }), { status: 200 })
    }
    return new Response(JSON.stringify({ total: 0, sinComuna: 0, truncado: false, reportes: [] }), { status: 200 })
  }
  let vista
  await act(async () => { vista = TestRenderer.create(React.createElement(MapaComunas)) })
  await act(async () => { await new Promise((r) => setTimeout(r, 30)) })
  const siguiente = vista.root.findAllByType('button').find((n) => texto(n.props.children) === 'Siguiente')
  await act(async () => { siguiente.props.onClick(); siguiente.props.onClick(); await new Promise((r) => setTimeout(r, 30)) })
  const calor = solicitudes.filter((url) => url.includes('/api/admin/mapa/calor'))
  assert.ok(calor.some((url) => url.includes('pagina=2')))
  assert.ok(!calor.some((url) => url.includes('pagina=3')))
  assert.equal(solicitudes.filter((url) => /\/api\/admin\/mapa(?:\?|$)/.test(url)).length, 1)
  assert.match(texto(vista.toJSON()), /Página\s+2\s+de\s+1/)
  assert.ok(vista.root.findAllByType('button').some((n) => texto(n.props.children) === 'Anterior' && !n.props.disabled))
  globalThis.window.location.pathname = '/admin/mapa'
})

test('expone una lista accesible cuando dos reportes comparten coordenadas', async () => {
  const { vista } = await abrirMapa([reporte('A', 4.08, -76.19), reporte('B', 4.08, -76.19)])
  const salida = texto(vista.toJSON())
  assert.match(salida, /Reportes en la misma ubicación/)
  assert.match(salida, /A/)
  assert.match(salida, /B/)
})

test('el diagnóstico conserva huecos, solapamientos y selección de incidencias', async () => {
  const { vista } = await abrirMapa([reporte('A', 4.08, -76.19)])
  const casillas = vista.root.findAllByType('input').filter((nodo) => nodo.props.type === 'checkbox')
  await act(async () => { casillas.at(-1).props.onChange({ target: { checked: true } }) })
  assert.match(texto(vista.toJSON()), /Huecos interiores/)
  assert.match(texto(vista.toJSON()), /Solapamientos/)
  const boton = vista.root.findAllByType('button').find((nodo) => nodo.props['aria-label']?.startsWith('Acercar a '))
  await act(async () => { boton.props.onClick() })
  assert.match(texto(vista.toJSON()), /posible exclusion cartografica/)
})
