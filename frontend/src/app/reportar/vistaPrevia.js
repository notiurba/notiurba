// La vista previa solo vale para los datos con que se pidió: mientras llega la de los datos actuales, «cargando».
export function estadoVistaPrevia(resultado, datos) {
  if (!datos) return { datos: null, error: null, cargando: false }
  const vigente = resultado?.clave === datos
  return { datos: vigente ? resultado.datos : null, error: vigente ? resultado.error : null, cargando: !vigente }
}

// Un 4xx dice qué está mal en el borrador (y reintentar no sirve); lo demás es la red o el servidor.
export function explicarErrorVistaPrevia(error) {
  // 404 y 405: el servidor aún no tiene la vista previa (despliegue a medias); se puede reintentar.
  if (error?.estado >= 400 && error.estado < 500 && ![404, 405].includes(error.estado)) {
    return { mensaje: error.mensaje ?? 'Revise los datos de su reporte.', reintentar: false }
  }
  return { mensaje: 'Su reporte se puede enviar igual.', reintentar: true }
}
