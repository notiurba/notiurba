import { Link } from 'react-router-dom'
import './CabeceraAuth.css'

// Línea final con un enlace, por ejemplo «¿Aún no tiene cuenta? Regístrese».
export default function PieAuth({ texto, enlace, to, anclado = false }) {
  return (
    <p className={anclado ? 'pie-auth pie-auth--anclado' : 'pie-auth'}>
      {texto} <Link to={to}>{enlace}</Link>
    </p>
  )
}
