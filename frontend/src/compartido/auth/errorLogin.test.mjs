import assert from 'node:assert/strict'
import { test } from 'node:test'
import { SITUACION, avisoDeGoogle, avisoDeIngreso, mensajeErrorLogin, situacionIngreso } from './errorLogin.js'
import { TEXTOS_INGRESO, TITULOS_INGRESO, horaColombia, textoBloqueo } from '../textos/ingreso.js'

const PALABRAS_TECNICAS = /servidor|token|\bapi\b|cors|error \d|http|exception|sql|\b[1-5]\d\d\b/i
const CODIGO_EN_MAYUSCULAS = /\b[A-Z]{3,}_[A-Z_]+\b/

const bloqueo = { estado: 429, codigo: 'INTENTOS_EXCEDIDOS', mensaje: 'texto interno',
  datos: { esperarSegundos: 300, bloqueadoHasta: '2026-11-13T20:42:00Z' } }

test('cada situación del ingreso se decide por código y estado', () => {
  assert.equal(situacionIngreso({ estado: 401, codigo: 'CREDENCIALES_INVALIDAS' }), SITUACION.DATOS_INCORRECTOS)
  assert.equal(situacionIngreso(bloqueo), SITUACION.BLOQUEO)
  assert.equal(situacionIngreso({ estado: 403, codigo: 'CORREO_NO_VERIFICADO' }), SITUACION.SIN_CONFIRMAR)
  assert.equal(situacionIngreso({ estado: 0, codigo: 'SIN_CONEXION' }), SITUACION.SIN_INTERNET)
  assert.equal(situacionIngreso({ estado: 503, codigo: 'ERROR_INTERNO' }), SITUACION.SERVICIO_CAIDO)
  assert.equal(situacionIngreso({ estado: 200, codigo: 'RESPUESTA_INESPERADA' }), SITUACION.SERVICIO_CAIDO)
  assert.equal(situacionIngreso({ estado: 418, codigo: 'CODIGO_NUEVO' }), SITUACION.GENERICO)
  assert.equal(situacionIngreso(), SITUACION.GENERICO)
})

test('un 401 o 403 con otro código no se confunde con datos incorrectos ni con correo sin confirmar', () => {
  assert.equal(situacionIngreso({ estado: 401, codigo: 'NO_AUTENTICADO' }), SITUACION.GENERICO)
  assert.equal(situacionIngreso({ estado: 403, codigo: 'SIN_PERMISO' }), SITUACION.GENERICO)
  assert.equal(situacionIngreso({ estado: 429, codigo: 'OTRO' }), SITUACION.GENERICO)
})

test('un fallo del servicio o un código desconocido nunca muestran el texto que trae el servicio', () => {
  const casos = [
    { estado: 500, codigo: 'ERROR_INTERNO', mensaje: 'SQL error: relation "usuario" does not exist' },
    { estado: 502, codigo: 'RESPUESTA_INESPERADA', mensaje: 'Bad gateway nginx/1.2' },
    { estado: 418, codigo: 'RARO', mensaje: 'java.lang.NullPointerException' },
    { estado: 200, mensaje: 'dato técnico privado' },
  ]
  for (const caso of casos) {
    const aviso = avisoDeIngreso(caso)
    assert.doesNotMatch(aviso.mensaje, /SQL|relation|nginx|java|privado|gateway/i)
    assert.doesNotMatch(avisoDeGoogle(caso).mensaje, /SQL|relation|nginx|java|privado|gateway/i)
  }
})

test('cada situación tiene un mensaje distinto', () => {
  const errores = [
    { estado: 401, codigo: 'CREDENCIALES_INVALIDAS' }, bloqueo, { estado: 403, codigo: 'CORREO_NO_VERIFICADO' },
    { estado: 0, codigo: 'SIN_CONEXION' }, { estado: 500, codigo: 'ERROR_INTERNO' }, { estado: 418, codigo: 'X' },
  ]
  const mensajes = errores.map(mensajeErrorLogin)
  assert.equal(new Set(mensajes).size, mensajes.length)
})

test('datos incorrectos avisa cuando queda un solo intento y no antes', () => {
  const base = { estado: 401, codigo: 'CREDENCIALES_INVALIDAS' }
  assert.equal(avisoDeIngreso({ ...base, datos: { intentosRestantes: 2 } }).mensaje, TEXTOS_INGRESO.datosIncorrectos)
  assert.equal(avisoDeIngreso(base).mensaje, TEXTOS_INGRESO.datosIncorrectos)
  const ultimo = avisoDeIngreso({ ...base, datos: { intentosRestantes: 1 } }).mensaje
  assert.match(ultimo, /Le queda 1 intento/)
  assert.match(ultimo, /no coinciden/)
})

test('el mensaje de datos incorrectos no dice cuál dato falló', () => {
  assert.doesNotMatch(TEXTOS_INGRESO.datosIncorrectos, /no existe|no está registrad|contraseña incorrecta|correo incorrecto/i)
  assert.match(TEXTOS_INGRESO.datosIncorrectos, /Continuar con Google/)
})

