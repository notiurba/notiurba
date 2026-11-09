import { TEXTO_CASILLA_REGISTRO } from '../../textos/avisoPrivacidad'
import { EnlacePrivacidad } from '../avisos/NotaPrivacidad'
import Casilla from './Casilla'

// Una sola aceptación para registro y Google; el aviso v3 se consulta sin perder el formulario.
export default function ConsentimientoRegistro({ datos, errores, cambiar }) {
  return (
    <Casilla
      id="aceptaPrivacidad"
      checked={datos.aceptaPrivacidad}
      onChange={cambiar('aceptaPrivacidad')}
      error={errores.aceptaPrivacidad}
    >
      {TEXTO_CASILLA_REGISTRO} <EnlacePrivacidad nuevaPestana />
    </Casilla>
  )
}
