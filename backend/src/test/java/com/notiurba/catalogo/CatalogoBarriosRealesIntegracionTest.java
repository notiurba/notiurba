package com.notiurba.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notiurba.reporte.BaseDeDatosTemporal;

/**
 * V17 carga el catálogo histórico de barrios (PMGR 2020, con las decisiones B01-B05 de Andrés: 133 barrios). Se comprueba contra el CSV de origen
 * (docs/datos/barrios-pmgr-2020.csv) y contra el catálogo público que ve el selector del registro.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:pruebas.properties")
class CatalogoBarriosRealesIntegracionTest {

    private static final Path CSV = Path.of("../docs/datos/barrios-pmgr-2020.csv");
    private static final Path V17 = Path.of("src/main/resources/db/migration/V17__catalogo_barrios_pmgr_2020.sql");

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
    @Autowired private ObjectMapper json;

    /** Pares «comuna|nombre» del CSV de origen. */
    private List<String> delCsv() throws IOException {
        List<String> pares = new ArrayList<>();
        for (String linea : Files.readAllLines(CSV, StandardCharsets.UTF_8).subList(1, Files.readAllLines(CSV).size())) {
            if (!linea.isBlank()) {
                int coma = linea.indexOf(',');
                pares.add(linea.substring(0, coma) + "|" + linea.substring(coma + 1));
            }
        }
        return pares;
    }

    @Test
    void laMigracionCargaExactamenteLosBarriosDelCsv() throws IOException {
        List<String> cargados = jdbc.queryForList(
                "SELECT id_comuna || '|' || nombre FROM barrio WHERE NOT interno", String.class);

        assertThat(delCsv()).hasSize(133);
        assertThat(cargados).containsExactlyInAnyOrderElementsOf(delCsv());
    }

    @Test
    void cadaComunaTieneSusBarrios() {
        Map<Integer, Integer> porComuna = new TreeMap<>();
        jdbc.query("SELECT id_comuna, count(*) FROM barrio WHERE NOT interno GROUP BY id_comuna",
                rs -> { porComuna.put(rs.getInt(1), rs.getInt(2)); });

        assertThat(porComuna).containsExactly(
                Map.entry(1, 12), Map.entry(2, 8), Map.entry(3, 21), Map.entry(4, 5), Map.entry(5, 15),
                Map.entry(6, 16), Map.entry(7, 21), Map.entry(8, 17), Map.entry(9, 15), Map.entry(10, 3));
    }

    @Test
    void elCatalogoPublicoDevuelveLos133OrdenadosYSinElBarrioInterno() throws Exception {
        JsonNode lista = json.readTree(mvc.perform(get("/api/catalogos/barrios")).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8));

        assertThat(lista).hasSize(133);
        assertThat(lista.findValuesAsText("nombre")).doesNotContain("Administración NotiUrba")
                .isSortedAccordingTo(java.text.Collator.getInstance(new java.util.Locale("es", "CO")));
        for (JsonNode b : lista) {
            assertThat(b.get("idBarrio").asInt()).isPositive();
            assertThat(b.get("idComuna").asInt()).isBetween(1, 10);
        }
    }

    @Test
    void ningunBarrioEstaEnDosComunas() {
        List<String> repetidos = jdbc.queryForList("""
                SELECT nombre FROM barrio WHERE NOT interno GROUP BY nombre HAVING count(*) > 1""", String.class);

        assertThat(repetidos).as("un barrio pertenece a una sola comuna").isEmpty();
    }

    @Test
    void lasDecisionesDeAndresSobreLosNombresDudososQuedaronAplicadas() {
        List<String> nombres = jdbc.queryForList("SELECT nombre FROM barrio WHERE NOT interno", String.class);

        assertThat(nombres).contains("El Príncipe", "Lusitania", "Diablos Rojos I", "Diablos Rojos II", "San Luis", "Santa Inés", "Santa Inés de Comfamiliar")
                .doesNotContain("El Principio", "Urbanización Lusitania", "Diablos Rojos", "Urbanización San Luis",
                        "Santa Inés - Comfamiliar");
        assertThat(jdbc.queryForObject("SELECT id_comuna FROM barrio WHERE nombre = 'Diablos Rojos I'", Integer.class)).isEqualTo(7);
        assertThat(jdbc.queryForObject("SELECT id_comuna FROM barrio WHERE nombre = 'Diablos Rojos II'", Integer.class)).isEqualTo(8);
        assertThat(jdbc.queryForObject("SELECT id_comuna FROM barrio WHERE nombre = 'Chiminangos'", Integer.class)).isEqualTo(8);
        assertThat(jdbc.queryForObject("SELECT id_comuna FROM barrio WHERE nombre = 'Santa Inés de Comfamiliar'", Integer.class)).isEqualTo(8);
    }

    @Test
    void elBarrioAdministrativoYaNoExisteYElAdministradorTieneUnoReal() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM barrio WHERE nombre = 'Administración NotiUrba'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM barrio WHERE interno", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM usuario u JOIN rol r ON r.id_rol = u.id_rol JOIN barrio b ON b.id_barrio = u.id_barrio
                 WHERE r.nombre = 'ADMIN' AND NOT b.interno""", Integer.class)).isEqualTo(1);
    }

    @Test
    void repetirLaCargaNoDuplicaNada() throws IOException {
        int antes = jdbc.queryForObject("SELECT count(*) FROM barrio", Integer.class);
        String sql = Files.readString(V17, StandardCharsets.UTF_8);

        jdbc.execute(sql.substring(sql.indexOf("INSERT INTO barrio")));   // el mismo INSERT, otra vez

        assertThat(jdbc.queryForObject("SELECT count(*) FROM barrio", Integer.class)).isEqualTo(antes);
    }
}
