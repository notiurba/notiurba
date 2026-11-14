// A dónde sigue el flujo según la categoría: si la entidad no recibe correo, no se piden más datos
// y se muestran sus canales oficiales (no se crea ningún reporte).
export const RUTA_CANALES = '/reportar/canales'
const PASOS_DEL_CORREO = ['/reportar/ubicacion', '/reportar/detalles', '/reportar/confirmacion']

const sinCorreo = (categoria) => categoria?.permiteCorreo === false

export const destinoTrasCategoria = (categoria) => sinCorreo(categoria) ? RUTA_CANALES : '/reportar/ubicacion'

export function redireccionPorCategoria(pathname, categoria, guardado) {
  if (guardado || !categoria) return null
  if (sinCorreo(categoria) && PASOS_DEL_CORREO.includes(pathname)) return RUTA_CANALES
  if (!sinCorreo(categoria) && pathname === RUTA_CANALES) return '/reportar'
  return null
}
