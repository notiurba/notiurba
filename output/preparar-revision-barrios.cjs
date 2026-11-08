const fs = require('node:fs');
const crypto = require('node:crypto');
const evidence = 'docs/validacion/evidencias-catalogo-barrios-20261107';
const pending = 'backend/migraciones-pendientes/barrios-pmgr-2020';
const hash = data => crypto.createHash('sha256').update(data).digest('hex');
fs.mkdirSync(pending, { recursive: true });
const prior = fs.readFileSync(evidence + '/V17__catalogo_barrios_pmgr_2020.propuesta.sql', 'utf8');
const csv = fs.readFileSync(evidence + '/barrios-pmgr-2020.csv');
const source = JSON.parse(fs.readFileSync(evidence + '/fuente.json', 'utf8'));
const rows = JSON.parse(fs.readFileSync(evidence + '/transcripcion.json', 'utf8'));
const sql = '-- V17 candidata: catalogo historico PMGR municipal diciembre de 2020.\n'
  + '-- PENDIENTE de aprobacion de Andres y confirmacion de datos por DAPM/responsable autorizado.\n'
  + '-- Mantener fuera de las ubicaciones Flyway activas hasta autorizar la carga.\n'
  + '-- Fuente: ' + source.url + '\n'
  + '-- Tabla 4, paginas 16-21. No acredita vigencia administrativa en 2026.\n'
  + '-- PDF SHA256: ' + source.sha256 + '\n'
  + '-- CSV SHA256: ' + hash(csv) + '\n'
  + '-- Mismo INSERT validado anteriormente: no renombra, borra ni habilita barrios internos.\n'
  + prior.slice(prior.indexOf('INSERT INTO barrio'));
const filename = 'V17__catalogo_barrios_pmgr_2020.sql';
if (fs.existsSync(pending + '/' + filename)) throw new Error('Ya existe la candidata: no se sobrescribe');
fs.writeFileSync(pending + '/' + filename, sql);
const flags = [
  { id: 'B01', nombres: ['El Principio'], comunas: [5], pendiente: 'Confirmar denominacion; no reemplazar automaticamente por El Principe.' },
  { id: 'B02', nombres: ['Lusitania', 'Urbanización Lusitania'], comunas: [5], pendiente: 'Confirmar si son barrios distintos o denominaciones de una misma entidad.' },
  { id: 'B03', nombres: ['Diablos Rojos'], comunas: [7,8], pendiente: 'Confirmar que ambas asociaciones son correctas; un nombre compartido no permite deduplicar por nombre solo.' },
  { id: 'B04', nombres: ['San Luis', 'Urbanización San Luis'], comunas: [8], pendiente: 'Revisar identidad administrativa de ambas variantes; no son duplicados demostrados.' },
  { id: 'B05', nombres: ['Santa Inés', 'Santa Inés - Comfamiliar'], comunas: [8], pendiente: 'Revisar identidad administrativa de ambas variantes; no son duplicados demostrados.' }
];
const data = {
  estado: 'PENDIENTE_DE_APROBACION', responsableAprobacion: 'Andrés', aprobadoPor: null, aprobadoEn: null,
  migracion: pending + '/' + filename, sha256Migracion: hash(Buffer.from(sql)),
  csv: evidence + '/barrios-pmgr-2020.csv', sha256Csv: hash(csv), sha256Pdf: source.sha256,
  filas: rows.length, ejecutadaEnProduccion: false, ubicadaEnFlywayActivo: false,
  incidencias: flags.map(f => ({ ...f, estado: 'PENDIENTE', decision: null, evidenciaDecision: null,
    filas: rows.filter(r => f.nombres.includes(r.nombre) && f.comunas.includes(r.comuna)) })),
  confirmados: ['Transcripcion de 135 filas y correspondencias barrio-comuna de la fuente de 2020', 'Cero duplicados exactos del par comuna/nombre en el CSV', 'Ordinales de la fuente no son IDs de base de datos', 'Cgto. Aguaclara excluido por ser corregimiento', 'Idempotencia, integridad y UI demostradas en QA anterior'],
  pendientesGenerales: ['Vigencia de las 135 asociaciones en 2026 y cobertura del catalogo', 'Resolucion documentada de B01-B05', 'Confirmacion de siguiente version Flyway libre en el destino autorizado', 'Aprobacion del CSV, SQL y hashes finales por Andres', 'Autorizacion explicita del equipo para ejecutar la carga en el destino indicado'],
};
fs.writeFileSync(evidence + '/revision-andres.json', JSON.stringify(data, null, 2) + '\n');
console.log(JSON.stringify({ archivo: data.migracion, sha256: data.sha256Migracion, filas: data.filas, incidencias: data.incidencias.length }));
