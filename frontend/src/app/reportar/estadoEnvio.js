export function textoProgresoEnvio(permiteCorreo) {
  return permiteCorreo ? 'Guardando y enviando…' : 'Guardando…'
}

export function textoAccionConfirmacion({ guardado, enviando, permiteCorreo }) {
  if (enviando) return textoProgresoEnvio(permiteCorreo)
  if (guardado) return 'Reintentar'
  return permiteCorreo ? 'Guardar y enviar' : 'Guardar reporte'
}
