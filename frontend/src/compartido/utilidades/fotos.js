import { MAX_BYTES_FOTO, TIPOS_FOTO } from '../validaciones/reporte.js'

const LADO_MAXIMO = 1600 // píxeles del lado más largo en el primer intento
const CALIDADES = [0.85, 0.7, 0.55] // de mejor a peor calidad JPG
const MAX_INTENTOS = 6 // cada intento fallido achica la imagen un 25 %

// Si el archivo no es JPG ni PNG devuelve el motivo; si sirve, devuelve ''.
export function motivoFormatoInvalido(archivo) {
  return TIPOS_FOTO.includes(archivo.type) ? '' : 'Solo se aceptan imágenes JPG o PNG.'
}

// Dibuja la imagen en un lienzo de ese tamaño y la guarda como JPG con la calidad dada.
function aJpg(imagen, escala, calidad) {
  const lienzo = document.createElement('canvas')
  lienzo.width = Math.round(imagen.width * escala)
  lienzo.height = Math.round(imagen.height * escala)
  const ctx = lienzo.getContext('2d')
  ctx.fillStyle = '#fff' // los PNG con transparencia quedarían negros en JPG
  ctx.fillRect(0, 0, lienzo.width, lienzo.height)
  ctx.drawImage(imagen, 0, 0, lienzo.width, lienzo.height)
  return new Promise((resolver) => lienzo.toBlob(resolver, 'image/jpeg', calidad))
}

// Recibe una foto y devuelve otra de 1 MB o menos.
//  - Si no es JPG ni PNG, lanza un Error con un mensaje en español.
//  - Si ya pesa 1 MB o menos, la devuelve tal cual.
//  - Si pesa más, la achica (escala y calidad) hasta que cabe; el resultado es JPG.
export async function reducirFoto(archivo, maxBytes = MAX_BYTES_FOTO) {
  const motivo = motivoFormatoInvalido(archivo)
  if (motivo) throw new Error(motivo)
  if (archivo.size <= maxBytes) return archivo

  const imagen = await createImageBitmap(archivo).catch(() => {
    throw new Error('No pudimos leer esta foto. Pruebe con otra.')
  })
  let escala = Math.min(1, LADO_MAXIMO / Math.max(imagen.width, imagen.height))
  for (let intento = 0; intento < MAX_INTENTOS; intento += 1) {
    for (const calidad of CALIDADES) {
      const blob = await aJpg(imagen, escala, calidad)
      if (blob && blob.size <= maxBytes) {
        imagen.close()
        const nombre = archivo.name.replace(/\.[^.]+$/, '') + '.jpg'
        return new File([blob], nombre, { type: 'image/jpeg' })
      }
    }
    escala *= 0.75
  }
  imagen.close()
  throw new Error('No pudimos reducir esta foto a 1 MB. Pruebe con otra.')
}
