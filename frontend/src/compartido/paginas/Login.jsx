import { useCallback, useState } from 'react'
import useUnaVez from '../hooks/useUnaVez'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { iniciarSesion } from '../api/auth'
import { SITUACION, avisoDeIngreso } from '../auth/errorLogin'
import { guardarToken } from '../auth/sesion'
import { destinoTrasAutenticacion, estadoRetornoReporte } from '../auth/retornoReporte'
import Aviso from '../componentes/avisos/Aviso'
import Boton from '../componentes/Boton'
import CabeceraAuth from '../componentes/acceso/CabeceraAuth'
import Campo from '../componentes/formulario/Campo'
import CampoPassword from '../componentes/formulario/CampoPassword'
import EncabezadoPantalla from '../componentes/EncabezadoPantalla'
import AvisoIngreso from '../componentes/acceso/AvisoIngreso'
import BotonEsperando from '../componentes/acceso/BotonEsperando'
import BotonGoogle from '../componentes/acceso/BotonGoogle'
import { IconoCorreo } from '../componentes/Iconos'
import LayoutAuth from '../componentes/acceso/LayoutAuth'
import PieAuth from '../componentes/acceso/PieAuth'
import Separador from '../componentes/Separador'
import enfocarPrimerError from '../componentes/formulario/enfocarPrimerError'
import '../estilos/marca.css'
import { finDelBloqueo } from '../textos/ingreso'
import { LIMITES, soloErrores, validarCorreo, validarObligatorio } from '../validaciones/campos'

const MENSAJE_RESTABLECIDA = 'Contraseña actualizada. Inicie sesión con la nueva.'
const MENSAJE_SESION_VENCIDA = 'Su sesión terminó. Inicie sesión de nuevo.'

export default function Login() {
  const [correo, setCorreo] = useState('')
  const [password, setPassword] = useState('')
  const [errores, setErrores] = useState({})
  // Lo que respondió el ingreso, en palabras de la app (errorLogin.js). Un bloqueo trae su hora de fin.
  const [aviso, setAviso] = useState(null)
  const [bloqueoHasta, setBloqueoHasta] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const enviarUnaVez = useUnaVez(enviar)
  const navigate = useNavigate()
  const { state } = useLocation()
  const retorno = estadoRetornoReporte(state?.siguiente ?? null)
  const [params] = useSearchParams()
  const sesionVencida = params.get('motivo') === 'vencida'
  const contrasenaRestablecida = params.get('motivo') === 'restablecida'

  const terminarBloqueo = useCallback(() => {
    setBloqueoHasta(null)
    setAviso(null)
  }, [])

  function cambiarCorreo(evento) {
    setCorreo(evento.target.value)
    // El bloqueo es de ese correo: con otro correo se puede intentar.
    if (bloqueoHasta) terminarBloqueo()
  }

  async function enviar(evento) {
    evento.preventDefault()
    // Durante el bloqueo no se intenta: el servicio respondería lo mismo.
    if (bloqueoHasta) return
    enfocarPrimerError(evento.currentTarget)
    const encontrados = soloErrores({
      correo: validarCorreo(correo),
      password: validarObligatorio(password, 'Escriba su contraseña'),
    })
    setErrores(encontrados)
    setAviso(null)
    if (Object.keys(encontrados).length > 0) return

    setEnviando(true)
    try {
      const { token, rol } = await iniciarSesion(correo.trim(), password)
      guardarToken(token)
      // Cada rol aterriza en su zona: el ciudadano en la PWA y el administrador en el panel.
      // Quien venía de «Inicie sesión para enviar» vuelve a su reporte (state.siguiente).
      const destino = destinoTrasAutenticacion(rol, state?.siguiente ?? null)
      navigate(destino, { replace: true })
    } catch (error) {
      const recibido = avisoDeIngreso(error)
      setAviso(recibido)
      if (recibido.situacion === SITUACION.BLOQUEO) setBloqueoHasta(finDelBloqueo(error.datos))
      setEnviando(false)
    }
  }

  return (
    <LayoutAuth web>
      <EncabezadoPantalla atras="/" />

      <form className="formulario-auth" onSubmit={enviarUnaVez} noValidate>
        <CabeceraAuth titulo="Bienvenido a NotiUrba" texto="Ingrese con su cuenta para continuar" />

        {sesionVencida && <Aviso variante="info">{MENSAJE_SESION_VENCIDA}</Aviso>}
        {contrasenaRestablecida && <Aviso variante="exito">{MENSAJE_RESTABLECIDA}</Aviso>}
        <AvisoIngreso aviso={aviso} correo={correo.trim()} retorno={retorno} />

        <Campo
          id="correo"
          etiqueta="Correo electrónico"
          icono={<IconoCorreo />}
          type="email"
          autoComplete="email"
          maxLength={LIMITES.correo}
          placeholder="nombre@correo.com"
          value={correo}
          onChange={cambiarCorreo}
          error={errores.correo}
        />

        <CampoPassword
          id="password"
          etiqueta="Contraseña"
          autoComplete="current-password"
          placeholder="Ingrese su contraseña"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={errores.password}
          acciones={
            <Link to="/recuperar" className="enlace-campo">
              ¿Olvidó su contraseña?
            </Link>
          }
        />

        {bloqueoHasta ? (
          <BotonEsperando key={bloqueoHasta} fin={bloqueoHasta} alTerminar={terminarBloqueo} />
        ) : (
          <Boton type="submit" cargando={enviando} textoCargando="Ingresando…">
            Iniciar sesión
          </Boton>
        )}

        <Separador texto="O ingrese con" />

        <BotonGoogle alFallar={setAviso} />

        <PieAuth texto="¿Aún no tiene cuenta?" enlace="Regístrese" to="/registro" state={retorno} />
      </form>
    </LayoutAuth>
  )
}
