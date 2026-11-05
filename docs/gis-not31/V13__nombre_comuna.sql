-- PROPUESTA Flyway. No aplicada. Confirmar V13 disponible antes de integrarla.
-- Copiar a backend/src/main/resources/db/migration tras revisión.
-- No duplica la tabla comuna ni ix_comuna_geom; no carga polígonos provisionales.
ALTER TABLE public.comuna ADD COLUMN nombre VARCHAR(100);
UPDATE public.comuna SET nombre = 'Comuna ' || id_comuna::text;
ALTER TABLE public.comuna ALTER COLUMN nombre SET NOT NULL;
ALTER TABLE public.comuna ADD CONSTRAINT ck_comuna_nombre_no_vacio
    CHECK (btrim(nombre) <> '');
