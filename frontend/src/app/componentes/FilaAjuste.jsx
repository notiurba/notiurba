import { Link } from 'react-router-dom'
import { IconoChevronDerecha } from '../../compartido/componentes/Iconos'
import './FilaAjuste.css'

// Una fila de Ajustes: ícono en cuadro de color, título, descripción y flecha.
// Con `to` es un enlace; con `alPulsar` es un botón (por ejemplo para abrir una hoja de confirmación).
//  - peligro: la fila va en rojo (eliminar datos o cuenta).
export default function FilaAjuste({ icono, titulo, descripcion, to, alPulsar, peligro = false }) {
  const clase = peligro ? 'fila-ajuste fila-ajuste--peligro' : 'fila-ajuste'
  const contenido = (
    <>
      <span className="fila-ajuste__icono">{icono}</span>
      <span className="fila-ajuste__textos">
        <span className="fila-ajuste__titulo">{titulo}</span>
        {descripcion && <span className="fila-ajuste__descripcion">{descripcion}</span>}
      </span>
      <IconoChevronDerecha tamano={18} />
    </>
  )

  if (to) {
    return (
      <Link to={to} className={clase}>
        {contenido}
      </Link>
    )
  }

  return (
    <button type="button" className={clase} onClick={alPulsar}>
      {contenido}
    </button>
  )
}
