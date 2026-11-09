// Mensajes controlados: nunca mostrar cuerpos HTTP, credenciales ni detalles técnicos.
export function mensajeErrorLogin(error) {
  if (error.estado === 0 || error.codigo === 'SIN_CONEXION') return 'No pudimos conectar con el servidor. Revise su conexión e inténtelo de nuevo.'
  if (error.estado === 400) return 'No se pudo procesar el ingreso. Revise el correo y la contraseña e inténtelo de nuevo.'
  if (error.estado === 401) return 'No se pudo autorizar el ingreso. Revise sus datos e inténtelo de nuevo.'
  if (error.estado === 403) return 'El servidor no permitió el ingreso. Si el problema continúa, contacte al equipo de NotiUrba.'
  if (error.estado === 429) return 'Se realizaron demasiados intentos. Espere unos minutos antes de volver a ingresar.'
  if (error.estado >= 500) return 'El servicio de ingreso no está disponible en este momento. Inténtelo más tarde.'
  return 'No pudimos completar el ingreso. Inténtelo de nuevo en un momento.'
}
