package com.notiurba;

import com.notiurba.geo.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

/** No writes, no Flyway, no DDL: enable explicitly on an isolated database only. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("validation")
@Transactional(readOnly = true)

class SpatialIntegrationTest {
    @org.junit.jupiter.api.BeforeAll static void requireIsolatedTlsConfiguration() {
        String url = System.getenv("TEST_DB_URL");
        if (url == null || !url.startsWith("jdbc:postgresql://"))
            throw new IllegalStateException("Missing authorized test JDBC configuration; value omitted");
        try {
            var uri = java.net.URI.create(url.substring(5));
            var options = new java.util.HashMap<String,String>();
            for (String part : uri.getRawQuery().split("&")) {
                var pair = part.split("=",2);
                if (pair.length != 2 || options.put(pair[0], java.net.URLDecoder.decode(pair[1],java.nio.charset.StandardCharsets.UTF_8)) != null)
                    throw new IllegalArgumentException();
            }
            if (!uri.getHost().endsWith(".neon.tech") || uri.getUserInfo() != null
                    || !"verify-full".equals(options.get("sslmode"))
                    || !"org.postgresql.ssl.DefaultJavaSSLFactory".equals(options.get("sslfactory")))
                throw new IllegalArgumentException();
        } catch (Exception e) {
            throw new IllegalStateException("Expected isolated Neon with certificate and hostname verification; URL omitted");
        }
    }
    @Autowired SpatialProbe probe;
    @Autowired EntityManager entityManager;
    @Autowired ComunaRepository comunas;
    @Autowired JdbcTemplate jdbc;
    @Test void realNeonTlsAndReadOnlyTransaction() {
        assertThat(jdbc.queryForObject("SHOW transaction_read_only", String.class)).isEqualTo("on");
        assertThat(jdbc.queryForObject("SELECT ssl FROM pg_stat_ssl WHERE pid=pg_backend_pid()", Boolean.class)).isTrue();
        System.out.println("UTC=" + java.time.Instant.now() + " isolated Neon JDBC SpringBoot JPA"
            + " PostgreSQL=" + jdbc.queryForObject("SHOW server_version", String.class)
            + " PostGIS=" + jdbc.queryForObject("SELECT PostGIS_Version()", String.class)
            + " TLS=true transaction_read_only=on");
    }
    @Test void jdbcAndSpatialOperations() {
        var result = probe.check();
        assertThat(result.get("contains_inside")).isEqualTo(true);
        assertThat(result.get("contains_border")).isEqualTo(false);
        assertThat(result.get("covers_border")).isEqualTo(true);
        assertThat(result.get("geojson").toString()).contains("Point","-76.195","4.085");
    }
    @Test void springDataJpaReadsExistingSchema() {
        assertThat(comunas.count()).isGreaterThanOrEqualTo(0L);
    }
    @Test void hibernateSpatialDecodesPoint() {
        var point = (org.locationtech.jts.geom.Point) entityManager.createNativeQuery(
            "SELECT ST_SetSRID(ST_Point(-76.195,4.085),4326) AS geom")
            .unwrap(org.hibernate.query.NativeQuery.class)
            .addScalar("geom", org.locationtech.jts.geom.Point.class).getSingleResult();
        assertThat(point.getSRID()).isEqualTo(4326);
        assertThat(point.getX()).isEqualTo(-76.195);
        assertThat(point.getY()).isEqualTo(4.085);
    }
    @Test void hibernateSpatialDecodesGeometryWithoutWritingTables() {
        var row = (Comuna) entityManager.createNativeQuery("""
            SELECT 1::smallint AS id_comuna,
              ST_Multi(ST_GeomFromText('POLYGON((0 0,2 0,2 2,0 2,0 0))',4326)) AS geom
            """, Comuna.class).getSingleResult();
        assertThat(row.getGeom().getSRID()).isEqualTo(4326);
        assertThat(row.getGeom().getArea()).isEqualTo(4.0);
        assertThat(row.getId()).isEqualTo((short)1);
    }
}
