-- =====================================================================
-- NotiUrba · v5 · V2: reglas que la estructura no puede expresar (B1–B12) y vistas
-- Referencia: hoja "Reglas v5" del Google Sheet.
-- =====================================================================

-- B1 · La dependencia hija es de la misma entidad que su padre y no hay ciclos
CREATE FUNCTION fn_dependencia_valida() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
  entidad_padre INT;
BEGIN
  IF NEW.id_dependencia_padre IS NOT NULL THEN
    SELECT id_entidad_destino INTO entidad_padre FROM dependencia WHERE id_dependencia = NEW.id_dependencia_padre;
    IF entidad_padre IS DISTINCT FROM NEW.id_entidad_destino THEN
      RAISE EXCEPTION 'La dependencia padre pertenece a otra entidad' USING ERRCODE = 'check_violation';
    END IF;
    IF EXISTS (
      WITH RECURSIVE ancestros AS (
        SELECT id_dependencia, id_dependencia_padre FROM dependencia WHERE id_dependencia = NEW.id_dependencia_padre
        UNION
        SELECT d.id_dependencia, d.id_dependencia_padre FROM dependencia d JOIN ancestros a ON d.id_dependencia = a.id_dependencia_padre
      ) SELECT 1 FROM ancestros WHERE id_dependencia = NEW.id_dependencia
    ) THEN
      RAISE EXCEPTION 'La jerarquía de dependencias no puede tener ciclos' USING ERRCODE = 'check_violation';
    END IF;
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER tg_dependencia_valida BEFORE INSERT OR UPDATE ON dependencia
  FOR EACH ROW EXECUTE FUNCTION fn_dependencia_valida();

-- B7 · El acceso (y el tipo) de un canal no se editan: se desactiva y se crea otro
CREATE FUNCTION fn_canal_inmutable() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF NEW.acceso IS DISTINCT FROM OLD.acceso OR NEW.id_tipo_canal IS DISTINCT FROM OLD.id_tipo_canal THEN
    RAISE EXCEPTION 'No se edita el acceso ni el tipo de un canal: desactívelo (activo = FALSE) y cree otro'
      USING ERRCODE = 'check_violation';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER tg_canal_inmutable BEFORE UPDATE ON canal_atencion
  FOR EACH ROW EXECUTE FUNCTION fn_canal_inmutable();

-- B3 · B6 · B9 · código de seguimiento y estado inicial (al insertar un reporte)
CREATE FUNCTION fn_reporte_antes_insertar() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
  exige_ubicacion BOOLEAN;
  dias INT;
BEGIN
  -- B9: vigencia congelada al crear
  SELECT valor::int INTO dias FROM configuracion WHERE clave = 'vigencia_dias';
  IF dias IS NULL THEN
    RAISE EXCEPTION 'Falta configuracion.vigencia_dias' USING ERRCODE = 'check_violation';
  END IF;
  NEW.vigente_hasta := NEW.creado_en + make_interval(days => dias);

  -- Código de seguimiento: NU-000001 (se amplía si supera 6 dígitos)
  IF NEW.codigo_seguimiento IS NULL THEN
    NEW.codigo_seguimiento := 'NU-' || lpad(NEW.id_reporte::text, greatest(6, length(NEW.id_reporte::text)), '0');
  END IF;

  -- Estado inicial: el de menor orden (Recibido)
  IF NEW.id_estado_reporte IS NULL THEN
    SELECT id_estado_reporte INTO NEW.id_estado_reporte FROM estado_reporte ORDER BY orden LIMIT 1;
  END IF;

  -- B3: categorías que exigen ubicación
  SELECT requiere_ubicacion INTO exige_ubicacion FROM categoria WHERE id_categoria = NEW.id_categoria;
  IF exige_ubicacion AND NEW.ubicacion IS NULL THEN
    RAISE EXCEPTION 'La categoría exige ubicación' USING ERRCODE = 'check_violation';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER tg_reporte_antes_insertar BEFORE INSERT ON reporte
  FOR EACH ROW EXECUTE FUNCTION fn_reporte_antes_insertar();

-- B6 · La matrícula solo aplica a categorías de empresas de servicios públicos
CREATE FUNCTION fn_reporte_matricula() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF NEW.matricula_cuenta_servicio IS NOT NULL AND NOT EXISTS (
    SELECT 1
    FROM categoria c
    JOIN dependencia d ON d.id_dependencia = c.id_dependencia
    JOIN entidad_destino e ON e.id_entidad_destino = d.id_entidad_destino
    JOIN tipo_entidad t ON t.id_tipo_entidad = e.id_tipo_entidad
    WHERE c.id_categoria = NEW.id_categoria AND t.nombre = 'Empresa de servicios públicos'
  ) THEN
    RAISE EXCEPTION 'La matrícula solo aplica a categorías de empresas de servicios públicos'
      USING ERRCODE = 'check_violation';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER tg_reporte_matricula BEFORE INSERT OR UPDATE OF matricula_cuenta_servicio, id_categoria ON reporte
  FOR EACH ROW EXECUTE FUNCTION fn_reporte_matricula();

