import { Link } from 'react-router-dom'
import { SITUACION } from '../../auth/errorLogin'
import Aviso from '../avisos/Aviso'

// Una sola zona de aviso para el ingreso: el mensaje de la situación y, si la hay, su acción.
export default function AvisoIngreso({ aviso, correo = '', retorno = null }) {
  if (!aviso) return null

  if (aviso.situacion === SITUACION.SIN_CONFIRMAR) {
    return (
      <Aviso variante="alerta" titulo={aviso.titulo}>
        {aviso.mensaje}{' '}
        <Link to="/verificar-correo" state={{ correo, ...retorno }} className="enlace-campo">
          Reenviar enlace
        </Link>
      </Aviso>
    )
  }

  if (aviso.situacion === SITUACION.BLOQUEO) {
    return (
      <Aviso variante="alerta" titulo={aviso.titulo}>
        {aviso.mensaje}{' '}
        <Link to="/recuperar" className="enlace-campo">
          Restablecer mi contraseña
        </Link>
      </Aviso>
    )
  }

  return (
    <Aviso variante="error" titulo={aviso.titulo}>
      {aviso.mensaje}
    </Aviso>
  )
}
