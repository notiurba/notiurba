package com.notiurba.reporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notiurba.foto.AlmacenFotos;
import com.notiurba.foto.AlmacenFotosNoDisponibleException;
import com.notiurba.foto.FotoGuardada;
import com.notiurba.seguridad.JwtProveedor;

/**
 * NOT-52: POST /api/reportes/{codigo}/fotos contra PostgreSQL + PostGIS reales, con el almacén de
 * fotos simulado (la subida real a Cloudinary se prueba aparte). Se omite sola sin Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:pruebas.properties")
class ReporteFotosIntegracionTest {

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

    private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F' };
    private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0 };
    private static final String REPORTE = "{\"idCategoria\":6,\"descripcion\":\"Consulta sobre el uso del suelo\",\"autorizaEnvio\":true}";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtProveedor jwt;
    @Autowired private ObjectMapper json;
    @MockBean private AlmacenFotos almacen;

    private record Ciudadano(long id, String token) {
    }

    private String token;
    private final AtomicInteger subidas = new AtomicInteger();

    private Ciudadano nuevoCiudadano() {
        String correo = "ciudadano-" + UUID.randomUUID() + "@prueba.com";
        int barrio = BarrioDePrueba.id(jdbc);
        long id = jdbc.queryForObject("""
                INSERT INTO usuario (nombres, apellidos, id_tipo_documento, numero_documento, id_barrio,
                                     direccion_correspondencia, correo, correo_verificado_en, password_hash,
                                     id_rol, id_aviso_privacidad, acepto_privacidad_en)
                VALUES ('Ana', 'Gómez', 1, ?, ?, 'Cra 1 # 2-3', ?, now(), 'hash-de-prueba', 1, 1, now())
                RETURNING id_usuario""", Long.class, UUID.randomUUID().toString().substring(0, 20), barrio, correo);
        return new Ciudadano(id, jwt.generar(correo, "CIUDADANO"));
    }

    @BeforeEach
    void preparar() {
        token = nuevoCiudadano().token();
        subidas.set(0);
        when(almacen.guardar(any(), anyString())).thenAnswer(i -> {
            int n = subidas.incrementAndGet();
            return new FotoGuardada("https://res.test/foto-" + n + ".jpg", "notiurba/reportes/foto-" + n + "-" + UUID.randomUUID());
        });
    }

    private String crearReporte(String tokenDelAutor) throws Exception {
        MvcResult r = mvc.perform(post("/api/reportes").header("Authorization", "Bearer " + tokenDelAutor)
                .contentType(MediaType.APPLICATION_JSON).content(REPORTE)).andReturn();
        return cuerpo(r).get("codigoSeguimiento").asText();
    }

    private MvcResult subir(String tokenQuien, String codigo, byte[] bytes) throws Exception {
        var peticion = multipart("/api/reportes/" + codigo + "/fotos")
                .file(new MockMultipartFile("foto", "foto.jpg", "image/jpeg", bytes));
        if (tokenQuien != null) {
            peticion = (org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder)
                    peticion.header("Authorization", "Bearer " + tokenQuien);
        }
        return mvc.perform(peticion).andReturn();
    }

    private JsonNode cuerpo(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private void esperarError(MvcResult r, int estado, String codigo, String campo) throws Exception {
        assertThat(r.getResponse().getStatus()).isEqualTo(estado);
        JsonNode j = cuerpo(r);
        assertThat(j.get("codigo").asText()).isEqualTo(codigo);
        assertThat(campo == null ? j.get("campo").isNull() : campo.equals(j.get("campo").asText())).isTrue();
    }

    private int fotosEnBase(String codigo) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM evidencia_reporte e JOIN reporte r USING (id_reporte)
                 WHERE r.codigo_seguimiento = ?""", Integer.class, codigo);
    }

    @Test
    void subeVariasFotosEnOrdenGuardaElIdentificadorYElDetalleLasMuestra() throws Exception {
        String codigo = crearReporte(token);

        MvcResult primera = subir(token, codigo, JPEG);
        MvcResult segunda = subir(token, codigo, PNG);

        assertThat(primera.getResponse().getStatus()).isEqualTo(201);
        JsonNode a = cuerpo(primera);
        assertThat(a.get("codigoSeguimiento").asText()).isEqualTo(codigo);
        assertThat(a.get("foto").get("orden").asInt()).isEqualTo(1);
        assertThat(a.get("foto").get("url").asText()).isEqualTo("https://res.test/foto-1.jpg");
        assertThat(cuerpo(segunda).get("foto").get("orden").asInt()).isEqualTo(2);
        // La columna nueva de V11 guarda el identificador de Cloudinary de cada foto
        assertThat(jdbc.queryForList("""
                SELECT e.id_publico_proveedor FROM evidencia_reporte e JOIN reporte r USING (id_reporte)
                 WHERE r.codigo_seguimiento = ? ORDER BY e.orden""", String.class, codigo))
                .hasSize(2).allSatisfy(id -> assertThat(id).startsWith("notiurba/reportes/foto-"));
        verify(almacen).guardar(JPEG, "image/jpeg");
        verify(almacen).guardar(PNG, "image/png");

        JsonNode detalle = json.readTree(mvc.perform(get("/api/reportes/" + codigo)
                .header("Authorization", "Bearer " + token)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(detalle.get("fotos")).hasSize(2);
        assertThat(detalle.get("fotos").get(0).get("url").asText()).isEqualTo("https://res.test/foto-1.jpg");
        assertThat(detalle.get("fotos").get(1).get("url").asText()).isEqualTo("https://res.test/foto-2.jpg");
        assertThat(detalle.get("fotos").toString()).doesNotContain("notiurba/reportes");   // el identificador no sale por la API
    }

    @Test
    void laCuartaFotoDa409SinSubirNadaYUnHuecoSeReutiliza() throws Exception {
        String codigo = crearReporte(token);
        for (int i = 0; i < 3; i++) {
            assertThat(subir(token, codigo, JPEG).getResponse().getStatus()).isEqualTo(201);
        }

        esperarError(subir(token, codigo, JPEG), 409, "FOTOS_MAXIMO", "foto");
        verify(almacen, times(3)).guardar(any(), anyString());
        assertThat(fotosEnBase(codigo)).isEqualTo(3);

        jdbc.update("DELETE FROM evidencia_reporte WHERE orden = 2 AND id_reporte = (SELECT id_reporte FROM reporte WHERE codigo_seguimiento = ?)", codigo);
        MvcResult reutiliza = subir(token, codigo, JPEG);
        assertThat(cuerpo(reutiliza).get("foto").get("orden").asInt()).isEqualTo(2);
    }

    @Test
    void elReporteAjenoDa404IgualQueUnoInexistenteYNoSubeNada() throws Exception {
        Ciudadano otra = nuevoCiudadano();
        String codigoAjeno = crearReporte(otra.token());

        MvcResult ajeno = subir(token, codigoAjeno, JPEG);
        MvcResult inexistente = subir(token, "NU-999999", JPEG);

        esperarError(ajeno, 404, "NO_ENCONTRADO", null);
        assertThat(ajeno.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .isEqualTo(inexistente.getResponse().getContentAsString(StandardCharsets.UTF_8));
        verifyNoInteractions(almacen);
        assertThat(fotosEnBase(codigoAjeno)).isZero();
    }

    @Test
    void soloAceptaJpgYPngReales() throws Exception {
        String codigo = crearReporte(token);

        // Se declara image/jpeg y se llama .jpg, pero el contenido no es una imagen
        esperarError(subir(token, codigo, "no soy una imagen".getBytes(StandardCharsets.UTF_8)), 415, "FORMATO_NO_SOPORTADO", "foto");
        esperarError(subir(token, codigo, "GIF89a....".getBytes(StandardCharsets.US_ASCII)), 415, "FORMATO_NO_SOPORTADO", "foto");
        esperarError(subir(token, codigo, new byte[0]), 400, "VALIDACION", "foto");
        verify(almacen, never()).guardar(any(), anyString());
        assertThat(fotosEnBase(codigo)).isZero();
    }

    @Test
    void sinLaParteFotoSinSesionOConCodigoMalFormado() throws Exception {
        String codigo = crearReporte(token);

        MvcResult sinFoto = mvc.perform(multipart("/api/reportes/" + codigo + "/fotos")
                .file(new MockMultipartFile("otra", "x.jpg", "image/jpeg", JPEG))
                .header("Authorization", "Bearer " + token)).andReturn();
        esperarError(sinFoto, 400, "VALIDACION", "foto");
        esperarError(subir(null, codigo, JPEG), 401, "NO_AUTENTICADO", null);
        esperarError(subir(token, "123", JPEG), 400, "VALIDACION", "codigo");
        verifyNoInteractions(almacen);
    }

    @Test
    void siElAlmacenEstaCaidoDa502YNoQuedaNingunaFilaHuerfana() throws Exception {
        String codigo = crearReporte(token);
        when(almacen.guardar(any(), anyString())).thenThrow(new AlmacenFotosNoDisponibleException("Cloudinary caído"));

        MvcResult r = subir(token, codigo, JPEG);

        esperarError(r, 502, "ALMACEN_FOTOS_NO_DISPONIBLE", "foto");
        assertThat(r.getResponse().getContentAsString(StandardCharsets.UTF_8)).doesNotContain("Cloudinary");
        assertThat(fotosEnBase(codigo)).isZero();
    }
}
