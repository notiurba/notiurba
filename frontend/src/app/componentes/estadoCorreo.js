export function presentacionCorreo(envio) {
  if (envio?.rebote) return { clase: 'rebote', titulo: 'Aviso de fallo de correo', etiqueta: 'FALLIDO (rebote)', subtitulo: 'Correo no entregado a la entidad', mensaje: 'El correo no pudo entregarse a la entidad. Tu reporte sigue guardado y conserva su estado de seguimiento.' }
  if (envio?.estado === 'ENVIADO') return { clase: 'enviado', titulo: 'Reporte enviado', etiqueta: 'ENVIADO', subtitulo: 'Envío aceptado por Resend', mensaje: 'El servicio de correo aceptó el mensaje dirigido a la entidad. Puedes seguir el avance desde Mis reportes.' }
  if (envio?.estado === 'FALLIDO') return { clase: 'fallido', titulo: 'Reporte guardado · Envío fallido', etiqueta: 'FALLIDO', subtitulo: 'No pudimos entregar el correo', mensaje: 'Tu reporte está guardado, pero no pudimos entregar el correo a la entidad. Puedes reintentar cuando esté disponible.' }
  if (envio?.estado === 'NO_ENVIADO_CORREO') return { clase: 'externo', titulo: 'Reporte guardado', etiqueta: 'NO_ENVIADO_CORREO', subtitulo: 'Utiliza el canal oficial', mensaje: envio.ruta.mensaje }
  if (envio?.estado === 'PENDIENTE') return { clase: 'pendiente', titulo: 'Reporte guardado', etiqueta: 'PENDIENTE', subtitulo: 'El correo está pendiente', mensaje: 'Tu reporte se guardó correctamente. Estamos preparando el envío del correo a la entidad.' }
  return { clase: 'pendiente', titulo: 'Reporte guardado', etiqueta: 'SIN SOLICITAR', subtitulo: 'Envío sin solicitar', mensaje: 'El reporte se conserva con su código. Completa las evidencias antes de solicitar el envío.' }
}

export function enlaceCanal(ruta) {
  if (ruta?.canal === 'Teléfono' && /^[+\d ()-]+$/.test(ruta.acceso)) return `tel:${ruta.acceso.replace(/[^+\d]/g, '')}`
  try { const u = new URL(ruta?.acceso); return u.protocol === 'https:' && !u.username && !u.password ? u.href : null } catch { return null }
}