-- Al crear el reporte se registra el primer estado en el historial
CREATE FUNCTION fn_reporte_despues_insertar() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  INSERT INTO historial_estado (id_reporte, id_estado_reporte, observacion, cambiado_en)
  VALUES (NEW.id_reporte, NEW.id_estado_reporte, 'Reporte registrado por el ciudadano.', NEW.creado_en);
  RETURN NULL;
END $$;
CREATE TRIGGER tg_reporte_despues_insertar AFTER INSERT ON reporte
  FOR EACH ROW EXECUTE FUNCTION fn_reporte_despues_insertar();

-- B11 · El historial solo se inserta y no retrocede
CREATE FUNCTION fn_historial_guardia() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
  orden_actual SMALLINT;
  orden_nuevo SMALLINT;
BEGIN
  IF TG_OP = 'UPDATE' THEN
    -- Las acciones referenciales (ON DELETE SET NULL del usuario) corren un nivel más profundo y se permiten
    IF pg_trigger_depth() = 1 THEN
      RAISE EXCEPTION 'El historial de estados no se modifica' USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
  ELSIF TG_OP = 'DELETE' THEN
    -- Los borrados en cascada (reporte / habeas data) también corren más profundo y se permiten
    IF pg_trigger_depth() = 1 THEN
      RAISE EXCEPTION 'El historial de estados no se borra directamente' USING ERRCODE = 'check_violation';
    END IF;
    RETURN OLD;
  END IF;
  SELECT e.orden INTO orden_actual FROM reporte r JOIN estado_reporte e USING (id_estado_reporte)
   WHERE r.id_reporte = NEW.id_reporte;
  SELECT orden INTO orden_nuevo FROM estado_reporte WHERE id_estado_reporte = NEW.id_estado_reporte;
  IF orden_nuevo < orden_actual THEN
    RAISE EXCEPTION 'El estado de un reporte no retrocede' USING ERRCODE = 'check_violation';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER tg_historial_guardia BEFORE INSERT OR UPDATE OR DELETE ON historial_estado
  FOR EACH ROW EXECUTE FUNCTION fn_historial_guardia();

-- B4 · reporte.id_estado_reporte = último registro del historial
CREATE FUNCTION fn_historial_sincroniza() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  UPDATE reporte r
     SET id_estado_reporte = (SELECT h.id_estado_reporte FROM historial_estado h
                               WHERE h.id_reporte = NEW.id_reporte
                               ORDER BY h.cambiado_en DESC, h.id_historial_estado DESC LIMIT 1)
   WHERE r.id_reporte = NEW.id_reporte;
  RETURN NULL;
END $$;
CREATE TRIGGER tg_historial_sincroniza AFTER INSERT ON historial_estado
  FOR EACH ROW EXECUTE FUNCTION fn_historial_sincroniza();

-- B2 · Solo suben evidencia quien reportó o quien apoyó
CREATE FUNCTION fn_evidencia_autor() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM reporte WHERE id_reporte = NEW.id_reporte AND id_usuario = NEW.id_usuario)
     AND NOT EXISTS (SELECT 1 FROM apoyo_reporte WHERE id_reporte = NEW.id_reporte AND id_usuario = NEW.id_usuario) THEN
    RAISE EXCEPTION 'Solo quien reportó o apoyó el reporte puede subir evidencia' USING ERRCODE = 'check_violation';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER tg_evidencia_autor BEFORE INSERT ON evidencia_reporte
  FOR EACH ROW EXECUTE FUNCTION fn_evidencia_autor();

-- B12 · El autor no apoya su reporte; el reporte debe estar vigente al apoyar
CREATE FUNCTION fn_apoyo_valido() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
  autor BIGINT;
  vigencia TIMESTAMPTZ;
BEGIN
  SELECT id_usuario, vigente_hasta INTO autor, vigencia FROM reporte WHERE id_reporte = NEW.id_reporte;
  IF autor = NEW.id_usuario THEN
    RAISE EXCEPTION 'El autor no puede apoyar su propio reporte' USING ERRCODE = 'check_violation';
  END IF;
  IF NEW.creado_en >= vigencia THEN
    RAISE EXCEPTION 'El reporte ya no está vigente' USING ERRCODE = 'check_violation';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER tg_apoyo_valido BEFORE INSERT ON apoyo_reporte
  FOR EACH ROW EXECUTE FUNCTION fn_apoyo_valido();

-- B5 · B10 · Registro de envío: solo el Correo principal activo de la entidad del reporte,
--            y coherencia entre estado y campos
CREATE FUNCTION fn_envio_valido() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
  estado TEXT;
