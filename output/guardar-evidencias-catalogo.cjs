const fs = require('node:fs');
const dir = 'docs/validacion/evidencias-catalogo-barrios-20261107/';
const log = fs.readFileSync('output/barrios-browser.log', 'utf16le').replace(/^\uFEFF/, '');
const text = log.includes('### Result') ? log : fs.readFileSync('output/barrios-browser.log', 'utf8');
const result = JSON.parse(text.split('### Result')[1].trim().split(/\r?\n/)[0]);
fs.writeFileSync(dir + 'chrome-resultado.json', JSON.stringify(result, null, 2) + '\n');
for (const file of ['com.notiurba.catalogo.CatalogoControllerTest.txt', 'com.notiurba.catalogo.CatalogoCargaBarriosIT.txt']) {
  fs.copyFileSync('backend/target/surefire-reports/' + file, dir + file);
}
fs.copyFileSync('output/playwright/catalogo-barrios-qa.js', dir + 'chrome-verificacion.js');
fs.copyFileSync('output/vite-barrios-qa.mjs', dir + 'vite-qa.config.referencia.mjs');
fs.writeFileSync(dir + 'produccion-http.json', JSON.stringify({
  fecha: '2026-11-07', entorno: 'https://notiurba-api.onrender.com',
  respuestas: [
    { ruta: '/api/catalogos/barrios', estado: 200, cuerpo: [] },
    { ruta: '/api/catalogos/tipos-documento', estado: 200, cuerpo: [
      { idTipoDocumento: 1, nombre: 'Cédula de ciudadanía', codigo: 'CC' },
      { idTipoDocumento: 2, nombre: 'Cédula de extranjería', codigo: 'CE' },
      { idTipoDocumento: 3, nombre: 'Permiso por protección temporal', codigo: 'PPT' },
      { idTipoDocumento: 4, nombre: 'Pasaporte', codigo: 'PAS' }
    ] },
    { ruta: '/api/reportes/opciones/barrios', estado: 401, cuerpo: { codigo: 'NO_AUTENTICADO', mensaje: 'Tu sesión no es válida o venció. Inicia sesión de nuevo.', campo: null } }
  ],
  metodo: 'curl.exe directo, sin token; no se crearon usuarios ni reportes'
}, null, 2) + '\n');
console.log(JSON.stringify({ opciones: result.opciones, mocksDesactivados: result.mocksDesactivados, conservado: result.conservado, escriturasHttp: result.escriturasHttp.length }));
