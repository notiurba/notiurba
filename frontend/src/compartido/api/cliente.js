import { obtenerToken } from '../auth/sesion'

const URL_BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export async function peticion(ruta, opciones = {}) {
  const token = obtenerToken()
  const respuesta = await fetch(`${URL_BASE}${ruta}`, {
    ...opciones,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...opciones.headers,
    },
  })

  if (!respuesta.ok) {
    throw new Error(`Error ${respuesta.status} al llamar ${ruta}`)
  }

  if (respuesta.status === 204) return null
  return respuesta.json()
}
