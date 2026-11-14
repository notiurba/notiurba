// Textos de los avisos al crear un reporte. Voz de la app: «usted», sin palabras técnicas.
import { MAX_DESCRIPCION, MIN_DESCRIPCION } from '../validaciones/reporte.js'

const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre']

// «2026-11-12» → «12 de noviembre». Se lee la fecha tal cual (sin zona horaria) para no correr el día.
export function fechaCorta(iso) {
  const partes = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso ?? '')
  if (!partes) return ''
  const mes = MESES[Number(partes[2]) - 1]
  return mes ? `${Number(partes[3])} de ${mes}` : ''
}

export const TEXTOS_DESCRIPCION = {
  titulo: 'Cuéntenos qué pasa',
  etiqueta: '¿Qué problema quiere reportar?',
  ejemplo: 'Ej. Hay un hueco grande en la calle desde hace dos semanas y las motos frenan de golpe.',
  guia: 'Cuente qué pasa, desde cuándo y a quién afecta.',
  reglas:
    `Entre ${MIN_DESCRIPCION} y ${MAX_DESCRIPCION} caracteres. No escriba datos de otras personas. Un resumen de lo que escriba, sin su nombre, puede verlo quien reporte lo mismo.`,
}

export const TEXTOS_FOTO = {
  etiqueta: 'Foto reciente del problema',
  opcional: '(opcional)',
  obligatoria: '(obligatoria)',
  ayuda: 'Una foto tomada hoy ayuda a que la entidad entienda y atienda el problema más rápido.',
  ayudaObligatoria: 'En este lugar ya se resolvió un reporte parecido. Agregue una foto de hoy que muestre que el problema sigue.',
}

export const TEXTOS_FOTO_NECESARIA = {
  titulo: 'Hace falta una foto',
  texto: 'En este lugar ya se resolvió un reporte parecido. Si el problema sigue o volvió, agregue una foto reciente que lo muestre.',
  accion: 'Agregar foto',
}

export function textoLimiteDiario(datos) {
  const maximo = Number(datos?.maximo)
  const cuantos = Number.isInteger(maximo) && maximo > 0 ? (maximo === 1 ? '1 reporte' : `${maximo} reportes`) : 'el máximo de reportes'
  return {
    titulo: 'Llegó al límite de hoy',
    texto: `Hoy ya envió ${cuantos}, que es el máximo por día. Podrá enviar otro desde mañana. Su reporte no se perdió: lo que escribió sigue aquí.`,
  }
}

// El aviso de reporte parecido, según quién lo hizo, cuántos hay y si ya salió a la entidad.
// Solo datos base: fecha, resumen, cantidad y, si es de la misma persona, su código.
export function textoDuplicado(datos = {}) {
  const fecha = fechaCorta(datos.creadoEn)
  const cuando = fecha ? `El ${fecha}` : 'Hace poco'
  const enviado = datos.estadoExistente && datos.estadoExistente !== 'Recibido'
  const propio = datos.esPropio === true
  const codigo = propio && typeof datos.codigoExistente === 'string' ? datos.codigoExistente : null
  const cita = datos.descripcion ? `«${datos.descripcion}»` : ''

  let inicio
  if (propio) {
    inicio = `${cuando} usted reportó algo parecido en este mismo lugar${codigo ? ` (${codigo})` : ''}.`
  } else if (Number(datos.cantidad) > 1) {
    const reciente = fecha ? `El más reciente, del ${fecha}` : 'El más reciente'
    inicio = `Ya hay ${Number(datos.cantidad)} reportes parecidos. ${reciente}${cita ? `: ${cita}` : ''}.`
  } else {
    inicio = `${cuando} una persona reportó en esta misma categoría${cita ? `: ${cita}` : ''}.`
  }
  const estado = enviado
    ? 'Ya lo enviamos a la entidad. Puede ser lo mismo que usted quiere reportar.'
    : 'Todavía no se envía a la entidad.'

  return {
    titulo: propio && enviado ? 'Ya envió un reporte parecido' : 'Ya hay un reporte parecido',
    texto: `${inicio} ${estado} Si es un problema distinto, puede enviarlo de todas maneras.`,
    codigo,
  }
}
