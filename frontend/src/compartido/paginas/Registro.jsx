import { useState } from 'react'
import useUnaVez from '../hooks/useUnaVez'
import { useLocation, useNavigate } from 'react-router-dom'
import { estadoRetornoReporte } from '../auth/retornoReporte'
import { registrarCuenta } from '../api/auth'
import { obtenerBarrios, obtenerTiposDocumento } from '../api/catalogos'
import Aviso from '../componentes/avisos/Aviso'
import EstadoCarga from '../componentes/avisos/EstadoCarga'
import Boton from '../componentes/Boton'
import EncabezadoPantalla from '../componentes/EncabezadoPantalla'
import FormularioPantalla from '../componentes/formulario/FormularioPantalla'
import LayoutAuth from '../componentes/acceso/LayoutAuth'
import PasosBarra from '../componentes/PasosBarra'
import '../estilos/marca.css'
import useCargar from '../hooks/useCargar'
import useFormulario from '../hooks/useFormulario'
import {
  armarSolicitudRegistro,
  CAMPOS_PASO_DATOS,
  DATOS_PERFIL_VACIOS,
  validarPasoAcceso,
  validarPasoDatos,
} from '../validaciones/registro'
import PasoAcceso from './registro/PasoAcceso'
import PasoDatos from './registro/PasoDatos'
import { LimiteDireccionContexto } from '../componentes/formulario/limiteDireccionContexto'
import { MAX_DIRECCION_REGISTRO, validarDireccionRegistro } from '../validaciones/direccionRegistro'
import DialogoAvisoPrivacidad from '../componentes/avisos/DialogoAvisoPrivacidad'

const DATOS_INICIALES = { ...DATOS_PERFIL_VACIOS, correo: '', password: '', confirmar: '' }

const MENSAJE_ERROR_GENERAL = 'No pudimos crear su cuenta. Inténtelo de nuevo en un momento.'

