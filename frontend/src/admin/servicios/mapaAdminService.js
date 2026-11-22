import { peticion } from '../../compartido/api/cliente'
import { identificadorEstado } from '../../compartido/utilidades/reportes'

export function rangoFechasInvalido(desde, hasta) {
  return Boolean(desde && hasta && desde > hasta)
}

export function consultaAdministrativa({ categoria, estado, comuna, desde, hasta, pagina, tamano, vigencia } = {}) {
  const parametros = new URLSearchParams()
  if (categoria) parametros.set('idCategoria', categoria)
  if (estado) parametros.set('estado', identificadorEstado(estado))
  if (comuna) parametros.set('comuna', comuna)
  if (desde) parametros.set('desde', desde)
  if (hasta) parametros.set('hasta', hasta)
  if (pagina) parametros.set('pagina', pagina)
  if (tamano) parametros.set('tamano', tamano)
  if (vigencia) parametros.set('vigencia', vigencia)
  const texto = parametros.toString()
  return texto ? `?${texto}` : ''
}

export const obtenerMapaAdministrativo = (filtros) =>
  peticion(`/api/admin/mapa${consultaAdministrativa(filtros)}`)

export const obtenerCalorAdministrativo = (filtros) =>
  peticion(`/api/admin/mapa/calor${consultaAdministrativa(filtros)}`)

export const obtenerEstadisticasAdministrativas = (filtros) =>
  peticion(`/api/admin/estadisticas${consultaAdministrativa(filtros)}`)
