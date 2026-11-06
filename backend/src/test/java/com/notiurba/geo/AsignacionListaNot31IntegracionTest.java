package com.notiurba.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import com.notiurba.reporte.BaseDeDatosTemporal;

/**
 * NOT-55, «Listo cuando»: la lista de coordenadas de prueba de NOT-31 (74 puntos) contra la capa de
 * NOT-31 tal como la carga la migración V14; los puntos con resultado distinto al esperado deben ser 0.
 *
 * Ojo: la capa es SINTÉTICA y NO OFICIAL (docs/gis-not31/README.md). Cuando llegue la capa oficial y
 * se reemplace V14 (ver backend/scripts/generar-capa-comunas.py), esta prueba y su CSV hay que
 * regenerarlos o retirarlos. Como esta capa no tiene huecos ni solapes, el hueco y el solape se
 * prueban en AsignacionDeComunaIntegracionTest.
 *
 * Esperado de cada punto: el CSV de resultados dice en qué comunas cae (columna «comunas») y si está
 * dentro del perímetro. Dentro: la de menor número; fuera: ninguna (fuera de cobertura).
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:pruebas.properties")
class AsignacionListaNot31IntegracionTest {

    private static final Path COORDENADAS = Path.of("../docs/gis-not31/pruebas/coordenadas_prueba.csv");
    private static final Path RESULTADOS = Path.of("../docs/gis-not31/pruebas/resultados_puntos_prototipo.csv");

    private static BaseDeDatosTemporal bd;

    @BeforeAll
    static void crearBaseDesechable() {
        assumeTrue(Files.exists(COORDENADAS) && Files.exists(RESULTADOS),
                "No están los archivos de docs/gis-not31: se omite la prueba de la lista de NOT-31");
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
        if (bd != null) {
            bd.registrar(registro);
        }
    }

    @Autowired private JdbcTemplate jdbc;

    private record Punto(String id, double latitud, double longitud) {
    }

    /** Líneas de datos de un CSV con BOM, partidas en comas (los campos de interés no llevan comas). */
    private static List<String[]> filas(Path archivo) throws IOException {
        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        List<String[]> filas = new ArrayList<>();
        for (String linea : lineas.subList(1, lineas.size())) {
            if (!linea.isBlank()) {
                filas.add(linea.split(",", -1));
            }
        }
        return filas;
    }

    /** La capa la instala la migración V14 (la misma que correrá en Render): aquí solo se comprueba. */
    private void verificarLaCapaDeV14() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM comuna WHERE geom IS NOT NULL AND ST_IsValid(geom)", Long.class))
                .isEqualTo(10L);
        assertThat(jdbc.queryForObject("SELECT valor FROM configuracion WHERE clave = 'comunas_capa_fuente'", String.class))
                .contains("EXPERIMENTAL");
    }

    @Test
    void ningunPuntoDeLaListaDaUnResultadoDistintoAlEsperado() throws IOException {
        verificarLaCapaDeV14();

        Map<String, Punto> puntos = new HashMap<>();
        for (String[] c : filas(COORDENADAS)) {                       // id,tipo,latitud,longitud,...
            puntos.put(c[0], new Punto(c[0], Double.parseDouble(c[2]), Double.parseDouble(c[3])));
        }

        List<String> distintos = new ArrayList<>();
        int asignados = 0;
        int rechazados = 0;
        int total = 0;
        for (String[] r : filas(RESULTADOS)) {   // id,anomalia_origen,tipo_original,esperado_original,resultado_experimental,comunas,dentro_perimetro,...
            Punto p = puntos.get(r[0]);
            assertThat(p).as("coordenadas de " + r[0]).isNotNull();
            boolean dentro = Boolean.parseBoolean(r[6].trim());
            Integer esperado = !dentro ? null
                    : Arrays.stream(r[5].split("\\|")).map(String::trim).map(Integer::valueOf).min(Integer::compare).get();

            Integer obtenido = jdbc.query("""
                    SELECT id_comuna FROM fn_asignar_comuna(ST_SetSRID(ST_MakePoint(?, ?), 4326))""",
                    rs -> rs.next() ? rs.getInt(1) : null, p.longitud(), p.latitud());

            total++;
            if (esperado == null) {
                rechazados++;
            } else {
                asignados++;
            }
            if (!java.util.Objects.equals(esperado, obtenido)) {
                distintos.add(p.id() + ": esperado=" + esperado + ", obtenido=" + obtenido);
            }
        }

        assertThat(total).isEqualTo(74);
        assertThat(asignados).isEqualTo(71);
        assertThat(rechazados).isEqualTo(3);                    // P022, P023 y P024, fuera del perímetro
        assertThat(distintos).as("puntos con resultado distinto al esperado").isEmpty();
    }
}
