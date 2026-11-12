package com.notiurba.correo;

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

/** Al migrar, los canales se corrigen sin borrar ninguno y sin tocar los envíos que ya existían. */
class CanalesMigracionIntegracionTest {

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
    void corrigeLosCanalesSinBorrarNingunoNiTocarLosEnvios() {
        DataSource fuente = bd.fuente();
        migrar(fuente, "20");
        var jdbc = new JdbcTemplate(fuente);
        int idBarrio = BarrioDePrueba.id(jdbc);
        long idAna = DatosDePrueba.usuario(jdbc, idBarrio, "ana@prueba.com", DatosDePrueba.CIUDADANO);
        long reporte = jdbc.queryForObject("""
                INSERT INTO reporte (id_usuario, id_categoria, id_barrio, descripcion, autorizacion_envio_en, creado_en)
                VALUES (?, 12, ?, 'Reporte de gas anterior a los canales nuevos', now(), now())
                RETURNING id_reporte""", Long.class, idAna, idBarrio);
        jdbc.update("""
                INSERT INTO registro_envio_correo (id_reporte, id_canal_atencion, id_modo_envio, id_estado_envio, destino)
                VALUES (?, 18, (SELECT id_modo_envio FROM modo_envio WHERE nombre = 'RESEND'),
                        (SELECT id_estado_envio FROM estado_envio WHERE nombre = 'PENDIENTE'), 'inquietudes@gdo.com.co')""",
                reporte);
        List<Integer> antes = jdbc.queryForList("SELECT id_canal_atencion FROM canal_atencion ORDER BY 1", Integer.class);

        migrar(fuente, "latest");

        assertThat(jdbc.queryForList("SELECT id_canal_atencion FROM canal_atencion ORDER BY 1", Integer.class))
                .containsAll(antes);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM registro_envio_correo WHERE id_reporte = ? AND id_canal_atencion = 18""",
                Integer.class, reporte)).isEqualTo(1);

        assertThat(jdbc.queryForMap("""
                SELECT x.correo_confirmado, k.acceso, k.es_principal, k.activo
                  FROM configuracion_enrutamiento x JOIN canal_atencion k ON k.id_canal_atencion = x.id_canal_correo
                 WHERE x.codigo = 'E01'""")).containsEntry("correo_confirmado", true)
                .containsEntry("acceso", "info@centroaguas.com").containsEntry("es_principal", true)
                .containsEntry("activo", true);
        assertThat(activo(jdbc, "notificacionespqr@centroaguas.com")).isFalse();

        assertThat(jdbc.queryForObject("SELECT correo_confirmado FROM configuracion_enrutamiento WHERE codigo = 'E02'",
                Boolean.class)).isFalse();
        assertThat(activo(jdbc, "inquietudes@gdo.com.co")).isFalse();
        assertThat(activo(jdbc, "315 665 8536")).isFalse();

        assertThat(activo(jdbc, "317 372 5078")).isTrue();
        assertThat(activo(jdbc, "pqr.tulua.co@veolia.com")).isFalse();

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM canal_atencion
                 WHERE activo AND id_entidad_destino IS NOT NULL AND (fuente IS NULL OR verificado_en IS NULL)""",
                Integer.class)).as("canales activos sin fuente o sin fecha de verificación").isZero();
    }

    private static Boolean activo(JdbcTemplate jdbc, String acceso) {
        return jdbc.queryForObject("SELECT activo FROM canal_atencion WHERE acceso = ?", Boolean.class, acceso);
    }
}
