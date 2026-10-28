-- =====================================================================
-- NotiUrba · PRUEBAS del esquema (requiere V1 a V7 y datos_ejemplo.sql)
-- Termina con error (ON_ERROR_STOP) si alguna regla falla.
-- =====================================================================
\set ON_ERROR_STOP on
\set QUIET on
\o /dev/null

CREATE FUNCTION pg_temp.debe_fallar(nombre text, sentencia text) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
  EXECUTE sentencia;
  RAISE EXCEPTION 'FALLO %: la sentencia debía ser rechazada', nombre;
EXCEPTION WHEN check_violation OR unique_violation OR foreign_key_violation OR not_null_violation OR string_data_right_truncation THEN
  RAISE NOTICE 'OK    rechazado: %', nombre;
END $$;

CREATE FUNCTION pg_temp.debe_ser(nombre text, condicion boolean) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
  IF condicion IS NOT TRUE THEN RAISE EXCEPTION 'FALLO %', nombre; END IF;
  RAISE NOTICE 'OK    %', nombre;
END $$;

\echo '== A. Estructura (CHECK / UNIQUE / FK) =='
SELECT pg_temp.debe_fallar('canal con entidad y dependencia', $q$INSERT INTO canal_atencion(id_tipo_canal,acceso,id_entidad_destino,id_dependencia) VALUES (1,'x@x.com',1,2)$q$);
SELECT pg_temp.debe_fallar('canal sin entidad ni dependencia', $q$INSERT INTO canal_atencion(id_tipo_canal,acceso) VALUES (1,'y@y.com')$q$);
SELECT pg_temp.debe_fallar('segundo Correo principal en la entidad', $q$INSERT INTO canal_atencion(id_tipo_canal,acceso,id_entidad_destino,es_principal) VALUES (1,'otro@tulua.gov.co',1,TRUE)$q$);
SELECT pg_temp.debe_fallar('canal principal inactivo', $q$INSERT INTO canal_atencion(id_tipo_canal,acceso,id_entidad_destino,es_principal,activo) VALUES (2,'https://x.co',1,TRUE,FALSE)$q$);
SELECT pg_temp.debe_fallar('foto con orden 4', $q$INSERT INTO evidencia_reporte(id_reporte,id_usuario,url,orden) VALUES (1,1,'u',4)$q$);
SELECT pg_temp.debe_fallar('foto repetida (mismo reporte, usuario y orden)', $q$INSERT INTO evidencia_reporte(id_reporte,id_usuario,url,orden) VALUES (1,1,'u',1)$q$);
SELECT pg_temp.debe_fallar('comuna 11', $q$INSERT INTO comuna(id_comuna) VALUES (11)$q$);
SELECT pg_temp.debe_fallar('descripción de 5 caracteres', $q$INSERT INTO reporte(id_usuario,id_categoria,descripcion,autorizacion_envio_en,vigente_hasta) VALUES (1,6,'corta',now(),now())$q$);
SELECT pg_temp.debe_fallar('descripción de 501 caracteres', format($q$INSERT INTO reporte(id_usuario,id_categoria,descripcion,autorizacion_envio_en,vigente_hasta) VALUES (1,6,%L,now(),now())$q$, repeat('x',501)));
SELECT pg_temp.debe_fallar('dependencia padre de sí misma', $q$UPDATE dependencia SET id_dependencia_padre=id_dependencia WHERE id_dependencia=9$q$);
SELECT pg_temp.debe_fallar('mismo documento dos veces', $q$INSERT INTO usuario(nombres,apellidos,id_tipo_documento,numero_documento,id_barrio,direccion_correspondencia,correo,password_hash,id_rol,id_aviso_privacidad,acepto_privacidad_en) VALUES ('A','B',1,'1118456789',1,'d','nuevo@x.com','h',1,1,now())$q$);
SELECT pg_temp.debe_fallar('correo repetido con otras mayúsculas', $q$INSERT INTO usuario(nombres,apellidos,id_tipo_documento,numero_documento,id_barrio,direccion_correspondencia,correo,password_hash,id_rol,id_aviso_privacidad,acepto_privacidad_en) VALUES ('A','B',1,'777','1','d','MARIA.GOMEZ@ejemplo.com','h',1,1,now())$q$);
SELECT pg_temp.debe_fallar('vigencia_dias no numérica', $q$UPDATE configuracion SET valor='treinta' WHERE clave='vigencia_dias'$q$);
SELECT pg_temp.debe_fallar('verificación de correo anterior al registro', $q$UPDATE usuario SET correo_verificado_en = creado_en - interval '1 day' WHERE id_usuario=1$q$);

