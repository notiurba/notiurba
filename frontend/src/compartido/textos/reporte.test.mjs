import assert from 'node:assert/strict'
import { test } from 'node:test'
import { TEXTOS_DESCRIPCION, TEXTOS_FOTO, TEXTOS_FOTO_NECESARIA, fechaCorta, textoDuplicado, textoLimiteDiario } from './reporte.js'

const PALABRAS_TECNICAS = /servidor|token|\bapi\b|cors|http|error \d|duplicad|\b[1-5]\d\d\b/i
const CODIGO_EN_MAYUSCULAS = /\b[A-Z]{3,}_[A-Z_]+\b/

const otraPersona = { esPropio: false, cantidad: 1, estadoExistente: 'Enviado', creadoEn: '2026-11-12',
  descripcion: 'Hueco grande frente a la panadería…' }

test('la fecha se escribe en palabras sin correr el día', () => {
  assert.equal(fechaCorta('2026-11-12'), '12 de noviembre')
  assert.equal(fechaCorta('2026-01-01'), '1 de enero')
  assert.equal(fechaCorta('basura'), '')
  assert.equal(fechaCorta(undefined), '')
})

test('parecido de otra persona ya enviado: fecha, resumen y que ya salió, sin código', () => {
  const t = textoDuplicado(otraPersona)
  assert.equal(t.titulo, 'Ya hay un reporte parecido')
  assert.match(t.texto, /^El 12 de noviembre una persona reportó en esta misma categoría: «Hueco grande frente a la panadería…»\./)
  assert.match(t.texto, /Ya lo enviamos a la entidad/)
  assert.match(t.texto, /puede enviarlo de todas maneras\.$/)
  assert.equal(t.codigo, null)
})

test('un código que llegara de otra persona nunca se muestra', () => {
  assert.equal(textoDuplicado({ ...otraPersona, codigoExistente: 'NU-000999' }).codigo, null)
  assert.doesNotMatch(textoDuplicado({ ...otraPersona, codigoExistente: 'NU-000999' }).texto, /NU-/)
})

test('parecido propio ya enviado: otro título, su código y sin citar a otra persona', () => {
  const t = textoDuplicado({ esPropio: true, cantidad: 1, estadoExistente: 'En trámite', creadoEn: '2026-11-12',
    descripcion: 'Mi hueco', codigoExistente: 'NU-000123' })
  assert.equal(t.titulo, 'Ya envió un reporte parecido')
  assert.match(t.texto, /usted reportó algo parecido en este mismo lugar \(NU-000123\)/)
  assert.doesNotMatch(t.texto, /una persona/)
  assert.equal(t.codigo, 'NU-000123')
})

test('parecido aún sin enviar dice que todavía no sale a la entidad', () => {
  const t = textoDuplicado({ ...otraPersona, estadoExistente: 'Recibido' })
  assert.equal(t.titulo, 'Ya hay un reporte parecido')
  assert.match(t.texto, /Todavía no se envía a la entidad/)
  assert.doesNotMatch(t.texto, /Ya lo enviamos/)
})

test('con varios parecidos cuenta cuántos y cita el más reciente', () => {
  const t = textoDuplicado({ ...otraPersona, cantidad: 3 })
  assert.match(t.texto, /^Ya hay 3 reportes parecidos\. El más reciente, del 12 de noviembre: «Hueco grande/)
})

test('con datos incompletos el aviso sigue siendo claro', () => {
  const t = textoDuplicado({})
  assert.match(t.texto, /^Hace poco una persona reportó en esta misma categoría\./)
  assert.match(t.texto, /puede enviarlo de todas maneras/)
})

test('el límite diario usa el máximo y dice que lo escrito sigue ahí', () => {
  assert.match(textoLimiteDiario({ maximo: 2 }).texto, /^Hoy ya envió 2 reportes, que es el máximo por día\./)
  assert.match(textoLimiteDiario({ maximo: 1 }).texto, /^Hoy ya envió 1 reporte,/)
  assert.match(textoLimiteDiario(null).texto, /el máximo de reportes/)
  assert.match(textoLimiteDiario({ maximo: 2 }).texto, /lo que escribió sigue aquí/)
})

test('ningún texto del reporte usa palabras técnicas ni códigos (RN-24)', () => {
  const textos = [
    ...Object.values(TEXTOS_DESCRIPCION), ...Object.values(TEXTOS_FOTO), ...Object.values(TEXTOS_FOTO_NECESARIA),
    ...Object.values(textoLimiteDiario({ maximo: 2 })), textoDuplicado(otraPersona).texto, textoDuplicado(otraPersona).titulo,
  ]
  for (const texto of textos) {
    assert.doesNotMatch(texto.replace(/\b(10|500)\b/g, ''), PALABRAS_TECNICAS, texto)
    assert.doesNotMatch(texto, CODIGO_EN_MAYUSCULAS, texto)
  }
})

test('la ayuda de la descripción trae su ejemplo y avisa que un resumen puede verse', () => {
  assert.match(TEXTOS_DESCRIPCION.guia, /Ejemplo:/)
  assert.match(TEXTOS_DESCRIPCION.reglas, /resumen de lo que escriba, sin su nombre/)
})
