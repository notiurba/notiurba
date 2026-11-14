import { useMemo, useState } from 'react'
import useUnaVez from '../../compartido/hooks/useUnaVez'
import { cambiarEstadoReporte, listarTodosLosReportes } from '../../compartido/api/reportes'
import ChipEstado from '../../compartido/componentes/ChipEstado'
import Filtro from '../../compartido/componentes/Filtro'
import Aviso from '../../compartido/componentes/avisos/Aviso'
import Boton from '../../compartido/componentes/Boton'
import EstadoVacio from '../../compartido/componentes/avisos/EstadoVacio'
import useCargar from '../../compartido/hooks/useCargar'
import { formatearFecha, formatearFechaHora } from '../../compartido/utilidades/fechas'
import { ESTADOS_REPORTE, ESTADO_ENVIADO, avisoDelCiudadano, comunasConReportes, estadoElegido, estadosConReportes, estadosSiguientes, lugarDelReporte } from '../../compartido/utilidades/reportes'
import './Reportes.css'

const ESTADOS = ['Todos', ...ESTADOS_REPORTE]
const DIAS_SIN_RESPUESTA = 15
const DIA_MS = 24 * 60 * 60 * 1000

// Un reporte lleva demasiado tiempo si sigue «Enviado» (sin respuesta) más de 15 días.
const sinRespuesta = (r) =>
  r.estado === ESTADO_ENVIADO && Date.now() - new Date(r.creadoEn).getTime() > DIAS_SIN_RESPUESTA * DIA_MS

