import CampoBase from './CampoBase'
import { ariaCampo } from './ariaCampo'

// Campo de varias líneas (por ejemplo, la descripción del reporte). Igual que Campo pero con <textarea>.
export default function AreaTexto({ id, etiqueta, opcional, acciones, error, ayuda, ...entrada }) {
  return (
    <CampoBase id={id} etiqueta={etiqueta} opcional={opcional} acciones={acciones} error={error} ayuda={ayuda} alta>
      <textarea id={id} {...entrada} {...ariaCampo(id, error, ayuda)} />
    </CampoBase>
  )
}
