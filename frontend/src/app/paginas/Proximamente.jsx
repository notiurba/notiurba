import { Link } from 'react-router-dom'
import './CorreoReportes.css'

export default function Proximamente() {
  return (
    <main className="correo-flujo">
      <h1>Reportes ciudadanos</h1>
      <nav className="correo-acciones" aria-label="Reportes">
        <Link className="correo-primario" to="/app/crear-reporte">Crear reporte</Link>
        <Link className="correo-secundario" to="/app/mis-reportes">Mis reportes</Link>
      </nav>
    </main>
  )
}
