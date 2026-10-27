package com.notiurba;

import com.notiurba.geo.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

/** No writes, no Flyway, no DDL: enable explicitly on an isolated database only. */
@DataJpaTest
@ActiveProfiles("validation")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SpatialProbe.class)
class SpatialIntegrationTest {
    @Autowired SpatialProbe probe;
    @Autowired EntityManager entityManager;
    @Autowired ComunaRepository comunas;
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
