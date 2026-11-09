-- Solo lectura. Candidatos por nombre NO acreditan por si solos procedencia.
BEGIN READ ONLY;
SELECT current_database(), current_schema();
SELECT b.id_barrio, b.id_comuna, b.nombre, b.interno,
       b.id_barrio BETWEEN 1 AND 10
         AND b.id_comuna = b.id_barrio
         AND b.nombre = 'Barrio ejemplo ' || b.id_barrio AS coincide_triple_sql_ejemplo,
       (SELECT count(*) FROM usuario u WHERE u.id_barrio=b.id_barrio) AS usuarios,
       (SELECT count(*) FROM reporte r WHERE r.id_barrio=b.id_barrio) AS reportes
FROM barrio b
WHERE b.interno OR b.nombre ~ '^Barrio (de )?ejemplo [0-9]+$'
ORDER BY b.id_barrio;
SELECT conrelid::regclass::text AS tabla, conname, pg_get_constraintdef(oid) AS definicion
FROM pg_constraint
WHERE contype='f' AND confrelid='barrio'::regclass
ORDER BY conrelid::regclass::text, conname;
COMMIT;
