import { peticion } from './cliente'

// Perfil de la persona con sesión (PROPUESTO: ver simulacion/README.md).
export const obtenerPerfil = () => peticion('/api/usuarios/me')

export const guardarPerfil = (cambios) =>
  peticion('/api/usuarios/me', { method: 'PUT', body: JSON.stringify(cambios) })

// Borra sus reportes y fotos, pero conserva la cuenta.
export const eliminarMisDatos = () => peticion('/api/usuarios/me/datos', { method: 'DELETE' })

export const eliminarMiCuenta = () => peticion('/api/usuarios/me', { method: 'DELETE' })
