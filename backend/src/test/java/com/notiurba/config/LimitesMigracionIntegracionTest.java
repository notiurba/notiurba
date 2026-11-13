package com.notiurba.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.notiurba.reporte.BaseDeDatosTemporal;

/** V22: valores iniciales, rangos que la base hace respetar y tablas nuevas. */
class LimitesMigracionIntegracionTest {

    private BaseDeDatosTemporal bd;
    private JdbcTemplate jdbc;

    @BeforeEach
    void migrar() {
        bd = BaseDeDatosTemporal.crearOOmitir();
        DataSource fuente = bd.fuente();
        Flyway.configure().dataSource(fuente)
                .placeholders(Map.of("adminPassword", "ClaveDePrueba-" + UUID.randomUUID()))
                .load().migrate();
        jdbc = new JdbcTemplate(fuente);
    }

    @AfterEach
    void borrar() throws SQLException {
        if (bd != null) {
            bd.borrar();
        }
    }

    private String valor(String clave) {
        return jdbc.queryForObject("SELECT valor FROM configuracion WHERE clave = ?", String.class, clave);
    }

    @Test
    void losValoresInicialesSonLosAprobados() {
        assertThat(valor("login_intentos_maximos")).isEqualTo("3");
        assertThat(valor("login_ventana_minutos")).isEqualTo("5");
        assertThat(valor("login_bloqueo_minutos")).isEqualTo("5");
        assertThat(valor("reportes_maximos_por_dia")).isEqualTo("2");
        assertThat(valor("duplicado_ventana_dias")).isEqualTo("30");
    }

    @Test
    void laBaseRechazaValoresFueraDeRango() {
        for (String[] fila : new String[][] {
                {"login_intentos_maximos", "0"}, {"login_intentos_maximos", "11"},
                {"login_ventana_minutos", "0"}, {"login_ventana_minutos", "61"},
                {"login_bloqueo_minutos", "0"}, {"login_bloqueo_minutos", "61"},
                {"reportes_maximos_por_dia", "0"}, {"reportes_maximos_por_dia", "51"},
                {"duplicado_ventana_dias", "0"}, {"duplicado_ventana_dias", "31"},
                {"duplicado_ventana_dias", "abc"}}) {
            assertThatThrownBy(() -> jdbc.update("UPDATE configuracion SET valor = ? WHERE clave = ?", fila[1], fila[0]))
                    .as(fila[0] + " = " + fila[1]).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Test
    void laBaseAceptaLosLimitesDelRango() {
        jdbc.update("UPDATE configuracion SET valor = '10' WHERE clave = 'login_intentos_maximos'");
        jdbc.update("UPDATE configuracion SET valor = '60' WHERE clave = 'login_bloqueo_minutos'");
        jdbc.update("UPDATE configuracion SET valor = '50' WHERE clave = 'reportes_maximos_por_dia'");
        jdbc.update("UPDATE configuracion SET valor = '1' WHERE clave = 'duplicado_ventana_dias'");
        assertThat(valor("login_intentos_maximos")).isEqualTo("10");
        assertThat(valor("duplicado_ventana_dias")).isEqualTo("1");
    }

    @Test
    void hayUnaDistanciaPorCadaCategoriaQueExigeUbicacionYNuncaMenosDeCincoMetros() {
        var conDistancia = jdbc.queryForList("SELECT id_categoria FROM distancia_duplicado ORDER BY 1", Integer.class);
        var exigenUbicacion = jdbc.queryForList(
                "SELECT id_categoria FROM categoria WHERE requiere_ubicacion ORDER BY 1", Integer.class);
        assertThat(conDistancia).hasSize(10).containsExactlyElementsOf(exigenUbicacion);
        assertThat(jdbc.queryForObject("SELECT metros FROM distancia_duplicado WHERE id_categoria = 1", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT metros FROM distancia_duplicado WHERE id_categoria = 4", Integer.class)).isEqualTo(8);
        assertThat(jdbc.queryForObject("SELECT max(metros) FROM distancia_duplicado", Integer.class)).isEqualTo(15);
        assertThatThrownBy(() -> jdbc.update("UPDATE distancia_duplicado SET metros = 4 WHERE id_categoria = 1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE distancia_duplicado SET metros = 501 WHERE id_categoria = 1"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existenLasTablasYLaColumnaNuevas() {
        jdbc.update("INSERT INTO intento_login (correo_normalizado, fallidos) VALUES ('a@b.co', 1)");
        assertThat(jdbc.queryForObject("SELECT fallidos FROM intento_login WHERE correo_normalizado = 'a@b.co'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT column_default FROM information_schema.columns
                 WHERE table_name = 'reporte' AND column_name = 'requiere_evidencia'""", String.class)).isEqualTo("false");
    }
}
