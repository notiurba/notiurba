import { useState } from 'react'
import useUnaVez from '../hooks/useUnaVez'
import { useLocation, useNavigate } from 'react-router-dom'
import { completarPerfilGoogle } from '../api/auth'
import { obtenerBarrios, obtenerTiposDocumento } from '../api/catalogos'
import { RUTA_POR_ROL, guardarToken } from '../auth/sesion'
import Aviso from '../componentes/avisos/Aviso'
import EstadoCarga from '../componentes/avisos/EstadoCarga'
import Boton from '../componentes/Boton'
import { CamposIdentidad, CamposResidencia } from '../componentes/formulario/CamposPerfil'
import ConsentimientoRegistro from '../componentes/formulario/ConsentimientoRegistro'
import EncabezadoPantalla from '../componentes/EncabezadoPantalla'
import FormularioPantalla from '../componentes/formulario/FormularioPantalla'
import LayoutAuth from '../componentes/acceso/LayoutAuth'
import NotaPrivacidad from '../componentes/avisos/NotaPrivacidad'
import '../estilos/marca.css'
import useCargar from '../hooks/useCargar'
import useFormulario from '../hooks/useFormulario'
import { armarDatosPerfil, DATOS_PERFIL_VACIOS, MENSAJE_ACEPTAR, validarPerfil } from '../validaciones/registro'

// Quien entró con Google ya tiene nombre y correo, pero faltan documento, barrio y dirección.
export default function CompletarPerfil() {
  const navigate = useNavigate()
  const { state } = useLocation()
  const { datos, errores, setErrores, cambiar, errorDeCampo } = useFormulario({
    ...DATOS_PERFIL_VACIOS,
    nombres: state?.nombres ?? '',
    apellidos: state?.apellidos ?? '',
  })
  const tipos = useCargar(obtenerTiposDocumento)
  const barrios = useCargar(obtenerBarrios)
  const [errorGeneral, setErrorGeneral] = useState('')
  const [enviando, setEnviando] = useState(false)
  // Un doble clic o un doble Enter no repite el envío mientras se espera la respuesta.
  const guardarUnaVez = useUnaVez(guardar)

  async function guardar(evento) {
    evento.preventDefault()
    const encontrados = validarPerfil(datos)
    if (!datos.aceptaPrivacidad) {
      encontrados.aceptaPrivacidad = MENSAJE_ACEPTAR
    }
    setErrores(encontrados)
    setErrorGeneral('')
    if (Object.keys(encontrados).length > 0) return

    setEnviando(true)
    try {
      const { token, rol } = await completarPerfilGoogle({
        // Lo entrega la API al entrar con Google; con la simulación no existe y no se envía.
        ...(state?.tokenRegistro ? { tokenRegistro: state.tokenRegistro } : {}),
        ...armarDatosPerfil(datos),
        aceptaPrivacidad: datos.aceptaPrivacidad,
        declaraMayorEdad: datos.aceptaPrivacidad, // misma casilla que acepta el aviso
      })
      guardarToken(token)
      navigate(RUTA_POR_ROL[rol] ?? '/app', { replace: true })
    } catch (error) {
      if (!errorDeCampo(error)) {
        setErrorGeneral(error.mensaje ?? 'No pudimos guardar sus datos. Inténtelo de nuevo en un momento.')
      }
      setEnviando(false)
    }
  }

  const listos = tipos.datos && barrios.datos
  const fallaron = tipos.error || barrios.error

  return (
    <LayoutAuth>
      <EncabezadoPantalla titulo="Complete su perfil" atras="/login" />

      <FormularioPantalla
        alEnviar={guardarUnaVez}
        acciones={
          <>
            <Boton type="submit" disabled={!listos} cargando={enviando} textoCargando="Guardando…">
              Guardar y continuar
            </Boton>
            <Boton to="/login" variante="texto">
              Cancelar
            </Boton>
          </>
        }
      >
        <Aviso variante="exito" titulo="Cuenta de Google conectada">
          Ingresó como {state?.correo ?? 'su cuenta de Google'}. Falta completar algunos datos para
          terminar su registro.
        </Aviso>

        {errorGeneral && (
          <Aviso variante="error" titulo="No pudimos guardar sus datos">
            {errorGeneral}
          </Aviso>
        )}
        <EstadoCarga cargando={!listos} fallo={fallaron} />

        {listos && (
          <>
            <NotaPrivacidad />
            <CamposIdentidad datos={datos} errores={errores} cambiar={cambiar} tiposDocumento={tipos.datos} />
            <CamposResidencia datos={datos} errores={errores} cambiar={cambiar} barrios={barrios.datos} />
            <ConsentimientoRegistro datos={datos} errores={errores} cambiar={cambiar} />
          </>
        )}
      </FormularioPantalla>
    </LayoutAuth>
  )
}
