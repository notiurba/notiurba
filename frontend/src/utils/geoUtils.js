import { getComunaProperties } from '../data/geo/comunasTulua'

const EPSILON = 1e-10

function pointOnSegment([x, y], [x1, y1], [x2, y2]) {
  const cross = (x - x1) * (y2 - y1) - (y - y1) * (x2 - x1)
  const length = Math.hypot(x2 - x1, y2 - y1)
  if (length === 0) return Math.hypot(x - x1, y - y1) <= EPSILON
  if (Math.abs(cross) > EPSILON * length) return false

  const minX = Math.min(x1, x2) - EPSILON
  const maxX = Math.max(x1, x2) + EPSILON
  const minY = Math.min(y1, y2) - EPSILON
  const maxY = Math.max(y1, y2) + EPSILON

  return x >= minX && x <= maxX && y >= minY && y <= maxY
}

function pointInRing(point, ring) {
  let inside = false

  for (let i = 0, j = ring.length - 1; i < ring.length; j = i, i += 1) {
    const current = ring[i]
    const previous = ring[j]

    if (pointOnSegment(point, previous, current)) {
      return 'boundary'
    }

    const intersects =
      current[1] > point[1] !== previous[1] > point[1] &&
      point[0] <
        ((previous[0] - current[0]) * (point[1] - current[1])) /
          (previous[1] - current[1]) +
          current[0]

    if (intersects) inside = !inside
  }

  return inside
}

function pointInPolygonCoordinates(point, coordinates) {
  const outerResult = pointInRing(point, coordinates[0])
  if (outerResult === 'boundary') return 'boundary'
  if (!outerResult) return false

  for (let i = 1; i < coordinates.length; i += 1) {
    const holeResult = pointInRing(point, coordinates[i])
    if (holeResult === 'boundary') return 'boundary'
    if (holeResult) return false
  }

  return true
}

function pointInFeature(point, feature) {
  const { geometry } = feature

  if (geometry.type === 'Polygon') {
    return pointInPolygonCoordinates(point, geometry.coordinates)
  }

  if (geometry.type === 'MultiPolygon') {
    for (const polygon of geometry.coordinates) {
      const result = pointInPolygonCoordinates(point, polygon)
      if (result) return result
    }
  }

  return false
}

export function identificarComunaPorCoordenadas({ latitud, longitud }, comunasGeoJson) {
  const point = [longitud, latitud]
  const matches = comunasGeoJson.features
    .map((feature) => ({
      feature,
      result: pointInFeature(point, feature),
    }))
    .filter(({ result }) => result)

  if (matches.length === 0) {
    return {
      status: 'sin_determinar',
      comuna: null,
      mensaje: 'Comuna sin determinar: el punto no coincide con la cobertura cargada; no implica estar fuera de Tulua.',
    }
  }

  const boundaryMatches = matches.filter(({ result }) => result === 'boundary')
  if (boundaryMatches.length > 0 || matches.length > 1) {
    return {
      status: 'pendiente_revision',
      comuna: null,
      comunasCandidatas: matches.map(({ feature }) => getComunaProperties(feature)),
      mensaje: 'Pendiente de revision: el punto coincide con un limite o con mas de un poligono.',
    }
  }

  return {
    status: 'dentro',
    comuna: getComunaProperties(matches[0].feature),
    mensaje: 'El punto esta dentro de una comuna de la capa provisional.',
  }
}
