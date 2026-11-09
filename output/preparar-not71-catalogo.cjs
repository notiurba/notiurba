// Preparacion local: lee la candidata original; nunca conecta con produccion ni ejecuta SQL.
const fs = require('node:fs/promises');
const path = require('node:path');
const crypto = require('node:crypto');
async function main() {
  const root = path.resolve(__dirname, '..');
  const base = path.join(root, 'backend/migraciones-pendientes/barrios-pmgr-2020');
  const evidence = path.join(root, 'docs/validacion/evidencias-catalogo-barrios-20261107');
  const out = path.join(base, 'preparacion-NOT71-20261108');
  const bytes = await fs.readFile(path.join(base, 'V17__catalogo_barrios_pmgr_2020.sql'));
  const sql = bytes.toString('utf8');
  const csv = await fs.readFile(path.join(evidence, 'barrios-pmgr-2020.csv'));
  const decisions = JSON.parse(await fs.readFile(path.join(evidence, 'revision-andres.json'), 'utf8'));
  const hash = b => crypto.createHash('sha256').update(b).digest('hex');
  const rows = [...sql.matchAll(/\((\d+),\s*'((?:[^']|'')*)'\)/g)].map(m=>({id_comuna:Number(m[1]),nombre:m[2].replaceAll("''", "'")}));
  const expected = csv.toString('utf8').trim().split(/\r?\n/).slice(1).map(line=>{
    const at=line.indexOf(','); return {id_comuna:Number(line.slice(0,at)),nombre:line.slice(at+1)};
  });
  const key = r => `${r.id_comuna}|${r.nombre}`;
  const norm = r => `${r.id_comuna}|${r.nombre.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().trim().replace(/\s+/g,' ')}`;
  const same = (a,b) => JSON.stringify(a.map(key).sort()) === JSON.stringify(b.map(key).sort());
  if(rows.length!==135 || !same(rows,expected) || new Set(rows.map(key)).size!==135 || new Set(rows.map(norm)).size!==135) throw Error('Filas, CSV o duplicados no coinciden');
  if(rows.some(r=>r.id_comuna<1||r.id_comuna>10||r.nombre.length>100)) throw Error('Comuna o longitud incompatible');
  if(hash(bytes)!==decisions.sha256Migracion || hash(csv)!==decisions.sha256Csv) throw Error('Hashes diferentes del registro original');
  const response=JSON.parse(await fs.readFile(path.join(root,'docs/validacion/evidencias-NOT-71-final-20261108/catalogo-qa.json'),'utf8'));
  const api=response.barrios;
  if(response.url!=='http://127.0.0.1:18091/api/catalogos/barrios' || response.status!==200 || !same(rows,api.map(r=>({id_comuna:r.idComuna,nombre:r.nombre})))) throw Error('API QA distinta de la candidata');
  await fs.mkdir(out,{recursive:true});
  const values=rows.map(r=>`(${r.id_comuna}, '${r.nombre.replaceAll("'","''")}')`).join(',\n');
  const cte=`WITH esperado(id_comuna,nombre) AS (VALUES\n${values}\n)`;
  const pre=`-- SOLO LECTURA. No autoriza cargar datos. Ejecutar con ON_ERROR_STOP.
BEGIN TRANSACTION READ ONLY;
SELECT current_database() AS base,current_user AS usuario,inet_server_addr() AS servidor,inet_server_port() AS puerto;
SELECT installed_rank,version,description,success FROM public.flyway_schema_history ORDER BY installed_rank;
SELECT count(*) FILTER (WHERE NOT interno) AS publicos_antes,count(*) FILTER (WHERE interno) AS internos_antes FROM public.barrio;
SELECT id_barrio,id_comuna,nombre,interno FROM public.barrio ORDER BY id_barrio;
SELECT conname,contype,convalidated,pg_get_constraintdef(oid) AS definicion FROM pg_constraint WHERE conrelid='public.barrio'::regclass;
DO $guard$
BEGIN
 IF EXISTS(SELECT 1 FROM public.flyway_schema_history WHERE NOT success) THEN RAISE EXCEPTION 'Historial fallido: detener y revisar'; END IF;
 IF EXISTS(SELECT 1 FROM public.flyway_schema_history WHERE version='17') THEN RAISE EXCEPTION 'V17 ocupada: resolver numeracion antes de promover la candidata'; END IF;
 IF NOT EXISTS(SELECT 1 FROM public.flyway_schema_history WHERE version='16' AND success) THEN RAISE EXCEPTION 'Falta base V16'; END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.barrio'::regclass AND contype='u' AND pg_get_constraintdef(oid)='UNIQUE (id_comuna, nombre)') THEN RAISE EXCEPTION 'Falta unicidad barrio-comuna'; END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.barrio'::regclass AND confrelid='public.comuna'::regclass AND contype='f' AND convalidated) THEN RAISE EXCEPTION 'Falta FK validada a comuna'; END IF;
END $guard$;
${cte}
SELECT e.* FROM esperado e LEFT JOIN public.comuna c USING(id_comuna) WHERE c.id_comuna IS NULL;
-- La consulta previa (comunas faltantes) y la siguiente (internos) deben devolver 0 filas.
${cte}
SELECT b.* FROM esperado e JOIN public.barrio b USING(id_comuna,nombre) WHERE b.interno;
-- Similitudes de nombre: revision humana, nunca fusion automatica.
${cte}
SELECT e.id_comuna,e.nombre AS candidato,b.id_barrio,b.nombre AS existente,b.interno FROM esperado e JOIN public.barrio b ON b.id_comuna=e.id_comuna AND lower(regexp_replace(trim(b.nombre),'\\s+',' ','g'))=lower(regexp_replace(trim(e.nombre),'\\s+',' ','g')) WHERE b.nombre<>e.nombre;
${cte}
SELECT count(*) AS esperados,count(*) FILTER(WHERE b.id_barrio IS NOT NULL AND NOT b.interno) AS ya_publicos,count(*) FILTER(WHERE b.id_barrio IS NULL) AS inserciones_previstas FROM esperado e LEFT JOIN public.barrio b USING(id_comuna,nombre);
COMMIT;
`;
  const post=`-- SOLO LECTURA. Verificar las 135 claves naturales; no exigir IDs de QA.
BEGIN TRANSACTION READ ONLY;
${cte}
SELECT count(*) AS esperados,count(b.id_barrio) FILTER(WHERE NOT b.interno) AS publicos_coincidentes FROM esperado e LEFT JOIN public.barrio b USING(id_comuna,nombre);
-- Debe devolver 0 filas: ninguno ausente o interno.
${cte}
SELECT e.* FROM esperado e LEFT JOIN public.barrio b USING(id_comuna,nombre) WHERE b.id_barrio IS NULL OR b.interno;
SELECT id_comuna,nombre,count(*) FROM public.barrio GROUP BY id_comuna,nombre HAVING count(*)>1;
SELECT b.id_barrio,b.id_comuna FROM public.barrio b LEFT JOIN public.comuna c USING(id_comuna) WHERE c.id_comuna IS NULL;
SELECT id_barrio,id_comuna,nombre,interno FROM public.barrio WHERE interno ORDER BY id_barrio;
SELECT count(*) FILTER(WHERE NOT interno) AS publicos_despues,count(*) FILTER(WHERE interno) AS internos_despues FROM public.barrio;
${cte}
SELECT e.id_comuna,count(*) AS esperados,count(b.id_barrio) FILTER(WHERE NOT b.interno) AS encontrados FROM esperado e LEFT JOIN public.barrio b USING(id_comuna,nombre) GROUP BY e.id_comuna ORDER BY e.id_comuna;
SELECT version,description,script,checksum,success FROM public.flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
COMMIT;
-- Total publico despues = publico antes + inserciones previstas del preflight.
-- Comparar filas internas e IDs originales contra el inventario previo.
`;
  const qa=`-- SOLO psql en la QA existente; prueba de reejecucion sobre 135 filas precargadas.
\\set ON_ERROR_STOP on
BEGIN;
DO $guard$ BEGIN
 IF current_database()<>'notiurba_barrios_qa' OR current_user<>'qa_barrios' THEN RAISE EXCEPTION 'Destino QA incorrecto'; END IF;
 IF (SELECT count(*) FROM public.barrio WHERE NOT interno)<>135 THEN RAISE EXCEPTION 'Se requieren 135 filas QA precargadas'; END IF;
END $guard$;
SET LOCAL lock_timeout='3s';
SET LOCAL statement_timeout='15s';
CREATE TEMP TABLE qa_barrios_antes ON COMMIT DROP AS TABLE public.barrio;
\\ir ../V17__catalogo_barrios_pmgr_2020.sql
\\ir ../V17__catalogo_barrios_pmgr_2020.sql
DO $guard$ BEGIN
 IF EXISTS((SELECT * FROM public.barrio EXCEPT ALL SELECT * FROM qa_barrios_antes) UNION ALL (SELECT * FROM qa_barrios_antes EXCEPT ALL SELECT * FROM public.barrio)) THEN RAISE EXCEPTION 'La reejecucion cambio filas'; END IF;
END $guard$;
SELECT 'Reejecucion idempotente en QA precargada; transaccion revertida' AS resultado;
ROLLBACK;
-- Las secuencias SERIAL pueden avanzar aun con ON CONFLICT/ROLLBACK; no se resecuencian.
-- No prueba primera carga vacia: esa evidencia es CatalogoCargaBarriosIT previa.
`;
  await fs.writeFile(path.join(out,'01-preflight.sql'),pre);
  await fs.writeFile(path.join(out,'02-postflight.sql'),post);
  await fs.writeFile(path.join(out,'03-qa-idempotencia.sql'),qa);
  const summary={fecha:new Date().toISOString(),candidata:'../V17__catalogo_barrios_pmgr_2020.sql',sha256Migracion:hash(bytes),sha256Csv:hash(csv),filas:rows.length,paresUnicos:135,duplicadosNormalizados:0,coincideCsv:true,coincideApiQa:true,apiQaStatus:response.status,maxCaracteres:Math.max(...rows.map(r=>r.nombre.length)),distribucion:Object.fromEntries(Array.from({length:10},(_,i)=>[i+1,rows.filter(r=>r.id_comuna===i+1).length])),aprobadoPor:decisions.aprobadoPor,decisiones:decisions.incidencias.map(i=>({id:i.id,estado:i.estado,decision:i.decision})),sqlNuevos:'preparados, NO ejecutados; Docker deniega acceso',pruebaCargaAnterior:'CatalogoCargaBarriosIT: primera carga 135, repeticion 0; reutilizada',estado:'CANDIDATA_NO_AUTORIZADA',originalModificado:false};
  await fs.writeFile(path.join(out,'validacion-artefactos.json'),JSON.stringify(summary,null,2)+'\n');
  console.log(JSON.stringify(summary,null,2));
}
main().catch(e=>{console.error(e.message);process.exitCode=1;});
