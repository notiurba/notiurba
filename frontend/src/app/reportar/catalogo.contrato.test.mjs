import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createServer } from 'vite'
import { prepararCategorias } from './catalogo.js'

test('conserva todas las categorías con ruta activa y distingue el catálogo general de V16', async () => {
  const server = await createServer({
    server: { middlewareMode: true, ws: false }, appType: 'custom',
    define: { 'import.meta.env.VITE_USAR_MOCKS': '"true"' },
  })
  const fetchAnterior = globalThis.fetch
  globalThis.fetch = async () => { throw new Error('Esta prueba no debe llamar al backend real') }
  try {
    const { peticion } = await server.ssrLoadModule('/src/compartido/api/cliente.js')
    const general = await peticion('/api/categorias')
    const rutas = await peticion('/api/enrutamiento/categorias')
    assert.equal(general.length, 15, 'El catálogo general conserva la categoría histórica activa')
    assert.ok(general.some(c => c.idCategoria === 15))
    assert.equal(rutas.length, 14, 'V16 define 14 rutas, no una ruta inventada para ID 15')
    const opciones = prepararCategorias(rutas)
    assert.deepEqual(opciones.map(c => c.idCategoria).sort((a, b) => a - b),
      rutas.map(c => c.idCategoria).sort((a, b) => a - b),
      'Ninguna categoría devuelta por el endpoint de rutas debe desaparecer en la presentación')
    assert.equal(opciones.at(-1).idCategoria, 10, 'Otro problema permanece al final')
    assert.ok(opciones.every(c => c.nombre && c.etiqueta && c.descripcion))
    await assert.rejects(() => peticion('/api/enrutamiento/categorias/15?emergencia=false'),
      e => e.estado === 400 && e.codigo === 'VALIDACION' && e.campo === 'idCategoria')
  } finally {
    globalThis.fetch = fetchAnterior
    await server.close()
  }
})
