package com.notiurba.reporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

/**
 * PATCH /api/reportes/{codigo}/actualizacion: el aviso del ciudadano queda en el historial SIN cambiar el
 * estado oficial, y el radicado se guarda en el reporte. Contra PostgreSQL real (se omite sola sin Docker).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:pruebas.properties")
class ActualizacionReporteIntegracionTest {

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
    private String token;

    private String nuevoCiudadano() {
        String correo = "ciudadano-" + UUID.randomUUID() + "@prueba.com";
        jdbc.update("""
                INSERT INTO usuario (nombres, apellidos, id_tipo_documento, numero_documento, id_barrio,
                                     direccion_correspondencia, correo, correo_verificado_en, password_hash,
                                     id_rol, id_aviso_privacidad, acepto_privacidad_en)
                VALUES ('Ana', 'Gómez', 1, ?, ?, 'Cra 1 # 2-3', ?, now(), 'hash-de-prueba', 1, 1, now())""",
                UUID.randomUUID().toString().substring(0, 20), idBarrio, correo);
        return jwt.generar(correo, "CIUDADANO");
    }

    @BeforeEach
    void preparar() {
        idBarrio = BarrioDePrueba.id(jdbc);
        token = nuevoCiudadano();
    }

    private String crearReporte() throws Exception {
        String cuerpo = "{\"idCategoria\":1,\"latitud\":4.0867,\"longitud\":-76.1950,\"idBarrio\":" + idBarrio
                + ",\"descripcion\":\"Hay un hueco enorme frente al parque\",\"autorizaEnvio\":true,\"confirmarDuplicado\":true}";
        MvcResult r = mvc.perform(post("/api/reportes").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        return leer(r).get("codigoSeguimiento").asText();
    }

    private MvcResult actualizar(String tokenQuien, String codigo, String cuerpo) throws Exception {
        var peticion = patch("/api/reportes/" + codigo + "/actualizacion")
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        if (tokenQuien != null) {
            peticion = peticion.header("Authorization", "Bearer " + tokenQuien);
        }
        return mvc.perform(peticion).andReturn();
    }

    private JsonNode leer(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void meRespondieronGuardaElRadicadoYUnaNotaSinCambiarElEstado() throws Exception {
        String codigo = crearReporte();

        MvcResult r = actualizar(token, codigo, "{\"tipo\":\"RESPUESTA\",\"radicado\":\" GAC-003660-26 \"}");

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        JsonNode reporte = leer(r);
        assertThat(reporte.get("radicadoOficial").asText()).isEqualTo("GAC-003660-26");
        assertThat(reporte.get("estado").asText()).isEqualTo("Recibido");           // el estado oficial no se movió
        JsonNode historial = reporte.get("historial");
        assertThat(historial).hasSize(2);
        assertThat(historial.get(1).get("estado").asText()).isEqualTo("Recibido");
        assertThat(historial.get(1).get("observacion").asText())
                .contains("la entidad le respondió").contains("Radicado: GAC-003660-26");
        // la nota queda a nombre del ciudadano que la escribió
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM historial_estado h JOIN reporte r USING (id_reporte)
                 WHERE r.codigo_seguimiento = ? AND h.id_usuario_cambia IS NOT NULL""", Integer.class, codigo))
                .isEqualTo(1);
    }

    @Test
    void yaSeResolvioNoPasaElReporteAResuelto() throws Exception {
        String codigo = crearReporte();

        MvcResult r = actualizar(token, codigo, "{\"tipo\":\"RESUELTO\",\"radicado\":\"\"}");

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        JsonNode reporte = leer(r);
        assertThat(reporte.get("estado").asText()).isEqualTo("Recibido");
        assertThat(reporte.get("radicadoOficial").isNull()).isTrue();               // vacío = no se guarda nada
        assertThat(reporte.get("historial").get(1).get("observacion").asText())
                .contains("ya se resolvió").contains("lo confirma el administrador");
    }

    @Test
    void unRadicadoVacioNoBorraElQueYaHabia() throws Exception {
        String codigo = crearReporte();
        actualizar(token, codigo, "{\"tipo\":\"RESPUESTA\",\"radicado\":\"RAD-1\"}");

        JsonNode reporte = leer(actualizar(token, codigo, "{\"tipo\":\"RESPUESTA\"}"));

        assertThat(reporte.get("radicadoOficial").asText()).isEqualTo("RAD-1");
    }

    @Test
    void validaElTipoYElRadicado() throws Exception {
        String codigo = crearReporte();

        MvcResult sinTipo = actualizar(token, codigo, "{\"radicado\":\"X\"}");
        assertThat(sinTipo.getResponse().getStatus()).isEqualTo(400);

        MvcResult tipoMalo = actualizar(token, codigo, "{\"tipo\":\"CANCELADO\"}");
        assertThat(tipoMalo.getResponse().getStatus()).isEqualTo(400);
        assertThat(leer(tipoMalo).get("campo").asText()).isEqualTo("tipo");

        MvcResult largo = actualizar(token, codigo, "{\"tipo\":\"RESPUESTA\",\"radicado\":\"" + "9".repeat(51) + "\"}");
        assertThat(largo.getResponse().getStatus()).isEqualTo(400);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM historial_estado h JOIN reporte r USING (id_reporte)
                 WHERE r.codigo_seguimiento = ?""", Integer.class, codigo)).isEqualTo(1);   // nada se guardó
    }

    @Test
    void soloElAutorPuedeActualizarYSeExigeSesion() throws Exception {
        String codigo = crearReporte();

        MvcResult ajeno = actualizar(nuevoCiudadano(), codigo, "{\"tipo\":\"RESPUESTA\"}");
        assertThat(ajeno.getResponse().getStatus()).isEqualTo(404);
        assertThat(leer(ajeno).get("codigo").asText()).isEqualTo("NO_ENCONTRADO");
        assertThat(actualizar(null, codigo, "{\"tipo\":\"RESPUESTA\"}").getResponse().getStatus()).isEqualTo(401);
    }
}
