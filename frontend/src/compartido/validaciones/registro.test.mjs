import assert from 'node:assert/strict'
import { test } from 'node:test'
import { armarDatosPerfil, armarSolicitudRegistro, validarPasoAcceso, validarPasoDatos } from './registro.js'

const completo = {
  nombres: ' Ana María ',
  apellidos: 'Pérez Gómez',
  idTipoDocumento: '1',
  numeroDocumento: '1112345678 ',
  telefono: '3001234567',
  idBarrio: '5',
  direccionCorrespondencia: ' Calle 25 # 30-15 ',
  correo: 'ana@correo.com',
  password: 'Clave1234',
  confirmar: 'Clave1234',
  aceptaPrivacidad: true,
}

test('paso 1: con todo lleno no hay errores', () => {
  assert.deepEqual(validarPasoDatos(completo), {})
})

test('paso 1 permite avanzar sin barrio, dirección ni credenciales del paso 2', () => {
  const datos = { ...completo, idBarrio: '', direccionCorrespondencia: '', correo: '', password: '', confirmar: '' }
  assert.deepEqual(validarPasoDatos(datos), {})
  assert.ok(validarPasoAcceso(datos).idBarrio)
})

test('nombres y apellidos: se conserva el límite 60 y se rechazan 61 caracteres', () => {
  assert.deepEqual(validarPasoDatos({ ...completo, nombres: 'A'.repeat(60), apellidos: 'B'.repeat(60) }), {})
  const errores = validarPasoDatos({ ...completo, nombres: 'A'.repeat(61), apellidos: 'B'.repeat(61) })
  assert.match(errores.nombres, /60/)
  assert.match(errores.apellidos, /60/)
})

test('paso 1: todo vacío marca cada campo obligatorio, menos el teléfono opcional', () => {
  const errores = validarPasoDatos({ nombres: '', apellidos: '', idTipoDocumento: '', numeroDocumento: '', telefono: '' })
  assert.deepEqual(Object.keys(errores).sort(), ['apellidos', 'idTipoDocumento', 'nombres', 'numeroDocumento'])
})

test('paso 1: teléfono de 5 dígitos da el error del prototipo', () => {
  const errores = validarPasoDatos({ ...completo, telefono: '30012' })
  assert.equal(errores.telefono, 'Escriba 10 dígitos, sin espacios.')
})

test('paso 2: con todo lleno no hay errores', () => {
  assert.deepEqual(validarPasoAcceso(completo), {})
})

test('paso 2: sin confirmar mayoría de edad y aceptar la Ley 1581 no se puede crear la cuenta', () => {
  const errores = validarPasoAcceso({ ...completo, aceptaPrivacidad: false })
  assert.deepEqual(Object.keys(errores), ['aceptaPrivacidad'])
})

test('paso 2: contraseñas distintas y débil', () => {
  assert.ok(validarPasoAcceso({ ...completo, confirmar: 'otra' }).confirmar)
  assert.ok(validarPasoAcceso({ ...completo, password: 'corta', confirmar: 'corta' }).password)
})

test('solicitud: limpia espacios, ids numéricos y sin confirmación', () => {
  const s = armarSolicitudRegistro(completo)
  assert.equal(s.nombres, 'Ana María')
  assert.equal(s.numeroDocumento, '1112345678')
  assert.equal(s.direccionCorrespondencia, 'Calle 25 # 30-15')
  assert.equal(s.idTipoDocumento, 1)
  assert.equal(s.idBarrio, 5)
  assert.equal('confirmar' in s, false)
  assert.equal(s.telefono, '3001234567')
  assert.equal(s.aceptaPrivacidad, true)
  assert.equal(s.declaraMayorEdad, true) // el contrato exige las dos (misma casilla)
})

test('solicitud: el teléfono vacío no se envía', () => {
  assert.equal('telefono' in armarSolicitudRegistro({ ...completo, telefono: '' }), false)
})

test('armarDatosPerfil: el teléfono vacío se omite, salvo al editar (para poder borrarlo)', () => {
  const sinTelefono = { ...completo, telefono: '' }
  assert.equal('telefono' in armarDatosPerfil(sinTelefono), false)
  assert.equal(armarDatosPerfil(sinTelefono, { enviarTelefonoVacio: true }).telefono, '')
  assert.equal(armarDatosPerfil(completo).nombres, 'Ana María')
  assert.equal(armarDatosPerfil(completo).idBarrio, 5)
})
