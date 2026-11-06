package com.notiurba.reporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notiurba.seguridad.JwtProveedor;

/** NOT-89: GET /api/reportes/{codigo} contra PostgreSQL + PostGIS reales (se omite sola sin Docker). */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:pruebas.properties")
class ReporteConsultaIntegracionTest {

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

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtProveedor jwt;
    @Autowired private ObjectMapper json;

    private long idUsuario;
    private String token;
    private int idBarrio;

    private record Ciudadano(long id, String token) {
    }

    private Ciudadano nuevoCiudadano() {
        String correo = "ciudadano-" + UUID.randomUUID() + "@prueba.com";
        long id = jdbc.queryForObject("""
                INSERT INTO usuario (nombres, apellidos, id_tipo_documento, numero_documento, id_barrio,
                                     direccion_correspondencia, correo, correo_verificado_en, password_hash,
                                     id_rol, id_aviso_privacidad, acepto_privacidad_en)
                VALUES ('Ana', 'Gómez', 1, ?, ?, 'Cra 1 # 2-3', ?, now(), 'hash-de-prueba', 1, 1, now())
                RETURNING id_usuario""", Long.class, UUID.randomUUID().toString().substring(0, 20), idBarrio, correo);
        return new Ciudadano(id, jwt.generar(correo, "CIUDADANO"));
    }

    @BeforeEach
    void preparar() {
        idBarrio = jdbc.queryForObject("SELECT min(id_barrio) FROM barrio", Integer.class);
        Ciudadano c = nuevoCiudadano();
        idUsuario = c.id();
        token = c.token();
    }

