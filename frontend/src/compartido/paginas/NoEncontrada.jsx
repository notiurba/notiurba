import { useLocation } from 'react-router-dom'
import Boton from '../componentes/Boton'
import EstadoVacio from '../componentes/EstadoVacio'
import { IconoPin } from '../componentes/Iconos'
import LayoutAuth from '../componentes/LayoutAuth'
import '../estilos/marca.css'

// Se muestra cuando la dirección no existe (por ejemplo, un enlace mal escrito), en lugar de dejar la página en blanco.
// Dentro de /app o /admin el botón lleva al inicio de esa zona; en cualquier otro sitio, a la pantalla de bienvenida.
export default function NoEncontrada() {
  const { pathname } = useLocation()
  const inicio = pathname.startsWith('/admin') ? '/admin' : pathname.startsWith('/app') ? '/app' : '/'

  return (
    <LayoutAuth>
      <EstadoVacio
        tono="alerta"
        icono={<IconoPin tamano={60} />}
        titulo="No encontramos esta página"
        texto="Puede que el enlace esté incompleto o que la página ya no exista."
      >
        <Boton to={inicio}>Ir al inicio</Boton>
      </EstadoVacio>
    </LayoutAuth>
  )
}
