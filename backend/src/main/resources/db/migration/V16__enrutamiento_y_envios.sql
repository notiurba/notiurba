-- Reutiliza los 14 IDs de V3; la categoría histórica 15 se conserva sin ofrecerla en el nuevo flujo.
CREATE TABLE configuracion_enrutamiento (
 id_categoria INT PRIMARY KEY REFERENCES categoria(id_categoria),
 codigo VARCHAR(3) NOT NULL UNIQUE,
 id_canal_correo INT NOT NULL REFERENCES canal_atencion(id_canal_atencion),
 id_canal_alterno INT NOT NULL REFERENCES canal_atencion(id_canal_atencion),
 correo_confirmado BOOLEAN NOT NULL DEFAULT FALSE,
 prioriza_alterno BOOLEAN NOT NULL DEFAULT FALSE,
 competencia_confirmada BOOLEAN NOT NULL DEFAULT TRUE,
 revision_clasificacion BOOLEAN NOT NULL DEFAULT FALSE,
 admite_matricula BOOLEAN NOT NULL DEFAULT FALSE,
 es_gas BOOLEAN NOT NULL DEFAULT FALSE,
 fuente_validacion TEXT,
 validado_en DATE
);
ALTER TABLE configuracion_enrutamiento ADD COLUMN id_canal_emergencia INT REFERENCES canal_atencion(id_canal_atencion);
-- No se habilitan correos por su mera presencia en el catálogo.
INSERT INTO configuracion_enrutamiento
 (id_categoria,codigo,id_canal_correo,id_canal_alterno,correo_confirmado,prioriza_alterno,
 competencia_confirmada,revision_clasificacion,admite_matricula,es_gas,fuente_validacion,validado_en)
SELECT n, 'A'||lpad(n::text,2,'0'),1,2,TRUE,FALSE,n NOT IN (8,9),n=10,FALSE,FALSE,
 'https://tulua.gov.co/documentos/210/servicio-al-publico-normas-formularios-y-protocolos-de-atencion/', DATE '2026-11-06'
FROM generate_series(1,10) n;
INSERT INTO configuracion_enrutamiento
 (id_categoria,codigo,id_canal_correo,id_canal_alterno,correo_confirmado,prioriza_alterno,
 competencia_confirmada,revision_clasificacion,admite_matricula,es_gas,fuente_validacion,validado_en) VALUES
 (11,'E01',12,14,FALSE,FALSE,TRUE,FALSE,TRUE,FALSE,'https://www.centroaguas.com/index.php/contactenos',NULL),
 (12,'E02',18,16,TRUE,FALSE,TRUE,FALSE,FALSE,TRUE,'https://www.gdo.com.co/Paginas/nuestros-canales-de-comunicacion.aspx',DATE '2026-11-06'),
 (13,'E03',22,19,FALSE,TRUE,TRUE,FALSE,FALSE,FALSE,'https://www.celsia.com/es/zona-clientes/',DATE '2026-11-06'),
 (14,'E04',26,23,FALSE,FALSE,TRUE,FALSE,FALSE,FALSE,'https://www.valle.veolia.co/servicio-cliente/oficinas-de-atencion-al-cliente/tulua',NULL);
UPDATE configuracion_enrutamiento SET id_canal_emergencia=15 WHERE codigo='E02';
-- Veolia: usar la página institucional vigente; no presumir operativo el correo ni el teléfono histórico.
INSERT INTO canal_atencion(id_tipo_canal,acceso,detalle,id_entidad_destino,es_principal,activo)
SELECT (SELECT id_tipo_canal FROM tipo_canal WHERE nombre='Web'),
 'https://www.valle.veolia.co/servicio-cliente/oficinas-de-atencion-al-cliente/tulua',
 'Canales oficiales de atención en Tuluá',5,FALSE,TRUE
WHERE NOT EXISTS(SELECT 1 FROM canal_atencion WHERE id_entidad_destino=5
 AND acceso='https://www.valle.veolia.co/servicio-cliente/oficinas-de-atencion-al-cliente/tulua');
UPDATE configuracion_enrutamiento SET id_canal_alterno=(SELECT min(id_canal_atencion) FROM canal_atencion
 WHERE id_entidad_destino=5 AND acceso='https://www.valle.veolia.co/servicio-cliente/oficinas-de-atencion-al-cliente/tulua') WHERE codigo='E04';
-- Activar el canal documentado de gas no urgente, conservando su acceso y entidad.
UPDATE canal_atencion k SET activo=TRUE, es_principal=NOT EXISTS(
 SELECT 1 FROM canal_atencion otro WHERE otro.id_entidad_destino=k.id_entidad_destino
 AND otro.id_tipo_canal=k.id_tipo_canal AND otro.es_principal AND otro.id_canal_atencion<>k.id_canal_atencion)
