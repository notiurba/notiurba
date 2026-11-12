-- La comuna del reporte se guarda al crearlo en vez de calcularse en cada lectura. La regla no
-- cambia (fn_asignar_comuna y, sin polígono, el barrio) y vive en un solo trigger: todo recálculo
-- reasigna la ubicación para pasar por él. Se recalcula si cambian el punto o el barrio del reporte,
-- el polígono de una comuna, la comuna de un barrio o la distancia máxima a una comuna.

ALTER TABLE reporte
  ADD COLUMN id_comuna SMALLINT REFERENCES comuna(id_comuna),
  ADD COLUMN comuna_origen TEXT,
  ADD CONSTRAINT ck_reporte_comuna_origen CHECK (comuna_origen IN ('poligono', 'barrio')),
  ADD CONSTRAINT ck_reporte_comuna_con_origen CHECK ((id_comuna IS NULL) = (comuna_origen IS NULL));

CREATE FUNCTION fn_reporte_guardar_comuna() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  NEW.id_comuna := NULL;
  NEW.comuna_origen := NULL;
  IF NEW.ubicacion IS NOT NULL THEN
    SELECT a.id_comuna INTO NEW.id_comuna FROM fn_asignar_comuna(NEW.ubicacion) a;
  END IF;
  IF NEW.id_comuna IS NOT NULL THEN
    NEW.comuna_origen := 'poligono';
  ELSIF NEW.id_barrio IS NOT NULL THEN
    SELECT b.id_comuna INTO NEW.id_comuna FROM barrio b WHERE b.id_barrio = NEW.id_barrio;
    IF NEW.id_comuna IS NOT NULL THEN
      NEW.comuna_origen := 'barrio';
    END IF;
  END IF;
  RETURN NEW;
END $$;

CREATE TRIGGER tg_reporte_comuna BEFORE INSERT OR UPDATE OF ubicacion, id_barrio ON reporte
  FOR EACH ROW EXECUTE FUNCTION fn_reporte_guardar_comuna();

-- La capa o la distancia máxima cambiaron: se recalculan los reportes con punto
CREATE FUNCTION fn_recalcular_comuna_por_punto() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  UPDATE reporte SET ubicacion = ubicacion WHERE ubicacion IS NOT NULL;
  RETURN NULL;
END $$;

CREATE TRIGGER tg_comuna_recalcular AFTER INSERT OR UPDATE OF geom ON comuna
  FOR EACH STATEMENT EXECUTE FUNCTION fn_recalcular_comuna_por_punto();

CREATE TRIGGER tg_distancia_comuna_recalcular AFTER UPDATE ON configuracion
  FOR EACH ROW WHEN (NEW.clave = 'comuna_distancia_maxima_m' AND NEW.valor IS DISTINCT FROM OLD.valor)
  EXECUTE FUNCTION fn_recalcular_comuna_por_punto();

-- Un barrio cambió de comuna: solo cambian sus reportes cuya comuna salía del barrio
CREATE FUNCTION fn_recalcular_comuna_por_barrio() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  UPDATE reporte SET id_barrio = id_barrio
   WHERE id_barrio = NEW.id_barrio AND comuna_origen IS DISTINCT FROM 'poligono';
  RETURN NULL;
END $$;

CREATE TRIGGER tg_barrio_recalcular AFTER UPDATE OF id_comuna ON barrio
  FOR EACH ROW WHEN (NEW.id_comuna IS DISTINCT FROM OLD.id_comuna)
  EXECUTE FUNCTION fn_recalcular_comuna_por_barrio();

-- Reportes que ya existían
UPDATE reporte SET ubicacion = ubicacion;

CREATE INDEX ix_reporte_comuna ON reporte (id_comuna);

-- Mismas columnas que antes, ahora leídas del reporte
CREATE OR REPLACE VIEW v_reporte_comuna AS
SELECT r.id_reporte, r.codigo_seguimiento, r.id_comuna, r.comuna_origen AS origen
  FROM reporte r;
