import { peticion } from './cliente'

// Listas para los selectores del registro. Son públicas: se piden antes de tener cuenta.
// La API ya publica estas rutas (NOT-55, rama develop). El respaldo de abajo solo se usa si el backend
// es una versión anterior que no las tiene: sus ids son los de las migraciones V3 y V4 (un solo barrio y los
// tipos de documento). Cuando todo el equipo use el backend nuevo, el respaldo se puede quitar: comente
// (no borre) estas listas, la función `conRespaldo` y los dos usos de abajo, y deje las llamadas directas.
const BARRIOS_RESPALDO = [{ idBarrio: 1, idComuna: 1, nombre: 'Administración NotiUrba' }]
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

export const obtenerBarrios = () => conRespaldo('/api/catalogos/barrios', BARRIOS_RESPALDO)
export const obtenerTiposDocumento = async () => {
  const tipos = await conRespaldo('/api/catalogos/tipos-documento', TIPOS_DOCUMENTO_RESPALDO)
  return tipos.filter((tipo) => !TIPOS_DOCUMENTO_OCULTOS.includes(tipo.codigo))
}
