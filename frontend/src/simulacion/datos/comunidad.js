// Reportes de «otras personas» para que el panel de administración y el mapa de calor tengan datos.
// Los puntos son interiores de cada comuna POT 2015 (los mismos de admin/datos/reportesPrueba.js);
// no son ubicaciones reales de problemas.
const PUNTOS = [
  { comuna: 1, idBarrio: 7, latitud: 4.0725468993547125, longitud: -76.1883620903414 },
  { comuna: 2, idBarrio: 1, latitud: 4.087103454039726, longitud: -76.18310661852672 },
  { comuna: 3, idBarrio: 8, latitud: 4.09433846913491, longitud: -76.17977506145986 },
  { comuna: 4, idBarrio: 9, latitud: 4.086947036670706, longitud: -76.19832931626084 },
  { comuna: 5, idBarrio: 5, latitud: 4.070831579999258, longitud: -76.20140160026219 },
  { comuna: 6, idBarrio: 10, latitud: 4.079682068628073, longitud: -76.20808938935758 },
  { comuna: 7, idBarrio: 11, latitud: 4.094062082877395, longitud: -76.2142389394119 },
  { comuna: 8, idBarrio: 12, latitud: 4.100523222496509, longitud: -76.20657838539428 },
  { comuna: 9, idBarrio: 13, latitud: 4.1010056564985655, longitud: -76.20000920940825 },
  { comuna: 10, idBarrio: 14, latitud: 4.109735156955706, longitud: -76.19580270777875 },
]

const CATEGORIAS = [1, 2, 3, 4, 11, 13, 14, 8, 5, 9] // ids del catálogo
const ESTADOS = ['Enviado', 'En trámite', 'Resuelto', 'Recibido', 'Enviado', 'En trámite']
const DESVIOS = [0, 0.0012, -0.0009, 0.0006] // pequeños corrimientos para que los puntos no se superpongan

// `semilla` viene de almacen.js (arma un reporte con su historial). Devuelve 26 reportes de ejemplo.
export function crearComunidad(semilla) {
  return Array.from({ length: 26 }, (_, i) => {
    const punto = PUNTOS[i % PUNTOS.length]
    const desvio = DESVIOS[i % DESVIOS.length]
    const dia = 1 + ((i * 5) % 30)
    return semilla(
      100 + i,
      CATEGORIAS[(i + (i >> 2)) % CATEGORIAS.length],
      punto.idBarrio,
      'Dirección de ejemplo',
      ESTADOS[i % ESTADOS.length],
      dia,
      'Reporte de ejemplo de otra persona.',
      { latitud: punto.latitud + desvio, longitud: punto.longitud - desvio },
      false,
    )
  })
}
