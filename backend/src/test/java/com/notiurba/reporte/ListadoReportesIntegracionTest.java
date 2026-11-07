package com.notiurba.reporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
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
import com.notiurba.reporte.DatosDePrueba.Reporte;
import com.notiurba.seguridad.JwtProveedor;

/** «Mis reportes» (GET /api/reportes) y el listado del panel (GET /api/admin/reportes) contra PostgreSQL real. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:pruebas.properties")
class ListadoReportesIntegracionTest {

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
    private long idLuis;
    private String tokenLuis;
    private String tokenAdmin;

    @BeforeEach
    void preparar() {
        idBarrio = BarrioDePrueba.id(jdbc);
        String ana = "ana-" + UUID.randomUUID() + "@prueba.com";
        String luis = "luis-" + UUID.randomUUID() + "@prueba.com";
        String admin = "admin-" + UUID.randomUUID() + "@prueba.com";
        idAna = DatosDePrueba.usuario(jdbc, idBarrio, ana, DatosDePrueba.CIUDADANO);
        idLuis = DatosDePrueba.usuario(jdbc, idBarrio, luis, DatosDePrueba.CIUDADANO);
        DatosDePrueba.usuario(jdbc, idBarrio, admin, DatosDePrueba.ADMIN);
        tokenAna = jwt.generar(ana, "CIUDADANO");
        tokenLuis = jwt.generar(luis, "CIUDADANO");
        tokenAdmin = jwt.generar(admin, "ADMIN");
    }

    private MvcResult pedir(String ruta, String token) throws Exception {
        var peticion = get(ruta);
        if (token != null) {
            peticion = peticion.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(peticion).andReturn();
    }

    private JsonNode leer(MvcResult r) throws Exception {
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private List<String> codigos(JsonNode lista) {
        return lista.findValuesAsText("codigoSeguimiento");
    }

    // ---------------------------------------------------------------- Mis reportes

    @Test
    void misReportesTraeSoloLosMiosLosMasRecientesPrimeroYConTodoLoDeLaTarjeta() throws Exception {
        Reporte viejo = DatosDePrueba.reporte(jdbc, idAna, 1, idBarrio, 5);
        Reporte nuevo = DatosDePrueba.reporte(jdbc, idAna, 2, idBarrio, 1);
        DatosDePrueba.reporte(jdbc, idLuis, 1, idBarrio, 0);

        JsonNode lista = leer(pedir("/api/reportes", tokenAna));

        assertThat(codigos(lista)).containsExactly(nuevo.codigo(), viejo.codigo());
        JsonNode primero = lista.get(0);
        assertThat(primero.get("estado").asText()).isEqualTo("Recibido");
        assertThat(primero.get("categoria").get("idCategoria").asInt()).isEqualTo(2);
        assertThat(primero.get("categoria").get("nombre").asText()).isNotBlank();
        assertThat(primero.get("direccionIncidente").asText()).isEqualTo("Cra 5 # 10-20");
        assertThat(primero.get("barrio").get("idBarrio").asInt()).isEqualTo(idBarrio);
        assertThat(primero.get("creadoEn").asText()).isNotBlank();
        assertThat(primero.get("vigenteHasta").asText()).isNotBlank();
        assertThat(primero.get("envio").isNull()).isTrue();
    }

    @Test
    void misReportesMuestraElEstadoDelCorreoYSePuedeFiltrarPorEstado() throws Exception {
        Reporte enviado = DatosDePrueba.reporte(jdbc, idAna, 1, idBarrio, 3);
        Reporte recibido = DatosDePrueba.reporte(jdbc, idAna, 1, idBarrio, 2);
        DatosDePrueba.pasarAEstado(jdbc, enviado, "Enviado");
        jdbc.update("""
                INSERT INTO registro_envio_correo (id_reporte, id_canal_atencion, id_modo_envio, id_estado_envio, error)
                VALUES (?, 1, (SELECT id_modo_envio FROM modo_envio WHERE nombre = 'RESEND'),
                        (SELECT id_estado_envio FROM estado_envio WHERE nombre = 'FALLIDO'), 'detalle técnico')""",
                recibido.id());

        JsonNode todos = leer(pedir("/api/reportes", tokenAna));
        JsonNode delRecibido = todos.get(0);
        assertThat(delRecibido.get("codigoSeguimiento").asText()).isEqualTo(recibido.codigo());
        assertThat(delRecibido.get("envio").get("estado").asText()).isEqualTo("FALLIDO");
        assertThat(delRecibido.get("envio").get("motivo").asText()).isEqualTo("No pudimos entregar el correo a la entidad.");

        assertThat(codigos(leer(pedir("/api/reportes?estado=Enviado", tokenAna)))).containsExactly(enviado.codigo());
        assertThat(codigos(leer(pedir("/api/reportes?estado=ENVIADO", tokenAna)))).containsExactly(enviado.codigo());
        assertThat(codigos(leer(pedir("/api/reportes?estado=Resuelto", tokenAna)))).isEmpty();
    }

    @Test
    void misReportesValidaElEstadoYExigeSesion() throws Exception {
        MvcResult malo = pedir("/api/reportes?estado=Cancelado", tokenAna);
        assertThat(malo.getResponse().getStatus()).isEqualTo(400);
        assertThat(json.readTree(malo.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("campo").asText())
                .isEqualTo("estado");
        assertThat(pedir("/api/reportes", null).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void sinReportesLaListaVieneVacia() throws Exception {
        assertThat(leer(pedir("/api/reportes", tokenLuis))).isEmpty();
    }

    // ---------------------------------------------------------------- Panel de administración

    @Test
    void elPanelVeLosReportesDeTodosConFotosYHistorial() throws Exception {
        Reporte deAna = DatosDePrueba.reporte(jdbc, idAna, 1, idBarrio, 2);
        Reporte deLuis = DatosDePrueba.reporte(jdbc, idLuis, 2, idBarrio, 1);
        DatosDePrueba.foto(jdbc, deAna, idAna, 1, "pub-a1");
        DatosDePrueba.foto(jdbc, deAna, idAna, 2, "pub-a2");
        DatosDePrueba.pasarAEstado(jdbc, deAna, "Enviado");

        JsonNode lista = leer(pedir("/api/admin/reportes", tokenAdmin));

        assertThat(codigos(lista)).contains(deAna.codigo(), deLuis.codigo());
        JsonNode ana = null;
        for (JsonNode r : lista) {
            if (r.get("codigoSeguimiento").asText().equals(deAna.codigo())) {
                ana = r;
            }
        }
        assertThat(ana).isNotNull();
        assertThat(ana.get("estado").asText()).isEqualTo("Enviado");
        assertThat(ana.get("descripcion").asText()).isEqualTo("Hay un hueco enorme frente al parque");
        assertThat(ana.get("latitud").asDouble()).isEqualTo(4.0867);
        assertThat(ana.get("longitud").asDouble()).isEqualTo(-76.1950);
        assertThat(ana.get("comuna").isInt()).isTrue();
        assertThat(ana.get("fotos")).hasSize(2);
        assertThat(ana.get("historial")).hasSize(2);
        assertThat(ana.get("historial").get(1).get("estado").asText()).isEqualTo("Enviado");
    }

    @Test
    void elPanelFiltraPorEstadoCategoriaComunaYFechas() throws Exception {
        Reporte cat1 = DatosDePrueba.reporte(jdbc, idAna, 1, idBarrio, 10);
        Reporte cat2 = DatosDePrueba.reporte(jdbc, idLuis, 2, idBarrio, 0);
        DatosDePrueba.pasarAEstado(jdbc, cat2, "En trámite");
        int comuna = jdbc.queryForObject(
                "SELECT id_comuna FROM fn_asignar_comuna(ST_SetSRID(ST_MakePoint(-76.1950, 4.0867), 4326))", Integer.class);
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));

        assertThat(codigos(leer(pedir("/api/admin/reportes?idCategoria=2", tokenAdmin))))
                .contains(cat2.codigo()).doesNotContain(cat1.codigo());
        assertThat(codigos(leer(pedir("/api/admin/reportes?estado=EN_TRAMITE", tokenAdmin))))
                .contains(cat2.codigo()).doesNotContain(cat1.codigo());
        assertThat(codigos(leer(pedir("/api/admin/reportes?comuna=" + comuna, tokenAdmin))))
                .contains(cat1.codigo(), cat2.codigo());
        assertThat(codigos(leer(pedir("/api/admin/reportes?comuna=" + (comuna % 10 + 1), tokenAdmin))))
                .doesNotContain(cat1.codigo(), cat2.codigo());
        assertThat(codigos(leer(pedir("/api/admin/reportes?desde=" + hoy, tokenAdmin))))
                .contains(cat2.codigo()).doesNotContain(cat1.codigo());
        assertThat(codigos(leer(pedir("/api/admin/reportes?hasta=" + hoy.minusDays(5), tokenAdmin))))
                .contains(cat1.codigo()).doesNotContain(cat2.codigo());
    }

    @Test
    void elPanelValidaLosFiltrosYSoloLoVeElAdministrador() throws Exception {
        assertThat(pedir("/api/admin/reportes?comuna=11", tokenAdmin).getResponse().getStatus()).isEqualTo(400);
        assertThat(pedir("/api/admin/reportes?estado=Cancelado", tokenAdmin).getResponse().getStatus()).isEqualTo(400);
        assertThat(pedir("/api/admin/reportes?desde=2026-11-10&hasta=2026-11-01", tokenAdmin).getResponse().getStatus())
                .isEqualTo(400);
        assertThat(pedir("/api/admin/reportes?desde=mañana", tokenAdmin).getResponse().getStatus()).isEqualTo(400);

        assertThat(pedir("/api/admin/reportes", tokenAna).getResponse().getStatus()).isEqualTo(403);
        assertThat(pedir("/api/admin/reportes", null).getResponse().getStatus()).isEqualTo(401);
    }
}
