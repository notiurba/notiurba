import { peticion } from './cliente'

// Listas para los selectores del registro. Son públicas: se piden antes de tener cuenta.
// Rutas del contrato (sección 4 de docs/contexto/11-contrato-api.md). PROPUESTAS: la API todavía no las tiene,
// y al implementarlas hay que agregar /api/catalogos/** a las rutas públicas de SecurityConfig.
export const obtenerBarrios = () => peticion('/api/catalogos/barrios', { publica: true })
export const obtenerTiposDocumento = () => peticion('/api/catalogos/tipos-documento', { publica: true })
