package com.notiurba.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import com.notiurba.reporte.DatosDePrueba;
import com.notiurba.reporte.DatosDePrueba.Reporte;
import com.notiurba.seguridad.JwtProveedor;

/**
 * NOT-129: GET /api/admin/mapa contra PostgreSQL + PostGIS real. Cada prueba empieza sin reportes
 * para que los totales sean exactos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:pruebas.properties")
class MapaAdminIntegracionTest {

    /** Punto de prueba de {@link DatosDePrueba#reporte}: cae dentro de la capa de comunas. */
    private static final String PUNTO_DE_PRUEBA = "ST_SetSRID(ST_MakePoint(-76.1950, 4.0867), 4326)";

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
    private long idAna;
    private String tokenAna;
    private String tokenAdmin;

    @BeforeEach
    void preparar() {
        jdbc.execute("TRUNCATE reporte CASCADE");
        idBarrio = BarrioDePrueba.id(jdbc);
        String ana = "ana-" + UUID.randomUUID() + "@prueba.com";
        String admin = "admin-" + UUID.randomUUID() + "@prueba.com";
        idAna = DatosDePrueba.usuario(jdbc, idBarrio, ana, DatosDePrueba.CIUDADANO);
        DatosDePrueba.usuario(jdbc, idBarrio, admin, DatosDePrueba.ADMIN);
        tokenAna = jwt.generar(ana, "CIUDADANO");
        tokenAdmin = jwt.generar(admin, "ADMIN");
    }

    private MvcResult pedir(String ruta, String token) throws Exception {
        var peticion = get(ruta);
        if (token != null) {
            peticion = peticion.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(peticion).andReturn();
    }

    private JsonNode cuerpo(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private JsonNode leer(MvcResult r) throws Exception {
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        return cuerpo(r);
    }

    private static List<String> codigos(JsonNode respuesta) {
        return respuesta.get("reportes").findValuesAsText("codigoSeguimiento");
    }

    private static List<String> campos(JsonNode nodo) {
        List<String> nombres = new ArrayList<>();
        nodo.fieldNames().forEachRemaining(nombres::add);
        return nombres;
    }

    private int comunaDelPuntoDePrueba() {
        return jdbc.queryForObject("SELECT id_comuna FROM fn_asignar_comuna(" + PUNTO_DE_PRUEBA + ")", Integer.class);
    }

    /** Reporte con ubicación pero sin barrio y lejos de toda comuna: su comuna no se puede determinar. */
    private Reporte reporteSinComuna() {
        return jdbc.queryForObject("""
                INSERT INTO reporte (id_usuario, id_categoria, descripcion, ubicacion, autorizacion_envio_en, creado_en)
                VALUES (?, 1, 'Reporte lejos de la capa de comunas', ST_SetSRID(ST_MakePoint(-75.5, 4.6), 4326),
                        now() - interval '1 hour', now() - interval '1 hour')
                RETURNING id_reporte, codigo_seguimiento""",
                (rs, i) -> new Reporte(rs.getLong(1), rs.getString(2)), idAna);
    }

    private Reporte reporteSinUbicacion() {
        return jdbc.queryForObject("""
                INSERT INTO reporte (id_usuario, id_categoria, id_barrio, descripcion, autorizacion_envio_en, creado_en)
                VALUES (?, 1, ?, 'Reporte sin punto en el mapa', now() - interval '1 hour', now() - interval '1 hour')
                RETURNING id_reporte, codigo_seguimiento""",
                (rs, i) -> new Reporte(rs.getLong(1), rs.getString(2)), idAna, idBarrio);
    }

    // ---------------------------------------------------------------- CA-01, CA-02

    @Test
    void elMapaTraeSoloReportesVigentesConUbicacionYLosDatosDelMarcador() throws Exception {
        Reporte vigente = DatosDePrueba.reporte(jdbc, idAna, 1, idBarrio, 1);
        Reporte vencido = DatosDePrueba.reporte(jdbc, idAna, 1, idBarrio, 40);
        reporteSinUbicacion();

        JsonNode mapa = leer(pedir("/api/admin/mapa", tokenAdmin));

        assertThat(codigos(mapa)).containsExactly(vigente.codigo());
        assertThat(mapa.get("total").asLong()).isEqualTo(1);
        assertThat(mapa.get("sinComuna").asLong()).isZero();
        assertThat(mapa.get("truncado").asBoolean()).isFalse();

        JsonNode marcador = mapa.get("reportes").get(0);
        assertThat(marcador.get("estado").asText()).isEqualTo("Recibido");
        assertThat(marcador.get("categoria").get("idCategoria").asInt()).isEqualTo(1);
        assertThat(marcador.get("categoria").get("nombre").asText()).isNotBlank();
        assertThat(marcador.get("barrio").get("idBarrio").asInt()).isEqualTo(idBarrio);
        assertThat(marcador.get("barrio").get("nombre").asText()).isEqualTo("Barrio de prueba");
        assertThat(marcador.get("comuna").asInt()).isEqualTo(comunaDelPuntoDePrueba());
        assertThat(marcador.get("latitud").asDouble()).isEqualTo(4.0867);
        assertThat(marcador.get("longitud").asDouble()).isEqualTo(-76.1950);
        assertThat(marcador.get("creadoEn").asText()).isNotBlank();
        assertThat(marcador.get("vigenteHasta").asText()).isNotBlank();

        // Ley 1581: el marcador no trae datos del ciudadano ni la descripción
        assertThat(campos(marcador)).containsExactlyInAnyOrder("codigoSeguimiento", "estado", "categoria", "barrio",
                "comuna", "latitud", "longitud", "creadoEn", "vigenteHasta");

        // El vencido sale del mapa pero sigue en el listado del panel
        JsonNode listado = leer(pedir("/api/admin/reportes", tokenAdmin));
        assertThat(listado.findValuesAsText("codigoSeguimiento")).contains(vencido.codigo(), vigente.codigo());
    }

    @Test
    void elMapaOrdenaDelMasRecienteAlMasAntiguo() throws Exception {
        Reporte viejo = DatosDePrueba.reporte(jdbc, idAna, 1, idBarrio, 5);
        Reporte nuevo = DatosDePrueba.reporte(jdbc, idAna, 2, idBarrio, 1);

        assertThat(codigos(leer(pedir("/api/admin/mapa", tokenAdmin)))).containsExactly(nuevo.codigo(), viejo.codigo());
    }

    @Test
    void elMapaFiltraPorEstadoCategoriaComunaYFechas() throws Exception {
        Reporte cat1 = DatosDePrueba.reporte(jdbc, idAna, 1, idBarrio, 10);
        Reporte cat2 = DatosDePrueba.reporte(jdbc, idAna, 2, idBarrio, 0);
        DatosDePrueba.pasarAEstado(jdbc, cat2, "En trámite");
        int comuna = comunaDelPuntoDePrueba();
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));

        assertThat(codigos(leer(pedir("/api/admin/mapa?idCategoria=2", tokenAdmin)))).containsExactly(cat2.codigo());
        assertThat(codigos(leer(pedir("/api/admin/mapa?estado=EN_TRAMITE", tokenAdmin)))).containsExactly(cat2.codigo());
        assertThat(codigos(leer(pedir("/api/admin/mapa?estado=Recibido", tokenAdmin)))).containsExactly(cat1.codigo());
        assertThat(codigos(leer(pedir("/api/admin/mapa?comuna=" + comuna, tokenAdmin))))
                .containsExactlyInAnyOrder(cat1.codigo(), cat2.codigo());
        assertThat(codigos(leer(pedir("/api/admin/mapa?comuna=" + (comuna % 10 + 1), tokenAdmin)))).isEmpty();
        assertThat(codigos(leer(pedir("/api/admin/mapa?desde=" + hoy, tokenAdmin)))).containsExactly(cat2.codigo());
        assertThat(codigos(leer(pedir("/api/admin/mapa?hasta=" + hoy.minusDays(5), tokenAdmin))))
                .containsExactly(cat1.codigo());
        assertThat(codigos(leer(pedir("/api/admin/mapa?idCategoria=1&estado=EN_TRAMITE", tokenAdmin)))).isEmpty();
    }

    // ---------------------------------------------------------------- CA-11, CA-12

    @Test
    void unReporteSinComunaApareceConComunaNulaYSeCuentaAparte() throws Exception {
        Reporte sinComuna = reporteSinComuna();
        Reporte conComuna = DatosDePrueba.reporte(jdbc, idAna, 1, idBarrio, 2);

        JsonNode mapa = leer(pedir("/api/admin/mapa", tokenAdmin));

        assertThat(codigos(mapa)).containsExactly(sinComuna.codigo(), conComuna.codigo());
        assertThat(mapa.get("total").asLong()).isEqualTo(2);
        assertThat(mapa.get("sinComuna").asLong()).isEqualTo(1);
        JsonNode marcador = mapa.get("reportes").get(0);
        assertThat(marcador.get("comuna").isNull()).isTrue();
        assertThat(marcador.get("barrio").isNull()).isTrue();
    }

    @Test
    void sinReportesRespondeVacioYSinError() throws Exception {
        JsonNode mapa = leer(pedir("/api/admin/mapa", tokenAdmin));

        assertThat(mapa.get("reportes")).isEmpty();
        assertThat(mapa.get("total").asLong()).isZero();
        assertThat(mapa.get("sinComuna").asLong()).isZero();
        assertThat(mapa.get("truncado").asBoolean()).isFalse();
    }

    // ---------------------------------------------------------------- CA-07, CA-10

    @Test
    void elMapaValidaLosFiltrosConElFormatoDelContrato() throws Exception {
        assertError(pedir("/api/admin/mapa?comuna=11", tokenAdmin), 400, "VALIDACION", "comuna");
        assertError(pedir("/api/admin/mapa?comuna=0", tokenAdmin), 400, "VALIDACION", "comuna");
        assertError(pedir("/api/admin/mapa?estado=Cancelado", tokenAdmin), 400, "VALIDACION", "estado");
        assertError(pedir("/api/admin/mapa?desde=2026-11-10&hasta=2026-11-01", tokenAdmin), 400, "VALIDACION", "desde");
        assertThat(pedir("/api/admin/mapa?desde=mañana", tokenAdmin).getResponse().getStatus()).isEqualTo(400);
        assertThat(pedir("/api/admin/mapa?comuna=cinco", tokenAdmin).getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void soloElAdministradorVeElMapa() throws Exception {
        DatosDePrueba.reporte(jdbc, idAna, 1, idBarrio, 1);

        assertError(pedir("/api/admin/mapa", tokenAna), 403, "SIN_PERMISO", null);
        assertError(pedir("/api/admin/mapa", null), 401, "NO_AUTENTICADO", null);
        assertError(pedir("/api/admin/mapa", "token-falso"), 401, "NO_AUTENTICADO", null);
    }

    private void assertError(MvcResult r, int http, String codigo, String campo) throws Exception {
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(http);
        JsonNode error = cuerpo(r);
        assertThat(error.get("codigo").asText()).isEqualTo(codigo);
        assertThat(error.get("mensaje").asText()).isNotBlank();
        if (campo == null) {
            assertThat(error.get("campo").isNull()).isTrue();
        } else {
            assertThat(error.get("campo").asText()).isEqualTo(campo);
        }
    }

    // ---------------------------------------------------------------- CA-08, CA-13

    @Test
    void conMasDeMilReportesDevuelveMilAvisaQueEstaTruncadoYRespondeEnMenosDeDosSegundos() throws Exception {
        // 1.001 reportes vigentes repartidos en una cuadrícula de unos 800 m alrededor del punto de prueba
        jdbc.update("""
                INSERT INTO reporte (id_usuario, id_categoria, id_barrio, descripcion, ubicacion,
                                     autorizacion_envio_en, creado_en)
                SELECT ?, 1 + g % 3, ?, 'Reporte de carga para medir el mapa',
                       ST_SetSRID(ST_MakePoint(-76.1950 + (g % 20) * 0.0004, 4.0867 + (g / 20 % 20) * 0.0004), 4326),
                       now() - make_interval(mins => g), now() - make_interval(mins => g)
                  FROM generate_series(1, 1001) AS g""", idAna, idBarrio);

        pedir("/api/admin/mapa?comuna=1", tokenAdmin); // calentamiento: la primera llamada carga clases y planes
        long inicio = System.nanoTime();
        MvcResult r = pedir("/api/admin/mapa", tokenAdmin);
        long milisegundos = (System.nanoTime() - inicio) / 1_000_000;

        JsonNode mapa = leer(r);
        assertThat(mapa.get("total").asLong()).isEqualTo(1001);
        assertThat(mapa.get("reportes")).hasSize(1000);
        assertThat(mapa.get("truncado").asBoolean()).isTrue();
        assertThat(milisegundos).as("GET /api/admin/mapa con 1.001 reportes tardó %d ms", milisegundos)
                .isLessThanOrEqualTo(2000);
    }
}
