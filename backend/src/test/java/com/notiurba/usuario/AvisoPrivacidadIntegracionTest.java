package com.notiurba.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

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

/** V12: la versión 3 del aviso queda como la vigente que se guarda al registrarse (se omite sola sin Docker). */
@SpringBootTest
@TestPropertySource(locations = "classpath:pruebas.properties")
class AvisoPrivacidadIntegracionTest {

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

    @Autowired private CatalogosRegistro catalogos;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void laVigenteQueSeGuardaAlRegistrarseEsLaVersion3() {
        Short vigente = catalogos.avisoPrivacidadVigente();

        assertThat(jdbc.queryForObject("SELECT version FROM aviso_privacidad WHERE id_aviso_privacidad = ?",
                String.class, vigente)).isEqualTo("3");
    }

    @Test
    void laVersion3TraeElTextoRealYLaVersion1ConservaSuMarcadorParaQuienYaLaAcepto() {
        String texto = jdbc.queryForObject("SELECT texto FROM aviso_privacidad WHERE version = '3'", String.class);

        assertThat(texto).startsWith("Ley 1581 de 2012 · Versión 3")
                .contains("Responsable del tratamiento", "notiurba@gmail.com", "NotiUrba no le envía copia del reporte",
                        "Solo mayores de edad", "Cómo ejercerlos")
                .doesNotContain("PENDIENTE");
        // La V3 no se editó: la versión 1.0 sigue existiendo para las cuentas que la aceptaron (p. ej. el admin sembrado)
        assertThat(jdbc.queryForObject("SELECT count(*) FROM aviso_privacidad", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT id_aviso_privacidad FROM usuario WHERE correo = 'admin.dev@notiurba.local'",
                Short.class)).isNotEqualTo(catalogos.avisoPrivacidadVigente());
    }
}
