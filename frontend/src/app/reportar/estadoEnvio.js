// Un reporte se conserva apenas se crea para que un fallo de una foto o del
// encolado se pueda recuperar sin duplicarlo. Eso no significa que la acción
// actual haya terminado: durante el envío la hoja debe conservar su mensaje
// de progreso, aun cuando ya exista un código de seguimiento.
export function textoAccionConfirmacion({ guardado, enviando, permiteCorreo }) {
  if (enviando) return 'Guardando y enviando…'
  if (guardado) return 'Reintentar'
  return permiteCorreo ? 'Guardar y enviar' : 'Guardar reporte'
}
