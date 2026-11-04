import { useCallback, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { obtenerReporte } from '../../compartido/api/reportes'
import Aviso from '../../compartido/componentes/Aviso'
import Boton from '../../compartido/componentes/Boton'
import BotonIcono from '../../compartido/componentes/BotonIcono'
import ChipEstado from '../../compartido/componentes/ChipEstado'
import { IconoCopiar, IconoCorreo, IconoX } from '../../compartido/componentes/Iconos'
import useCargar from '../../compartido/hooks/useCargar'
import './ReporteEnviado.css'

// Pantalla final: el reporte salió. Muestra el código de seguimiento de NotiUrba (no es el radicado oficial).
export default function ReporteEnviado() {
  const { codigo } = useParams()
  const navigate = useNavigate()
  const { state } = useLocation()
  const cargador = useCallback(() => obtenerReporte(codigo), [codigo])
  const { datos: reporte } = useCargar(cargador)
  const [copiado, setCopiado] = useState(false)

  async function copiar() {
    try {
      await navigator.clipboard.writeText(codigo)
      setCopiado(true)
    } catch {
      setCopiado(false) // sin permiso para copiar: la persona puede seleccionar el código a mano
    }
  }

  return (
    <div className="enviado">
      <div className="enviado__cerrar">
        <BotonIcono etiqueta="Cerrar" onClick={() => navigate('/app/reportes')}>
          <IconoX tamano={20} />
        </BotonIcono>
      </div>

      <span className="enviado__icono">
        <IconoCorreo tamano={44} />
      </span>
      <h1>Reporte enviado</h1>
      <p className="enviado__texto">
        NotiUrba envía su reporte a la Alcaldía de Tuluá. La entidad le responderá directamente a su
        correo.
      </p>

      <div className="enviado__tarjeta">
        <div className="enviado__codigo">
          <div>
            <span>Código de seguimiento NotiUrba</span>
            <strong>{codigo}</strong>
          </div>
          <BotonIcono etiqueta="Copiar el código" onClick={copiar}>
            <IconoCopiar tamano={20} />
          </BotonIcono>
        </div>
        {copiado && (
          <p className="enviado__copiado" role="status">
            Código copiado
          </p>
        )}
        {reporte && (
          <>
            <div className="enviado__fila">
              <span>Estado</span>
              <ChipEstado estado={reporte.estado} />
            </div>
            <div className="enviado__fila">
              <span>Tipo de problema</span>
              <strong>{reporte.categoria}</strong>
            </div>
            <div className="enviado__fila">
              <span>Lugar</span>
              <strong>
                {[reporte.barrio && `Barrio ${reporte.barrio}`, reporte.idComuna && `Comuna ${reporte.idComuna}`]
                  .filter(Boolean)
                  .join(' · ') || 'Punto marcado en el mapa'}
              </strong>
            </div>
          </>
        )}
      </div>

      {state?.fotosFallidas > 0 && (
        <Aviso variante="alerta" titulo="Algunas fotos no se subieron">
          Su reporte quedó guardado, pero {state.fotosFallidas === 1 ? 'una foto no se pudo subir' : `${state.fotosFallidas} fotos no se pudieron subir`}.
        </Aviso>
      )}

      <Aviso variante="info">
        Este código es de NotiUrba. La Alcaldía asigna el radicado oficial cuando recibe su correo.
      </Aviso>

      <div className="enviado__acciones">
        <Boton to="/app/reportes">Ver mis reportes</Boton>
        <Boton to="/reportar" variante="secundario">
          Hacer otro reporte
        </Boton>
      </div>
    </div>
  )
}
