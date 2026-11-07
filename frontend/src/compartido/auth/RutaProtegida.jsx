import { Navigate } from 'react-router-dom'
import { RUTA_POR_ROL, estadoSesion, obtenerRol } from './sesion'

// Uso: <RutaProtegida> exige sesión; <RutaProtegida rol="ADMIN"> exige además ese rol.
// Sin la propiedad "rol" entra cualquiera con sesión (así un ADMIN también usa /app).
export default function RutaProtegida({ rol, children }) {
  const estado = estadoSesion()

  if (estado === 'ninguna') {
    return <Navigate to="/login" replace />
  }
  if (estado === 'vencida') {
    return <Navigate to="/login?motivo=vencida" replace />
  }

  const rolActual = obtenerRol()
  if (rol && rolActual !== rol) {
    return (
      <Navigate
        to={RUTA_POR_ROL[rolActual] ?? '/login'}
        replace
        state={{ aviso: 'Acceso no permitido' }}
      />
    )
  }

  return children
}
