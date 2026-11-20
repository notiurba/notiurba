import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import 'leaflet.heat'
import { GeoJSON, MapContainer, Marker, Popup, TileLayer, useMap } from 'react-leaflet'
import { CENTRO_TULUA, comunasTulua, getComunaProperties } from '../../compartido/geo/comunasTulua'
import { estiloComuna } from '../../compartido/geo/estiloComunas'
import useCargar from '../../compartido/hooks/useCargar'
import { obtenerCategorias } from '../../compartido/api/reportes'
import { ESTADOS_REPORTE, identificadorEstado } from '../../compartido/utilidades/reportes'
import CargaReportesMapa from '../componentes/CargaReportesMapa'
import { CapaDiagnostico, PanelDiagnostico } from '../componentes/DiagnosticoLimites'
import { diagnosticoLimites } from '../datos/diagnosticoLimites'
import { obtenerCalorAdministrativo, obtenerMapaAdministrativo, rangoFechasInvalido } from '../servicios/mapaAdminService'
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'
import './MapaComunas.css'

delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({ iconRetinaUrl: markerIcon2x, iconUrl: markerIcon, shadowUrl: markerShadow })
const VACIO = { total: 0, sinComuna: 0, truncado: false, reportes: [] }

function VistaTulua() {
  const map = useMap(); const yaAjustado = useRef(false)
  const limites = useMemo(() => L.geoJSON(comunasTulua).getBounds(), [])
  useEffect(() => {
    if (!limites.isValid()) return undefined
    if (!yaAjustado.current) { map.fitBounds(limites, { padding: [28, 28], maxZoom: 14, animate: false }); yaAjustado.current = true }
    const control = L.control({ position: 'topleft' }); let boton
    control.onAdd = () => { const caja = L.DomUtil.create('div', 'leaflet-bar leaflet-control-centrar'); boton = L.DomUtil.create('button', '', caja); boton.type = 'button'; boton.title = 'Centrar en Tuluá'; boton.setAttribute('aria-label', 'Centrar en Tuluá'); boton.textContent = '⌖'; Object.assign(boton.style, { width: '44px', height: '44px', border: '0', background: '#fff', fontSize: '28px' }); L.DomEvent.disableClickPropagation(caja); L.DomEvent.on(boton, 'click', () => { map.closePopup(); map.fitBounds(limites, { padding: [28, 28], maxZoom: 14 }) }); return caja }
    control.addTo(map); return () => { if (boton) L.DomEvent.off(boton, 'click'); control.remove() }
  }, [limites, map])
  return null
}

function HeatmapLayer({ puntos, visible }) {
  const map = useMap(); const capa = useRef(null); const puntosActuales = useRef(puntos)
  useEffect(() => {
    if (!visible) { if (capa.current) { map.removeLayer(capa.current); capa.current = null }; return undefined }
    const datos = puntosActuales.current.map(({ latitud, longitud, peso }) => [latitud, longitud, peso])
    capa.current = L.heatLayer(datos, { radius: 28, blur: 22, maxZoom: 16, minOpacity: 0.28, gradient: { 0.25: '#2f80ed', 0.45: '#27ae60', 0.7: '#f2c94c', 1: '#eb5757' } }).addTo(map)
    return () => { if (capa.current) { map.removeLayer(capa.current); capa.current = null } }
  }, [map, visible])
  useEffect(() => { puntosActuales.current = puntos; if (visible && capa.current) capa.current.setLatLngs(puntos.map(({ latitud, longitud, peso }) => [latitud, longitud, peso])) }, [puntos, visible])
  return null
}

function PopupReporte({ reporte }) {
  return <div className="map-popup"><strong>{reporte.codigoSeguimiento}</strong><span>{reporte.categoria?.nombre}</span><dl><dt>Estado</dt><dd>{reporte.estado}</dd><dt>Fecha</dt><dd>{reporte.creadoEn?.slice(0, 10)}</dd><dt>Comuna</dt><dd>{reporte.comuna ?? 'Sin asignar'}</dd><dt>Barrio</dt><dd>{reporte.barrio?.nombre ?? 'Sin asignar'}</dd></dl></div>
}

function ReportesCoincidentes({ reportes }) {
  const grupos = useMemo(() => {
    const porPunto = new Map()
    reportes.forEach((reporte) => {
      const clave = `${reporte.latitud},${reporte.longitud}`
      porPunto.set(clave, [...(porPunto.get(clave) ?? []), reporte])
    })
    return [...porPunto.values()].filter((grupo) => grupo.length > 1)
  }, [reportes])
  if (!grupos.length) return null
  return <section className="map-coincidentes" aria-labelledby="reportes-coincidentes-title"><h2 id="reportes-coincidentes-title">Reportes en la misma ubicación</h2><p>Estos marcadores se superponen en el mapa; consulte cada reporte desde esta lista accesible.</p>{grupos.map((grupo) => <ul key={`${grupo[0].latitud},${grupo[0].longitud}`}>{grupo.map((reporte) => <li key={reporte.codigoSeguimiento}><strong>{reporte.codigoSeguimiento}</strong><span>{reporte.categoria?.nombre}</span><small>{reporte.estado}</small></li>)}</ul>)}</section>
}

