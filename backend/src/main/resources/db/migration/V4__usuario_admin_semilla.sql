-- =====================================================================
-- NotiUrba · V4: usuario ADMIN de desarrollo (semilla)
-- La contraseña NUNCA va en texto plano ni en hash fijo en el repo: se recibe
-- por el placeholder de Flyway ${adminPassword}, resuelto desde la variable
-- de entorno ADMIN_DEV_PASSWORD (ver application.yml). pgcrypto calcula el
-- hash bcrypt en la propia base de datos.
--
-- Los barrios reales se siembran aparte (ver docs/contexto/08-pendientes.md).
-- Como el usuario exige id_barrio NOT NULL, esta migración crea uno
-- administrativo mínimo solo si todavía no hay ninguno, para no depender del
-- orden en que se carguen los datos reales ni de db/datos_ejemplo.sql (que es
-- explícitamente "no es migración").
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO barrio (id_comuna, nombre)
SELECT 1, 'Administración NotiUrba'
WHERE NOT EXISTS (SELECT 1 FROM barrio);

INSERT INTO usuario (
  primer_nombre, primer_apellido, id_tipo_documento, numero_documento,
  id_barrio, direccion_correspondencia, correo, correo_verificado_en,
  password_hash, id_rol, id_aviso_privacidad, acepto_privacidad_en
) VALUES (
  'Admin', 'NotiUrba', 1, 'ADMIN-DEV-0001',
  (SELECT id_barrio FROM barrio ORDER BY id_barrio LIMIT 1),
  'Interno - no aplica', 'admin.dev@notiurba.local', now(),
  crypt('${adminPassword}', gen_salt('bf')), 2, 1, now()
)
ON CONFLICT (id_tipo_documento, numero_documento) DO NOTHING;
