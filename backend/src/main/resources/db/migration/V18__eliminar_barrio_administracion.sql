-- El barrio «Administración NotiUrba» (V4) solo existía porque el usuario administrador exige un barrio
-- (id_barrio NOT NULL) y todavía no había barrios reales. Con los barrios reales cargados (V17) ya no hace
-- falta: se le asigna al administrador un barrio real y se elimina el barrio interno.
--
-- Es seguro repetirla y no falla en una base sin ese barrio. Si algún reporte o usuario que no es el
-- administrador lo usara, el barrio se conserva (sigue oculto, ver V15) en vez de borrar datos ajenos.

-- 1) Los usuarios que lo tienen (el administrador) pasan al primer barrio real, de forma determinista
UPDATE usuario
   SET id_barrio = (SELECT min(id_barrio) FROM barrio WHERE NOT interno)
 WHERE id_barrio IN (SELECT id_barrio FROM barrio WHERE interno AND nombre = 'Administración NotiUrba')
   AND EXISTS (SELECT 1 FROM barrio WHERE NOT interno);

-- 2) Se borra el barrio solo si ya nadie lo usa
DELETE FROM barrio b
 WHERE b.interno AND b.nombre = 'Administración NotiUrba'
   AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.id_barrio = b.id_barrio)
   AND NOT EXISTS (SELECT 1 FROM reporte r WHERE r.id_barrio = b.id_barrio);
