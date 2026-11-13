package com.notiurba.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.notiurba.NotiUrbaApplication;
import com.notiurba.correo.EnviadorCorreo;
import com.notiurba.foto.AlmacenFotos;
import com.notiurba.reporte.BaseDeDatosTemporal;
import com.notiurba.seguridad.JwtProveedor;

/** GET y PUT de los límites: valores, rangos, todo o nada, auditoría y permisos. */
@SpringBootTest(classes = NotiUrbaApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:pruebas.properties")
class ParametrosIntegracionTest {

    private static BaseDeDatosTemporal bd;

    @BeforeAll
    static void crearBase() {
        bd = BaseDeDatosTemporal.crearOOmitir();
    }

    @AfterAll
    static void borrarBase() throws SQLException {
        if (bd != null) {
            bd.borrar();
        }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        bd.registrar(registro);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtProveedor jwt;
    @Autowired ParametrosServicio parametros;
    @MockBean EnviadorCorreo correoExterno;
    @MockBean AlmacenFotos fotosExternas;

    private String admin;
    private String ciudadano;

    private static final String VALIDO = """
            {"login":{"intentosMaximos":4,"ventanaMinutos":10,"bloqueoMinutos":15},
             "reportesMaximosPorDia":3,
             "duplicados":{"ventanaDias":20,"categorias":[{"idCategoria":1,"metros":6},{"idCategoria":11,"metros":30}]}}""";

    @BeforeEach
    void preparar() {
        admin = "Bearer " + jwt.generar("admin.dev@notiurba.local", "ADMIN");
        ciudadano = "Bearer " + jwt.generar("ana@example.test", "CIUDADANO");
        jdbc.update("UPDATE configuracion SET valor = '3' WHERE clave = 'login_intentos_maximos'");
        jdbc.update("UPDATE configuracion SET valor = '5' WHERE clave IN ('login_ventana_minutos', 'login_bloqueo_minutos')");
        jdbc.update("UPDATE configuracion SET valor = '2' WHERE clave = 'reportes_maximos_por_dia'");
        jdbc.update("UPDATE configuracion SET valor = '30' WHERE clave = 'duplicado_ventana_dias'");
        jdbc.update("UPDATE distancia_duplicado SET metros = 5 WHERE id_categoria = 1");
        jdbc.update("UPDATE distancia_duplicado SET metros = 10 WHERE id_categoria = 11");
    }

    private org.springframework.test.web.servlet.ResultActions guardar(String cuerpo) throws Exception {
        return mvc.perform(put("/api/admin/configuracion/limites").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    @Test
    void consultaDevuelveLosValoresActualesYLasCategoriasConUbicacion() throws Exception {
        mvc.perform(get("/api/admin/configuracion/limites").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login.intentosMaximos").value(3))
                .andExpect(jsonPath("$.login.ventanaMinutos").value(5))
                .andExpect(jsonPath("$.login.bloqueoMinutos").value(5))
                .andExpect(jsonPath("$.reportesMaximosPorDia").value(2))
                .andExpect(jsonPath("$.duplicados.ventanaDias").value(30))
                .andExpect(jsonPath("$.duplicados.categorias.length()").value(10))
                .andExpect(jsonPath("$.duplicados.categorias[0].idCategoria").value(1))
                .andExpect(jsonPath("$.duplicados.categorias[0].metros").value(5))
                .andExpect(jsonPath("$.duplicados.categorias[0].nombre").isNotEmpty());
    }

    @Test
    void guardarAplicaDeInmediatoYDejaQuienYCuando() throws Exception {
        guardar(VALIDO).andExpect(status().isOk())
                .andExpect(jsonPath("$.reportesMaximosPorDia").value(3))
                .andExpect(jsonPath("$.duplicados.categorias[?(@.idCategoria==11)].metros").value(30));

        assertThat(parametros.intentosMaximos()).isEqualTo(4);
        assertThat(parametros.ventanaLoginMinutos()).isEqualTo(10);
        assertThat(parametros.bloqueoLoginMinutos()).isEqualTo(15);
        assertThat(parametros.reportesMaximosPorDia()).isEqualTo(3);
        assertThat(parametros.ventanaDuplicadoDias()).isEqualTo(20);
        assertThat(parametros.distanciaDuplicado(1)).contains(6);
        assertThat(parametros.distanciaDuplicado(6)).isEmpty();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM configuracion c JOIN usuario u ON u.id_usuario = c.id_usuario_actualiza
                 WHERE u.correo = 'admin.dev@notiurba.local' AND c.clave = 'reportes_maximos_por_dia'""", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM distancia_duplicado WHERE id_categoria = 11 AND id_usuario_actualiza IS NOT NULL""", Integer.class)).isEqualTo(1);
    }

    @Test
    void fueraDeRangoDa400ConElCampoYNoCambiaNada() throws Exception {
        String[][] casos = {
                {"\"intentosMaximos\":4", "\"intentosMaximos\":0", "intentosMaximos"},
                {"\"intentosMaximos\":4", "\"intentosMaximos\":11", "intentosMaximos"},
                {"\"ventanaMinutos\":10", "\"ventanaMinutos\":61", "ventanaMinutos"},
                {"\"bloqueoMinutos\":15", "\"bloqueoMinutos\":0", "bloqueoMinutos"},
                {"\"reportesMaximosPorDia\":3", "\"reportesMaximosPorDia\":0", "reportesMaximosPorDia"},
                {"\"reportesMaximosPorDia\":3", "\"reportesMaximosPorDia\":51", "reportesMaximosPorDia"},
                {"\"ventanaDias\":20", "\"ventanaDias\":31", "ventanaDias"},
                {"\"metros\":6", "\"metros\":4", "categorias.1.metros"},
                {"\"metros\":30", "\"metros\":501", "categorias.11.metros"},
                {"\"idCategoria\":11", "\"idCategoria\":6", "categorias"}};
        for (String[] caso : casos) {
            guardar(VALIDO.replace(caso[0], caso[1])).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                    .andExpect(jsonPath("$.campo").value(caso[2]));
        }
        assertThat(parametros.intentosMaximos()).isEqualTo(3);
        assertThat(parametros.reportesMaximosPorDia()).isEqualTo(2);
        assertThat(parametros.distanciaDuplicado(1)).contains(5);
    }

    @Test
    void losMensajesNombranLaCategoriaYConcuerdanEnNumero() throws Exception {
        guardar(VALIDO.replace("\"metros\":6", "\"metros\":4"))
                .andExpect(jsonPath("$.mensaje").value("La distancia de «Vías, huecos, andenes e infraestructura» debe estar entre 5 y 500 metros."));
        guardar(VALIDO.replace("\"reportesMaximosPorDia\":3", "\"reportesMaximosPorDia\":0"))
                .andExpect(jsonPath("$.mensaje").value("Los reportes por cuenta al día deben estar entre 1 y 50."));
        guardar(VALIDO.replace("\"idCategoria\":11", "\"idCategoria\":6"))
                .andExpect(jsonPath("$.mensaje").value("Esa categoría no se revisa por cercanía: no tiene distancia para reportes parecidos."));
    }

    @Test
    void sinLosDatosDeIngresoOFaltandoUnValorNoGuardaNada() throws Exception {
        guardar("{\"reportesMaximosPorDia\":3,\"duplicados\":{\"ventanaDias\":20}}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campo").value("login"));
        guardar("{\"login\":{\"intentosMaximos\":4,\"ventanaMinutos\":10},\"reportesMaximosPorDia\":3,\"duplicados\":{\"ventanaDias\":20}}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campo").value("bloqueoMinutos"));
        assertThat(parametros.reportesMaximosPorDia()).isEqualTo(2);
    }

    @Test
    void soloElAdministradorPuedeVerYCambiar() throws Exception {
        mvc.perform(get("/api/admin/configuracion/limites")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/configuracion/limites").header("Authorization", ciudadano)).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/configuracion/limites").header("Authorization", ciudadano)
                .contentType(MediaType.APPLICATION_JSON).content(VALIDO)).andExpect(status().isForbidden());
        assertThat(parametros.reportesMaximosPorDia()).isEqualTo(2);
    }
}
