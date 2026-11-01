package com.notiurba.reporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.notiurba.seguridad.JwtProveedor;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * NOT-69 contra PostgreSQL + PostGIS reales (el contenedor del proyecto: docker compose up -d).
 * Crea una base desechable, aplica las migraciones V1..V10 con Flyway y la borra al terminar.
 * Se omite sola si no están DB_URL, DB_USER y DB_PASSWORD o si no se puede conectar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:pruebas.properties")
class ReporteCodigoIntegracionTest {

    private static final String NOMBRE_BD = "notiurba_it_" + Long.toHexString(System.nanoTime());
    private static String urlServidor;
    private static String usuarioBd;
    private static String claveBd;
    private static boolean creada;

    @BeforeAll
    static void crearBaseDesechable() {
        String url = System.getenv("DB_URL");
        usuarioBd = System.getenv("DB_USER");
        claveBd = System.getenv("DB_PASSWORD");
        assumeTrue(url != null && !url.isBlank() && usuarioBd != null && claveBd != null,
                "Faltan DB_URL, DB_USER y DB_PASSWORD: se omiten las pruebas con PostGIS");
        urlServidor = url.substring(0, url.lastIndexOf('/'));
        try (Connection c = DriverManager.getConnection(urlServidor + "/postgres", usuarioBd, claveBd);
                Statement s = c.createStatement()) {
            s.execute("CREATE DATABASE " + NOMBRE_BD);
            creada = true;
        } catch (SQLException e) {
            assumeTrue(false, "No hay PostgreSQL disponible en " + urlServidor + ": se omiten las pruebas con PostGIS");
        }
    }

