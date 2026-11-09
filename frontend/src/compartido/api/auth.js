import { peticion } from './cliente'
import { ErrorApi } from './errorApi'

// POST /api/auth/login → { token, rol }. Con credenciales malas la API responde 401.
export async function iniciarSesion(correo, password) {
  const respuesta = await peticion('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ correo, password }),
    publica: true,
  })
  if (!respuesta || typeof respuesta.token !== 'string' || !respuesta.token.trim() || typeof respuesta.rol !== 'string') {
    throw new ErrorApi({ estado: 200, codigo: 'RESPUESTA_INESPERADA', mensaje: 'No pudimos completar el ingreso. Inténtelo de nuevo en un momento.' })
  }
  return respuesta
}

// POST /api/auth/registro → 201 { correo, mensaje } SIN token: hay que confirmar el correo antes de entrar.
// `datos` lleva los campos del contrato: nombres, apellidos, idTipoDocumento, numeroDocumento, idBarrio,
// direccionCorrespondencia, correo, telefono (opcional), password, aceptaPrivacidad y declaraMayorEdad.
export function registrarCuenta(datos) {
  return peticion('/api/auth/registro', {
    method: 'POST',
    body: JSON.stringify(datos),
    publica: true,
  })
}

// POST /api/auth/recuperar: manda el enlace al correo. La respuesta es la misma exista o no la cuenta.
export function solicitarRecuperacion(correo) {
  return peticion('/api/auth/recuperar', {
    method: 'POST',
    body: JSON.stringify({ correo }),
    publica: true,
  })
}

// POST /api/auth/restablecer → 204. `token` viene en el enlace del correo. Falla (400 ENLACE_INVALIDO) si el
// enlace venció o ya se usó. El campo de la contraseña en la API se llama `nuevaContrasena`.
export function restablecerPassword(token, password) {
  return peticion('/api/auth/restablecer', {
    method: 'POST',
    body: JSON.stringify({ token, nuevaContrasena: password }),
    publica: true,
  })
}

// POST /api/auth/google: el botón de Google entrega un `credential` (ID token) y la API responde
// { token, rol } si la cuenta ya existe, o { estado: 'PERFIL_INCOMPLETO', tokenRegistro, correo, nombres, apellidos }.
export async function iniciarSesionConGoogle(credential) {
  const respuesta = await peticion('/api/auth/google', { method: 'POST', body: JSON.stringify({ credential }), publica: true })
  if (respuesta?.estado === 'PERFIL_INCOMPLETO') {
    if (typeof respuesta.tokenRegistro !== 'string' || !respuesta.tokenRegistro.trim()
        || typeof respuesta.correo !== 'string' || !respuesta.correo.trim()) throw respuestaGoogleInvalida()
    return respuesta
  }
  return validarSesionGoogle(respuesta)
}

// POST /api/auth/google/completar: completa el perfil de una cuenta creada con Google → { token, rol }.
// La API real pide `tokenRegistro` (lo entrega POST /api/auth/google; dura 15 min) y `declaraMayorEdad`.
export async function completarPerfilGoogle(datos) {
  const respuesta = await peticion('/api/auth/google/completar', { method: 'POST', body: JSON.stringify(datos), publica: true })
  return validarSesionGoogle(respuesta)
}

function respuestaGoogleInvalida() {
  return new ErrorApi({ estado: 200, codigo: 'RESPUESTA_INESPERADA',
    mensaje: 'No pudimos completar el ingreso con Google. Inténtelo de nuevo.' })
}

function validarSesionGoogle(respuesta) {
  if (typeof respuesta?.token !== 'string' || !respuesta.token.trim()
      || typeof respuesta.rol !== 'string' || !respuesta.rol.trim()) throw respuestaGoogleInvalida()
  return respuesta
}

// POST /api/auth/verificar: confirma el correo con el token del enlace y devuelve la sesión → { token, rol }.
// Falla con 400 ENLACE_INVALIDO si el enlace venció (24 h) o ya se usó.
export function verificarCorreo(token) {
  return peticion('/api/auth/verificar', {
    method: 'POST',
    body: JSON.stringify({ token }),
    publica: true,
  })
}

// POST /api/auth/reenviar-verificacion: siempre 200 { mensaje }, exista o no la cuenta (máximo uno por minuto).
export function reenviarVerificacion(correo) {
  return peticion('/api/auth/reenviar-verificacion', {
    method: 'POST',
    body: JSON.stringify({ correo }),
    publica: true,
  })
}
