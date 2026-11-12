-- Cada canal dice para qué sirve, en qué orden se muestra, su horario y de dónde sale el dato.
-- Datos corregidos con la evidencia del 12 nov 2026 (respuestas de las entidades y sus sitios
-- oficiales). Ningún canal se borra: el que no sirve se desactiva y su motivo queda en detalle.

CREATE TABLE finalidad_canal (
  id_finalidad_canal SMALLSERIAL PRIMARY KEY,
  nombre VARCHAR(40) NOT NULL UNIQUE
);
INSERT INTO finalidad_canal (id_finalidad_canal, nombre) VALUES
 (1, 'Radicar PQR'), (2, 'Reportar daño'), (3, 'PQR anónimo'),
 (4, 'Consultar estado'), (5, 'Emergencia'), (6, 'Información');
SELECT setval(pg_get_serial_sequence('finalidad_canal', 'id_finalidad_canal'), 6);

ALTER TABLE canal_atencion
  ADD COLUMN id_finalidad_canal SMALLINT NOT NULL DEFAULT 6
    REFERENCES finalidad_canal(id_finalidad_canal) ON DELETE RESTRICT,
  ADD COLUMN orden SMALLINT NOT NULL DEFAULT 100,
  ADD COLUMN horario VARCHAR(200),
  ADD COLUMN fuente VARCHAR(500),
  ADD COLUMN verificado_en DATE;

CREATE INDEX ix_canal_entidad_orden ON canal_atencion (id_entidad_destino, orden) WHERE activo;

-- Alcaldía (CAIC)
UPDATE canal_atencion SET id_finalidad_canal = 1, orden = 1,
       fuente = 'Respuesta del CAIC con radicado E-.29570 (30 sep 2026)', verificado_en = DATE '2026-11-12'
 WHERE id_canal_atencion = 1;
UPDATE canal_atencion SET id_finalidad_canal = 6, orden = 5,
       horario = 'Lunes a jueves, 7:30 a. m. a 12:00 m. y 1:30 a 6:00 p. m.; viernes hasta las 5:00 p. m.',
       fuente = 'https://www.tulua.gov.co/publicaciones/26/servicio-al-publico-normas-formularios-y-protocolos-de-atencion/',
       verificado_en = DATE '2026-11-12'
 WHERE id_canal_atencion = 2;
INSERT INTO canal_atencion (id_tipo_canal, acceso, detalle, id_entidad_destino, id_finalidad_canal, orden, horario, fuente, verificado_en) VALUES
 (2, 'https://www.tulua.gov.co/publicaciones/6404/pqrsdf/', 'Formulario PQRSDF de la Alcaldía', 1, 1, 2, NULL,
  'https://www.tulua.gov.co/publicaciones/6404/pqrsdf/', DATE '2026-11-12'),
 (2, 'https://tulua.tramiter.co/documentos/seleccionar-tramite-anonimo', 'PQRSDF sin dar sus datos', 1, 3, 3, NULL,
  'https://www.tulua.gov.co/publicaciones/6404/pqrsdf/', DATE '2026-11-12'),
 (2, 'https://tulua.tramiter.co/documentos/consultar-tramite-web', 'Con el número de radicado', 1, 4, 4, NULL,
  'https://www.tulua.gov.co/publicaciones/6404/pqrsdf/', DATE '2026-11-12'),
 (2, 'https://www.tulua.gov.co/', 'Agente virtual en la página de la Alcaldía', 1, 6, 6, '24 horas',
  'https://www.tulua.gov.co/publicaciones/26/servicio-al-publico-normas-formularios-y-protocolos-de-atencion/', DATE '2026-11-12'),
 (5, 'Carrera 25 #25-04, Tuluá', 'Centro Administrativo Municipal (CAM)', 1, 1, 7,
  'Lunes a jueves, 7:30 a. m. a 12:00 m. y 1:30 a 6:00 p. m.; viernes hasta las 5:00 p. m.',
  'https://www.tulua.gov.co/publicaciones/26/servicio-al-publico-normas-formularios-y-protocolos-de-atencion/', DATE '2026-11-12');