\echo '== B. Triggers (reglas B1–B12) =='
SELECT pg_temp.debe_fallar('B1 dependencia hija de otra entidad', $q$INSERT INTO dependencia(id_entidad_destino,id_dependencia_padre,id_tipo_dependencia,nombre) VALUES (2,3,4,'Hija cruzada')$q$);
SELECT pg_temp.debe_fallar('B1 ciclo en la jerarquía', $q$UPDATE dependencia SET id_dependencia_padre=9 WHERE id_dependencia=3$q$);
SELECT pg_temp.debe_fallar('B2 foto de quien no reportó ni apoyó', $q$INSERT INTO evidencia_reporte(id_reporte,id_usuario,url,orden) VALUES (1,4,'u',1)$q$);
SELECT pg_temp.debe_fallar('B3 categoría que exige ubicación, sin ubicación', $q$INSERT INTO reporte(id_usuario,id_categoria,descripcion,autorizacion_envio_en,vigente_hasta) VALUES (1,1,'sin ubicación aunque la exige',now(),now())$q$);
SELECT pg_temp.debe_fallar('B5 envío a un canal de Teléfono', $q$INSERT INTO registro_envio_correo(id_reporte,id_canal_atencion,id_modo_envio,id_estado_envio) VALUES (1,2,1,1)$q$);
SELECT pg_temp.debe_fallar('B5 envío al correo de una dependencia', $q$INSERT INTO registro_envio_correo(id_reporte,id_canal_atencion,id_modo_envio,id_estado_envio) VALUES (1,3,1,1)$q$);
SELECT pg_temp.debe_fallar('B5 envío al correo de otra entidad', $q$INSERT INTO registro_envio_correo(id_reporte,id_canal_atencion,id_modo_envio,id_estado_envio) VALUES (1,12,1,1)$q$);
SELECT pg_temp.debe_fallar('B6 matrícula en reporte de la Alcaldía', $q$UPDATE reporte SET matricula_cuenta_servicio='CS-1' WHERE id_reporte=3$q$);
SELECT pg_temp.debe_fallar('B7 editar el acceso de un canal', $q$UPDATE canal_atencion SET acceso='nuevo@centroaguas.com' WHERE id_canal_atencion=12$q$);
SELECT pg_temp.debe_fallar('B10 envío FALLIDO sin error', $q$INSERT INTO registro_envio_correo(id_reporte,id_canal_atencion,id_modo_envio,id_estado_envio) VALUES (1,1,1,3)$q$);
SELECT pg_temp.debe_fallar('B10 envío ENVIADO sin fecha', $q$INSERT INTO registro_envio_correo(id_reporte,id_canal_atencion,id_modo_envio,id_estado_envio) VALUES (1,1,1,2)$q$);
SELECT pg_temp.debe_fallar('B10 envío PENDIENTE con error', $q$INSERT INTO registro_envio_correo(id_reporte,id_canal_atencion,id_modo_envio,id_estado_envio,error) VALUES (1,1,1,1,'x')$q$);
SELECT pg_temp.debe_fallar('B11 el estado retrocede', $q$INSERT INTO historial_estado(id_reporte,id_estado_reporte) VALUES (2,1)$q$);
SELECT pg_temp.debe_fallar('B11 modificar el historial', $q$UPDATE historial_estado SET observacion='x' WHERE id_historial_estado=1$q$);
SELECT pg_temp.debe_fallar('B11 borrar el historial directamente', $q$DELETE FROM historial_estado WHERE id_historial_estado=1$q$);
SELECT pg_temp.debe_fallar('B12 el autor apoya su reporte', $q$INSERT INTO apoyo_reporte(id_reporte,id_usuario) VALUES (1,1)$q$);
SELECT pg_temp.debe_fallar('B12 apoyo a un reporte vencido', $q$INSERT INTO apoyo_reporte(id_reporte,id_usuario,creado_en) VALUES (4,3,'2026-12-01')$q$);

