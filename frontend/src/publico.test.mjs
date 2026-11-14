import assert from 'node:assert/strict'
import { readdirSync } from 'node:fs'
import test from 'node:test'

// Netlify sirve un archivo de public/ antes que la ruta de la app: un .html con el nombre de una pantalla
// (por ejemplo aviso-privacidad.html) tapa esa pantalla en producción, aunque en desarrollo se vea bien.
test('public/ no tiene páginas .html que tapen las rutas de la app', () => {
  const paginas = readdirSync(new URL('../public/', import.meta.url)).filter((f) => f.endsWith('.html'))
  assert.deepEqual(paginas, [])
})
