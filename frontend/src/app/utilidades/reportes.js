// Estados en los que la persona ya puede avisar que la entidad respondió o que se resolvió.
export const puedeActualizar = (reporte) => reporte.estado === 'Enviado' || reporte.estado === 'En trámite'
