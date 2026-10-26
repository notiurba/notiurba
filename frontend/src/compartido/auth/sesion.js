const CLAVE_TOKEN = 'notiurba.token'

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
