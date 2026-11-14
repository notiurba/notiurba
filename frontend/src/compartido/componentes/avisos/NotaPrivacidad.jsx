import { Link } from 'react-router-dom'
import Aviso from './Aviso'
import './NotaPrivacidad.css'

// Abre el aviso público v3; durante el registro conserva el formulario en su pestaña.
export function EnlacePrivacidad({ nuevaPestana = false }) {
  return (
    <Link
      to="/aviso-privacidad"
      className="enlace-privacidad"
      target={nuevaPestana ? '_blank' : undefined}
      rel={nuevaPestana ? 'noopener noreferrer' : undefined}
    >
      {nuevaPestana ? 'Aviso de privacidad' : 'Ver aviso de privacidad'}
    </Link>
  )
}

// Nota que explica para qué se usan los datos (la usan Registro y Completar perfil).
// Es un Aviso en su variante «nota»: así no se repite el escudo ni los estilos.
export default function NotaPrivacidad() {
  return (
    <Aviso variante="nota">
      Sus datos solo se usan para enviar los reportes que usted haga a la Alcaldía o a las
      empresas de servicios públicos. <EnlacePrivacidad nuevaPestana />
    </Aviso>
  )
}
