import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { iniciarSesionConGoogle } from '../../api/auth'
import { guardarToken } from '../../auth/sesion'
import { avisoDeGoogle } from '../../auth/errorLogin'
import { destinoTrasAutenticacion, estadoRetornoReporte } from '../../auth/retornoReporte'
import { TEXTOS_INGRESO, TITULOS_INGRESO } from '../../textos/ingreso'
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

function BotonGoogleReal({ alFallar }) {
  const navigate = useNavigate()
  const { state } = useLocation()
  const siguiente = estadoRetornoReporte(state?.siguiente ?? null)?.siguiente
  const contenedor = useRef(null)
  const [error, setError] = useState(null)
  // La función de la pantalla puede cambiar en cada render: se guarda aparte para no recargar el botón de Google.
  const alFallarActual = useRef(alFallar)
  useEffect(() => {
    alFallarActual.current = alFallar
  }, [alFallar])

  useEffect(() => {
    let activo = true
    const avisar = (aviso) => (alFallarActual.current ? alFallarActual.current(aviso) : setError(aviso))

    // Google devuelve el credential; la API responde la sesión o «PERFIL_INCOMPLETO» (cuenta nueva).
    async function alRecibirCredencial({ credential }) {
      avisar(null)
      try {
        const respuesta = await iniciarSesionConGoogle(credential)
        if (respuesta.estado === 'PERFIL_INCOMPLETO') {
          navigate('/completar-perfil', { state: { ...respuesta, siguiente } })
          return
        }
        guardarToken(respuesta.token)
        navigate(destinoTrasAutenticacion(respuesta.rol, siguiente ?? null), { replace: true })
      } catch (fallo) {
        avisar(avisoDeGoogle(fallo))
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
      .catch(() => activo && avisar({ titulo: TITULOS_INGRESO.google, mensaje: TEXTOS_INGRESO.googleNoCargo }))

    return () => {
      activo = false
    }
  }, [navigate, siguiente])

  return (
    <>
      <div ref={contenedor} style={{ display: 'flex', justifyContent: 'center' }} />
      {error && (
        <Aviso variante="error" titulo={error.titulo}>
          {error.mensaje}
        </Aviso>
      )}
    </>
  )
}

// alFallar: si la pantalla tiene su propia zona de aviso (el ingreso), el error de Google va allí.
export default function BotonGoogle({ alFallar }) {
  const { state } = useLocation()
  if (!SIMULANDO && ID_CLIENTE) return <BotonGoogleReal alFallar={alFallar} />

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
