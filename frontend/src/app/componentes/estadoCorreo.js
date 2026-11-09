// Textos que ve la persona después de enviar el reporte. Se habla de «usted», sin nombres de proveedor ni códigos técnicos.
export function presentacionCorreo(envio) {
  if (envio?.rebote) return { clase: 'rebote', titulo: 'El correo no llegó', etiqueta: 'No entregado', subtitulo: 'El correo no llegó a la entidad', mensaje: 'Su reporte sigue guardado y conserva su seguimiento. El correo no pudo entregarse a la entidad.' }
  if (envio?.estado === 'ENVIADO') return { clase: 'enviado', titulo: 'Reporte enviado', etiqueta: 'Enviado', subtitulo: 'Su reporte ya salió hacia la entidad', mensaje: 'Enviamos su reporte a la entidad. Puede seguir el avance desde Mis reportes.' }
  if (envio?.estado === 'FALLIDO') return { clase: 'fallido', titulo: 'Reporte guardado', etiqueta: 'No enviado', subtitulo: 'No pudimos enviar el correo', mensaje: 'Su reporte quedó guardado, pero el correo no salió. Puede intentarlo de nuevo.' }
  if (envio?.estado === 'NO_ENVIADO_CORREO') return { clase: 'externo', titulo: 'Reporte guardado', etiqueta: 'Otro canal', subtitulo: 'Siga por el canal oficial', mensaje: envio.ruta?.mensaje ?? 'Su reporte quedó guardado. Esta entidad atiende por otro canal: use el que se indica abajo.' }
  if (envio?.estado === 'PENDIENTE') return { clase: 'pendiente', titulo: 'Reporte guardado', etiqueta: 'En camino', subtitulo: 'Estamos enviando su reporte', mensaje: 'Su reporte quedó guardado y lo estamos enviando a la entidad.' }
  return { clase: 'pendiente', titulo: 'Reporte guardado', etiqueta: 'Sin enviar', subtitulo: 'Falta enviarlo', mensaje: 'Su reporte quedó guardado. Falta enviarlo a la entidad.' }
}

export function enlaceCanal(ruta) {
  if (ruta?.canal === 'Teléfono' && /^[+\d ()-]+$/.test(ruta.acceso)) return `tel:${ruta.acceso.replace(/[^+\d]/g, '')}`
  try { const u = new URL(ruta?.acceso); return u.protocol === 'https:' && !u.username && !u.password ? u.href : null } catch { return null }
}
