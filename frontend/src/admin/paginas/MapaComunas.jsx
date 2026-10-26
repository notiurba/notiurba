import { useEffect, useMemo, useRef, useState } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import 'leaflet.heat'
import {
  GeoJSON,
  MapContainer,
  Marker,
  Popup,
  TileLayer,
  useMap,
} from 'react-leaflet'
import { comunasTulua, getComunaProperties } from '../../compartido/geo/comunasTulua'
import {
  categoriasReporte,
  estadosReporte,
} from '../datos/reportesPrueba'
import { identificarComunaPorCoordenadas } from '../../compartido/utils/geoUtils'
import { CapaDiagnostico, PanelDiagnostico } from '../componentes/DiagnosticoLimites'
import { diagnosticoLimites } from '../datos/diagnosticoLimites'
import { obtenerReportesCiudadanos } from '../servicios/reportesService'
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'
import './MapaComunas.css'

delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
})

const TULUA_CENTER = [4.0847, -76.1954]

const comunaColors = [
  '#2f80ed',
  '#27ae60',
  '#f2994a',
  '#9b51e0',
  '#eb5757',
  '#00a6a6',
  '#f2c94c',
  '#6fcf97',
  '#bb6bd9',
  '#d35478',
]

function VistaTulua({ geoJson }) {
  const map = useMap()
  const fittedRef = useRef(false)
  const bounds = useMemo(() => L.geoJSON(geoJson).getBounds(), [geoJson])

  useEffect(() => {
    if (!bounds.isValid()) return undefined

    if (!fittedRef.current) {
      map.fitBounds(bounds, { padding: [28, 28], maxZoom: 14, animate: false })
      fittedRef.current = true
    }

    const control = L.control({ position: 'topleft' })
    const ajustar = () => map.fitBounds(bounds, {
      padding: [28, 28], maxZoom: 14, animate: false,
    })
    const centrar = (event) => {
      L.DomEvent.stop(event)
      map.off('moveend', ajustar)
      map.stop()
      map.closePopup()
      // flyToBounds animates long trips; fitBounds confirms the final framing.
      map.once('moveend', ajustar)
      map.flyToBounds(bounds, {
        padding: [28, 28],
        maxZoom: 14,
        animate: true,
        duration: 0.8,
      })
    }
    let button
    control.onAdd = () => {
      const container = L.DomUtil.create('div', 'leaflet-bar leaflet-control-centrar')
      button = L.DomUtil.create('button', '', container)
      button.type = 'button'
      button.title = 'Centrar en Tuluá'
      button.setAttribute('aria-label', 'Centrar en Tuluá')
      button.textContent = '\u2316'
      Object.assign(button.style, {
        width: '44px', height: '44px', padding: '0', border: '0',
        background: '#fff', color: '#1f2937', fontSize: '28px',
        lineHeight: '44px', cursor: 'pointer', display: 'block',
      })
      L.DomEvent.disableClickPropagation(container)
      L.DomEvent.disableScrollPropagation(container)
      L.DomEvent.on(button, 'click', centrar)
      return container
    }
    control.addTo(map)
    return () => {
      map.off('moveend', ajustar)
      if (button) L.DomEvent.off(button, 'click', centrar)
      control.remove()
    }
  }, [bounds, map])

  return null
}

function HeatmapLayer({ reportes, visible }) {
  const map = useMap()
  const layerRef = useRef(null)

  useEffect(() => {
    if (!visible) {
      if (layerRef.current) {
        map.removeLayer(layerRef.current)
        layerRef.current = null
      }
      return undefined
    }

    const points = reportes.map((reporte) => [
      reporte.latitud,
      reporte.longitud,
      0.75,
    ])

    if (!layerRef.current) {
      layerRef.current = L.heatLayer(points, {
        radius: 28,
        blur: 22,
        maxZoom: 16,
        minOpacity: 0.28,
        gradient: {
          0.25: '#2f80ed',
          0.45: '#27ae60',
          0.7: '#f2c94c',
          1: '#eb5757',
        },
      }).addTo(map)
    } else {
      layerRef.current.setLatLngs(points)
    }

    return () => {
      if (layerRef.current) {
        map.removeLayer(layerRef.current)
        layerRef.current = null
      }
    }
  }, [map, reportes, visible])

  return null
}

