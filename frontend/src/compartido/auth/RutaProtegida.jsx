import { Navigate } from 'react-router-dom'
import { obtenerRol, obtenerToken } from './sesion'

const RUTA_POR_ROL = {
  CIUDADANO: '/app',
  ADMIN: '/admin',
}

export default function RutaProtegida({ rol, children }) {
  if (!obtenerToken()) {
    return <Navigate to="/login" replace />
  }

  const rolActual = obtenerRol()
  if (rol && rolActual !== rol) {
    return <Navigate to={RUTA_POR_ROL[rolActual] ?? '/login'} replace />
  }

  return children
}
