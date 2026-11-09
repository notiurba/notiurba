import EncabezadoPantalla from '../componentes/EncabezadoPantalla'
import LayoutAuth from '../componentes/acceso/LayoutAuth'
import ContenidoAvisoPrivacidad from '../componentes/avisos/ContenidoAvisoPrivacidad'
import '../estilos/marca.css'

// Aviso de privacidad (Ley 1581). Es público: se abre desde el registro, los ajustes y el envío del reporte.
export default function AvisoPrivacidad() {
  return (
    <LayoutAuth>
      <EncabezadoPantalla titulo="Aviso de privacidad" atras="/" />
      <ContenidoAvisoPrivacidad />
    </LayoutAuth>
  )
}
