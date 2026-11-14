import { Navigate } from 'react-router-dom'
import { RUTA_POR_ROL, estadoSesion, obtenerRol } from './sesion'

// Para las pantallas de «entrar» (bienvenida, iniciar sesión, crear cuenta). Si la persona YA tiene sesión,
// se va directo a su pantalla de inicio. Sin esto, al volver «atrás» desde un reporte aparecía la pantalla de
// iniciar sesión y parecía que la sesión se había perdido (no se perdía: seguía guardada).
export default function SoloSinSesion({ children }) {
  if (estadoSesion() === 'activa') {
    return <Navigate to={RUTA_POR_ROL[obtenerRol()] ?? '/app'} replace />
  }
  return children
}
