// «Base de datos» de la simulación: guarda en el navegador (localStorage) los reportes y el perfil
// de la persona, para que lo que se haga en una pantalla se vea en las demás y sobreviva a recargar.
// Si localStorage no está disponible, se usa solo la memoria de la pestaña.
import barrios from './datos/catalogos/barrios.json'
import categorias from './datos/catalogos/categorias.json'
import { crearComunidad } from './datos/comunidad'

const CLAVE = 'notiurba.simulacion.v3'

const PERFIL_INICIAL = {
  nombres: 'María Camila',
  apellidos: 'Gómez Rojas',
  idTipoDocumento: 1,
  numeroDocumento: '1098765432',
  correo: 'ciudadano@correo.com',
  telefono: '3001234567',
  idBarrio: 5,
  direccionCorrespondencia: 'Cra 26 #28-40',
}

const PERFIL_NUEVO = {
  nombres: 'Ana',
  apellidos: 'Rojas Díaz',
  idTipoDocumento: 1,
  numeroDocumento: '1112223334',
  correo: 'nuevo@correo.com',
  telefono: '',
  idBarrio: 2,
  direccionCorrespondencia: 'Cra 20 #15-10',
}

const CORREO_CIUDADANA = 'ciudadano@correo.com'

function fecha(dia, hora = 9) {
  return new Date(2026, 9, dia, hora, 0).toISOString()
}

// `punto` ({ latitud, longitud }) es opcional; `esMio` indica si el reporte es de la persona que inició sesión.
function semilla(codigo, idCategoria, idBarrio, direccion, estado, dia, descripcion, punto = null, esMio = true) {
  const barrio = barrios.find((b) => b.idBarrio === idBarrio)
  const categoria = categorias.find((c) => c.idCategoria === idCategoria)
  const creadoEn = fecha(dia)
  return {
    idReporte: Number(codigo),
    codigoSeguimiento: `NU-${String(codigo).padStart(6, '0')}`,
    idCategoria,
    categoria: categoria.nombre,
    descripcion,
    estado,
    barrio: barrio.nombre,
    idBarrio,
    idComuna: barrio.idComuna,
    direccion,
    latitud: punto?.latitud ?? 4.0847,
    longitud: punto?.longitud ?? -76.1954,
    propietario: esMio ? CORREO_CIUDADANA : null, // correo de quien lo hizo (null = otra persona)
    fotos: [],
    radicadoOficial: null,
    creadoEn,
    historial: [{ estado, observacion: null, cambiadoEn: creadoEn }],
  }
}

const REPORTES_INICIALES = [
  semilla(42, 1, 5, 'Cra 26 #28-40', 'Recibido', 17, 'Hueco profundo frente a la panadería.', { latitud: 4.0711, longitud: -76.2018 }),
  semilla(31, 2, 5, 'Cl 30 con Cra 24', 'En trámite', 9, 'Bombillo apagado en la esquina, la zona queda oscura.', { latitud: 4.0702, longitud: -76.2009 }),
  semilla(27, 3, 5, 'Cra 27 #25-10', 'Enviado', 5, 'Basura acumulada desde hace una semana.', { latitud: 4.0715, longitud: -76.2008 }),
  semilla(19, 4, 5, 'Cra 25 con Cl 28', 'Resuelto', 1, 'Semáforo sin funcionar.', { latitud: 4.0699, longitud: -76.2021 }),
  ...crearComunidad(semilla),
]

let enMemoria = null

function leer() {
  if (enMemoria) return enMemoria
  try {
    const guardado = localStorage.getItem(CLAVE)
    if (guardado) enMemoria = JSON.parse(guardado)
  } catch {
    // sin localStorage o con datos dañados: se parte de los datos iniciales
  }
  enMemoria ??= {
    sesion: CORREO_CIUDADANA,
    perfiles: { [CORREO_CIUDADANA]: PERFIL_INICIAL, 'nuevo@correo.com': PERFIL_NUEVO },
    reportes: REPORTES_INICIALES,
  }
  return enMemoria
}

function guardar() {
  try {
    localStorage.setItem(CLAVE, JSON.stringify(enMemoria))
  } catch {
    // ignorar: la simulación sigue funcionando en memoria
  }
}

