import CampoBase from './CampoBase'
import { ariaCampo } from './ariaCampo'
import { IconoChevron } from './Iconos'

// Lista desplegable. Usa el <select> del navegador: en el celular abre su selector nativo,
// que es lo más cómodo y accesible. `opciones` es una lista de { valor, texto }.
export default function Selector({ id, etiqueta, opcional, error, ayuda, opciones, marcador = 'Elegir', ...resto }) {
  return (
    <CampoBase id={id} etiqueta={etiqueta} opcional={opcional} error={error} ayuda={ayuda}>
      <select id={id} {...resto} {...ariaCampo(id, error, ayuda)}>
        <option value="" disabled>
          {marcador}
        </option>
        {opciones.map((opcion) => (
          <option key={opcion.valor} value={opcion.valor}>
            {opcion.texto}
          </option>
        ))}
      </select>
      <span className="campo__chevron">
        <IconoChevron />
      </span>
    </CampoBase>
  )
}
