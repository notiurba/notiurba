import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { verificarCorreo } from '../api/auth'
import { guardarToken } from '../auth/sesion'
import { destinoTrasAutenticacion, estadoRetornoReporte } from '../auth/retornoReporte'
import Aviso from '../componentes/avisos/Aviso'
import Boton from '../componentes/Boton'
import LayoutAuth from '../componentes/acceso/LayoutAuth'
import '../estilos/marca.css'
import './VerificarCorreo.css'

// Página a la que lleva el enlace del correo de confirmación: /app/verificar-correo?token=…
// Confirma el correo con la API, que devuelve la sesión, y entra a la app. Si el enlace venció o ya se
// usó, lo dice y manda al ingreso (allí, al intentar entrar, se ofrece reenviar el enlace).
export default function ConfirmarCorreo() {
  const navigate = useNavigate()
  const retorno = estadoRetornoReporte()
  const [params] = useSearchParams()
  const token = params.get('token')
  const [fallo, setFallo] = useState(token ? null : 'enlace')
  // El token sirve una sola vez: en desarrollo React ejecuta el efecto dos veces, así que se pide solo una.
  const yaPedido = useRef(false)

  useEffect(() => {
    if (!token || yaPedido.current) return
    yaPedido.current = true
    verificarCorreo(token)
      .then(({ token: sesion, rol }) => {
        guardarToken(sesion)
        navigate(destinoTrasAutenticacion(rol), { replace: true })
      })
      .catch((error) => setFallo(error.codigo === 'ENLACE_INVALIDO' ? 'enlace' : 'conexion'))
  }, [token, navigate])

  return (
    <LayoutAuth>
      <div className="verificar">
        {fallo === null && <p className="cargando-texto">Confirmando su correo…</p>}

        {fallo === 'enlace' && (
          <>
            <h1>Este enlace ya no sirve</h1>
            <Aviso variante="error">
              El enlace venció o ya se usó. Inicie sesión: si su correo aún no está confirmado, le
              ofreceremos enviar uno nuevo.
            </Aviso>
            <Boton to="/login" state={retorno}>Ir a iniciar sesión</Boton>
          </>
        )}

        {fallo === 'conexion' && (
          <>
            <h1>No pudimos confirmar su correo</h1>
            <Aviso variante="error">Revise su conexión y vuelva a abrir el enlace del correo.</Aviso>
            <Link to="/login" state={retorno} className="enlace-campo">
              Ir a iniciar sesión
            </Link>
          </>
        )}
      </div>
    </LayoutAuth>
  )
}
