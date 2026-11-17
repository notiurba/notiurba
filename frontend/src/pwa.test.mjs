import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import { mkdtemp, readFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { runInNewContext } from 'node:vm'
import { build } from 'vite'

let salida
let manifiesto
let recursos
let rutas
let tomaControl = false
let activaNuevaVersion = false

before(async () => {
  salida = await mkdtemp(join(tmpdir(), 'notiurba-pwa-test-'))
  await build({ logLevel: 'silent', build: { outDir: salida } })
  manifiesto = JSON.parse(await readFile(join(salida, 'manifest.webmanifest'), 'utf8'))
  rutas = []
  const workbox = {
    precacheAndRoute: (entradas) => { recursos = entradas },
    cleanupOutdatedCaches: () => {},
    clientsClaim: () => { tomaControl = true },
    createHandlerBoundToURL: (url) => url,
    NavigationRoute: class {
      constructor(destino, opciones) { this.destino = destino; this.opciones = opciones }
    },
    registerRoute: (ruta) => rutas.push(ruta),
  }
  runInNewContext(await readFile(join(salida, 'sw.js'), 'utf8'), {
    define: (_dependencias, cargar) => cargar(workbox),
    self: { skipWaiting: () => { activaNuevaVersion = true } },
  })
})

after(async () => { if (salida) await rm(salida, { recursive: true, force: true }) })

test('el build entrega un manifiesto instalable con iconos PNG de las dimensiones declaradas', async () => {
  assert.equal(manifiesto.name, 'NotiUrba')
  assert.equal(manifiesto.short_name, 'NotiUrba')
  assert.equal(manifiesto.start_url, '/')
  assert.equal(manifiesto.scope, '/')
  assert.equal(manifiesto.display, 'standalone')
  assert.equal(manifiesto.theme_color, '#082f6b')
  assert.equal(manifiesto.background_color, '#f5f7f4')
  for (const dimension of [192, 512]) {
    const icono = manifiesto.icons.find(icono => icono.sizes === `${dimension}x${dimension}`)
    assert.ok(icono)
    assert.equal(icono.type, 'image/png')
    const png = await readFile(join(salida, icono.src))
    assert.equal(png.subarray(1, 4).toString(), 'PNG')
    assert.equal(png.readUInt32BE(16), dimension)
    assert.equal(png.readUInt32BE(20), dimension)
  }
  assert.match(await readFile(join(salida, 'index.html'), 'utf8'), /rel="manifest"/)
})

test('el worker solo precarga recursos propios y no registra caché de API ni teselas', () => {
  assert.ok(recursos.length > 0)
  for (const { url } of recursos) {
    assert.doesNotMatch(url, /https?:|openstreetmap|(?:^|\/)api(?:\/|$)/i)
    assert.match(url, /\.(?:js|css|html|svg|png|webmanifest)$/)
  }
  assert.equal(rutas.length, 1, 'solo debe haber fallback de navegación, sin rutas de caché en ejecución')
  assert.equal(rutas[0].destino, '/index.html')
  const denegada = ruta => rutas[0].opciones.denylist.some(patron => patron.test(ruta))
  for (const ruta of ['/api/reportes', '/api/auth/login', '/api/admin/reportes?estado=RECIBIDO']) {
    assert.equal(denegada(ruta), true)
  }
  for (const ruta of ['/', '/login', '/app/mis-reportes', '/reportar/categoria']) {
    assert.equal(denegada(ruta), false)
  }
})

test('el worker permite activar una actualización y tomar el control de la app', () => {
  assert.equal(activaNuevaVersion, true)
  assert.equal(tomaControl, true)
})
