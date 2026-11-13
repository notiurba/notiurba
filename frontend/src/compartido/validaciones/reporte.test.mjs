import assert from 'node:assert/strict'
import test from 'node:test'
import { armarSolicitudReporte, armarVistaPrevia, validarDetalles, validarFoto, validarUbicacion, validarConfirmacionReporte } from './reporte.js'

test('validarUbicacion pide el punto', () => {
  assert.ok(validarUbicacion({ latitud: null, longitud: null, idBarrio: '' }).punto)
})

test('validarUbicacion no exige el barrio si hay punto', () => {
  assert.deepEqual(validarUbicacion({ latitud: 4.08, longitud: -76.19, idBarrio: '' }), {})
  assert.deepEqual(validarUbicacion({ latitud: 4.08, longitud: -76.19, idBarrio: '5' }), {})
})

test('validarDetalles exige 10 caracteres (sin contar espacios de los lados)', () => {
  assert.ok(validarDetalles({ descripcion: '  corto   ' }).descripcion)
  assert.deepEqual(validarDetalles({ descripcion: 'Hueco grande frente a la tienda' }), {})
})

test('validarDetalles rechaza más de 500 caracteres', () => {
  assert.ok(validarDetalles({ descripcion: 'a'.repeat(501) }).descripcion)
})

test('validarFoto acepta JPG y PNG, sin importar el peso (se reducen después)', () => {
  assert.equal(validarFoto({ type: 'image/png', size: 500_000 }, 0), '')
  assert.equal(validarFoto({ type: 'image/jpeg', size: 1024 * 1024 }, 2), '')
})

test('validarFoto rechaza otros tipos y la cuarta foto', () => {
  assert.ok(validarFoto({ type: 'image/gif', size: 100 }, 0))
  assert.equal(validarFoto({ type: 'image/png', size: 5 * 1024 * 1024 }, 0), '')
  assert.ok(validarFoto({ type: 'image/png', size: 100 }, 3))
})

test('armarSolicitudReporte convierte ids a número y omite lo vacío', () => {
  const solicitud = armarSolicitudReporte(
    { idCategoria: '1', descripcion: ' Hueco grande ', latitud: 4.1, longitud: -76.2, idBarrio: '5', direccion: '', matricula: '' },
    { autorizaEnvio: true },
  )
  assert.equal(solicitud.idCategoria, 1)
  assert.equal(solicitud.idBarrio, 5)
  assert.equal(solicitud.descripcion, 'Hueco grande')
  assert.equal(solicitud.autorizaEnvio, true)
  assert.ok(!('cantidadFotos' in solicitud))
  assert.ok(!('direccionIncidente' in solicitud))
  assert.ok(!('matriculaCuentaServicio' in solicitud))
})

test('armarSolicitudReporte manda las fotos declaradas y la confirmación del parecido solo si se piden', () => {
  const b = { idCategoria: '1', descripcion: 'Hueco grande', latitud: 4.1, longitud: -76.2, idBarrio: '', direccion: '', matricula: '' }
  const simple = armarSolicitudReporte(b, { autorizaEnvio: true })
  assert.ok(!('totalFotos' in simple))
  assert.ok(!('confirmarDuplicado' in simple))
  const completa = armarSolicitudReporte(b, { autorizaEnvio: true, totalFotos: 2, confirmarDuplicado: true })
  assert.equal(completa.totalFotos, 2)
  assert.equal(completa.confirmarDuplicado, true)
  assert.equal(armarSolicitudReporte(b, { totalFotos: 0 }).totalFotos, 0)
})

test('ubicación admite barrio sin GPS y omisión cuando la categoría no lo requiere', () => {
  assert.deepEqual(validarUbicacion({ latitud: null, longitud: null, idBarrio: '5' }), {})
  assert.deepEqual(validarUbicacion({ latitud: null, longitud: null, idBarrio: '' }, { requiereUbicacion: false }), {})
  assert.ok(validarUbicacion({ latitud: 4.1, longitud: null, idBarrio: '5' }).punto)
  assert.ok(validarUbicacion({ latitud: 91, longitud: -76, idBarrio: '' }).punto)
})

test('ni el consentimiento de registro ni el constructor infieren autorización de envío', () => {
  const b = { idCategoria: '1', descripcion: 'Hueco frente al parque', latitud: null, longitud: null, idBarrio: '5', direccion: '', matricula: '', aceptaPrivacidad: true }
  const categoria = { idCategoria: 1, requiereUbicacion: true }
  assert.equal(armarSolicitudReporte(b).autorizaEnvio, false)
  assert.ok(validarConfirmacionReporte(b, categoria, false).autorizaEnvio)
  assert.deepEqual(validarConfirmacionReporte(b, categoria, true), {})
  assert.ok(validarConfirmacionReporte(b, { idCategoria: 2 }, true).idCategoria)
})

test('el payload omite ubicación y matrícula residuales según la ruta y comunica emergencia', () => {
  const b = { idCategoria: '6', descripcion: 'Construcción sin licencia', latitud: 4.1, longitud: -76.2, idBarrio: '5', direccion: 'Calle 10', matricula: '123' }
  const solicitud = armarSolicitudReporte(b, { autorizaEnvio: true, emergencia: true, categoria: { requiereUbicacion: false, admiteMatricula: false } })
  assert.equal(solicitud.emergencia, true)
  for (const campo of ['latitud', 'longitud', 'idBarrio', 'direccionIncidente', 'matriculaCuentaServicio']) assert.equal(campo in solicitud, false)
})

test('armarSolicitudReporte omite punto y barrio cuando no los hay', () => {
  const solicitud = armarSolicitudReporte(
    { idCategoria: '6', descripcion: 'Cobro excesivo en la factura', latitud: null, longitud: null, idBarrio: '', direccion: '', matricula: '123' },
  )
  assert.ok(!('latitud' in solicitud))
  assert.ok(!('idBarrio' in solicitud))
  assert.equal(solicitud.matriculaCuentaServicio, '123')
})

test('la vista previa lleva los mismos datos que la creación, cuántas fotos van y nada de autorización', () => {
  const borrador = { idCategoria: '1', descripcion: '  Hay un hueco grande  ', latitud: 4.0867, longitud: -76.195,
    idBarrio: '7', direccion: 'Cra 5 # 10-20', matricula: '', emergencia: false }
  const previa = armarVistaPrevia(borrador, { emergencia: false, categoria: { requiereUbicacion: true, admiteMatricula: false }, totalFotos: 2 })
  assert.deepEqual(previa, { idCategoria: 1, descripcion: 'Hay un hueco grande', emergencia: false, latitud: 4.0867,
    longitud: -76.195, idBarrio: 7, direccionIncidente: 'Cra 5 # 10-20', totalFotos: 2 })
  assert.equal('autorizaEnvio' in previa, false)
  assert.equal(armarVistaPrevia(borrador, { categoria: { requiereUbicacion: false } }).totalFotos, 0)
})
