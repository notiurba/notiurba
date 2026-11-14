package com.notiurba.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notiurba.reporte.BarrioDePrueba;
import com.notiurba.reporte.BaseDeDatosTemporal;
import com.notiurba.seguridad.JwtProveedor;

/**
 * NOT-55: las reglas de la comuna (contiene, solape, hueco cercano, fuera de cobertura) contra PostGIS
 * real, con tres comunas inventadas que sí tienen un hueco y un solape (la capa de NOT-31 no tiene
 * ninguno). Se omite sola sin base de datos.
 *
 * Plano de la prueba, en unidades u = 0,001° (unos 111 m), con origen cerca de Tuluá:
 * <pre>
 *   y 1,5                        +----------+
 *   y 1,0   +-----+              |  3       |
 *           |  1  |  hueco   +---+--+       |
 *   y 0,5   |     |  (0,5 u) | 2 +--+-------+   (2 y 3 se solapan en x 2,0–2,5 / y 0,5–1,0)
 *   y 0     +-----+          +------+
 *           x0  x1           1,5    2,5   3,0
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:pruebas.properties")
class AsignacionDeComunaIntegracionTest {

    private static final double LON0 = -76.2000;
    private static final double LAT0 = 4.0800;
    private static final double U = 0.001;

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

    private String token;

    @BeforeEach
    void prepararComunasYCiudadano() {
        jdbc.update("UPDATE comuna SET geom = NULL");
        poner(1, 0.0, 1.0, 0.0, 1.0);
        poner(2, 1.5, 2.5, 0.0, 1.0);
        poner(3, 2.0, 3.0, 0.5, 1.5);
        jdbc.update("UPDATE configuracion SET valor = '50' WHERE clave = 'comuna_distancia_maxima_m'");
        jdbc.update("UPDATE configuracion SET valor = '50' WHERE clave = 'reportes_maximos_por_dia'");

        int idBarrio = BarrioDePrueba.id(jdbc);
        String correo = "ciudadano-" + UUID.randomUUID() + "@prueba.com";
        jdbc.queryForObject("""
                INSERT INTO usuario (nombres, apellidos, id_tipo_documento, numero_documento, id_barrio,
                                     direccion_correspondencia, correo, correo_verificado_en, password_hash,
                                     id_rol, id_aviso_privacidad, acepto_privacidad_en)
                VALUES ('Ana', 'Gómez', 1, ?, ?, 'Cra 1 # 2-3', ?, now(), 'hash-de-prueba', 1, 1, now())
                RETURNING id_usuario""", Long.class, UUID.randomUUID().toString().substring(0, 20), idBarrio, correo);
        token = jwt.generar(correo, "CIUDADANO");
    }

    private void poner(int idComuna, double x0, double x1, double y0, double y1) {
        String wkt = String.format(Locale.ROOT, "POLYGON((%s %s,%s %s,%s %s,%s %s,%s %s))",
                lon(x0), lat(y0), lon(x1), lat(y0), lon(x1), lat(y1), lon(x0), lat(y1), lon(x0), lat(y0));
        jdbc.update("UPDATE comuna SET geom = ST_Multi(ST_GeomFromText(?, 4326)) WHERE id_comuna = ?", wkt, idComuna);
    }

    private static double lon(double x) {
        return LON0 + x * U;
    }

    private static double lat(double y) {
        return LAT0 + y * U;
    }

    /** Lo que decide fn_asignar_comuna para el punto (x, y) del plano: vacío = ninguna. */
    private List<Map<String, Object>> asignar(double x, double y, Double maximoMetros) {
        return jdbc.query("""
                SELECT id_comuna, regla, distancia_m
                  FROM fn_asignar_comuna(ST_SetSRID(ST_MakePoint(?, ?), 4326), ?)""",
                (rs, i) -> Map.<String, Object>of("id_comuna", rs.getInt("id_comuna"), "regla", rs.getString("regla"),
                        "distancia_m", rs.getDouble("distancia_m")),
                lon(x), lat(y), maximoMetros);
    }

    private Map<String, Object> unica(double x, double y) {
        List<Map<String, Object>> filas = asignar(x, y, null);
        assertThat(filas).hasSize(1);
        return filas.get(0);
    }

    @Test
    void unPuntoDentroDeUnaComunaSeAsignaAEsa() {
        assertThat(unica(0.5, 0.5)).containsEntry("id_comuna", 1).containsEntry("regla", "contiene");
        assertThat(unica(2.0, 0.2)).containsEntry("id_comuna", 2).containsEntry("regla", "contiene");
    }

    @Test
    void elBordeDeUnaComunaCuentaComoDentro() {
        assertThat(unica(1.0, 0.5)).containsEntry("id_comuna", 1).containsEntry("regla", "contiene");
    }

    @Test
    void unPuntoEnUnSolapeVaALaComunaDeMenorNumero() {
        assertThat(unica(2.2, 0.7)).containsEntry("id_comuna", 2).containsEntry("regla", "solape");
    }

    @Test
    void unPuntoEnUnHuecoVaALaComunaMasCercanaSiEstaDentroDeLaDistanciaMaxima() {
        // hueco entre 1 (x 1,0) y 2 (x 1,5): 0,1 u son unos 11 m de la 1 y 0,4 u unos 44 m de la 2
        Map<String, Object> cercaDeLa1 = unica(1.1, 0.5);
        assertThat(cercaDeLa1).containsEntry("id_comuna", 1).containsEntry("regla", "cercana");
        assertThat((double) cercaDeLa1.get("distancia_m")).isBetween(8.0, 15.0);

        assertThat(unica(1.4, 0.5)).containsEntry("id_comuna", 2).containsEntry("regla", "cercana");
    }

    @Test
    void masLejosQueLaDistanciaMaximaNoHayComuna() {
        assertThat(asignar(5.0, 0.5, null)).isEmpty();      // unas 200 m de la 3
        assertThat(asignar(0.5, -2.0, null)).isEmpty();     // unas 220 m al sur de la 1
    }

    @Test
    void laDistanciaMaximaSeLeeDeConfiguracionYSePuedeCambiar() {
        assertThat(asignar(1.1, 0.5, 5.0)).isEmpty();                    // 11 m > 5 m
        assertThat(asignar(1.1, 0.5, 15.0)).hasSize(1);                  // 11 m <= 15 m

        jdbc.update("UPDATE configuracion SET valor = '5' WHERE clave = 'comuna_distancia_maxima_m'");
        assertThat(asignar(1.1, 0.5, null)).isEmpty();

        jdbc.update("UPDATE configuracion SET valor = '0' WHERE clave = 'comuna_distancia_maxima_m'");
        assertThat(asignar(1.1, 0.5, null)).isEmpty();                   // 0 desactiva la regla del hueco
        assertThat(asignar(0.5, 0.5, null)).hasSize(1);                  // dentro de una comuna siempre cuenta
    }

    @Test
    void laBaseRechazaUnaDistanciaMaximaQueNoEsUnNumero() {
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE configuracion SET valor = 'cerca' WHERE clave = 'comuna_distancia_maxima_m'"));
    }

    // ---- por la API --------------------------------------------------------------------------

    private static String cuerpo(double x, double y) {
        return String.format(Locale.ROOT,
                "{\"idCategoria\":1,\"latitud\":%s,\"longitud\":%s,"
                        + "\"descripcion\":\"Hay un hueco enorme frente al parque\",\"autorizaEnvio\":true}",
                lat(y), lon(x));
    }

    private MvcResult crear(double x, double y) throws Exception {
        return mvc.perform(post("/api/reportes").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo(x, y))).andReturn();
    }

    private JsonNode cuerpoDe(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private int comunaDelReporte(MvcResult creado) throws Exception {
        String codigo = cuerpoDe(creado).get("codigoSeguimiento").asText();
        MvcResult detalle = mvc.perform(get("/api/reportes/" + codigo).header("Authorization", "Bearer " + token))
                .andReturn();
        return cuerpoDe(detalle).get("comuna").asInt();
    }

    @Test
    void laApiGuardaElReporteYLaComunaSeLeeConLasMismasReglas() throws Exception {
        MvcResult dentro = crear(0.5, 0.5);
        MvcResult enHueco = crear(1.4, 0.5);
        MvcResult enSolape = crear(2.2, 0.7);

        assertThat(dentro.getResponse().getStatus()).isEqualTo(201);
        assertThat(enHueco.getResponse().getStatus()).isEqualTo(201);
        assertThat(enSolape.getResponse().getStatus()).isEqualTo(201);
        assertThat(comunaDelReporte(dentro)).isEqualTo(1);
        assertThat(comunaDelReporte(enHueco)).isEqualTo(2);
        assertThat(comunaDelReporte(enSolape)).isEqualTo(2);
    }

    @Test
    void laApiRechazaConFueraDeCoberturaUnPuntoLejosDeTodaComunaYNoGuardaNada() throws Exception {
        long antes = jdbc.queryForObject("SELECT count(*) FROM reporte", Long.class);

        MvcResult r = crear(5.0, 0.5);

        assertThat(r.getResponse().getStatus()).isEqualTo(422);
        JsonNode j = cuerpoDe(r);
        assertThat(j.get("codigo").asText()).isEqualTo("FUERA_DE_COBERTURA");
        assertThat(j.get("campo").asText()).isEqualTo("latitud");
        assertThat(j.get("mensaje").asText()).contains("fuera de cobertura");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reporte", Long.class)).isEqualTo(antes);
    }

    @Test
    void sinPoligonosCargadosNoSeRechazaNada() throws Exception {
        jdbc.update("UPDATE comuna SET geom = NULL");

        assertThat(crear(5.0, 0.5).getResponse().getStatus()).isEqualTo(201);
    }
}
