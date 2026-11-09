CREATE SCHEMA qa_revision; SET search_path TO qa_revision;
CREATE TABLE barrio(id_barrio SERIAL PRIMARY KEY,id_comuna SMALLINT NOT NULL,nombre VARCHAR(100) NOT NULL,interno BOOLEAN NOT NULL DEFAULT FALSE);
CREATE TABLE usuario(id_usuario INTEGER PRIMARY KEY,id_barrio INTEGER NOT NULL REFERENCES barrio ON DELETE RESTRICT);
CREATE TABLE reporte(id_reporte INTEGER PRIMARY KEY,id_barrio INTEGER REFERENCES barrio ON DELETE RESTRICT);
INSERT INTO barrio VALUES (1,1,'Administración NotiUrba',TRUE),(3,3,'Barrio ejemplo 3',FALSE),(99,2,'Barrio aprobado solo de QA',FALSE);
INSERT INTO usuario VALUES(1,3); INSERT INTO reporte VALUES(1,3);
INSERT INTO barrio (id_barrio, id_comuna, nombre, interno)
SELECT g, g, 'Barrio ejemplo ' || g, TRUE FROM generate_series(1,10) g
ON CONFLICT (id_barrio) DO NOTHING;
DO $$ BEGIN
  ASSERT (SELECT count(*) FROM barrio)=11, 'Se perdieron filas';
  ASSERT NOT (SELECT interno FROM barrio WHERE id_barrio=3), 'Se reclasifico fila existente sin revision';
  ASSERT (SELECT bool_and(interno) FROM barrio WHERE id_barrio BETWEEN 1 AND 10 AND id_barrio<>3), 'Nuevos ejemplos expuestos';
  RAISE NOTICE 'PASS: nuevos ejemplos internos; identidades existentes conservadas';
END $$;
-- PLANTILLA PENDIENTE DE REVISION. No ejecutada; no es migracion Flyway.
-- Solo entorno y filas expresamente autorizados. Nunca aplicar por coincidencia de ID.
-- No borra ni reemplaza IDs. Conserva usuario/reporte y sus referencias.
-- Andres debe revisar la auditoria, la lista exacta, otras FK y las reglas de edicion de perfiles.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';
CREATE TEMP TABLE barrios_temporales_aprobados (
    id_barrio INTEGER PRIMARY KEY,
    id_comuna SMALLINT NOT NULL,
    nombre VARCHAR(100) NOT NULL
) ON COMMIT DROP;
INSERT INTO barrios_temporales_aprobados VALUES (3,3,'Barrio ejemplo 3');
-- Lista vacia deliberadamente: no se encontraron temporales en Neon durante la auditoria.
-- Completar SOLO con triples comprobados en el destino y aprobados por el equipo.

-- Si alguna identidad cambio desde la revision, abortar sin cambios.
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM barrios_temporales_aprobados a
    LEFT JOIN barrio b ON b.id_barrio=a.id_barrio
    WHERE b.id_barrio IS NULL OR b.id_comuna<>a.id_comuna OR b.nombre<>a.nombre
       OR a.nombre !~ '^Barrio (de )?ejemplo [0-9]+$'
  ) THEN
    RAISE EXCEPTION 'Lista de temporales no coincide con el destino; revisar autorizacion';
  END IF;
END $$;

SELECT b.id_barrio, b.id_comuna, b.nombre, b.interno,
       (SELECT count(*) FROM usuario u WHERE u.id_barrio=b.id_barrio) AS usuarios,
       (SELECT count(*) FROM reporte r WHERE r.id_barrio=b.id_barrio) AS reportes
FROM barrio b JOIN barrios_temporales_aprobados a USING (id_barrio);

UPDATE barrio b SET interno=TRUE
FROM barrios_temporales_aprobados a
WHERE b.id_barrio=a.id_barrio AND b.id_comuna=a.id_comuna AND b.nombre=a.nombre
  AND NOT b.interno;

-- No existe DELETE ni UPDATE de usuario/reporte ni cambio de claves.
-- Antes de un eventual COMMIT autorizado comprobar conteos, FK, catalogos y perfiles existentes.
DO $$ DECLARE filas integer; BEGIN
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
END $$;
ROLLBACK;
DO $$ BEGIN
 ASSERT NOT (SELECT interno FROM barrio WHERE id_barrio=3), 'Rollback no restaura el estado';
 ASSERT (SELECT count(*) FROM barrio)=11, 'Rollback perdio filas';
 RAISE NOTICE 'PASS: plantilla termina en rollback sin cambios persistidos';
END $$;
