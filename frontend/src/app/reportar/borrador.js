// El borrador del reporte se guarda en sessionStorage: sobrevive a una recarga y a ir a iniciar sesión
// y volver. Las fotos no se guardan (un archivo no cabe ahí), solo los textos y la ubicación.
const CLAVE = 'notiurba.borrador'

const BORRADOR_VACIO = {
  idCategoria: '',
  latitud: null,
  longitud: null,
  direccion: '',
  idBarrio: '',
  descripcion: '',
  matricula: '',
  emergencia: false,
  // El servicio avisó que en ese lugar se repite un problema ya resuelto: la foto pasa a ser obligatoria.
  fotoObligatoria: false,
}

export function leerBorrador() {
  try {
    const guardado = sessionStorage.getItem(CLAVE)
    return guardado ? { ...BORRADOR_VACIO, ...JSON.parse(guardado) } : BORRADOR_VACIO
  } catch {
    return BORRADOR_VACIO
  }
}

export function guardarBorrador(borrador) {
  try {
    sessionStorage.setItem(CLAVE, JSON.stringify(borrador))
  } catch {
    // sin almacenamiento: el borrador vive solo mientras la pantalla esté abierta
  }
}

export function borrarBorrador() {
  try {
    sessionStorage.removeItem(CLAVE)
  } catch {
    // nada que borrar
  }
}

// ¿La persona ya escribió o eligió algo? Decide si al salir se pide confirmación.
export const tieneContenido = (borrador, cantidadFotos) =>
  Boolean(borrador.idCategoria || borrador.descripcion || cantidadFotos > 0)

// El código ya creado se conserva separado: recargar nunca debe provocar otro POST de creación.
const CLAVE_GUARDADO = 'notiurba.reporteGuardado'
export function leerGuardado() {
  try { return JSON.parse(sessionStorage.getItem(CLAVE_GUARDADO)) || null } catch { return null }
}
export function guardarAvance(guardado) {
  try {
    if (guardado) sessionStorage.setItem(CLAVE_GUARDADO, JSON.stringify(guardado))
    else sessionStorage.removeItem(CLAVE_GUARDADO)
  } catch { /* Sin almacenamiento, la recuperación permanece en memoria durante la sesión. */ }
}
