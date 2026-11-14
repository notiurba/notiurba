import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { useEffect, useRef } from 'react'
import { CENTRO_TULUA, comunasTulua } from '../../compartido/geo/comunasTulua'
import { estiloComuna } from '../../compartido/geo/estiloComunas'
import { identificarComunaPorCoordenadas } from '../../compartido/geo/geoUtils'
import { TEXTOS_UBICACION } from './ubicacionGps'
import './MapaUbicacion.css'

// Marcador dibujado con SVG (así no hay que empaquetar las imágenes por defecto de Leaflet).
const ICONO = L.divIcon({
  className: 'mapa-ubicacion__pin',
  html: '<svg width="34" height="44" viewBox="0 0 24 32"><path d="M12 0a10 10 0 0 0-10 10c0 7 10 18 10 18s10-11 10-18A10 10 0 0 0 12 0z" fill="#082f6b"/><circle cx="12" cy="10" r="4" fill="#fff"/></svg>',
  iconSize: [34, 44],
  iconAnchor: [17, 44],
})

// Mapa (Leaflet + OpenStreetMap) donde la persona toca para marcar el lugar del problema.
//  - latitud, longitud: números del punto marcado (o null).   - alElegir(latitud, longitud): se llama al tocar el mapa.
// Se reciben como números (no como objeto) para que el mapa solo se recentre cuando el punto cambia de verdad.
export default function MapaUbicacion({ latitud, longitud, alElegir, error }) {
  const contenedor = useRef(null)
  const mapa = useRef(null)
  const marcador = useRef(null)
  const capaComunas = useRef(null)
  const alElegirActual = useRef(alElegir)

  useEffect(() => {
    alElegirActual.current = alElegir
  })

  // El mapa se crea una sola vez y se destruye al salir de la pantalla.
  useEffect(() => {
    mapa.current = L.map(contenedor.current, { center: CENTRO_TULUA, zoom: 13, zoomControl: false })
    L.control.zoom({ zoomInTitle: 'Acercar mapa', zoomOutTitle: 'Alejar mapa' }).addTo(mapa.current)
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap',
    }).addTo(mapa.current)
    // Comunas del POT 2015 (referencia): coloreadas y con su etiqueta; no capturan toques del mapa.
    capaComunas.current = L.geoJSON(comunasTulua, {
      interactive: false,
      style: estiloComuna,
      onEachFeature: (comuna, capa) => {
        capa.bindTooltip(`COMUNA ${Number(comuna.properties.COD_LOC_COM)}`, {
          permanent: true,
          direction: 'center',
          className: 'mapa-ubicacion__etiqueta',
        })
      },
    }).addTo(mapa.current)
    // Las etiquetas solo se ven con zoom suficiente; más lejos se amontonan.
    const mostrarEtiquetas = () =>
      contenedor.current?.classList.toggle('mapa-ubicacion--sin-etiquetas', mapa.current.getZoom() < 14)
    mapa.current.on('zoomend', mostrarEtiquetas)
    mostrarEtiquetas()
    mapa.current.on('click', (evento) => alElegirActual.current(evento.latlng.lat, evento.latlng.lng))
    return () => {
      mapa.current.off()
      mapa.current.remove()
      mapa.current = null
      marcador.current = null
      capaComunas.current = null
    }
  }, [])

  // Cada vez que cambia el punto (toque o GPS) se mueve el marcador y se centra el mapa.
  useEffect(() => {
    if (!mapa.current) return
    if (latitud == null || longitud == null) {
      marcador.current?.remove()
      marcador.current = null
      capaComunas.current?.eachLayer(capa => capa.setStyle(estiloComuna(capa.feature)))
      return
    }
    const posicion = [latitud, longitud]
    if (marcador.current) marcador.current.setLatLng(posicion)
    else marcador.current = L.marker(posicion, { icon: ICONO, keyboard: false }).addTo(mapa.current)
    // Sin animación: una animación en curso al salir de la pantalla hace que Leaflet falle.
    mapa.current.setView(posicion, Math.max(mapa.current.getZoom(), 16), { animate: false })

    // La comuna donde cae el punto se resalta; las demás vuelven a su estilo normal.
    const { comuna } = identificarComunaPorCoordenadas({ latitud, longitud }, comunasTulua)
    capaComunas.current?.eachLayer((capa) =>
      capa.setStyle(estiloComuna(capa.feature, comuna?.id === capa.feature.properties.COD_LOC_COM)),
    )
  }, [latitud, longitud])

  function elegirConTeclado(evento) {
    if (evento.target !== contenedor.current || !['Enter', ' '].includes(evento.key)) return
    evento.preventDefault()
    const centro = mapa.current?.getCenter()
    if (centro) alElegirActual.current(centro.lat, centro.lng)
  }

  return (
    <div
      ref={contenedor}
      className="mapa-ubicacion"
      role="application"
      tabIndex={0}
      onKeyDown={elegirConTeclado}
      aria-label={TEXTOS_UBICACION.etiquetaMapa}
      aria-describedby={`ubicacion-instruccion${error ? ' ubicacion-error' : ''}`}
      aria-invalid={error ? true : undefined}
    />
  )
}
