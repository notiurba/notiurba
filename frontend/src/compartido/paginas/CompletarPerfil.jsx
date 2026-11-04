import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { completarPerfilGoogle } from '../api/auth'
import { obtenerBarrios, obtenerTiposDocumento } from '../api/catalogos'
import { guardarToken } from '../auth/sesion'
import Aviso from '../componentes/Aviso'
import Boton from '../componentes/Boton'
import { CamposIdentidad, CamposResidencia } from '../componentes/CamposPerfil'
import Casilla from '../componentes/Casilla'
import EncabezadoPantalla from '../componentes/EncabezadoPantalla'
import FormularioPantalla from '../componentes/FormularioPantalla'
import LayoutAuth from '../componentes/LayoutAuth'
import NotaPrivacidad, { EnlacePrivacidad } from '../componentes/NotaPrivacidad'
import '../estilos/marca.css'
import useCargar from '../hooks/useCargar'
import useFormulario from '../hooks/useFormulario'
import { MENSAJE_ACEPTAR, validarPerfil } from '../validaciones/registro'

const INICIAL = {
  nombres: '',
  apellidos: '',
  idTipoDocumento: '',
  numeroDocumento: '',
  telefono: '',
  idBarrio: '',
  direccionCorrespondencia: '',
  aceptaPrivacidad: false,
}

// Quien entró con Google ya tiene nombre y correo, pero faltan documento, barrio y dirección.
export default function CompletarPerfil() {
  const navigate = useNavigate()
  const { state } = useLocation()
  const { datos, errores, setErrores, cambiar } = useFormulario(INICIAL)
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
        nombres: datos.nombres.trim(),
        apellidos: datos.apellidos.trim(),
        idTipoDocumento: Number(datos.idTipoDocumento),
        numeroDocumento: datos.numeroDocumento.trim(),
        ...(datos.telefono ? { telefono: datos.telefono } : {}),
        idBarrio: Number(datos.idBarrio),
        direccionCorrespondencia: datos.direccionCorrespondencia.trim(),
        aceptaPrivacidad: true,
        declaraMayorEdad: true, // misma casilla que acepta el aviso
      })
      guardarToken(token)
      navigate(rol === 'ADMIN' ? '/admin' : '/app', { replace: true })
    } catch (error) {
      if (error.campo && error.campo in datos) {
        setErrores({ [error.campo]: error.mensaje ?? 'Revise este dato' })
      } else {
        setErrorGeneral(error.mensaje ?? 'No pudimos guardar sus datos. Inténtelo de nuevo en un momento.')
      }
      setEnviando(false)
    }
  }

  const listos = tipos.datos && barrios.datos
  const fallaron = tipos.error || barrios.error

  return (
    <LayoutAuth>
      <EncabezadoPantalla titulo="Completa tu perfil" atras="/login" />

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
        {fallaron && (
          <Aviso variante="error" titulo="No pudimos cargar las listas">
            Revise su conexión y vuelva a abrir esta pantalla.
          </Aviso>
        )}
        {!listos && !fallaron && <p className="cargando-texto">Cargando…</p>}

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
