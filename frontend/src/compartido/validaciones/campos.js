// Reglas de validación de los formularios. Cada función recibe lo que escribió la persona y
// devuelve el mensaje de error, o '' (texto vacío) si todo está bien. Así cualquier pantalla
// las reutiliza y se pueden probar sin abrir el navegador.

const FORMATO_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const FORMATO_TELEFONO = /^\d{10}$/

// Largos máximos de cada casilla. Son más estrictos que los del contrato (nombres 255, dirección 300)
// porque en la práctica nadie escribe tanto; los usan tanto la validación como el `maxLength` de las casillas.
export const LIMITES = {
  nombre: 60, // nombres y apellidos, cada uno
  direccion: 150,
  correo: 254, // límite del contrato
  password: 72, // límite del contrato (bytes)
  telefono: 10,
  documento: 15,
  matricula: 50, // matrícula o cuenta del servicio, límite del contrato
  radicado: 50, // radicado oficial, límite del contrato
}

// Reglas del número de documento según el tipo (ids del contrato: 1 CC, 2 CE, 3 PPT, 4 PAS).
// CC y CE: solo dígitos, de 6 a 10. Pasaporte y PPT: letras y números, de 6 a 15.
const REGLAS_DOCUMENTO = {
  1: { patron: /^\d{6,10}$/, mensaje: 'La cédula de ciudadanía lleva de 6 a 10 dígitos, sin puntos ni espacios.' },
  2: { patron: /^\d{6,10}$/, mensaje: 'La cédula de extranjería lleva de 6 a 10 dígitos, sin puntos ni espacios.' },
}
const REGLA_DOCUMENTO_GENERAL = {
  patron: /^[A-Za-z0-9]{6,15}$/,
  mensaje: 'Escriba de 6 a 15 letras o números, sin espacios ni símbolos.',
}

const MAX_DIGITOS_CEDULA = 10

// Las cédulas (CC y CE) son solo números; el pasaporte y el PPT admiten letras.
export const documentoEsNumerico = (idTipo) => Number(idTipo) in REGLAS_DOCUMENTO

// Deja pasar solo lo que ese tipo de documento admite. Se usa mientras la persona escribe.
export function limpiarNumeroDocumento(valor, idTipo) {
  const texto = String(valor ?? '')
  if (documentoEsNumerico(idTipo)) return texto.replace(/\D/g, '').slice(0, MAX_DIGITOS_CEDULA)
  return texto.replace(/[^A-Za-z0-9]/g, '').toUpperCase().slice(0, LIMITES.documento)
}

export function validarNumeroDocumento(valor, idTipo) {
  const numero = String(valor ?? '').trim()
  if (!numero) return 'Escriba su número de documento'
  const regla = REGLAS_DOCUMENTO[Number(idTipo)] ?? REGLA_DOCUMENTO_GENERAL
  return regla.patron.test(numero) ? '' : regla.mensaje
}

// Nombres y apellidos: letras (con tildes y ñ), espacios, apóstrofo, punto y guion.
const FORMATO_NOMBRE = /^\p{L}[\p{L} '’.-]*$/u

export function validarNombre(valor, mensajeVacio) {
  const nombre = String(valor ?? '').trim()
  if (!nombre) return mensajeVacio
  if (nombre.length > LIMITES.nombre) return `Use máximo ${LIMITES.nombre} caracteres.`
  return FORMATO_NOMBRE.test(nombre) ? '' : 'Use solo letras y espacios.'
}

export function validarDireccion(valor) {
  const direccion = String(valor ?? '').trim()
  if (!direccion) return 'Escriba su dirección'
  if (direccion.length < 5) return 'Escriba la dirección completa, por ejemplo Cra 26 #28-40.'
  return direccion.length > LIMITES.direccion ? `Use máximo ${LIMITES.direccion} caracteres.` : ''
}

export function validarObligatorio(valor, mensaje) {
  return String(valor ?? '').trim() ? '' : mensaje
}

export function validarCorreo(valor) {
  const correo = String(valor ?? '').trim()
  if (!correo) return 'Escriba su correo'
  if (correo.length > LIMITES.correo) return `Use máximo ${LIMITES.correo} caracteres.`
  return FORMATO_CORREO.test(correo) ? '' : 'Escriba un correo válido, por ejemplo nombre@correo.com'
}

// El teléfono es opcional: vacío es válido; si se escribe, deben ser 10 dígitos sin espacios.
export function validarTelefono(valor) {
  const telefono = String(valor ?? '')
  if (!telefono) return ''
  return FORMATO_TELEFONO.test(telefono) ? '' : 'Escriba 10 dígitos, sin espacios.'
}

// Contraseña nueva: mínimo 8 caracteres, con al menos una letra y un número (RF-01).
export function validarPasswordNueva(valor) {
  const password = String(valor ?? '')
  if (!password) return 'Cree una contraseña'
  if (new TextEncoder().encode(password).length > LIMITES.password) return `Use máximo ${LIMITES.password} caracteres.`
  const cumple = password.length >= 8 && /[A-Za-z]/.test(password) && /\d/.test(password)
  return cumple ? '' : 'Use mínimo 8 caracteres, con letras y números.'
}

export function validarConfirmacion(password, confirmacion) {
  if (!confirmacion) return 'Repita su contraseña'
  return password === confirmacion ? '' : 'Las contraseñas no coinciden.'
}

// Recibe un objeto { campo: mensaje } y deja solo los campos que sí tienen error.
export function soloErrores(resultados) {
  return Object.fromEntries(Object.entries(resultados).filter(([, mensaje]) => mensaje))
}
