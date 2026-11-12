package com.notiurba.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import com.notiurba.reporte.BarrioDePrueba;
import com.notiurba.reporte.BaseDeDatosTemporal;
import com.notiurba.reporte.DatosDePrueba;

/** La comuna se guarda en el reporte y se mantiene igual a la que calcularía PostGIS en vivo. */
@SpringBootTest
@TestPropertySource(locations = "classpath:pruebas.properties")
class ComunaGuardadaIntegracionTest {

    private static final String PUNTO_LEJANO = "ST_SetSRID(ST_MakePoint(-75.5, 4.6), 4326)";

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
    @Autowired private DataSource fuente;

    private int idBarrio;
    private long idAna;

    @BeforeEach
    void preparar() {
        jdbc.execute("TRUNCATE reporte CASCADE");
        idBarrio = BarrioDePrueba.id(jdbc);
        idAna = DatosDePrueba.usuario(jdbc, idBarrio, "ana-" + UUID.randomUUID() + "@prueba.com",
                DatosDePrueba.CIUDADANO);
    }

    private long reporte(String ubicacion, Integer barrio) {
        return jdbc.queryForObject("""
                INSERT INTO reporte (id_usuario, id_categoria, id_barrio, descripcion, ubicacion,
                                     autorizacion_envio_en, creado_en)
                VALUES (?, 1, ?, 'Reporte para la comuna guardada', %s, now(), now())
                RETURNING id_reporte""".formatted(ubicacion), Long.class, idAna, barrio);
    }

    private Map<String, Object> guardada(long idReporte) {
        return jdbc.queryForMap("SELECT id_comuna::int AS id_comuna, comuna_origen FROM reporte WHERE id_reporte = ?", idReporte);
    }

    /** La comuna calculada en vivo, con la regla de siempre: polígono y, si no hay, el barrio. */
    private Integer enVivo(long idReporte) {
        return jdbc.queryForObject("""
                SELECT COALESCE((SELECT a.id_comuna FROM fn_asignar_comuna(r.ubicacion) a WHERE r.ubicacion IS NOT NULL),
                                b.id_comuna)::int
                  FROM reporte r LEFT JOIN barrio b ON b.id_barrio = r.id_barrio
                 WHERE r.id_reporte = ?""", Integer.class, idReporte);
    }

    private String puntoEnComuna(int comuna) {
        return jdbc.queryForObject("""
                SELECT ST_AsEWKT(ST_PointOnSurface(ST_Difference(c.geom,
                    (SELECT ST_Union(o.geom) FROM comuna o WHERE o.id_comuna <> c.id_comuna AND o.geom IS NOT NULL))))
                  FROM comuna c WHERE c.id_comuna = ?""", String.class, comuna);
    }

    /** Un punto en un hueco entre comunas, a menos de la distancia máxima de alguna. */
    private String puntoEnHueco() {
        return jdbc.queryForObject("""
                WITH caja AS (SELECT ST_Extent(geom)::geometry e FROM comuna),
                     puntos AS (
                       SELECT ST_SetSRID(ST_MakePoint(ST_XMin(e) + i * (ST_XMax(e) - ST_XMin(e)) / 80,
                                                      ST_YMin(e) + j * (ST_YMax(e) - ST_YMin(e)) / 80), 4326) p
                         FROM caja, generate_series(0, 80) i, generate_series(0, 80) j)
                SELECT ST_AsEWKT(p) FROM puntos, LATERAL fn_asignar_comuna(p) a
                 WHERE a.regla = 'cercana' LIMIT 1""", String.class);
    }

    /** Cambia cada vez que la fila se reescribe. */
    private String versionDeLaFila(long idReporte) {
        return jdbc.queryForObject("SELECT xmin::text FROM reporte WHERE id_reporte = ?", String.class, idReporte);
    }

    private static String ewkt(String punto) {
        return "ST_GeomFromEWKT('" + punto + "')";
    }

    @Test
    void alCrearSeGuardaLaComunaDelPoligono() {
        long id = reporte(ewkt(puntoEnComuna(5)), idBarrio);

        assertThat(guardada(id)).containsEntry("id_comuna", 5).containsEntry("comuna_origen", "poligono");
    }

