const fs = require('node:fs');
const crypto = require('node:crypto');
const dir = 'docs/validacion/evidencias-catalogo-barrios-20261107';
fs.mkdirSync(dir, { recursive: true });
const pages = JSON.parse(fs.readFileSync('output/pmgr-tabla.json'));
const counts = [12, 8, 21, 5, 16, 16, 21, 18, 15, 3];
const entries = [];
for (const page of pages) {
  for (const row of page.rows) {
    const ordinal = row.c.find(c => c.x >= 190 && c.x <= 210 && /^\d+$/.test(c.s.trim()));
    const local = row.c.find(c => c.x >= 450 && /^\d+$/.test(c.s.trim()));
    if (!ordinal || !local) continue;
    let nombre = row.c.filter(c => c.x >= 300 && c.x < 434).map(c => c.s).join('').trim();
    if (page.page === 19 && ordinal.s === '70') {
      nombre = page.rows.filter(r => Math.abs(r.y - row.y) <= 8).flatMap(r => r.c.filter(c => c.x >= 300 && c.x < 434).map(c => c.s)).join(' ').replace(/\s+/g, ' ').trim();
    }
    if (!nombre) throw new Error('Fila sin nombre: ' + JSON.stringify(row));
    entries.push({ nombre, pagina: page.page, ordenFuente: Number(ordinal.s), numeroEnComuna: Number(local.s) });
  }
}
if (entries.length !== 135) throw new Error('Se esperaban 135 filas, hay ' + entries.length);
let offset = 0;
for (let c = 1; c <= 10; c++) {
  for (let n = 1; n <= counts[c - 1]; n++) {
    const row = entries[offset++];
    if (row.numeroEnComuna !== n) throw new Error('Numeración local inesperada: ' + JSON.stringify(row));
    row.comuna = c;
  }
}
if (new Set(entries.map(e => e.comuna + ':' + e.nombre)).size !== 135) throw new Error('Duplicados');
const csv = 'comuna,barrio\n' + entries.map(e => e.comuna + ',' + e.nombre).join('\n') + '\n';
fs.writeFileSync(dir + '/barrios-pmgr-2020.csv', csv);
fs.writeFileSync(dir + '/transcripcion.json', JSON.stringify(entries, null, 2) + '\n');
// El PDF fuente canonico ya esta versionado; no duplicarlo en output.
const source = {
  url: 'https://tulua.gov.co/loader.php?lServicio=Tools2&lTipo=descargas&lFuncion=exposeDocument&idFile=12112',
  titulo: 'Actualización Plan Municipal de Gestión del Riesgo V3 diciembre 2020',
  emisor: 'Alcaldía de Tuluá, Secretaría de Gobierno; tabla atribuida a DAPM Tuluá',
  tabla: '4. División Político Administrativa de Tuluá', paginas: [16,17,18,19,20,21],
  sha256: crypto.createHash('sha256').update(fs.readFileSync('docs/validacion/evidencias-catalogo-barrios-20261107/fuente-pmgr-2020.pdf')).digest('hex'),
  filas: entries.length, conteosPorComuna: counts,
  alcance: 'Fuente oficial histórica de 2020; no acredita vigencia administrativa en 2026. Preparación para revisión, no aplicada a producción.',
  incidencias: ['La fuente repite el ordinal global 41 y omite 46: se usa la columna Comuna y la numeración local, no el ordinal como id_barrio.', 'Cgto. Aguaclara no se incluye: la fuente lo identifica como corregimiento.', 'Se conservan los nombres tal como aparecen, incluido El Principio y Urbanización Lusitania/Lusitania (comuna 5). Validar esos nombres y la vigencia con DAPM antes de producción.']
};
fs.writeFileSync(dir + '/fuente.json', JSON.stringify(source, null, 2) + '\n');
const sql = '-- PROPUESTA V17: catalogo de barrios, PMGR municipal diciembre 2020.\n'
  + '-- Revisar fuente.json y vigencia antes de copiar a db/migration y desplegar.\n'
  + '-- No incluye geometrias, coordenadas, barrios internos ni datos ficticios.\n'
  + '-- Fuente: ' + source.url + '\n'
  + '-- PDF SHA256: ' + source.sha256 + '\n'
  + 'INSERT INTO barrio (id_comuna, nombre) VALUES\n'
  + entries.map(e => "  (" + e.comuna + ", '" + e.nombre.replaceAll("'", "''") + "')").join(',\n')
  + '\nON CONFLICT (id_comuna, nombre) DO NOTHING;\n';
fs.writeFileSync(dir + '/V17__catalogo_barrios_pmgr_2020.propuesta.sql', sql);
console.log(JSON.stringify({ filas: entries.length, conteos: counts, fuente: source.sha256, destino: dir }));
