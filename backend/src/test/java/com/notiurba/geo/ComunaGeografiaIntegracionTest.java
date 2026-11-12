package com.notiurba.geo;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import com.notiurba.reporte.BaseDeDatosTemporal;

/** La comuna guarda su polígono en geography para no convertirlo en cada asignación. */
@SpringBootTest
@TestPropertySource(locations = "classpath:pruebas.properties")
class ComunaGeografiaIntegracionTest {

    /** Copia de la función de V13, que convierte geom a geography en cada llamada. Es la referencia. */
    private static final String FUNCION_DE_REFERENCIA = """
            CREATE OR REPLACE FUNCTION pg_temp.asignar_referencia(p_punto geometry)
            RETURNS TABLE (id_comuna smallint, regla text, distancia_m double precision)
            LANGUAGE sql STABLE AS $$
              WITH contiene AS (
                SELECT c.id_comuna FROM comuna c WHERE c.geom IS NOT NULL AND ST_Covers(c.geom, p_punto)
              ), cercana AS (
                SELECT c.id_comuna, ST_Distance(c.geom::geography, p_punto::geography) AS d
                  FROM comuna c
                 WHERE c.geom IS NOT NULL
                   AND ST_DWithin(c.geom::geography, p_punto::geography,
                                  COALESCE((SELECT valor::double precision FROM configuracion
                                             WHERE clave = 'comuna_distancia_maxima_m'), 0))
                 ORDER BY d, c.id_comuna LIMIT 1
              )
              SELECT (SELECT min(x.id_comuna) FROM contiene x)::smallint,
                     CASE WHEN (SELECT count(*) FROM contiene) > 1 THEN 'solape' ELSE 'contiene' END,
                     0::double precision
               WHERE EXISTS (SELECT 1 FROM contiene)
              UNION ALL
              SELECT y.id_comuna, 'cercana', y.d FROM cercana y WHERE NOT EXISTS (SELECT 1 FROM contiene)
            $$""";

    private static BaseDeDatosTemporal bd;

    @BeforeAll
    static void crearBaseDesechable() {
        bd = BaseDeDatosTemporal.crearOOmitir();
    }

    @AfterAll
    static void borrarBaseDesechable() throws SQLException {
        if (bd != null) {
            bd.borrar();
        }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        bd.registrar(registro);
    }

    @Autowired private JdbcTemplate jdbc;

    @Test
    void geogEsElMismoPoligonoQueGeomEnGeography() {
        int distintas = jdbc.queryForObject("""
                SELECT count(*) FROM comuna
                 WHERE (geom IS NULL) <> (geog IS NULL)
                    OR (geom IS NOT NULL AND NOT ST_Equals(geog::geometry, geom))""", Integer.class);

        assertThat(distintas).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM comuna WHERE geog IS NOT NULL", Integer.class))
                .isPositive();
        assertThat(jdbc.queryForObject("SELECT indexdef FROM pg_indexes WHERE indexname = 'ix_comuna_geog'",
                String.class)).containsIgnoringCase("gist").contains("geog");
    }

    @Test
    void geogSigueAlPoligonoCuandoCambia() {
        String original = jdbc.queryForObject("SELECT ST_AsEWKT(geom) FROM comuna WHERE id_comuna = 3", String.class);
        try {
            jdbc.update("UPDATE comuna SET geom = ST_Multi(ST_Buffer(geom, 0.0005)) WHERE id_comuna = 3");
            assertThat(jdbc.queryForObject(
                    "SELECT ST_Equals(geog::geometry, geom) FROM comuna WHERE id_comuna = 3", Boolean.class)).isTrue();

            jdbc.update("UPDATE comuna SET geom = NULL WHERE id_comuna = 3");
            assertThat(jdbc.queryForObject("SELECT geog IS NULL FROM comuna WHERE id_comuna = 3", Boolean.class))
                    .isTrue();
        } finally {
            jdbc.update("UPDATE comuna SET geom = ST_GeomFromEWKT(?) WHERE id_comuna = 3", original);
        }
    }

    @Test
    void laAsignacionDaLoMismoQueLaFuncionAnteriorEnCadaPuntoDeUnaCuadricula() {
        jdbc.execute(FUNCION_DE_REFERENCIA);

        // 900 puntos sobre la capa y 300 m alrededor: dentro, en huecos, cerca del borde y fuera
        Map<String, Object> r = jdbc.queryForMap("""
                WITH caja AS (SELECT ST_Expand(ST_Extent(geom)::geometry, 0.003) e FROM comuna),
                     puntos AS (
                       SELECT ST_SetSRID(ST_MakePoint(
                                ST_XMin(e) + (i + 0.5) * (ST_XMax(e) - ST_XMin(e)) / 30,
                                ST_YMin(e) + (j + 0.5) * (ST_YMax(e) - ST_YMin(e)) / 30), 4326) p
                         FROM caja, generate_series(0, 29) i, generate_series(0, 29) j),
                     comparacion AS (
                       SELECT nueva.id_comuna AS c1, nueva.regla AS r1, nueva.distancia_m AS d1,
                              ref.id_comuna AS c2, ref.regla AS r2, ref.distancia_m AS d2
                         FROM puntos
                         LEFT JOIN LATERAL fn_asignar_comuna(p) nueva ON TRUE
                         LEFT JOIN LATERAL pg_temp.asignar_referencia(p) ref ON TRUE)
                SELECT count(*) AS puntos,
                       count(*) FILTER (WHERE c1 IS DISTINCT FROM c2 OR r1 IS DISTINCT FROM r2
                                           OR abs(coalesce(d1, 0) - coalesce(d2, 0)) > 0.001) AS diferencias,
                       count(*) FILTER (WHERE r2 IN ('contiene', 'solape')) AS dentro,
                       count(*) FILTER (WHERE r2 = 'cercana') AS en_hueco,
                       count(*) FILTER (WHERE r2 IS NULL) AS fuera
                  FROM comparacion""");

        assertThat(((Number) r.get("puntos")).intValue()).isEqualTo(900);
        assertThat(((Number) r.get("diferencias")).intValue()).isZero();
        assertThat(((Number) r.get("dentro")).intValue()).isPositive();
        assertThat(((Number) r.get("en_hueco")).intValue()).isPositive();
        assertThat(((Number) r.get("fuera")).intValue()).isPositive();
    }
}
