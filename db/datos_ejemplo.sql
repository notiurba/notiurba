-- =====================================================================
-- NotiUrba · v5 · DATOS DE EJEMPLO (solo desarrollo y pruebas; NO es migración)
-- Requiere V1, V2 y V3. Equivale a los ejemplos de la hoja "v5" del Google Sheet.
-- Correos @ejemplo.com y hashes son marcadores ficticios.
-- =====================================================================

INSERT INTO barrio (id_barrio, id_comuna, nombre)
SELECT g, g, 'Barrio ejemplo ' || g FROM generate_series(1,10) g;
SELECT setval(pg_get_serial_sequence('barrio','id_barrio'), 10);

INSERT INTO usuario (id_usuario, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, id_tipo_documento,
  numero_documento, id_barrio, direccion_correspondencia, correo, correo_verificado_en, telefono, password_hash,
  id_rol, id_aviso_privacidad, acepto_privacidad_en, creado_en) VALUES
 (1,'María','Fernanda','Gómez','Ruiz',1,'1118456789',1,'Cra 25 # 20-15, Tuluá','maria.gomez@ejemplo.com','2026-09-01 10:20:00-05','3101234567','$2b$10$EJEMPLOhashMariaGomez',1,1,'2026-09-01 10:05:00-05','2026-09-01 10:05:00-05'),
 (2,'Carlos',NULL,'Pérez','Londoño',1,'94512345',4,'Calle 30 # 15-40, Tuluá','carlos.perez@ejemplo.com','2026-09-10 15:45:00-05','3207654321','$2b$10$EJEMPLOhashCarlosPerez',1,1,'2026-09-10 15:30:00-05','2026-09-10 15:30:00-05'),
 (3,'Jhoan','Andrés','Rodríguez',NULL,3,'5678901',6,'Av. Cali # 40-22, Tuluá','jhoan.rodriguez@ejemplo.com','2026-10-02 08:30:00-05',NULL,'$2b$10$EJEMPLOhashJhoanRodriguez',1,1,'2026-10-02 08:12:00-05','2026-10-02 08:12:00-05'),
 (4,'Laura',NULL,'Administradora','Municipal',1,'66123456',7,'Carrera 24 # 28-30, Tuluá','admin@ejemplo.com','2026-08-15 09:10:00-05','3150000000','$2b$10$EJEMPLOhashAdmin',2,1,'2026-08-15 09:00:00-05','2026-08-15 09:00:00-05');
SELECT setval(pg_get_serial_sequence('usuario','id_usuario'), 4);

INSERT INTO token_verificacion (id_usuario, id_tipo_token, token_hash, expira_en, usado_en, creado_en) VALUES
 (1,1,'sha256_ejemplo_token_0001','2026-09-02 10:05:00-05','2026-09-01 10:20:00-05','2026-09-01 10:05:00-05'),
 (2,1,'sha256_ejemplo_token_0002','2026-09-11 15:30:00-05','2026-09-10 15:45:00-05','2026-09-10 15:30:00-05'),
 (3,1,'sha256_ejemplo_token_0003','2026-10-03 08:12:00-05','2026-10-02 08:30:00-05','2026-10-02 08:12:00-05');

-- Reportes (el trigger fija código, vigencia, estado inicial y primer historial)
INSERT INTO reporte (id_usuario, id_categoria, id_barrio, descripcion, ubicacion, direccion_incidente, matricula_cuenta_servicio,
                     autorizacion_envio_en, requiere_revision, creado_en) VALUES
 (1,1,3,'Hueco profundo en la carrera 25 frente al parque, riesgo para motociclistas.',
    ST_SetSRID(ST_MakePoint(-76.1953,4.0847),4326),'Cra 25 # 22-10',NULL,'2026-10-01 09:20:00-05',FALSE,'2026-10-01 09:20:00-05'),
 (2,11,5,'Fuga de agua constante en la esquina, lleva tres días sin arreglo.',
    ST_SetSRID(ST_MakePoint(-76.2011,4.0902),4326),'Calle 30 esq. Cra 12','123456789','2026-10-05 14:05:00-05',FALSE,'2026-10-05 14:05:00-05'),
 (3,2,2,'Tres luminarias seguidas apagadas en la calle, la zona queda muy oscura de noche.',
    ST_SetSRID(ST_MakePoint(-76.1887,4.0795),4326),'Calle 15 # 8-30',NULL,'2026-10-13 19:40:00-05',TRUE,'2026-10-13 19:40:00-05'),
 (1,14,8,'Bolsas de basura acumuladas hace una semana frente al colegio del barrio.',
    ST_SetSRID(ST_MakePoint(-76.2102,4.0711),4326),'Av. Cali # 40-05',NULL,'2026-09-20 07:50:00-05',FALSE,'2026-09-20 07:50:00-05'),
 (2,15,NULL,'Solicito información sobre los horarios y requisitos de atención en la Alcaldía.',
    NULL,NULL,NULL,'2026-10-20 11:00:00-05',FALSE,'2026-10-20 11:00:00-05');

INSERT INTO apoyo_reporte (id_reporte, id_usuario, creado_en) VALUES
 (1,2,'2026-10-02 08:15:00-05'),(1,3,'2026-10-03 12:40:00-05'),(2,3,'2026-10-06 09:05:00-05'),
 (3,2,'2026-10-14 10:00:00-05'),(4,2,'2026-09-21 18:30:00-05');

INSERT INTO evidencia_reporte (id_reporte, id_usuario, url, orden) VALUES
 (1,1,'https://ejemplo.com/fotos/reporte1.jpg',1),
 (1,1,'https://ejemplo.com/fotos/reporte1_b.jpg',2),
 (1,2,'https://ejemplo.com/fotos/reporte1_apoyo.jpg',1),
 (2,2,'https://ejemplo.com/fotos/reporte2.jpg',1);

INSERT INTO historial_estado (id_reporte, id_estado_reporte, observacion, id_usuario_cambia, cambiado_en) VALUES
 (1,2,'Correo enviado a la Alcaldía de Tuluá (CAIC).',4,'2026-10-01 10:00:00-05'),
 (2,2,'Correo enviado a Centroaguas (reintento tras rebote).',4,'2026-10-05 15:00:00-05'),
 (2,3,'La empresa confirmó visita técnica.',4,'2026-10-07 11:30:00-05'),
 (4,4,'Recolección realizada por Veolia Aseo.',4,'2026-09-22 16:10:00-05');

INSERT INTO registro_envio_correo (id_reporte, id_canal_atencion, id_modo_envio, id_estado_envio, error, enviado_en, creado_en) VALUES
 (1,1,1,2,NULL,'2026-10-01 10:00:00-05','2026-10-01 09:59:00-05'),
 (2,12,1,3,'Buzón no disponible (rebote)',NULL,'2026-10-05 14:59:00-05'),
 (2,12,1,2,NULL,'2026-10-05 15:00:00-05','2026-10-05 14:59:30-05'),
 (3,1,2,1,NULL,NULL,'2026-10-13 19:41:00-05');
