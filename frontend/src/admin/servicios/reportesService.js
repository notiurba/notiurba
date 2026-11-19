import { listarTodosLosReportes } from '../../compartido/api/reportes'

const tieneUbicacion = (r) => Number.isFinite(r.latitud) && Number.isFinite(r.longitud)

// La ubicación es opcional según la categoría: el mapa solo puede dibujar los que la traen.
export function separarPorUbicacion(reportes) {
  return {
    conUbicacion: reportes.filter(tieneUbicacion),
    sinUbicacion: reportes.filter((r) => !tieneUbicacion(r)),
  }
}

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
