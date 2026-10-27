package com.notiurba;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;
import static org.assertj.core.api.Assertions.assertThat;

/** Explicit -Pmigration-tests only: a new local container, never a Neon URL. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties={
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"})
@Testcontainers
class MigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgis/postgis:18-3.6").asCompatibleSubstituteFor("postgres"))
        .withDatabaseName("notiurba_edt51_disposable");
    private static final String TEST_ADMIN_PASSWORD = UUID.randomUUID().toString();
    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry props) {
        props.add("spring.datasource.url", postgres::getJdbcUrl);
        props.add("spring.datasource.username", postgres::getUsername);
        props.add("spring.datasource.password", postgres::getPassword);
        props.add("spring.datasource.hikari.read-only", () -> false);
        props.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        props.add("spring.flyway.enabled", () -> true);
        props.add("spring.flyway.clean-disabled", () -> true);
        props.add("spring.flyway.placeholders.adminPassword", () -> TEST_ADMIN_PASSWORD);
        props.add("notiurba.jwt.secreto", () -> UUID.randomUUID().toString()+UUID.randomUUID());
    }
    @Test void validationProfileStartsWithoutGeneralDatabaseOrSeedPasswords() throws Exception {
        // A second, real Boot context on the disposable database: validation must
        // override application.yml without DB_PASSWORD or ADMIN_DEV_PASSWORD.
        assertThat(System.getenv("DB_PASSWORD")).isNull();
        assertThat(System.getenv("ADMIN_DEV_PASSWORD")).isNull();
        var application = new org.springframework.boot.SpringApplication(NotiUrbaApplication.class);
        application.setAdditionalProfiles("validation");
        application.addInitializers(context -> context.getEnvironment().getPropertySources().addFirst(
            new org.springframework.core.env.MapPropertySource("isolated-validation-test", java.util.Map.of(
                "TEST_DB_URL", postgres.getJdbcUrl(),
                "TEST_DB_USER", postgres.getUsername(),
                "TEST_DB_PASSWORD", postgres.getPassword(),
                "JWT_SECRET", UUID.randomUUID().toString()+UUID.randomUUID(),
                "server.port", "0",
                "spring.autoconfigure.exclude", "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"))));
        // Ensure schema exists independent of JUnit method order. Container only.
        Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .placeholders(java.util.Map.of("adminPassword", TEST_ADMIN_PASSWORD)).load().migrate();
        try (var validation = application.run()) {
            var environment = validation.getEnvironment();
            assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isFalse();
            assertThat(validation.getBeansOfType(Flyway.class)).isEmpty();
            assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
            assertThat(validation.getBean(com.zaxxer.hikari.HikariDataSource.class).isReadOnly()).isTrue();
            var template = validation.getBean(JdbcTemplate.class);
            assertThat(template.queryForObject("SHOW transaction_read_only", String.class)).isEqualTo("on");
            int port = ((org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext)validation).getWebServer().getPort();
            var response = java.net.http.HttpClient.newHttpClient().send(
                java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:"+port+"/api/health"))
                    .timeout(java.time.Duration.ofSeconds(10)).GET().build(), java.net.http.HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            System.out.println("UTC="+java.time.Instant.now()+" validation profile startup HTTP=200 Flyway=disabled readOnly=true DB_PASSWORD=absent ADMIN_DEV_PASSWORD=absent");
        }
    }
    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;
    @Test void migratesV1ToV4AndHibernateValidatesOnPostgres18() {
        flyway.validate();
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().applied()).hasSize(4);
        assertThat(jdbc.queryForObject("SHOW server_version_num",Integer.class)).isBetween(180000,189999);
        assertThat(jdbc.queryForObject("SELECT PostGIS_Version()",String.class)).startsWith("3.6");
        System.out.println("UTC="+java.time.Instant.now()+" PostgreSQL="
            +jdbc.queryForObject("SHOW server_version",String.class)
            +" PostGIS="+jdbc.queryForObject("SELECT PostGIS_Full_Version()",String.class)
            +" image="+postgres.getDockerImageName()+" Flyway V1-V4; Hibernate validate OK");
    }
}
