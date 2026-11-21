export function textoProgresoEnvio(permiteCorreo) {
  return permiteCorreo ? 'Guardando y enviando…' : 'Guardando…'
}

export function textoAccionConfirmacion({ guardado, permiteCorreo }) {
  if (guardado) return 'Reintentar'
  return permiteCorreo ? 'Guardar y enviar' : 'Guardar reporte'
}
