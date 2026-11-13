import { useCallback, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { obtenerReporte } from '../../compartido/api/reportes'
import Aviso from '../../compartido/componentes/avisos/Aviso'
import Boton from '../../compartido/componentes/Boton'
import BotonIcono from '../../compartido/componentes/BotonIcono'
import ChipEstado from '../../compartido/componentes/ChipEstado'
import { IconoCopiar, IconoCorreo, IconoX } from '../../compartido/componentes/Iconos'
import useCargar from '../../compartido/hooks/useCargar'
import { textoLugar } from '../../compartido/utilidades/reportes'
import { estadoCorreo } from '../servicios/correoReportes'
import { ofrecerOtrosCanales, presentacionCorreo } from '../componentes/estadoCorreo'
import OtrosCanales from '../componentes/OtrosCanales'
import { CanalOficial } from '../componentes/ResultadoCorreo'
import '../paginas/CorreoReportes.css'
import './ReporteEnviado.css'

// Pantalla final: el reporte salió. Muestra el código de seguimiento de NotiUrba (no es el radicado oficial).
export default function ReporteEnviado() {
  const { codigo } = useParams()
  const navigate = useNavigate()
  const { state } = useLocation()
  const cargador = useCallback(() => obtenerReporte(codigo), [codigo])
  const { datos: reporte } = useCargar(cargador)
  const cargarEnvio = useCallback(() => estadoCorreo(codigo), [codigo])
  const consultaEnvio = useCargar(cargarEnvio)
  const envio = consultaEnvio.datos ?? state?.envio
  const presentacion = presentacionCorreo(envio)
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
      <h1>{presentacion.titulo}</h1>
      <p className="enviado__texto">
        {presentacion.mensaje}
      </p>
      <Aviso titulo={presentacion.subtitulo}>Enviar el reporte no asegura que la entidad lo lea de inmediato. Si le responden, lo harán a su correo.</Aviso>
      {consultaEnvio.error && <Aviso variante="error" titulo="No pudimos actualizar el estado del correo">El reporte sigue guardado. Inténtelo de nuevo.</Aviso>}
      <Boton variante="secundario" onClick={consultaEnvio.recargar} cargando={consultaEnvio.cargando} textoCargando="Consultando…">Actualizar estado del correo</Boton>
      {ofrecerOtrosCanales(envio)
        ? <OtrosCanales codigo={codigo} ruta={envio.ruta} />
        : envio?.ruta && <CanalOficial ruta={envio.ruta} />}

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
              <strong>{textoLugar(reporte) || 'Punto marcado en el mapa'}</strong>
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
        Este código es de NotiUrba, no un radicado oficial. Use el seguimiento y el canal indicado para consultar a la entidad.
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
