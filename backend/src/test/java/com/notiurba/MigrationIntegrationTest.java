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
