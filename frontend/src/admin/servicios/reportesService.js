import { listarTodosLosReportes } from '../../compartido/api/reportes'
import { reportesPrueba } from '../datos/reportesPrueba'

// Reportes que dibuja el mapa. Se piden a la API (`GET /api/admin/reportes`, simulada o real) y se
// convierten a la forma que usa el mapa. Si la API todavía no tiene ese endpoint, se usan los datos de prueba.
export async function obtenerReportesCiudadanos() {
  try {
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
  } catch {
    return reportesPrueba
  }
}
