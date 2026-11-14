import { Link } from 'react-router-dom'
import IconoCategoria from '../../compartido/componentes/IconoCategoria'
import './ResumenCategoria.css'

// Tarjeta «Tipo de problema … Cambiar» que se repite arriba de los pasos 2 y 3.
export default function ResumenCategoria({ categoria }) {
  return (
    <div className="resumen-categoria">
      <IconoCategoria idCategoria={categoria.idCategoria} />
      <div>
        <span>Tipo de problema</span>
        <strong>{categoria.nombre}</strong>
      </div>
      <Link to="/reportar">Cambiar</Link>
    </div>
  )
}
