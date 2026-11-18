package com.notiurba.auth.recuperacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import com.notiurba.NotiUrbaApplication;
import com.notiurba.reporte.BarrioDePrueba;
import com.notiurba.reporte.BaseDeDatosTemporal;

/** Demuestra que la respuesta pública no espera al proveedor real de correo. */
@SpringBootTest(classes = NotiUrbaApplication.class)
@TestPropertySource(locations = "classpath:pruebas.properties",
        properties = "notiurba.segundo-plano.sincrono=false")
class RecuperacionAsincronaIntegracionTest {

    private static final String CORREO = "recuperacion-asincrona@example.test";
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

    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired RecuperacionServicio servicio;
    @MockBean EnviadorEnlaceRecuperacionCorreo enviador;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM token_verificacion WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE correo = ?)", CORREO);
        jdbc.update("DELETE FROM usuario WHERE correo = ?", CORREO);
        int barrio = BarrioDePrueba.id(jdbc);
        jdbc.update("""
                INSERT INTO usuario (nombres, apellidos, id_tipo_documento, numero_documento, id_barrio,
                                     direccion_correspondencia, correo, correo_verificado_en, password_hash,
                                     id_rol, id_aviso_privacidad, acepto_privacidad_en)
                VALUES ('Cuenta', 'Asíncrona', 1, 'recuperacion-async-doc', ?, 'Cra 1 # 2-3', ?, now(), ?, 1, 1, now())""",
                barrio, CORREO, passwordEncoder.encode("Anterior123"));
    }

    @Test
    void solicitarRetornaMientrasElProveedorSigueBloqueadoYActivaDespues() throws Exception {
        CountDownLatch proveedorInicio = new CountDownLatch(1);
        CountDownLatch liberarProveedor = new CountDownLatch(1);
        doAnswer(invocacion -> {
            proveedorInicio.countDown();
            liberarProveedor.await(5, TimeUnit.SECONDS);
            return null;
        }).when(enviador).enviar(anyString(), anyString());

        try {
            servicio.solicitar(CORREO);

            // solicitar ya volvió y el proveedor sigue bloqueado: el token espera pendiente, sin activarse
            assertThat(proveedorInicio.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(tokensPendientes()).isEqualTo(1);
            assertThat(tokensActivos()).isZero();
        } finally {
            liberarProveedor.countDown();
        }

        long limite = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (tokensActivos() != 1 && System.nanoTime() < limite) {
            Thread.sleep(25);
        }
        assertThat(tokensActivos()).isEqualTo(1);
        assertThat(tokensPendientes()).isZero();
    }

    private int tokensPendientes() {
        return jdbc.queryForObject("""
                SELECT count(*) FROM token_verificacion t JOIN usuario u USING (id_usuario)
                 WHERE u.correo = ? AND t.usado_en = t.expira_en""", Integer.class, CORREO);
    }

    private int tokensActivos() {
        return jdbc.queryForObject("""
                SELECT count(*) FROM token_verificacion t JOIN usuario u USING (id_usuario)
                 WHERE u.correo = ? AND t.usado_en IS NULL AND t.expira_en > now()""", Integer.class, CORREO);
    }
}
