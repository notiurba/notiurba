import { Link } from 'react-router-dom'
import Boton from '../../compartido/componentes/Boton'
import ChipEstado from '../../compartido/componentes/ChipEstado'
import { IconoLapiz } from '../../compartido/componentes/Iconos'
import { formatearFecha } from '../../compartido/utilidades/fechas'
import { puedeActualizar } from '../../compartido/utilidades/reportes'
import './TarjetaReporte.css'

// Tarjeta de un reporte en «Mis reportes»: abre el seguimiento y, si corresponde, ofrece «Actualizar mi reporte».
export default function TarjetaReporte({ reporte, alActualizar }) {
  return (
    <article className="tarjeta-reporte">
      <Link to={`/app/reportes/${reporte.codigoSeguimiento}`} className="tarjeta-reporte__enlace">
        <h3>{reporte.categoria}</h3>
        <p className="tarjeta-reporte__lugar">
          {reporte.direccion ? `${reporte.direccion} · ` : ''}
          {reporte.barrio}
        </p>
        <div className="tarjeta-reporte__fila">
          <ChipEstado estado={reporte.estado} />
          <span>{formatearFecha(reporte.creadoEn)}</span>
        </div>
        <p className="tarjeta-reporte__codigo">{reporte.codigoSeguimiento}</p>
      </Link>

      {puedeActualizar(reporte) && (
        <Boton variante="secundario" className="tarjeta-reporte__boton" onClick={() => alActualizar(reporte)}>
          <IconoLapiz tamano={16} />
          Actualizar mi reporte
        </Boton>
      )}
    </article>
  )
}
