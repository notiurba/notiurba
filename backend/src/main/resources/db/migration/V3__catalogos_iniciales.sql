-- =====================================================================
-- NotiUrba · v5 · V3: catálogos y datos de referencia
-- Fuente: hoja "v5" del Google Sheet y Oficio S-43215 (correos de la Alcaldía).
-- Los canales marcados "sin confirmar" quedan INACTIVOS hasta validarlos.
-- No incluye barrios (se cargan aparte) ni polígonos de comunas (comuna.geom queda NULL).
-- =====================================================================

INSERT INTO rol (id_rol, nombre) VALUES (1,'CIUDADANO'),(2,'ADMIN');

INSERT INTO tipo_documento (id_tipo_documento, nombre, codigo) VALUES
 (1,'Cédula de ciudadanía','CC'),(2,'Cédula de extranjería','CE'),
 (3,'Permiso por protección temporal','PPT'),(4,'Pasaporte','PAS');

-- PENDIENTE: reemplazar el texto por el aviso de privacidad oficial antes de producción
INSERT INTO aviso_privacidad (id_aviso_privacidad, nombre, version, texto, vigente_desde) VALUES
 (1,'Aviso de privacidad NotiUrba','1.0','PENDIENTE: texto oficial del aviso de privacidad y tratamiento de datos personales.','2026-08-01 00:00:00-05');

INSERT INTO tipo_token (id_tipo_token, nombre) VALUES (1,'Verificación de correo'),(2,'Recuperación de contraseña');
INSERT INTO tipo_entidad (id_tipo_entidad, nombre) VALUES (1,'Alcaldía'),(2,'Empresa de servicios públicos');
INSERT INTO tipo_dependencia (id_tipo_dependencia, nombre) VALUES
 (1,'Centro de atención'),(2,'Secretaría'),(3,'Departamento Administrativo'),(4,'Oficina'),(5,'Área'),(6,'Servicio público');
INSERT INTO tipo_canal (id_tipo_canal, nombre) VALUES (1,'Correo'),(2,'Web'),(3,'Teléfono'),(4,'WhatsApp'),(5,'Presencial');
INSERT INTO estado_reporte (id_estado_reporte, nombre, orden) VALUES
 (1,'Recibido',1),(2,'Enviado',2),(3,'En trámite',3),(4,'Resuelto',4);
INSERT INTO modo_envio (id_modo_envio, nombre) VALUES (1,'RESEND'),(2,'BORRADOR');
INSERT INTO estado_envio (id_estado_envio, nombre) VALUES (1,'PENDIENTE'),(2,'ENVIADO'),(3,'FALLIDO');

INSERT INTO configuracion (clave, valor) VALUES ('vigencia_dias','30');

INSERT INTO comuna (id_comuna) SELECT generate_series(1,10);

INSERT INTO entidad_destino (id_entidad_destino, nombre, id_tipo_entidad) VALUES
 (1,'Alcaldía de Tuluá',1),
 (2,'Centroaguas S.A. E.S.P.',2),
 (3,'Gases de Occidente',2),
 (4,'Celsia',2),
 (5,'Veolia Aseo Tuluá S.A.S. E.S.P.',2);

-- id_dependencia, entidad, padre, tipo, nombre
INSERT INTO dependencia (id_dependencia, id_entidad_destino, id_dependencia_padre, id_tipo_dependencia, nombre) VALUES
 (1,1,NULL,1,'CAIC'),
 (2,1,NULL,2,'Secretaría de Hábitat e Infraestructura'),
 (3,1,NULL,2,'Secretaría de Gobierno, Convivencia y Seguridad'),
 (4,1,NULL,2,'SEDAMA'),
 (5,1,NULL,3,'Dpto. Administrativo de Movilidad y Seguridad Vial'),
 (6,1,NULL,2,'Secretaría de Salud'),
 (7,1,NULL,3,'Dpto. Administrativo de Planeación'),
 (8,1,NULL,2,'Secretaría de Educación'),
 (9,1,3,4,'Oficina de Gestión del Riesgo'),
 (10,1,7,5,'Área de Vivienda'),
 (11,2,NULL,6,'Acueducto y alcantarillado'),
 (12,3,NULL,6,'Gas natural'),
 (13,4,NULL,6,'Energía eléctrica'),
 (14,5,NULL,6,'Aseo');

