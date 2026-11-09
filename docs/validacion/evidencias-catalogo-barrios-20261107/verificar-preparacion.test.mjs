import assert from 'node:assert/strict'
import { readFileSync, existsSync } from 'node:fs'
import { createHash } from 'node:crypto'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'
import { test } from 'node:test'

const root = new URL('../../../', import.meta.url)
const read = path => readFileSync(new URL(path, root))
const json = path => JSON.parse(read(path).toString('utf8'))
const dir = 'docs/validacion/evidencias-catalogo-barrios-20261107/'
const review = json(dir + 'revision-andres.json')
const source = json(dir + 'fuente.json')
const transcription = json(dir + 'transcripcion.json')
const recorded = json(dir + 'chrome-resultado.json')
const csv = read(dir + 'barrios-pmgr-2020.csv').toString('utf8').trim().split(/\r?\n/).slice(1).map(line => {
  const comma = line.indexOf(',')
  return { comuna: Number(line.slice(0, comma)), nombre: line.slice(comma + 1) }
})
const hash = bytes => createHash('sha256').update(bytes).digest('hex')
const pair = row => `${row.comuna ?? row.idComuna}:${row.nombre}`
const dml = text => text.slice(text.indexOf('INSERT INTO barrio')).replace(/\r\n/g, '\n')

test('fuente, CSV y transcripción mantienen hashes, 135 pares únicos y comunas históricas', () => {
  assert.equal(hash(read(dir + 'fuente-pmgr-2020.pdf')), source.sha256)
  assert.equal(hash(read(dir + 'barrios-pmgr-2020.csv')), review.sha256Csv)
  assert.equal(csv.length, 135)
  assert.deepEqual(csv.map(pair), transcription.map(pair))
  assert.equal(new Set(csv.map(pair)).size, 135)
  assert.deepEqual(Array.from({ length: 10 }, (_, i) => csv.filter(r => r.comuna === i + 1).length), source.conteosPorComuna)
})

test('V17 conserva exactamente el SQL ya probado y sigue fuera de Flyway activo', () => {
  const sql = read(review.migracion)
  assert.equal(hash(sql), review.sha256Migracion)
  assert.equal(dml(sql.toString('utf8')), dml(read(dir + 'V17__catalogo_barrios_pmgr_2020.propuesta.sql').toString('utf8')))
  assert.match(sql.toString('utf8'), /ON CONFLICT \(id_comuna, nombre\) DO NOTHING;/)
  assert.equal(existsSync(new URL('backend/src/main/resources/db/migration/V17__catalogo_barrios_pmgr_2020.sql', root)), false)
})

test('duplicado de nombre entre comunas se mantiene separado y las decisiones siguen pendientes', () => {
  assert.deepEqual(csv.filter(r => r.nombre === 'Diablos Rojos').map(r => r.comuna), [7, 8])
  assert.equal(review.incidencias.length, 5)
  assert.ok(review.incidencias.every(r => r.estado === 'PENDIENTE' && r.decision === null && r.evidenciaDecision === null))
  assert.equal(review.aprobadoPor, null)
  assert.equal(review.aprobadoEn, null)
  assert.equal(review.ejecutadaEnProduccion, false)
})

test('evidencia QA sin mocks recibe exclusivamente barrios del CSV, IDs válidos y ningún administrativo', () => {
  assert.equal(recorded.mocksDesactivados, true)
  assert.equal(recorded.estado, 200)
  assert.equal(recorded.barrios.length, 135)
  assert.deepEqual(recorded.barrios.map(pair).sort(), csv.map(pair).sort())
  assert.equal(new Set(recorded.barrios.map(b => b.idBarrio)).size, 135)
  assert.ok(recorded.barrios.every(b => Number.isInteger(b.idBarrio) && b.idBarrio > 0 && b.nombre !== 'Administración NotiUrba'))
  assert.equal(recorded.conservado, String(recorded.seleccionado.idBarrio))
  assert.deepEqual(recorded.escriturasHttp, [])
})

test('selector real del registro renderiza los 135 registros guardados de QA sin barrio interno', async () => {
  const require = createRequire(new URL('frontend/package.json', root))
  const React = require('react')
  const { renderToStaticMarkup } = require('react-dom/server')
  const { createServer } = await import(new URL('frontend/node_modules/vite/dist/node/index.js', root))
  const server = await createServer({ root: fileURLToPath(new URL('frontend/', root)), server: { middlewareMode: true }, appType: 'custom' })
  try {
    const { CamposResidencia } = await server.ssrLoadModule('/src/compartido/componentes/formulario/CamposPerfil.jsx')
    const html = renderToStaticMarkup(React.createElement(CamposResidencia, {
      datos: { idBarrio: recorded.conservado, direccionCorrespondencia: '' },
      errores: {}, cambiar: () => () => {}, barrios: recorded.barrios,
    }))
    const options = [...html.matchAll(/<option\b[^>]*value="([^"]*)"[^>]*>/g)].map(m => m[1]).filter(Boolean)
    assert.deepEqual(options, recorded.barrios.map(b => String(b.idBarrio)))
    assert.match(html, new RegExp(`<option[^>]*value="${recorded.conservado}"[^>]*selected=`))
    assert.doesNotMatch(html, /Administración NotiUrba|No hay barrios disponibles/)
  } finally { await server.close() }
})
