package com.notiurba.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.notiurba.NotiUrbaApplication;
import com.notiurba.correo.EnviadorCorreo;
import com.notiurba.foto.AlmacenFotos;
import com.notiurba.reporte.BarrioDePrueba;
import com.notiurba.reporte.BaseDeDatosTemporal;

/** Bloqueo del ingreso por correo, con un reloj que se adelanta a mano para no esperar minutos de verdad. */
@SpringBootTest(classes = NotiUrbaApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:pruebas.properties")
@Import(LoginBloqueoIntegracionTest.Config.class)
class LoginBloqueoIntegracionTest {

    /** Reloj de prueba: arranca en una hora fija y solo avanza cuando la prueba lo pide. */
    static class RelojAjustable extends Clock {
        private final AtomicReference<Instant> ahora = new AtomicReference<>(Instant.parse("2026-11-13T15:00:00Z"));

        void reiniciar() {
            ahora.set(Instant.parse("2026-11-13T15:00:00Z"));
        }

        void avanzar(Duration d) {
            ahora.updateAndGet(i -> i.plus(d));
        }

        @Override public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override public Instant instant() {
            return ahora.get();
        }
    }

    /** BCrypt de verdad, pero cuenta cuántas contraseñas se comparan. */
    static class CodificadorQueCuenta implements PasswordEncoder {
        private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        final AtomicInteger comparaciones = new AtomicInteger();

        @Override public String encode(CharSequence clave) {
            return bcrypt.encode(clave);
        }

        @Override public boolean matches(CharSequence clave, String hash) {
            comparaciones.incrementAndGet();
            return bcrypt.matches(clave, hash);
        }
    }

    @TestConfiguration
    static class Config {
        @Bean @Primary RelojAjustable relojDePrueba() {
            return new RelojAjustable();
        }

        @Bean @Primary CodificadorQueCuenta codificadorQueCuenta() {
            return new CodificadorQueCuenta();
        }
    }

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
    @Autowired RelojAjustable reloj;
    @Autowired PasswordEncoder codificador;
    @Autowired CodificadorQueCuenta contador;
    @MockBean EnviadorCorreo correoExterno;
    @MockBean AlmacenFotos fotosExternas;

    private static final String CORRECTA = "Correcta123";

    @BeforeEach
    void preparar() {
        reloj.reiniciar();
        jdbc.update("DELETE FROM intento_login");
        jdbc.update("UPDATE configuracion SET valor = '3' WHERE clave = 'login_intentos_maximos'");
        jdbc.update("UPDATE configuracion SET valor = '5' WHERE clave IN ('login_ventana_minutos', 'login_bloqueo_minutos')");
        int barrio = BarrioDePrueba.id(jdbc);
        for (String correo : new String[] {"ana@example.test", "luis@example.test"}) {
            if (jdbc.queryForObject("SELECT count(*) FROM usuario WHERE correo = ?", Integer.class, correo) == 0) {
                jdbc.update("""
                        INSERT INTO usuario (nombres, apellidos, id_tipo_documento, numero_documento, id_barrio,
                                             direccion_correspondencia, correo, correo_verificado_en, password_hash,
                                             id_rol, id_aviso_privacidad, acepto_privacidad_en)
                        VALUES ('Ana', 'Gómez', 1, ?, ?, 'Cra 1 # 2-3', ?, now(), ?, 1, 1, now())""",
                        correo.substring(0, 3) + "-doc", barrio, correo, codificador.encode(CORRECTA));
            }
        }
        if (jdbc.queryForObject("SELECT count(*) FROM usuario WHERE correo = 'google@example.test'", Integer.class) == 0) {
            jdbc.update("""
                    WITH u AS (
                      INSERT INTO usuario (nombres, apellidos, id_tipo_documento, numero_documento, id_barrio,
                                           direccion_correspondencia, correo, correo_verificado_en, password_hash,
                                           id_rol, id_aviso_privacidad, acepto_privacidad_en)
                      VALUES ('Gabi', 'Solo', 1, 'gabi-doc', ?, 'Cra 1 # 2-3', 'google@example.test', now(), NULL, 1, 1, now())
                      RETURNING id_usuario)
                    INSERT INTO identidad_externa (id_usuario, id_proveedor_identidad, identificador_externo)
                    SELECT id_usuario, (SELECT id_proveedor_identidad FROM proveedor_identidad WHERE nombre = 'GOOGLE'), 'sub-gabi' FROM u""",
                    barrio);
        }
    }

    private record Respuesta(int estado, String cuerpo, String retryAfter) {
    }

    private Respuesta login(String correo, String password) throws Exception {
        var r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + correo + "\",\"password\":\"" + password + "\"}")).andReturn().getResponse();
        return new Respuesta(r.getStatus(), r.getContentAsString(java.nio.charset.StandardCharsets.UTF_8), r.getHeader("Retry-After"));
    }

    @Test
    void elTercerFalloBloqueaYAunLaContrasenaCorrectaSeRechazaHastaQuePasenLosMinutos() throws Exception {
        var primero = login("ana@example.test", "Mala12345");
        var segundo = login("ana@example.test", "Mala12345");
        assertThat(primero.estado()).isEqualTo(401);
        assertThat(primero.cuerpo()).contains("Correo o contraseña incorrectos.").contains("\"intentosRestantes\":2");
        assertThat(segundo.cuerpo()).contains("\"intentosRestantes\":1");

        var tercero = login("ana@example.test", "Mala12345");
        assertThat(tercero.estado()).isEqualTo(429);
        assertThat(tercero.cuerpo()).contains("INTENTOS_EXCEDIDOS").contains("Espera 5 minutos").contains("\"esperarSegundos\":300")
                .contains("\"bloqueadoHasta\":\"2026-11-13T15:05:00Z\"");
        assertThat(tercero.retryAfter()).isEqualTo("300");

        assertThat(login("ana@example.test", CORRECTA).estado()).as("con la contraseña correcta, bloqueado").isEqualTo(429);

        reloj.avanzar(Duration.ofMinutes(2).plusSeconds(30));
        var aunBloqueado = login("ana@example.test", CORRECTA);
        assertThat(aunBloqueado.estado()).isEqualTo(429);
        assertThat(aunBloqueado.cuerpo()).contains("Espera 3 minutos").contains("\"esperarSegundos\":150");

        reloj.avanzar(Duration.ofMinutes(2).plusSeconds(31));
        var libre = login("ana@example.test", CORRECTA);
        assertThat(libre.estado()).isEqualTo(200);
        assertThat(libre.cuerpo()).contains("token");
    }

    @Test
    void unIngresoCorrectoEmpiezaDeCero() throws Exception {
        login("ana@example.test", "Mala12345");
        login("ana@example.test", "Mala12345");
        assertThat(login("ana@example.test", CORRECTA).estado()).isEqualTo(200);

        assertThat(login("ana@example.test", "Mala12345").cuerpo()).contains("\"intentosRestantes\":2");
        assertThat(login("ana@example.test", "Mala12345").estado()).isEqualTo(401);
        assertThat(login("ana@example.test", "Mala12345").estado()).as("el tercero de la nueva cuenta").isEqualTo(429);
    }

    @Test
    void despuesDeVencerLaBloqueoElContadorEmpiezaDeCeroTambienParaLosFallos() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("ana@example.test", "Mala12345");
        }
        reloj.avanzar(Duration.ofMinutes(6));
        var otraVez = login("ana@example.test", "Mala12345");
        assertThat(otraVez.estado()).isEqualTo(401);
        assertThat(otraVez.cuerpo()).contains("\"intentosRestantes\":2");
    }

    @Test
    void losFallosFueraDeLaVentanaNoSeSuman() throws Exception {
        login("ana@example.test", "Mala12345");
        login("ana@example.test", "Mala12345");
        reloj.avanzar(Duration.ofMinutes(6));
        var r = login("ana@example.test", "Mala12345");
        assertThat(r.estado()).isEqualTo(401);
        assertThat(r.cuerpo()).contains("\"intentosRestantes\":2");
    }

    @Test
    void correoInexistenteSoloGoogleYContrasenaMalaRecibenExactamenteLoMismo() throws Exception {
        String[] correos = {"nadie@example.test", "google@example.test", "luis@example.test"};
        List<List<String>> secuencias = new ArrayList<>();
        for (String correo : correos) {
            List<String> secuencia = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                var r = login(correo, "Mala12345");
                secuencia.add(r.estado() + "|" + r.cuerpo().replaceAll("\"bloqueadoHasta\":\"[^\"]+\"", "")
                        + "|" + r.retryAfter());
            }
            secuencias.add(secuencia);
        }
        assertThat(secuencias.get(1)).isEqualTo(secuencias.get(0));
        assertThat(secuencias.get(2)).isEqualTo(secuencias.get(0));
        assertThat(secuencias.get(0).get(0)).startsWith("401|").contains("Correo o contraseña incorrectos.");
        assertThat(secuencias.get(0).get(2)).startsWith("429|");
        assertThat(secuencias.get(0).get(3)).startsWith("429|");
    }

    @Test
    void mayusculasYEspaciosCuentanComoElMismoCorreo() throws Exception {
        login("ANA@example.test", "Mala12345");
        login(" ana@example.test ", "Mala12345");
        assertThat(login("Ana@Example.test", "Mala12345").estado()).isEqualTo(429);
    }

    @Test
    void elBloqueoDeUnCorreoNoAfectaAOtro() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("ana@example.test", "Mala12345");
        }
        assertThat(login("luis@example.test", CORRECTA).estado()).isEqualTo(200);
    }

    @Test
    void loQueCambiaElAdministradorAplicaDeInmediato() throws Exception {
        jdbc.update("UPDATE configuracion SET valor = '2' WHERE clave = 'login_intentos_maximos'");
        jdbc.update("UPDATE configuracion SET valor = '10' WHERE clave = 'login_bloqueo_minutos'");
        login("ana@example.test", "Mala12345");
        var segundo = login("ana@example.test", "Mala12345");
        assertThat(segundo.estado()).isEqualTo(429);
        assertThat(segundo.cuerpo()).contains("Espera 10 minutos");
    }

    @Test
    void veinteIntentosALaVezNoSeSaltanElTope() throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(20);
        List<Future<Integer>> resultados = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            resultados.add(hilos.submit(() -> login("ana@example.test", "Mala12345").estado()));
        }
        int respuestas401 = 0;
        int respuestas429 = 0;
        for (Future<Integer> f : resultados) {
            int estado = f.get();
            if (estado == 401) respuestas401++;
            if (estado == 429) respuestas429++;
        }
        hilos.shutdown();
        assertThat(respuestas401).as("solo los 2 primeros fallos dejan intentar").isEqualTo(2);
        assertThat(respuestas429).isEqualTo(18);
    }

    @Test
    void unaRafagaAlMismoTiempoNoPruebaMasContrasenasQueElMaximo() throws Exception {
        contador.comparaciones.set(0);
        ExecutorService hilos = Executors.newFixedThreadPool(20);
        List<Future<Integer>> resultados = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            String clave = "Mala" + i + "2345";
            resultados.add(hilos.submit(() -> login("ana@example.test", clave).estado()));
        }
        for (Future<Integer> f : resultados) {
            f.get();
        }
        hilos.shutdown();
        assertThat(contador.comparaciones.get()).as("contraseñas comparadas en la ráfaga").isEqualTo(3);
    }

    @Test
    void laContrasenaCorrectaDentroDelCupoEntraAunqueYaHayaDosFallos() throws Exception {
        login("ana@example.test", "Mala12345");
        login("ana@example.test", "Mala12345");
        assertThat(login("ana@example.test", CORRECTA).estado()).as("el tercer intento es el correcto").isEqualTo(200);
        assertThat(login("ana@example.test", "Mala12345").cuerpo()).contains("\"intentosRestantes\":2");
    }

    @Test
    void laTablaNoEngordaConFilasViejas() throws Exception {
        login("nadie1@example.test", "Mala12345");
        reloj.avanzar(Duration.ofHours(25));
        login("nadie2@example.test", "Mala12345");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM intento_login WHERE correo_normalizado = 'nadie1@example.test'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM intento_login WHERE correo_normalizado = 'nadie2@example.test'", Integer.class)).isEqualTo(1);
    }

    @Test
    void restablecerLaContrasenaConElEnlaceDelCorreoLevantaElBloqueo() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("ana@example.test", "Mala12345");
        }
        assertThat(login("ana@example.test", CORRECTA).estado()).isEqualTo(429);

        String token = "token-de-recuperacion-de-prueba";
        String hash = java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        jdbc.update("""
                INSERT INTO token_verificacion (id_usuario, id_tipo_token, token_hash, expira_en)
                SELECT u.id_usuario, t.id_tipo_token, ?, now() + interval '1 hour'
                  FROM usuario u, tipo_token t WHERE u.correo = 'ana@example.test' AND t.nombre = 'Recuperación de contraseña'""", hash);
        mvc.perform(post("/api/auth/restablecer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"nuevaContrasena\":\"NuevaClave2026\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());

        try {
            assertThat(login("ana@example.test", "NuevaClave2026").estado()).as("sin esperar los 5 minutos").isEqualTo(200);
        } finally {
            jdbc.update("UPDATE usuario SET password_hash = ? WHERE correo = 'ana@example.test'", codificador.encode(CORRECTA));
        }
    }
}
