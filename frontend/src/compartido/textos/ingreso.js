// Textos del ingreso (correo y Google). Voz de la app: «usted», frases cortas, sin palabras técnicas.
// Cada situación dice qué pasó y qué puede hacer la persona.
export const TEXTOS_INGRESO = {
  datosIncorrectos:
    'El correo o la contraseña no coinciden. Revise los datos e inténtelo de nuevo. Si se registró con Google, use «Continuar con Google».',
  ultimoIntento: 'Le queda 1 intento antes de que bloqueemos el ingreso con este correo.',
  bloqueoSinDatos: 'Por su seguridad, bloqueamos el ingreso con este correo por unos minutos porque hubo varios intentos fallidos.',
  sinConfirmar: 'Falta confirmar su correo. Abra el enlace que le enviamos al registrarse.',
  sinInternet: 'No hay conexión a internet. Revise su conexión e inténtelo de nuevo.',
  servicioCaido:
    'NotiUrba no está respondiendo en este momento. No es un problema con sus datos. Inténtelo de nuevo en unos minutos.',
  google: 'No pudimos confirmar su cuenta de Google. Inténtelo de nuevo o ingrese con su correo y contraseña.',
  googleNoCargo: 'No pudimos cargar Google. Revise su conexión e inténtelo de nuevo.',
  generico: 'No pudimos completar el ingreso. Inténtelo de nuevo en un momento.',
}

export const TITULOS_INGRESO = {
  datosIncorrectos: 'No pudimos iniciar sesión',
  bloqueo: 'Ingreso bloqueado por un momento',
  sinConfirmar: 'Confirme su correo',
  sinInternet: 'Sin conexión',
  servicioCaido: 'NotiUrba no responde',
  google: 'No pudimos ingresar con Google',
  generico: 'No pudimos iniciar sesión',
}

export function textoMinutos(minutos) {
  return minutos === 1 ? '1 minuto' : `${minutos} minutos`
}

// Hora local de Colombia en el formato de la app: «3:42 p. m.».
export function horaColombia(fecha) {
  const partes = new Intl.DateTimeFormat('es-CO', {
    timeZone: 'America/Bogota',
    hour: 'numeric',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(fecha)
  const hora = Number(partes.find((p) => p.type === 'hour')?.value)
  const minuto = partes.find((p) => p.type === 'minute')?.value
  const sufijo = hora >= 12 ? 'p. m.' : 'a. m.'
  const hora12 = hora % 12 === 0 ? 12 : hora % 12
  return `${hora12}:${minuto} ${sufijo}`
}

export function textoBloqueo(esperarSegundos, bloqueadoHasta) {
  const hasta = new Date(bloqueadoHasta)
  if (!Number.isFinite(esperarSegundos) || esperarSegundos <= 0 || Number.isNaN(hasta.getTime())) {
    return TEXTOS_INGRESO.bloqueoSinDatos
  }
  const minutos = Math.ceil(esperarSegundos / 60)
  return `Por su seguridad, bloqueamos el ingreso con este correo durante ${textoMinutos(minutos)} porque hubo varios intentos fallidos. Podrá intentarlo de nuevo a las ${horaColombia(hasta)}`
}

// Cuenta regresiva del bloqueo: «4:32». Sube al segundo entero para no mostrar 0:00 antes de tiempo.
export function formatoCuenta(segundos) {
  const total = Math.max(0, Math.ceil(segundos))
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`
}

// Momento (en milisegundos) en que termina el bloqueo, o null si el error no lo dice bien.
export function finDelBloqueo(datos, ahora = Date.now()) {
  const hasta = new Date(datos?.bloqueadoHasta).getTime()
  if (Number.isFinite(hasta) && hasta > ahora) return hasta
  const espera = Number(datos?.esperarSegundos)
  if (Number.isFinite(espera) && espera > 0) return ahora + espera * 1000
  return null
}
