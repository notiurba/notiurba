// Reglas de validación de los formularios. Cada función recibe lo que escribió la persona y
// devuelve el mensaje de error, o '' (texto vacío) si todo está bien. Así cualquier pantalla
// las reutiliza y se pueden probar sin abrir el navegador.

const FORMATO_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const SOLO_DIGITOS = /^\d{10}$/

export function validarObligatorio(valor, mensaje) {
  return String(valor ?? '').trim() ? '' : mensaje
}

export function validarCorreo(valor) {
  const correo = String(valor ?? '').trim()
  if (!correo) return 'Escriba su correo'
  return FORMATO_CORREO.test(correo) ? '' : 'Escriba un correo válido, por ejemplo nombre@correo.com'
}

// El teléfono es opcional: vacío es válido; si se escribe, deben ser 10 dígitos sin espacios.
export function validarTelefono(valor) {
  const telefono = String(valor ?? '')
  if (!telefono) return ''
  return SOLO_DIGITOS.test(telefono) ? '' : 'Escriba 10 dígitos, sin espacios.'
}

// Contraseña nueva: mínimo 8 caracteres, con al menos una letra y un número (RF-01).
export function validarPasswordNueva(valor) {
  const password = String(valor ?? '')
  if (!password) return 'Cree una contraseña'
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
