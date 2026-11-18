package com.notiurba.auth.recuperacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import com.notiurba.NotiUrbaApplication;
import com.notiurba.reporte.BarrioDePrueba;
import com.notiurba.reporte.BaseDeDatosTemporal;
import com.notiurba.usuario.TokenVerificacionRepository;
import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

/** Persistencia real del token de recuperación sobre una base PostGIS desechable. */
@SpringBootTest(classes = NotiUrbaApplication.class)
@TestPropertySource(locations = "classpath:pruebas.properties")
class RecuperacionIntegracionTest {

    private static final String CORREO = "recuperacion@example.test";
    private static final String HASH_ANTERIOR = "a".repeat(64);
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
    @Autowired PersistenciaRecuperacion persistencia;
    @MockBean EnviadorEnlaceRecuperacionCorreo enviador;
    @SpyBean TokenVerificacionRepository tokenRepository;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM token_verificacion WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE correo = ?)", CORREO);
        jdbc.update("DELETE FROM usuario WHERE correo = ?", CORREO);
        int barrio = BarrioDePrueba.id(jdbc);
        Long usuario = jdbc.queryForObject("""
                INSERT INTO usuario (nombres, apellidos, id_tipo_documento, numero_documento, id_barrio,
                                     direccion_correspondencia, correo, correo_verificado_en, password_hash,
                                     id_rol, id_aviso_privacidad, acepto_privacidad_en)
                VALUES ('Cuenta', 'Recuperación', 1, 'recuperacion-doc', ?, 'Cra 1 # 2-3', ?, now(), ?, 1, 1, now())
                RETURNING id_usuario""", Long.class, barrio, CORREO, passwordEncoder.encode("Anterior123"));
        OffsetDateTime ahora = OffsetDateTime.now();
        jdbc.update("""
                INSERT INTO token_verificacion
                    (id_usuario, id_tipo_token, token_hash, expira_en, creado_en)
                VALUES (?, (SELECT id_tipo_token FROM tipo_token WHERE nombre = 'Recuperación de contraseña'),
                        ?, ?, ?)""",
                usuario, HASH_ANTERIOR, ahora.plusMinutes(30), ahora.minusMinutes(10));
    }

    @Test
    void unFalloBorraElTokenNuevoYUnaSolicitudPosteriorConservaSoloElAceptado() {
        AtomicInteger intento = new AtomicInteger();
        AtomicBoolean pendienteAntesDelEnvio = new AtomicBoolean(true);
        doAnswer(invocacion -> {
            pendienteAntesDelEnvio.compareAndSet(true, totalTokens() == 2);
            if (intento.getAndIncrement() == 0) {
                throw new IllegalStateException("proveedor sintético caído");
            }
            return null;
        }).when(enviador).enviar(anyString(), anyString());

        servicio.solicitar(CORREO);

        assertThat(totalTokens()).isEqualTo(1);
        assertThat(usadoEn(HASH_ANTERIOR)).isNull();

        servicio.solicitar(CORREO);

        assertThat(pendienteAntesDelEnvio).isTrue();
        assertThat(totalTokens()).isEqualTo(2);
        assertThat(usadoEn(HASH_ANTERIOR)).isNotNull();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM token_verificacion t
                 JOIN usuario u USING (id_usuario)
                WHERE u.correo = ? AND t.usado_en IS NULL AND t.expira_en > now()""",
                Integer.class, CORREO)).isEqualTo(1);
    }

    @Test
    void cuatroSolicitudesSimultaneasReservanComoMaximoTresTokens() throws Exception {
        jdbc.update("DELETE FROM token_verificacion WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE correo = ?)", CORREO);
        int cantidad = 4;
        CountDownLatch listos = new CountDownLatch(cantidad);
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService ejecutor = Executors.newFixedThreadPool(cantidad);
        try {
            List<Future<PersistenciaRecuperacion.Estado>> resultados = java.util.stream.IntStream.range(0, cantidad)
                    .mapToObj(indice -> ejecutor.submit(() -> {
                        listos.countDown();
                        inicio.await();
                        return persistencia.preparar(CORREO, Integer.toHexString(indice).repeat(64),
                                OffsetDateTime.now(), Duration.ofHours(1), 3).estado();
                    })).toList();
            listos.await();
            inicio.countDown();

            long preparadas = 0;
            for (Future<PersistenciaRecuperacion.Estado> resultado : resultados) {
                if (resultado.get() == PersistenciaRecuperacion.Estado.LISTA) {
                    preparadas++;
                }
            }
            assertThat(preparadas).isEqualTo(3);
            assertThat(totalTokens()).isEqualTo(3);
        } finally {
            ejecutor.shutdownNow();
        }
    }

    @Test
    void unPendienteNoSirveHastaConfirmarseYUnoConsumidoNuncaSeReactiva() {
        jdbc.update("DELETE FROM token_verificacion WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE correo = ?)", CORREO);
        String tokenPlano = "token-pendiente-seguro";
        String hash = RecuperacionServicio.hash(tokenPlano);
        PersistenciaRecuperacion.Preparacion preparada = persistencia.preparar(
                CORREO, hash, OffsetDateTime.now(), Duration.ofHours(1), 3);
        assertThat(preparada.estado()).isEqualTo(PersistenciaRecuperacion.Estado.LISTA);

        esEnlaceInvalido(() -> servicio.restablecer(tokenPlano, "AntesDeAceptar2026"));
        assertThat(persistencia.confirmar(preparada, OffsetDateTime.now())).isTrue();

        servicio.restablecer(tokenPlano, "NuevaClave2026");
        Object consumidoEn = usadoEn(hash);
        assertThat(consumidoEn).isNotNull();

        assertThat(persistencia.confirmar(preparada, OffsetDateTime.now())).isFalse();
        assertThat(usadoEn(hash)).isEqualTo(consumidoEn);
        esEnlaceInvalido(() -> servicio.restablecer(tokenPlano, "OtraClave2026"));
    }

    @Test
    void siFallaLaInvalidacionLaConfirmacionCompletaSeDeshaceYElTokenSigueSinServir() {
        jdbc.update("DELETE FROM token_verificacion WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE correo = ?)", CORREO);
        String tokenPlano = "token-confirmacion-atomica";
        String hash = RecuperacionServicio.hash(tokenPlano);
        PersistenciaRecuperacion.Preparacion preparada = persistencia.preparar(
                CORREO, hash, OffsetDateTime.now(), Duration.ofHours(1), 3);
        doThrow(new IllegalStateException("fallo sintético")).when(tokenRepository)
                .invalidarVigentesExcepto(any(), any(), anyString(), any());

        assertThatThrownBy(() -> persistencia.confirmar(preparada, OffsetDateTime.now()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(jdbc.queryForObject("SELECT usado_en = expira_en FROM token_verificacion WHERE token_hash = ?",
                Boolean.class, hash)).isTrue();
        esEnlaceInvalido(() -> servicio.restablecer(tokenPlano, "NoDebeFuncionar2026"));
    }

    @Test
    void unPendienteRecienteNoSeBorraYOcupaCupo() {
        jdbc.update("DELETE FROM token_verificacion WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE correo = ?)", CORREO);
        OffsetDateTime ahora = OffsetDateTime.now();
        for (int indice = 0; indice < 3; indice++) {
            insertarPendiente(("e" + indice).repeat(32), ahora.minusMinutes(1), ahora.plusMinutes(59));
        }

        PersistenciaRecuperacion.Preparacion nueva = persistencia.preparar(
                CORREO, "c".repeat(64), ahora, Duration.ofHours(1), 3);

        assertThat(nueva.estado()).isEqualTo(PersistenciaRecuperacion.Estado.LIMITE);
        assertThat(totalTokens()).isEqualTo(3);
    }

    /** Usa la hora de Java y no now() de la base: el reloj del contenedor puede ir desfasado. */
    private void insertarPendiente(String hash, OffsetDateTime creadoEn, OffsetDateTime expiraEn) {
        Long usuario = jdbc.queryForObject("SELECT id_usuario FROM usuario WHERE correo = ?", Long.class, CORREO);
        jdbc.update("""
                INSERT INTO token_verificacion
                    (id_usuario, id_tipo_token, token_hash, creado_en, expira_en, usado_en)
                VALUES (?, (SELECT id_tipo_token FROM tipo_token WHERE nombre = 'Recuperación de contraseña'),
                        ?, ?, ?, ?)""",
                usuario, hash, creadoEn, expiraEn, expiraEn);
    }

    private static void esEnlaceInvalido(org.assertj.core.api.ThrowableAssert.ThrowingCallable accion) {
        assertThatThrownBy(accion).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(e.getCodigo()).isEqualTo(Codigos.ENLACE_INVALIDO);
        });
    }

    @Test
    void unaSolicitudNuevaReconciliaPendientesAntiguosYRecuperaElCupo() {
        jdbc.update("DELETE FROM token_verificacion WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE correo = ?)", CORREO);
        OffsetDateTime ahora = OffsetDateTime.now();
        for (int indice = 0; indice < 3; indice++) {
            insertarPendiente(("f" + indice).repeat(32), ahora.minusMinutes(10), ahora.plusMinutes(50));
        }

        PersistenciaRecuperacion.Preparacion nueva = persistencia.preparar(
                CORREO, "b".repeat(64), ahora, Duration.ofHours(1), 3);

        assertThat(nueva.estado()).isEqualTo(PersistenciaRecuperacion.Estado.LISTA);
        assertThat(totalTokens()).isEqualTo(1);
        assertThat(nueva.token().getUsadoEn()).isEqualTo(nueva.token().getExpiraEn());
    }

    private int totalTokens() {
        return jdbc.queryForObject("""
                SELECT count(*) FROM token_verificacion t
                 JOIN usuario u USING (id_usuario) WHERE u.correo = ?""", Integer.class, CORREO);
    }

    private Object usadoEn(String hash) {
        return jdbc.queryForObject("SELECT usado_en FROM token_verificacion WHERE token_hash = ?",
                (resultado, fila) -> resultado.getObject(1), hash);
    }
}
