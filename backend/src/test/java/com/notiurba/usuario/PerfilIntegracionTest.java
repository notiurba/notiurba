package com.notiurba.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

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
import com.notiurba.reporte.BarrioDePrueba;
import com.notiurba.reporte.BaseDeDatosTemporal;
import com.notiurba.seguridad.JwtProveedor;

/** GET y PUT /api/usuarios/me contra PostgreSQL real (se omite sola sin Docker). */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:pruebas.properties")
class PerfilIntegracionTest {

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

    private int idBarrio;
    private String correo;
    private String documento;
    private String token;

    @BeforeEach
    void preparar() {
        idBarrio = BarrioDePrueba.id(jdbc);
        correo = "perfil-" + UUID.randomUUID() + "@prueba.com";
        documento = UUID.randomUUID().toString().substring(0, 12);
        jdbc.update("""
                INSERT INTO usuario (nombres, apellidos, id_tipo_documento, numero_documento, id_barrio,
                                     direccion_correspondencia, telefono, correo, correo_verificado_en, password_hash,
                                     id_rol, id_aviso_privacidad, acepto_privacidad_en)
                VALUES ('Ana', 'Gómez', 1, ?, ?, 'Cra 1 # 2-3', '3001234567', ?, now(), 'hash-de-prueba', 1, 1, now())""",
                documento, idBarrio, correo);
        token = jwt.generar(correo, "CIUDADANO");
    }

    private String cuerpo(String nombres, int tipo, String numero, int barrio, String direccion, String telefono) {
        return "{\"nombres\":\"" + nombres + "\",\"apellidos\":\"Gómez Rojas\",\"idTipoDocumento\":" + tipo
                + ",\"numeroDocumento\":\"" + numero + "\",\"idBarrio\":" + barrio
                + ",\"direccionCorrespondencia\":\"" + direccion + "\""
                + (telefono == null ? "" : ",\"telefono\":\"" + telefono + "\"") + "}";
    }

    private MvcResult guardar(String tokenQuien, String cuerpo) throws Exception {
        var peticion = put("/api/usuarios/me").contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        if (tokenQuien != null) {
            peticion = peticion.header("Authorization", "Bearer " + tokenQuien);
        }
        return mvc.perform(peticion).andReturn();
    }

