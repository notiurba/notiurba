import { listarTodosLosReportes } from '../../compartido/api/reportes'

// Reportes que dibuja el mapa. Se piden a la API (`GET /api/admin/reportes`, simulada o real) y se
// convierten a la forma que usa el mapa. Los fallos se propagan para que la pantalla pueda mostrarlos.
export async function obtenerReportesCiudadanos() {
  const reportes = await listarTodosLosReportes()
  return reportes.map((r) => ({
    id: r.codigoSeguimiento,
    categoria: r.categoria,
    descripcion: r.descripcion,
    latitud: r.latitud,
    longitud: r.longitud,
    fechaCreacion: r.creadoEn.slice(0, 10),
    estado: r.estado,
  }))
}