function Filtros({ filtros, setFiltros, categorias }) {
  const cambiar = (campo) => (evento) => setFiltros((actual) => ({ ...actual, pagina: 1, [campo]: evento.target.value }))
  return <div className="control-group"><h2>Filtros</h2><label>Categoría<select value={filtros.categoria} onChange={cambiar('categoria')}><option value="">Todas</option>{categorias.map((c) => <option key={c.idCategoria} value={c.idCategoria}>{c.nombre}</option>)}</select></label><label>Estado<select value={filtros.estado} onChange={cambiar('estado')}><option value="">Todos</option>{ESTADOS_REPORTE.map((e) => <option key={e} value={identificadorEstado(e)}>{e}</option>)}</select></label><label>Comuna<select value={filtros.comuna} onChange={cambiar('comuna')}><option value="">Todas</option>{Array.from({ length: 10 }, (_, i) => i + 1).map((n) => <option key={n} value={n}>Comuna {n}</option>)}</select></label><label>Fecha inicial<input type="date" value={filtros.desde} max={filtros.hasta || undefined} onChange={cambiar('desde')} /></label><label>Fecha final<input type="date" value={filtros.hasta} min={filtros.desde || undefined} onChange={cambiar('hasta')} /></label><button className="map-limpiar" type="button" onClick={() => setFiltros({ categoria: '', estado: '', comuna: '', desde: '', hasta: '', pagina: 1 })}>Limpiar filtros</button></div>
}

function Criticos({ calor, setFiltros }) {
  const criticos = calor?.comunasCriticas
  if (!criticos?.items?.length) return <p>No hay puntos críticos para estos filtros.</p>
  return <div className="map-criticos"><h2>Puntos críticos por comuna</h2><table><caption>Ranking calculado por la API administrativa</caption><thead><tr><th>#</th><th>Comuna</th><th>Reportes</th><th>Categoría principal</th></tr></thead><tbody>{criticos.items.map((item) => <tr key={`${item.comuna}-${item.posicion}`}><td>{item.posicion}</td><td>{item.comuna}</td><td>{item.total}</td><td>{item.categoriaPrincipal?.nombre ?? 'Sin categoría'}</td></tr>)}</tbody></table>{criticos.totalPaginas > 1 && <div className="map-paginacion"><button type="button" disabled={criticos.pagina <= 1} onClick={() => setFiltros((f) => ({ ...f, pagina: f.pagina - 1 }))}>Anterior</button><span>Página {criticos.pagina} de {criticos.totalPaginas}</span><button type="button" disabled={criticos.pagina >= criticos.totalPaginas} onClick={() => setFiltros((f) => ({ ...f, pagina: f.pagina + 1 }))}>Siguiente</button></div>}</div>
}

