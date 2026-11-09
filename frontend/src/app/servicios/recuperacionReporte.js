/** Se conserva el código ANTES de las fotos. Un fallo posterior nunca vuelve a crear el reporte. */
export async function guardarConEvidencias({ datos, fotos, guardado, api, conservar, alSubirFoto = () => {} }) {
  const creado = guardado || await api.guardarReporte(datos)
  conservar(creado)
  let pendientes = fotos
  // Si la respuesta de una subida se perdió, primero conciliar con las fotos realmente guardadas.
  if (guardado && api.consultar) {
    const actual = await api.consultar(creado.codigoSeguimiento)
    if (!Array.isArray(actual.fotos)) throw new Error('No pudimos comprobar las fotos guardadas. Consulte el reporte antes de reintentar.')
    const adicionales = Math.max(0, actual.fotos.length - (guardado.fotosSubidas ?? 0))
    for (let i = 0; i < adicionales; i++) alSubirFoto(fotos[i])
    pendientes = fotos.slice(adicionales)
  }
  for (const foto of pendientes) {
    await api.subirFoto(creado.codigoSeguimiento, foto)
    alSubirFoto(foto)
  }
  const envio = await api.enviar(creado.codigoSeguimiento)
  return { ...creado, envio }
}

export function claveRuta(idCategoria, emergencia) {
  return `${idCategoria}:${Boolean(emergencia)}`
}

/** Un resultado solo sirve para la selección que originó su petición, aunque otra respuesta llegue tarde. */
export function rutaActual(respuesta, clave) {
  return respuesta?.clave === clave ? respuesta.ruta : null
}
