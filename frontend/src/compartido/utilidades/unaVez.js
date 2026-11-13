// Candado para acciones que consultan o envían datos al servicio: desde el primer clic (o Enter) y hasta
// que llega la respuesta, los siguientes se ignoran. Se cierra en el mismo instante, sin esperar a que la
// pantalla se vuelva a pintar con el botón apagado. Si la acción falla, se abre otra vez para reintentar.
export async function ejecutarUnaVez(candado, accion, args) {
  // Un formulario ignorado igual no debe recargar la página.
  args[0]?.preventDefault?.()
  if (candado.current) return undefined
  candado.current = true
  try {
    return await accion(...args)
  } finally {
    candado.current = false
  }
}

// Versión sin React: `obtenerAccion` devuelve la función a ejecutar en ese momento.
export function unaVez(obtenerAccion) {
  const candado = { current: false }
  return (...args) => ejecutarUnaVez(candado, obtenerAccion(), args)
}
