-- =====================================================================
-- NotiUrba · Modelo de datos v5 · V1: esquema
-- PostgreSQL 16 + PostGIS 3.4. Se ejecuta en una base nueva.
-- Convenciones: id_<tabla> en PK y FK; nombres fragmentados; catálogos en tablas;
-- longitudes generosas (servicio público) y reglas estrictas en CHECK/triggers.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS postgis;

-- ---------------------------------------------------------------------
-- Catálogos
-- ---------------------------------------------------------------------
CREATE TABLE rol (
  id_rol SMALLSERIAL PRIMARY KEY,
  nombre VARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE tipo_documento (
  id_tipo_documento SMALLSERIAL PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL UNIQUE,
  codigo VARCHAR(10)  NOT NULL UNIQUE
);
CREATE TABLE aviso_privacidad (
  id_aviso_privacidad SMALLSERIAL PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL UNIQUE,
  version VARCHAR(10) NOT NULL UNIQUE,
  texto TEXT NOT NULL,
  vigente_desde TIMESTAMPTZ NOT NULL
);
CREATE TABLE tipo_token (
  id_tipo_token SMALLSERIAL PRIMARY KEY,
  nombre VARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE tipo_entidad (
  id_tipo_entidad SMALLSERIAL PRIMARY KEY,
  nombre VARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE tipo_dependencia (
  id_tipo_dependencia SMALLSERIAL PRIMARY KEY,
  nombre VARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE tipo_canal (
  id_tipo_canal SMALLSERIAL PRIMARY KEY,
  nombre VARCHAR(30) NOT NULL UNIQUE
);
CREATE TABLE estado_reporte (
  id_estado_reporte SMALLSERIAL PRIMARY KEY,
  nombre VARCHAR(40) NOT NULL UNIQUE,
  orden SMALLINT NOT NULL UNIQUE
);
CREATE TABLE modo_envio (
  id_modo_envio SMALLSERIAL PRIMARY KEY,
  nombre VARCHAR(20) NOT NULL UNIQUE
);
CREATE TABLE estado_envio (
  id_estado_envio SMALLSERIAL PRIMARY KEY,
  nombre VARCHAR(20) NOT NULL UNIQUE
);

-- ---------------------------------------------------------------------
-- Territorio
-- ---------------------------------------------------------------------
CREATE TABLE comuna (
  id_comuna SMALLINT PRIMARY KEY,
  geom geometry(MultiPolygon, 4326),            -- NULL hasta cargar límites oficiales
  CONSTRAINT ck_comuna_rango CHECK (id_comuna BETWEEN 1 AND 10)
);
CREATE TABLE barrio (
  id_barrio SERIAL PRIMARY KEY,
  id_comuna SMALLINT NOT NULL REFERENCES comuna(id_comuna) ON DELETE RESTRICT,
  nombre VARCHAR(100) NOT NULL,
  CONSTRAINT uq_barrio_comuna_nombre UNIQUE (id_comuna, nombre)
);

-- ---------------------------------------------------------------------
-- Usuarios
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
  id_usuario BIGSERIAL PRIMARY KEY,
  primer_nombre VARCHAR(100) NOT NULL,
  segundo_nombre VARCHAR(100),
  primer_apellido VARCHAR(100) NOT NULL,
  segundo_apellido VARCHAR(100),
  id_tipo_documento SMALLINT NOT NULL REFERENCES tipo_documento(id_tipo_documento) ON DELETE RESTRICT,
  numero_documento VARCHAR(30) NOT NULL,
  id_barrio INT NOT NULL REFERENCES barrio(id_barrio) ON DELETE RESTRICT,
  direccion_correspondencia VARCHAR(300) NOT NULL,
  correo VARCHAR(254) NOT NULL,
  correo_verificado_en TIMESTAMPTZ,
  telefono VARCHAR(30),
  password_hash VARCHAR(255) NOT NULL,
  id_rol SMALLINT NOT NULL REFERENCES rol(id_rol) ON DELETE RESTRICT,
  id_aviso_privacidad SMALLINT NOT NULL REFERENCES aviso_privacidad(id_aviso_privacidad) ON DELETE RESTRICT,
  acepto_privacidad_en TIMESTAMPTZ NOT NULL,
  creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_usuario_documento UNIQUE (id_tipo_documento, numero_documento),
  CONSTRAINT ck_usuario_fechas CHECK (correo_verificado_en IS NULL OR correo_verificado_en >= creado_en)
);
CREATE UNIQUE INDEX uq_usuario_correo ON usuario (lower(correo));

CREATE TABLE token_verificacion (
  id_token_verificacion BIGSERIAL PRIMARY KEY,
  id_usuario BIGINT NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,
  id_tipo_token SMALLINT NOT NULL REFERENCES tipo_token(id_tipo_token) ON DELETE RESTRICT,
  token_hash VARCHAR(255) NOT NULL,
  expira_en TIMESTAMPTZ NOT NULL,
  usado_en TIMESTAMPTZ,
  creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_token_hash UNIQUE (token_hash),
  CONSTRAINT ck_token_fechas CHECK (
    expira_en > creado_en AND (usado_en IS NULL OR (usado_en >= creado_en AND usado_en <= expira_en)))
);

CREATE TABLE configuracion (
  clave VARCHAR(100) PRIMARY KEY,
  valor VARCHAR(500) NOT NULL,
  id_usuario_actualiza BIGINT REFERENCES usuario(id_usuario) ON DELETE SET NULL,
  actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_config_vigencia CHECK (clave <> 'vigencia_dias' OR valor ~ '^[1-9][0-9]*$')
);

-- ---------------------------------------------------------------------
-- Destinos: entidad → dependencia → canal; categoría → dependencia
-- ---------------------------------------------------------------------
CREATE TABLE entidad_destino (
  id_entidad_destino SERIAL PRIMARY KEY,
  nombre VARCHAR(200) NOT NULL UNIQUE,
  id_tipo_entidad SMALLINT NOT NULL REFERENCES tipo_entidad(id_tipo_entidad) ON DELETE RESTRICT,
  activa BOOLEAN NOT NULL DEFAULT TRUE,
  id_usuario_actualiza BIGINT REFERENCES usuario(id_usuario) ON DELETE SET NULL,
  actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE dependencia (
  id_dependencia SERIAL PRIMARY KEY,
  id_entidad_destino INT NOT NULL REFERENCES entidad_destino(id_entidad_destino) ON DELETE RESTRICT,
  id_dependencia_padre INT REFERENCES dependencia(id_dependencia) ON DELETE RESTRICT,
  id_tipo_dependencia SMALLINT NOT NULL REFERENCES tipo_dependencia(id_tipo_dependencia) ON DELETE RESTRICT,
  nombre VARCHAR(200) NOT NULL,
  activa BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uq_dependencia_entidad_nombre UNIQUE (id_entidad_destino, nombre),
  CONSTRAINT ck_dependencia_padre CHECK (id_dependencia_padre IS NULL OR id_dependencia_padre <> id_dependencia)
);
CREATE TABLE canal_atencion (
  id_canal_atencion SERIAL PRIMARY KEY,
  id_tipo_canal SMALLINT NOT NULL REFERENCES tipo_canal(id_tipo_canal) ON DELETE RESTRICT,
  acceso VARCHAR(500) NOT NULL,
  detalle VARCHAR(500),
  id_entidad_destino INT REFERENCES entidad_destino(id_entidad_destino) ON DELETE RESTRICT,
  id_dependencia INT REFERENCES dependencia(id_dependencia) ON DELETE RESTRICT,
  es_principal BOOLEAN NOT NULL DEFAULT FALSE,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT ck_canal_un_destino CHECK ((id_entidad_destino IS NULL) <> (id_dependencia IS NULL)),
  CONSTRAINT ck_canal_principal_entidad CHECK (NOT es_principal OR (id_entidad_destino IS NOT NULL AND activo))
);
CREATE UNIQUE INDEX uq_canal_principal
  ON canal_atencion (id_entidad_destino, id_tipo_canal) WHERE es_principal;

CREATE TABLE categoria (
  id_categoria SERIAL PRIMARY KEY,
  nombre VARCHAR(200) NOT NULL UNIQUE,
  descripcion VARCHAR(300),
  id_dependencia INT NOT NULL REFERENCES dependencia(id_dependencia) ON DELETE RESTRICT,
  requiere_ubicacion BOOLEAN NOT NULL DEFAULT FALSE,
  requiere_evidencia BOOLEAN NOT NULL DEFAULT FALSE,
  activa BOOLEAN NOT NULL DEFAULT TRUE,
  id_usuario_actualiza BIGINT REFERENCES usuario(id_usuario) ON DELETE SET NULL,
  actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- Reportes
-- ---------------------------------------------------------------------
CREATE TABLE reporte (
  id_reporte BIGSERIAL PRIMARY KEY,
  codigo_seguimiento VARCHAR(20) NOT NULL,                 -- 'NU-' + id con 6 dígitos (trigger)
  id_usuario BIGINT NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,
  id_categoria INT NOT NULL REFERENCES categoria(id_categoria) ON DELETE RESTRICT,
  id_barrio INT REFERENCES barrio(id_barrio) ON DELETE RESTRICT,
  id_estado_reporte SMALLINT NOT NULL REFERENCES estado_reporte(id_estado_reporte) ON DELETE RESTRICT,
  descripcion VARCHAR(500) NOT NULL,
  ubicacion geometry(Point, 4326),
  direccion_incidente VARCHAR(300),
  matricula_cuenta_servicio VARCHAR(50),                   -- solo categorías de empresa
  vigente_hasta TIMESTAMPTZ NOT NULL,                      -- creado_en + vigencia_dias (trigger)
  autorizacion_envio_en TIMESTAMPTZ NOT NULL,
  requiere_revision BOOLEAN NOT NULL DEFAULT FALSE,
  creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_reporte_codigo UNIQUE (codigo_seguimiento),
  CONSTRAINT ck_reporte_descripcion CHECK (char_length(descripcion) BETWEEN 10 AND 500),
  CONSTRAINT ck_reporte_fechas CHECK (vigente_hasta > creado_en AND autorizacion_envio_en <= creado_en)
);
CREATE TABLE evidencia_reporte (
  id_evidencia BIGSERIAL PRIMARY KEY,
  id_reporte BIGINT NOT NULL REFERENCES reporte(id_reporte) ON DELETE CASCADE,
  id_usuario BIGINT NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,   -- quien subió la foto
  url VARCHAR(2048) NOT NULL,
  orden SMALLINT NOT NULL,
  CONSTRAINT uq_evidencia_orden UNIQUE (id_reporte, id_usuario, orden),
  CONSTRAINT ck_evidencia_orden CHECK (orden BETWEEN 1 AND 3)
);
CREATE TABLE apoyo_reporte (
  id_reporte BIGINT NOT NULL REFERENCES reporte(id_reporte) ON DELETE CASCADE,
  id_usuario BIGINT NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,
  creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT pk_apoyo PRIMARY KEY (id_reporte, id_usuario)
);
CREATE TABLE historial_estado (
  id_historial_estado BIGSERIAL PRIMARY KEY,
  id_reporte BIGINT NOT NULL REFERENCES reporte(id_reporte) ON DELETE CASCADE,
  id_estado_reporte SMALLINT NOT NULL REFERENCES estado_reporte(id_estado_reporte) ON DELETE RESTRICT,
  observacion VARCHAR(1000),
  id_usuario_cambia BIGINT REFERENCES usuario(id_usuario) ON DELETE SET NULL,
  cambiado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE registro_envio_correo (
  id_registro_envio BIGSERIAL PRIMARY KEY,
  id_reporte BIGINT NOT NULL REFERENCES reporte(id_reporte) ON DELETE CASCADE,
  id_canal_atencion INT NOT NULL REFERENCES canal_atencion(id_canal_atencion) ON DELETE RESTRICT,
  id_modo_envio SMALLINT NOT NULL REFERENCES modo_envio(id_modo_envio) ON DELETE RESTRICT,
  id_estado_envio SMALLINT NOT NULL REFERENCES estado_envio(id_estado_envio) ON DELETE RESTRICT,
  error VARCHAR(1000),
  enviado_en TIMESTAMPTZ,
  creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_envio_fechas CHECK (enviado_en IS NULL OR enviado_en >= creado_en)
);

-- ---------------------------------------------------------------------
-- Índices
-- ---------------------------------------------------------------------
CREATE INDEX ix_reporte_ubicacion  ON reporte USING GIST (ubicacion);
CREATE INDEX ix_comuna_geom        ON comuna  USING GIST (geom);
CREATE INDEX ix_reporte_usuario    ON reporte (id_usuario);
CREATE INDEX ix_reporte_categoria  ON reporte (id_categoria);
CREATE INDEX ix_reporte_barrio     ON reporte (id_barrio);
CREATE INDEX ix_reporte_estado     ON reporte (id_estado_reporte);
CREATE INDEX ix_reporte_vigencia   ON reporte (vigente_hasta);
CREATE INDEX ix_barrio_comuna      ON barrio (id_comuna);
CREATE INDEX ix_usuario_barrio     ON usuario (id_barrio);
CREATE INDEX ix_token_usuario      ON token_verificacion (id_usuario);
CREATE INDEX ix_dependencia_entidad ON dependencia (id_entidad_destino);
CREATE INDEX ix_dependencia_padre  ON dependencia (id_dependencia_padre);
CREATE INDEX ix_canal_entidad      ON canal_atencion (id_entidad_destino);
CREATE INDEX ix_canal_dependencia  ON canal_atencion (id_dependencia);
CREATE INDEX ix_categoria_dependencia ON categoria (id_dependencia);
CREATE INDEX ix_evidencia_usuario  ON evidencia_reporte (id_usuario);
CREATE INDEX ix_apoyo_usuario      ON apoyo_reporte (id_usuario);
CREATE INDEX ix_historial_reporte  ON historial_estado (id_reporte, cambiado_en DESC, id_historial_estado DESC);
CREATE INDEX ix_envio_reporte      ON registro_envio_correo (id_reporte);
CREATE INDEX ix_envio_canal        ON registro_envio_correo (id_canal_atencion);
