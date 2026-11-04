const CLAVE_TOKEN = 'notiurba.token'

/** Pantalla de ingreso (correo y contraseña). RutaProtegida y el cliente de la API llevan aquí. */
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

export function obtenerRol() {
  const token = obtenerToken()
  if (!token) return null

  try {
    const payload = token.split('.')[1]
    const json = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
    return json.rol ?? null
  } catch {
    return null
  }
}
