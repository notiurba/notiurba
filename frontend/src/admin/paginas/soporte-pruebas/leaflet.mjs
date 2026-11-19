// Sustituto de Leaflet para probar MapaComunas sin navegador.
export default {
  Icon: { Default: { prototype: { _getIconUrl: 1 }, mergeOptions() {} } },
  geoJSON: () => ({ getBounds: () => ({ isValid: () => false }) }),
  heatLayer: () => ({ addTo() { return this }, setLatLngs() {} }),
}
