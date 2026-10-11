import { useEffect, useRef } from 'react'
import L from 'leaflet'
import { GeoJSON, Pane, Popup, useMap } from 'react-leaflet'
import { diagnosticoLimites } from '../../data/geo/diagnosticoLimites'

const incidencias = [...diagnosticoLimites.features].sort(
  (a, b) => b.properties.area_m2 - a.properties.area_m2,
)
const formato = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 2 })
const areaTexto = (area) => area < 0.01 ? area.toExponential(2) : formato.format(area)
const comunasTexto = (p) => p.comunas.length ? p.comunas.join(', ') : 'Sin atribución concluyente'
const apreciables = (tipo) => incidencias.filter((f) => f.properties.tipo === tipo && !f.properties.micro_superficie).length

export function PanelDiagnostico({ huecos, solapamientos, onHuecos, onSolapamientos, seleccion, onSeleccion }) {
  const resumen = diagnosticoLimites.metadata.summary
  return (
    <div className="diagnostico-panel">
      <label><input type="checkbox" checked={solapamientos} onChange={(e) => onSolapamientos(e.target.checked)} />
        <span className="diagnostico-swatch rojo" />Solapamientos</label>
      <label><input type="checkbox" checked={huecos} onChange={(e) => onHuecos(e.target.checked)} />
        <span className="diagnostico-swatch amarillo" />Huecos interiores</label>
      <dl className="diagnostico-resumen">
        <dt>Huecos</dt><dd>{resumen.hueco.cantidad} · {areaTexto(resumen.hueco.area_m2)} m²<br />{apreciables('hueco')} superiores a 0,01 m²</dd>
        <dt>Solapamientos</dt><dd>{resumen.solapamiento.cantidad} · {areaTexto(resumen.solapamiento.area_m2)} m²<br />{apreciables('solapamiento')} superiores a 0,01 m²</dd>
        <dt>Comunas relacionadas</dt><dd>{resumen.comunas_involucradas.length}</dd>
      </dl>
      <div className="diagnostico-lista" tabIndex={0} aria-label="Incidencias ordenadas por superficie descendente">
        <table>
          <caption>Incidencias · mayor superficie primero</caption>
          <thead><tr><th>Incidencia</th><th>m²</th><th>Comunas</th></tr></thead>
          <tbody>{incidencias.map(({ properties: p }) => (
            <tr key={p.id} className={seleccion === p.id ? 'seleccionada' : ''}>
              <td><button type="button" aria-label={`Acercar a ${p.id}`} aria-pressed={seleccion === p.id}
                onClick={() => onSeleccion(p.id)}>{p.id}</button><small>{p.tipo}</small></td>
              <td>{areaTexto(p.area_m2)}</td><td>{comunasTexto(p)}</td>
            </tr>
          ))}</tbody>
        </table>
      </div>
      {seleccion && (() => {
        const p = incidencias.find((f) => f.properties.id === seleccion).properties
        return <div className="diagnostico-detalle" aria-live="polite">
          <strong>{p.id} · {p.clasificacion}</strong>
          <p>{p.posible_causa}</p><p>{p.efecto_reportes}</p>
          <p>{p.recomendacion}</p>
        </div>
      })()}
    </div>
  )
}

function Incidencia({ feature, seleccion, revision, onSeleccion }) {
  const map = useMap()
  const layer = useRef(null)
  const p = feature.properties
  const color = p.tipo === 'solapamiento' ? '#dc2626' : '#eab308'
  const acercar = () => {
    map.fitBounds(L.geoJSON(feature).getBounds(), { padding: [40, 40], maxZoom: 19 })
    layer.current?.openPopup(L.latLng(p.referencia[1], p.referencia[0]))
  }
  useEffect(() => {
    if (seleccion === p.id) {
      map.fitBounds(L.geoJSON(feature).getBounds(), { padding: [40, 40], maxZoom: 19 })
      layer.current?.openPopup(L.latLng(p.referencia[1], p.referencia[0]))
      map.getContainer().scrollIntoView({ block: 'nearest', behavior: 'smooth' })
    }
  }, [seleccion, revision, feature, map, p])
  return (
    <GeoJSON ref={layer} data={feature} pane="diagnostico-limites"
      style={{ color, fillColor: color, weight: 3, fillOpacity: 0.65 }}
      eventHandlers={{ click: () => onSeleccion(p.id) }}>
      <Popup pane="popupPane"><div className="map-popup diagnostico-popup">
        <strong>{p.id} · {p.tipo}</strong>
        <dl><dt>Comunas</dt><dd>{comunasTexto(p)}</dd>
          <dt>Superficie</dt><dd>{areaTexto(p.area_m2)} m²</dd>
          <dt>Latitud</dt><dd>{p.referencia[1].toFixed(8)}</dd>
          <dt>Longitud</dt><dd>{p.referencia[0].toFixed(8)}</dd>
          <dt>Estado</dt><dd>Pendiente de revisión</dd></dl>
        <p>{p.clasificacion}</p><p>{p.efecto_reportes}</p>
        <button type="button" onClick={acercar}>Acercar a incidencia</button>
      </div></Popup>
    </GeoJSON>
  )
}

export function CapaDiagnostico({ huecos, solapamientos, seleccion, revision, onSeleccion }) {
  return <Pane name="diagnostico-limites" style={{ zIndex: 450 }}>
    {incidencias.filter((f) => f.properties.tipo === 'hueco' ? huecos : solapamientos)
      .map((feature) => <Incidencia key={feature.properties.id} feature={feature}
        seleccion={seleccion} revision={revision} onSeleccion={onSeleccion} />)}
  </Pane>
}
