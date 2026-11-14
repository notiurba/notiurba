import { soloErrores } from './campos.js'

export const MAX_DESCRIPCION = 500
export const MIN_DESCRIPCION = 10
export const MAX_FOTOS = 3
export const MAX_BYTES_FOTO = 1024 * 1024 // 1 MB por foto (contrato propuesto en simulacion/README.md)
export const TIPOS_FOTO = ['image/jpeg', 'image/png']

// Paso 2: GPS o barrio si la categoría requiere ubicación; el servidor decide la cobertura.
export function validarUbicacion(d, { requiereUbicacion = true } = {}) {
  if (!requiereUbicacion) return {}
  const lat = d.latitud != null && d.latitud !== ''
  const lon = d.longitud != null && d.longitud !== ''
  let punto = ''
  if (lat !== lon) punto = 'Indique la ubicación completa.'
  else if (lat && (!Number.isFinite(d.latitud) || !Number.isFinite(d.longitud) || Math.abs(d.latitud) > 90 || Math.abs(d.longitud) > 180)) punto = 'Indique una ubicación válida.'
  else if (!lat && !d.idBarrio) punto = 'Marque una ubicación o seleccione un barrio para continuar.'
  return soloErrores({
    punto,
  })
}

// Paso 3: la descripción es obligatoria (10 a 500 caracteres).
export function validarDetalles(d) {
  const largo = d.descripcion.trim().length
  let error = ''
  if (largo === 0) error = 'Escriba una descripción del problema.'
  else if (largo < MIN_DESCRIPCION) error = `Escriba al menos ${MIN_DESCRIPCION} caracteres`
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
// totalFotos y confirmarDuplicado solo viajan si se pasan (la vista previa pone totalFotos por su cuenta).
export function armarSolicitudReporte(b, { autorizaEnvio = false, emergencia = false, categoria, totalFotos, confirmarDuplicado = false } = {}) {
  const solicitud = {
    idCategoria: Number(b.idCategoria),
    descripcion: b.descripcion.trim(),
    autorizaEnvio: autorizaEnvio === true,
    emergencia: emergencia === true,
  }
  const pideUbicacion = categoria?.requiereUbicacion !== false
  if (pideUbicacion && b.latitud != null && b.longitud != null) {
    solicitud.latitud = b.latitud
    solicitud.longitud = b.longitud
  }
  if (pideUbicacion && b.idBarrio) solicitud.idBarrio = Number(b.idBarrio)
  if (pideUbicacion && b.direccion.trim()) solicitud.direccionIncidente = b.direccion.trim()
  if (categoria?.admiteMatricula !== false && b.matricula.trim()) solicitud.matriculaCuentaServicio = b.matricula.trim()
  if (Number.isInteger(totalFotos)) solicitud.totalFotos = totalFotos
  if (confirmarDuplicado === true) solicitud.confirmarDuplicado = true
  return solicitud
}

export function validarConfirmacionReporte(b, categoria, autorizaEnvio) {
  return {
    ...validarUbicacion(b, { requiereUbicacion: categoria?.requiereUbicacion !== false }),
    ...validarDetalles(b),
    ...soloErrores({
      idCategoria: categoria && String(categoria.idCategoria) === String(b.idCategoria) ? '' : 'Esta categoría ya no está disponible. Elija otra.',
      autorizaEnvio: autorizaEnvio === true ? '' : 'Autorice el envío de este reporte para continuar.',
    }),
  }
}

// La vista previa del mensaje usa los mismos datos que la creación, más cuántas fotos van; no lleva autorización.
export function armarVistaPrevia(b, { emergencia = false, categoria, totalFotos = 0 } = {}) {
  const datos = armarSolicitudReporte(b, { emergencia, categoria })
  delete datos.autorizaEnvio
  return { ...datos, totalFotos }
}
