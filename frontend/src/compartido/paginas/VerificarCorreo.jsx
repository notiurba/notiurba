import { useState } from 'react'
import useUnaVez from '../hooks/useUnaVez'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { estadoRetornoReporte } from '../auth/retornoReporte'
import { reenviarVerificacion } from '../api/auth'
import Aviso from '../componentes/avisos/Aviso'
import Boton from '../componentes/Boton'
import EncabezadoPantalla from '../componentes/EncabezadoPantalla'
import { IconoCorreo } from '../componentes/Iconos'
import LayoutAuth from '../componentes/acceso/LayoutAuth'
import '../estilos/marca.css'
import './VerificarCorreo.css'

// Pantalla que sigue al registro: pide abrir el enlace de confirmación que llegó al correo.
// El correo viaja en el estado de la navegación; si alguien entra directo a la dirección, vuelve al login.
export default function VerificarCorreo() {
  const { state } = useLocation()
  const navigate = useNavigate()
  const retorno = estadoRetornoReporte(state?.siguiente)
  const [reenviando, setReenviando] = useState(false)
  // Un doble clic o un doble Enter no repite el envío mientras se espera la respuesta.
  const reenviarUnaVez = useUnaVez(reenviar)
  const [resultado, setResultado] = useState(null) // { tipo: 'exito' | 'error', texto }
  if (!state?.correo) return <Navigate to="/login" state={retorno} replace />

  // La API limita a un reenvío por minuto y responde siempre lo mismo exista o no la cuenta.
  async function reenviar() {
    setReenviando(true)
    setResultado(null)
    try {
      const { mensaje } = await reenviarVerificacion(state.correo)
      setResultado({ tipo: 'exito', texto: mensaje })
    } catch (error) {
      setResultado({ tipo: 'error', texto: error.mensaje ?? 'No pudimos reenviar el enlace. Inténtelo en un momento.' })
    }
    setReenviando(false)
  }

  return (
    <LayoutAuth>
      <EncabezadoPantalla alAtras={() => navigate('/login', { state: retorno })} />

      <div className="verificar">
        <div className="verificar__sobre" aria-hidden="true">
          <IconoCorreo tamano={64} />
        </div>
        <h1>Revise su correo</h1>
        <p>
          Enviamos un enlace de confirmación a <strong>{state.correo}</strong>. Ábralo para activar su
          cuenta.
        </p>

        <Boton href="mailto:">Abrir mi correo</Boton>
        <Boton variante="secundario" onClick={reenviarUnaVez} cargando={reenviando} textoCargando="Reenviando…">
          Reenviar enlace
        </Boton>

        {resultado && <Aviso variante={resultado.tipo}>{resultado.texto}</Aviso>}

        <Aviso variante="info">
          ¿No lo encuentra? Revise la carpeta de spam. ¿Necesita ayuda? Escríbanos a
          notiurba@gmail.com.
        </Aviso>
      </div>
    </LayoutAuth>
  )
}
