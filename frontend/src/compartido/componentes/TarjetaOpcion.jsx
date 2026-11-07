import { IconoCheck } from './Iconos'
import './TarjetaOpcion.css'

// Tarjeta de selección única (una sola opción entre varias): es un <input type="radio"> de verdad,
// así funciona con teclado y lector de pantalla. La usan la lista de categorías y «Actualizar mi reporte».
//  - grupo: el `name` común de todas las tarjetas de la lista.  - icono: opcional, a la izquierda.
export default function TarjetaOpcion({ grupo, valor, seleccionada, alElegir, icono, titulo, descripcion }) {
  return (
    <label className={seleccionada ? 'tarjeta-opcion tarjeta-opcion--elegida' : 'tarjeta-opcion'}>
      <input
        type="radio"
        name={grupo}
        value={valor}
        checked={seleccionada}
        onChange={() => alElegir(valor)}
      />
      {icono}
      <span className="tarjeta-opcion__textos">
        <span className="tarjeta-opcion__titulo">{titulo}</span>
        {descripcion && <span className="tarjeta-opcion__descripcion">{descripcion}</span>}
      </span>
      <span className="tarjeta-opcion__marca" aria-hidden="true">
        {seleccionada && <IconoCheck tamano={14} />}
      </span>
    </label>
  )
}
