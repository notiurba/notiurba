import { Navigate } from 'react-router-dom'
import { RUTA_LOGIN, RUTA_POR_ROL, obtenerRol, obtenerToken } from './sesion'

export default function RutaProtegida({ rol, children }) {
  if (!obtenerToken()) {
    return <Navigate to={RUTA_LOGIN} replace />
  }

  const rolActual = obtenerRol()
  if (rol && rolActual !== rol) {
    return <Navigate to={RUTA_POR_ROL[rolActual] ?? RUTA_LOGIN} replace />
  }

  return children
}
