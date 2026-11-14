package com.notiurba.auth.registro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import com.notiurba.correo.CorreoNoEnviadoException;
import com.notiurba.correo.EnviadorCorreo;
import com.notiurba.foto.AlmacenFotos;
import com.notiurba.reporte.BarrioDePrueba;
import com.notiurba.reporte.BaseDeDatosTemporal;

/** Registrarse no espera al correo: responde rápido y el enlace sale después, con la cuenta ya guardada. */
@SpringBootTest(classes = NotiUrbaApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:pruebas.properties", properties = "notiurba.segundo-plano.sincrono=false")
class RegistroRapidoIntegracionTest {

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
    @MockBean EnviadorCorreo correo;
    @MockBean AlmacenFotos fotos;

    private int barrio;

    @BeforeEach
    void preparar() {
        Mockito.reset(correo);
        barrio = BarrioDePrueba.id(jdbc);
    }

    private String registro(String correo, String documento) {
        return """
                {"nombres":"Ana","apellidos":"Gómez","idTipoDocumento":1,"numeroDocumento":"%s","idBarrio":%d,
                 "direccionCorrespondencia":"Cra 1 # 2-3","correo":"%s","password":"Clave12345",
                 "aceptaPrivacidad":true,"declaraMayorEdad":true}""".formatted(documento, barrio, correo);
    }

    private org.springframework.test.web.servlet.ResultActions registrar(String correo, String documento) throws Exception {
        return mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro(correo, documento)));
    }

    @Test
    void respondeSinEsperarAUnCorreoLentoYElCorreoSaleConLaCuentaYaGuardada() throws Exception {
        AtomicBoolean cuentaYaGuardada = new AtomicBoolean();
        when(correo.enviar(any())).thenAnswer(inv -> {
            Thread.sleep(6000);
            cuentaYaGuardada.set(Boolean.TRUE.equals(jdbc.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM usuario WHERE correo = 'lenta@example.test')", Boolean.class)));
            return null;
        });

        long inicio = System.nanoTime();
        registrar("lenta@example.test", "1001").andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("lenta@example.test"));
        long milisegundos = (System.nanoTime() - inicio) / 1_000_000;

        assertThat(milisegundos).as("el registro no espera a Resend").isLessThan(3000);
        verify(correo, timeout(20000)).enviar(any());
        for (int i = 0; i < 100 && !cuentaYaGuardada.get(); i++) {
            Thread.sleep(200);
        }
        assertThat(cuentaYaGuardada).as("al enviar el correo la cuenta ya estaba guardada").isTrue();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM token_verificacion t JOIN usuario u ON u.id_usuario = t.id_usuario
                 WHERE u.correo = 'lenta@example.test'""", Integer.class)).isEqualTo(1);
    }

    @Test
    void siElRegistroFallaNoSaleNingunCorreo() throws Exception {
        registrar("repetida@example.test", "2001").andExpect(status().isCreated());
        verify(correo, timeout(5000)).enviar(any());
        Mockito.reset(correo);

        registrar("repetida@example.test", "2002").andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CORREO_YA_REGISTRADO"));
        registrar("otra@example.test", "2001").andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DOCUMENTO_YA_REGISTRADO"));

        verify(correo, after(500).never()).enviar(any());
    }

    @Test
    void siElCorreoFallaLaCuentaQuedaYElReenvioEmiteOtroEnlace() throws Exception {
        when(correo.enviar(any())).thenThrow(new CorreoNoEnviadoException("caído", true));

        registrar("sinenvio@example.test", "3001").andExpect(status().isCreated());
        verify(correo, timeout(5000)).enviar(any());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario WHERE correo = 'sinenvio@example.test'", Integer.class)).isEqualTo(1);

        // El límite de un reenvío por minuto cuenta desde el token anterior: se envejece para probar el reenvío
        jdbc.update("""
                UPDATE token_verificacion SET creado_en = now() - interval '2 minutes'
                 WHERE id_usuario = (SELECT id_usuario FROM usuario WHERE correo = 'sinenvio@example.test')""");
        Mockito.reset(correo);
        mvc.perform(post("/api/auth/reenviar-verificacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"sinenvio@example.test\"}"))
                .andExpect(status().isOk());

        verify(correo, timeout(5000)).enviar(any());
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM token_verificacion t JOIN usuario u ON u.id_usuario = t.id_usuario
                 WHERE u.correo = 'sinenvio@example.test' AND t.usado_en IS NULL AND t.expira_en > now()""", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void conDatosNoValidosNoHayCorreo() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(registro("no-es-un-correo", "4001"))).andExpect(status().isBadRequest());
        verify(correo, never()).enviar(any());
    }
}
