-- El polígono de cada comuna guardado también en geography, con su índice. fn_asignar_comuna medía
-- la distancia en metros convirtiendo las 10 comunas a geography en cada llamada (~7 ms por punto en
-- un hueco o fuera de la capa). Las reglas no cambian.

ALTER TABLE comuna
  ADD COLUMN geog geography(MultiPolygon, 4326) GENERATED ALWAYS AS (geom::geography) STORED;

CREATE INDEX ix_comuna_geog ON comuna USING GIST (geog);

CREATE OR REPLACE FUNCTION fn_asignar_comuna(p_punto geometry, p_max_m double precision DEFAULT NULL)
RETURNS TABLE (id_comuna smallint, regla text, distancia_m double precision)
LANGUAGE sql STABLE AS $$
  WITH contiene AS (
    SELECT c.id_comuna
      FROM comuna c
     WHERE c.geom IS NOT NULL AND ST_Covers(c.geom, p_punto)
  ), cercana AS (
    SELECT c.id_comuna, ST_Distance(c.geog, p_punto::geography) AS d
      FROM comuna c
     WHERE c.geog IS NOT NULL
       AND ST_DWithin(c.geog, p_punto::geography,
                      COALESCE(p_max_m,
                               (SELECT valor::double precision FROM configuracion
                                 WHERE clave = 'comuna_distancia_maxima_m'),
                               0))
     ORDER BY d, c.id_comuna
     LIMIT 1
  )
  SELECT (SELECT min(x.id_comuna) FROM contiene x)::smallint,
         CASE WHEN (SELECT count(*) FROM contiene) > 1 THEN 'solape' ELSE 'contiene' END,
         0::double precision
   WHERE EXISTS (SELECT 1 FROM contiene)
  UNION ALL
  SELECT y.id_comuna, 'cercana', y.d
    FROM cercana y
   WHERE NOT EXISTS (SELECT 1 FROM contiene)
$$;
