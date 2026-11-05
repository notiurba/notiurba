import { useEffect, useState } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import { EVENTO_SESION_TERMINADA } from '../../compartido/api/cliente'
import Boton from '../../compartido/componentes/Boton'
import HojaInferior from '../../compartido/componentes/avisos/HojaInferior'
import { IconoReloj } from '../../compartido/componentes/Iconos'
import MarcoApp from './MarcoApp'
import PestanaNavegacion from './PestanaNavegacion'

// Marco de las pantallas de las pestañas: contenido arriba y barra de navegación abajo.
// También atiende el aviso «Su sesión terminó» que lanza el cliente de la API cuando un token deja de servir.
export default function LayoutApp() {
  const navigate = useNavigate()
  const [sesionTerminada, setSesionTerminada] = useState(false)

  useEffect(() => {
    function alTerminarSesion(evento) {
      evento.preventDefault() // avisa al cliente que esta pantalla se encarga (no redirige)
      setSesionTerminada(true)
    }
    window.addEventListener(EVENTO_SESION_TERMINADA, alTerminarSesion)
    return () => window.removeEventListener(EVENTO_SESION_TERMINADA, alTerminarSesion)
  }, [])

  return (
    <MarcoApp pie={<PestanaNavegacion />}>
      <Outlet />

      <HojaInferior
        abierta={sesionTerminada}
        alCerrar={() => navigate('/', { replace: true })}
        icono={<IconoReloj tamano={30} />}
        tono="alerta"
        titulo="Su sesión terminó"
        texto="Por seguridad, vuelva a iniciar sesión para continuar."
      >
        <Boton onClick={() => navigate('/login', { replace: true })}>Iniciar sesión</Boton>
        <Boton variante="texto" onClick={() => navigate('/', { replace: true })}>
          Ahora no
        </Boton>
      </HojaInferior>
    </MarcoApp>
  )
}
