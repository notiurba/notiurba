import Aviso from '../../compartido/componentes/avisos/Aviso'

// Dice cuántos reportes del listado no se dibujan porque no traen ubicación.
export default function AvisoSinUbicacion({ cantidad }) {
  if (cantidad < 1) return null

  return (
    <Aviso variante="info">
      {cantidad === 1
        ? '1 reporte no tiene ubicación y no aparece en el mapa.'
        : `${cantidad} reportes no tienen ubicación y no aparecen en el mapa.`}
    </Aviso>
  )
}
