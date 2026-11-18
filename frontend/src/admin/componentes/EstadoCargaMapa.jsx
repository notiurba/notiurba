import { ErrorApi } from '../../compartido/api/cliente'
import Boton from '../../compartido/componentes/Boton'
import EstadoVacio from '../../compartido/componentes/avisos/EstadoVacio'

const MENSAJE_GENERICO = 'No pudimos consultar los reportes. Inténtelo de nuevo en unos minutos.'

function mensajeErrorMapa(error) {
  return error instanceof ErrorApi && error.mensaje?.trim()
    ? error.mensaje
    : MENSAJE_GENERICO
}

export default function EstadoCargaMapa({ error, recargar }) {
  return (
    <section className="map-shell map-shell--estado" role="alert">
      <EstadoVacio
        tono="alerta"
        titulo="No pudimos cargar el mapa de reportes"
        texto={mensajeErrorMapa(error)}
      >
        <Boton onClick={recargar}>Reintentar</Boton>
      </EstadoVacio>
    </section>
  )
}