-- id, tipo_canal, acceso, detalle, entidad, dependencia, principal, activo
-- Los correos de dependencias son solo de referencia: el envío va al Correo principal de la entidad (regla N1).
INSERT INTO canal_atencion (id_canal_atencion, id_tipo_canal, acceso, detalle, id_entidad_destino, id_dependencia, es_principal, activo) VALUES
 (1, 1,'servicioalciudadano@tulua.gov.co',NULL,1,NULL,TRUE,TRUE),
 (2, 3,'(602) 233 9300',NULL,1,NULL,FALSE,TRUE),
 (3, 1,'infraestructura@tulua.gov.co',NULL,NULL,2,FALSE,TRUE),
 (4, 1,'gobierno@tulua.gov.co',NULL,NULL,3,FALSE,TRUE),
 (5, 1,'movilidad@tulua.gov.co',NULL,NULL,5,FALSE,TRUE),
 (6, 1,'salud@tulua.gov.co',NULL,NULL,6,FALSE,TRUE),
 (7, 1,'planeacion@tulua.gov.co',NULL,NULL,7,FALSE,TRUE),
 (8, 1,'educacion@tulua.gov.co',NULL,NULL,8,FALSE,TRUE),
 (9, 1,'gestiondelriesgo@tulua.gov.co',NULL,NULL,9,FALSE,TRUE),
 (10,1,'vivienda@tulua.gov.co',NULL,NULL,10,FALSE,TRUE),
 (11,1,'sedama@tulua.gov.co','sin confirmar',NULL,4,FALSE,FALSE),
 (12,1,'notificacionespqr@centroaguas.com',NULL,2,NULL,TRUE,TRUE),
 (13,1,'info@centroaguas.com','solo notificaciones judiciales',2,NULL,FALSE,FALSE),
 (14,3,'(602) 231 7300',NULL,2,NULL,FALSE,TRUE),
 (15,3,'164','emergencias 24h',3,NULL,FALSE,TRUE),
 (16,3,'018000528888',NULL,3,NULL,FALSE,TRUE),
 (17,4,'315 665 8536',NULL,3,NULL,FALSE,TRUE),
 (18,1,'inquietudes@gdo.com.co','sin confirmar',3,NULL,FALSE,FALSE),
 (19,2,'https://clientes.celsia.com','PQR en Zona Clientes',4,NULL,FALSE,TRUE),
 (20,3,'01 8000 112 115',NULL,4,NULL,FALSE,TRUE),
 (21,4,'+57 310 390 3468',NULL,4,NULL,FALSE,TRUE),
 (22,1,'Servicioalcliente@celsia.com','sin confirmar',4,NULL,FALSE,FALSE),
 (23,3,'01 8000 950 096',NULL,5,NULL,FALSE,TRUE),
 (24,4,'317 372 5078',NULL,5,NULL,FALSE,TRUE),
 (25,5,'Carrera 26 No. 26-20, local 104',NULL,5,NULL,FALSE,TRUE),
 (26,1,'pqr.tulua@veolia.com','sin confirmar',5,NULL,FALSE,FALSE);

-- id, nombre, descripcion, dependencia, requiere_ubicacion
INSERT INTO categoria (id_categoria, nombre, descripcion, id_dependencia, requiere_ubicacion) VALUES
 (1,'Vías, huecos y andenes','Daños o huecos en el pavimento, vías y andenes',2,TRUE),
 (2,'Alumbrado, espacio público, convivencia y seguridad','Luminarias apagadas, espacio público, convivencia y seguridad',3,TRUE),
 (3,'Basuras, ambiente, árboles y contaminación','Residuos, afectaciones ambientales y manejo de árboles',4,TRUE),
 (4,'Señalización, semáforos y tránsito','Señales, semáforos y problemas de tránsito',5,TRUE),
 (5,'Riesgo sanitario y salud pública','Situaciones que afectan la salud pública',6,TRUE),
 (6,'Uso del suelo, planeación y licencias','Consultas y quejas sobre uso del suelo y licencias',7,FALSE),
 (7,'Instituciones educativas',NULL,8,FALSE),
 (8,'Riesgo de desastres','Amenazas y riesgo de desastres',9,TRUE),
 (9,'Vivienda',NULL,10,FALSE),
 (10,'Otro / eventos comunitarios',NULL,1,FALSE),
 (11,'Acueducto y alcantarillado','Fugas y fallas del servicio de acueducto y alcantarillado',11,TRUE),
 (12,'Gas natural','Fallas o sospecha de fuga en el servicio de gas natural',12,TRUE),
 (13,'Energía eléctrica','Fallas del servicio de energía eléctrica',13,TRUE),
 (14,'Aseo (falla del servicio)','Falla en la recolección de residuos',14,TRUE),
 (15,'Solicitud de información',NULL,1,FALSE);

-- Alinear las secuencias con los ids insertados a mano
SELECT setval(pg_get_serial_sequence('rol','id_rol'), (SELECT max(id_rol) FROM rol));
SELECT setval(pg_get_serial_sequence('tipo_documento','id_tipo_documento'), (SELECT max(id_tipo_documento) FROM tipo_documento));
SELECT setval(pg_get_serial_sequence('aviso_privacidad','id_aviso_privacidad'), (SELECT max(id_aviso_privacidad) FROM aviso_privacidad));
SELECT setval(pg_get_serial_sequence('tipo_token','id_tipo_token'), (SELECT max(id_tipo_token) FROM tipo_token));
SELECT setval(pg_get_serial_sequence('tipo_entidad','id_tipo_entidad'), (SELECT max(id_tipo_entidad) FROM tipo_entidad));
SELECT setval(pg_get_serial_sequence('tipo_dependencia','id_tipo_dependencia'), (SELECT max(id_tipo_dependencia) FROM tipo_dependencia));
SELECT setval(pg_get_serial_sequence('tipo_canal','id_tipo_canal'), (SELECT max(id_tipo_canal) FROM tipo_canal));
SELECT setval(pg_get_serial_sequence('estado_reporte','id_estado_reporte'), (SELECT max(id_estado_reporte) FROM estado_reporte));
SELECT setval(pg_get_serial_sequence('modo_envio','id_modo_envio'), (SELECT max(id_modo_envio) FROM modo_envio));
SELECT setval(pg_get_serial_sequence('estado_envio','id_estado_envio'), (SELECT max(id_estado_envio) FROM estado_envio));
SELECT setval(pg_get_serial_sequence('entidad_destino','id_entidad_destino'), (SELECT max(id_entidad_destino) FROM entidad_destino));
SELECT setval(pg_get_serial_sequence('dependencia','id_dependencia'), (SELECT max(id_dependencia) FROM dependencia));
SELECT setval(pg_get_serial_sequence('canal_atencion','id_canal_atencion'), (SELECT max(id_canal_atencion) FROM canal_atencion));
SELECT setval(pg_get_serial_sequence('categoria','id_categoria'), (SELECT max(id_categoria) FROM categoria));
