import { soloErrores } from './campos.js'

export const MAX_DESCRIPCION = 500
const MIN_DESCRIPCION = 10
export const MAX_FOTOS = 3
export const MAX_BYTES_FOTO = 1024 * 1024 // 1 MB por foto (contrato propuesto en simulacion/README.md)
export const TIPOS_FOTO = ['image/jpeg', 'image/png']

// Paso 2: hace falta un punto (GPS o marcado en el mapa). El barrio y la dirección son opcionales
// (contrato 3.4: la comuna sale del GPS o, si no hay, del barrio).
export function validarUbicacion(d) {
  return soloErrores({
    punto: d.latitud == null || d.longitud == null ? 'Marque el punto en el mapa o use su ubicación actual.' : '',
  })
}

// Paso 3: la descripción es obligatoria (10 a 500 caracteres).
export function validarDetalles(d) {
  const largo = d.descripcion.trim().length
  let error = ''
  if (largo < MIN_DESCRIPCION) error = `Escriba al menos ${MIN_DESCRIPCION} caracteres`
  else if (largo > MAX_DESCRIPCION) error = `Máximo ${MAX_DESCRIPCION} caracteres`
  return soloErrores({ descripcion: error })
}

// Una foto sirve si es JPG o PNG y todavía hay cupo. El peso no se rechaza: `reducirFoto`
// (utilidades/fotos.js) la achica a 1 MB o menos. Devuelve el motivo o ''.
export function validarFoto(archivo, fotosActuales) {
  if (fotosActuales >= MAX_FOTOS) return `Solo se aceptan hasta ${MAX_FOTOS} fotos por reporte.`
  if (!TIPOS_FOTO.includes(archivo.type)) return 'Solo se aceptan imágenes JPG o PNG.'
  return ''
}

// Cuerpo que espera POST /api/reportes (contrato 3.4). Las fotos se suben aparte, una por una.
// Sin punto en el mapa (categorías que no piden ubicación) se omiten latitud y longitud.
export function armarSolicitudReporte(b) {
  const solicitud = {
    idCategoria: Number(b.idCategoria),
    descripcion: b.descripcion.trim(),
    autorizaEnvio: true,
  }
  if (b.latitud != null && b.longitud != null) {
    solicitud.latitud = b.latitud
    solicitud.longitud = b.longitud
  }
  if (b.idBarrio) solicitud.idBarrio = Number(b.idBarrio)
  if (b.direccion.trim()) solicitud.direccionIncidente = b.direccion.trim()
  if (b.matricula.trim()) solicitud.matriculaCuentaServicio = b.matricula.trim()
  return solicitud
}
