import { peticion } from '../../compartido/api/cliente'

// Límites contra el abuso (contrato 5.5): GET para leerlos y PUT para guardarlos todos a la vez.
export const consultarLimites = () => peticion('/api/admin/configuracion/limites')
export const guardarLimites = (limites) =>
  peticion('/api/admin/configuracion/limites', { method: 'PUT', body: JSON.stringify(limites) })