BEGIN
  IF TG_OP = 'INSERT' OR NEW.id_canal_atencion IS DISTINCT FROM OLD.id_canal_atencion
     OR NEW.id_reporte IS DISTINCT FROM OLD.id_reporte THEN
    IF NOT EXISTS (
      SELECT 1
      FROM reporte r
      JOIN categoria c ON c.id_categoria = r.id_categoria
      JOIN dependencia d ON d.id_dependencia = c.id_dependencia
      JOIN canal_atencion k ON k.id_entidad_destino = d.id_entidad_destino
      JOIN tipo_canal t ON t.id_tipo_canal = k.id_tipo_canal
      WHERE r.id_reporte = NEW.id_reporte AND k.id_canal_atencion = NEW.id_canal_atencion
        AND t.nombre = 'Correo' AND k.es_principal AND k.activo
    ) THEN
      RAISE EXCEPTION 'El canal debe ser el Correo principal activo de la entidad destino del reporte'
        USING ERRCODE = 'check_violation';
    END IF;
  END IF;

  SELECT nombre INTO estado FROM estado_envio WHERE id_estado_envio = NEW.id_estado_envio;
  IF estado = 'ENVIADO' AND NEW.enviado_en IS NULL THEN
    RAISE EXCEPTION 'Un envío ENVIADO requiere enviado_en' USING ERRCODE = 'check_violation';
  ELSIF estado = 'FALLIDO' AND NEW.error IS NULL THEN
    RAISE EXCEPTION 'Un envío FALLIDO requiere el error' USING ERRCODE = 'check_violation';
  ELSIF estado = 'PENDIENTE' AND (NEW.enviado_en IS NOT NULL OR NEW.error IS NOT NULL) THEN
    RAISE EXCEPTION 'Un envío PENDIENTE no tiene enviado_en ni error' USING ERRCODE = 'check_violation';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER tg_envio_valido BEFORE INSERT OR UPDATE ON registro_envio_correo
  FOR EACH ROW EXECUTE FUNCTION fn_envio_valido();

-- ---------------------------------------------------------------------
-- Vistas
-- ---------------------------------------------------------------------

-- Destino de cada reporte: categoría → dependencia → entidad, con su Correo principal activo (si hay)
CREATE VIEW v_reporte_destino AS
SELECT r.id_reporte, r.codigo_seguimiento, c.id_categoria, d.id_dependencia,
       e.id_entidad_destino, e.nombre AS entidad,
       k.id_canal_atencion AS id_canal_correo, k.acceso AS correo_destino
FROM reporte r
JOIN categoria c ON c.id_categoria = r.id_categoria
JOIN dependencia d ON d.id_dependencia = c.id_dependencia
JOIN entidad_destino e ON e.id_entidad_destino = d.id_entidad_destino
LEFT JOIN canal_atencion k
       ON k.id_entidad_destino = e.id_entidad_destino AND k.es_principal AND k.activo
      AND k.id_tipo_canal = (SELECT id_tipo_canal FROM tipo_canal WHERE nombre = 'Correo');

-- N2 · Reportes que no se pueden enviar por correo (la entidad no tiene Correo principal activo)
CREATE VIEW v_reporte_no_enviado_por_correo AS
SELECT id_reporte, codigo_seguimiento, id_entidad_destino, entidad
FROM v_reporte_destino WHERE correo_destino IS NULL;

-- N3 · Comuna de cada reporte: por el polígono que contiene el punto o, si no, por el barrio
CREATE VIEW v_reporte_comuna AS
SELECT r.id_reporte, r.codigo_seguimiento,
       COALESCE(cp.id_comuna, b.id_comuna) AS id_comuna,
       CASE WHEN cp.id_comuna IS NOT NULL THEN 'poligono'
            WHEN b.id_comuna IS NOT NULL THEN 'barrio' END AS origen
FROM reporte r
LEFT JOIN barrio b ON b.id_barrio = r.id_barrio
LEFT JOIN LATERAL (
  SELECT c.id_comuna FROM comuna c WHERE r.ubicacion IS NOT NULL AND ST_Covers(c.geom, r.ubicacion) LIMIT 1
) cp ON TRUE;

-- Reportes visibles en el mapa público: con ubicación y vigentes
CREATE VIEW v_reporte_mapa AS
SELECT r.id_reporte, r.codigo_seguimiento, r.id_categoria, r.id_estado_reporte,
       r.ubicacion, r.creado_en, r.vigente_hasta,
       vc.id_comuna,
       (SELECT count(*) FROM apoyo_reporte a WHERE a.id_reporte = r.id_reporte) AS apoyos
FROM reporte r
JOIN v_reporte_comuna vc USING (id_reporte)
WHERE r.ubicacion IS NOT NULL AND r.vigente_hasta > now();
