import BotonGoogle from '../../componentes/BotonGoogle'
import { CamposIdentidad } from '../../componentes/CamposPerfil'
import NotaPrivacidad from '../../componentes/NotaPrivacidad'
import Separador from '../../componentes/Separador'

// Paso 1 de 2: datos personales. No guarda nada por sí mismo: recibe los datos y los
// errores de Registro, y le avisa con `cambiar` cuando la persona escribe.
export default function PasoDatos({ datos, errores, cambiar, tiposDocumento }) {
  return (
    <>
      <NotaPrivacidad />

      <BotonGoogle />

      <Separador texto="O complete sus datos" />

      <CamposIdentidad datos={datos} errores={errores} cambiar={cambiar} tiposDocumento={tiposDocumento} />
    </>
  )
}
