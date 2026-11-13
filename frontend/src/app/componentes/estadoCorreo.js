// Textos que ve la persona después de enviar el reporte. Se habla de «usted», sin nombres de proveedor ni códigos técnicos.
export function presentacionCorreo(envio) {
  if (envio?.rebote) return { clase: 'rebote', titulo: 'El correo no llegó', etiqueta: 'No entregado', subtitulo: 'El correo no llegó a la entidad', mensaje: 'Su reporte sigue guardado y conserva su seguimiento. El correo no pudo entregarse a la entidad.' }
  if (envio?.estado === 'ENVIADO') return { clase: 'enviado', titulo: 'Reporte enviado', etiqueta: 'Enviado', subtitulo: 'Su reporte ya salió hacia la entidad', mensaje: 'Enviamos su reporte a la entidad. Puede seguir el avance desde Mis reportes.' }
  if (envio?.estado === 'FALLIDO') return { clase: 'fallido', titulo: 'Reporte guardado', etiqueta: 'No enviado', subtitulo: 'No pudimos enviar el correo', mensaje: 'Su reporte quedó guardado, pero el correo no salió. Puede intentarlo de nuevo.' }
  if (envio?.estado === 'NO_ENVIADO_CORREO') return { clase: 'externo', titulo: 'Reporte guardado', etiqueta: 'Otro canal', subtitulo: 'Siga por el canal oficial', mensaje: envio.ruta?.mensaje ?? 'Su reporte quedó guardado. Esta entidad atiende por otro canal: use el que se indica abajo.' }
  if (envio?.estado === 'PENDIENTE') return { clase: 'pendiente', titulo: 'Reporte guardado', etiqueta: 'En camino', subtitulo: 'Estamos enviando su reporte', mensaje: 'Su reporte quedó guardado y lo estamos enviando a la entidad.' }
  return { clase: 'pendiente', titulo: 'Reporte guardado', etiqueta: 'Sin enviar', subtitulo: 'Falta enviarlo', mensaje: 'Su reporte quedó guardado. Falta enviarlo a la entidad.' }
}

// Acepta un canal de la lista ({ tipo, acceso }) o la ruta antigua ({ canal, acceso }).
const tipoCanal = (canal) => canal?.tipo ?? canal?.canal

export function enlaceCanal(canal) {
  const tipo = tipoCanal(canal)
  const acceso = String(canal?.acceso ?? '')
  if (tipo === 'WhatsApp') {
    if (!/^[+\d ()-]+$/.test(acceso)) return null
    const digitos = acceso.replace(/\D/g, '')
    const numero = /^3\d{9}$/.test(digitos) ? '57' + digitos : /^573\d{9}$/.test(digitos) ? digitos : null
    return numero ? `https://wa.me/${numero}` : null
  }
  if (tipo === 'Teléfono') return /^[+\d ()-]+$/.test(acceso) ? `tel:${acceso.replace(/[^+\d]/g, '')}` : null
  if (tipo === 'Presencial') return null
  try { const u = new URL(acceso); return u.protocol === 'https:' && !u.username && !u.password ? u.href : null } catch { return null }
}

export function nombreCanal(canal) {
  const tipo = tipoCanal(canal)
  if (tipo === 'Teléfono') {
    if (canal.finalidad === 'Emergencia') return 'Línea de emergencias'
    return /^01\s?8000/.test(canal.acceso ?? '') ? 'Línea gratuita' : 'Teléfono'
  }
  if (tipo === 'Web') {
    return { 'PQR anónimo': 'Formulario sin sus datos', 'Consultar estado': 'Consultar su trámite',
      'Información': 'Página de atención' }[canal.finalidad] ?? 'Formulario web'
  }
  return { WhatsApp: 'WhatsApp', Presencial: 'Oficina en Tuluá', Correo: 'Correo' }[tipo] ?? tipo ?? 'Canal oficial'
}

export function accionCanal(canal) {
  if (!enlaceCanal(canal)) return null
  if (tipoCanal(canal) === 'WhatsApp') return 'Abrir WhatsApp'
  if (tipoCanal(canal) === 'Teléfono') return `Llamar al ${canal.acceso}`
  return 'Abrir la página'
}

// Si el correo no llegó, la persona puede usar otro canal con el mismo mensaje.
export const ofrecerOtrosCanales = (envio) => Boolean(envio?.rebote || envio?.estado === 'FALLIDO')

export const textoParaCopiar = (mensaje) => `Asunto: ${mensaje.asunto}\n\n${mensaje.texto}`

// Safari solo copia dentro del toque: con ClipboardItem la copia empieza ya y espera el texto.
// Si no se puede copiar, devuelve el texto para mostrarlo y que la persona lo copie a mano.
export async function copiarMensaje({ obtener, portapapeles, Item }) {
  let texto = ''
  const traer = obtener().then((mensaje) => (texto = textoParaCopiar(mensaje)))
  traer.catch(() => {})
  try {
    if (Item && portapapeles?.write) {
      await portapapeles.write([new Item({ 'text/plain': traer.then((t) => new Blob([t], { type: 'text/plain' })) })])
    } else {
      await portapapeles.writeText(await traer)
    }
    return { copiado: true, texto }
  } catch {
    try { await traer } catch { return { copiado: false, texto: '' } }
    return { copiado: false, texto }
  }
}
