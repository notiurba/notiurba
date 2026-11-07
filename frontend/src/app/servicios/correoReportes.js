import { peticion } from '../../compartido/api/cliente'

export const categorias = () => peticion('/api/enrutamiento/categorias')
export const barrios = () => peticion('/api/reportes/opciones/barrios')
export const resolver = (id, emergencia) => peticion(`/api/enrutamiento/categorias/${id}?emergencia=${Boolean(emergencia)}`)
export const consultar = (codigo) => peticion(`/api/reportes/${encodeURIComponent(codigo)}`)
export const estadoCorreo = (codigo) => peticion(`/api/reportes/${encodeURIComponent(codigo)}/envio`)
export const enviar = (codigo) => peticion(`/api/reportes/${encodeURIComponent(codigo)}/enviar`, { method: 'POST' })

/** Guarda UNA vez y devuelve el código incluso si fallan fotos o solicitud de correo. */
export async function guardarReporte(datos) {
  return peticion('/api/reportes', { method: 'POST', body: JSON.stringify(datos) })
}
export async function subirFoto(codigo, foto) {
  const body = new FormData()
  body.append('foto', foto)
  return peticion(`/api/reportes/${encodeURIComponent(codigo)}/fotos`, { method: 'POST', body })
}
