import test from 'node:test'
import assert from 'node:assert/strict'
import { claveRuta, rutaActual, guardarConEvidencias } from './recuperacionReporte.js'

test('no muestra la ruta anterior al cambiar, limpiar categoría o cambiar la emergencia', () => {
  const anterior = { clave: claveRuta(1, false), ruta: { idCategoria: 1, permiteCorreo: true } }
  assert.equal(rutaActual(anterior, claveRuta(2, false)), null)
  assert.equal(rutaActual(anterior, claveRuta('', false)), null)
  assert.equal(rutaActual(anterior, claveRuta(1, true)), null)
  assert.equal(rutaActual(anterior, claveRuta(1, false)), anterior.ruta)
})

test('una respuesta tardía o fallida nunca habilita la ruta de otra selección', async () => {
  let resolverAntigua
  const antigua = new Promise(r => { resolverAntigua = r })
  const claveNueva = claveRuta(12, true)
  const reciente = { clave: claveNueva, ruta: { idCategoria: 12, permiteCorreo: false } }
  assert.equal(rutaActual(reciente, claveNueva).permiteCorreo, false)
  resolverAntigua({ clave: claveRuta(1, false), ruta: { permiteCorreo: true } })
  assert.equal(rutaActual(await antigua, claveNueva), null)
  assert.equal(rutaActual({ clave: claveNueva, ruta: null }, claveNueva), null)
})

test('fallar la segunda fotografía conserva código y primera evidencia, sin enviar ni recrear', async () => {
  const creado = { codigoSeguimiento: 'NU-000042' }
  const subidas = []
  let guardado, creaciones = 0, envios = 0
  const api = {
    async guardarReporte() { creaciones++; return creado },
    async subirFoto(codigo, foto) { assert.equal(codigo, creado.codigoSeguimiento); if (foto === 'foto2') throw new Error('Falló la foto'); subidas.push(foto) },
    async enviar() { envios++ },
  }
  await assert.rejects(guardarConEvidencias({ datos: {}, fotos: ['foto1', 'foto2'], api, conservar: r => { guardado = r } }), /Falló la foto/)
  assert.equal(guardado, creado)
  assert.deepEqual(subidas, ['foto1'])
  assert.equal(creaciones, 1)
  assert.equal(envios, 0)
  // Recuperación sobre el código existente, aportando solo la evidencia que faltó.
  api.subirFoto = async (codigo, foto) => { assert.equal(codigo, creado.codigoSeguimiento); subidas.push(foto) }
  await guardarConEvidencias({ datos: {}, fotos: ['foto2'], guardado, api, conservar() {} })
  assert.equal(creaciones, 1)
  assert.equal(envios, 1)
  assert.deepEqual(subidas, ['foto1', 'foto2'])
})

test('fallar el encolado conserva el mismo reporte y sus fotografías', async () => {
  let guardado
  const api = {
    async guardarReporte() { return { codigoSeguimiento: 'NU-000043' } },
    async subirFoto() {},
    async enviar() { throw new Error('Sin conexión') },
  }
  await assert.rejects(guardarConEvidencias({ datos: {}, fotos: ['foto'], api, conservar: r => { guardado = r } }), /Sin conexión/)
  assert.equal(guardado.codigoSeguimiento, 'NU-000043')
})
