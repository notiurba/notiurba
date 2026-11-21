import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import { createServer } from 'vite'

let servidor
let consultaAdministrativa
let rangoFechasInvalido

before(async () => {
  servidor = await createServer({ server: { middlewareMode: true, ws: false }, appType: 'custom' })
  ;({ consultaAdministrativa, rangoFechasInvalido } = await servidor.ssrLoadModule('/src/admin/servicios/mapaAdminService.js'))
})

after(async () => { await servidor?.close() })

test('serializa los nombres y parámetros que define el contrato administrativo', () => {
  assert.equal(
    consultaAdministrativa({ categoria: 2, estado: 'En trámite', comuna: 5, desde: '2026-11-01', hasta: '2026-11-19', pagina: 2, tamano: 5, vigencia: 'vigentes' }),
    '?idCategoria=2&estado=EN_TRAMITE&comuna=5&desde=2026-11-01&hasta=2026-11-19&pagina=2&tamano=5&vigencia=vigentes',
  )
})

test('detecta únicamente rangos de fecha invertidos', () => {
  assert.equal(rangoFechasInvalido('2026-11-20', '2026-11-19'), true)
  assert.equal(rangoFechasInvalido('2026-11-19', '2026-11-19'), false)
  assert.equal(rangoFechasInvalido('', '2026-11-19'), false)
})