    private JsonNode leer(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void devuelveElPerfilPropio() throws Exception {
        MvcResult r = mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + token)).andReturn();

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        JsonNode p = leer(r);
        assertThat(p.get("nombres").asText()).isEqualTo("Ana");
        assertThat(p.get("apellidos").asText()).isEqualTo("Gómez");
        assertThat(p.get("idTipoDocumento").asInt()).isEqualTo(1);
        assertThat(p.get("numeroDocumento").asText()).isEqualTo(documento);
        assertThat(p.get("correo").asText()).isEqualTo(correo);
        assertThat(p.get("telefono").asText()).isEqualTo("3001234567");
        assertThat(p.get("idBarrio").asInt()).isEqualTo(idBarrio);
        assertThat(p.get("direccionCorrespondencia").asText()).isEqualTo("Cra 1 # 2-3");
        assertThat(p.has("passwordHash")).isFalse();
    }

    @Test
    void sinSesionDa401() throws Exception {
        assertThat(mvc.perform(get("/api/usuarios/me")).andReturn().getResponse().getStatus()).isEqualTo(401);
        assertThat(guardar(null, cuerpo("Ana", 1, documento, idBarrio, "Cra 1", null)).getResponse().getStatus())
                .isEqualTo(401);
    }

    @Test
    void editaLosDatosYElCorreoNoCambia() throws Exception {
        String nuevoDoc = UUID.randomUUID().toString().substring(0, 12);

        MvcResult r = guardar(token, cuerpo("María Camila", 2, nuevoDoc, idBarrio, "Cra 26 #28-40", "3100000000"));

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        JsonNode p = leer(r);
        assertThat(p.get("nombres").asText()).isEqualTo("María Camila");
        assertThat(p.get("idTipoDocumento").asInt()).isEqualTo(2);
        assertThat(p.get("numeroDocumento").asText()).isEqualTo(nuevoDoc);
        assertThat(p.get("telefono").asText()).isEqualTo("3100000000");
        assertThat(p.get("correo").asText()).isEqualTo(correo);
        assertThat(jdbc.queryForObject("SELECT nombres FROM usuario WHERE correo = ?", String.class, correo))
                .isEqualTo("María Camila");
    }

    @Test
    void unTelefonoVacioLoBorra() throws Exception {
        MvcResult r = guardar(token, cuerpo("Ana", 1, documento, idBarrio, "Cra 1 # 2-3", ""));

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(leer(r).get("telefono").isNull()).isTrue();
        assertThat(jdbc.queryForObject("SELECT telefono FROM usuario WHERE correo = ?", String.class, correo)).isNull();
    }

    @Test
    void validaYMarcaElCampo() throws Exception {
        MvcResult sinNombre = guardar(token, cuerpo("   ", 1, documento, idBarrio, "Cra 1", null));
        assertThat(sinNombre.getResponse().getStatus()).isEqualTo(400);
        assertThat(leer(sinNombre).get("campo").asText()).isEqualTo("nombres");

        MvcResult tipoMalo = guardar(token, cuerpo("Ana", 99, documento, idBarrio, "Cra 1", null));
        assertThat(tipoMalo.getResponse().getStatus()).isEqualTo(400);
        assertThat(leer(tipoMalo).get("campo").asText()).isEqualTo("idTipoDocumento");

        MvcResult barrioMalo = guardar(token, cuerpo("Ana", 1, documento, 999999, "Cra 1", null));
        assertThat(barrioMalo.getResponse().getStatus()).isEqualTo(400);
        assertThat(leer(barrioMalo).get("campo").asText()).isEqualTo("idBarrio");
    }

    @Test
    void unDocumentoDeOtraCuentaDa409() throws Exception {
        String otroDoc = UUID.randomUUID().toString().substring(0, 12);
        jdbc.update("""
                INSERT INTO usuario (nombres, apellidos, id_tipo_documento, numero_documento, id_barrio,
                                     direccion_correspondencia, correo, correo_verificado_en, password_hash,
                                     id_rol, id_aviso_privacidad, acepto_privacidad_en)
                VALUES ('Luis', 'Pérez', 1, ?, ?, 'Cra 9', ?, now(), 'hash', 1, 1, now())""",
                otroDoc, idBarrio, "otro-" + UUID.randomUUID() + "@prueba.com");

        MvcResult r = guardar(token, cuerpo("Ana", 1, otroDoc, idBarrio, "Cra 1", null));

        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(leer(r).get("codigo").asText()).isEqualTo("DOCUMENTO_YA_REGISTRADO");
        assertThat(leer(r).get("campo").asText()).isEqualTo("numeroDocumento");
        // guardar con su mismo documento sí se puede
        assertThat(guardar(token, cuerpo("Ana", 1, documento, idBarrio, "Cra 1", null)).getResponse().getStatus())
                .isEqualTo(200);
    }

    @Test
    void noSePuedeElegirUnBarrioInternoPeroSiConservarlo() throws Exception {
        int interno = jdbc.queryForObject("SELECT id_barrio FROM barrio WHERE interno LIMIT 1", Integer.class);

        MvcResult elegirlo = guardar(token, cuerpo("Ana", 1, documento, interno, "Cra 1", null));
        assertThat(elegirlo.getResponse().getStatus()).isEqualTo(400);
        assertThat(leer(elegirlo).get("campo").asText()).isEqualTo("idBarrio");

        // el administrador vive en el barrio interno: puede guardar su perfil sin cambiarlo
        String admin = jdbc.queryForObject("SELECT correo FROM usuario WHERE id_barrio = ? LIMIT 1", String.class, interno);
        String tokenAdmin = jwt.generar(admin, "ADMIN");
        JsonNode suyo = leer(mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + tokenAdmin)).andReturn());
        MvcResult conservar = guardar(tokenAdmin, cuerpo(suyo.get("nombres").asText(), suyo.get("idTipoDocumento").asInt(),
                suyo.get("numeroDocumento").asText(), interno, suyo.get("direccionCorrespondencia").asText(), null));
        assertThat(conservar.getResponse().getStatus()).isEqualTo(200);
    }
}
