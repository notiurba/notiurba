import { Link } from 'react-router-dom'
import Aviso from './Aviso'
import './NotaPrivacidad.css'

// «Ver aviso de privacidad»: abre el aviso (público) en la misma pestaña.
export function EnlacePrivacidad() {
  return (
    <Link to="/aviso-privacidad" className="enlace-privacidad">
      Ver aviso de privacidad
    </Link>
  )
}

// Nota que explica para qué se usan los datos (la usan Registro y Completar perfil).
// Es un Aviso en su variante «nota»: así no se repite el escudo ni los estilos.
export default function NotaPrivacidad() {
  return (
    <Aviso variante="nota">
      Sus datos solo se usan para enviar los reportes que usted haga a la Alcaldía o a las
      empresas de servicios públicos. <EnlacePrivacidad />
    </Aviso>
  )
}
