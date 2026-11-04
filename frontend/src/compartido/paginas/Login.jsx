import { useState } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { iniciarSesion } from '../api/auth'
import { guardarToken } from '../auth/sesion'
import Aviso from '../componentes/Aviso'
import Boton from '../componentes/Boton'
import CabeceraAuth from '../componentes/CabeceraAuth'
import Campo from '../componentes/Campo'
import CampoPassword from '../componentes/CampoPassword'
import EncabezadoPantalla from '../componentes/EncabezadoPantalla'
import BotonGoogle from '../componentes/BotonGoogle'
import { IconoCorreo } from '../componentes/Iconos'
import LayoutAuth from '../componentes/LayoutAuth'
import PieAuth from '../componentes/PieAuth'
import Separador from '../componentes/Separador'
import '../estilos/marca.css'
import { LIMITES, soloErrores, validarCorreo, validarObligatorio } from '../validaciones/campos'

// Cuando el servidor dice QUÉ campo falló, el mensaje va bajo esa casilla (decisión del equipo:
// que el usuario sepa dónde está el error). Si no lo dice, se usa el mensaje general.
const MENSAJE_POR_CAMPO = {
  correo: 'No encontramos una cuenta con ese correo. Revíselo o regístrese.',
  password: 'La contraseña no es correcta. Inténtelo de nuevo.',
}
const MENSAJE_CREDENCIALES = 'Correo o contraseña incorrectos. Revise sus datos e inténtelo de nuevo.'
const MENSAJE_SIN_CONEXION = 'No pudimos conectar. Inténtelo de nuevo en un momento.'
const MENSAJE_SIN_VERIFICAR = 'Falta confirmar su correo. Abra el enlace que le enviamos al registrarse.'
const MENSAJE_RESTABLECIDA = 'Contraseña actualizada. Inicie sesión con la nueva.'
const MENSAJE_SESION_VENCIDA = 'Su sesión terminó. Inicie sesión de nuevo.'

export default function Login() {
  const [correo, setCorreo] = useState('')
  const [password, setPassword] = useState('')
  const [errores, setErrores] = useState({})
  const [errorGeneral, setErrorGeneral] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [sinVerificar, setSinVerificar] = useState(false)
  const navigate = useNavigate()
  const { state } = useLocation()
  const [params] = useSearchParams()
  const sesionVencida = params.get('motivo') === 'vencida'
  const contrasenaRestablecida = params.get('motivo') === 'restablecida'

  async function enviar(evento) {
    evento.preventDefault()
    const encontrados = soloErrores({
      correo: validarCorreo(correo),
      password: validarObligatorio(password, 'Escriba su contraseña'),
    })
    setErrores(encontrados)
    setErrorGeneral('')
    setSinVerificar(false)
    if (Object.keys(encontrados).length > 0) return

    setEnviando(true)
    try {
      const { token, rol } = await iniciarSesion(correo.trim(), password)
      guardarToken(token)
      // Cada rol aterriza en su zona: el ciudadano en la PWA y el administrador en el panel.
      // Quien venía de «Inicie sesión para enviar» vuelve a su reporte (state.siguiente).
      const destino = rol === 'ADMIN' ? '/admin' : (state?.siguiente ?? '/app')
      navigate(destino, { replace: true })
    } catch (error) {
      if (error.codigo === 'CORREO_NO_VERIFICADO') {
        // La contraseña era correcta, pero falta abrir el enlace del correo: se ofrece reenviarlo.
        setSinVerificar(true)
      } else if (error.estado === 401 && MENSAJE_POR_CAMPO[error.campo]) {
        // Si el servidor dijera cuál campo falló, el mensaje iría bajo esa casilla (hoy no lo dice).
        setErrores({ [error.campo]: error.mensaje ?? MENSAJE_POR_CAMPO[error.campo] })
      } else if (error.codigo === 'CREDENCIALES_INVALIDAS' || error.estado === 401) {
        // La API no dice cuál dato falló (RF-02): mensaje general.
        setErrorGeneral(MENSAJE_CREDENCIALES)
      } else {
        setErrorGeneral(error.codigo === 'SIN_CONEXION' ? MENSAJE_SIN_CONEXION : error.mensaje ?? MENSAJE_SIN_CONEXION)
      }
      setEnviando(false)
    }
  }

  return (
    <LayoutAuth web>
      <EncabezadoPantalla atras="/" />

      <form className="formulario-auth" onSubmit={enviar} noValidate>
        <CabeceraAuth titulo="Bienvenido a NotiUrba" texto="Ingrese con su cuenta para continuar" />

        {sesionVencida && <Aviso variante="info">{MENSAJE_SESION_VENCIDA}</Aviso>}
        {contrasenaRestablecida && <Aviso variante="exito">{MENSAJE_RESTABLECIDA}</Aviso>}
        {errorGeneral && (
          <Aviso variante="error" titulo="No pudimos iniciar sesión">
            {errorGeneral}
          </Aviso>
        )}
        {sinVerificar && (
          <Aviso variante="alerta" titulo="Confirme su correo">
            {MENSAJE_SIN_VERIFICAR}{' '}
            <Link to="/verificar-correo" state={{ correo: correo.trim() }} className="enlace-campo">
              Reenviar enlace
            </Link>
          </Aviso>
        )}

        <Campo
          id="correo"
          etiqueta="Correo electrónico"
          icono={<IconoCorreo />}
          type="email"
          autoComplete="email"
          maxLength={LIMITES.correo}
          placeholder="nombre@correo.com"
          value={correo}
          onChange={(e) => setCorreo(e.target.value)}
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

        <Boton type="submit" cargando={enviando} textoCargando="Ingresando…">
          Iniciar sesión
        </Boton>

        <Separador texto="O ingrese con" />

        <BotonGoogle />

        <PieAuth texto="¿Aún no tiene cuenta?" enlace="Regístrese" to="/registro" />
      </form>
    </LayoutAuth>
  )
}
