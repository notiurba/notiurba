import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { iniciarSesionConGoogle } from '../../api/auth'
import { guardarToken } from '../../auth/sesion'
import { googleSdk } from '../../auth/googleSdk'
import { destinoTrasAutenticacion, estadoRetornoReporte } from '../../auth/retornoReporte'
import Aviso from '../avisos/Aviso'
import Boton from '../Boton'
import { IconoGoogle } from '../Iconos'

// «Continuar con Google». Tiene tres modos:
//  - Simulación (VITE_USAR_MOCKS=true): lleva a «Complete su perfil» para poder ver esa pantalla.
//  - Real: si hay VITE_GOOGLE_CLIENT_ID, muestra el botón oficial de Google (Google Identity Services).
//    Google entrega un «credential» (ID token) y se manda a POST /api/auth/google.
//  - Sin ID de cliente: el botón queda apagado con «Próximamente».
const SIMULANDO = import.meta.env.DEV && import.meta.env.VITE_USAR_MOCKS === 'true'
const ID_CLIENTE = import.meta.env.VITE_GOOGLE_CLIENT_ID?.trim()

function BotonGoogleReal() {
  const navigate = useNavigate()
  const { state } = useLocation()
  const siguiente = estadoRetornoReporte(state?.siguiente ?? null)?.siguiente
  const contenedor = useRef(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let activo = true
    let procesando = false
    let desconectar = () => {}

    // Google devuelve el credential; la API responde la sesión o «PERFIL_INCOMPLETO» (cuenta nueva).
    async function alRecibirCredencial({ credential } = {}) {
      if (!activo || procesando || !credential) return
      procesando = true
      setError('')
      try {
        const respuesta = await iniciarSesionConGoogle(credential)
        if (!activo) return
        if (respuesta.estado === 'PERFIL_INCOMPLETO') {
          navigate('/completar-perfil', { state: { ...respuesta, siguiente } })
          return
        }
        guardarToken(respuesta.token)
        navigate(destinoTrasAutenticacion(respuesta.rol, siguiente ?? null), { replace: true })
      } catch (fallo) {
        if (activo) setError(fallo.mensaje ?? 'No pudimos ingresar con Google. Inténtelo de nuevo.')
      } finally {
        procesando = false
      }
    }

    googleSdk().cargar()
      .then(() => {
        if (!activo || !contenedor.current) return
        desconectar = googleSdk().conectar(ID_CLIENTE, alRecibirCredencial)
        contenedor.current.replaceChildren()
        window.google.accounts.id.renderButton(contenedor.current, {
          theme: 'outline',
          size: 'large',
          text: 'continue_with',
          locale: 'es',
          width: Math.min(400, contenedor.current.offsetWidth || 320),
        })
      })
      .catch(() => activo && setError('No pudimos cargar Google. Revise su conexión.'))

    return () => {
      activo = false
      desconectar()
    }
  }, [navigate, siguiente])

  return (
    <>
      <div ref={contenedor} style={{ display: 'flex', justifyContent: 'center', width: '100%' }} />
      {error && <Aviso variante="error">{error}</Aviso>}
    </>
  )
}

export default function BotonGoogle() {
  const { state } = useLocation()
  if (!SIMULANDO && ID_CLIENTE) return <BotonGoogleReal />

  const propiedades = SIMULANDO
    ? { to: '/completar-perfil', state: { correo: 'nombre@gmail.com', ...estadoRetornoReporte(state?.siguiente ?? null) } }
    : { disabled: true, title: 'El ingreso con Google aún no está disponible.', 'aria-describedby': 'google-no-disponible' }

  return (
    <>
    <Boton variante="secundario" {...propiedades}>
      <IconoGoogle />
      Continuar con Google
    </Boton>
    {!SIMULANDO && <p id="google-no-disponible" className="campo__ayuda">El ingreso con Google aún no está disponible. Puede usar su correo y contraseña.</p>}
    </>
  )
}