    @Test
    void sinPuntoOFueraDeLaCapaLaComunaSaleDelBarrio() {
        long sinPunto = reporte("NULL", idBarrio);
        long lejano = reporte(PUNTO_LEJANO, idBarrio);
        long lejanoSinBarrio = reporte(PUNTO_LEJANO, null);

        assertThat(guardada(sinPunto)).containsEntry("id_comuna", 1).containsEntry("comuna_origen", "barrio");
        assertThat(guardada(lejano)).containsEntry("id_comuna", 1).containsEntry("comuna_origen", "barrio");
        assertThat(guardada(lejanoSinBarrio).get("id_comuna")).isNull();
        assertThat(guardada(lejanoSinBarrio).get("comuna_origen")).isNull();
    }

    @Test
    void alMoverElPuntoSeRecalcula() {
        long id = reporte(ewkt(puntoEnComuna(5)), idBarrio);

        jdbc.update("UPDATE reporte SET ubicacion = " + ewkt(puntoEnComuna(2)) + " WHERE id_reporte = ?", id);

        assertThat(guardada(id)).containsEntry("id_comuna", 2);
    }

    @Test
    void alCambiarLaComunaDeUnBarrioSeRecalculanSusReportesSinPoligono() {
        long sinPunto = reporte("NULL", idBarrio);
        long lejano = reporte(PUNTO_LEJANO, idBarrio);
        long conPunto = reporte(ewkt(puntoEnComuna(5)), idBarrio);
        try {
            jdbc.update("UPDATE barrio SET id_comuna = 7 WHERE id_barrio = ?", idBarrio);

            assertThat(guardada(sinPunto)).containsEntry("id_comuna", 7);
            assertThat(guardada(lejano)).containsEntry("id_comuna", 7).containsEntry("comuna_origen", "barrio");
            assertThat(guardada(conPunto)).containsEntry("id_comuna", 5);
        } finally {
            jdbc.update("UPDATE barrio SET id_comuna = 1 WHERE id_barrio = ?", idBarrio);
        }
        assertThat(guardada(sinPunto)).containsEntry("id_comuna", 1);
        assertThat(guardada(lejano)).containsEntry("id_comuna", 1);
    }

    @Test
    void escribirLaComunaAManoNoSaltaLaRegla() {
        long id = reporte(ewkt(puntoEnComuna(5)), idBarrio);

        jdbc.update("UPDATE reporte SET id_comuna = 9, comuna_origen = 'poligono' WHERE id_reporte = ?", id);
        assertThat(guardada(id)).containsEntry("id_comuna", 5).containsEntry("comuna_origen", "poligono");

        jdbc.update("UPDATE reporte SET id_comuna = NULL, comuna_origen = NULL WHERE id_reporte = ?", id);
        assertThat(guardada(id)).containsEntry("id_comuna", 5).containsEntry("comuna_origen", "poligono");
    }

    @Test
    void borrarCrearORenombrarLaDistanciaMaximaTambienRecalcula() {
        long enHueco = reporte(ewkt(puntoEnHueco()), null);
        try {
            jdbc.update("DELETE FROM configuracion WHERE clave = 'comuna_distancia_maxima_m'");
            assertThat(guardada(enHueco).get("id_comuna")).isNull();

            jdbc.update("INSERT INTO configuracion (clave, valor) VALUES ('comuna_distancia_maxima_m', '50')");
            assertThat(guardada(enHueco).get("id_comuna")).isNotNull();

            jdbc.update("UPDATE configuracion SET clave = 'distancia_renombrada' WHERE clave = 'comuna_distancia_maxima_m'");
            assertThat(guardada(enHueco).get("id_comuna")).isNull();
        } finally {
            jdbc.update("DELETE FROM configuracion WHERE clave IN ('comuna_distancia_maxima_m', 'distancia_renombrada')");
            jdbc.update("INSERT INTO configuracion (clave, valor) VALUES ('comuna_distancia_maxima_m', '50')");
        }
        assertThat(guardada(enHueco).get("id_comuna")).isNotNull();
    }

    @Test
    void unCambioQueNoTocaElPoligonoNiLaDistanciaNoReescribeLosReportes() {
        long id = reporte(ewkt(puntoEnComuna(5)), idBarrio);
        String version = versionDeLaFila(id);

        jdbc.update("UPDATE comuna SET geom = geom WHERE id_comuna = 5");
        jdbc.update("UPDATE comuna SET geom = NULL WHERE id_comuna = 99");
        jdbc.update("UPDATE configuracion SET valor = valor WHERE clave = 'comuna_distancia_maxima_m'");

        assertThat(versionDeLaFila(id)).isEqualTo(version);
    }

