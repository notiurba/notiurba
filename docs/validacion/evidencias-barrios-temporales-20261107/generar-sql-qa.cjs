const fs = require('node:fs');
const seed = fs.readFileSync('db/datos_ejemplo.sql','utf8').match(/INSERT INTO barrio[\s\S]*?ON CONFLICT \(id_barrio\) DO NOTHING;/)[0];
const cleanup = fs.readFileSync('backend/migraciones-pendientes/barrios-temporales/limpieza-revisable.sql','utf8')
  .replace('-- Lista vacia deliberadamente:', "INSERT INTO barrios_temporales_aprobados VALUES (3,3,'Barrio ejemplo 3');\n-- Lista vacia deliberadamente:");
const assertInside = `DO $$ DECLARE filas integer; BEGIN
  ASSERT (SELECT interno FROM barrio WHERE id_barrio=3), 'Temporal no excluido';
  ASSERT (SELECT count(*) FROM barrio WHERE NOT interno)=1, 'Catalogo publico incorrecto';
  ASSERT (SELECT nombre FROM barrio WHERE NOT interno)='Barrio aprobado solo de QA', 'Se altero opcion valida sintetica';
  ASSERT (SELECT id_barrio FROM usuario WHERE id_usuario=1)=3, 'Cambio referencia usuario';
  ASSERT (SELECT id_barrio FROM reporte WHERE id_reporte=1)=3, 'Cambio referencia reporte';
  ASSERT (SELECT count(*) FROM usuario)=1 AND (SELECT count(*) FROM reporte)=1, 'Perdida de datos';
  UPDATE barrio b SET interno=TRUE FROM barrios_temporales_aprobados a
    WHERE b.id_barrio=a.id_barrio AND b.id_comuna=a.id_comuna AND b.nombre=a.nombre AND NOT b.interno;
  GET DIAGNOSTICS filas=ROW_COUNT;
  ASSERT filas=0, 'La segunda actualizacion no es idempotente';
  RAISE NOTICE 'PASS: exclusion, opcion valida intacta, usuario/reporte/FK preservados e idempotencia';
END $$;\n`;
const setup = `CREATE SCHEMA qa_revision; SET search_path TO qa_revision;
CREATE TABLE barrio(id_barrio SERIAL PRIMARY KEY,id_comuna SMALLINT NOT NULL,nombre VARCHAR(100) NOT NULL,interno BOOLEAN NOT NULL DEFAULT FALSE);
CREATE TABLE usuario(id_usuario INTEGER PRIMARY KEY,id_barrio INTEGER NOT NULL REFERENCES barrio ON DELETE RESTRICT);
CREATE TABLE reporte(id_reporte INTEGER PRIMARY KEY,id_barrio INTEGER REFERENCES barrio ON DELETE RESTRICT);
INSERT INTO barrio VALUES (1,1,'Administración NotiUrba',TRUE),(3,3,'Barrio ejemplo 3',FALSE),(99,2,'Barrio aprobado solo de QA',FALSE);
INSERT INTO usuario VALUES(1,3); INSERT INTO reporte VALUES(1,3);
${seed}
DO $$ BEGIN
  ASSERT (SELECT count(*) FROM barrio)=11, 'Se perdieron filas';
  ASSERT NOT (SELECT interno FROM barrio WHERE id_barrio=3), 'Se reclasifico fila existente sin revision';
  ASSERT (SELECT bool_and(interno) FROM barrio WHERE id_barrio BETWEEN 1 AND 10 AND id_barrio<>3), 'Nuevos ejemplos expuestos';
  RAISE NOTICE 'PASS: nuevos ejemplos internos; identidades existentes conservadas';
END $$;
`;
const after = `DO $$ BEGIN
 ASSERT NOT (SELECT interno FROM barrio WHERE id_barrio=3), 'Rollback no restaura el estado';
 ASSERT (SELECT count(*) FROM barrio)=11, 'Rollback perdio filas';
 RAISE NOTICE 'PASS: plantilla termina en rollback sin cambios persistidos';
END $$;
`;
fs.writeFileSync('output/temporales-qa.sql', setup + cleanup.replace(/ROLLBACK;\s*$/,() => assertInside+'ROLLBACK;\n') + after);
console.log('SQL de QA sintetico preparado; no utiliza Neon ni el script de ejemplos completo.');
