import { RUTA_POR_ROL } from './sesion.js'

const CLAVE = 'notiurba.retornoReporte'
const VIGENCIA = 30 * 60 * 1000
const RUTAS = new Set(['/reportar', '/reportar/ubicacion', '/reportar/detalles', '/reportar/confirmacion'])

export function rutaReportePermitida(ruta) {
  return typeof ruta === 'string' && RUTAS.has(ruta) ? ruta : null
}

export function borrarRetornoReporte() {
  try { sessionStorage.removeItem(CLAVE) } catch { /* Almacenamiento no disponible. */ }
}

export function leerRetornoReporte() {
  try {
    const retorno = JSON.parse(sessionStorage.getItem(CLAVE))
    const edad = Date.now() - retorno?.creadoEn
    if (rutaReportePermitida(retorno?.ruta) && Number.isFinite(retorno.creadoEn)
      && edad >= 0 && edad < VIGENCIA && ['acceso', 'perfil'].includes(retorno.etapa)) return retorno
  } catch { /* Un dato corrupto o bloqueado no debe impedir autenticarse. */ }
  borrarRetornoReporte()
  return null
}

function guardar(retorno) {
  try { sessionStorage.setItem(CLAVE, JSON.stringify(retorno)) } catch { /* Se conserva state si existe. */ }
}

// Entrar directamente al acceso descarta intenciones anteriores; nunca infiere un destino del borrador.
export function prepararRetornoReporte(siguiente) {
  const ruta = rutaReportePermitida(siguiente)
  if (ruta) guardar({ ruta, creadoEn: Date.now(), etapa: 'acceso' })
  else borrarRetornoReporte()
}

export function prepararRetornoPerfil(siguiente) {
  const previo = leerRetornoReporte()
  const ruta = siguiente === undefined ? previo?.ruta : rutaReportePermitida(siguiente)
  if (ruta) guardar({ ruta, creadoEn: previo?.ruta === ruta ? previo.creadoEn : Date.now(), etapa: 'perfil' })
  else borrarRetornoReporte()
}

export function estadoRetornoReporte(siguiente) {
  const ruta = siguiente === undefined ? leerRetornoReporte()?.ruta : rutaReportePermitida(siguiente)
  return ruta ? { siguiente: ruta } : undefined
}

export function destinoTrasAutenticacion(rol, siguiente) {
  const ruta = estadoRetornoReporte(siguiente)?.siguiente
  borrarRetornoReporte()
  return rol === 'CIUDADANO' && ruta ? ruta : (RUTA_POR_ROL[rol] ?? '/app')
}

// CompletarPerfil conserva su implementación y su consentimiento: solo interceptamos su llegada a /app.
export function destinoDesdePerfil(rol, pathname) {
  const retorno = leerRetornoReporte()
  return pathname === '/app' && rol === 'CIUDADANO' && retorno?.etapa === 'perfil' ? retorno.ruta : null
}
