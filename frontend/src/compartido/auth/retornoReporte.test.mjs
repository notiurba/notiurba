import assert from 'node:assert/strict'
import { beforeEach, test } from 'node:test'
import {
  borrarRetornoReporte, destinoDesdePerfil, destinoTrasAutenticacion, estadoRetornoReporte,
  leerRetornoReporte, prepararRetornoPerfil, prepararRetornoReporte, rutaReportePermitida,
} from './retornoReporte.js'
import { guardarAvance, guardarBorrador, leerBorrador, leerGuardado } from '../../app/reportar/borrador.js'

const almacen = new Map()
const almacenamiento = {
  getItem: (k) => almacen.get(k) ?? null,
  setItem: (k, v) => almacen.set(k, String(v)),
  removeItem: (k) => almacen.delete(k),
}
beforeEach(() => { globalThis.sessionStorage = almacenamiento; almacen.clear() })

test('acepta únicamente los cuatro pasos internos de Crear reporte', () => {
  for (const ruta of ['/reportar', '/reportar/ubicacion', '/reportar/detalles', '/reportar/confirmacion']) {
    assert.equal(rutaReportePermitida(ruta), ruta)
    prepararRetornoReporte(ruta)
    assert.equal(destinoTrasAutenticacion('CIUDADANO', ruta), ruta)
    assert.equal(leerRetornoReporte(), null)
  }
})

test('rechaza destinos externos, administrativos, ambiguos y valores no textuales', () => {
  for (const ruta of ['https://evil.test', '//evil.test', 'javascript:alert(1)', '/admin', '/app',
    '/reportar/../admin', '/reportar/%2e%2e/admin', '/reportar?next=https://evil.test',
    '/reportar#externo', '/reportar\\evil', '/reportar/enviado/123', '', null, {}, 1]) {
    prepararRetornoReporte('/reportar/detalles')
    assert.equal(rutaReportePermitida(ruta), null)
    assert.equal(destinoTrasAutenticacion('CIUDADANO', ruta), '/app')
  }
})

test('login directo elimina un retorno previo y conserva la navegación por rol', () => {
  prepararRetornoReporte('/reportar/confirmacion')
  prepararRetornoReporte(undefined)
  assert.equal(destinoTrasAutenticacion('CIUDADANO'), '/app')
  assert.equal(destinoTrasAutenticacion('ADMIN'), '/admin')
  assert.equal(destinoTrasAutenticacion('DESCONOCIDO'), '/app')
})

test('administrador nunca es enviado al formulario ciudadano y consume la intención', () => {
  prepararRetornoReporte('/reportar')
  assert.equal(destinoTrasAutenticacion('ADMIN', '/reportar'), '/admin')
  assert.equal(leerRetornoReporte(), null)
})

test('Google con perfil completo vuelve al paso solicitado', () => {
  prepararRetornoReporte('/reportar/detalles')
  assert.equal(destinoTrasAutenticacion('CIUDADANO', '/reportar/detalles'), '/reportar/detalles')
})

test('Google incompleto conserva el retorno al pasar por completar perfil y lo consume al volver', () => {
  prepararRetornoReporte('/reportar/confirmacion')
  const estadoGoogle = { tokenRegistro: 'no-se-guarda', correo: 'no-se-guarda', ...estadoRetornoReporte('/reportar/confirmacion') }
  prepararRetornoPerfil(estadoGoogle.siguiente)
  assert.equal(destinoDesdePerfil('CIUDADANO', '/app'), '/reportar/confirmacion')
  assert.equal(destinoDesdePerfil('CIUDADANO', '/app/reportes'), null)
  assert.equal(destinoDesdePerfil('ADMIN', '/app'), null)
  assert.deepEqual(Object.keys(leerRetornoReporte()).sort(), ['creadoEn', 'etapa', 'ruta'])
  borrarRetornoReporte()
  assert.equal(destinoDesdePerfil('CIUDADANO', '/app'), null)
})

test('perfil sin retorno o con retorno inválido sigue a su zona normal', () => {
  prepararRetornoPerfil()
  assert.equal(destinoDesdePerfil('CIUDADANO', '/app'), null)
  prepararRetornoReporte('/reportar')
  prepararRetornoPerfil('//evil.test')
  assert.equal(destinoDesdePerfil('CIUDADANO', '/app'), null)
})

test('registro, espera del correo y verificación en la misma pestaña conservan el destino una sola vez', () => {
  const estadoRegistro = estadoRetornoReporte('/reportar/ubicacion')
  prepararRetornoReporte(estadoRegistro.siguiente)
  const estadoCorreo = { correo: 'ejemplo@test.invalid', ...estadoRetornoReporte(estadoRegistro.siguiente) }
  assert.equal(estadoCorreo.siguiente, '/reportar/ubicacion')
  assert.equal(destinoTrasAutenticacion('CIUDADANO'), '/reportar/ubicacion')
  assert.equal(destinoTrasAutenticacion('CIUDADANO'), '/app')
})

test('verificación en otra pestaña sin intención local usa el destino normal', () => {
  assert.equal(destinoTrasAutenticacion('CIUDADANO'), '/app')
})

test('descarta datos corruptos, caducados o manipulados sin extender su vigencia al completar perfil', () => {
  for (const valor of ['{', 'null', JSON.stringify({ ruta: '/admin', creadoEn: Date.now(), etapa: 'perfil' }),
    JSON.stringify({ ruta: '/reportar', creadoEn: Date.now() - 30 * 60 * 1000, etapa: 'acceso' }),
    JSON.stringify({ ruta: '/reportar', creadoEn: Date.now() + 60000, etapa: 'perfil' })]) {
    almacen.set('notiurba.retornoReporte', valor)
    assert.equal(leerRetornoReporte(), null)
    assert.equal(almacen.has('notiurba.retornoReporte'), false)
  }
  prepararRetornoReporte('/reportar')
  const { creadoEn } = leerRetornoReporte()
  prepararRetornoPerfil('/reportar')
  assert.equal(leerRetornoReporte().creadoEn, creadoEn)
})

test('almacenamiento bloqueado no impide login y permite usar un state válido', () => {
  globalThis.sessionStorage = {
    getItem() { throw new Error('bloqueado') },
    setItem() { throw new Error('bloqueado') },
    removeItem() { throw new Error('bloqueado') },
  }
  prepararRetornoReporte('/reportar')
  assert.equal(destinoTrasAutenticacion('CIUDADANO', '/reportar'), '/reportar')
  assert.equal(destinoTrasAutenticacion('CIUDADANO'), '/app')
})

test('autenticarse, completar perfil o descartar retornos no modifica borrador ni recuperación del reporte', () => {
  guardarBorrador({ idCategoria: 3, descripcion: 'Bache', latitud: 4.08, longitud: -76.19, idBarrio: 7 })
  guardarAvance({ codigo: 'NU-prueba', fotosPendientes: 2 })
  const borrador = leerBorrador()
  const guardado = leerGuardado()
  const datosAntes = [...almacen.entries()]
  prepararRetornoReporte('/reportar/confirmacion')
  prepararRetornoPerfil()
  assert.equal(destinoDesdePerfil('CIUDADANO', '/app'), '/reportar/confirmacion')
  destinoTrasAutenticacion('CIUDADANO')
  prepararRetornoReporte(null)
  assert.deepEqual(leerBorrador(), borrador)
  assert.deepEqual(leerGuardado(), guardado)
  assert.deepEqual([...almacen.entries()], datosAntes)
})