\echo '== C. Reglas derivadas =='
SELECT pg_temp.debe_ser('B4 estado del reporte = último historial (5 reportes)',
  (SELECT count(*) FROM reporte r WHERE r.id_estado_reporte =
     (SELECT h.id_estado_reporte FROM historial_estado h WHERE h.id_reporte=r.id_reporte
       ORDER BY h.cambiado_en DESC, h.id_historial_estado DESC LIMIT 1)) = 5);
SELECT pg_temp.debe_ser('B9 vigente_hasta = creado_en + 30 días',
  (SELECT count(*) FROM reporte WHERE vigente_hasta <> creado_en + interval '30 days') = 0);
SELECT pg_temp.debe_ser('Código de seguimiento NU-000001 … NU-000005',
  (SELECT string_agg(codigo_seguimiento, ',' ORDER BY id_reporte) FROM reporte) = 'NU-000001,NU-000002,NU-000003,NU-000004,NU-000005');
SELECT pg_temp.debe_ser('Cada reporte nuevo queda con su primer historial "Recibido"',
  (SELECT count(*) FROM historial_estado WHERE id_estado_reporte=1) = 5);
SELECT pg_temp.debe_ser('N2 solo NU-000004 (Veolia) queda sin correo',
  (SELECT string_agg(codigo_seguimiento, ',') FROM v_reporte_no_enviado_por_correo) = 'NU-000004');
SELECT pg_temp.debe_ser('N1 los reportes 1, 3 y 5 van al CAIC y el 2 a Centroaguas',
  (SELECT string_agg(id_reporte::text || '=' || correo_destino, ',' ORDER BY id_reporte)
     FROM v_reporte_destino WHERE correo_destino IS NOT NULL)
  = '1=servicioalciudadano@tulua.gov.co,2=notificacionespqr@centroaguas.com,3=servicioalciudadano@tulua.gov.co,5=servicioalciudadano@tulua.gov.co');
SELECT pg_temp.debe_ser('N3 sin ubicación ni barrio no hay comuna (reporte 5)',
  (SELECT id_comuna IS NULL FROM v_reporte_comuna WHERE id_reporte=5));
SELECT pg_temp.debe_ser('N3 con geom vacío la comuna sale del barrio',
  (SELECT string_agg(id_comuna::text, ',' ORDER BY id_reporte) FROM v_reporte_comuna WHERE id_reporte IN (1,2,3,4)) = '3,5,2,8');

\echo '== D. PostGIS: punto en polígono =='
UPDATE comuna SET geom = ST_Multi(ST_MakeEnvelope(-76.20,4.08,-76.19,4.09,4326)) WHERE id_comuna = 10;
SELECT pg_temp.debe_ser('Reporte 1 cae en el polígono de la comuna 10 (por polígono)',
  (SELECT id_comuna = 10 AND origen = 'poligono' FROM v_reporte_comuna WHERE id_reporte=1));
SELECT pg_temp.debe_ser('Reporte 2 (fuera del polígono) sigue por barrio',
  (SELECT id_comuna = 5 AND origen = 'barrio' FROM v_reporte_comuna WHERE id_reporte=2));
UPDATE comuna SET geom = NULL WHERE id_comuna = 10;
SELECT pg_temp.debe_ser('El mapa solo muestra reportes con ubicación y vigentes',
  (SELECT count(*) FROM v_reporte_mapa WHERE ubicacion IS NULL OR vigente_hasta <= now()) = 0);
SELECT pg_temp.debe_ser('Los índices GiST existen',
  (SELECT count(*) FROM pg_indexes WHERE indexname IN ('ix_reporte_ubicacion','ix_comuna_geom')) = 2);

