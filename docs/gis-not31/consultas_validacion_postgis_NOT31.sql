-- Validación REAL de la carga, dentro de la transacción aislada.
-- Requiere las tablas temporales de entrada, perímetro y puntos del importador.
-- No modifica vistas de asignación ni reglas NOT-21. No crea índices/tablas permanentes.
CREATE OR REPLACE FUNCTION pg_temp.validar_not31(iteracion integer)
RETURNS jsonb LANGUAGE plpgsql AS $validacion$
DECLARE
    filas bigint; nulas bigint; validas bigint; srids bigint; multis bigint;
    nombres_ok bigint; ids smallint[]; indice_ok boolean;
    cobertura geometry; esperado geometry; huecos bigint;
    pares bigint; area_solape double precision;
    faltante geometry; excedente geometry; coincidencias_ok bigint; puntos_total bigint;
    resultados_puntos jsonb; detalle_filas jsonb;
BEGIN
    SELECT count(*), count(*) FILTER(WHERE geom IS NULL),
           count(*) FILTER(WHERE ST_IsValid(geom) AND NOT ST_IsEmpty(geom)),
           count(*) FILTER(WHERE ST_SRID(geom)=4326),
           count(*) FILTER(WHERE ST_GeometryType(geom)='ST_MultiPolygon'),
           array_agg(id_comuna ORDER BY id_comuna)
      INTO filas,nulas,validas,srids,multis,ids FROM public.comuna;
    SELECT count(*) INTO nombres_ok FROM public.comuna c JOIN _not31_input i USING(id_comuna)
      WHERE c.nombre=i.nombre AND ST_OrderingEquals(c.geom,i.geom);
    SELECT EXISTS (
      SELECT 1 FROM pg_index x JOIN pg_class ic ON ic.oid=x.indexrelid
      JOIN pg_am am ON am.oid=ic.relam
      WHERE x.indrelid='public.comuna'::regclass AND ic.relname='ix_comuna_geom'
        AND x.indisvalid AND x.indisready AND am.amname='gist'
        AND pg_get_indexdef(x.indexrelid) LIKE '%(geom)%'
    ) INTO indice_ok;
    SELECT count(*),coalesce(sum(ST_Area(ST_Transform(interseccion,3115))),0)
      INTO pares,area_solape FROM (
        SELECT ST_Intersection(a.geom,b.geom) AS interseccion
        FROM public.comuna a JOIN public.comuna b ON a.id_comuna<b.id_comuna
        WHERE a.geom && b.geom
      ) q WHERE ST_Area(interseccion)>0;
    SELECT ST_UnaryUnion(ST_Collect(geom)) INTO cobertura FROM public.comuna;
    SELECT geom INTO esperado FROM _not31_perimetro;
    SELECT coalesce(sum(ST_NumInteriorRings(d.geom)),0) INTO huecos
      FROM ST_Dump(cobertura) d;
    faltante:=ST_Difference(esperado,cobertura);
    excedente:=ST_Difference(cobertura,esperado);
    WITH observados AS (
      SELECT p.id,p.tipo,p.esperado_comunas,p.esperado_estado,
        coalesce(string_agg(lpad(c.id_comuna::text,2,'0'),'|' ORDER BY c.id_comuna),'') AS comunas,
        count(c.id_comuna) AS candidatos,ST_Covers(esperado,p.geom) AS dentro
      FROM _not31_puntos p LEFT JOIN public.comuna c ON ST_Covers(c.geom,p.geom)
      GROUP BY p.id,p.tipo,p.esperado_comunas,p.esperado_estado,p.geom
    ), clasificados AS (
      SELECT *,CASE WHEN candidatos=1 THEN 'INTERIOR_UNICO_EXPERIMENTAL'
                    WHEN candidatos>1 THEN 'BORDE_EXPERIMENTAL'
                    ELSE 'FUERA_PERIMETRO_EXPERIMENTAL' END AS estado
      FROM observados
    ) SELECT count(*),count(*) FILTER(WHERE comunas=esperado_comunas AND estado=esperado_estado
                  AND ((candidatos>0)=dentro)),
             jsonb_agg(jsonb_build_object('id',id,'tipo',tipo,'candidatos',candidatos,
               'comunas',comunas,'estado',estado,'coincide',comunas=esperado_comunas AND estado=esperado_estado)
               ORDER BY id)
       INTO puntos_total,coincidencias_ok,resultados_puntos FROM clasificados;
    SELECT jsonb_agg(jsonb_build_object('id',id_comuna,'nombre',nombre,
               'srid',ST_SRID(geom),'tipo',ST_GeometryType(geom),'valida',ST_IsValid(geom),
               'geometria_md5',md5(ST_AsEWKB(geom))) ORDER BY id_comuna)
       INTO detalle_filas FROM public.comuna;
    IF filas<>10 OR nulas<>0 OR validas<>10 OR srids<>10 OR multis<>10 OR nombres_ok<>10
       OR ids IS DISTINCT FROM ARRAY[1,2,3,4,5,6,7,8,9,10]::smallint[] OR NOT indice_ok
       OR pares<>0 OR huecos<>0 OR NOT ST_IsEmpty(faltante) OR NOT ST_IsEmpty(excedente)
       OR NOT ST_Equals(cobertura,esperado) OR puntos_total<>74 OR coincidencias_ok<>74 THEN
       RAISE EXCEPTION 'NOT31: fallo de validacion iteracion %, filas %, validas %, nulas %, SRID %, Multi %, nombres %, solapes %, huecos %, faltante m2 %, fuera m2 %, puntos %/%',
         iteracion,filas,validas,nulas,srids,multis,nombres_ok,pares,huecos,
         ST_Area(ST_Transform(faltante,3115)),ST_Area(ST_Transform(excedente,3115)),coincidencias_ok,puntos_total;
    END IF;
    RETURN jsonb_build_object('ejecucion',iteracion,'aprobada',true,'comunas',filas,
      'geometrias_nulas',nulas,'validas',validas,'srid_4326',srids,'multipolygon',multis,
      'ids',ids,'nombres_y_geometrias_iguales_al_archivo',nombres_ok,'gist_valido',indice_ok,
      'parejas_con_solape',pares,'solapamientos_m2',area_solape,'huecos',huecos,
      'cobertura_perimetro',true,'sin_asignar_m2',ST_Area(ST_Transform(faltante,3115)),
      'fuera_perimetro_m2',ST_Area(ST_Transform(excedente,3115)),
      'puntos_aprobados',coincidencias_ok,'puntos_total',puntos_total,
      'puntos',resultados_puntos,'filas',detalle_filas);
END $validacion$;