function Mapa({ respuesta, filtros, setFiltros }) {
  const [marcadores, setMarcadores] = useState(true); const [calorVisible, setCalorVisible] = useState(() => typeof window !== 'undefined' && window.location.pathname.endsWith('/puntos-criticos')); const [comunas, setComunas] = useState(true)
  const [diagnostico, setDiagnostico] = useState(false)
  const [mostrarHuecos, setMostrarHuecos] = useState(true)
  const [mostrarSolapamientos, setMostrarSolapamientos] = useState(true)
  const [incidenciaSeleccionada, setIncidenciaSeleccionada] = useState(null)
  const [revisionSeleccion, setRevisionSeleccion] = useState(0)
  const invalido = rangoFechasInvalido(filtros.desde, filtros.hasta)
  const cargarCalor = useCallback(() => calorVisible && !invalido ? obtenerCalorAdministrativo({ ...filtros, tamano: 5 }) : Promise.resolve(null), [calorVisible, filtros, invalido])
  const capaCalor = useCargar(cargarCalor)
  const categoriasCatalogo = useCargar(obtenerCategorias)
  const reportes = useMemo(() => respuesta.reportes ?? [], [respuesta.reportes])
  const categorias = useMemo(() => (categoriasCatalogo.datos ?? [...new Map(reportes.filter((r) => r.categoria).map((r) => [r.categoria.idCategoria, r.categoria])).values()]).sort((a, b) => a.nombre.localeCompare(b.nombre)), [categoriasCatalogo.datos, reportes])
  const seleccionarIncidencia = (id) => { const incidencia = diagnosticoLimites.features.find((item) => item.properties.id === id); if (incidencia?.properties.tipo === 'hueco') setMostrarHuecos(true); else setMostrarSolapamientos(true); setIncidenciaSeleccionada(id); setRevisionSeleccion((valor) => valor + 1) }
  const cadaComuna = (feature, layer) => { const comuna = getComunaProperties(feature); layer.bindPopup(`<strong>${comuna.nombre}</strong><br/>Código: ${comuna.id}`); layer.on({ mouseover: (evento) => { evento.target.setStyle({ fillOpacity: 0.36, weight: 4 }); evento.target.bringToFront() }, mouseout: (evento) => evento.target.setStyle(estiloComuna(feature)) }) }
  return <section className="map-shell" aria-labelledby="map-title"><div className="map-header"><div><p className="eyebrow">Panel administrativo</p><h1 id="map-title">Mapa de reportes vigentes</h1><p>La API administrativa excluye reportes vencidos y sin ubicación válida.</p></div><div className="map-summary"><strong>{respuesta.total ?? reportes.length}</strong><span>reportes visibles</span><small>{respuesta.sinComuna ?? 0} sin comuna asignada</small></div></div><p className="map-aviso" role="status">El mapa solo muestra reportes vigentes con ubicación válida; el listado conserva el historial completo.</p><div className="map-layout"><aside className="map-panel" aria-label="Controles del mapa"><div className="control-group"><h2>Capas</h2><label><input type="checkbox" checked={comunas} onChange={(e) => setComunas(e.target.checked)} />Comunas POT 2015 (provisional)</label><label><input type="checkbox" checked={marcadores} onChange={(e) => setMarcadores(e.target.checked)} />Marcadores de reportes</label><label><input type="checkbox" checked={calorVisible} onChange={(e) => setCalorVisible(e.target.checked)} />Mapa de calor</label></div><Filtros filtros={filtros} setFiltros={setFiltros} categorias={categorias} />{invalido && <p className="map-error" role="alert">La fecha inicial no puede ser posterior a la fecha final.</p>}{calorVisible && capaCalor.cargando && <p role="status">Cargando mapa de calor…</p>}{calorVisible && capaCalor.error && <p className="map-error" role="alert">No fue posible cargar el mapa de calor. <button type="button" onClick={capaCalor.recargar}>Reintentar</button></p>}{calorVisible && !capaCalor.cargando && !capaCalor.error && <Criticos calor={capaCalor.datos} setFiltros={setFiltros} />}<ReportesCoincidentes reportes={reportes} /><div className="control-group"><label><input type="checkbox" checked={diagnostico} onChange={(e) => { setDiagnostico(e.target.checked); setIncidenciaSeleccionada(null) }} />Diagnóstico de límites</label>{diagnostico && <PanelDiagnostico huecos={mostrarHuecos} solapamientos={mostrarSolapamientos} onHuecos={setMostrarHuecos} onSolapamientos={setMostrarSolapamientos} seleccion={incidenciaSeleccionada} onSeleccion={seleccionarIncidencia} />}</div></aside><div className="map-frame"><MapContainer center={CENTRO_TULUA} zoom={13} scrollWheelZoom className="leaflet-map"><TileLayer attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" /><VistaTulua />{(comunas || diagnostico) && <GeoJSON data={comunasTulua} style={estiloComuna} onEachFeature={cadaComuna} />}{diagnostico && <CapaDiagnostico huecos={mostrarHuecos} solapamientos={mostrarSolapamientos} seleccion={incidenciaSeleccionada} revision={revisionSeleccion} onSeleccion={seleccionarIncidencia} />}<HeatmapLayer puntos={capaCalor.datos?.puntos ?? []} visible={calorVisible && !capaCalor.error} />{marcadores && reportes.map((reporte) => <Marker key={reporte.codigoSeguimiento} position={[reporte.latitud, reporte.longitud]}><Popup autoPan keepInView autoPanPaddingTopLeft={[72, 96]} autoPanPaddingBottomRight={[24, 24]} maxWidth={280}><PopupReporte reporte={reporte} /></Popup></Marker>)}</MapContainer></div></div></section>
}

export default function MapaComunas() {
  const [filtros, setFiltros] = useState({ categoria: '', estado: '', comuna: '', desde: '', hasta: '', pagina: 1 })
  const invalido = rangoFechasInvalido(filtros.desde, filtros.hasta)
  const cargarMapa = useCallback(() => {
    return invalido ? Promise.resolve(VACIO) : obtenerMapaAdministrativo({ categoria: filtros.categoria, estado: filtros.estado, comuna: filtros.comuna, desde: filtros.desde, hasta: filtros.hasta })
  }, [filtros, invalido])
  return <CargaReportesMapa cargador={cargarMapa}>{(respuesta) => <Mapa respuesta={respuesta || VACIO} filtros={filtros} setFiltros={setFiltros} />}</CargaReportesMapa>
}
