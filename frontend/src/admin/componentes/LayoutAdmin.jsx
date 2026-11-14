import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { cerrarSesion } from '../../compartido/auth/sesion'
import Boton from '../../compartido/componentes/Boton'
import './LayoutAdmin.css'

// Marco del panel: cabecera con las secciones (Reportes, Mapa de calor y Configuración) y «Cerrar sesión».
export default function LayoutAdmin() {
  const navigate = useNavigate()

  function salir() {
    cerrarSesion()
    navigate('/', { replace: true })
  }

  return (
    <div className="admin">
      <header className="admin__cabecera">
        <div className="admin__marca">
          <strong>NotiUrba</strong>
          <span>Panel administrativo</span>
        </div>
        <nav className="admin__pestanas" aria-label="Secciones del panel">
          <NavLink to="/admin" end>Reportes</NavLink>
          <NavLink to="/admin/mapa">Mapa de calor</NavLink>
          <NavLink to="/admin/configuracion">Configuración</NavLink>
        </nav>
        <Boton variante="secundario" onClick={salir}>Cerrar sesión</Boton>
      </header>
      <main className="admin__contenido">
        <Outlet />
      </main>
    </div>
  )
}
