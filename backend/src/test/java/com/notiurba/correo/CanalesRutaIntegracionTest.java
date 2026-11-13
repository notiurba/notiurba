package com.notiurba.correo;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import com.notiurba.correo.EnrutamientoServicio.Canal;
import com.notiurba.reporte.BaseDeDatosTemporal;

/** La ruta de cada categoría dice si va por correo y trae los demás canales de la entidad, en orden. */
@SpringBootTest
@TestPropertySource(locations = "classpath:pruebas.properties")
class CanalesRutaIntegracionTest {

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

    @Autowired private EnrutamientoServicio rutas;

    @Test
    void laAlcaldiaYCentroaguasRecibenCorreo() {
        var vias = rutas.resolver(1, false);
        var agua = rutas.resolver(11, false);

        assertThat(vias.permiteCorreo()).isTrue();
        assertThat(vias.acceso()).isEqualTo("servicioalciudadano@tulua.gov.co");
        assertThat(agua.permiteCorreo()).isTrue();
        assertThat(agua.acceso()).isEqualTo("info@centroaguas.com");
    }

    @Test
    void gasEnergiaYAseoNoRecibenCorreoYTraenSusCanales() {
        for (int categoria : new int[] {12, 13, 14}) {
            var ruta = rutas.resolver(categoria, false);

            assertThat(ruta.permiteCorreo()).as("categoría %d", categoria).isFalse();
            assertThat(ruta.canales()).as("categoría %d", categoria).isNotEmpty()
                    .noneMatch(c -> "Correo".equals(c.tipo()))
                    .allMatch(c -> c.finalidad() != null && !c.acceso().isBlank());
        }
    }

    @Test
    void losCanalesVienenEnOrdenConElRecomendadoPrimero() {
        Canal whatsapp = rutas.resolver(14, false).canales().get(0);
        assertThat(whatsapp.tipo()).isEqualTo("WhatsApp");
        assertThat(whatsapp.acceso()).isEqualTo("317 372 5078");
        assertThat(whatsapp.detalle()).contains("Dto Palmira Buga Tuluá");
        assertThat(rutas.resolver(14, false).acceso()).isEqualTo("317 372 5078");
        assertThat(rutas.resolver(12, false).acceso()).isEqualTo("https://www.gdo.com.co/Paginas/Registro-de-pqrs.aspx");

        assertThat(rutas.resolver(12, false).canales()).extracting(Canal::acceso)
                .startsWith("https://www.gdo.com.co/Paginas/Registro-de-pqrs.aspx")
                .doesNotContain("315 665 8536", "inquietudes@gdo.com.co");

        var alcaldia = rutas.resolver(1, false).canales();
        assertThat(alcaldia).extracting(Canal::acceso).containsSubsequence(
                "https://www.tulua.gov.co/publicaciones/6404/pqrsdf/",
                "https://tulua.tramiter.co/documentos/seleccionar-tramite-anonimo",
                "https://tulua.tramiter.co/documentos/consultar-tramite-web",
                "(602) 233 9300");
        assertThat(alcaldia).extracting(Canal::finalidad).contains("PQR anónimo", "Consultar estado");
        assertThat(alcaldia).anyMatch(c -> "Presencial".equals(c.tipo()) && c.horario() != null);
    }

    @Test
    void laEmergenciaDeGasVaAl164YConservaLosCanales() {
        var ruta = rutas.resolver(12, true);

        assertThat(ruta.permiteCorreo()).isFalse();
        assertThat(ruta.acceso()).isEqualTo("164");
        assertThat(ruta.canales()).extracting(Canal::acceso).startsWith("164");
    }

    @Test
    void enUnaEmergenciaLosTelefonosVanPrimero() {
        var inundacion = rutas.resolver(8, true).canales();

        assertThat(inundacion.get(0).tipo()).isEqualTo("Teléfono");
        assertThat(inundacion).extracting(Canal::acceso).startsWith("(602) 233 9300");
        assertThat(rutas.resolver(8, false).canales().get(0).tipo()).isEqualTo("Web");
    }

    @Test
    void cadaCategoriaUsaLaPlantillaDeSuGrupoConLosDatosDeLaBase() {
        var esperado = new java.util.HashMap<Integer, GrupoPlantilla>();
        for (int c : new int[] {1, 2, 3, 4, 5, 8}) esperado.put(c, GrupoPlantilla.A);
        for (int c : new int[] {6, 7, 9, 10}) esperado.put(c, GrupoPlantilla.B);
        for (int c : new int[] {11, 12, 13, 14}) esperado.put(c, GrupoPlantilla.C);

        assertThat(rutas.listar()).allSatisfy(r ->
                assertThat(GrupoPlantilla.de(r)).as("categoría %d", r.idCategoria()).isEqualTo(esperado.get(r.idCategoria())));
    }

    @Test
    void listarTraeLosCanalesDeTodasLasCategorias() {
        assertThat(rutas.listar()).hasSize(14).allMatch(r -> !r.canales().isEmpty());
    }
}
