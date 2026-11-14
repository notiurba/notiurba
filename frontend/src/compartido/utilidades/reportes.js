// Reglas de los reportes que usan varias pantallas (la app ciudadana y el panel).

export const ESTADOS_REPORTE = ['Recibido', 'Enviado', 'En trámite', 'Resuelto']
export const ESTADO_ENVIADO = 'Enviado'
const ESTADO_EN_TRAMITE = 'En trámite'
export const ESTADO_RESUELTO = 'Resuelto'

// Estados en los que la persona ya puede avisar que la entidad respondió o que se resolvió.
export const puedeActualizar = (reporte) => reporte.estado === ESTADO_ENVIADO || reporte.estado === ESTADO_EN_TRAMITE

// «Dirección · Barrio · Comuna 3», sin los datos que falten.
export const textoLugar = (reporte) =>
  [reporte.direccion, reporte.barrio, reporte.idComuna && `Comuna ${reporte.idComuna}`].filter(Boolean).join(' · ')

// Lo que se lee en el detalle: el lugar escrito, el punto del mapa o, si no hay nada, que no hay ubicación.
export const lugarDelReporte = (reporte) =>
  textoLugar(reporte) || (reporte.latitud != null && reporte.longitud != null ? 'Punto marcado en el mapa' : 'Sin ubicación')

// La API recibe el estado como identificador, no como la etiqueta que se lee en pantalla.
export const identificadorEstado = (estado) =>
  ESTADOS_REPORTE.includes(estado) ? estado.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase().replace(/ /g, '_') : estado
// Lo contrario: de «EN_TRAMITE» a «En trámite».
export const etiquetaEstado = (identificador) => ESTADOS_REPORTE.find((e) => identificadorEstado(e) === identificador) ?? identificador

// El estado que muestra el selector del panel: el elegido si sigue siendo posible; si no, el primero que se puede.
export const estadoElegido = (opciones, elegido, inicial = opciones[0] ?? '') => (opciones.includes(elegido) ? elegido : inicial)

// A qué estado puede pasar el administrador a mano: no se retrocede y «Enviado» lo pone el envío del correo.
export const estadosSiguientes = (estado) => {
  const actual = ESTADOS_REPORTE.indexOf(estado)
  return ESTADOS_REPORTE.filter((e, i) => i > actual && e !== ESTADO_ENVIADO)
}

// Filtros del panel: solo lo que tiene reportes. «Todos» y el estado elegido se quedan aunque estén en cero.
export const comunasConReportes = (reportes) =>
  [...new Set(reportes.map((r) => r.idComuna).filter((c) => c != null))].sort((a, b) => a - b)
export const estadosConReportes = (estados, cuenta, elegido) =>
  estados.filter((e) => e === 'Todos' || e === elegido || cuenta(e) > 0)

// El aviso que la persona dejó en «Actualizar mi reporte» y que el administrador aún no respondió: la última nota del
// historial es suya y el reporte no está resuelto. No cambia el estado; solo le avisa al administrador.
export const avisoDelCiudadano = (reporte) => {
  const ultimo = reporte.historial?.at(-1)
  return ultimo?.delCiudadano === true && reporte.estado !== ESTADO_RESUELTO ? ultimo : null
}

