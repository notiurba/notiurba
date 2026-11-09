package com.notiurba.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.charset.StandardCharsets;
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
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notiurba.reporte.BarrioDePrueba;
import com.notiurba.reporte.BaseDeDatosTemporal;
import com.notiurba.usuario.CatalogosRegistro;

/** NOT-55: los catálogos leen los datos sembrados por las migraciones (se omite sola sin Docker). */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:pruebas.properties")
class CatalogoIntegracionTest {

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
    @Autowired private CatalogosRegistro catalogos;

    private JsonNode pedir(String ruta) throws Exception {
        MvcResult r = mvc.perform(get(ruta)).andReturn(); // sin token: son públicos
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void tiposDocumentoTraeLosCuatroSembrados() throws Exception {
        JsonNode lista = pedir("/api/catalogos/tipos-documento");
        assertThat(lista).hasSize(4);
        assertThat(lista.findValuesAsText("codigo")).containsExactly("CC", "CE", "PPT", "PAS");
        assertThat(lista.get(0).get("idTipoDocumento").asInt()).isPositive();
        assertThat(lista.get(0).get("nombre").asText()).isNotBlank();
    }

    @Test
    void barriosVienenOrdenadosPorNombreYConSuComuna() throws Exception {
        jdbc.update("INSERT INTO barrio (id_comuna, nombre) VALUES (2, 'Zzz Prueba catálogo'), (2, 'Aaa Prueba catálogo')");

        JsonNode lista = pedir("/api/catalogos/barrios");
        assertThat(lista.size()).isGreaterThanOrEqualTo(3);
        var nombres = lista.findValuesAsText("nombre");
        assertThat(nombres).isSortedAccordingTo(java.text.Collator.getInstance(new java.util.Locale("es", "CO")));
        assertThat(nombres).contains("Aaa Prueba catálogo", "Zzz Prueba catálogo");
        assertThat(lista.get(0).get("idBarrio").asInt()).isPositive();
        assertThat(lista.get(0).get("idComuna").asInt()).isBetween(1, 10);
    }

    @Test
    void elBarrioInternoDelAdministradorNoSaleNiSePuedeElegir() throws Exception {
        int idInterno = jdbc.queryForObject(
                "SELECT id_barrio FROM barrio WHERE nombre = 'Administración NotiUrba'", Integer.class);

        assertThat(pedir("/api/catalogos/barrios").findValuesAsText("nombre")).doesNotContain("Administración NotiUrba");
        assertThat(catalogos.existeBarrio(idInterno)).isFalse();
        assertThat(catalogos.existeBarrio(BarrioDePrueba.id(jdbc))).isTrue();
    }
}
