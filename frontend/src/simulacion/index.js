// SIMULACIÓN DEL BACKEND (solo para desarrollo).
// Responde con datos de ./datos y ./almacen cuando el backend todavía no tiene el endpoint, o cuando se
// quiere probar sin servidor. Se activa con VITE_USAR_MOCKS=true en .env.local y solo la usa api/cliente.js.
//
// Para ELIMINARLA: borre esta carpeta entera (src/simulacion). Nada más se rompe: cliente.js la
// busca con import.meta.glob y, si no existe, simplemente habla con la API real.
import { ErrorApi } from '../compartido/api/errorApi'
import * as almacen from './almacen'
import barrios from './datos/catalogos/barrios.json'
import categorias from './datos/catalogos/categorias.json'
import tiposDocumento from './datos/catalogos/tipos-documento.json'
import loginAdmin from './datos/auth/login.ok.admin.json'
import loginCiudadano from './datos/auth/login.ok.ciudadano.json'
import loginSinVerificar from './datos/auth/login.error.correo-no-verificado.json'
import loginCorreoNoExiste from './datos/auth/login.error.correo-no-existe.json'
import loginPasswordIncorrecta from './datos/auth/login.error.password-incorrecta.json'
import recuperarOk from './datos/auth/recuperar.ok.json'
import registroOk from './datos/auth/registro.ok.json'
import correoRepetido from './datos/auth/registro.error.correo-repetido.json'
import documentoRepetido from './datos/auth/registro.error.documento-repetido.json'
import enlaceVencido from './datos/auth/restablecer.error.enlace-vencido.json'
import verificarVencido from './datos/auth/verificar.error.enlace-vencido.json'
import reenviarOk from './datos/auth/reenviar-verificacion.ok.json'

// Arma un ErrorApi con el cuerpo que daría el servidor.
function crearErrorApi(estado, cuerpo = {}, ruta = '') {
  return new ErrorApi({
    estado,
    codigo: cuerpo.codigo ?? (estado === 400 ? 'VALIDACION' : `HTTP_${estado}`),
    mensaje: cuerpo.mensaje ?? `El servidor respondió algo inesperado (${estado} ${ruta})`.trim(),
    campo: cuerpo.campo ?? null,
  })
}

const ESPERA_MS = 500 // pausa para que se vean los estados de carga

// Cuentas de prueba para entrar sin backend (todas con la contraseña PASSWORD_PRUEBA).
const PASSWORD_PRUEBA = 'Clave1234'
const CUENTAS = {
  'ciudadano@correo.com': loginCiudadano, // entra a /app
  'nuevo@correo.com': loginCiudadano, //    entra a /app con la lista de reportes vacía
  'admin@correo.com': loginAdmin, //         entra a /admin
}
// Cuenta que aún no confirmó su correo: el login responde 403 CORREO_NO_VERIFICADO (contrato 3.2).
const CORREO_SIN_VERIFICAR = 'sinverificar@correo.com'

