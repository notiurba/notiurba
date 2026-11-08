import { peticion } from './cliente'

// Las pantallas usan un reporte «plano» (categoria como texto, barrio como texto, idComuna). La API
// (contrato 3.6) devuelve objetos anidados: categoria { idCategoria, nombre }, barrio { idBarrio, nombre }
// y comuna numérica. Esta función convierte una forma en la otra; también acepta el reporte ya plano.
function normalizarReporte(r) {
  const categoria = typeof r.categoria === 'object' && r.categoria ? r.categoria : null
  const barrio = typeof r.barrio === 'object' && r.barrio ? r.barrio : null
  return {
    ...r,
    idCategoria: categoria ? categoria.idCategoria : r.idCategoria,
    categoria: categoria ? categoria.nombre : r.categoria,
    barrio: barrio ? barrio.nombre : (r.barrio ?? ''),
    idComuna: r.comuna ?? r.idComuna ?? null,
    direccion: r.direccionIncidente ?? r.direccion ?? '',
    fotos: r.fotos ?? [],
    historial: r.historial ?? [],
  }
}

// Mis reportes (GET /api/reportes). Lista propia, más recientes primero. Cualquier fallo se avisa en pantalla.
export const listarMisReportes = async () => (await peticion('/api/reportes')).map(normalizarReporte)

// Reporte completo (contrato 3.6, solo reportes propios).
export const obtenerReporte = async (codigo) =>
  normalizarReporte(await peticion(`/api/reportes/${encodeURIComponent(codigo)}`))

// Crea el reporte (contrato 3.4). Devuelve { codigoSeguimiento, estado, creadoEn, vigenteHasta }.
const crearReporte = (solicitud) =>
  peticion('/api/reportes', { method: 'POST', body: JSON.stringify(solicitud) })

// Sube UNA foto de un reporte ya creado (contrato 3.5): multipart con la parte «foto», 1 MB como máximo.
function subirFoto(codigo, archivo) {
  const formulario = new FormData()
  formulario.append('foto', archivo)
  return peticion(`/api/reportes/${encodeURIComponent(codigo)}/fotos`, { method: 'POST', body: formulario })
}

// Pide a la API que envíe el reporte a la entidad (contrato sección 4; responde 202). Se llama al final,
// después de subir las fotos.
const enviarReporte = (codigo) =>
  peticion(`/api/reportes/${encodeURIComponent(codigo)}/enviar`, { method: 'POST' })

// tipo: 'RESPUESTA' (la entidad respondió) o 'RESUELTO'; radicado es opcional.
// Publica un reporte completo: lo crea, sube sus fotos una por una y pide el envío a la entidad.
// Si una foto falla el reporte sigue creado (se cuenta en `fotosFallidas`); lo mismo si falla el envío,
// porque ese endpoint aún falta en la API (contrato, sección 4). Solo falla si no se pudo crear el reporte.
export async function publicarReporte(solicitud, fotos) {
  const reporte = await crearReporte(solicitud)
  let fotosFallidas = 0
  for (const foto of fotos) {
    try {
      await subirFoto(reporte.codigoSeguimiento, foto)
    } catch {
      fotosFallidas += 1
    }
  }
  await enviarReporte(reporte.codigoSeguimiento).catch(() => null)
  return { codigoSeguimiento: reporte.codigoSeguimiento, fotosFallidas }
}

export const actualizarMiReporte = (codigo, tipo, radicado) =>
  peticion(`/api/reportes/${encodeURIComponent(codigo)}/actualizacion`, {
    method: 'PATCH',
    body: JSON.stringify({ tipo, radicado }),
  })

// Catálogo de categorías (EXISTE en el backend; exige sesión).
export const obtenerCategorias = () => peticion('/api/categorias')

// Panel de administración
export const listarTodosLosReportes = () => peticion('/api/admin/reportes')

export const cambiarEstadoReporte = (codigo, estado, observacion) =>
  peticion(`/api/admin/reportes/${encodeURIComponent(codigo)}/estado`, {
    method: 'PATCH',
    body: JSON.stringify({ estado, observacion }),
  })
