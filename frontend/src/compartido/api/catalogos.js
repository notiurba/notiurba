import { peticion } from './cliente'

// Catálogos públicos. Solo los tipos de documento conservan el respaldo histórico.
// Los barrios siempre proceden de la API; una lista vacía es un bloqueo de datos.
const TIPOS_DOCUMENTO_RESPALDO = [
  { idTipoDocumento: 1, codigo: 'CC', nombre: 'Cédula de ciudadanía' },
  { idTipoDocumento: 2, codigo: 'CE', nombre: 'Cédula de extranjería' },
  { idTipoDocumento: 4, codigo: 'PAS', nombre: 'Pasaporte' },
]

// Tipos que el equipo decidió no ofrecer aunque el catálogo de la API los traiga (basta con la cédula de
// extranjería). Para volver a mostrar el PPT, quite 'PPT' de esta lista.
const TIPOS_DOCUMENTO_OCULTOS = ['PPT']

// Pide la lista a la API; si el endpoint aún no existe (404) o no es público (401/403), usa el respaldo.
// Un fallo de red (sin conexión) NO usa el respaldo: eso sí se avisa a la persona.
async function conRespaldo(ruta, respaldo) {
  try {
    return await peticion(ruta, { publica: true })
  } catch (error) {
    if ([401, 403, 404].includes(error.estado)) return respaldo
    throw error
  }
}
// ────────────────────────────────────────────────────────────────────────────────────────────────

// Nunca sustituir los barrios ciudadanos por el barrio interno de administración.
export const obtenerBarrios = () => peticion('/api/catalogos/barrios', { publica: true })
export const obtenerTiposDocumento = async () => {
  const tipos = await conRespaldo('/api/catalogos/tipos-documento', TIPOS_DOCUMENTO_RESPALDO)
  return tipos.filter((tipo) => !TIPOS_DOCUMENTO_OCULTOS.includes(tipo.codigo))
}
