import { RUTA_LOGIN, cerrarSesion, obtenerToken } from '../auth/sesion'

export const URL_BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

/** Error de la API: la respuesta {codigo, mensaje, campo} del contrato. `mensaje` ya está en español. */
export class ErrorApi extends Error {
  constructor({ estado, codigo, mensaje, campo = null }) {
    super(mensaje)
    this.name = 'ErrorApi'
    this.estado = estado
    this.codigo = codigo
    this.campo = campo
  }
}

const MENSAJE_SIN_CONEXION = 'No pudimos comunicarnos con el servidor. Revisa tu conexión e intenta de nuevo.'
const MENSAJE_RESPUESTA_RARA = 'El servidor respondió algo inesperado. Intenta de nuevo en un momento.'

async function leerError(respuesta) {
  const cuerpo = await respuesta.json().catch(() => null)
  if (cuerpo && typeof cuerpo.codigo === 'string' && typeof cuerpo.mensaje === 'string') {
    return new ErrorApi({ estado: respuesta.status, ...cuerpo })
  }
  // Sin cuerpo del contrato (p. ej. Render despertando, o un proxy): mensaje genérico
  return new ErrorApi({ estado: respuesta.status, codigo: 'RESPUESTA_INESPERADA', mensaje: MENSAJE_RESPUESTA_RARA })
}

/**
 * Llama a la API. Devuelve el JSON (o null si es 204) y lanza ErrorApi si falla.
 * Con un cuerpo FormData (fotos) no se fija Content-Type: el navegador agrega el boundary.
 */
export async function peticion(ruta, opciones = {}) {
  const token = obtenerToken()
  const esFormData = typeof FormData !== 'undefined' && opciones.body instanceof FormData
  const encabezados = {
    ...(opciones.body && !esFormData ? { 'Content-Type': 'application/json' } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...opciones.headers,
  }

  let respuesta
  try {
    respuesta = await fetch(`${URL_BASE}${ruta}`, { ...opciones, headers: encabezados })
  } catch {
    throw new ErrorApi({ estado: 0, codigo: 'SIN_CONEXION', mensaje: MENSAJE_SIN_CONEXION })
  }

  if (!respuesta.ok) {
    const error = await leerError(respuesta)
    // Sesión vencida o token inválido estando con sesión: se limpia y se vuelve a ingresar
    if (respuesta.status === 401 && error.codigo === 'NO_AUTENTICADO' && token) {
      cerrarSesion()
      if (window.location.pathname !== RUTA_LOGIN) window.location.assign(RUTA_LOGIN)
    }
    throw error
  }

  if (respuesta.status === 204) return null
  return respuesta.json()
}
