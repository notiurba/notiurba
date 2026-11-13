import { IconoDocumento, IconoInfo, IconoPin, IconoTelefono } from '../../compartido/componentes/Iconos'
import { accionCanal, enlaceCanal, nombreCanal } from './estadoCorreo'
import './ListaCanales.css'

const ICONOS = { WhatsApp: IconoTelefono, Teléfono: IconoTelefono, Web: IconoDocumento, Presencial: IconoPin }

// Lo que se lee del acceso: el número, la dirección o solo el nombre del sitio web.
function textoAcceso(canal) {
  if (canal.tipo !== 'Web') return canal.acceso
  try { return new URL(canal.acceso).hostname.replace(/^www\./, '') } catch { return canal.acceso }
}

// Canales oficiales de la entidad, en el orden en que se recomiendan; el primero se marca.
export default function ListaCanales({ canales, recomendarPrimero = true }) {
  if (!canales?.length) return null
  return (
    <ul className="lista-canales" aria-label="Canales oficiales">
      {canales.map((canal, i) => {
        const Icono = ICONOS[canal.tipo] ?? IconoInfo
        const enlace = enlaceCanal(canal)
        return (
          <li key={`${canal.tipo}-${canal.acceso}`} className="lista-canales__fila">
            <span className="lista-canales__icono" aria-hidden="true"><Icono tamano={20} /></span>
            <div className="lista-canales__textos">
              <strong>
                {nombreCanal(canal)}
                {recomendarPrimero && i === 0 && <span className="lista-canales__chip">Recomendado</span>}
              </strong>
              {canal.finalidad && <span>{canal.finalidad}</span>}
              {canal.detalle && <span>{canal.detalle}</span>}
              {canal.horario && <span>{canal.horario}</span>}
              {enlace
                ? <a href={enlace} target={enlace.startsWith('https:') ? '_blank' : undefined} rel="noopener noreferrer"
                    aria-label={`${accionCanal(canal)}: ${canal.acceso}`}>{textoAcceso(canal)}</a>
                : <span className="lista-canales__acceso">{canal.acceso}</span>}
            </div>
          </li>
        )
      })}
    </ul>
  )
}
