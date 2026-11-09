-- PREPARADO, NO ejecutado como archivo. Solo lecturas; no aplica V17 ni llama nextval/setval.
-- Usar solo junto al preflight original y con proyecto/rama/base previamente autorizados.
BEGIN TRANSACTION READ ONLY;
SELECT current_database() AS base, current_schema() AS esquema,
       current_user AS rol, current_setting('transaction_read_only') AS solo_lectura;
SELECT version,script,checksum,success FROM public.flyway_schema_history ORDER BY installed_rank;
SELECT pg_get_serial_sequence('public.barrio','id_barrio') AS secuencia,
       (SELECT max(id_barrio) FROM public.barrio) AS max_id;
-- Detener si pg_get_serial_sequence no coincide con esta secuencia antes de evaluar la siguiente consulta.
SELECT last_value,is_called FROM public.barrio_id_barrio_seq;
SELECT sequencename,increment_by,last_value FROM pg_sequences
 WHERE schemaname='public' AND sequencename='barrio_id_barrio_seq';
-- Con incremento 1, el siguiente candidato es last_value+1 si is_called; si no, last_value.
-- Debe superar los IDs existentes. No corregir la secuencia automaticamente ni reutilizar IDs de QA.
SELECT s.nombre AS estado,count(*) AS cantidad
 FROM public.registro_envio_correo e JOIN public.estado_envio s USING(id_estado_envio)
 GROUP BY s.nombre ORDER BY s.nombre;
SELECT c.id_categoria,c.nombre,c.requiere_ubicacion,x.correo_confirmado,x.prioriza_alterno
 FROM public.configuracion_enrutamiento x JOIN public.categoria c USING(id_categoria)
 WHERE c.id_categoria=14;
COMMIT;
