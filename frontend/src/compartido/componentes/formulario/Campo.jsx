import CampoBase from './CampoBase'
import { ariaCampo } from './ariaCampo'

// Campo de texto: etiqueta, ícono opcional a la izquierda y mensaje de error o ayuda.
// Todo lo que no es de "presentación" (type, value, onChange, placeholder...) pasa directo al <input>.
export default function Campo({ id, etiqueta, opcional, acciones, error, ayuda, icono, final, ...entrada }) {
  return (
    <CampoBase id={id} etiqueta={etiqueta} opcional={opcional} acciones={acciones} error={error} ayuda={ayuda}>
      {icono && <span className="campo__icono">{icono}</span>}
      <input id={id} {...entrada} {...ariaCampo(id, error, ayuda)} />
      {final}
    </CampoBase>
  )
}
