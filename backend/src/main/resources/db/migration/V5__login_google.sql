-- =====================================================================
-- NotiUrba · V5: login con Google («Continuar con Google»)
-- Agrega el catálogo de proveedores de identidad y la identidad externa de
-- cada usuario (el `sub` de Google). La contraseña pasa a ser opcional:
-- un usuario puede entrar solo con Google, pero nunca sin ninguna credencial (B13).
-- =====================================================================

-- ---------------------------------------------------------------------
-- Catálogo
-- ---------------------------------------------------------------------
CREATE TABLE proveedor_identidad (
  id_proveedor_identidad SMALLSERIAL PRIMARY KEY,
  nombre VARCHAR(50) NOT NULL UNIQUE
);
INSERT INTO proveedor_identidad (id_proveedor_identidad, nombre) VALUES (1,'GOOGLE');
SELECT setval(pg_get_serial_sequence('proveedor_identidad','id_proveedor_identidad'),
              (SELECT max(id_proveedor_identidad) FROM proveedor_identidad));

-- ---------------------------------------------------------------------
-- Identidad externa: una por proveedor y usuario; el identificador es único por proveedor
-- ---------------------------------------------------------------------
CREATE TABLE identidad_externa (
  id_identidad_externa BIGSERIAL PRIMARY KEY,
  id_usuario BIGINT NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,   -- habeas data
  id_proveedor_identidad SMALLINT NOT NULL REFERENCES proveedor_identidad(id_proveedor_identidad) ON DELETE RESTRICT,
  identificador_externo VARCHAR(255) NOT NULL,                                   -- `sub` del ID token
  creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_identidad_proveedor_identificador UNIQUE (id_proveedor_identidad, identificador_externo),
  CONSTRAINT uq_identidad_usuario_proveedor UNIQUE (id_usuario, id_proveedor_identidad)
);

ALTER TABLE usuario ALTER COLUMN password_hash DROP NOT NULL;

-- B13 · Un usuario sin contraseña debe tener al menos una identidad externa.
-- Un CHECK no puede mirar otra tabla: se usa un CONSTRAINT TRIGGER diferido, que se
-- evalúa al confirmar la transacción (así se puede crear el usuario y luego su identidad).
-- Se revisa el estado real de la base, no NEW/OLD: si el usuario ya no existe
-- (borrado en cascada por habeas data), no hay nada que exigir.
CREATE FUNCTION fn_usuario_tiene_credencial() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
  usuario_revisar BIGINT;
BEGIN
  IF TG_TABLE_NAME = 'usuario' THEN
    usuario_revisar := NEW.id_usuario;
  ELSE
    usuario_revisar := OLD.id_usuario;
  END IF;
  IF EXISTS (SELECT 1 FROM usuario WHERE id_usuario = usuario_revisar AND password_hash IS NULL)
     AND NOT EXISTS (SELECT 1 FROM identidad_externa WHERE id_usuario = usuario_revisar) THEN
    RAISE EXCEPTION 'Un usuario sin contraseña debe tener al menos una identidad externa'
      USING ERRCODE = 'check_violation';
  END IF;
  RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER tg_usuario_tiene_credencial
  AFTER INSERT OR UPDATE OF password_hash ON usuario
  DEFERRABLE INITIALLY DEFERRED
  FOR EACH ROW EXECUTE FUNCTION fn_usuario_tiene_credencial();
CREATE CONSTRAINT TRIGGER tg_identidad_externa_credencial
  AFTER DELETE OR UPDATE OF id_usuario ON identidad_externa
  DEFERRABLE INITIALLY DEFERRED
  FOR EACH ROW EXECUTE FUNCTION fn_usuario_tiene_credencial();
