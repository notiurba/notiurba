import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import { createServer } from 'vite'

let servidor
let opcionesConSeleccion

before(async () => {
  servidor = await createServer({ server: { middlewareMode: true, ws: false }, appType: 'custom' })
  ;({ opcionesConSeleccion } = await servidor.ssrLoadModule('/src/admin/paginas/opcionesFiltros.js'))
})

after(async () => { await servidor?.close() })

test('conserva Comuna 3 seleccionada cuando las fechas ya no devuelven reportes', () => {
  assert.deepEqual(opcionesConSeleccion([], '3', 'Todas'), ['3'])
  assert.deepEqual(opcionesConSeleccion(['Vías'], 'Aseo', 'Todos'), ['Vías', 'Aseo'])
})
