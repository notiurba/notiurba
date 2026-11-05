import Boton from '../../compartido/componentes/Boton'
import ChipEstado from '../../compartido/componentes/ChipEstado'
import Aviso from '../../compartido/componentes/avisos/Aviso'
import HojaInferior from '../../compartido/componentes/avisos/HojaInferior'
import './HojaEstados.css'

// Qué significa cada estado de un reporte (como en el Figma).
const ESTADOS = [
  { estado: 'Recibido', texto: 'Reporte registrado' },
  { estado: 'Enviado', texto: 'Salió hacia la entidad' },
  { estado: 'En trámite', texto: 'Lo atiende la Secretaría' },
  { estado: 'Resuelto', texto: 'Problema atendido' },
]

export default function HojaEstados({ abierta, alCerrar }) {
  return (
    <HojaInferior abierta={abierta} alCerrar={alCerrar} titulo="Estados de un reporte">
      <ul className="hoja-estados">
        {ESTADOS.map(({ estado, texto }) => (
          <li key={estado}>
            <ChipEstado estado={estado} />
            <span>{texto}</span>
          </li>
        ))}
      </ul>
      <Aviso variante="info">
        NotiUrba envía el correo por usted: «Recibido» es el reporte ya registrado y «Enviado» es el correo que
        salió hacia la entidad con su correo como dirección de respuesta.
      </Aviso>
      <Boton variante="secundario" onClick={alCerrar}>
        Entendido
      </Boton>
    </HojaInferior>
  )
}
