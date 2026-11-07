import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { iniciarSesionConGoogle } from '../../api/auth'
import { RUTA_POR_ROL, guardarToken } from '../../auth/sesion'
import Aviso from '../avisos/Aviso'
import Boton from '../Boton'
import { IconoGoogle } from '../Iconos'

// «Continuar con Google». Tiene tres modos:
//  - Simulación (VITE_USAR_MOCKS=true): lleva a «Complete su perfil» para poder ver esa pantalla.
//  - Real: si hay VITE_GOOGLE_CLIENT_ID, muestra el botón oficial de Google (Google Identity Services).
//    Google entrega un «credential» (ID token) y se manda a POST /api/auth/google.
//  - Sin ID de cliente: el botón queda apagado con «Próximamente».
const SIMULANDO = import.meta.env.VITE_USAR_MOCKS === 'true'
const ID_CLIENTE = import.meta.env.VITE_GOOGLE_CLIENT_ID

// Carga el script de Google una sola vez.
function cargarScriptGoogle() {
  return new Promise((resolver, rechazar) => {
    if (window.google?.accounts?.id) return resolver()
    const existente = document.querySelector('script[data-google-gsi]')
    const script = existente ?? document.createElement('script')
    script.addEventListener('load', resolver)
    script.addEventListener('error', rechazar)
    if (!existente) {
      script.src = 'https://accounts.google.com/gsi/client'
      script.async = true
      script.dataset.googleGsi = 'true'
      document.head.appendChild(script)
    }
  })
}

function BotonGoogleReal() {
  const navigate = useNavigate()
  const contenedor = useRef(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let activo = true

    // Google devuelve el credential; la API responde la sesión o «PERFIL_INCOMPLETO» (cuenta nueva).
    async function alRecibirCredencial({ credential }) {
      setError('')
      try {
        const respuesta = await iniciarSesionConGoogle(credential)
        if (respuesta.estado === 'PERFIL_INCOMPLETO') {
          navigate('/completar-perfil', { state: respuesta })
          return
        }
        guardarToken(respuesta.token)
        navigate(RUTA_POR_ROL[respuesta.rol] ?? '/app', { replace: true })
      } catch (fallo) {
        setError(fallo.mensaje ?? 'No pudimos ingresar con Google. Inténtelo de nuevo.')
      }
    }

    cargarScriptGoogle()
      .then(() => {
        if (!activo || !contenedor.current) return
        window.google.accounts.id.initialize({ client_id: ID_CLIENTE, callback: alRecibirCredencial })
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
    }
  }, [navigate])

  return (
    <>
      <div ref={contenedor} style={{ display: 'flex', justifyContent: 'center' }} />
      {error && <Aviso variante="error">{error}</Aviso>}
    </>
  )
}

export default function BotonGoogle() {
  if (!SIMULANDO && ID_CLIENTE) return <BotonGoogleReal />

  const propiedades = SIMULANDO
    ? { to: '/completar-perfil', state: { correo: 'nombre@gmail.com' } }
    : { disabled: true, title: 'Próximamente' }

  return (
    <Boton variante="secundario" {...propiedades}>
      <IconoGoogle />
      Continuar con Google
    </Boton>
  )
}
