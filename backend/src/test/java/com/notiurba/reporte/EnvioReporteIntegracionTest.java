package com.notiurba.reporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notiurba.correo.CorreoNoEnviadoException;
import com.notiurba.correo.CorreoSaliente;
import com.notiurba.correo.EnviadorCorreo;
import com.notiurba.seguridad.JwtProveedor;

/**
 * POST /api/reportes/{codigo}/enviar contra PostgreSQL + PostGIS reales, con Resend simulado
 * (nunca se escribe a una entidad de verdad). Se omite sola sin Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:pruebas.properties", properties = "notiurba.correo.destino-prueba=")
class EnvioReporteIntegracionTest {

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
    @MockBean private EnviadorCorreo enviador;

    private int idBarrio;
    private String correo;
    private String token;

    private record Ciudadano(String correo, String token) {
    }

    private Ciudadano nuevoCiudadano() {
        String c = "ciudadano-" + UUID.randomUUID() + "@prueba.com";
        jdbc.update("""
                INSERT INTO usuario (nombres, apellidos, id_tipo_documento, numero_documento, id_barrio,
                                     direccion_correspondencia, correo, correo_verificado_en, password_hash,
                                     id_rol, id_aviso_privacidad, acepto_privacidad_en)
                VALUES ('Ana', 'Gómez', 1, ?, ?, 'Cra 1 # 2-3', ?, now(), 'hash-de-prueba', 1, 1, now())""",
                UUID.randomUUID().toString().substring(0, 20), idBarrio, c);
        return new Ciudadano(c, jwt.generar(c, "CIUDADANO"));
    }

    @BeforeEach
    void preparar() {
        reset(enviador);
        idBarrio = BarrioDePrueba.id(jdbc);
        Ciudadano c = nuevoCiudadano();
        correo = c.correo();
        token = c.token();
    }

    private String crearReporte(String tokenDelAutor, int idCategoria, String extra) throws Exception {
        String cuerpo = "{\"idCategoria\":" + idCategoria + ",\"latitud\":4.0867,\"longitud\":-76.1950,\"idBarrio\":" + idBarrio
                + ",\"descripcion\":\"Hay un hueco enorme frente al parque\",\"direccionIncidente\":\"Cra 5 # 10-20\","
                + "\"autorizaEnvio\":true" + extra + "}";
        MvcResult r = mvc.perform(post("/api/reportes").header("Authorization", "Bearer " + tokenDelAutor)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        return cuerpo(r).get("codigoSeguimiento").asText();
    }

    private JsonNode cuerpo(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private MvcResult enviar(String tokenDeQuien, String codigo) throws Exception {
        var peticion = post("/api/reportes/" + codigo + "/enviar");
        if (tokenDeQuien != null) {
            peticion = peticion.header("Authorization", "Bearer " + tokenDeQuien);
        }
        return mvc.perform(peticion).andReturn();
    }

    private JsonNode consultar(String codigo) throws Exception {
        return cuerpo(mvc.perform(get("/api/reportes/" + codigo).header("Authorization", "Bearer " + token)).andReturn());
    }

    private List<String> estadosDeEnvio(String codigo) {
        return jdbc.queryForList("""
                SELECT s.nombre FROM registro_envio_correo e
                  JOIN estado_envio s USING (id_estado_envio) JOIN reporte r USING (id_reporte)
                 WHERE r.codigo_seguimiento = ? ORDER BY e.id_registro_envio""", String.class, codigo);
    }

    @Test
    void enviaElCorreoALaEntidadYElReportePasaAEnviado() throws Exception {
        when(enviador.enviar(any())).thenReturn("re_abc123");
        String codigo = crearReporte(token, 1, "");

        MvcResult r = enviar(token, codigo);

        assertThat(r.getResponse().getStatus()).isEqualTo(202);
        assertThat(cuerpo(r).get("estado").asText()).isEqualTo("PENDIENTE");

        ArgumentCaptor<CorreoSaliente> enviado = ArgumentCaptor.forClass(CorreoSaliente.class);
        verify(enviador, times(1)).enviar(enviado.capture());
        CorreoSaliente c = enviado.getValue();
        assertThat(c.para()).containsExactly("servicioalciudadano@tulua.gov.co");
        assertThat(c.asunto()).isEqualTo("NotiUrba #" + codigo);
        assertThat(c.responderA()).isEqualTo(correo);
        assertThat(c.texto()).contains("Hay un hueco enorme frente al parque", "Ana Gómez", "CC ");

        assertThat(estadosDeEnvio(codigo)).containsExactly("ENVIADO");
        assertThat(jdbc.queryForObject("""
                SELECT e.id_envio_proveedor FROM registro_envio_correo e JOIN reporte r USING (id_reporte)
                 WHERE r.codigo_seguimiento = ?""", String.class, codigo)).isEqualTo("re_abc123");
        JsonNode detalle = consultar(codigo);
        assertThat(detalle.get("estado").asText()).isEqualTo("Enviado");
        assertThat(detalle.get("envio").get("estado").asText()).isEqualTo("ENVIADO");
        assertThat(detalle.get("historial")).hasSize(2);
    }

    @Test
    void pedirloDeNuevoNoRepiteElCorreo() throws Exception {
        when(enviador.enviar(any())).thenReturn("re_1");
        String codigo = crearReporte(token, 1, "");
        enviar(token, codigo);

        MvcResult segunda = enviar(token, codigo);

        assertThat(segunda.getResponse().getStatus()).isEqualTo(202);
        assertThat(cuerpo(segunda).get("estado").asText()).isEqualTo("ENVIADO");
        verify(enviador, times(1)).enviar(any());
        assertThat(estadosDeEnvio(codigo)).containsExactly("ENVIADO");
    }

    @Test
    void siFallaQuedaFallidoElReporteSeConservaYSePuedeReintentar() throws Exception {
        when(enviador.enviar(any())).thenThrow(new CorreoNoEnviadoException("Resend rechazó el envío (HTTP 500)", true));
        String codigo = crearReporte(token, 1, "");

        assertThat(enviar(token, codigo).getResponse().getStatus()).isEqualTo(202);

        JsonNode tras = consultar(codigo);
        assertThat(tras.get("estado").asText()).isEqualTo("Recibido");
        assertThat(tras.get("envio").get("estado").asText()).isEqualTo("FALLIDO");
        assertThat(tras.get("envio").get("motivo").asText()).isEqualTo("No pudimos entregar el correo a la entidad.");
        assertThat(estadosDeEnvio(codigo)).containsExactly("FALLIDO");

        doReturn("re_2").when(enviador).enviar(any());   // when(...) volvería a llamar al simulador que lanza
        assertThat(enviar(token, codigo).getResponse().getStatus()).isEqualTo(202);

        assertThat(estadosDeEnvio(codigo)).containsExactly("FALLIDO", "ENVIADO");
        assertThat(consultar(codigo).get("estado").asText()).isEqualTo("Enviado");
    }

    @Test
    void soloElAutorPuedeEnviarYSeExigeSesion() throws Exception {
        String codigo = crearReporte(token, 1, "");
        Ciudadano otro = nuevoCiudadano();

        MvcResult ajeno = enviar(otro.token(), codigo);
        assertThat(ajeno.getResponse().getStatus()).isEqualTo(404);
        assertThat(cuerpo(ajeno).get("codigo").asText()).isEqualTo("NO_ENCONTRADO");
        assertThat(enviar(null, codigo).getResponse().getStatus()).isEqualTo(401);
        assertThat(enviar(token, "XX-1").getResponse().getStatus()).isEqualTo(400);
        verify(enviador, times(0)).enviar(any());
    }

    @Test
    void unaCategoriaSinCorreoDeDestinoDa422YNoCreaEnvio() throws Exception {
        int gas = jdbc.queryForObject("""
                SELECT c.id_categoria FROM categoria c JOIN dependencia d USING (id_dependencia)
                 WHERE d.id_entidad_destino = 3 ORDER BY c.id_categoria LIMIT 1""", Integer.class);
        String codigo = crearReporte(token, gas, ",\"matriculaCuentaServicio\":\"123456\"");

        MvcResult r = enviar(token, codigo);

        assertThat(r.getResponse().getStatus()).isEqualTo(422);
        assertThat(cuerpo(r).get("codigo").asText()).isEqualTo("SIN_DESTINO_DE_CORREO");
        assertThat(estadosDeEnvio(codigo)).isEmpty();
        verify(enviador, times(0)).enviar(any());
    }
}
