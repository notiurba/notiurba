import { Link } from 'react-router-dom'
import './Boton.css'

// Un solo botón para toda la app.
//  - variante: 'primario' (verde), 'secundario' (con borde), 'peligro' (rosa) o 'texto' (neutro, sin fondo).
//  - cargando: bloquea el botón y muestra `textoCargando` mientras se espera a la API.
//  - to: si se pasa, se dibuja como enlace interno (<Link>) con el mismo aspecto.
//  - href: igual, pero para enlaces externos (por ejemplo mailto:).
export default function Boton({
  variante = 'primario',
  cargando = false,
  textoCargando,
  to,
  href,
  className = '',
  children,
  ...resto
}) {
  const clase = `boton boton--${variante} ${className}`.trim()

  if (to) {
    return (
      <Link to={to} className={clase} {...resto}>
        {children}
      </Link>
    )
  }

  if (href) {
    return (
      <a href={href} className={clase} {...resto}>
        {children}
      </a>
    )
  }

  return (
    <button
      type="button"
      className={clase}
      {...resto}
      disabled={cargando || resto.disabled}
      aria-busy={cargando || undefined}
    >
      {cargando && textoCargando ? textoCargando : children}
    </button>
  )
}