// Quién inició sesión por última vez (la simulación no valida el token, solo recuerda el correo).
export function iniciarSesion(correo) {
  leer().sesion = correo
  guardar()
}

export const obtenerPerfil = () => {
  const datos = leer()
  return datos.perfiles[datos.sesion] ?? PERFIL_INICIAL
}

export function guardarPerfil(cambios) {
  const datos = leer()
  datos.perfiles[datos.sesion] = { ...obtenerPerfil(), ...cambios }
  guardar()
  return datos.perfiles[datos.sesion]
}

export const listarReportes = () => leer().reportes

// Solo los reportes de quien inició sesión.
export const listarMisReportes = () => leer().reportes.filter((r) => r.propietario === leer().sesion)

export const buscarReporte = (codigo) => leer().reportes.find((r) => r.codigoSeguimiento === codigo)

export function crearReporte(solicitud) {
  const datos = leer()
  const idReporte = Math.max(0, ...datos.reportes.map((r) => r.idReporte)) + 1
  const barrio = barrios.find((b) => b.idBarrio === solicitud.idBarrio) // puede no venir (es opcional)
  const categoria = categorias.find((c) => c.idCategoria === solicitud.idCategoria)
  const creadoEn = new Date().toISOString()
  const reporte = {
    idReporte,
    codigoSeguimiento: `NU-${String(idReporte).padStart(6, '0')}`,
    idCategoria: solicitud.idCategoria,
    categoria: categoria.nombre,
    descripcion: solicitud.descripcion,
    emergencia: solicitud.emergencia === true,
    estado: 'Recibido', // la API real lo deja en «Recibido»; pasa a «Enviado» cuando sale el correo
    barrio: barrio?.nombre ?? '',
    idBarrio: barrio?.idBarrio ?? null,
    idComuna: barrio?.idComuna ?? null,
    direccion: solicitud.direccionIncidente ?? '',
    latitud: solicitud.latitud ?? null,
    longitud: solicitud.longitud ?? null,
    propietario: datos.sesion,
    fotos: [],
    radicadoOficial: null,
    creadoEn,
    historial: [
      { estado: 'Recibido', observacion: null, cambiadoEn: creadoEn },
    ],
  }
  datos.reportes = [reporte, ...datos.reportes]
  guardar()
  return reporte
}

// Agrega una foto (hasta 3) a un reporte propio. La simulación no guarda la imagen, solo su lugar.
export function agregarFoto(codigo) {
  const reporte = buscarReporte(codigo)
  const orden = reporte.fotos.length + 1
  reporte.fotos = [...reporte.fotos, { orden, url: null }]
  guardar()
  return orden
}

// El envío a la entidad: el reporte pasa a «Enviado».
export const marcarEnviado = (codigo) => cambiarEstado(codigo, 'Enviado')

export function guardarEnvio(codigo, estado) {
  buscarReporte(codigo).envioSimulado = estado
  guardar()
}

// Cambia el estado de un reporte y lo anota en el historial.
export function cambiarEstado(codigo, estado, observacion = null, radicado = null) {
  const reporte = buscarReporte(codigo)
  reporte.estado = estado
  if (radicado) reporte.radicadoOficial = radicado
  reporte.historial = [...reporte.historial, { estado, observacion, cambiadoEn: new Date().toISOString() }]
  guardar()
  return reporte
}

export function borrarReportes() {
  // «Eliminar mis datos» borra solo los reportes de la persona; los de otras personas siguen.
  const datos = leer()
  datos.reportes = datos.reportes.filter((r) => r.propietario !== datos.sesion)
  guardar()
}

export function borrarTodo() {
  enMemoria = null
  try {
    localStorage.removeItem(CLAVE)
  } catch {
    // ignorar
  }
}

// La persona avisa algo: queda una nota a su nombre con el estado que ya tenía (no cambia el estado oficial).
export function agregarNotaDelCiudadano(codigo, observacion, radicado = null) {
  const reporte = buscarReporte(codigo)
  if (radicado) reporte.radicadoOficial = radicado
  reporte.historial = [...reporte.historial,
    { estado: reporte.estado, observacion, cambiadoEn: new Date().toISOString(), delCiudadano: true }]
  guardar()
  return reporte
}

