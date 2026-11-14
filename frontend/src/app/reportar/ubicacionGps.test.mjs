import assert from 'node:assert/strict'
import { test } from 'node:test'
import { TEXTOS_UBICACION, describirErrorGps, etiquetaContinuarUbicacion, obtenerPosicionGps } from './ubicacionGps.js'

// Dobles explícitos de la API: estos casos no son permisos ni GPS de un dispositivo físico.
test('resuelve coordenadas y solicita una lectura nueva con espera máxima de 15 segundos', async () => {
  let opciones
  const geo = { getCurrentPosition(exito, _fallo, configuracion) {
    opciones = configuracion
    exito({ coords: { latitude: 4.0847, longitude: -76.1954 } })
  } }
  assert.deepEqual(await obtenerPosicionGps(geo), { latitud: 4.0847, longitud: -76.1954 })
  assert.deepEqual(opciones, { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 })
})

for (const [code, codigo, texto] of [
  [1, 'PERMISO_DENEGADO', 'No tenemos permiso'],
  [2, 'NO_DISPONIBLE', 'Compruebe que la ubicación esté activada'],
  [3, 'TIEMPO_AGOTADO', 'tardó demasiado'],
]) {
  test(`error Geolocation ${code}: informa ${codigo} y permite la alternativa manual`, async () => {
    const geo = { getCurrentPosition(_exito, fallo) { fallo({ code, message: 'detalle técnico privado' }) } }
    await assert.rejects(obtenerPosicionGps(geo), e => {
      assert.equal(e.codigo, codigo)
      assert.ok(e.texto.includes(texto))
      assert.match(e.texto, /mapa/)
      assert.doesNotMatch(e.texto, /privado/)
      return true
    })
  })
}

test('sin API de geolocalización devuelve un error recuperable', async () => {
  await assert.rejects(obtenerPosicionGps(undefined), e => e.codigo === 'NO_DISPONIBLE')
})

test('una excepción de seguridad se informa como permiso denegado', async () => {
  const geo = { getCurrentPosition() { throw Object.assign(new Error('interno'), { name: 'SecurityError' }) } }
  await assert.rejects(obtenerPosicionGps(geo), e => e.codigo === 'PERMISO_DENEGADO')
})

test('una excepción inesperada conserva el mensaje seguro de recuperación', async () => {
  await assert.rejects(obtenerPosicionGps({ getCurrentPosition() { throw new Error('interno') } }),
    e => e.codigo === 'NO_DISPONIBLE' && !e.texto.includes('interno'))
  assert.equal(describirErrorGps({ code: 99 }).codigo, 'NO_DISPONIBLE')
})

test('un reintento tras denegación vuelve a pedir coordenadas y puede resolver', async () => {
  let intentos = 0
  const geo = { getCurrentPosition(exito, fallo) {
    if (++intentos === 1) fallo({ code: 1 })
    else exito({ coords: { latitude: 4.0847, longitude: -76.1954 } })
  } }
  await assert.rejects(obtenerPosicionGps(geo), e => e.codigo === 'PERMISO_DENEGADO')
  assert.equal((await obtenerPosicionGps(geo)).latitud, 4.0847)
  assert.equal(intentos, 2)
})

test('no confunde un GPS correcto fuera de Tuluá con permiso denegado', async () => {
  const geo = { getCurrentPosition(exito) { exito({ coords: { latitude: 0, longitude: 0 } }) } }
  assert.deepEqual(await obtenerPosicionGps(geo), { latitud: 0, longitud: 0 })
})

test('rechaza coordenadas inválidas sin fabricar ni guardar un punto', async () => {
  for (const [latitude, longitude] of [[NaN, 0], [91, 0], [0, 181]]) {
    const geo = { getCurrentPosition(exito) { exito({ coords: { latitude, longitude } }) } }
    await assert.rejects(obtenerPosicionGps(geo), e => e.codigo === 'NO_DISPONIBLE')
  }
})

test('con ubicación opcional el botón dice «sin ubicación» solo si la persona no puso nada', () => {
  const vacio = { pideUbicacion: false, hayPunto: false, hayBarrio: false, hayDireccion: false }
  assert.equal(etiquetaContinuarUbicacion(vacio), 'Continuar sin ubicación')
  for (const dato of ['hayPunto', 'hayBarrio', 'hayDireccion']) assert.equal(etiquetaContinuarUbicacion({ ...vacio, [dato]: true }), 'Continuar')
  assert.equal(etiquetaContinuarUbicacion({ ...vacio, pideUbicacion: true }), 'Continuar')
})

test('los textos del mapa son breves y sin palabras técnicas (RN-24)', () => {
  assert.ok(TEXTOS_UBICACION.instruccionMapa.length < 50)
  assert.ok(TEXTOS_UBICACION.etiquetaMapa.length < 50)
  for (const texto of Object.values(TEXTOS_UBICACION)) assert.doesNotMatch(texto, /servidor|token|\bapi\b|cors|http|error \d/i)
})

test('el aviso de ubicación opcional dice claro que no es obligatorio y que se puede continuar', () => {
  assert.match(TEXTOS_UBICACION.ubicacionOpcional, /No es obligatorio/)
  assert.match(TEXTOS_UBICACION.ubicacionOpcional, /continuar sin hacerlo/)
})

