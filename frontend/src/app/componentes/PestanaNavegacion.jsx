import { NavLink } from 'react-router-dom'
import { IconoAjustes, IconoDocumento, IconoMas } from '../../compartido/componentes/Iconos'
import './PestanaNavegacion.css'

// Barra de abajo de la PWA: Mis reportes · Reportar (botón verde) · Ajustes.
// La pestaña activa se marca con fondo verde suave Y negrita, no solo con color.
export default function PestanaNavegacion() {
  const clase = ({ isActive }) => (isActive ? 'pestana pestana--activa' : 'pestana')

  return (
    <nav className="pestanas" aria-label="Navegación principal">
      <NavLink to="/app/reportes" className={clase}>
        <IconoDocumento tamano={22} />
        Mis reportes
      </NavLink>
      <NavLink to="/reportar" className="pestana pestana--reportar">
        <span className="pestana__mas">
          <IconoMas tamano={22} />
        </span>
        Reportar
      </NavLink>
      <NavLink to="/app/ajustes" className={clase}>
        <IconoAjustes tamano={22} />
        Ajustes
      </NavLink>
    </nav>
  )
}
