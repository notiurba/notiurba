import assert from 'node:assert/strict'
import { test } from 'node:test'
import { unaVez } from './unaVez.js'

const esperar = () => {
  let soltar
  const promesa = new Promise((resolver) => { soltar = resolver })
  return { promesa, soltar }
}

test('un doble clic mientras se espera la respuesta solo envía una vez', async () => {
  let llamadas = 0
  const respuesta = esperar()
  const enviar = unaVez(() => async () => { llamadas++; await respuesta.promesa; return 'ok' })

  const primera = enviar()
  const segunda = enviar()
  const tercera = enviar()
  respuesta.soltar()

  assert.equal(await primera, 'ok')
  assert.equal(await segunda, undefined)
  assert.equal(await tercera, undefined)
  assert.equal(llamadas, 1)
})

test('cuando llega la respuesta se puede volver a enviar (por ejemplo, para reintentar)', async () => {
  let llamadas = 0
  const enviar = unaVez(() => async () => { llamadas++ })
  await enviar()
  await enviar()
  assert.equal(llamadas, 2)
})

test('si falla, el candado se abre para que la persona pueda reintentar', async () => {
  let llamadas = 0
  const enviar = unaVez(() => async () => { llamadas++; throw new Error('sin red') })
  await assert.rejects(enviar(), /sin red/)
  await assert.rejects(enviar(), /sin red/)
  assert.equal(llamadas, 2)
})

test('el formulario ignorado no recarga la página', async () => {
  const respuesta = esperar()
  const enviar = unaVez(() => async () => { await respuesta.promesa })
  let prevenidos = 0
  const evento = { preventDefault: () => { prevenidos++ } }
  const primera = enviar(evento)
  enviar(evento)
  respuesta.soltar()
  await primera
  assert.equal(prevenidos, 2)
})

test('usa la acción más reciente (con los datos que la persona escribió al final)', async () => {
  let accion = async () => 'vieja'
  const enviar = unaVez(() => accion)
  accion = async () => 'nueva'
  assert.equal(await enviar(), 'nueva')
})
