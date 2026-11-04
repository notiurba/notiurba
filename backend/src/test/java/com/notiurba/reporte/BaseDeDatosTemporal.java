package com.notiurba.reporte;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * Base PostgreSQL + PostGIS desechable para las pruebas de integración: se crea en el servidor del
 * proyecto (docker compose up -d), Flyway le aplica V1..V10 al arrancar Spring y se borra al final.
 * Si no están DB_URL, DB_USER y DB_PASSWORD, o no hay servidor, las pruebas se omiten solas.
 */
public final class BaseDeDatosTemporal {

    private final String nombre = "notiurba_it_" + Long.toHexString(System.nanoTime());
    private final String urlServidor;
    private final String usuario;
    private final String clave;

    private BaseDeDatosTemporal(String urlServidor, String usuario, String clave) {
        this.urlServidor = urlServidor;
        this.usuario = usuario;
        this.clave = clave;
    }

    public static BaseDeDatosTemporal crearOOmitir() {
        String url = System.getenv("DB_URL");
        String usuario = System.getenv("DB_USER");
        String clave = System.getenv("DB_PASSWORD");
        assumeTrue(url != null && !url.isBlank() && usuario != null && clave != null,
                "Faltan DB_URL, DB_USER y DB_PASSWORD: se omiten las pruebas con PostGIS");
        var base = new BaseDeDatosTemporal(url.substring(0, url.lastIndexOf('/')), usuario, clave);
        try (Connection c = DriverManager.getConnection(base.urlServidor + "/postgres", usuario, clave);
                Statement s = c.createStatement()) {
            s.execute("CREATE DATABASE " + base.nombre);
        } catch (SQLException e) {
            assumeTrue(false, "No hay PostgreSQL disponible en " + base.urlServidor + ": se omiten las pruebas con PostGIS");
        }
        return base;
    }

    public void registrar(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> urlServidor + "/" + nombre);
        registro.add("spring.datasource.username", () -> usuario);
        registro.add("spring.datasource.password", () -> clave);
        // Valor ficticio: V4 siembra el administrador con esta contraseña, solo en la base desechable
        registro.add("spring.flyway.placeholders.adminPassword", () -> "ClaveDePrueba-" + UUID.randomUUID());
    }

    public void borrar() throws SQLException {
        try (Connection c = DriverManager.getConnection(urlServidor + "/postgres", usuario, clave);
                Statement s = c.createStatement()) {
            s.execute("DROP DATABASE IF EXISTS " + nombre + " WITH (FORCE)");
        }
    }
}
