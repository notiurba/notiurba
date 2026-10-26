import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import { createServer } from 'vite'
import { readFileSync } from 'node:fs'
import { createHash } from 'node:crypto'

let server
let identificar
let comunas
let reportes
let reportesFueraCobertura
let obtenerReportes

before(async () => {
  server = await createServer({ server: { middlewareMode: true }, appType: 'custom' })
  identificar = (await server.ssrLoadModule('/src/compartido/utils/geoUtils.js')).identificarComunaPorCoordenadas
  comunas = (await server.ssrLoadModule('/src/compartido/geo/comunasTulua.js')).comunasTulua
  const datos = await server.ssrLoadModule('/src/admin/datos/reportesPrueba.js')
  reportes = datos.reportesPrueba
  reportesFueraCobertura = datos.reportesFueraCobertura
  obtenerReportes = (await server.ssrLoadModule('/src/admin/servicios/reportesService.js')).obtenerReportesCiudadanos
})

after(async () => { await server?.close() })

const polygon = (id, coordinates) => ({
  type: 'Feature',
  properties: { COD_LOC_COM: id, NOM_LOC_COM: `Comuna ${id}` },
  geometry: { type: 'Polygon', coordinates },
})
const square = (id, x) => polygon(id, [[[x, 0], [x + 2, 0], [x + 2, 2], [x, 2], [x, 0]]])
const collection = (...features) => ({ type: 'FeatureCollection', features })

test('un interior unico recibe comuna; un punto exterior no recibe comuna', () => {
  const capa = collection(square('01', 0))
  assert.equal(identificar({ latitud: 1, longitud: 1 }, capa).comuna.id, '01')
  assert.equal(identificar({ latitud: 3, longitud: 3 }, capa).comuna, null)
  assert.equal(identificar({ latitud: 3, longitud: 3 }, capa).status, 'sin_determinar')
})

test('un limite compartido queda pendiente sin elegir la primera comuna', () => {
  const features = [square('01', 0), square('02', 2)]
  for (const capa of [collection(...features), collection(...features.toReversed())]) {
    const result = identificar({ latitud: 1, longitud: 2 }, capa)
    assert.equal(result.status, 'pendiente_revision')
    assert.equal(result.comuna, null)
    assert.deepEqual(result.comunasCandidatas.map((c) => c.id).sort(), ['01', '02'])
  }
})

test('un solapamiento interior y un borde exterior quedan pendientes', () => {
  const capa = collection(square('01', 0), square('02', 1))
  assert.equal(identificar({ latitud: 1, longitud: 1.5 }, capa).comuna, null)
  assert.equal(identificar({ latitud: 1, longitud: 1.5 }, capa).status, 'pendiente_revision')
  assert.equal(identificar({ latitud: 0, longitud: 0.5 }, capa).status, 'pendiente_revision')
})

test('los huecos no se asignan como interiores', () => {
  const capa = collection(polygon('01', [
    [[0, 0], [4, 0], [4, 4], [0, 4], [0, 0]],
    [[1, 1], [3, 1], [3, 3], [1, 3], [1, 1]],
  ]))
  assert.equal(identificar({ latitud: 2, longitud: 2 }, capa).comuna, null)
  assert.equal(identificar({ latitud: 1, longitud: 2 }, capa).status, 'pendiente_revision')
})

test('POT contiene codigos 01 a 10; su punto interior de Aguaclara pertenece a 10', () => {
  assert.deepEqual(comunas.features.map((f) => f.properties.COD_LOC_COM),
    Array.from({ length: 10 }, (_, i) => String(i + 1).padStart(2, '0')))
  const result = identificar({ latitud: 4.10975003647537, longitud: -76.19584751183994 }, comunas)
  assert.equal(result.status, 'dentro')
  assert.equal(result.comuna.id, '10')
})