\echo '== E. Habeas data (borrados en cascada) =='
BEGIN;
DELETE FROM usuario WHERE id_usuario = 1;
SELECT pg_temp.debe_ser('Se borran los reportes del usuario 1 y todo lo que cuelga de ellos',
  (SELECT count(*) FROM reporte WHERE id_usuario=1) = 0
  AND (SELECT count(*) FROM historial_estado WHERE id_reporte IN (1,4)) = 0
  AND (SELECT count(*) FROM evidencia_reporte WHERE id_reporte = 1) = 0
  AND (SELECT count(*) FROM registro_envio_correo WHERE id_reporte = 1) = 0
  AND (SELECT count(*) FROM apoyo_reporte WHERE id_reporte IN (1,4)) = 0
  AND (SELECT count(*) FROM reporte) = 3);
ROLLBACK;
BEGIN;
DELETE FROM usuario WHERE id_usuario = 4;
SELECT pg_temp.debe_ser('Al borrar al admin la auditoría queda en NULL y los reportes siguen',
  (SELECT count(*) FROM historial_estado WHERE id_usuario_cambia IS NULL) = 9
  AND (SELECT count(*) FROM reporte) = 5);
ROLLBACK;

\echo '== F. Login con Google (V5 · B13) =='
-- B13 es un trigger diferido: dentro de una transacción, SET CONSTRAINTS ALL IMMEDIATE
-- hace que se evalúe al final de cada sentencia y no solo al confirmar.
BEGIN;
SET CONSTRAINTS ALL IMMEDIATE;
SELECT pg_temp.debe_fallar('B13 usuario sin contraseña ni identidad externa', $q$INSERT INTO usuario(nombres,apellidos,id_tipo_documento,numero_documento,id_barrio,direccion_correspondencia,correo,id_rol,id_aviso_privacidad,acepto_privacidad_en) VALUES ('Ana','Sin Credencial',1,'900001',1,'d','ana@ejemplo.com',1,1,now())$q$);
ROLLBACK;

-- Flujo real del backend: usuario + identidad en una transacción; B13 se revisa al confirmar
BEGIN;
INSERT INTO usuario(id_usuario,nombres,apellidos,id_tipo_documento,numero_documento,id_barrio,direccion_correspondencia,correo,id_rol,id_aviso_privacidad,acepto_privacidad_en)
VALUES (50,'Ana','Google',1,'900002',1,'d','ana.google@ejemplo.com',1,1,now());
INSERT INTO identidad_externa(id_usuario,id_proveedor_identidad,identificador_externo) VALUES (50,1,'google-sub-0001');
COMMIT;
SELECT pg_temp.debe_ser('B13 usuario sin contraseña con identidad GOOGLE',
  (SELECT password_hash IS NULL FROM usuario WHERE id_usuario=50)
  AND (SELECT count(*) FROM identidad_externa WHERE id_usuario=50) = 1);
SELECT pg_temp.debe_fallar('mismo sub de Google dos veces', $q$INSERT INTO identidad_externa(id_usuario,id_proveedor_identidad,identificador_externo) VALUES (1,1,'google-sub-0001')$q$);
SELECT pg_temp.debe_fallar('dos identidades GOOGLE para el mismo usuario', $q$INSERT INTO identidad_externa(id_usuario,id_proveedor_identidad,identificador_externo) VALUES (50,1,'google-sub-0002')$q$);
BEGIN;
SET CONSTRAINTS ALL IMMEDIATE;
SELECT pg_temp.debe_fallar('B13 borrar la única identidad de un usuario sin contraseña', $q$DELETE FROM identidad_externa WHERE id_usuario=50$q$);
ROLLBACK;
DELETE FROM usuario WHERE id_usuario = 50;
SELECT pg_temp.debe_ser('Borrar el usuario borra sus identidades (habeas data)',
  (SELECT count(*) FROM identidad_externa WHERE id_usuario=50) = 0);

\echo '== G. Nombres y apellidos (V6) =='
SELECT pg_temp.debe_fallar('nombres en blanco', $q$INSERT INTO usuario(nombres,apellidos,id_tipo_documento,numero_documento,id_barrio,direccion_correspondencia,correo,password_hash,id_rol,id_aviso_privacidad,acepto_privacidad_en) VALUES ('  ','B',1,'900003',1,'d','blanco@x.com','h',1,1,now())$q$);
SELECT pg_temp.debe_fallar('apellidos NULL', $q$INSERT INTO usuario(nombres,apellidos,id_tipo_documento,numero_documento,id_barrio,direccion_correspondencia,correo,password_hash,id_rol,id_aviso_privacidad,acepto_privacidad_en) VALUES ('A',NULL,1,'900004',1,'d','nulo@x.com','h',1,1,now())$q$);
SELECT pg_temp.debe_ser('Las 4 columnas de nombre fragmentado ya no existen',
  (SELECT count(*) FROM information_schema.columns WHERE table_name='usuario'
     AND column_name ~ '^(primer|segundo)_(nombre|apellido)$') = 0);