-- Centroaguas: el correo autorizado para PQR es info@ (acto GAC-003660-26); notificacionespqr@
-- solo envía sus respuestas. Primero se quita lo principal y después se marca el otro.
UPDATE canal_atencion SET es_principal = FALSE, activo = FALSE,
       detalle = 'Solo envía las respuestas automáticas de Centroaguas; no recibe PQR',
       fuente = 'Acto GAC-003660-26 de Centroaguas (13 oct 2026)', verificado_en = DATE '2026-11-12'
 WHERE id_canal_atencion = 12;
UPDATE canal_atencion SET activo = TRUE, es_principal = TRUE, id_finalidad_canal = 1, orden = 1,
       detalle = 'Único correo autorizado para PQR',
       fuente = 'Acto GAC-003660-26 de Centroaguas (13 oct 2026), radicado CO260929-3786', verificado_en = DATE '2026-11-12'
 WHERE id_canal_atencion = 13;
UPDATE configuracion_enrutamiento SET id_canal_correo = 13, correo_confirmado = TRUE,
       fuente_validacion = 'Acto GAC-003660-26 de Centroaguas (13 oct 2026)', validado_en = DATE '2026-10-13'
 WHERE codigo = 'E01';
UPDATE canal_atencion SET id_finalidad_canal = 1, orden = 2, detalle = 'Opción 3: PQR. Opción 116: daños',
       fuente = 'https://www.centroaguas.com/index.php/contactenos', verificado_en = DATE '2026-11-12'
 WHERE id_canal_atencion = 14;
INSERT INTO canal_atencion (id_tipo_canal, acceso, detalle, id_entidad_destino, id_finalidad_canal, orden, horario, fuente, verificado_en) VALUES
 (2, 'https://www.centroaguas.com/index.php/contactenos', 'Página de contacto con el aplicativo PQRS', 2, 1, 3, NULL,
  'https://www.centroaguas.com/index.php/contactenos', DATE '2026-11-12'),
 (5, 'Calle 25 #32A-31, barrio Alvernia, Tuluá', 'Oficina de Atención al Usuario', 2, 1, 4,
  'Lunes a viernes, 7:30 a 11:30 a. m. y 1:30 a 4:00 p. m.',
  'https://www.centroaguas.com/index.php/contactenos', DATE '2026-11-12');

-- Gases de Occidente: su correo exige confirmar en 24 horas, así que no sirve para envío automático.
UPDATE canal_atencion SET es_principal = FALSE, activo = FALSE,
       detalle = 'Responde con un aviso que exige confirmar en 24 horas; no sirve para envío automático',
       fuente = 'Respuesta automática de inquietudes@gdo.com.co (29 sep 2026)', verificado_en = DATE '2026-11-12'
 WHERE id_canal_atencion = 18;
UPDATE configuracion_enrutamiento SET correo_confirmado = FALSE WHERE codigo = 'E02';
UPDATE canal_atencion SET activo = FALSE, detalle = 'No aparece en el sitio oficial; sin verificar'
 WHERE id_canal_atencion = 17;
UPDATE canal_atencion SET id_finalidad_canal = 5, orden = 4, horario = '24 horas', detalle = 'Fugas y emergencias de gas',
       fuente = 'https://www.gdo.com.co/Paginas/nuestros-canales-de-comunicacion.aspx', verificado_en = DATE '2026-11-12'
 WHERE id_canal_atencion = 15;
UPDATE canal_atencion SET id_finalidad_canal = 1, orden = 3, horario = '24 horas', detalle = 'Contact center desde teléfono fijo',
       fuente = 'https://www.gdo.com.co/Paginas/nuestros-canales-de-comunicacion.aspx', verificado_en = DATE '2026-11-12'
 WHERE id_canal_atencion = 16;
INSERT INTO canal_atencion (id_tipo_canal, acceso, detalle, id_entidad_destino, id_finalidad_canal, orden, horario, fuente, verificado_en) VALUES
 (2, 'https://www.gdo.com.co/Paginas/Registro-de-pqrs.aspx', 'Registro de PQR', 3, 1, 1, NULL,
  'https://www.gdo.com.co/Paginas/nuestros-canales-de-comunicacion.aspx', DATE '2026-11-12'),
 (3, '(602) 418 7333', 'Contact center desde celular', 3, 1, 2, '24 horas',
  'https://www.gdo.com.co/Paginas/nuestros-canales-de-comunicacion.aspx', DATE '2026-11-12');