test('demostracion: diez reportes, uno exclusivamente dentro de cada comuna POT', () => {
  assert.equal(reportes.length, 10)
  const ids = new Set(reportes.map((r) => r.id))
  assert.equal(ids.size, 10)
  const asignadas = []
  for (const reporte of reportes) {
    const result = identificar(reporte, comunas)
    assert.equal(result.status, 'dentro', reporte.id)
    assert.equal(result.comuna.id, reporte.comunaEsperada, reporte.id)
    const matches = comunas.features.filter((feature) =>
      identificar(reporte, collection(feature)).status !== 'sin_determinar')
    assert.equal(matches.length, 1, reporte.id)
    assert.equal(matches[0].properties.COD_LOC_COM, reporte.comunaEsperada)
    asignadas.push(result.comuna.id)
  }
  assert.deepEqual(asignadas.sort(), Array.from({ length: 10 }, (_, i) => String(i + 1).padStart(2, '0')))
})

test('NTU-010 exterior intencional se conserva intacto y sin comuna asignada', () => {
  assert.equal(reportesFueraCobertura.length, 1)
  const exterior = reportesFueraCobertura[0]
  assert.deepEqual(exterior, {
    id: 'NTU-010', categoria: 'Seguridad',
    descripcion: 'Reporte de prueba fuera del perimetro urbano cargado.',
    latitud: 4.126, longitud: -76.236, fechaCreacion: '2026-10-11', estado: 'Nuevo',
  })
  const result = identificar(exterior, comunas)
  assert.equal(result.status, 'sin_determinar')
  assert.equal(result.comuna, null)
  for (const feature of comunas.features) {
    assert.equal(identificar(exterior, collection(feature)).status, 'sin_determinar')
  }
})

test('el servicio de demostracion excluye casos fuera de cobertura', async () => {
  const visibles = await obtenerReportes()
  assert.deepEqual(visibles, reportes)
  assert.equal(visibles.length, 10)
  assert.ok(visibles.every((r) => !reportesFueraCobertura.some((e) => e.id === r.id)))
})

test('el solapamiento real POT entre comunas 03 y 10 requiere revision', () => {
  const result = identificar({ latitud: 4.0988354546698815, longitud: -76.194989986852 }, comunas)
  assert.equal(result.status, 'pendiente_revision')
  assert.equal(result.comuna, null)
  assert.deepEqual(result.comunasCandidatas.map((c) => c.id).sort(), ['03', '10'])
})

test('el diagnostico corresponde al POT intacto y sus sumas coinciden con el resumen', async () => {
  const { diagnosticoLimites: d } = await server.ssrLoadModule('/src/admin/datos/diagnosticoLimites.js')
  const original = readFileSync(new URL('../geo/comunas-tulua-POT2015-candidato.geojson', import.meta.url))
  assert.equal(createHash('sha256').update(original).digest('hex'), d.metadata.source_sha256)
  assert.equal(d.metadata.geometry_crs, 'EPSG:4326')
  assert.equal(d.metadata.area_crs, 'EPSG:3115')
  assert.equal(new Set(d.features.map((f) => f.properties.id)).size, d.features.length)
  for (const tipo of ['hueco', 'solapamiento']) {
    const features = d.features.filter((f) => f.properties.tipo === tipo)
    assert.equal(features.length, d.metadata.summary[tipo].cantidad)
    assert.ok(Math.abs(features.reduce((sum, f) => sum + f.properties.area_m2, 0) - d.metadata.summary[tipo].area_m2) < 1e-8)
    for (const f of features) {
      const p = f.properties
      assert.ok(p.area_m2 > 0)
      assert.equal(f.geometry.type, 'Polygon')
      for (const ring of f.geometry.coordinates) {
        assert.deepEqual(ring[0], ring.at(-1))
        for (const [lon, lat] of ring) {
          assert.ok(lon > -77 && lon < -75 && lat > 3 && lat < 5)
        }
      }
      if (!p.micro_superficie) {
        const result = identificar({ longitud: p.referencia[0], latitud: p.referencia[1] }, comunas)
        assert.equal(result.comuna, null, p.id)
        assert.equal(result.status, tipo === 'hueco' ? 'sin_determinar' : 'pendiente_revision', p.id)
      }
    }
  }
})
