import assert from 'node:assert/strict'
import { after, before, beforeEach, test } from 'node:test'
import { createServer } from 'vite'

let servidor
let obtenerReportesCiudadanos
let separarPorUbicacion

const respuestaJson = (estado, cuerpo) =>
  new Response(JSON.stringify(cuerpo), { status: estado })

before(async () => {
  const almacen = new Map()
  globalThis.localStorage = {
    getItem: (clave) => almacen.get(clave) ?? null,
    setItem: (clave, valor) => almacen.set(clave, String(valor)),
    removeItem: (clave) => almacen.delete(clave),
  }
  globalThis.window = {
    location: { pathname: '/admin/mapa', assign: () => {} },
    dispatchEvent: () => true,
  }
  servidor = await createServer({
    server: { middlewareMode: true, ws: false },
    appType: 'custom',
    define: { 'import.meta.env.VITE_USAR_MOCKS': '"false"' },
  })
  ;({ obtenerReportesCiudadanos, separarPorUbicacion } = await servidor.ssrLoadModule('/src/admin/servicios/reportesService.js'))
})

after(async () => { await servidor?.close() })

beforeEach(() => localStorage.removeItem('notiurba.token'))

test('normaliza los reportes reales que devuelve la API', async () => {
  globalThis.fetch = async () => respuestaJson(200, [{
    codigoSeguimiento: 'NU-000321',
    categoria: { idCategoria: 2, nombre: 'Alumbrado público' },
    descripcion: 'Luminaria apagada',
    latitud: 4.084,
    longitud: -76.198,
    creadoEn: '2026-11-16T19:20:00Z',
    estado: 'Enviado',
  }])

  assert.deepEqual(await obtenerReportesCiudadanos(), [{
    id: 'NU-000321',
    categoria: 'Alumbrado público',
    descripcion: 'Luminaria apagada',
    latitud: 4.084,
    longitud: -76.198,
    fechaCreacion: '2026-11-16',
    estado: 'Enviado',
  }])
})

test('propaga un error de red sin sustituirlo por reportes ficticios', async () => {
  globalThis.fetch = async () => { throw new TypeError('red caída') }

  await assert.rejects(obtenerReportesCiudadanos, (error) =>
    error.codigo === 'SIN_CONEXION' && error.estado === 0)
})

for (const estado of [401, 403, 500]) {
  test(`propaga HTTP ${estado} sin sustituirlo por reportes ficticios`, async () => {
    globalThis.fetch = async () => respuestaJson(estado, {
      codigo: estado === 401 ? 'NO_AUTENTICADO' : 'NO_AUTORIZADO',
      mensaje: 'No fue posible consultar los reportes.',
      campo: null,
    })

    await assert.rejects(obtenerReportesCiudadanos, (error) => error.estado === estado)
  })
}

const reporte = (id, latitud, longitud) => ({ id, latitud, longitud })

test('separa los reportes con ubicación de los que no la tienen, sin perder el orden', () => {
  const reportes = [
    reporte('A', 4.08, -76.19),
    reporte('B', null, null),
    reporte('C', 4.09, -76.2),
    reporte('D', 4.1, null),
  ]

  const { conUbicacion, sinUbicacion } = separarPorUbicacion(reportes)

  assert.deepEqual(conUbicacion.map((r) => r.id), ['A', 'C'])
  assert.deepEqual(sinUbicacion.map((r) => r.id), ['B', 'D'])
})

test('un listado donde ningún reporte tiene ubicación no deja nada para dibujar', () => {
  const { conUbicacion, sinUbicacion } = separarPorUbicacion([reporte('A', null, null), reporte('B', null, null)])

  assert.equal(conUbicacion.length, 0)
  assert.equal(sinUbicacion.length, 2)
})

test('un listado completo con ubicación no deja reportes fuera', () => {
  const { conUbicacion, sinUbicacion } = separarPorUbicacion([reporte('A', 4.08, -76.19), reporte('B', 4.09, -76.2)])

  assert.equal(conUbicacion.length, 2)
  assert.equal(sinUbicacion.length, 0)
})

test('trata como sin ubicación las coordenadas que no son números finitos', () => {
  const invalidos = [
    reporte('nulo', null, null),
    reporte('indefinido', undefined, undefined),
    reporte('nan', Number.NaN, -76.19),
    reporte('texto', '4.08', '-76.19'),
    reporte('infinito', Number.POSITIVE_INFINITY, -76.19),
    reporte('faltan', undefined, -76.19),
  ]

  const { conUbicacion, sinUbicacion } = separarPorUbicacion(invalidos)

  assert.equal(conUbicacion.length, 0)
  assert.equal(sinUbicacion.length, invalidos.length)
})

test('el cero es una coordenada válida y no se confunde con «sin ubicación»', () => {
  const { conUbicacion } = separarPorUbicacion([reporte('cero', 0, 0)])

  assert.deepEqual(conUbicacion.map((r) => r.id), ['cero'])
})

test('no modifica la lista que recibe', () => {
  const reportes = [reporte('A', 4.08, -76.19), reporte('B', null, null)]
  const copia = structuredClone(reportes)

  separarPorUbicacion(reportes)

  assert.deepEqual(reportes, copia)
})
