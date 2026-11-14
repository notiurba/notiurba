import { Link } from 'react-router-dom'
import './BotonIcono.css'

// Botón circular que solo lleva un ícono (volver, eliminar, más opciones…).
//  - etiqueta: texto para el lector de pantalla (obligatorio: el ícono solo no dice nada).
//  - variante: 'neutro' (por defecto) o 'peligro'.
//  - to: si se pasa, se dibuja como enlace interno con el mismo aspecto.
export default function BotonIcono({ etiqueta, variante = 'neutro', to, children, ...resto }) {
  const clase = `boton-icono boton-icono--${variante}`

  if (to) {
    return (
      <Link to={to} className={clase} aria-label={etiqueta} {...resto}>
        {children}
      </Link>
    )
  }

  return (
    <button type="button" className={clase} aria-label={etiqueta} {...resto}>
      {children}
    </button>
  )
}
