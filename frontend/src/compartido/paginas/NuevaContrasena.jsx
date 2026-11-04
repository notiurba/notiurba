import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { restablecerPassword } from '../api/auth'
import Aviso from '../componentes/Aviso'
import Boton from '../componentes/Boton'
import CabeceraAuth from '../componentes/CabeceraAuth'
import CampoPassword from '../componentes/CampoPassword'
import EncabezadoPantalla from '../componentes/EncabezadoPantalla'
import LayoutAuth from '../componentes/LayoutAuth'
import PieAuth from '../componentes/PieAuth'
import '../estilos/marca.css'
import useFormulario from '../hooks/useFormulario'
import { soloErrores, validarConfirmacion, validarPasswordNueva } from '../validaciones/campos'

// El enlace del correo trae el token: /nueva-contrasena?token=…  Sin token o con token vencido
// se avisa y se ofrece pedir otro enlace.
export default function NuevaContrasena() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const token = params.get('token')
  const { datos, errores, setErrores, cambiar } = useFormulario({ password: '', confirmar: '' })
  const [enlaceInvalido, setEnlaceInvalido] = useState(!token)
  const [fallo, setFallo] = useState(false)
  const [enviando, setEnviando] = useState(false)

  async function guardar(evento) {
    evento.preventDefault()
    const encontrados = soloErrores({
      password: validarPasswordNueva(datos.password),
      confirmar: validarConfirmacion(datos.password, datos.confirmar),
    })
    setErrores(encontrados)
    setFallo(false)
    if (Object.keys(encontrados).length > 0) return

    setEnviando(true)
    try {
      await restablecerPassword(token, datos.password)
      navigate('/login?motivo=restablecida', { replace: true })
    } catch (error) {
      // ENLACE_INVALIDO = el enlace venció o ya se usó. VALIDACION en nuevaContrasena = la contraseña no cumple.
      // Cualquier otro fallo es de conexión.
      if (error.codigo === 'ENLACE_INVALIDO' || (error.estado === 400 && error.campo === 'token')) setEnlaceInvalido(true)
      else if (error.codigo === 'VALIDACION' && error.campo === 'nuevaContrasena') setErrores({ password: error.mensaje })
      else setFallo(true)
      setEnviando(false)
    }
  }

  return (
    <LayoutAuth>
      <EncabezadoPantalla atras="/login" />

      <form className="formulario-auth" onSubmit={guardar} noValidate>
        <CabeceraAuth
          titulo="Cree una nueva contraseña"
          texto="Elija una contraseña que no haya usado antes en NotiUrba."
        />

        {enlaceInvalido && (
          <Aviso variante="error" titulo="Este enlace ya no sirve">
            El enlace venció o ya se usó. Pida uno nuevo para continuar.
          </Aviso>
        )}
        {fallo && (
          <Aviso variante="error" titulo="No pudimos guardar la contraseña">
            Revise su conexión e inténtelo de nuevo.
          </Aviso>
        )}

        <CampoPassword
          id="password"
          etiqueta="Nueva contraseña"
          autoComplete="new-password"
          placeholder="Cree una contraseña"
          ayuda="Mínimo 8 caracteres, con letras y números. Puede usar símbolos."
          value={datos.password}
          onChange={cambiar('password')}
          error={errores.password}
        />
        <CampoPassword
          id="confirmar"
          etiqueta="Confirmar contraseña"
          autoComplete="new-password"
          placeholder="Repita su contraseña"
          value={datos.confirmar}
          onChange={cambiar('confirmar')}
          error={errores.confirmar}
        />

        <Boton type="submit" disabled={enlaceInvalido} cargando={enviando} textoCargando="Guardando…">
          Guardar contraseña
        </Boton>

        <PieAuth anclado texto="¿El enlace venció?" enlace="Solicitar otro" to="/recuperar" />
      </form>
    </LayoutAuth>
  )
}