SELECT pg_temp.debe_ser('tipo_documento es solo CC, CE, PPT y PAS (sin TI)',
  (SELECT string_agg(codigo, ',' ORDER BY id_tipo_documento) FROM tipo_documento) = 'CC,CE,PPT,PAS');
SELECT pg_temp.debe_ser('El admin de V4 sigue y quedó como Admin / NotiUrba',
  (SELECT nombres='Admin' AND apellidos='NotiUrba' AND password_hash IS NOT NULL
     FROM usuario WHERE numero_documento='ADMIN-DEV-0001'));

\echo '== H. Ajustes del modelo v8 (V7) =='
BEGIN;
INSERT INTO reporte(id_usuario,id_categoria,id_barrio,descripcion,autorizacion_envio_en)
VALUES (1,1,3,'Hueco reportado solo con el barrio',now());
INSERT INTO reporte(id_usuario,id_categoria,descripcion,ubicacion,autorizacion_envio_en)
VALUES (1,1,'Hueco reportado solo con el GPS',ST_SetSRID(ST_MakePoint(-76.1953,4.0847),4326),now());
SELECT pg_temp.debe_ser('B3 categoría que exige ubicación: solo barrio o solo GPS pasan',
  (SELECT count(*) FROM reporte WHERE descripcion LIKE 'Hueco reportado solo con%') = 2);
ROLLBACK;
SELECT pg_temp.debe_fallar('B3 categoría que exige ubicación, ni GPS ni barrio', $q$INSERT INTO reporte(id_usuario,id_categoria,descripcion,autorizacion_envio_en) VALUES (1,1,'Sin GPS y sin barrio aunque la exige',now())$q$);
BEGIN;
UPDATE reporte SET radicado_oficial = 'RAD-2026-000123' WHERE id_reporte = 1;
SELECT pg_temp.debe_ser('radicado_oficial acepta NULL y texto',
  (SELECT radicado_oficial = 'RAD-2026-000123' FROM reporte WHERE id_reporte=1)
  AND (SELECT radicado_oficial IS NULL FROM reporte WHERE id_reporte=2));
ROLLBACK;
SELECT pg_temp.debe_ser('reporte.ubicacion existe y reporte.punto no',
  (SELECT count(*) FROM information_schema.columns WHERE table_name='reporte' AND column_name='ubicacion') = 1
  AND (SELECT count(*) FROM information_schema.columns WHERE table_name='reporte' AND column_name='punto') = 0);
-- Comparar con los textos de V7 detecta un UPDATE que no encontró su categoría (0 filas)
SELECT pg_temp.debe_ser('Las 15 categorías tienen su ejemplo de V7 en descripcion',
  (SELECT count(*) FROM categoria WHERE btrim(coalesce(descripcion,'')) <> '') = 15
  AND (SELECT count(*) FROM categoria WHERE descripcion IN (
    'Huecos, grietas, andenes rotos','Bombillo apagado, ruido, riñas','Basura acumulada, quema, tala',
    'Semáforo dañado, señal caída','Fuga de agua, sin agua, caño tapado','Apagón, cable caído, transformador',
    'Olor a gas, sin servicio de gas','Camión no pasó, basura sin recoger','Derrumbe, inundación, árbol por caer',
    'Casa en mal estado, riesgo de colapso','Plagas, aguas estancadas, alimentos','Construcción sin licencia, invasión',
    'Problema en un colegio o escuela','Una pregunta o trámite en la Alcaldía','Un evento o caso que no encaja')) = 15);
SELECT pg_temp.debe_ser('Existe el tipo de token «Recuperación de contraseña»',
  EXISTS (SELECT 1 FROM tipo_token WHERE nombre = 'Recuperación de contraseña'));

\o
\echo '== TODAS LAS PRUEBAS PASARON =='
