import { TEXTOS_INGRESO, TITULOS_INGRESO, textoBloqueo } from '../textos/ingreso.js'

// Decide qué ve la persona según el código y el estado del error, nunca por el texto que traiga el
// servicio: un fallo 5xx o un código desconocido puede traer detalles técnicos que no se muestran.
export const SITUACION = {
  DATOS_INCORRECTOS: 'DATOS_INCORRECTOS',
  BLOQUEO: 'BLOQUEO',
  SIN_CONFIRMAR: 'SIN_CONFIRMAR',
  SIN_INTERNET: 'SIN_INTERNET',
  SERVICIO_CAIDO: 'SERVICIO_CAIDO',
  GENERICO: 'GENERICO',
}

const esServicioCaido = (error) => error.estado >= 500 || error.codigo === 'RESPUESTA_INESPERADA'

export function situacionIngreso(error = {}) {
  if (error.estado === 0 || error.codigo === 'SIN_CONEXION') return SITUACION.SIN_INTERNET
  if (error.estado === 429 && error.codigo === 'INTENTOS_EXCEDIDOS') return SITUACION.BLOQUEO
  if (error.estado === 403 && error.codigo === 'CORREO_NO_VERIFICADO') return SITUACION.SIN_CONFIRMAR
  if (error.estado === 401 && error.codigo === 'CREDENCIALES_INVALIDAS') return SITUACION.DATOS_INCORRECTOS
  if (esServicioCaido(error)) return SITUACION.SERVICIO_CAIDO
  return SITUACION.GENERICO
}

// { situacion, titulo, mensaje } listo para mostrar. En el bloqueo, `esperarSegundos` y `bloqueadoHasta`
// salen de `error.datos`; si faltan, el texto no promete una hora.
export function avisoDeIngreso(error = {}) {
  const situacion = situacionIngreso(error)
  switch (situacion) {
    case SITUACION.DATOS_INCORRECTOS: {
      const ultimo = error.datos?.intentosRestantes === 1
      return {
        situacion,
        titulo: TITULOS_INGRESO.datosIncorrectos,
        mensaje: ultimo ? `${TEXTOS_INGRESO.datosIncorrectos} ${TEXTOS_INGRESO.ultimoIntento}` : TEXTOS_INGRESO.datosIncorrectos,
      }
    }
    case SITUACION.BLOQUEO:
      return {
        situacion,
        titulo: TITULOS_INGRESO.bloqueo,
        mensaje: textoBloqueo(error.datos?.esperarSegundos, error.datos?.bloqueadoHasta),
      }
    case SITUACION.SIN_CONFIRMAR:
      return { situacion, titulo: TITULOS_INGRESO.sinConfirmar, mensaje: TEXTOS_INGRESO.sinConfirmar }
    case SITUACION.SIN_INTERNET:
      return { situacion, titulo: TITULOS_INGRESO.sinInternet, mensaje: TEXTOS_INGRESO.sinInternet }
    case SITUACION.SERVICIO_CAIDO:
      return { situacion, titulo: TITULOS_INGRESO.servicioCaido, mensaje: TEXTOS_INGRESO.servicioCaido }
    default:
      return { situacion, titulo: TITULOS_INGRESO.generico, mensaje: TEXTOS_INGRESO.generico }
  }
}

// Falla de «Continuar con Google»: la red y el servicio caído se dicen igual que en el ingreso con
// correo; todo lo demás (ventana cerrada, cuenta no válida) es el mismo mensaje de Google.
export function avisoDeGoogle(error = {}) {
  const situacion = situacionIngreso(error)
  if (situacion === SITUACION.SIN_INTERNET || situacion === SITUACION.SERVICIO_CAIDO || situacion === SITUACION.BLOQUEO) {
    return avisoDeIngreso(error)
  }
  return { situacion: SITUACION.GENERICO, titulo: TITULOS_INGRESO.google, mensaje: TEXTOS_INGRESO.google }
}

export function mensajeErrorLogin(error) {
  return avisoDeIngreso(error).mensaje
}