// Panel de reportes: búsqueda, filtros, tabla y detalle con cambio de estado.
export default function Reportes() {
  const { datos, cargando, error, recargar } = useCargar(listarTodosLosReportes)
  const [codigo, setCodigo] = useState('')
  const [comuna, setComuna] = useState('Todas')
  const [categoria, setCategoria] = useState('Todos')
  const [estado, setEstado] = useState('Todos')
  const [elegido, setElegido] = useState(null) // código del reporte abierto
  const [pestana, setPestana] = useState('resumen')

  const reportes = useMemo(() => datos ?? [], [datos])
  const comunas = comunasConReportes(reportes)
  const categorias = [...new Set(reportes.map((r) => r.categoria))]

  // Los filtros de arriba (sin el de estado) deciden los números de las pestañas de estado.
  const base = reportes.filter(
    (r) =>
      r.codigoSeguimiento.toLowerCase().includes(codigo.trim().toLowerCase()) &&
      (comuna === 'Todas' || String(r.idComuna) === comuna) &&
      (categoria === 'Todos' || r.categoria === categoria),
  )
  const visibles = estado === 'Todos' ? base : base.filter((r) => r.estado === estado)
  const cuenta = (e) => (e === 'Todos' ? base.length : base.filter((r) => r.estado === e).length)
  const atrasados = reportes.filter(sinRespuesta).length
  const conAviso = reportes.filter((r) => avisoDelCiudadano(r)).length
  const abierto = reportes.find((r) => r.codigoSeguimiento === elegido)

  if (cargando && !datos) return <p className="cargando-texto">Cargando reportes…</p>
  if (error) {
    return (
      <EstadoVacio titulo="No pudimos cargar los reportes">
        <Boton onClick={recargar}>Reintentar</Boton>
      </EstadoVacio>
    )
  }

  return (
    <div className="adm-rep">
      <section className="adm-rep__lista">
        <div className="adm-rep__titulo">
          <h1>Reportes</h1>
          {conAviso > 0 && <span className="adm-rep__aviso-total">{conAviso} con aviso del ciudadano</span>}
          {atrasados > 0 && (
            <span className="adm-rep__atraso">
              {atrasados} con más de {DIAS_SIN_RESPUESTA} días sin respuesta
            </span>
          )}
        </div>

        <div className="adm-rep__filtros">
          <label>
            Buscar por código
            <input value={codigo} onChange={(e) => setCodigo(e.target.value)} placeholder="NU-000042" />
          </label>
          <label>
            Comuna
            <select value={comuna} onChange={(e) => setComuna(e.target.value)}>
              <option>Todas</option>
              {comunas.map((c) => (
                <option key={c} value={c}>Comuna {c}</option>
              ))}
            </select>
          </label>
          <label>
            Tipo
            <select value={categoria} onChange={(e) => setCategoria(e.target.value)}>
              <option>Todos</option>
              {categorias.map((c) => (
                <option key={c}>{c}</option>
              ))}
            </select>
          </label>
        </div>

        <div className="adm-rep__estados">
          {estadosConReportes(ESTADOS, cuenta, estado).map((e) => (
            <Filtro key={e} activo={estado === e} onClick={() => setEstado(e)}>
              {e} ({cuenta(e)})
            </Filtro>
          ))}
        </div>

        {visibles.length === 0 ? (
          <p className="adm-rep__vacio">No hay reportes con esos filtros.</p>
        ) : (
          <div className="adm-rep__tabla-caja">
            <table className="adm-rep__tabla">
              <thead>
                <tr>
                  <th>Código</th><th>Tipo</th><th className="adm-rep__extra">Comuna</th><th className="adm-rep__extra">Fecha</th><th>Estado</th>
                </tr>
              </thead>
              <tbody>
                {visibles.map((r) => (
                  <tr
                    key={r.codigoSeguimiento}
                    className={r.codigoSeguimiento === elegido ? 'adm-rep__fila--activa' : ''}
                  >
                    <td>
                      <button
                        type="button"
                        className="adm-rep__abrir"
                        onClick={() => { setElegido(r.codigoSeguimiento); setPestana('resumen') }}
                      >
                        {r.codigoSeguimiento}
                      </button>
                      {avisoDelCiudadano(r) && <span className="adm-rep__etiqueta-aviso">Aviso del ciudadano</span>}
                    </td>
                    <td>{r.categoria}</td>
                    <td className="adm-rep__extra">{r.idComuna != null ? `Comuna ${r.idComuna}` : 'Sin comuna'}</td>
                    <td className="adm-rep__extra">{formatearFecha(r.creadoEn)}</td>
                    <td><ChipEstado estado={r.estado} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <aside className="adm-rep__detalle" aria-label="Detalle del reporte">
        {abierto ? (
          <Detalle
            key={abierto.codigoSeguimiento}
            reporte={abierto}
            pestana={pestana}
            setPestana={setPestana}
            alGuardar={recargar}
          />
        ) : (
          <p className="adm-rep__vacio">Elija un reporte de la lista para ver su detalle.</p>
        )}
      </aside>
    </div>
  )
}

function Detalle({ reporte, pestana, setPestana, alGuardar }) {
  const opciones = estadosSiguientes(reporte.estado)
  const aviso = avisoDelCiudadano(reporte)
  const [elegido, setElegido] = useState('')
  const nuevo = estadoElegido(opciones, elegido)
  const [observacion, setObservacion] = useState('')
  const [guardando, setGuardando] = useState(false)
  const guardarUnaVez = useUnaVez(guardar)
  const [mensaje, setMensaje] = useState('')
  const [fallo, setFallo] = useState('')

  async function guardar(e) {
    e.preventDefault()
    setGuardando(true)
    setFallo('')
    setMensaje('')
    try {
      await cambiarEstadoReporte(reporte.codigoSeguimiento, nuevo, observacion.trim() || undefined)
      setMensaje('Estado actualizado.')
      setObservacion('')
      alGuardar()
    } catch (error) {
      setFallo(error?.mensaje || 'No pudimos guardar el cambio. Intente de nuevo.')
    } finally {
      setGuardando(false)
    }
  }

  return (
    <div>
      <h2>{reporte.codigoSeguimiento}</h2>
      <ChipEstado estado={reporte.estado} />
      {aviso && (
        <Aviso variante="alerta" titulo="Aviso del ciudadano">
          {aviso.observacion} Para confirmarlo, cambie el estado o deje una observación.
        </Aviso>
      )}
      <div className="adm-rep__pestanas" role="tablist">
        {['resumen', 'historial'].map((p) => (
          <button
            key={p}
            type="button"
            role="tab"
            aria-selected={pestana === p}
            className={pestana === p ? 'adm-rep__pestana adm-rep__pestana--activa' : 'adm-rep__pestana'}
            onClick={() => setPestana(p)}
          >
            {p === 'resumen' ? 'Resumen' : 'Historial'}
          </button>
        ))}
      </div>

      {pestana === 'resumen' ? (
        <dl className="adm-rep__datos">
          <dt>Tipo</dt><dd>{reporte.categoria}</dd>
          <dt>Ubicación</dt><dd>{lugarDelReporte(reporte)}</dd>
          <dt>Descripción</dt><dd>{reporte.descripcion}</dd>
          <dt>Creado</dt><dd>{formatearFecha(reporte.creadoEn)}</dd>
          {reporte.radicadoOficial && (<><dt>Radicado oficial</dt><dd>{reporte.radicadoOficial}</dd></>)}
        </dl>
      ) : (
        <ol className="adm-rep__historial">
          {[...reporte.historial].reverse().map((h, i) => (
            <li key={i}>
              <strong>{h.estado}</strong> <span>{formatearFechaHora(h.cambiadoEn)}</span>
              {h.observacion && <p>{h.observacion}</p>}
            </li>
          ))}
        </ol>
      )}

      {opciones.length === 0 ? (
        <p className="adm-rep__vacio">Este reporte ya está resuelto: no admite más cambios.</p>
      ) : (
      <form className="adm-rep__cambio" onSubmit={guardarUnaVez}>
        <h3>Cambiar estado</h3>
        <label>
          Nuevo estado
          <select value={nuevo} onChange={(e) => setElegido(e.target.value)}>
            {opciones.map((e) => (
              <option key={e}>{e}</option>
            ))}
          </select>
        </label>
        <label>
          Observación (opcional)
          <textarea rows={3} value={observacion} onChange={(e) => setObservacion(e.target.value)} />
        </label>
        {fallo && <Aviso variante="error">{fallo}</Aviso>}
        <Boton type="submit" cargando={guardando} textoCargando="Guardando…" disabled={!nuevo}>
          Guardar cambio
        </Boton>
      </form>
      )}
      {mensaje && <Aviso variante="exito">{mensaje}</Aviso>}
    </div>
  )
}