    private String crearReporte(String tokenDelAutor, String cuerpo) throws Exception {
        MvcResult r = mvc.perform(post("/api/reportes").header("Authorization", "Bearer " + tokenDelAutor)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("codigoSeguimiento").asText();
    }

    private MvcResult consultar(String tokenDeQuienPregunta, String codigo) throws Exception {
        var peticion = get("/api/reportes/" + codigo);
        if (tokenDeQuienPregunta != null) {
            peticion = peticion.header("Authorization", "Bearer " + tokenDeQuienPregunta);
        }
        return mvc.perform(peticion).andReturn();
    }

    private JsonNode cuerpo(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static final String CON_UBICACION = "{\"idCategoria\":1,\"latitud\":4.0867,\"longitud\":-76.1950,\"idBarrio\":%d,"
            + "\"descripcion\":\"Hay un hueco enorme frente al parque\",\"direccionIncidente\":\"Cra 5 # 10-20\","
            + "\"radicadoOficial\":\"RAD-2026-1\",\"autorizaEnvio\":true}";
    private static final String SIN_UBICACION = "{\"idCategoria\":6,\"descripcion\":\"Consulta sobre el uso del suelo\","
            + "\"autorizaEnvio\":true}";

    @Test
    void devuelveElReporteCompletoDelAutor() throws Exception {
        String codigo = crearReporte(token, CON_UBICACION.formatted(idBarrio));

        MvcResult r = consultar(token, codigo);

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        JsonNode j = cuerpo(r);
        assertThat(j.get("codigoSeguimiento").asText()).isEqualTo(codigo);
        assertThat(j.get("estado").asText()).isEqualTo("Recibido");
        assertThat(j.get("categoria").get("idCategoria").asInt()).isEqualTo(1);
        assertThat(j.get("categoria").get("nombre").asText()).isEqualTo("Vías, huecos y andenes");
        assertThat(j.get("descripcion").asText()).isEqualTo("Hay un hueco enorme frente al parque");
        assertThat(j.get("latitud").asDouble()).isEqualTo(4.0867);
        assertThat(j.get("longitud").asDouble()).isEqualTo(-76.1950);
        assertThat(j.get("barrio").get("idBarrio").asInt()).isEqualTo(idBarrio);
        // V14 carga la capa experimental: la comuna sale del polígono que contiene el punto (v_reporte_comuna)
        assertThat(j.get("comuna").asInt()).isEqualTo(jdbc.queryForObject(
                "SELECT id_comuna FROM fn_asignar_comuna(ST_SetSRID(ST_MakePoint(-76.1950, 4.0867), 4326))",
                Integer.class));
        assertThat(j.get("direccionIncidente").asText()).isEqualTo("Cra 5 # 10-20");
        assertThat(j.get("radicadoOficial").asText()).isEqualTo("RAD-2026-1");
        assertThat(j.get("matriculaCuentaServicio").isNull()).isTrue();
        assertThat(j.get("fotos").isArray()).isTrue();
        assertThat(j.get("fotos")).isEmpty();
        assertThat(j.get("vigenteHasta").asText()).isNotBlank();
        assertThat(j.get("historial")).hasSize(1);
        assertThat(j.get("historial").get(0).get("estado").asText()).isEqualTo("Recibido");
        assertThat(j.get("envio").isNull()).isTrue();
    }

    @Test
    void reporteSinUbicacionNiBarrioDevuelveNulosYAceptaElCodigoEnMinusculas() throws Exception {
        String codigo = crearReporte(token, SIN_UBICACION);

        JsonNode j = cuerpo(consultar(token, " " + codigo.toLowerCase() + " ".trim()));

        assertThat(j.get("codigoSeguimiento").asText()).isEqualTo(codigo);
        assertThat(j.get("latitud").isNull()).isTrue();
        assertThat(j.get("barrio").isNull()).isTrue();
        assertThat(j.get("comuna").isNull()).isTrue();
    }

    @Test
    void devuelveLasFotosEnOrdenYElUltimoEstadoDelEnvio() throws Exception {
        String codigo = crearReporte(token, SIN_UBICACION);
        long id = jdbc.queryForObject("SELECT id_reporte FROM reporte WHERE codigo_seguimiento = ?", Long.class, codigo);
        jdbc.update("INSERT INTO evidencia_reporte (id_reporte, id_usuario, url, orden) VALUES (?, ?, 'https://img.test/b.jpg', 2)", id, idUsuario);
        jdbc.update("INSERT INTO evidencia_reporte (id_reporte, id_usuario, url, orden) VALUES (?, ?, 'https://img.test/a.jpg', 1)", id, idUsuario);
        // Canal 1 = correo principal de la Alcaldía (el destino de la categoría 6)
        jdbc.update("INSERT INTO registro_envio_correo (id_reporte, id_canal_atencion, id_modo_envio, id_estado_envio, error, creado_en)"
                + " VALUES (?, 1, 1, 3, 'detalle técnico interno de Resend', now())", id);

        JsonNode j = cuerpo(consultar(token, codigo));

        assertThat(j.get("fotos")).hasSize(2);
        assertThat(j.get("fotos").get(0).get("orden").asInt()).isEqualTo(1);
        assertThat(j.get("fotos").get(0).get("url").asText()).isEqualTo("https://img.test/a.jpg");
        assertThat(j.get("fotos").get(1).get("url").asText()).isEqualTo("https://img.test/b.jpg");
        assertThat(j.get("envio").get("estado").asText()).isEqualTo("FALLIDO");
        // Nunca sale el texto técnico de la columna error
        assertThat(j.get("envio").get("motivo").asText()).isEqualTo("No pudimos entregar el correo a la entidad.")
                .doesNotContain("Resend");
    }

    @Test
    void elReporteDeOtraPersonaYUnCodigoInexistenteDanLaMismaRespuesta404() throws Exception {
        Ciudadano otra = nuevoCiudadano();
        String codigoAjeno = crearReporte(otra.token(), SIN_UBICACION);

        MvcResult ajeno = consultar(token, codigoAjeno);
        MvcResult inexistente = consultar(token, "NU-999999");

        assertThat(ajeno.getResponse().getStatus()).isEqualTo(404);
        assertThat(inexistente.getResponse().getStatus()).isEqualTo(404);
        assertThat(ajeno.getResponse().getContentAsString(StandardCharsets.UTF_8)).isEqualTo(inexistente.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode j = cuerpo(ajeno);
        assertThat(j.get("codigo").asText()).isEqualTo("NO_ENCONTRADO");
        assertThat(j.get("campo").isNull()).isTrue();
        assertThat(consultar(otra.token(), codigoAjeno).getResponse().getStatus()).isEqualTo(200);   // la dueña sí lo ve
    }

    @Test
    void codigoMalFormadoDa400ConElCampoYSinSesionDa401() throws Exception {
        for (String malo : new String[] { "123", "NU-12", "XX-000001", "NU-00000A" }) {
            MvcResult r = consultar(token, malo);
            assertThat(r.getResponse().getStatus()).as(malo).isEqualTo(400);
            assertThat(cuerpo(r).get("campo").asText()).isEqualTo("codigo");
            assertThat(cuerpo(r).get("codigo").asText()).isEqualTo("VALIDACION");
        }
        MvcResult sinSesion = consultar(null, "NU-000001");
        assertThat(sinSesion.getResponse().getStatus()).isEqualTo(401);
        assertThat(cuerpo(sinSesion).get("codigo").asText()).isEqualTo("NO_AUTENTICADO");
    }
}
