// Alias de presentación: los IDs y el nombre completo siempre proceden de la API.
const PRESENTACION = {
  1: ['Vías, huecos y andenes', 'Huecos, grietas, andenes rotos y daños de infraestructura.'],
  2: ['Alumbrado y convivencia', 'Luminarias apagadas, ocupación del espacio público o problemas de convivencia.'],
  3: ['Basuras y ambiente', 'Acumulación de residuos, quemas, tala o afectaciones ambientales.'],
  4: ['Señales, semáforos y movilidad', 'Semáforos dañados, señales caídas o problemas de movilidad.'],
  11: ['Agua y alcantarillado', 'Fugas de agua, falta del servicio o alcantarillas obstruidas.'],
  13: ['Energía eléctrica', 'Apagones o daños en la red eléctrica. Ante riesgo inmediato use el canal de emergencia.'],
  12: ['Gas natural', 'Fallas no urgentes. Si huele a gas o hay fuga, llame al 164.'],
  14: ['Recolección de basuras y barrido', 'El camión no pasó o hay una falla del servicio de aseo.'],
  8: ['Inundaciones y deslizamientos', 'Reporte afectaciones. La competencia está pendiente de validación; no es atención inmediata.'],
  9: ['Vivienda', 'Problemas de vivienda. La competencia está pendiente de validación.'],
  5: ['Riesgos sanitarios y salud pública', 'Plagas, aguas estancadas u otras condiciones que afecten la salud pública.'],
  6: ['Construcciones y licencias', 'Construcciones sin licencia o problemas con el uso del suelo.'],
  7: ['Colegios y servicios educativos', 'Problemas de infraestructura o servicios en colegios y escuelas.'],
  10: ['Otro problema', 'Describa el caso si no encuentra una categoría. Se revisará su clasificación.'],
}
export const ORDEN_REPORTE = [1, 2, 3, 4, 11, 13, 12, 14, 8, 9, 5, 6, 7, 10]

// Conserva todas las opciones devueltas por la API, también las añadidas después del diseño.
export function prepararCategorias(rutas) {
  const orden = (id) => id === 10 ? Number.MAX_SAFE_INTEGER
    : ORDEN_REPORTE.includes(id) ? ORDEN_REPORTE.indexOf(id) : ORDEN_REPORTE.length
  return rutas
    .map(r => ({ ...r, nombre: r.categoria ?? r.nombre,
      etiqueta: PRESENTACION[r.idCategoria]?.[0] ?? r.categoria ?? r.nombre,
      descripcion: PRESENTACION[r.idCategoria]?.[1] ?? r.descripcion ?? '' }))
    .sort((a, b) => orden(a.idCategoria) - orden(b.idCategoria))
}

export function esEmergencia(borrador, categoria) {
  return borrador.emergencia === true || Boolean(categoria?.esGas && /(fuga|olor.*gas|escape.*gas)/i.test(borrador.descripcion))
}
