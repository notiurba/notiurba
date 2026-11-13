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
export function enlaceCanal(canal) {
  const tipo = canal?.tipo ?? canal?.canal
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
  if (canal?.tipo === 'Teléfono') {
    if (canal.finalidad === 'Emergencia') return 'Línea de emergencias'
    return /^01\s?8000/.test(canal.acceso ?? '') ? 'Línea gratuita' : 'Teléfono'
  }
  if (canal?.tipo === 'Web') {
    return { 'PQR anónimo': 'Formulario sin sus datos', 'Consultar estado': 'Consultar su trámite',
      'Información': 'Página de atención' }[canal.finalidad] ?? 'Formulario web'
  }
  return { WhatsApp: 'WhatsApp', Presencial: 'Oficina en Tuluá', Correo: 'Correo' }[canal?.tipo] ?? canal?.tipo ?? 'Canal oficial'
}

export function accionCanal(canal) {
  if (!enlaceCanal(canal)) return null
  if (canal.tipo === 'WhatsApp') return 'Abrir WhatsApp'
  if (canal.tipo === 'Teléfono') return `Llamar al ${canal.acceso}`
  return 'Abrir la página'
}

// Si el correo no llegó, la persona puede usar otro canal con el mismo mensaje.
export const ofrecerOtrosCanales = (envio) => Boolean(envio?.rebote || envio?.estado === 'FALLIDO')

export const textoParaCopiar = (mensaje) => `Asunto: ${mensaje.asunto}\n\n${mensaje.texto}`
