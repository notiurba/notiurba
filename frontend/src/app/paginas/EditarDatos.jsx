import { useState } from 'react'
import useUnaVez from '../../compartido/hooks/useUnaVez'
import { useNavigate } from 'react-router-dom'
import { obtenerBarrios, obtenerTiposDocumento } from '../../compartido/api/catalogos'
import { guardarPerfil, obtenerPerfil } from '../../compartido/api/usuario'
import Aviso from '../../compartido/componentes/avisos/Aviso'
import EstadoCarga from '../../compartido/componentes/avisos/EstadoCarga'
import Boton from '../../compartido/componentes/Boton'
import { CamposIdentidad, CamposResidencia } from '../../compartido/componentes/formulario/CamposPerfil'
import EncabezadoPantalla from '../../compartido/componentes/EncabezadoPantalla'
import FormularioPantalla from '../../compartido/componentes/formulario/FormularioPantalla'
import useCargar from '../../compartido/hooks/useCargar'
import useFormulario from '../../compartido/hooks/useFormulario'
import { armarDatosPerfil, validarPerfil } from '../../compartido/validaciones/registro'

// Carga el perfil y, cuando llega, muestra el formulario ya relleno. Se separa en dos componentes
// para que el formulario nazca con los datos (useFormulario solo usa el valor inicial una vez).
export default function EditarDatos() {
  const perfil = useCargar(obtenerPerfil)
  const tipos = useCargar(obtenerTiposDocumento)
  const barrios = useCargar(obtenerBarrios)
  const listos = perfil.datos && tipos.datos && barrios.datos
  const fallaron = perfil.error || tipos.error || barrios.error

  return (
    <>
      <EncabezadoPantalla titulo="Editar mis datos" atras="/app/ajustes" />
      <EstadoCarga cargando={!listos} fallo={fallaron} titulo="No pudimos cargar sus datos" />
      {listos && <Formulario perfil={perfil.datos} tipos={tipos.datos} barrios={barrios.datos} />}
    </>
  )
}

function Formulario({ perfil, tipos, barrios }) {
  const navigate = useNavigate()
  const { datos, errores, setErrores, cambiar, errorDeCampo } = useFormulario({
    nombres: perfil.nombres,
    apellidos: perfil.apellidos,
    idTipoDocumento: String(perfil.idTipoDocumento),
    numeroDocumento: perfil.numeroDocumento,
    telefono: perfil.telefono ?? '',
    idBarrio: String(perfil.idBarrio),
    direccionCorrespondencia: perfil.direccionCorrespondencia,
  })
  const [errorGeneral, setErrorGeneral] = useState('')
  const [guardando, setGuardando] = useState(false)
  // Un doble clic o un doble Enter no repite el envío mientras se espera la respuesta.
  const guardarUnaVez = useUnaVez(guardar)

  async function guardar(evento) {
    evento.preventDefault()
    const encontrados = validarPerfil(datos)
    setErrores(encontrados)
    setErrorGeneral('')
    if (Object.keys(encontrados).length > 0) return

    setGuardando(true)
    try {
      await guardarPerfil(armarDatosPerfil(datos, { enviarTelefonoVacio: true }))
      navigate('/app/ajustes', { replace: true, state: { mensaje: 'Sus datos se guardaron.' } })
    } catch (error) {
      if (!errorDeCampo(error)) setErrorGeneral('No pudimos guardar sus cambios. Inténtelo de nuevo en un momento.')
      setGuardando(false)
    }
  }

  return (
    <FormularioPantalla
      alEnviar={guardarUnaVez}
      acciones={
        <>
          <Boton type="submit" cargando={guardando} textoCargando="Guardando…">
            Guardar cambios
          </Boton>
          <Boton to="/app/ajustes" variante="texto">
            Cancelar
          </Boton>
        </>
      }
    >
      {errorGeneral && <Aviso variante="error">{errorGeneral}</Aviso>}
      <CamposIdentidad
        datos={datos}
        errores={errores}
        cambiar={cambiar}
        tiposDocumento={tipos}
        correo={perfil.correo}
      />
      <CamposResidencia datos={datos} errores={errores} cambiar={cambiar} barrios={barrios} />
    </FormularioPantalla>
  )
}
