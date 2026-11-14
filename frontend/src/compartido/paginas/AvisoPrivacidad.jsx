import { useLocation, useNavigate } from 'react-router-dom'
import EncabezadoPantalla from '../componentes/EncabezadoPantalla'
import LayoutAuth from '../componentes/acceso/LayoutAuth'
import ContenidoAvisoPrivacidad from '../componentes/avisos/ContenidoAvisoPrivacidad'
import '../estilos/marca.css'

// Aviso de privacidad (Ley 1581). Es público: se abre desde el registro, los ajustes y el envío del reporte.
export default function AvisoPrivacidad() {
  const navigate = useNavigate()
  // Si la persona llegó desde otra pantalla (Ajustes, el registro o el envío), «atrás» vuelve allí.
  const vieneDeOtraPantalla = useLocation().key !== 'default'
  return (
    <LayoutAuth>
      <EncabezadoPantalla titulo="Aviso de privacidad" atras="/" alAtras={vieneDeOtraPantalla ? () => navigate(-1) : undefined} />
      <ContenidoAvisoPrivacidad />
    </LayoutAuth>
  )
}