WHERE k.id_canal_atencion=18;
UPDATE categoria c SET nombre=v.nombre FROM (VALUES
 (1,'Vías, huecos, andenes e infraestructura'),(2,'Alumbrado, espacio público y convivencia'),
 (3,'Basuras, árboles y afectaciones ambientales'),(4,'Señalización vial, semáforos y movilidad'),
 (5,'Riesgos sanitarios y salud pública'),(6,'Uso del suelo y licencias urbanas'),
 (7,'Infraestructura y servicios educativos'),(8,'Desastres, inundaciones y deslizamientos'),
 (9,'Vivienda'),(10,'Otro / no clasificado'),(11,'Acueducto y alcantarillado'),
 (12,'Gas natural'),(13,'Energía eléctrica'),(14,'Recolección de basuras y barrido')) v(id,nombre)
WHERE c.id_categoria=v.id;
INSERT INTO estado_envio(nombre) VALUES ('NO_ENVIADO_CORREO');
INSERT INTO modo_envio(nombre) VALUES ('EXTERNO');
ALTER TABLE reporte ADD COLUMN emergencia BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE registro_envio_correo
 ADD COLUMN destino VARCHAR(500),
 ADD COLUMN clave_idempotencia VARCHAR(100),
 ADD COLUMN reintentable BOOLEAN NOT NULL DEFAULT FALSE,
 ADD COLUMN rebote BOOLEAN NOT NULL DEFAULT FALSE,
 ADD COLUMN procesando BOOLEAN NOT NULL DEFAULT FALSE;
CREATE UNIQUE INDEX uq_envio_pendiente ON registro_envio_correo(id_reporte)
 WHERE id_estado_envio=1 AND clave_idempotencia IS NOT NULL;
-- La carga se congela antes de encolar para reutilizar exactamente el mismo mensaje en reintentos.
ALTER TABLE registro_envio_correo ADD COLUMN mensaje_json TEXT;
CREATE TABLE evento_correo (
 id_evento VARCHAR(200) PRIMARY KEY,
 id_envio_proveedor VARCHAR(100) NOT NULL,
 creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- B5/B10: conservar la regla de correo principal; permitir registrar canales alternos sin fingir envío.
CREATE OR REPLACE FUNCTION fn_envio_valido() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE estado TEXT; modo TEXT;
BEGIN
 SELECT nombre INTO estado FROM estado_envio WHERE id_estado_envio=NEW.id_estado_envio;
 SELECT nombre INTO modo FROM modo_envio WHERE id_modo_envio=NEW.id_modo_envio;
 IF TG_OP='INSERT' OR NEW.id_canal_atencion IS DISTINCT FROM OLD.id_canal_atencion
    OR NEW.id_reporte IS DISTINCT FROM OLD.id_reporte THEN
  IF NOT EXISTS (
   SELECT 1 FROM reporte r JOIN categoria c USING(id_categoria)
   JOIN dependencia d USING(id_dependencia)
   JOIN canal_atencion k ON k.id_entidad_destino=d.id_entidad_destino
   JOIN tipo_canal t USING(id_tipo_canal)
   WHERE r.id_reporte=NEW.id_reporte AND k.id_canal_atencion=NEW.id_canal_atencion AND k.activo
    AND ((modo='EXTERNO' AND estado='NO_ENVIADO_CORREO' AND t.nombre<>'Correo')
      OR (modo='RESEND' AND t.nombre='Correo' AND k.es_principal))) THEN
    RAISE EXCEPTION 'Canal no autorizado para la entidad del reporte' USING ERRCODE='check_violation';
  END IF;
 END IF;
 IF estado='ENVIADO' AND (NEW.enviado_en IS NULL OR NEW.id_envio_proveedor IS NULL) THEN
  RAISE EXCEPTION 'Enviado requiere aceptación del proveedor' USING ERRCODE='check_violation';
 ELSIF estado='FALLIDO' AND NEW.error IS NULL THEN
  RAISE EXCEPTION 'Fallido requiere motivo' USING ERRCODE='check_violation';
 ELSIF estado IN ('PENDIENTE','NO_ENVIADO_CORREO') AND (NEW.enviado_en IS NOT NULL OR NEW.error IS NOT NULL) THEN
  RAISE EXCEPTION 'Canal sin envío no admite éxito ni error' USING ERRCODE='check_violation';
 END IF;
 RETURN NEW;
END $$;
