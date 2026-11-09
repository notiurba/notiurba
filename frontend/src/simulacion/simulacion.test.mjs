import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createServer } from 'vite'

test('el flujo simulado conserva las cuentas y solo consulta el catálogo público de barrios', async () => {
  const server = await createServer({
    server: { middlewareMode: true, ws: false }, appType: 'custom',
    define: { 'import.meta.env.VITE_USAR_MOCKS': '"true"' },
  })
  const originalFetch = globalThis.fetch
  const originalStorage = globalThis.localStorage
  const memoria = new Map()
  globalThis.localStorage = { getItem: k => memoria.get(k) ?? null,
    setItem: (k, v) => memoria.set(k, String(v)), removeItem: k => memoria.delete(k) }
  let llamadasRed = 0
  globalThis.fetch = async url => {
    llamadasRed++
    assert.ok(url.endsWith('/api/catalogos/barrios'))
    // Doble HTTP exclusivo de esta prueba; no alimenta el selector de la aplicación.
    return new Response(JSON.stringify([{ idBarrio: 2, idComuna: 2, nombre: 'Franciscanos' }]), { status: 200 })
  }
  try {
    const { peticion } = await server.ssrLoadModule('/src/compartido/api/cliente.js')
    const post = (ruta, cuerpo) => peticion(ruta, { method: 'POST', body: JSON.stringify(cuerpo) })
    const { iniciarSesion } = await server.ssrLoadModule('/src/compartido/api/auth.js')
    const { estadoSesion, obtenerRol } = await server.ssrLoadModule('/src/compartido/auth/sesion.js')
    const { guardarToken } = await server.ssrLoadModule('/src/compartido/auth/sesion.js')
    const sesion = await iniciarSesion('ciudadano@correo.com', 'Clave1234')
    guardarToken(sesion.token)
    assert.equal(estadoSesion(), 'activa')
    assert.equal(obtenerRol(), 'CIUDADANO')
    await assert.rejects(() => iniciarSesion('ciudadano@correo.com', 'incorrecta'), e => e.estado === 401)
    const categorias = await peticion('/api/enrutamiento/categorias')
    const barrios = await peticion('/api/catalogos/barrios')
    assert.equal(categorias.length, 14)
    assert.ok(categorias.every(c => c.categoria && c.codigo))
    assert.ok(barrios.length > 0)
    const ruta = await peticion('/api/enrutamiento/categorias/1?emergencia=false')
    assert.equal(ruta.permiteCorreo, true)
    assert.match(ruta.mensaje, /simulada/)
    assert.equal((await peticion('/api/enrutamiento/categorias/12?emergencia=true')).permiteCorreo, false)
    const validacion = await server.ssrLoadModule('/src/compartido/validaciones/reporte.js')
    assert.match(validacion.validarUbicacion({}).punto, /ubicación/)
    assert.match(validacion.validarDetalles({ descripcion: '' }).descripcion, /descripción/)
    assert.match(validacion.validarDetalles({ descripcion: 'corta' }).descripcion, /10/)
    const datos = { idCategoria: 1, idBarrio: barrios[0].idBarrio, descripcion: 'Hueco de prueba local frente al parque.', autorizaEnvio: true }
    await assert.rejects(() => post('/api/reportes', { ...datos, autorizaEnvio: false }), e => e.estado === 400)
    const creado = await post('/api/reportes', datos)
    const url = `/api/reportes/${creado.codigoSeguimiento}`
    assert.equal((await peticion(url + '/envio')).estado, 'SIN_SOLICITAR')
    for (let i = 0; i < 3; i++) await peticion(url + '/fotos', { method: 'POST', body: new FormData() })
    await assert.rejects(() => peticion(url + '/fotos', { method: 'POST', body: new FormData() }), e => e.codigo === 'FOTOS_MAXIMO')
    assert.equal((await post(url + '/enviar')).estado, 'PENDIENTE')
    const reporte = await peticion(url)
    assert.equal(reporte.categoria.nombre, categorias[0].categoria)
    assert.equal(reporte.barrio.nombre, barrios[0].nombre)
    assert.equal(reporte.fotos.length, 3)
    assert.equal((await peticion(url + '/envio')).estado, 'PENDIENTE')
    const emergencia = await post('/api/reportes', { ...datos, idCategoria: 12, emergencia: true })
    assert.equal((await post(`/api/reportes/${emergencia.codigoSeguimiento}/enviar`)).estado, 'NO_ENVIADO_CORREO')
    await iniciarSesion('nuevo@correo.com', 'Clave1234')
    assert.deepEqual(await peticion('/api/reportes'), [])
    await assert.rejects(() => peticion('/api/no-simulado'), e => e.codigo === 'MOCK_NO_DISPONIBLE')
    assert.equal(llamadasRed, 1)
  } finally {
    globalThis.fetch = originalFetch
    if (originalStorage === undefined) delete globalThis.localStorage
    else globalThis.localStorage = originalStorage
    await server.close()
  }
})
