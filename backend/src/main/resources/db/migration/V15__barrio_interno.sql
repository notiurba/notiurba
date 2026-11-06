-- NOT-55: el barrio «Administración NotiUrba» (V4) existe solo porque el usuario administrador
-- exige un barrio. No es un barrio real: se marca como interno para que no salga en el selector
-- del ciudadano (GET /api/catalogos/barrios) ni se pueda elegir al registrarse o reportar.
-- No se borra: el administrador lo referencia (id_barrio NOT NULL).
ALTER TABLE barrio ADD COLUMN interno BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE barrio SET interno = TRUE WHERE nombre = 'Administración NotiUrba';
