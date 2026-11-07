/** Se conserva el código ANTES de las fotos. Un fallo posterior nunca vuelve a crear el reporte. */
export async function guardarConEvidencias({ datos, fotos, guardado, api, conservar }) {
  const creado = guardado || await api.guardarReporte(datos)
  conservar(creado)
  for (const foto of fotos) await api.subirFoto(creado.codigoSeguimiento, foto)
  await api.enviar(creado.codigoSeguimiento)
  return creado
}

export function claveRuta(idCategoria, emergencia) {
  return `${idCategoria}:${Boolean(emergencia)}`
}

/** Un resultado solo sirve para la selección que originó su petición, aunque otra respuesta llegue tarde. */
export function rutaActual(respuesta, clave) {
  return respuesta?.clave === clave ? respuesta.ruta : null
}
