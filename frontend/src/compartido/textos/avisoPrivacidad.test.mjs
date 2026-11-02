import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import {
  AVISO_PRIVACIDAD,
  TEXTO_CASILLA_ENVIO,
  TEXTO_CASILLA_REGISTRO,
  VERSION_AVISO,
  avisoComoTextoPlano,
} from './avisoPrivacidad.js'

const MIGRACION = new URL(
  '../../../../backend/src/main/resources/db/migration/V12__aviso_de_privacidad_v3.sql',
  import.meta.url,
)

test('el aviso del frontend y el que guarda la migración V12 dicen exactamente lo mismo', () => {
  const sql = readFileSync(MIGRACION, 'utf8')
  const guardado = sql.match(/\$aviso\$([\s\S]*?)\$aviso\$/)
  assert.ok(guardado, 'la migración no trae el texto entre $aviso$ ... $aviso$')
  assert.equal(guardado[1], avisoComoTextoPlano())
  assert.match(sql, new RegExp(`'${VERSION_AVISO}',\\s*\\$aviso\\$`), 'la versión de la migración no es la del frontend')
})

test('el aviso trae las secciones que exige la Ley 1581 y no es el marcador', () => {
  const texto = avisoComoTextoPlano()
  assert.ok(!/PENDIENTE/.test(texto))
  assert.deepEqual(AVISO_PRIVACIDAD.cabecera, ['Ley 1581 de 2012', 'Versión 3'])
  const titulos = AVISO_PRIVACIDAD.secciones.map((s) => s.titulo)
  for (const esperado of [
    'Responsable del tratamiento',
    'Datos que recolectamos',
    'Para qué los usamos',
    'Con quién los compartimos',
    'Su correo y la respuesta de la entidad',
    'Fotos de sus reportes',
    'Servicios de terceros',
    'Conservación',
    'Solo mayores de edad',
    'Sus derechos (Ley 1581, art. 8)',
    'Cómo ejercerlos',
    'Cambios a este aviso',
  ]) {
    assert.ok(titulos.includes(esperado), `falta la sección «${esperado}»`)
  }
  assert.equal(AVISO_PRIVACIDAD.secciones.find((s) => s.titulo.startsWith('Sus derechos')).items.length, 5)
})

test('el aviso no promete copia al ciudadano y dice que su correo va como dirección de respuesta', () => {
  const texto = avisoComoTextoPlano()
  assert.match(texto, /NotiUrba no le envía copia del reporte/)
  assert.match(texto, /dirección de respuesta/)
})

test('los textos de las casillas son los acordados el 01/11/2026', () => {
  assert.equal(
    TEXTO_CASILLA_REGISTRO,
    'Soy mayor de 18 años y acepto el tratamiento de mis datos personales (Ley 1581). Ver aviso de privacidad',
  )
  assert.equal(
    TEXTO_CASILLA_ENVIO,
    'Autorizo a NotiUrba a enviar este reporte a la entidad competente, a mi nombre y con mis datos, ubicación y fotos (con enlace público). La entidad podrá responderme a mi correo. Ver aviso de privacidad',
  )
})
