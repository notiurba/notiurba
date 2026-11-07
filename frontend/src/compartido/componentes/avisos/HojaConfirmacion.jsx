import Aviso from './Aviso'
import Boton from '../Boton'
import HojaInferior from './HojaInferior'

// Hoja de «¿está segura?»: ícono, título, texto, un aviso opcional y dos botones.
// La usan «Eliminar mis datos», «Eliminar mi cuenta», «Cancelar el reporte» y «¿Enviar el reporte?».
//  - peligro: el botón de confirmar va en rosa (acciones que no se pueden deshacer).
export default function HojaConfirmacion({
  abierta,
  alCancelar,
  alConfirmar,
  icono,
  tono,
  titulo,
  texto,
  aviso,
  textoConfirmar,
  textoCancelar = 'Cancelar',
  peligro = false,
  cargando = false,
  textoCargando,
}) {
  return (
    <HojaInferior abierta={abierta} alCerrar={alCancelar} icono={icono} tono={tono} titulo={titulo} texto={texto}>
      {aviso && <Aviso variante="alerta">{aviso}</Aviso>}
      <Boton
        variante={peligro ? 'peligro' : 'primario'}
        onClick={alConfirmar}
        cargando={cargando}
        textoCargando={textoCargando}
      >
        {textoConfirmar}
      </Boton>
      <Boton variante="texto" onClick={alCancelar} disabled={cargando}>
        {textoCancelar}
      </Boton>
    </HojaInferior>
  )
}
