import test from 'node:test'
import assert from 'node:assert/strict'
import { presentacionCorreo, enlaceCanal, nombreCanal, accionCanal, ofrecerOtrosCanales, resumenCorreoDetalle, textoParaCopiar, copiarMensaje } from './estadoCorreo.js'

test('guardar un reporte no equivale a correo enviado', () => {
  assert.equal(presentacionCorreo(null).etiqueta, 'Sin enviar')
  assert.equal(presentacionCorreo({ estado: 'PENDIENTE' }).titulo, 'Reporte guardado')
  assert.equal(presentacionCorreo({ estado: 'PENDIENTE' }).etiqueta, 'En camino')
})
test('solo cuando el correo salió se muestra «enviado» y no se promete lectura', () => {
  const e = presentacionCorreo({ estado: 'ENVIADO' })
  assert.equal(e.titulo, 'Reporte enviado')
  assert.match(e.mensaje, /Enviamos su reporte/)
  assert.doesNotMatch(e.mensaje, /Alcaldía recibió|leyó|radicado/)
})
test('fallo inicial conserva el reporte; rebote tiene aviso propio sin retroceso', () => {
  assert.match(presentacionCorreo({ estado: 'FALLIDO' }).mensaje, /quedó guardado/)
  assert.equal(presentacionCorreo({ estado: 'FALLIDO', rebote: true }).clase, 'rebote')
  assert.match(presentacionCorreo({ estado: 'FALLIDO', rebote: true }).mensaje, /sigue guardado/)
})
test('canal externo y gas 164 no simulan envío exitoso', () => {
  const ruta = { canal: 'Teléfono', acceso: '164', mensaje: 'Emergencia de gas: llame al 164' }
  assert.equal(enlaceCanal(ruta), 'tel:164')
  assert.equal(presentacionCorreo({ estado: 'NO_ENVIADO_CORREO', ruta }).etiqueta, 'Otro canal')
  assert.equal(presentacionCorreo({ estado: 'NO_ENVIADO_CORREO' }).titulo, 'Reporte guardado')
})
test('los enlaces de canales rechazan scripts, HTTP y credenciales', () => {
  for (const acceso of ['javascript:alert(1)', 'http://celsia.com', 'https://clave@celsia.com']) assert.equal(enlaceCanal({ acceso }), null)
  assert.equal(enlaceCanal({ acceso: 'https://www.celsia.com/es/zona-clientes/' }), 'https://www.celsia.com/es/zona-clientes/')
})
test('cada canal tiene su acción directa: WhatsApp, teléfono, web; la oficina no tiene enlace', () => {
  assert.equal(enlaceCanal({ tipo: 'WhatsApp', acceso: '317 372 5078' }), 'https://wa.me/573173725078')
  assert.equal(enlaceCanal({ tipo: 'WhatsApp', acceso: '+57 310 390 3468' }), 'https://wa.me/573103903468')
  assert.equal(enlaceCanal({ tipo: 'Teléfono', acceso: '(602) 233 9300' }), 'tel:6022339300')
  assert.equal(enlaceCanal({ tipo: 'Teléfono', acceso: '01 8000 950 096' }), 'tel:018000950096')
  assert.equal(enlaceCanal({ tipo: 'Web', acceso: 'https://www.gdo.com.co/Paginas/Registro-de-pqrs.aspx' }),
    'https://www.gdo.com.co/Paginas/Registro-de-pqrs.aspx')
  assert.equal(enlaceCanal({ tipo: 'Presencial', acceso: 'Carrera 25 #25-04, Tuluá' }), null)
})
test('un WhatsApp o teléfono raro no se convierte en enlace', () => {
  for (const acceso of ['123', '317 372 50781', 'javascript:alert(1)', '+1 202 555 0100']) assert.equal(enlaceCanal({ tipo: 'WhatsApp', acceso }), null)
  assert.equal(enlaceCanal({ tipo: 'Teléfono', acceso: 'llamar ya' }), null)
})
test('los canales se nombran en lenguaje claro y su acción dice qué pasa', () => {
  assert.equal(nombreCanal({ tipo: 'WhatsApp' }), 'WhatsApp')
  assert.equal(nombreCanal({ tipo: 'Teléfono', acceso: '01 8000 950 096' }), 'Línea gratuita')
  assert.equal(nombreCanal({ tipo: 'Teléfono', acceso: '164', finalidad: 'Emergencia' }), 'Línea de emergencias')
  assert.equal(nombreCanal({ tipo: 'Web', finalidad: 'PQR anónimo' }), 'Formulario sin sus datos')
  assert.equal(nombreCanal({ tipo: 'Web', finalidad: 'Radicar PQR' }), 'Formulario web')
  assert.equal(nombreCanal({ tipo: 'Presencial' }), 'Oficina en Tuluá')
  assert.equal(accionCanal({ tipo: 'WhatsApp', acceso: '317 372 5078' }), 'Abrir WhatsApp')
  assert.equal(accionCanal({ tipo: 'Teléfono', acceso: '164' }), 'Llamar al 164')
  assert.equal(accionCanal({ tipo: 'Web', acceso: 'https://www.celsia.com/' }), 'Abrir la página')
  assert.equal(accionCanal({ tipo: 'Presencial', acceso: 'Calle 25' }), null)
})
test('solo con rebote o fallo se ofrecen los otros canales y «Copiar mi mensaje»', () => {
  assert.equal(ofrecerOtrosCanales({ estado: 'FALLIDO', rebote: true }), true)
  assert.equal(ofrecerOtrosCanales({ estado: 'FALLIDO' }), true)
  for (const estado of ['PENDIENTE', 'ENVIADO', 'NO_ENVIADO_CORREO', 'SIN_SOLICITAR']) assert.equal(ofrecerOtrosCanales({ estado }), false)
  assert.equal(ofrecerOtrosCanales(null), false)
})
test('el mensaje copiado lleva el asunto y el texto, listo para pegar', () => {
  assert.equal(textoParaCopiar({ asunto: 'Petición ciudadana – Vías', texto: 'Respetados señores:\n\nYo, Ana…' }),
    'Asunto: Petición ciudadana – Vías\n\nRespetados señores:\n\nYo, Ana…')
})
test('copiar mi mensaje: con ClipboardItem, con writeText y, si falla, deja el texto para copiarlo a mano', async () => {
  const obtener = async () => ({ asunto: 'A', texto: 'T' })
  class Item { constructor(datos) { this.datos = datos } }
  let copiado = ''
  const conItem = { write: async ([item]) => { copiado = await (await item.datos['text/plain']).text() } }
  assert.deepEqual(await copiarMensaje({ obtener, portapapeles: conItem, Item }), { copiado: true, texto: 'Asunto: A\n\nT' })
  assert.equal(copiado, 'Asunto: A\n\nT')
  const soloTexto = { writeText: async (t) => { copiado = t + '!' } }
  assert.equal((await copiarMensaje({ obtener, portapapeles: soloTexto })).copiado, true)
  assert.equal(copiado, 'Asunto: A\n\nT!')
  const negado = { writeText: async () => { throw new Error('sin permiso') } }
  assert.deepEqual(await copiarMensaje({ obtener, portapapeles: negado }), { copiado: false, texto: 'Asunto: A\n\nT' })
  const sinMensaje = async () => { throw new Error('red') }
  assert.deepEqual(await copiarMensaje({ obtener: sinMensaje, portapapeles: conItem, Item }), { copiado: false, texto: '' })
})
test('nombre y acción también entienden la ruta antigua ({ canal })', () => {
  assert.equal(accionCanal({ canal: 'Teléfono', acceso: '164' }), 'Llamar al 164')
  assert.equal(nombreCanal({ canal: 'Teléfono', acceso: '164' }), 'Teléfono')
})