    @AfterAll
    static void borrarBaseDesechable() throws SQLException {
        if (!creada) {
            return;
        }
        try (Connection c = DriverManager.getConnection(urlServidor + "/postgres", usuarioBd, claveBd);
                Statement s = c.createStatement()) {
            s.execute("DROP DATABASE IF EXISTS " + NOMBRE_BD + " WITH (FORCE)");
        }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> urlServidor + "/" + NOMBRE_BD);
        registro.add("spring.datasource.username", () -> usuarioBd);
        registro.add("spring.datasource.password", () -> claveBd);
        // Valor ficticio: V4 siembra el administrador con esta contraseña, solo en la base desechable
        registro.add("spring.flyway.placeholders.adminPassword", () -> "ClaveDePrueba-" + UUID.randomUUID());
    }

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtProveedor jwt;
    @Autowired private ObjectMapper json;

    private long idUsuario;
    private String token;
    private int idBarrio;

    @BeforeEach
    void crearCiudadano() {
        String correo = "ciudadano-" + UUID.randomUUID() + "@prueba.com";
        idBarrio = jdbc.queryForObject("SELECT min(id_barrio) FROM barrio", Integer.class);
        idUsuario = jdbc.queryForObject("""
                INSERT INTO usuario (nombres, apellidos, id_tipo_documento, numero_documento, id_barrio,
                                     direccion_correspondencia, correo, correo_verificado_en, password_hash,
                                     id_rol, id_aviso_privacidad, acepto_privacidad_en)
                VALUES ('Ana', 'Gómez', 1, ?, ?, 'Cra 1 # 2-3', ?, now(), 'hash-de-prueba', 1, 1, now())
                RETURNING id_usuario""", Long.class, UUID.randomUUID().toString().substring(0, 20), idBarrio, correo);
        token = jwt.generar(correo, "CIUDADANO");
    }

    private static final String CATEGORIA_SIN_UBICACION = "6";   // Uso del suelo, planeación y licencias
    private static final String CATEGORIA_CON_UBICACION = "1";   // Vías, huecos y andenes

    private MvcResult crear(String cuerpo) throws Exception {
        return mvc.perform(post("/api/reportes").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn();
    }

    private static String cuerpo(String idCategoria, String extras) {
        return "{\"idCategoria\":" + idCategoria + ",\"descripcion\":\"Hay un problema que necesita atención\","
                + "\"autorizaEnvio\":true" + extras + "}";
    }

    @Test
    void dosReportesConsecutivosTienenCodigosDistintosYLocationApuntaAlCodigoDeLaBase() throws Exception {
        MvcResult primero = crear(cuerpo(CATEGORIA_SIN_UBICACION, ""));
        MvcResult segundo = crear(cuerpo(CATEGORIA_SIN_UBICACION, ""));

        List<String> codigos = List.of(codigoDe(primero), codigoDe(segundo));
        assertThat(codigos.get(0)).isNotEqualTo(codigos.get(1)).matches("NU-\\d{6,}");
        assertThat(codigos.get(1)).matches("NU-\\d{6,}");
        // El código es lo que quedó guardado en la base, no algo calculado en Java
        assertThat(jdbc.queryForList("SELECT codigo_seguimiento FROM reporte WHERE id_usuario = ? ORDER BY id_reporte",
                String.class, idUsuario)).containsExactlyElementsOf(codigos);
        for (MvcResult r : List.of(primero, segundo)) {
            assertThat(r.getResponse().getStatus()).isEqualTo(201);
            assertThat(r.getResponse().getHeader("Location")).isEqualTo("/api/reportes/" + codigoDe(r));
            assertThat(json.readTree(r.getResponse().getContentAsString()).get("estado").asText()).isEqualTo("Recibido");
        }
        // NOT-69 pedía «máximo 10 caracteres»: hoy son 9 (hasta el reporte 999 999); la columna admite 20
        assertThat(codigos.get(0).length()).isLessThanOrEqualTo(10);
    }

    @Test
    void elTriggerAsignaElCodigoAntesDelInsert() {
        Long id = jdbc.queryForObject("""
                INSERT INTO reporte (id_usuario, id_categoria, descripcion, creado_en, autorizacion_envio_en)
                VALUES (?, 6, 'Descripción de prueba suficiente', now(), now())
                RETURNING id_reporte""", Long.class, idUsuario);
        String codigo = jdbc.queryForObject("SELECT codigo_seguimiento FROM reporte WHERE id_reporte = ?", String.class, id);

        assertThat(codigo).isEqualTo(String.format("NU-%06d", id));
    }

    @Test
    void laBaseRechazaUnCodigoRepetido() {
        String existente = jdbc.queryForObject("""
                INSERT INTO reporte (id_usuario, id_categoria, descripcion, creado_en, autorizacion_envio_en)
                VALUES (?, 6, 'Descripción de prueba suficiente', now(), now())
                RETURNING codigo_seguimiento""", String.class, idUsuario);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO reporte (id_usuario, id_categoria, descripcion, codigo_seguimiento, creado_en, autorizacion_envio_en)
                VALUES (?, 6, 'Otro reporte con el mismo código', ?, now(), now())""", idUsuario, existente))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("uq_reporte_codigo");
    }

    @Test
    void guardaElPuntoGpsComoPostGisYAceptaCategoriaConUbicacionConGpsOConBarrio() throws Exception {
        MvcResult conGps = crear(cuerpo(CATEGORIA_CON_UBICACION, ",\"latitud\":4.0867,\"longitud\":-76.1950"));
        MvcResult conBarrio = crear(cuerpo(CATEGORIA_CON_UBICACION, ",\"idBarrio\":" + idBarrio));

        assertThat(conGps.getResponse().getStatus()).isEqualTo(201);
        assertThat(conBarrio.getResponse().getStatus()).isEqualTo(201);
        Double lat = jdbc.queryForObject("SELECT ST_Y(ubicacion) FROM reporte WHERE codigo_seguimiento = ?",
                Double.class, codigoDe(conGps));
        assertThat(lat).isEqualTo(4.0867);
        assertThat(jdbc.queryForObject("SELECT ST_SRID(ubicacion) FROM reporte WHERE codigo_seguimiento = ?",
                Integer.class, codigoDe(conGps))).isEqualTo(4326);
    }

    @Test
    void erroresConElFormatoUnico() throws Exception {
        mvc.perform(post("/api/reportes").contentType(MediaType.APPLICATION_JSON).content(cuerpo("6", "")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));

        esperar400(crear("{\"descripcion\":\"Hay un problema que necesita atención\",\"autorizaEnvio\":true}"), "idCategoria");
        esperar400(crear(cuerpo("999", "")), "idCategoria");
        esperar400(crear(cuerpo(CATEGORIA_CON_UBICACION, "")), "idBarrio");                       // exige GPS o barrio
        esperar400(crear(cuerpo("6", ",\"latitud\":4.08")), "longitud");                             // GPS a medias
        esperar400(crear(cuerpo("6", ",\"matriculaCuentaServicio\":\"123\"")), "matriculaCuentaServicio");
        esperar400(crear("{\"idCategoria\":6,\"descripcion\":\"corta\",\"autorizaEnvio\":true}"), "descripcion");
        esperar400(crear("{\"idCategoria\":6,\"descripcion\":\"Hay un problema que necesita atención\",\"autorizaEnvio\":false}"), "autorizaEnvio");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reporte WHERE id_usuario = ?", Integer.class, idUsuario)).isZero();
    }

    private void esperar400(MvcResult r, String campo) throws Exception {
        assertThat(r.getResponse().getStatus()).isEqualTo(400);
        var cuerpo = json.readTree(r.getResponse().getContentAsString());
        assertThat(cuerpo.get("codigo").asText()).isEqualTo("VALIDACION");
        assertThat(cuerpo.get("campo").asText()).isEqualTo(campo);
        assertThat(cuerpo.get("mensaje").asText()).isNotBlank();
    }

    private String codigoDe(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString()).get("codigoSeguimiento").asText();
    }
}
