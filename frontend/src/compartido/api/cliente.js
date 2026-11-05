import { RUTA_LOGIN, cerrarSesion, obtenerToken } from '../auth/sesion'
import { ErrorApi } from './errorApi'

export { ErrorApi }
export const EVENTO_SESION_TERMINADA = 'notiurba:sesion-terminada'
export const URL_BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

const MENSAJE_SIN_CONEXION = 'No pudimos comunicarnos con el servidor. Revise su conexión e intente de nuevo.'
const MENSAJE_RESPUESTA_RARA = 'El servidor respondió algo inesperado. Intente de nuevo en un momento.'

async function leerError(respuesta) {
  const cuerpo = await respuesta.json().catch(() => null)
  // Un error del servidor (500 en adelante) puede traer texto técnico (SQL, nombres de clases): no se muestra.
  if (respuesta.status < 500 && cuerpo && typeof cuerpo.codigo === 'string' && typeof cuerpo.mensaje === 'string') {
    return new ErrorApi({ estado: respuesta.status, ...cuerpo })
  }
  // Sin cuerpo del contrato (p. ej. Render despertando, o un proxy): mensaje genérico
  return new ErrorApi({ estado: respuesta.status, codigo: 'RESPUESTA_INESPERADA', mensaje: MENSAJE_RESPUESTA_RARA })
}

/**
 * Llama a la API. Devuelve el JSON (o null si es 204) y lanza ErrorApi si falla.
 * - Con un cuerpo FormData (fotos) no se fija Content-Type: el navegador agrega el boundary.
 * - publica: true es para llamadas que se hacen SIN sesión (login, registro): no envían el token.
 */
export async function peticion(ruta, { publica = false, ...opciones } = {}) {
  // Backend simulado: solo en desarrollo con VITE_USAR_MOCKS=true. En el build de producción esta
  // rama se elimina y la carpeta src/simulacion no viaja al navegador. Si esa carpeta se borra,
  // import.meta.glob devuelve un objeto vacío y la petición sigue a la API real.
  if (import.meta.env.VITE_USAR_MOCKS === 'true') {
    const [cargarSimulacion] = Object.values(import.meta.glob('../../simulacion/index.js'))
    if (cargarSimulacion) {
      const { buscarMock } = await cargarSimulacion()
      const simulada = buscarMock(opciones.method ?? 'GET', ruta, opciones.body)
      if (simulada) return simulada
    }
  }

  const token = publica ? null : obtenerToken()
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
    // Teníamos sesión y la API la rechaza: el token venció. Se borra y se pide ingresar de nuevo.
    if (respuesta.status === 401 && error.codigo === 'NO_AUTENTICADO' && token) {
      cerrarSesion()
      // La PWA ciudadana muestra la hoja «Su sesión terminó»: si la atiende, cancela el evento.
      // Si nadie lo atiende (por ejemplo en el panel de administración), se va directo al ingreso.
      const hayVentana = typeof window.dispatchEvent === 'function'
      const atendido = hayVentana && !window.dispatchEvent(new CustomEvent(EVENTO_SESION_TERMINADA, { cancelable: true }))
      if (!atendido && window.location.pathname !== RUTA_LOGIN) window.location.assign(RUTA_LOGIN)
    }
    throw error
  }

  if (respuesta.status === 204) return null
  return respuesta.json()
}
