package com.notiurba.geo;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.notiurba.reporte.BarrioDePrueba;
import com.notiurba.reporte.BaseDeDatosTemporal;
import com.notiurba.reporte.DatosDePrueba;

/** Al migrar, los reportes que ya existían quedan con la misma comuna que daba la vista. */
class ComunaGuardadaMigracionIntegracionTest {

    private BaseDeDatosTemporal bd;

    @BeforeEach
    void crearBaseDesechable() {
        bd = BaseDeDatosTemporal.crearOOmitir();
    }

    @AfterEach
    void borrarBaseDesechable() throws SQLException {
        if (bd != null) {
            bd.borrar();
        }
    }

    private static void migrar(DataSource fuente, String hasta) {
        Flyway.configure().dataSource(fuente)
                .placeholders(Map.of("adminPassword", "ClaveDePrueba-" + UUID.randomUUID()))
                .target(hasta).load().migrate();
    }

    @Test
    void losReportesQueYaExistianQuedanConLaComunaQueDabaLaVista() {
        DataSource fuente = bd.fuente();
        migrar(fuente, "19");
        var jdbc = new JdbcTemplate(fuente);
        int idBarrio = BarrioDePrueba.id(jdbc);
        long idAna = DatosDePrueba.usuario(jdbc, idBarrio, "ana@prueba.com", DatosDePrueba.CIUDADANO);
        String enComuna = "ST_GeomFromEWKT('" + jdbc.queryForObject(
                "SELECT ST_AsEWKT(ST_PointOnSurface(geom)) FROM comuna WHERE id_comuna = 5", String.class) + "')";
        String lejano = "ST_SetSRID(ST_MakePoint(-75.5, 4.6), 4326)";
        for (var reporte : List.of(new Object[] {enComuna, idBarrio}, new Object[] {lejano, idBarrio},
                new Object[] {"NULL", idBarrio}, new Object[] {lejano, null})) {
            jdbc.update("""
                    INSERT INTO reporte (id_usuario, id_categoria, id_barrio, descripcion, ubicacion,
                                         autorizacion_envio_en, creado_en)
                    VALUES (?, 1, ?, 'Reporte creado antes de guardar la comuna', %s, now(), now())"""
                    .formatted(reporte[0]), idAna, reporte[1]);
        }
        String consulta = "SELECT id_reporte, id_comuna::int AS id_comuna, origen FROM v_reporte_comuna ORDER BY id_reporte";
        List<Map<String, Object>> antes = jdbc.queryForList(consulta);

        migrar(fuente, "latest");

        assertThat(jdbc.queryForList("""
                SELECT id_reporte, id_comuna::int AS id_comuna, comuna_origen AS origen
                  FROM reporte ORDER BY id_reporte""")).isEqualTo(antes);
        assertThat(jdbc.queryForList(consulta)).isEqualTo(antes);
        assertThat(antes).extracting(fila -> fila.get("origen")).containsExactly("poligono", "barrio", "barrio", null);
    }
}
