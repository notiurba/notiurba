-- =====================================================================
-- NotiUrba · V6: usuario.nombres y usuario.apellidos (modelo v7)
-- Reemplaza primer_nombre, segundo_nombre, primer_apellido y segundo_apellido
-- por dos columnas, como en el Figma («Nombres» y «Apellidos»).
-- En una base nueva convierte al admin que sembró V4; en una existente migra los datos.
-- IRREVERSIBLE (DROP COLUMN): antes de aplicarla en Neon, crear un branch de respaldo.
-- =====================================================================

-- 255 ≥ 100 + 1 + 100: el backfill no trunca nada
ALTER TABLE usuario ADD COLUMN nombres VARCHAR(255), ADD COLUMN apellidos VARCHAR(255);

UPDATE usuario SET
  nombres   = btrim(concat_ws(' ', primer_nombre, segundo_nombre)),
  apellidos = btrim(concat_ws(' ', primer_apellido, segundo_apellido));

ALTER TABLE usuario
  ALTER COLUMN nombres SET NOT NULL,
  ALTER COLUMN apellidos SET NOT NULL,
  ADD CONSTRAINT ck_usuario_nombres CHECK (btrim(nombres) <> '' AND btrim(apellidos) <> '');

ALTER TABLE usuario
  DROP COLUMN primer_nombre,
  DROP COLUMN segundo_nombre,
  DROP COLUMN primer_apellido,
  DROP COLUMN segundo_apellido;
