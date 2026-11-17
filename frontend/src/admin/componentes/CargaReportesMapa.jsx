import useCargar from '../../compartido/hooks/useCargar'
import EstadoCargaMapa from './EstadoCargaMapa'

// Decide entre «cargando», el error con «Reintentar» y el mapa. El mapa solo recibe lo que devolvió el API.
// Vive aparte de MapaComunas para poder probarlo sin Leaflet.
export default function CargaReportesMapa({ cargador, children }) {
  const { datos, cargando, error, recargar } = useCargar(cargador)

  if (cargando) {
    return (
      <div className="app-loading" role="status">
        <span>Cargando mapa de comunas...</span>
      </div>
    )
  }

  if (error) {
    return <EstadoCargaMapa error={error} recargar={recargar} />
  }

  return children(datos ?? [])
}