test('el bloqueo dice los minutos y la hora de Colombia', () => {
  const aviso = avisoDeIngreso(bloqueo)
  assert.equal(aviso.situacion, SITUACION.BLOQUEO)
  assert.match(aviso.mensaje, /durante 5 minutos/)
  assert.match(aviso.mensaje, /a las 3:42 p\. m\.$/)
})

test('el bloqueo sube al minuto entero y usa el singular', () => {
  assert.match(textoBloqueo(61, '2026-11-13T20:42:00Z'), /durante 2 minutos/)
  assert.match(textoBloqueo(60, '2026-11-13T20:42:00Z'), /durante 1 minuto /)
  assert.match(textoBloqueo(1, '2026-11-13T20:42:00Z'), /durante 1 minuto /)
})

test('el bloqueo sin datos o con datos dañados no promete una hora', () => {
  for (const mensaje of [textoBloqueo(undefined, undefined), textoBloqueo(300, 'no es fecha'), textoBloqueo(-5, '2026-11-13T20:42:00Z')]) {
    assert.equal(mensaje, TEXTOS_INGRESO.bloqueoSinDatos)
  }
  assert.equal(avisoDeIngreso({ estado: 429, codigo: 'INTENTOS_EXCEDIDOS' }).mensaje, TEXTOS_INGRESO.bloqueoSinDatos)
})

test('la hora usa 12 horas con a. m. y p. m. en hora de Colombia', () => {
  assert.equal(horaColombia(new Date('2026-11-13T05:00:00Z')), '12:00 a. m.')
  assert.equal(horaColombia(new Date('2026-11-13T17:05:00Z')), '12:05 p. m.')
  assert.equal(horaColombia(new Date('2026-11-13T14:09:00Z')), '9:09 a. m.')
  assert.equal(horaColombia(new Date('2026-11-14T04:59:00Z')), '11:59 p. m.')
})

test('Google: sin red o con el servicio caído dice lo mismo que el ingreso; lo demás, el mensaje de Google', () => {
  assert.equal(avisoDeGoogle({ estado: 0, codigo: 'SIN_CONEXION' }).mensaje, TEXTOS_INGRESO.sinInternet)
  assert.equal(avisoDeGoogle({ estado: 500, codigo: 'ERROR_INTERNO' }).mensaje, TEXTOS_INGRESO.servicioCaido)
  assert.equal(avisoDeGoogle({ estado: 401, codigo: 'NO_AUTENTICADO' }).mensaje, TEXTOS_INGRESO.google)
  assert.equal(avisoDeGoogle({ estado: 400, codigo: 'VALIDACION' }).mensaje, TEXTOS_INGRESO.google)
  assert.equal(avisoDeGoogle({ estado: 429, codigo: 'INTENTOS_EXCEDIDOS' }).situacion, SITUACION.BLOQUEO)
})

test('ningún texto del ingreso usa palabras técnicas, números de estado ni códigos (RN-24)', () => {
  const textos = [
    ...Object.values(TEXTOS_INGRESO), ...Object.values(TITULOS_INGRESO), textoBloqueo(300, '2026-11-13T20:42:00Z'),
    textoBloqueo(61, '2026-11-13T20:42:00Z'),
  ]
  for (const texto of textos) {
    const sinHora = texto.replace(/\d{1,2}:\d{2} [ap]\. m\./g, '').replace(/\b\d+ minutos?\b/g, '').replace(/\b1 intento\b/g, '')
    assert.doesNotMatch(sinHora, PALABRAS_TECNICAS, texto)
    assert.doesNotMatch(texto, CODIGO_EN_MAYUSCULAS, texto)
  }
})

test('la cuenta regresiva se muestra en minutos y segundos y nunca baja de cero', async () => {
  const { formatoCuenta } = await import('../textos/ingreso.js')
  assert.equal(formatoCuenta(272), '4:32')
  assert.equal(formatoCuenta(59.2), '1:00')
  assert.equal(formatoCuenta(0.1), '0:01')
  assert.equal(formatoCuenta(-3), '0:00')
})

test('el fin del bloqueo se cuenta con los segundos de espera, no con el reloj del celular', async () => {
  const { finDelBloqueo } = await import('../textos/ingreso.js')
  const ahora = Date.parse('2026-11-13T20:40:00Z')
  // Aunque la hora del servicio diga otra cosa (reloj del equipo corrido), cuentan los segundos.
  assert.equal(finDelBloqueo({ bloqueadoHasta: '2026-11-13T20:50:00Z', esperarSegundos: 300 }, ahora), ahora + 300000)
  assert.equal(finDelBloqueo({ esperarSegundos: 30 }, ahora), ahora + 30000)
  assert.equal(finDelBloqueo({ bloqueadoHasta: '2026-11-13T20:42:00Z' }, ahora), null)
  assert.equal(finDelBloqueo({ esperarSegundos: 0 }, ahora), null)
  assert.equal(finDelBloqueo(null, ahora), null)
})
