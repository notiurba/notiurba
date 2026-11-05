// Colores y estilo de las comunas de Tuluá en los mapas (el del panel y el de «Nuevo reporte»).
const COLORES_COMUNA = [
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

// Estilo de Leaflet para una comuna; `resaltada` la marca más fuerte (por ejemplo, la del punto elegido).
export function estiloComuna(feature, resaltada = false) {
  const indice = Number(feature.properties.COD_LOC_COM) - 1
  const color = COLORES_COMUNA[indice % COLORES_COMUNA.length]
  return {
    color,
    fillColor: color,
    fillOpacity: resaltada ? 0.36 : 0.18,
    opacity: 0.95,
    weight: resaltada ? 4 : 2,
  }
}
