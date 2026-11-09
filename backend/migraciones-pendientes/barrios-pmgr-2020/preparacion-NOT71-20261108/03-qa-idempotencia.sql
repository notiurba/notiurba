-- SOLO psql en la QA existente; prueba de reejecucion sobre 135 filas precargadas.
\set ON_ERROR_STOP on
BEGIN;
DO $guard$ BEGIN
 IF current_database()<>'notiurba_barrios_qa' OR current_user<>'qa_barrios' THEN RAISE EXCEPTION 'Destino QA incorrecto'; END IF;
 IF (SELECT count(*) FROM public.barrio WHERE NOT interno)<>135 THEN RAISE EXCEPTION 'Se requieren 135 filas QA precargadas'; END IF;
END $guard$;
SET LOCAL lock_timeout='3s';
SET LOCAL statement_timeout='15s';
CREATE TEMP TABLE qa_barrios_antes ON COMMIT DROP AS TABLE public.barrio;
\ir ../V17__catalogo_barrios_pmgr_2020.sql
\ir ../V17__catalogo_barrios_pmgr_2020.sql
DO $guard$ BEGIN
 IF EXISTS((SELECT * FROM public.barrio EXCEPT ALL SELECT * FROM qa_barrios_antes) UNION ALL (SELECT * FROM qa_barrios_antes EXCEPT ALL SELECT * FROM public.barrio)) THEN RAISE EXCEPTION 'La reejecucion cambio filas'; END IF;
END $guard$;
SELECT 'Reejecucion idempotente en QA precargada; transaccion revertida' AS resultado;
ROLLBACK;
-- Las secuencias SERIAL pueden avanzar aun con ON CONFLICT/ROLLBACK; no se resecuencian.
-- No prueba primera carga vacia: esa evidencia es CatalogoCargaBarriosIT previa.