    /** Un reporte que se está creando mientras cambia la capa no puede quedar con la regla anterior. */
    @Test
    void unReporteCreadoMientrasCambiaLaCapaQuedaConLaCapaNueva() throws Exception {
        String original = jdbc.queryForObject("SELECT ST_AsEWKT(geom) FROM comuna WHERE id_comuna = 5", String.class);
        long id;
        try (ExecutorService otraSesion = Executors.newSingleThreadExecutor();
                Connection creador = fuente.getConnection()) {
            creador.setAutoCommit(false);
            try (PreparedStatement ps = creador.prepareStatement("""
                    INSERT INTO reporte (id_usuario, id_categoria, id_barrio, descripcion, ubicacion,
                                         autorizacion_envio_en, creado_en)
                    VALUES (?, 1, ?, 'Reporte creado mientras cambia la capa', %s, now(), now())
                    RETURNING id_reporte""".formatted(ewkt(puntoEnComuna(5))))) {
                ps.setLong(1, idAna);
                ps.setInt(2, idBarrio);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    id = rs.getLong(1);
                }
            }

            Future<Integer> cambioDeCapa = otraSesion.submit(
                    () -> jdbc.update("UPDATE comuna SET geom = NULL WHERE id_comuna = 5"));
            assertThatThrownBy(() -> cambioDeCapa.get(1, TimeUnit.SECONDS))
                    .as("el cambio de capa espera a que termine de crearse el reporte")
                    .isInstanceOf(TimeoutException.class);
            creador.commit();
            cambioDeCapa.get(10, TimeUnit.SECONDS);

            assertThat(guardada(id).get("id_comuna")).isNotEqualTo(5).isEqualTo(enVivo(id));
        } finally {
            jdbc.update("UPDATE comuna SET geom = ST_GeomFromEWKT(?) WHERE id_comuna = 5", original);
        }
    }

    @Test
    void alCambiarElPoligonoDeUnaComunaSeRecalculanLosReportes() {
        long id = reporte(ewkt(puntoEnComuna(5)), idBarrio);
        String original = jdbc.queryForObject("SELECT ST_AsEWKT(geom) FROM comuna WHERE id_comuna = 5", String.class);
        try {
            jdbc.update("UPDATE comuna SET geom = NULL WHERE id_comuna = 5");

            assertThat(guardada(id).get("id_comuna")).isNotEqualTo(5);
            assertThat(guardada(id).get("id_comuna")).isEqualTo(enVivo(id));
        } finally {
            jdbc.update("UPDATE comuna SET geom = ST_GeomFromEWKT(?) WHERE id_comuna = 5", original);
        }
        assertThat(guardada(id)).containsEntry("id_comuna", 5).containsEntry("comuna_origen", "poligono");
    }

    @Test
    void alCambiarLaDistanciaMaximaSeRecalculanLosReportesEnHuecos() {
        long enHueco = reporte(ewkt(puntoEnHueco()), null);
        assertThat(guardada(enHueco).get("id_comuna")).isNotNull();
        try {
            jdbc.update("UPDATE configuracion SET valor = '0' WHERE clave = 'comuna_distancia_maxima_m'");

            assertThat(guardada(enHueco).get("id_comuna")).isNull();
        } finally {
            jdbc.update("UPDATE configuracion SET valor = '50' WHERE clave = 'comuna_distancia_maxima_m'");
        }
        assertThat(guardada(enHueco).get("id_comuna")).isNotNull();
    }

    @Test
    void laVistaLeeLoGuardadoYCoincideConElCalculoEnVivo() {
        reporte(ewkt(puntoEnComuna(3)), idBarrio);
        reporte(ewkt(puntoEnHueco()), idBarrio);
        reporte("NULL", idBarrio);
        reporte(PUNTO_LEJANO, null);

        int distintos = jdbc.queryForObject("""
                SELECT count(*) FROM reporte r JOIN v_reporte_comuna vc USING (id_reporte)
                  LEFT JOIN barrio b ON b.id_barrio = r.id_barrio
                 WHERE vc.id_comuna IS DISTINCT FROM r.id_comuna
                    OR vc.origen IS DISTINCT FROM r.comuna_origen
                    OR r.id_comuna IS DISTINCT FROM COALESCE(
                         (SELECT a.id_comuna FROM fn_asignar_comuna(r.ubicacion) a WHERE r.ubicacion IS NOT NULL),
                         b.id_comuna)""", Integer.class);

        assertThat(distintos).isZero();
        assertThat(jdbc.queryForObject("SELECT indexdef FROM pg_indexes WHERE indexname = 'ix_reporte_comuna'",
                String.class)).contains("id_comuna");
    }
}
