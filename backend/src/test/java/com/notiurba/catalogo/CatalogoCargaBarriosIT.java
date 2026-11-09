package com.notiurba.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notiurba.reporte.BaseDeDatosTemporal;
import com.notiurba.usuario.CatalogosRegistro;

/** Opt-in: valida la propuesta historica en una base desechable, nunca en Neon. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:pruebas.properties")
class CatalogoCargaBarriosIT {
    private static BaseDeDatosTemporal bd;

    @BeforeAll
    static void crearBase() { bd = BaseDeDatosTemporal.crearOOmitir(); }

    @AfterAll
    static void borrarBase() throws SQLException { if (bd != null) bd.borrar(); }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        bd.registrar(registro);
        // Evita recursos compilados antiguos en target: usa solo las migraciones del checkout.
        registro.add("spring.flyway.locations", () -> System.getProperty("notiurba.catalogo.migraciones"));
    }

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CatalogosRegistro catalogos;

    @Test
    void cargaIdempotenteExponeBarriosConIdsYComunasExistentes() throws Exception {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM barrio WHERE NOT interno", Integer.class)).isZero();
        int interno = jdbc.queryForObject("SELECT id_barrio FROM barrio WHERE interno", Integer.class);
        assertThat(catalogos.existeBarrio(interno)).isFalse();
        assertThat(mvc.perform(get("/api/catalogos/barrios")).andReturn().getResponse().getContentAsString()).isEqualTo("[]");

        String sql = Files.readString(Path.of(System.getProperty("notiurba.catalogo.propuesta")), StandardCharsets.UTF_8);
        assertThat(jdbc.update(sql)).isEqualTo(135);
        assertThat(jdbc.update(sql)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM barrio", Integer.class)).isEqualTo(136);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM barrio b LEFT JOIN comuna c USING (id_comuna) WHERE c.id_comuna IS NULL", Integer.class)).isZero();

        var respuesta = mvc.perform(get("/api/catalogos/barrios")).andReturn().getResponse();
        assertThat(respuesta.getStatus()).isEqualTo(200);
        var lista = json.readTree(respuesta.getContentAsString(StandardCharsets.UTF_8));
        assertThat(lista).hasSize(135);
        var ordenados = jdbc.queryForList("SELECT nombre FROM barrio WHERE NOT interno ORDER BY nombre, id_barrio", String.class);
        assertThat(lista.findValuesAsText("nombre")).containsExactlyElementsOf(ordenados);
        for (var barrio : lista) {
            assertThat(barrio.fieldNames()).toIterable().containsExactlyInAnyOrder("idBarrio", "idComuna", "nombre");
            assertThat(catalogos.existeBarrio(barrio.get("idBarrio").asInt())).isTrue();
            assertThat(jdbc.queryForObject("SELECT id_comuna FROM barrio WHERE id_barrio = ?", Integer.class, barrio.get("idBarrio").asInt())).isEqualTo(barrio.get("idComuna").asInt());
        }
        assertThat(catalogos.existeBarrio(interno)).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class)).isEqualTo(16);
    }
}
