-- NOT-55 (EDT 7.3): asignar la comuna de un punto.
--   1. El punto cae dentro de una comuna (ST_Covers cuenta el borde)      -> esa comuna.
--   2. Cae dentro de 2 o más (solape)                                     -> la de menor número.
--   3. Cae en un hueco: la comuna más cercana a una distancia en metros
--      menor o igual a la máxima (configuracion.comuna_distancia_maxima_m) -> esa comuna.
--   4. Más lejos que la máxima de toda comuna                             -> ninguna (fuera de cobertura, RN-03).
-- Las comunas sin polígono (comuna.geom NULL) no participan.
--
-- La comuna no se guarda en el reporte: se deriva (v_reporte_comuna). Por eso la misma función
-- sirve para rechazar al crear el reporte y para leer la comuna después, sin que puedan discrepar.

-- 50 m: margen razonable de error de un GPS de celular en zona urbana. Es un valor propuesto;
-- se cambia con UPDATE configuracion, sin tocar el código. 0 desactiva la regla del hueco.
INSERT INTO configuracion (clave, valor) VALUES ('comuna_distancia_maxima_m', '50');

ALTER TABLE configuracion ADD CONSTRAINT ck_config_distancia_comuna
  CHECK (clave <> 'comuna_distancia_maxima_m' OR valor ~ '^[0-9]+(\.[0-9]+)?$');

-- Devuelve 0 o 1 fila. regla: 'contiene' | 'solape' | 'cercana'. distancia_m es 0 salvo en 'cercana'.
-- p_max_m NULL usa el valor de configuracion; se puede pasar otro para probar.
CREATE FUNCTION fn_asignar_comuna(p_punto geometry, p_max_m double precision DEFAULT NULL)
RETURNS TABLE (id_comuna smallint, regla text, distancia_m double precision)
LANGUAGE sql STABLE AS $$
  WITH contiene AS (
    SELECT c.id_comuna
      FROM comuna c
     WHERE c.geom IS NOT NULL AND ST_Covers(c.geom, p_punto)
  ), cercana AS (
    SELECT c.id_comuna, ST_Distance(c.geom::geography, p_punto::geography) AS d
      FROM comuna c
     WHERE c.geom IS NOT NULL
       AND ST_DWithin(c.geom::geography, p_punto::geography,
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

-- Misma vista de V2, ahora con las reglas del solape y del hueco (antes: ST_Covers ... LIMIT 1,
-- que en un solape elegía una comuna al azar). 'poligono' incluye las tres reglas; sin
-- polígono, o sin punto, la comuna sale del barrio.
CREATE OR REPLACE VIEW v_reporte_comuna AS
SELECT r.id_reporte, r.codigo_seguimiento,
       COALESCE(cp.id_comuna, b.id_comuna) AS id_comuna,
       CASE WHEN cp.id_comuna IS NOT NULL THEN 'poligono'
            WHEN b.id_comuna IS NOT NULL THEN 'barrio' END AS origen
FROM reporte r
LEFT JOIN barrio b ON b.id_barrio = r.id_barrio
LEFT JOIN LATERAL (
  SELECT a.id_comuna FROM fn_asignar_comuna(r.ubicacion) a WHERE r.ubicacion IS NOT NULL
) cp ON TRUE;
