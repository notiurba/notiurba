import { createElement as h } from 'react'

// Igual que Leaflet real, el marcador falla si su posición no es un par de números.
export const Marker = ({ position }) => {
  if (!Array.isArray(position) || position[0] == null) {
    throw new TypeError("Cannot read properties of null (reading 'lat')")
  }
  return h('marker', { lat: position[0], lng: position[1] })
}

export const MapContainer = ({ children }) => h('div', null, children)
export const Popup = ({ children }) => h('div', null, children)
export const TileLayer = () => null
export const GeoJSON = () => null
export const useMap = () => ({ removeLayer() {}, addLayer() {} })
