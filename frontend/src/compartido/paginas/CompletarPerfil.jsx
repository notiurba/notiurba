import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { completarPerfilGoogle } from '../api/auth'
import { obtenerBarrios, obtenerTiposDocumento } from '../api/catalogos'
import { RUTA_POR_ROL, guardarToken } from '../auth/sesion'
import Aviso from '../componentes/avisos/Aviso'
import EstadoCarga from '../componentes/avisos/EstadoCarga'
import Boton from '../componentes/Boton'
import { CamposIdentidad, CamposResidencia } from '../componentes/formulario/CamposPerfil'
import Casilla from '../componentes/formulario/Casilla'
import EncabezadoPantalla from '../componentes/EncabezadoPantalla'
import FormularioPantalla from '../componentes/formulario/FormularioPantalla'
import LayoutAuth from '../componentes/acceso/LayoutAuth'
import NotaPrivacidad, { EnlacePrivacidad } from '../componentes/avisos/NotaPrivacidad'
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
        aceptaPrivacidad: true,
        declaraMayorEdad: true, // misma casilla que acepta el aviso
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
        alEnviar={guardar}
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
            <Casilla
              id="aceptaPrivacidad"
              checked={datos.aceptaPrivacidad}
              onChange={cambiar('aceptaPrivacidad')}
              error={errores.aceptaPrivacidad}
            >
              Soy mayor de 18 años y acepto el tratamiento de mis datos personales (Ley 1581). <EnlacePrivacidad />
            </Casilla>
          </>
        )}
      </FormularioPantalla>
    </LayoutAuth>
  )
}