// Registro en dos pasos. Este componente guarda TODO lo escrito (así no se pierde al
// volver al paso 1) y decide qué paso mostrar; cada paso solo dibuja sus campos.
export default function Registro() {
  const navigate = useNavigate()
  const { state } = useLocation()
  const retorno = estadoRetornoReporte(state?.siguiente ?? null)
  const [paso, setPaso] = useState(1)
  const [avisoAbierto, setAvisoAbierto] = useState(false)
  const { datos, errores, setErrores, cambiar, errorDeCampo } = useFormulario(DATOS_INICIALES)
  const [errorGeneral, setErrorGeneral] = useState('')
  const [enviando, setEnviando] = useState(false)
  const crearCuentaUnaVez = useUnaVez(crearCuenta)
  const tipos = useCargar(obtenerTiposDocumento)
  const barrios = useCargar(obtenerBarrios)

  function continuar(evento) {
    evento.preventDefault()
    const encontrados = validarPasoDatos(datos)
    setErrores(encontrados)
    setErrorGeneral(Object.keys(encontrados).length ? 'Revise los campos indicados para continuar a Ubicación y acceso.' : '')
    if (Object.keys(encontrados).length === 0) setPaso(2)
  }

  // Si la API rechaza un campo (por ejemplo «documento ya registrado»), se muestra bajo esa
  // casilla y, si pertenece al paso 1, se vuelve a ese paso. Sin `campo`, va el aviso general.
  function mostrarErrorApi(error) {
    if (errorDeCampo(error)) {
      if (CAMPOS_PASO_DATOS.includes(error.campo)) setPaso(1)
    } else {
      setErrorGeneral(error.mensaje ?? MENSAJE_ERROR_GENERAL)
    }
  }

  async function crearCuenta(evento) {
    evento.preventDefault()
    if (!barrios.datos?.length) {
      setErrorGeneral('No pudimos cargar la lista de barrios. Inténtelo de nuevo en un momento.')
      return
    }
    const encontrados = { ...validarPasoDatos(datos), ...validarPasoAcceso(datos) }
    const errorDireccion = validarDireccionRegistro(datos.direccionCorrespondencia)
    if (errorDireccion) encontrados.direccionCorrespondencia = errorDireccion
    if (CAMPOS_PASO_DATOS.some((campo) => encontrados[campo])) setPaso(1)
    if (!barrios.datos.some((barrio) => String(barrio.idBarrio) === datos.idBarrio)) {
      encontrados.idBarrio = 'Seleccione un barrio disponible en la lista.'
    }
    setErrores(encontrados)
    setErrorGeneral('')
    if (Object.keys(encontrados).length > 0) return

    setEnviando(true)
    try {
      await registrarCuenta(armarSolicitudRegistro(datos))
      navigate('/verificar-correo', { replace: true, state: { correo: datos.correo.trim(), ...retorno } })
    } catch (error) {
      mostrarErrorApi(error)
      setEnviando(false)
    }
  }

  const tiposListos = Boolean(tipos.datos?.length)
  const barriosListos = Array.isArray(barrios.datos)

  // Reutiliza los enlaces del consentimiento protegido sin cambiar sus archivos.
  // Clic normal/Enter consulta v3 aquí; Ctrl/clic central conserva la ruta pública.
  function consultarPrivacidad(evento) {
    const enlace = evento.target.closest?.('a[href="/aviso-privacidad"]')
    if (!enlace || evento.button !== 0 || evento.ctrlKey || evento.metaKey || evento.shiftKey || evento.altKey) return
    evento.preventDefault()
    enlace.focus()
    setAvisoAbierto(true)
  }

  return (
    <LimiteDireccionContexto value={MAX_DIRECCION_REGISTRO}>
    <div onClickCapture={consultarPrivacidad}>
    <LayoutAuth web>
      <EncabezadoPantalla
        titulo="Crear cuenta"
        atras="/login"
        alAtras={paso === 2 ? () => setPaso(1) : () => navigate('/login', { state: retorno })}
      />

      <FormularioPantalla
        alEnviar={paso === 1 ? continuar : crearCuentaUnaVez}
        acciones={
          <>
            <Boton
              type="submit"
              disabled={paso === 1 ? !tiposListos : !barrios.datos?.length}
              cargando={enviando}
              textoCargando="Creando cuenta…"
            >
              {paso === 1 ? 'Continuar' : 'Crear cuenta'}
            </Boton>
            {paso === 2 && <Boton onClick={() => setPaso(1)} variante="secundario">Volver a datos personales</Boton>}
            <Boton to="/" variante="texto">Volver al inicio de NotiUrba</Boton>
          </>
        }
      >
        <PasosBarra
          paso={paso}
          total={2}
          etiqueta={paso === 1 ? 'Datos personales' : 'Ubicación y acceso'}
        />

        {errorGeneral && (
          <Aviso variante="error" titulo="No pudimos crear su cuenta">
            {errorGeneral}
          </Aviso>
        )}

        <EstadoCarga cargando={tipos.cargando} fallo={tipos.error} />
        {tipos.datos?.length === 0 && <Aviso variante="alerta">No hay tipos de documento disponibles para continuar.</Aviso>}
        {barrios.datos?.length === 0 && (
          <Aviso variante="alerta" titulo="No hay barrios disponibles">
            Para crear la cuenta necesitamos que se habiliten los barrios de residencia. Puede volver a sus datos personales o regresar al inicio sin iniciar sesión.
          </Aviso>
        )}
        {paso === 2 && <EstadoCarga cargando={barrios.cargando} fallo={barrios.error} />}
        {paso === 2 && barrios.error && <Boton onClick={barrios.recargar} variante="texto">Reintentar barrios</Boton>}

        {tiposListos && paso === 1 && (
          <PasoDatos datos={datos} errores={errores} cambiar={cambiar} tiposDocumento={tipos.datos} />
        )}
        {barriosListos && paso === 2 && (
          <PasoAcceso datos={datos} errores={errores} cambiar={cambiar} barrios={barrios.datos} />
        )}
      </FormularioPantalla>
    </LayoutAuth>
    </div>
    {avisoAbierto && <DialogoAvisoPrivacidad alCerrar={() => setAvisoAbierto(false)} />}
    </LimiteDireccionContexto>
  )
}
