import assert from 'node:assert/strict'
import { readdirSync } from 'node:fs'
import test from 'node:test'

// Netlify sirve un archivo o carpeta de public/ antes que la ruta de la app: un aviso-privacidad.html (o una carpeta
// aviso-privacidad/) tapa esa pantalla en producción, aunque en desarrollo se vea bien. Solo se permite lo de esta lista;
// si hace falta algo más, se agrega aquí a propósito.
const PERMITIDOS = ['_redirects', 'favicon.svg']

test('public/ solo tiene los archivos permitidos y nada que tape las rutas de la app', () => {
  const encontrados = readdirSync(new URL('../public/', import.meta.url), { recursive: true }).map(String).sort()
  assert.deepEqual(encontrados, PERMITIDOS)
})
