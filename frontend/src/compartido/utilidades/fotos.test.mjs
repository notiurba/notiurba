import assert from 'node:assert/strict'
import test from 'node:test'
import { motivoFormatoInvalido, reducirFoto } from './fotos.js'

test('motivoFormatoInvalido acepta JPG y PNG y rechaza lo demás', () => {
  assert.equal(motivoFormatoInvalido({ type: 'image/jpeg' }), '')
  assert.equal(motivoFormatoInvalido({ type: 'image/png' }), '')
  assert.ok(motivoFormatoInvalido({ type: 'application/pdf' }))
  assert.ok(motivoFormatoInvalido({ type: 'image/gif' }))
})

test('reducirFoto rechaza un PDF con mensaje en español', async () => {
  await assert.rejects(reducirFoto({ type: 'application/pdf', size: 100, name: 'a.pdf' }), {
    message: 'Solo se aceptan imágenes JPG o PNG.',
  })
})

test('reducirFoto devuelve igual la foto que ya pesa 1 MB o menos', async () => {
  const foto = { type: 'image/jpeg', size: 900_000, name: 'a.jpg' }
  assert.equal(await reducirFoto(foto), foto)
})
