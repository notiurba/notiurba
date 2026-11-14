package com.notiurba.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.RandomValuePropertySource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import com.notiurba.reporte.BaseDeDatosTemporal;

/** El administrador sembrado ya no depende de ADMIN_DEV_PASSWORD: sin ella la clave es aleatoria. */
class AdminSemillaIntegracionTest {

    private static final String CLAVE_DE_DESARROLLO = "ClaveDeDesarrolloConocida-2026";
    private static final String CLAVE_ADMIN = "spring.flyway.placeholders.adminPassword";

    private BaseDeDatosTemporal bd;

    @BeforeEach
    void crear() {
        bd = BaseDeDatosTemporal.crearOOmitir();
    }

    @AfterEach
    void borrar() throws SQLException {
        if (bd != null) {
            bd.borrar();
        }
    }

    /** Lee application.yml sin variables de entorno, como un servidor al que le falta ADMIN_DEV_PASSWORD. */
    private String claveSinVariable() throws IOException {
        var entorno = new StandardEnvironment();
        MutablePropertySources fuentes = entorno.getPropertySources();
        fuentes.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        fuentes.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        RandomValuePropertySource.addToEnvironment(entorno);
        for (PropertySource<?> fuente : new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"))) {
            fuentes.addLast(fuente);
        }
        return entorno.getProperty(CLAVE_ADMIN);
    }

    @Test
    void sinLaVariableLaClaveEsAleatoriaYDistintaCadaVez() throws IOException {
        String primera = claveSinVariable();
        String segunda = claveSinVariable();

        assertThat(primera).isNotBlank().hasSizeGreaterThanOrEqualTo(32);
        assertThat(segunda).isNotEqualTo(primera);
    }

    @Test
    void conLaVariableSeUsaSuValor() throws IOException {
        var entorno = new StandardEnvironment();
        entorno.getPropertySources().addFirst(new MapPropertySource("prueba", Map.of("ADMIN_DEV_PASSWORD", "valor-de-prueba")));
        RandomValuePropertySource.addToEnvironment(entorno);
        for (PropertySource<?> fuente : new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"))) {
            entorno.getPropertySources().addLast(fuente);
        }

        assertThat(entorno.getProperty(CLAVE_ADMIN)).isEqualTo("valor-de-prueba");
    }

    @Test
    void unaBaseNuevaNoQuedaConUnaClaveConocida() throws IOException {
        DataSource fuente = bd.fuente();
        Flyway.configure().dataSource(fuente).placeholders(Map.of("adminPassword", claveSinVariable()))
                .load().migrate();

        var jdbc = new JdbcTemplate(fuente);
        Boolean sirveLaConocida = jdbc.queryForObject(
                "SELECT password_hash = crypt(?, password_hash) FROM usuario WHERE correo = 'admin.dev@notiurba.local'",
                Boolean.class, CLAVE_DE_DESARROLLO);

        assertThat(sirveLaConocida).isFalse();
    }

    @Test
    void unaBaseQueYaTieneV4NoRechazaUnaClaveDistinta() throws IOException {
        DataSource fuente = bd.fuente();
        Flyway.configure().dataSource(fuente).placeholders(Map.of("adminPassword", CLAVE_DE_DESARROLLO))
                .load().migrate();

        assertThatCode(() -> Flyway.configure().dataSource(fuente)
                .placeholders(Map.of("adminPassword", claveSinVariable())).load().migrate())
                .doesNotThrowAnyException();
        assertThatCode(() -> Flyway.configure().dataSource(fuente)
                .placeholders(Map.of("adminPassword", claveSinVariable())).load().validate())
                .doesNotThrowAnyException();
    }
}
