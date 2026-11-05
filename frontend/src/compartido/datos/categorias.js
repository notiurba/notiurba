import {
  IconoAlerta,
  IconoArbol,
  IconoBombillo,
  IconoCasa,
  IconoEscudo,
  IconoGorro,
  IconoGota,
  IconoInfo,
  IconoLlama,
  IconoPuntos,
  IconoRayo,
  IconoSemaforo,
  IconoVia,
  IconoCuadricula,
  IconoPapelera,
} from '../componentes/Iconos'

// Cómo se dibuja cada categoría (ícono y color de fondo, del sistema de diseño).
// Va por idCategoria porque el nombre puede ajustarse; lo que llega de la API es solo el id y el nombre.
const POR_ID = {
  1: { Icono: IconoVia, tono: 'vias' },
  2: { Icono: IconoBombillo, tono: 'alumbrado' },
  3: { Icono: IconoArbol, tono: 'basuras' },
  4: { Icono: IconoSemaforo, tono: 'senalizacion' },
  5: { Icono: IconoEscudo, tono: 'senalizacion' },
  6: { Icono: IconoCuadricula, tono: 'otro' },
  7: { Icono: IconoGorro, tono: 'otro' },
  8: { Icono: IconoAlerta, tono: 'senalizacion' },
  9: { Icono: IconoCasa, tono: 'vias' },
  10: { Icono: IconoPuntos, tono: 'otro' },
  11: { Icono: IconoGota, tono: 'agua' },
  12: { Icono: IconoLlama, tono: 'parques' },
  13: { Icono: IconoRayo, tono: 'parques' },
  14: { Icono: IconoPapelera, tono: 'basuras' },
  15: { Icono: IconoInfo, tono: 'agua' },
}

// «Gas natural» (Gases de Occidente). Con una fuga la app manda a llamar a la línea 164 en vez de enviar correo (RN-16).
export const ID_CATEGORIA_GAS = 12
export const LINEA_EMERGENCIA_GAS = '164'

export function visualCategoria(idCategoria) {
  return POR_ID[idCategoria] ?? { Icono: IconoPuntos, tono: 'otro' }
}

// Orden en que el diseño muestra las categorías (por idCategoria). Las que no estén aquí van al final
// y «Otro / eventos comunitarios» (10) siempre queda de última.
const ORDEN = [1, 2, 3, 4, 11, 13, 12, 14, 8, 9, 5, 6, 7, 15, 10]

export function ordenarCategorias(categorias) {
  const lugar = (c) => {
    const i = ORDEN.indexOf(c.idCategoria)
    return i === -1 ? ORDEN.length - 1 + c.idCategoria / 1000 : i // desconocidas: antes de «Otro»
  }
  return [...categorias].sort((a, b) => lugar(a) - lugar(b))
}
