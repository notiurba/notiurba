import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
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

const DATOS_INICIALES = { ...DATOS_PERFIL_VACIOS, correo: '', password: '', confirmar: '' }

const MENSAJE_ERROR_GENERAL = 'No pudimos crear su cuenta. Inténtelo de nuevo en un momento.'

// Registro en dos pasos. Este componente guarda TODO lo escrito (así no se pierde al
// volver al paso 1) y decide qué paso mostrar; cada paso solo dibuja sus campos.
export default function Registro() {
  const navigate = useNavigate()
  const [paso, setPaso] = useState(1)
  const { datos, errores, setErrores, cambiar, errorDeCampo } = useFormulario(DATOS_INICIALES)
  const [errorGeneral, setErrorGeneral] = useState('')
  const [enviando, setEnviando] = useState(false)
  const tipos = useCargar(obtenerTiposDocumento)
  const barrios = useCargar(obtenerBarrios)

  function continuar(evento) {
    evento.preventDefault()
    const encontrados = validarPasoDatos(datos)
    setErrores(encontrados)
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
    const encontrados = validarPasoAcceso(datos)
    setErrores(encontrados)
    setErrorGeneral('')
    if (Object.keys(encontrados).length > 0) return

    setEnviando(true)
    try {
      await registrarCuenta(armarSolicitudRegistro(datos))
      navigate('/verificar-correo', { replace: true, state: { correo: datos.correo.trim() } })
    } catch (error) {
      mostrarErrorApi(error)
      setEnviando(false)
    }
  }

  const catalogosListos = tipos.datos && barrios.datos
  const catalogosFallaron = tipos.error || barrios.error

  return (
    <LayoutAuth web>
      <EncabezadoPantalla
        titulo="Crear cuenta"
        atras="/login"
        alAtras={paso === 2 ? () => setPaso(1) : undefined}
      />

      <FormularioPantalla
        alEnviar={paso === 1 ? continuar : crearCuenta}
        acciones={
          <>
            <Boton
              type="submit"
              disabled={!catalogosListos}
              cargando={enviando}
              textoCargando="Creando cuenta…"
            >
              {paso === 1 ? 'Continuar' : 'Crear cuenta'}
            </Boton>
            <Boton to="/login" variante="texto">
              Cancelar
            </Boton>
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

        <EstadoCarga cargando={!catalogosListos} fallo={catalogosFallaron} />

        {catalogosListos && paso === 1 && (
          <PasoDatos datos={datos} errores={errores} cambiar={cambiar} tiposDocumento={tipos.datos} />
        )}
        {catalogosListos && paso === 2 && (
          <PasoAcceso datos={datos} errores={errores} cambiar={cambiar} barrios={barrios.datos} />
        )}
      </FormularioPantalla>
    </LayoutAuth>
  )
}
