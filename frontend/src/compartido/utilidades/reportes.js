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
