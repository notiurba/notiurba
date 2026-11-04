import { useState } from 'react'
import { Link } from 'react-router-dom'
import { listarMisReportes } from '../../compartido/api/reportes'
import Aviso from '../../compartido/componentes/Aviso'
import Boton from '../../compartido/componentes/Boton'
import EncabezadoPantalla from '../../compartido/componentes/EncabezadoPantalla'
import EstadoVacio from '../../compartido/componentes/EstadoVacio'
import Filtro from '../../compartido/componentes/Filtro'
import { IconoDocumento, IconoMas, IconoOrdenar, IconoSinConexion } from '../../compartido/componentes/Iconos'
import useCargar from '../../compartido/hooks/useCargar'
import HojaActualizarReporte from '../componentes/HojaActualizarReporte'
import HojaEstados from '../componentes/HojaEstados'
import TarjetaReporte from '../componentes/TarjetaReporte'
import './MisReportes.css'

const FILTROS = [
  { clave: 'activos', texto: 'Activos', coincide: (r) => r.estado !== 'Resuelto' },
  { clave: 'resueltos', texto: 'Resueltos', coincide: (r) => r.estado === 'Resuelto' },
  { clave: 'todos', texto: 'Todos', coincide: () => true },
]

// Lista de reportes de la persona con filtro por estado y orden por fecha.
export default function MisReportes() {
  const { datos, cargando, error, recargar } = useCargar(listarMisReportes)
  const [filtro, setFiltro] = useState('activos')
  const [masRecientes, setMasRecientes] = useState(true)
  const [aActualizar, setAActualizar] = useState(null)
  const [verEstados, setVerEstados] = useState(false)

  if (error) {
    return (
      <>
        <EncabezadoPantalla titulo="Mis reportes" sinVolver />
        <EstadoVacio
          icono={<IconoSinConexion tamano={56} />}
          tono="alerta"
          titulo="Sin conexión a internet"
          texto="Revise su conexión e inténtelo de nuevo."
        >
          <Boton onClick={recargar}>Reintentar</Boton>
        </EstadoVacio>
      </>
    )
  }

  const reportes = datos ?? []
  const elegido = FILTROS.find((f) => f.clave === filtro)
  const visibles = reportes
    .filter(elegido.coincide)
    .toSorted((a, b) => (masRecientes ? 1 : -1) * (new Date(b.creadoEn) - new Date(a.creadoEn)))

  return (
    <>
      <EncabezadoPantalla titulo="Mis reportes" sinVolver />

      {!cargando && reportes.length === 0 ? (
        <EstadoVacio
          icono={<IconoDocumento tamano={52} />}
          titulo="Le damos la bienvenida a NotiUrba"
          texto="Aquí verá sus reportes, su estado y su código de seguimiento. Empiece reportando el primer problema de su barrio: toque «Reportar» en la barra de abajo o use este botón."
        >
          <Boton to="/reportar">
            <IconoMas tamano={18} />
            Hacer mi primer reporte
          </Boton>
        </EstadoVacio>
      ) : (
        <div className="mis-reportes">
          <Aviso variante="info" titulo="¿Cómo se actualizan los estados?">
            NotiUrba no conoce la respuesta de la entidad. El estado solo cambia si un administrador lo
            actualiza o si usted lo actualiza aquí. Revise también su correo.
            <br />
            <button type="button" className="enlace-boton" onClick={() => setVerEstados(true)}>
              Ver qué significa cada estado
            </button>
          </Aviso>

          <div className="mis-reportes__filtros" role="group" aria-label="Filtrar por estado">
            {FILTROS.map((f) => (
              <Filtro key={f.clave} activo={filtro === f.clave} onClick={() => setFiltro(f.clave)}>
                {f.texto}
              </Filtro>
            ))}
          </div>

          <div className="mis-reportes__cabecera">
            <span aria-live="polite">{cargando ? 'Cargando…' : `${visibles.length} reportes`}</span>
            <Filtro onClick={() => setMasRecientes(!masRecientes)}>
              <IconoOrdenar tamano={16} />
              {masRecientes ? 'Más recientes' : 'Más antiguos'}
            </Filtro>
          </div>

          <div className="mis-reportes__lista">
            {visibles.map((reporte) => (
              <TarjetaReporte key={reporte.codigoSeguimiento} reporte={reporte} alActualizar={setAActualizar} />
            ))}
            {!cargando && visibles.length === 0 && (
              <p className="cargando-texto">
                No tiene reportes en esta lista. <Link to="/reportar">Hacer un reporte</Link>
              </p>
            )}
          </div>
        </div>
      )}

      <HojaEstados abierta={verEstados} alCerrar={() => setVerEstados(false)} />

      <HojaActualizarReporte
        reporte={aActualizar}
        alCerrar={() => setAActualizar(null)}
        alGuardar={() => {
          setAActualizar(null)
          recargar()
        }}
      />
    </>
  )
}
