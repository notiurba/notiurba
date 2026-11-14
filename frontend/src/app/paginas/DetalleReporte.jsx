import { useCallback, useState } from 'react'
import { useParams } from 'react-router-dom'
import { obtenerReporte } from '../../compartido/api/reportes'
import Boton from '../../compartido/componentes/Boton'
import ChipEstado from '../../compartido/componentes/ChipEstado'
import EncabezadoPantalla from '../../compartido/componentes/EncabezadoPantalla'
import EstadoVacio from '../../compartido/componentes/avisos/EstadoVacio'
import IconoCategoria from '../../compartido/componentes/IconoCategoria'
import { IconoDocumento, IconoLapiz } from '../../compartido/componentes/Iconos'
import useCargar from '../../compartido/hooks/useCargar'
import useUnaVez from '../../compartido/hooks/useUnaVez'
import { formatearFechaHora } from '../../compartido/utilidades/fechas'
import HojaActualizarReporte from '../componentes/HojaActualizarReporte'
import OtrosCanales from '../componentes/OtrosCanales'
import { resumenCorreoDetalle } from '../componentes/estadoCorreo'
import { enviar, estadoCorreo } from '../servicios/correoReportes'
import { puedeActualizar, textoLugar } from '../../compartido/utilidades/reportes'
import './DetalleReporte.css'

// Seguimiento de un reporte: datos, estado actual e historial de cambios.
export default function DetalleReporte() {
  const { codigo } = useParams()
  // Con otro código se parte de cero: no se queda el correo del reporte anterior.
  return <DetalleDeReporte key={codigo} codigo={codigo} />
}

function DetalleDeReporte({ codigo }) {
  const cargador = useCallback(() => obtenerReporte(codigo), [codigo])
  const { datos: reporte, cargando, error, recargar } = useCargar(cargador)
  // El correo es información extra: si no se puede consultar, el seguimiento se ve igual.
  const cargadorCorreo = useCallback(() => estadoCorreo(codigo).catch(() => null), [codigo])
  const { datos: envio, recargar: recargarCorreo } = useCargar(cargadorCorreo)
  const correo = resumenCorreoDetalle(envio)
  const reintentarUnaVez = useUnaVez(async () => {
    try { await enviar(codigo) } catch { /* el estado que se recarga dice cómo quedó */ }
    recargarCorreo()
  })
  const [actualizando, setActualizando] = useState(null)
  const noExiste = error?.estado === 404

  return (
    <>
      <EncabezadoPantalla titulo="Seguimiento" atras="/app/reportes" />

      {cargando && !reporte && <p className="cargando-texto detalle__cargando">Cargando…</p>}

      {error && (
        <EstadoVacio
          icono={<IconoDocumento tamano={52} />}
          tono="alerta"
          titulo={noExiste ? 'No encontramos este reporte' : 'No pudimos cargar el reporte'}
          texto={noExiste ? 'Revise el código o vuelva a Mis reportes.' : 'Revise su conexión e intente de nuevo.'}
        >
          {noExiste ? <Boton to="/app/reportes">Ver mis reportes</Boton> : <Boton onClick={recargar}>Reintentar</Boton>}
        </EstadoVacio>
      )}

      {reporte && (
        <div className="detalle">
          <div className="detalle__titulo">
            <IconoCategoria idCategoria={reporte.idCategoria} />
            <div>
              <h2>{reporte.categoria}</h2>
              <p>{reporte.codigoSeguimiento}</p>
            </div>
          </div>

          <dl className="detalle__datos">
            <div>
              <dt>Estado</dt>
              <dd>
                <ChipEstado estado={reporte.estado} />
              </dd>
            </div>
            <div>
              <dt>Lugar</dt>
              <dd>{textoLugar(reporte)}</dd>
            </div>
            <div>
              <dt>Descripción</dt>
              <dd>{reporte.descripcion}</dd>
            </div>
            {reporte.radicadoOficial && (
              <div>
                <dt>Radicado de la entidad</dt>
                <dd>{reporte.radicadoOficial}</dd>
              </div>
            )}
          </dl>

          {correo && (
            <section className={`detalle__correo detalle__correo--${correo.clase}`} aria-labelledby="detalle-correo-titulo">
              <h3 id="detalle-correo-titulo" className="detalle__subtitulo">{correo.titulo}</h3>
              <p className="detalle__correo-etiqueta">{correo.etiqueta}</p>
              <p>{correo.mensaje}</p>
              {correo.reintentar && <Boton variante="secundario" onClick={reintentarUnaVez}>Reintentar envío</Boton>}
            </section>
          )}
          {correo?.otrosCanales && <OtrosCanales codigo={reporte.codigoSeguimiento} ruta={envio.ruta} />}

          <h3 className="detalle__subtitulo">Historial</h3>
          <ol className="detalle__historial">
            {reporte.historial.map((cambio) => (
              <li key={`${cambio.estado}-${cambio.cambiadoEn}`}>
                <ChipEstado estado={cambio.estado} />
                <span className="detalle__fecha">{formatearFechaHora(cambio.cambiadoEn)}</span>
                {cambio.observacion && <p>{cambio.observacion}</p>}
              </li>
            ))}
          </ol>

          {puedeActualizar(reporte) && (
            <Boton variante="secundario" onClick={() => setActualizando(reporte)}>
              <IconoLapiz tamano={16} />
              Actualizar mi reporte
            </Boton>
          )}
        </div>
      )}

      <HojaActualizarReporte
        key={actualizando?.codigoSeguimiento ?? 'cerrada'}
        reporte={actualizando}
        alCerrar={() => setActualizando(null)}
        alGuardar={() => {
          setActualizando(null)
          recargar()
        }}
      />
    </>
  )
}
