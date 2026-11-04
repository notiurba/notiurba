import { useNavigate } from 'react-router-dom'
import { cerrarSesion } from '../auth/sesion'
import Boton from './Boton'

export default function BotonCerrarSesion() {
  const navigate = useNavigate()

  function salir() {
    cerrarSesion()
    navigate('/', { replace: true })
  }

  return (
    <Boton variante="secundario" onClick={salir}>
      Cerrar sesión
    </Boton>
  )
}
