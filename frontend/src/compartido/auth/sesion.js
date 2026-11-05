const CLAVE_TOKEN = 'notiurba.token'

/** Ruta a la que el cliente de la API manda cuando la sesión venció (ahí se redirige al login). */
export const RUTA_LOGIN = '/app/ingresar'

/** Página de inicio de cada rol después de ingresar. */
export const RUTA_POR_ROL = {
  CIUDADANO: '/app',
  ADMIN: '/admin',
}

export function guardarToken(token) {
  localStorage.setItem(CLAVE_TOKEN, token)
}

export function obtenerToken() {
  return localStorage.getItem(CLAVE_TOKEN)
}

export function cerrarSesion() {
  localStorage.removeItem(CLAVE_TOKEN)
}

// Un JWT son tres partes separadas por puntos. La del medio (payload) trae los datos
// de la sesión: el rol y la fecha de vencimiento (exp, en segundos).
function leerPayload() {
  const token = obtenerToken()
  if (!token) return null

  try {
    const payload = token.split('.')[1]
    return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
  } catch {
    return null
  }
}

export function obtenerRol() {
  return leerPayload()?.rol ?? null
}

// 'ninguna' = no hay token (o está dañado), 'vencida' = ya pasó su fecha, 'activa' = sirve.
// OJO: esto solo decide qué pantalla mostrar. Quien de verdad protege los datos es la API.
export function estadoSesion() {
  const payload = leerPayload()
  if (!payload) return 'ninguna'
  if (payload.exp && payload.exp * 1000 <= Date.now()) return 'vencida'
  return 'activa'
}