// Datos de prueba que provocan errores, para poder ver esas pantallas:
//   correo repetido@correo.com  → registro: error de correo
//   documento 1098765432        → registro: error de documento (también en «Completar perfil»)
//   otro correo en el login     → «no encontramos una cuenta»; contraseña distinta → «no es correcta»
//   sinverificar@correo.com    → login: «Confirme su correo» (403 CORREO_NO_VERIFICADO)
//   token «vencido» en /nueva-contrasena?token=vencido o /app/verificar-correo?token=vencido → «el enlace venció»
// Cada ruta: [método, patrón, manejador(cuerpo, params, ruta)]. `:codigo` es un parámetro de la ruta.
const RUTAS = [
  ['GET', '/api/catalogos/barrios', () => barrios],
  ['GET', '/api/catalogos/tipos-documento', () => tiposDocumento],
  ['GET', '/api/categorias', () => categorias],

  ['POST', '/api/auth/login', (cuerpo, _p, ruta) => {
    if (cuerpo.correo === CORREO_SIN_VERIFICAR && cuerpo.password === PASSWORD_PRUEBA) {
      throw crearErrorApi(403, loginSinVerificar, ruta)
    }
    const cuenta = CUENTAS[cuerpo.correo]
    if (!cuenta) throw crearErrorApi(401, loginCorreoNoExiste, ruta)
    if (cuerpo.password !== PASSWORD_PRUEBA) throw crearErrorApi(401, loginPasswordIncorrecta, ruta)
    almacen.iniciarSesion(cuerpo.correo)
    return cuenta
  }],
  ['POST', '/api/auth/registro', (cuerpo, _p, ruta) => {
    if (cuerpo.correo === 'repetido@correo.com') throw crearErrorApi(409, correoRepetido, ruta)
    if (cuerpo.numeroDocumento === '1098765432') throw crearErrorApi(409, documentoRepetido, ruta)
    // Sin token: la cuenta queda por confirmar (contrato 3.1). `declaraMayorEdad` y `aceptaPrivacidad` deben venir en true.
    if (cuerpo.declaraMayorEdad !== true) {
      throw crearErrorApi(400, { codigo: 'VALIDACION', mensaje: 'Debes declarar que eres mayor de 18 años.', campo: 'declaraMayorEdad' }, ruta)
    }
    return { ...registroOk, correo: cuerpo.correo }
  }],
  ['POST', '/api/auth/verificar', (cuerpo, _p, ruta) => {
    if (cuerpo.token === 'vencido') throw crearErrorApi(400, verificarVencido, ruta)
    almacen.iniciarSesion('nuevo@correo.com')
    return loginCiudadano
  }],
  ['POST', '/api/auth/reenviar-verificacion', () => reenviarOk],
  ['POST', '/api/auth/recuperar', () => recuperarOk],
  ['POST', '/api/auth/restablecer', (cuerpo, _p, ruta) => {
    if (cuerpo.token === 'vencido') throw crearErrorApi(400, enlaceVencido, ruta)
    return null // la API real responde 204 sin cuerpo
  }],
  ['POST', '/api/auth/google/completar', (cuerpo, _p, ruta) => {
    if (cuerpo.numeroDocumento === '1098765432') throw crearErrorApi(409, documentoRepetido, ruta)
    return loginCiudadano
  }],

  ['GET', '/api/usuarios/me', () => almacen.obtenerPerfil()],
  ['PUT', '/api/usuarios/me', (cuerpo) => almacen.guardarPerfil(cuerpo)],
  ['DELETE', '/api/usuarios/me/datos', () => almacen.borrarReportes() ?? null],
  ['DELETE', '/api/usuarios/me', () => almacen.borrarTodo() ?? null],

  ['GET', '/api/reportes', () => almacen.listarMisReportes()],
  ['POST', '/api/reportes', (cuerpo, _p, ruta) => {
    if ((cuerpo.descripcion ?? '').trim().length < 10) {
      throw crearErrorApi(400, { mensaje: 'La descripción debe tener al menos 10 caracteres', campo: 'descripcion' }, ruta)
    }
    if (cuerpo.autorizaEnvio !== true) {
      throw crearErrorApi(400, { mensaje: 'Debes autorizar el envío del reporte.', campo: 'autorizaEnvio' }, ruta)
    }
    const { codigoSeguimiento, estado, creadoEn } = almacen.crearReporte(cuerpo)
    // Contrato 3.4: la creación devuelve solo lo mínimo; el detalle se pide aparte (3.6).
    return { codigoSeguimiento, estado, creadoEn, vigenteHasta: null }
  }],
  // Contrato 3.5: una foto por llamada (multipart). La simulación no mira la imagen, solo cuenta el cupo.
  ['POST', '/api/reportes/:codigo/fotos', (_c, { codigo }, ruta) => {
    const reporte = almacen.buscarReporte(codigo)
    if (!reporte) throw crearErrorApi(404, { codigo: 'NO_ENCONTRADO', mensaje: 'No se encontró ese reporte' }, ruta)
    if (reporte.fotos.length >= 3) {
      throw crearErrorApi(409, { codigo: 'FOTOS_MAXIMO', mensaje: 'Un reporte admite hasta 3 fotos.' }, ruta)
    }
    return { codigoSeguimiento: codigo, foto: { orden: almacen.agregarFoto(codigo), url: null } }
  }],
  // Propuesto en el contrato (sección 4): envía el reporte a la entidad; responde 202 sin cuerpo útil.
  ['POST', '/api/reportes/:codigo/enviar', (_c, { codigo }) => {
    almacen.marcarEnviado(codigo)
    return null
  }],
  ['GET', '/api/reportes/:codigo', (_c, { codigo }, ruta) => {
    const reporte = almacen.buscarReporte(codigo)
    if (!reporte) throw crearErrorApi(404, { mensaje: 'No se encontró ese reporte' }, ruta)
    return reporte
  }],
  // La persona avisa que la entidad respondió o que ya se resolvió (pantalla «Actualizar mi reporte»).
  ['PATCH', '/api/reportes/:codigo/actualizacion', (cuerpo, { codigo }) => {
    const estado = cuerpo.tipo === 'RESUELTO' ? 'Resuelto' : 'En trámite'
    return almacen.cambiarEstado(codigo, estado, 'Actualizado por la persona que reportó', cuerpo.radicado || null)
  }],

  // Panel de administración: todos los reportes y cambio de estado con observación.
  ['GET', '/api/admin/reportes', () => almacen.listarReportes()],
  ['PATCH', '/api/admin/reportes/:codigo/estado', (cuerpo, { codigo }) =>
    almacen.cambiarEstado(codigo, cuerpo.estado, cuerpo.observacion || null)],
]

// Convierte '/api/reportes/:codigo' en una expresión que reconoce la ruta y saca el parámetro.
function coincide(patron, ruta) {
  const nombres = []
  const expresion = patron.replace(/:(\w+)/g, (_, nombre) => {
    nombres.push(nombre)
    return '([^/]+)'
  })
  const resultado = new RegExp(`^${expresion}$`).exec(ruta)
  if (!resultado) return null
  return Object.fromEntries(nombres.map((nombre, i) => [nombre, decodeURIComponent(resultado[i + 1])]))
}

// Devuelve una promesa con la respuesta simulada, o null si esa ruta no tiene simulación
// (entonces la petición va a la API real).
export function buscarMock(metodo, ruta, cuerpo) {
  for (const [metodoRuta, patron, manejador] of RUTAS) {
    const params = metodoRuta === metodo ? coincide(patron, ruta) : null
    if (!params) continue

    return new Promise((resolver, rechazar) => {
      setTimeout(() => {
        try {
          // Un FormData (fotos) no es JSON: la simulación no lo lee.
          resolver(manejador(typeof cuerpo === 'string' ? JSON.parse(cuerpo) : {}, params, ruta))
        } catch (error) {
          rechazar(error)
        }
      }, ESPERA_MS)
    })
  }
  return null
}