test('el detalle no muestra bloque de correo si no hay dato del envío', () => {
  assert.equal(resumenCorreoDetalle(null), null)
  assert.equal(resumenCorreoDetalle({}), null)
})
test('el detalle con el correo en camino o enviado no ofrece otros canales ni habla de fallo', () => {
  for (const estado of ['PENDIENTE', 'ENVIADO']) {
    const r = resumenCorreoDetalle({ estado })
    assert.equal(r.otrosCanales, false)
    assert.doesNotMatch(r.mensaje, /no (llegó|salió|pudo)/i)
  }
  assert.equal(resumenCorreoDetalle({ estado: 'ENVIADO' }).etiqueta, 'Enviado')
})
test('el detalle con rebote dice que no llegó y ofrece otros canales', () => {
  const r = resumenCorreoDetalle({ estado: 'FALLIDO', rebote: true, reintentable: false })
  assert.equal(r.etiqueta, 'No entregado')
  assert.equal(r.subtitulo, 'El correo no llegó a la entidad')
  assert.equal(r.otrosCanales, true)
})
test('el detalle con un envío fallido sin rebote ofrece otros canales y no manda a reintentar', () => {
  const r = resumenCorreoDetalle({ estado: 'FALLIDO', rebote: false, reintentable: false })
  assert.equal(r.otrosCanales, true)
  assert.doesNotMatch(r.mensaje, /intentarlo de nuevo/i)
  assert.match(r.mensaje, /no salió a la entidad/)
})
test('los textos del detalle no usan palabras técnicas', () => {
  for (const envio of [{ estado: 'PENDIENTE' }, { estado: 'ENVIADO' }, { estado: 'FALLIDO' }, { estado: 'FALLIDO', rebote: true }]) {
    const r = resumenCorreoDetalle(envio)
    assert.doesNotMatch(`${r.etiqueta} ${r.subtitulo} ${r.mensaje}`, /servidor|token|\bapi\b|rebote|webhook|resend|FALLIDO|\b[45]\d\d\b/i)
  }
})

test('el detalle de un reporte que sigue por otro canal muestra los canales y no habla de correo', () => {
  const r = resumenCorreoDetalle({ estado: 'NO_ENVIADO_CORREO', ruta: { mensaje: 'Use el canal oficial.' } })
  assert.equal(r.otrosCanales, true)
  assert.equal(r.reintentar, false)
  assert.equal(r.titulo, 'Canal de atención')
  assert.equal(resumenCorreoDetalle({ estado: 'ENVIADO' }).titulo, 'Correo a la entidad')
})
test('el detalle ofrece reintentar si el fallo pudo ser pasajero o si nunca se pidió el envío', () => {
  const pasajero = resumenCorreoDetalle({ estado: 'FALLIDO', rebote: false, reintentable: true })
  assert.equal(pasajero.reintentar, true)
  assert.equal(pasajero.otrosCanales, false)
  assert.match(pasajero.mensaje, /intentarlo de nuevo/)
  assert.equal(resumenCorreoDetalle({ estado: 'SIN_SOLICITAR' }).reintentar, true)
  for (const e of [{ estado: 'ENVIADO' }, { estado: 'PENDIENTE' }, { estado: 'FALLIDO', rebote: true, reintentable: true }, { estado: 'FALLIDO', reintentable: false }])
    assert.equal(resumenCorreoDetalle(e).reintentar, false)
})
test('el detalle no manda a «Mis reportes» cuando ya está en el reporte', () => {
  assert.doesNotMatch(resumenCorreoDetalle({ estado: 'ENVIADO' }).mensaje, /Mis reportes/)
})