-- Celsia: su correo rechaza mensajes externos; la PQR se radica en Zona Clientes.
UPDATE canal_atencion SET detalle = 'Rechaza correos externos (550 5.7.1); remite a Zona Clientes',
       fuente = 'Rechazo de servicioalcliente@celsia.com (29 sep 2026)', verificado_en = DATE '2026-11-12'
 WHERE id_canal_atencion = 22;
UPDATE canal_atencion SET id_finalidad_canal = 1, orden = 1, detalle = 'Zona Clientes, opción PQR',
       fuente = 'https://www.celsia.com/es/zona-clientes/', verificado_en = DATE '2026-11-12'
 WHERE id_canal_atencion = 19;
UPDATE canal_atencion SET id_finalidad_canal = 2, orden = 2, detalle = 'Para reportar daños de energía',
       fuente = 'https://www.celsia.com/es/zona-clientes/', verificado_en = DATE '2026-11-12'
 WHERE id_canal_atencion = 21;
UPDATE canal_atencion SET id_finalidad_canal = 6, orden = 3, detalle = 'Línea nacional gratuita',
       fuente = 'https://www.celsia.com/es/zona-clientes/', verificado_en = DATE '2026-11-12'
 WHERE id_canal_atencion = 20;

-- Veolia: su correo no respondió; el WhatsApp de Veolia Colombia atiende Tuluá.
UPDATE canal_atencion SET detalle = 'No coincide con el sitio oficial (pqr.tulua.co@veolia.com)'
 WHERE id_canal_atencion = 26;
INSERT INTO canal_atencion (id_tipo_canal, acceso, detalle, id_entidad_destino, activo, id_finalidad_canal, orden, fuente, verificado_en) VALUES
 (1, 'pqr.tulua.co@veolia.com', 'Correo del sitio oficial; no respondió en 38 días', 5, FALSE, 1, 100,
  'https://www.valle.veolia.co/servicio-cliente/oficinas-de-atencion-al-cliente/tulua', DATE '2026-11-12');
UPDATE canal_atencion SET activo = TRUE, id_finalidad_canal = 1, orden = 1,
       detalle = 'Elija «Dto Palmira Buga Tuluá» → «Residuos» → «Aseo Valle»',
       fuente = 'Verificado por Andrés con capturas del chat (12 nov 2026)', verificado_en = DATE '2026-11-12'
 WHERE id_canal_atencion = 24;
UPDATE canal_atencion SET id_finalidad_canal = 6, orden = 2, horario = 'Lunes a viernes, 8:00 a. m. a 4:00 p. m.',
       detalle = 'Línea nacional gratuita',
       fuente = 'https://www.valle.veolia.co/servicio-cliente/oficinas-de-atencion-al-cliente/tulua', verificado_en = DATE '2026-11-12'
 WHERE id_canal_atencion = 23;
UPDATE canal_atencion SET id_finalidad_canal = 1, orden = 4, horario = 'Lunes a viernes, 8:00 a. m. a 4:00 p. m.',
       detalle = 'Oficina de atención al cliente en Tuluá',
       fuente = 'https://www.valle.veolia.co/servicio-cliente/oficinas-de-atencion-al-cliente/tulua', verificado_en = DATE '2026-11-12'
 WHERE id_canal_atencion = 25;
UPDATE canal_atencion SET id_finalidad_canal = 6, orden = 5,
       fuente = 'https://www.valle.veolia.co/servicio-cliente/oficinas-de-atencion-al-cliente/tulua', verificado_en = DATE '2026-11-12'
 WHERE id_entidad_destino = 5 AND acceso = 'https://www.valle.veolia.co/servicio-cliente/oficinas-de-atencion-al-cliente/tulua';
INSERT INTO canal_atencion (id_tipo_canal, acceso, detalle, id_entidad_destino, id_finalidad_canal, orden, fuente, verificado_en) VALUES
 (2, 'https://www.valle.veolia.co/contactanos', 'Formulario «Contáctanos»', 5, 1, 3,
  'https://www.valle.veolia.co/contactanos', DATE '2026-11-12');