function ReportePopup({ reporte, resultadoComuna }) {
  return (
    <div className="map-popup">
      <strong>{reporte.id}</strong>
      <span>{reporte.categoria}</span>
      <p>{reporte.descripcion}</p>
      <dl>
        <dt>Estado</dt>
        <dd>{reporte.estado}</dd>
        <dt>Fecha</dt>
        <dd>{reporte.fechaCreacion}</dd>
        <dt>Comuna</dt>
        <dd>
          {resultadoComuna.status === 'pendiente_revision'
            ? 'Pendiente de revisión'
            : resultadoComuna.comuna
            ? `${resultadoComuna.comuna.nombre} (${resultadoComuna.status})`
            : 'Comuna sin determinar'}
        </dd>
        {resultadoComuna.status === 'pendiente_revision' && (
          <>
            <dt>Comunas candidatas</dt>
            <dd>
              {resultadoComuna.comunasCandidatas.map((comuna) => comuna.nombre).join(', ')}
            </dd>
          </>
        )}
      </dl>
    </div>
  )
}

export default function MapaComunas() {
  const [reportes, setReportes] = useState([])
  const [cargando, setCargando] = useState(true)
  const [mostrarComunas, setMostrarComunas] = useState(true)
  const [mostrarMarcadores, setMostrarMarcadores] = useState(true)
  const [mostrarCalor, setMostrarCalor] = useState(false)
  const [categoria, setCategoria] = useState('Todas')
  const [estado, setEstado] = useState('Todos')
  const [diagnostico, setDiagnostico] = useState(false)
  const [mostrarHuecos, setMostrarHuecos] = useState(true)
  const [mostrarSolapamientos, setMostrarSolapamientos] = useState(true)
  const [incidenciaSeleccionada, setIncidenciaSeleccionada] = useState(null)
  const [revisionSeleccion, setRevisionSeleccion] = useState(0)
  const seleccionarIncidencia = (id) => {
    const incidencia = diagnosticoLimites.features.find((f) => f.properties.id === id)
    if (incidencia.properties.tipo === 'hueco') setMostrarHuecos(true)
    else setMostrarSolapamientos(true)
    setIncidenciaSeleccionada(id)
    setRevisionSeleccion((value) => value + 1)
  }

  useEffect(() => {
    obtenerReportesCiudadanos()
      .then(setReportes)
      .finally(() => setCargando(false))
  }, [])

  const reportesConComuna = useMemo(
    () =>
      reportes.map((reporte) => ({
        ...reporte,
        resultadoComuna: identificarComunaPorCoordenadas(reporte, comunasTulua),
      })),
    [reportes],
  )

  const reportesFiltrados = useMemo(
    () =>
      reportesConComuna.filter((reporte) => {
        const categoriaOk = categoria === 'Todas' || reporte.categoria === categoria
        const estadoOk = estado === 'Todos' || reporte.estado === estado
        return categoriaOk && estadoOk
      }),
    [categoria, estado, reportesConComuna],
  )

  const conteoFueraZona = reportesFiltrados.filter(
    (reporte) => reporte.resultadoComuna.status === 'sin_determinar',
  ).length
  const conteoPendientes = reportesFiltrados.filter(
    (reporte) => reporte.resultadoComuna.status === 'pendiente_revision',
  ).length

  const styleComuna = (feature) => {
    const index = Number(feature.properties.COD_LOC_COM) - 1
    const color = comunaColors[index % comunaColors.length]

    return {
      color,
      fillColor: color,
      fillOpacity: 0.18,
      opacity: 0.95,
      weight: 2,
    }
  }

  const onEachComuna = (feature, layer) => {
    const comuna = getComunaProperties(feature)
    layer.bindPopup(`<strong>${comuna.nombre}</strong><br/>Codigo: ${comuna.id}`)
    layer.on({
      mouseover: (event) => {
        event.target.setStyle({
          fillOpacity: 0.36,
          weight: 4,
        })
        event.target.bringToFront()
      },
      mouseout: (event) => {
        event.target.setStyle(styleComuna(feature))
      },
    })
  }

  if (cargando) {
    return (
      <main className="app-loading">
        <span>Cargando mapa de comunas...</span>
      </main>
    )
  }

  return (
    <section className="map-shell" aria-labelledby="map-title">
      <div className="map-header">
        <div>
          <p className="eyebrow">Prototipo georreferenciado</p>
          <h1 id="map-title">NOTIURBA Tuluá</h1>
          <p>
            Visualización inicial de reportes ciudadanos, comunas urbanas y
            concentración espacial sobre OpenStreetMap.
          </p>
        </div>
        <div className="map-summary" aria-label="Resumen de reportes visibles">
          <strong>{reportesFiltrados.length}</strong>
          <span>reportes visibles</span>
          <small>{conteoFueraZona} con comuna sin determinar</small>
          <small>{conteoPendientes} pendientes de revisión geográfica</small>
        </div>
      </div>

      <div className="map-layout">
        <aside className="map-panel" aria-label="Controles del mapa">
          <div className="control-group">
            <h2>Capas</h2>
            <label>
              <input
                type="checkbox"
                checked={mostrarComunas}
                onChange={(event) => setMostrarComunas(event.target.checked)}
              />
              Comunas POT 2015 (provisional)
            </label>
            <label>
              <input
                type="checkbox"
                checked={mostrarMarcadores}
                onChange={(event) => setMostrarMarcadores(event.target.checked)}
              />
              Marcadores de reportes
            </label>
            <label>
              <input
                type="checkbox"
                checked={mostrarCalor}
                onChange={(event) => setMostrarCalor(event.target.checked)}
              />
              Mapa de calor
            </label>
          </div>

          <div className="control-group">
            <h2>Filtros</h2>
            <label>
              Categoria
              <select
                value={categoria}
                onChange={(event) => setCategoria(event.target.value)}
              >
                <option>Todas</option>
                {categoriasReporte.map((item) => (
                  <option key={item}>{item}</option>
                ))}
              </select>
            </label>
            <label>
              Estado
              <select
                value={estado}
                onChange={(event) => setEstado(event.target.value)}
              >
                <option>Todos</option>
                {estadosReporte.map((item) => (
                  <option key={item}>{item}</option>
                ))}
              </select>
            </label>
          </div>

          <div className="control-group">
            <label><input type="checkbox" checked={diagnostico}
              onChange={(e) => { setDiagnostico(e.target.checked); setIncidenciaSeleccionada(null) }} />
              Diagnóstico de límites</label>
            {diagnostico && <PanelDiagnostico huecos={mostrarHuecos} solapamientos={mostrarSolapamientos}
              onHuecos={setMostrarHuecos} onSolapamientos={setMostrarSolapamientos}
              seleccion={incidenciaSeleccionada} onSeleccion={seleccionarIncidencia} />}
          </div>

          <div className="control-group">
            <h2>Validación</h2>
            <p>
              POT 2015: referencia provisional, pendiente de validación
              administrativa. No confirma la división vigente en 2026.
              Los reportes son ficticios.
            </p>
          </div>
        </aside>

        <div className="map-frame">
          <MapContainer
            center={TULUA_CENTER}
            zoom={13}
            scrollWheelZoom
            className="leaflet-map"
          >
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            <VistaTulua geoJson={comunasTulua} />
            {(mostrarComunas || diagnostico) && (
              <GeoJSON
                key="comunas-tulua-pot2015"
                data={comunasTulua}
                style={styleComuna}
                onEachFeature={onEachComuna}
              />
            )}
            {diagnostico && <CapaDiagnostico huecos={mostrarHuecos} solapamientos={mostrarSolapamientos}
              seleccion={incidenciaSeleccionada} revision={revisionSeleccion} onSeleccion={seleccionarIncidencia} />}
            <HeatmapLayer reportes={reportesFiltrados} visible={mostrarCalor} />
            {mostrarMarcadores &&
              reportesFiltrados.map((reporte) => (
                <Marker
                  key={reporte.id}
                  position={[reporte.latitud, reporte.longitud]}
                >
                  <Popup>
                    <ReportePopup
                      reporte={reporte}
                      resultadoComuna={reporte.resultadoComuna}
                    />
                  </Popup>
                </Marker>
              ))}
          </MapContainer>
        </div>
      </div>
    </section>
  )
}
