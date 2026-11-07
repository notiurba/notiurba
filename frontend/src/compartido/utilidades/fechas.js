// Fecha corta como en el diseño: 17/10/2026. Recibe el texto ISO que manda la API.
export function formatearFecha(iso) {
  const fecha = new Date(iso)
  if (Number.isNaN(fecha.getTime())) return ''
  const dia = String(fecha.getDate()).padStart(2, '0')
  const mes = String(fecha.getMonth() + 1).padStart(2, '0')
  return `${dia}/${mes}/${fecha.getFullYear()}`
}

// Fecha y hora para el historial: 17/10/2026 · 9:05 a. m.
export function formatearFechaHora(iso) {
  const fecha = new Date(iso)
  if (Number.isNaN(fecha.getTime())) return ''
  const hora = fecha.toLocaleTimeString('es-CO', { hour: 'numeric', minute: '2-digit' })
  return `${formatearFecha(iso)} · ${hora}`
}
