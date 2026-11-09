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
ROLLBACK;
