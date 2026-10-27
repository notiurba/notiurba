package com.notiurba.geo;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpatialProbe {
    public static final String SQL = """
        WITH shape AS (SELECT ST_GeomFromText('POLYGON((0 0,2 0,2 2,0 2,0 0))',4326) AS geom)
        SELECT CURRENT_TIMESTAMP AS executed_at_utc, PostGIS_Version() AS postgis,
          ST_Contains(geom, ST_SetSRID(ST_Point(1,1),4326)) AS contains_inside,
          ST_Contains(geom, ST_SetSRID(ST_Point(0,1),4326)) AS contains_border,
          ST_Covers(geom, ST_SetSRID(ST_Point(0,1),4326)) AS covers_border,
          ST_AsGeoJSON(ST_SetSRID(ST_Point(-76.195,4.085),4326)) AS geojson
        FROM shape
        """;
    private final JdbcTemplate jdbc;
    public SpatialProbe(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Transactional(readOnly = true)
    public Map<String,Object> check() {
        var result = jdbc.queryForMap(SQL);
        if (!Boolean.TRUE.equals(result.get("contains_inside"))
                || !Boolean.FALSE.equals(result.get("contains_border"))
                || !Boolean.TRUE.equals(result.get("covers_border")))
            throw new IllegalStateException("Resultado espacial inesperado");
        return result;
    }
}
