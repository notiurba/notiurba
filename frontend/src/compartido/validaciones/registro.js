import {
  soloErrores,
  validarConfirmacion,
  validarCorreo,
  validarDireccion,
  validarNombre,
  validarNumeroDocumento,
  validarObligatorio,
  validarPasswordNueva,
  validarTelefono,
} from './campos.js'

// Una sola casilla: declara ser mayor de 18 años y acepta el aviso de privacidad (contrato 3.1).
export const MENSAJE_ACEPTAR = 'Debe confirmar que es mayor de 18 años y aceptar el tratamiento de sus datos.'

// Campos del paso 1 (datos personales). Sirve para saber a qué paso volver si la API
// rechaza uno de ellos.
export const CAMPOS_PASO_DATOS = ['nombres', 'apellidos', 'idTipoDocumento', 'numeroDocumento', 'telefono']

// Paso 1 de 2: datos personales.
export function validarPasoDatos(d) {
  return soloErrores({
    nombres: validarNombre(d.nombres, 'Escriba sus nombres'),
    apellidos: validarNombre(d.apellidos, 'Escriba sus apellidos'),
    idTipoDocumento: validarObligatorio(d.idTipoDocumento, 'Elija el tipo de documento'),
    numeroDocumento: d.idTipoDocumento ? validarNumeroDocumento(d.numeroDocumento, d.idTipoDocumento) : validarObligatorio(d.numeroDocumento, 'Escriba su número de documento'),
    telefono: validarTelefono(d.telefono),
  })
}

// Dónde vive: barrio y dirección (registro paso 2, «Complete su perfil» y «Editar mis datos»).
function validarResidencia(d) {
  return soloErrores({
    idBarrio: validarObligatorio(d.idBarrio, 'Seleccione su barrio'),
    direccionCorrespondencia: validarDireccion(d.direccionCorrespondencia),
  })
}

// Pantallas de un solo paso con todos los datos personales (sin correo ni contraseña).
export function validarPerfil(d) {
  return { ...validarPasoDatos(d), ...validarResidencia(d) }
}

// Paso 2 de 2: ubicación y acceso.
export function validarPasoAcceso(d) {
  return soloErrores({
    ...validarResidencia(d),
    correo: validarCorreo(d.correo),
    password: validarPasswordNueva(d.password),
    confirmar: validarConfirmacion(d.password, d.confirmar),
    aceptaPrivacidad: d.aceptaPrivacidad ? '' : MENSAJE_ACEPTAR,
  })
}

// Valores iniciales de los datos personales (los usan el registro y «Complete su perfil»).
export const DATOS_PERFIL_VACIOS = {
  nombres: '',
  apellidos: '',
  idTipoDocumento: '',
  numeroDocumento: '',
  telefono: '',
  idBarrio: '',
  direccionCorrespondencia: '',
  aceptaPrivacidad: false,
}

// Cuerpo de la API con los datos personales (contrato en docs/contexto/11-contrato-api.md): quita espacios
// sobrantes y pasa los ids a número. El teléfono vacío se omite, salvo en «Editar mis datos»
// (`enviarTelefonoVacio`), donde hay que enviarlo para poder borrarlo.
export function armarDatosPerfil(d, { enviarTelefonoVacio = false } = {}) {
  const datos = {
    nombres: d.nombres.trim(),
    apellidos: d.apellidos.trim(),
    idTipoDocumento: Number(d.idTipoDocumento),
    numeroDocumento: d.numeroDocumento.trim(),
    idBarrio: Number(d.idBarrio),
    direccionCorrespondencia: d.direccionCorrespondencia.trim(),
  }
  if (d.telefono || enviarTelefonoVacio) datos.telefono = d.telefono
  return datos
}

// Cuerpo del registro: los datos personales más correo y contraseña (la confirmación no se manda).
export function armarSolicitudRegistro(d) {
  return {
    ...armarDatosPerfil(d),
    correo: d.correo.trim(),
    password: d.password,
    aceptaPrivacidad: d.aceptaPrivacidad,
    declaraMayorEdad: d.aceptaPrivacidad, // es la misma casilla
  }
}
