package com.notiurba.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notiurba.foto.AlmacenFotos;
import com.notiurba.foto.AlmacenFotosNoDisponibleException;
import com.notiurba.reporte.BarrioDePrueba;
import com.notiurba.reporte.BaseDeDatosTemporal;
import com.notiurba.reporte.DatosDePrueba;
import com.notiurba.reporte.DatosDePrueba.Reporte;
import com.notiurba.seguridad.JwtProveedor;

/** «Eliminar mis datos» y «Eliminar mi cuenta» contra PostgreSQL real, con el almacén de fotos simulado. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:pruebas.properties")
class EliminacionDatosIntegracionTest {

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
    @MockBean private AlmacenFotos almacen;

    private int idBarrio;
    private String correoAna;
    private long idAna;
    private String tokenAna;
    private long idLuis;
    private Reporte reporteDeLuis;

    @BeforeEach
    void preparar() {
        reset(almacen);
        idBarrio = BarrioDePrueba.id(jdbc);
        correoAna = "ana-" + UUID.randomUUID() + "@prueba.com";
        idAna = DatosDePrueba.usuario(jdbc, idBarrio, correoAna, DatosDePrueba.CIUDADANO);
        tokenAna = jwt.generar(correoAna, "CIUDADANO");
        String correoLuis = "luis-" + UUID.randomUUID() + "@prueba.com";
        idLuis = DatosDePrueba.usuario(jdbc, idBarrio, correoLuis, DatosDePrueba.CIUDADANO);
        reporteDeLuis = DatosDePrueba.reporte(jdbc, idLuis, 1, idBarrio, 1);
        DatosDePrueba.foto(jdbc, reporteDeLuis, idLuis, 1, "pub-luis");
    }

    private MvcResult borrar(String ruta, String token) throws Exception {
        var peticion = delete(ruta);
        if (token != null) {
            peticion = peticion.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(peticion).andReturn();
    }

    private int cuenta(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    private void conDosReportesYTresFotosDeAna() {
        Reporte r1 = DatosDePrueba.reporte(jdbc, idAna, 1, idBarrio, 3);
        Reporte r2 = DatosDePrueba.reporte(jdbc, idAna, 2, idBarrio, 2);
        DatosDePrueba.foto(jdbc, r1, idAna, 1, "pub-ana-1");
        DatosDePrueba.foto(jdbc, r1, idAna, 2, "pub-ana-2");
        DatosDePrueba.foto(jdbc, r2, idAna, 1, "pub-ana-3");
        DatosDePrueba.pasarAEstado(jdbc, r1, "Enviado");
        // una nota de Ana en el historial del reporte de Luis: al borrar a Ana el autor pasa a NULL (SET NULL)
        jdbc.update("""
                INSERT INTO historial_estado (id_reporte, id_estado_reporte, observacion, id_usuario_cambia)
                SELECT ?, id_estado_reporte, 'Nota de Ana', ? FROM reporte WHERE id_reporte = ?""",
                reporteDeLuis.id(), idAna, reporteDeLuis.id());
    }

    @Test
    void eliminarMisDatosBorraReportesYFotosPeroConservaLaCuenta() throws Exception {
        conDosReportesYTresFotosDeAna();

        MvcResult r = borrar("/api/usuarios/me/datos", tokenAna);

        assertThat(r.getResponse().getStatus()).isEqualTo(204);
        verify(almacen).borrar("pub-ana-1");
        verify(almacen).borrar("pub-ana-2");
        verify(almacen).borrar("pub-ana-3");
        verify(almacen, never()).borrar("pub-luis");
        assertThat(cuenta("SELECT count(*) FROM reporte WHERE id_usuario = ?", idAna)).isZero();
        assertThat(cuenta("SELECT count(*) FROM evidencia_reporte WHERE id_usuario = ?", idAna)).isZero();
        assertThat(cuenta("SELECT count(*) FROM usuario WHERE id_usuario = ?", idAna)).isEqualTo(1);
        assertThat(cuenta("SELECT count(*) FROM reporte WHERE id_usuario = ?", idLuis)).isEqualTo(1);   // lo de Luis intacto
        assertThat(mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + tokenAna)).andReturn()
                .getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void eliminarMiCuentaBorraTodoLoMioYLaSesionDejaDeServir() throws Exception {
        conDosReportesYTresFotosDeAna();
        jdbc.update("""
                INSERT INTO token_verificacion (id_usuario, id_tipo_token, token_hash, creado_en, expira_en)
                SELECT ?, id_tipo_token, 'hash-' || ?, now(), now() + interval '1 hour' FROM tipo_token LIMIT 1""",
                idAna, UUID.randomUUID().toString());

        MvcResult r = borrar("/api/usuarios/me", tokenAna);

        assertThat(r.getResponse().getStatus()).isEqualTo(204);
        verify(almacen, times(3)).borrar(any());
        assertThat(cuenta("SELECT count(*) FROM usuario WHERE id_usuario = ?", idAna)).isZero();
        assertThat(cuenta("SELECT count(*) FROM reporte WHERE id_usuario = ?", idAna)).isZero();
        assertThat(cuenta("SELECT count(*) FROM token_verificacion WHERE id_usuario = ?", idAna)).isZero();
        assertThat(cuenta("SELECT count(*) FROM usuario WHERE id_usuario = ?", idLuis)).isEqualTo(1);
        assertThat(cuenta("SELECT count(*) FROM historial_estado WHERE observacion = 'Nota de Ana' AND id_usuario_cambia IS NULL"))
                .isEqualTo(1);                                                       // la nota queda, sin autor
        assertThat(mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + tokenAna)).andReturn()
                .getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void siElAlmacenDeFotosFallaNoSeBorraNadaYSePuedeReintentar() throws Exception {
        conDosReportesYTresFotosDeAna();
        doThrow(new AlmacenFotosNoDisponibleException("Cloudinary caído")).when(almacen).borrar(any());

        MvcResult r = borrar("/api/usuarios/me", tokenAna);

        assertThat(r.getResponse().getStatus()).isEqualTo(502);
        assertThat(json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("codigo").asText())
                .isEqualTo("ALMACEN_FOTOS_NO_DISPONIBLE");
        assertThat(cuenta("SELECT count(*) FROM usuario WHERE id_usuario = ?", idAna)).isEqualTo(1);
        assertThat(cuenta("SELECT count(*) FROM reporte WHERE id_usuario = ?", idAna)).isEqualTo(2);
        assertThat(borrar("/api/usuarios/me/datos", tokenAna).getResponse().getStatus()).isEqualTo(502);
        assertThat(cuenta("SELECT count(*) FROM reporte WHERE id_usuario = ?", idAna)).isEqualTo(2);
    }

    @Test
    void laCuentaDeAdministracionNoSeEliminaPorAqui() throws Exception {
        String correoAdmin = "admin-" + UUID.randomUUID() + "@prueba.com";
        long idAdmin = DatosDePrueba.usuario(jdbc, idBarrio, correoAdmin, DatosDePrueba.ADMIN);

        MvcResult r = borrar("/api/usuarios/me", jwt.generar(correoAdmin, "ADMIN"));

        assertThat(r.getResponse().getStatus()).isEqualTo(403);
        assertThat(json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("codigo").asText())
                .isEqualTo("SIN_PERMISO");
        assertThat(cuenta("SELECT count(*) FROM usuario WHERE id_usuario = ?", idAdmin)).isEqualTo(1);
    }

    @Test
    void sinSesionDa401() throws Exception {
        assertThat(borrar("/api/usuarios/me", null).getResponse().getStatus()).isEqualTo(401);
        assertThat(borrar("/api/usuarios/me/datos", null).getResponse().getStatus()).isEqualTo(401);
    }
}
