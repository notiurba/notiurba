import assert from 'node:assert/strict'
import { after, before, beforeEach, test } from 'node:test'
import { createServer } from 'vite'

let servidor
let obtenerReportesCiudadanos

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
  ;({ obtenerReportesCiudadanos } = await servidor.ssrLoadModule('/src/admin/servicios/reportesService.js'))
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
