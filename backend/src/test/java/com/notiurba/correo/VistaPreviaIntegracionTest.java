package com.notiurba.correo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notiurba.reporte.BarrioDePrueba;
import com.notiurba.reporte.BaseDeDatosTemporal;
import com.notiurba.reporte.DatosDePrueba;
import com.notiurba.seguridad.JwtProveedor;

/** La vista previa no escribe nada y dice lo mismo que el correo que se encola. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:pruebas.properties")
class VistaPreviaIntegracionTest {

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

    private static final double LATITUD = 4.0867;
    private static final double LONGITUD = -76.1950;

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtProveedor jwt;
    @Autowired private ObjectMapper json;

    private String token;
    private int idBarrio;
    private long idUsuario;

    @BeforeEach
    void crearCiudadano() {
        String correo = "ciudadano-" + UUID.randomUUID() + "@prueba.com";
        idBarrio = BarrioDePrueba.id(jdbc);
        idUsuario = DatosDePrueba.usuario(jdbc, idBarrio, correo, DatosDePrueba.CIUDADANO);
        token = jwt.generar(correo, "CIUDADANO");
    }

    private MvcResult hacer(MockHttpServletRequestBuilder peticion, String sesion) throws Exception {
        if (sesion != null) peticion.header("Authorization", "Bearer " + sesion);
        return mvc.perform(peticion).andReturn();
    }

    private MvcResult vistaPrevia(String cuerpo) throws Exception {
        return hacer(post("/api/reportes/vista-previa").contentType(MediaType.APPLICATION_JSON).content(cuerpo), token);
    }

    private JsonNode leer(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String cuerpoVias(String extras) {
        return "{\"idCategoria\":1,\"latitud\":" + LATITUD + ",\"longitud\":" + LONGITUD + ",\"idBarrio\":" + idBarrio
                + ",\"descripcion\":\"Hay un hueco enorme frente al parque\"" + extras + "}";
    }

    private Map<String, Object> conteos() {
        return jdbc.queryForMap("""
                SELECT (SELECT count(*) FROM reporte) AS reportes, (SELECT count(*) FROM registro_envio_correo) AS envios,
                       (SELECT count(*) FROM evidencia_reporte) AS fotos""");
    }

    @Test
    void respondeElMensajeYNoCreaReporteNiEnvioNiFotos() throws Exception {
        var antes = conteos();

        MvcResult r = vistaPrevia(cuerpoVias(",\"totalFotos\":2"));

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        JsonNode cuerpo = leer(r);
        int comuna = jdbc.queryForObject("""
                SELECT id_comuna FROM fn_asignar_comuna(ST_SetSRID(ST_MakePoint(?, ?), 4326))""",
                Integer.class, LONGITUD, LATITUD);
        assertThat(cuerpo.get("asunto").asText()).startsWith("Petición ciudadana – ")
                .endsWith("Barrio de prueba, Comuna " + comuna);
        assertThat(cuerpo.get("texto").asText()).startsWith("Respetados señores:")
                .contains("Hechos: Hay un hueco enorme frente al parque", "Evidencia: 2 fotografías.",
                        " · 4.08670, -76.19500\n", "Yo, Ana Gómez, con cédula de ciudadanía No. ")
                .doesNotContain("NotiUrba", "NU-");
        assertThat(cuerpo.get("permiteCorreo").asBoolean()).isTrue();
        assertThat(cuerpo.at("/ruta/acceso").asText()).isEqualTo("servicioalciudadano@tulua.gov.co");
        assertThat(cuerpo.get("secciones")).anySatisfy(s -> {
            assertThat(s.get("id").asText()).isEqualTo("ubicacion");
            assertThat(s.get("paso").asText()).isEqualTo("ubicacion");
        });
        assertThat(cuerpo.has("html")).isFalse();
        assertThat(conteos()).isEqualTo(antes);
    }

    @Test
    void sinGpsLaComunaSaleDelBarrioYLaSolicitudUsaLaPlantillaB() throws Exception {
        MvcResult r = vistaPrevia("{\"idCategoria\":6,\"idBarrio\":" + idBarrio
                + ",\"descripcion\":\"Quiero saber si puedo construir un segundo piso\"}");

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        JsonNode cuerpo = leer(r);
        assertThat(cuerpo.get("asunto").asText()).startsWith("Solicitud ciudadana – ");
        assertThat(cuerpo.get("texto").asText()).contains("Solicitud: Quiero saber si puedo construir",
                "Ubicación: barrio Barrio de prueba, Comuna 1\n").doesNotContain("openstreetmap");
    }

    @Test
    void lasCategoriasSinCorreoTambienRespondenConPermiteCorreoFalso() throws Exception {
        MvcResult r = vistaPrevia("{\"idCategoria\":14,\"idBarrio\":" + idBarrio
                + ",\"descripcion\":\"No pasó el carro de la basura esta semana\"}");

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(leer(r).get("permiteCorreo").asBoolean()).isFalse();
        assertThat(leer(r).at("/ruta/canales/0/acceso").asText()).isEqualTo("317 372 5078");
    }

    @Test
    void losDatosNoValidosDan400ConElCampo() throws Exception {
        record Caso(String cuerpo, String campo) { }
        for (Caso c : new Caso[] {
                new Caso("{\"idCategoria\":1,\"idBarrio\":" + idBarrio + "}", "descripcion"),
                new Caso(cuerpoVias(",\"totalFotos\":4"), "totalFotos"),
                new Caso(cuerpoVias(",\"matriculaCuentaServicio\":\"123\""), "matriculaCuentaServicio"),
                new Caso("{\"idCategoria\":1,\"descripcion\":\"Hay un hueco enorme frente al parque\"}", "idBarrio"),
                new Caso("{\"idCategoria\":999,\"descripcion\":\"Hay un hueco enorme frente al parque\"}", "idCategoria")}) {
            MvcResult r = vistaPrevia(c.cuerpo());

            assertThat(r.getResponse().getStatus()).as(c.cuerpo()).isEqualTo(400);
            assertThat(leer(r).get("codigo").asText()).isEqualTo("VALIDACION");
            assertThat(leer(r).get("campo").asText()).as(c.cuerpo()).isEqualTo(c.campo());
        }
    }

    @Test
    void sinSesionDa401() throws Exception {
        assertThat(hacer(post("/api/reportes/vista-previa").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoVias("")), null).getResponse().getStatus()).isEqualTo(401);
        assertThat(hacer(get("/api/reportes/NU-000001/mensaje"), null).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void laVistaPreviaElMensajeDelReporteYElCorreoEncoladoDicenLoMismo() throws Exception {
        String previa = leer(vistaPrevia(cuerpoVias(",\"totalFotos\":2"))).get("texto").asText();
        MvcResult creado = hacer(post("/api/reportes").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoVias(",\"autorizaEnvio\":true,\"confirmarDuplicado\":true")), token);
        String codigo = leer(creado).get("codigoSeguimiento").asText();
        var reporte = new DatosDePrueba.Reporte(jdbc.queryForObject(
                "SELECT id_reporte FROM reporte WHERE codigo_seguimiento = ?", Long.class, codigo), codigo);
        DatosDePrueba.foto(jdbc, reporte, idUsuario, 1, "notiurba/pruebas/uno");
        DatosDePrueba.foto(jdbc, reporte, idUsuario, 2, "notiurba/pruebas/dos");

        JsonNode antesDeEnviar = leer(hacer(get("/api/reportes/" + codigo + "/mensaje"), token));
        assertThat(hacer(post("/api/reportes/" + codigo + "/enviar"), token).getResponse().getStatus()).isEqualTo(202);
        String encolado = json.readValue(jdbc.queryForObject("""
                SELECT e.mensaje_json FROM registro_envio_correo e JOIN reporte r USING (id_reporte)
                 WHERE r.codigo_seguimiento = ?""", String.class, codigo), CorreoSaliente.class).texto();
        JsonNode despuesDeEnviar = leer(hacer(get("/api/reportes/" + codigo + "/mensaje"), token));

        assertThat(encolado).contains("Fotografía 1: https://res.cloudinary.com/", "Fotografía 2: ");
        assertThat(comparable(antesDeEnviar.get("texto").asText())).isEqualTo(comparable(previa));
        assertThat(despuesDeEnviar.get("texto").asText()).isEqualTo(encolado);
        assertThat(comparable(encolado)).isEqualTo(comparable(previa));
        assertThat(despuesDeEnviar.get("asunto").asText()).isEqualTo(antesDeEnviar.get("asunto").asText());
    }

    @Test
    void elMensajeDeUnReporteAjenoDa404() throws Exception {
        String otro = "otro-" + UUID.randomUUID() + "@prueba.com";
        long idOtro = DatosDePrueba.usuario(jdbc, idBarrio, otro, DatosDePrueba.CIUDADANO);
        var ajeno = DatosDePrueba.reporte(jdbc, idOtro, 1, idBarrio, 0);

        MvcResult r = hacer(get("/api/reportes/" + ajeno.codigo() + "/mensaje"), token);

        assertThat(r.getResponse().getStatus()).isEqualTo(404);
        assertThat(r.getResponse().getContentAsString(StandardCharsets.UTF_8)).doesNotContain("Respetados", "Hueco");
        assertThat(hacer(get("/api/reportes/" + ajeno.codigo() + "/mensaje"), jwt.generar(otro, "CIUDADANO"))
                .getResponse().getStatus()).isEqualTo(200);
    }

    /**
     * La vista previa usa la hora de ahora (el reporte, la de la base) y aún no tiene los enlaces de las
     * fotos: lo demás debe ser idéntico.
     */
    private static String comparable(String texto) {
        return texto.replaceAll("Fecha del registro: [^\\n]*", "Fecha del registro: —")
                .replaceAll("Fotografía \\d+: [^\\n]*\\n", "");
    }

    @Test
    void unPuntoFueraDeTuluaDa422() throws Exception {
        MvcResult r = vistaPrevia("{\"idCategoria\":1,\"latitud\":4.6,\"longitud\":-74.08,"
                + "\"descripcion\":\"Hay un hueco enorme frente al parque\"}");

        assertThat(r.getResponse().getStatus()).isEqualTo(422);
        assertThat(leer(r).get("campo").asText()).isEqualTo("latitud");
    }

    @Test
    void unaEmergenciaNoVaPorCorreoYUnOlorAGasSeTrataComoEmergencia() throws Exception {
        JsonNode marcada = leer(vistaPrevia(cuerpoVias(",\"emergencia\":true")));
        JsonNode gas = leer(vistaPrevia("{\"idCategoria\":12,\"idBarrio\":" + idBarrio
                + ",\"descripcion\":\"Hay olor a gas en la esquina del parque\"}"));

        assertThat(marcada.get("permiteCorreo").asBoolean()).isFalse();
        assertThat(gas.get("permiteCorreo").asBoolean()).isFalse();
        assertThat(gas.at("/ruta/acceso").asText()).isEqualTo("164");
    }

    @Test
    void elAcueductoUsaLaPlantillaCConLaMatricula() throws Exception {
        JsonNode cuerpo = leer(vistaPrevia("{\"idCategoria\":11,\"idBarrio\":" + idBarrio
                + ",\"descripcion\":\"No llega el agua desde ayer en la tarde\",\"matriculaCuentaServicio\":\"778899\"}"));

        assertThat(cuerpo.get("asunto").asText()).startsWith("Petición ciudadana – ").doesNotContain("Comuna");
        assertThat(cuerpo.get("texto").asText()).startsWith("Señores\n")
                .contains("en calidad de usuario del servicio de ", "(matrícula o cuenta No. 778899)");
        assertThat(cuerpo.at("/ruta/acceso").asText()).isEqualTo("info@centroaguas.com");
    }

    @Test
    void unCodigoMalEscritoDa400ConElCampo() throws Exception {
        MvcResult r = hacer(get("/api/reportes/abc/mensaje"), token);

        assertThat(r.getResponse().getStatus()).isEqualTo(400);
        assertThat(leer(r).get("campo").asText()).isEqualTo("codigo");
    }

    @Test
    void unMensajeViejoSeDevuelveTalCualYUnoIlegibleSeVuelveAArmar() throws Exception {
        var viejo = DatosDePrueba.reporte(jdbc, idUsuario, 1, idBarrio, 0);
        var ilegible = DatosDePrueba.reporte(jdbc, idUsuario, 1, idBarrio, 0);
        encolar(viejo, json.writeValueAsString(new CorreoSaliente(java.util.List.of("servicioalciudadano@tulua.gov.co"),
                "NotiUrba #" + viejo.codigo(), "Formato anterior", null, "Ana Gómez", "ana@prueba.com", "clave-vieja")));
        encolar(ilegible, "{}");

        JsonNode deViejo = leer(hacer(get("/api/reportes/" + viejo.codigo() + "/mensaje"), token));
        MvcResult deIlegible = hacer(get("/api/reportes/" + ilegible.codigo() + "/mensaje"), token);

        assertThat(deViejo.get("asunto").asText()).isEqualTo("NotiUrba #" + viejo.codigo());
        assertThat(deViejo.get("texto").asText()).isEqualTo("Formato anterior");
        assertThat(deIlegible.getResponse().getStatus()).isEqualTo(200);
        assertThat(leer(deIlegible).get("texto").asText()).startsWith("Respetados señores:");
    }

    private void encolar(DatosDePrueba.Reporte reporte, String mensajeJson) {
        jdbc.update("""
                INSERT INTO registro_envio_correo (id_reporte, id_canal_atencion, id_modo_envio, id_estado_envio, destino,
                                                   clave_idempotencia, mensaje_json)
                VALUES (?, 1, (SELECT id_modo_envio FROM modo_envio WHERE nombre = 'RESEND'),
                        (SELECT id_estado_envio FROM estado_envio WHERE nombre = 'PENDIENTE'),
                        'servicioalciudadano@tulua.gov.co', ?, ?)""", reporte.id(), "clave-" + UUID.randomUUID(), mensajeJson);
    }
}
