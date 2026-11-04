import { peticion } from './cliente'

// Listas para los selectores del registro. Son públicas: se piden antes de tener cuenta.
// Rutas del contrato (sección 4 de docs/contexto/11-contrato-api.md). PROPUESTAS: la API todavía no las tiene,
// y al implementarlas hay que agregar /api/catalogos/** a las rutas públicas de SecurityConfig.

// ─── RESPALDO TEMPORAL ──────────────────────────────────────────────────────────────────────────
// Mientras la API no tenga esos endpoints, se usan estas listas. Los ids son los que existen hoy en la
// base (migraciones V3 y V4): un solo barrio, y los 4 tipos de documento. Cuando Andrés publique los
// endpoints, la API responde y este respaldo deja de usarse solo. Para quitarlo, comente (no borre)
// estas listas, la función `conRespaldo` y los dos usos de abajo, y deje las llamadas directas a peticion.
const BARRIOS_RESPALDO = [{ idBarrio: 1, idComuna: 1, nombre: 'Administración NotiUrba' }]
const TIPOS_DOCUMENTO_RESPALDO = [
  { idTipoDocumento: 1, codigo: 'CC', nombre: 'Cédula de ciudadanía' },
  { idTipoDocumento: 2, codigo: 'CE', nombre: 'Cédula de extranjería' },
  // PPT oculto por decisión del equipo (basta con la cédula de extranjería). Para volver a mostrarlo, quite el //
  // { idTipoDocumento: 3, codigo: 'PPT', nombre: 'Permiso por protección temporal' },
  { idTipoDocumento: 4, codigo: 'PAS', nombre: 'Pasaporte' },
]

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
export const obtenerTiposDocumento = () => conRespaldo('/api/catalogos/tipos-documento', TIPOS_DOCUMENTO_RESPALDO)
