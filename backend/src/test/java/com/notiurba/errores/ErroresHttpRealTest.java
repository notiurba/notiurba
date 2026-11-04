package com.notiurba.errores;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.notiurba.admin.AdminPingController;
import com.notiurba.auth.AuthController;
import com.notiurba.categoria.CategoriaController;
import com.notiurba.categoria.CategoriaRepository;
import com.notiurba.config.SecurityConfig;
import com.notiurba.seguridad.JwtFiltro;
import com.notiurba.seguridad.JwtProveedor;
import com.notiurba.usuario.UsuarioRepository;
import com.notiurba.web.error.ManejadorErrores;

/**
 * Con Tomcat real (MockMvc no hace el redespacho a /error): todo error sale como
 * {codigo, mensaje, campo} y el 403 no se convierte en 401.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:pruebas.properties",
        properties = { "spring.servlet.multipart.max-file-size=1MB", "spring.servlet.multipart.max-request-size=2MB" })
class ErroresHttpRealTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class })
    @Import({ SecurityConfig.class, JwtProveedor.class, JwtFiltro.class, ManejadorErrores.class,
            AdminPingController.class, CategoriaController.class, AuthController.class, FotoDePrueba.class })
    static class Config {
    }

    /** Sustituto de la subida real de fotos (aún no existe): sirve para probar el límite multipart. */
    @RestController
    static class FotoDePrueba {
        @PostMapping("/api/prueba/almacen")
        String almacenCaido() {
            throw new com.notiurba.foto.AlmacenFotosNoDisponibleException("Cloudinary rechazó la petición (HTTP 500)");
        }

        @PostMapping("/api/prueba/foto")
        String subir(@RequestPart("foto") MultipartFile foto) {
            return "ok";
        }
    }

    @LocalServerPort int puerto;
    @Autowired JwtProveedor jwt;
    @MockBean CategoriaRepository categoriaRepository;
    @MockBean UsuarioRepository usuarioRepository;
    @MockBean org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private HttpResponse<String> enviar(HttpRequest.Builder b, String token) throws Exception {
        if (token != null) b.header("Authorization", "Bearer " + token);
        return HttpClient.newHttpClient().send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpRequest.Builder get(String ruta) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta)).GET();
    }

    private void esperar(HttpResponse<String> r, int estado, String codigo, String campo) {
        assertThat(r.statusCode()).isEqualTo(estado);
        assertThat(r.headers().firstValue("Content-Type").orElse("")).startsWith("application/json");
        assertThat(r.body()).contains("\"codigo\":\"" + codigo + "\"")
                .contains("\"campo\":" + (campo == null ? "null" : "\"" + campo + "\""));
    }

    @Test
    void sinTokenOTokenInvalidoDa401() throws Exception {
        esperar(enviar(get("/api/categorias"), null), 401, "NO_AUTENTICADO", null);
        esperar(enviar(get("/api/categorias"), "basura"), 401, "NO_AUTENTICADO", null);
    }

    @Test
    void ciudadanoEnRutaDeAdminDa403NoDa401() throws Exception {
        esperar(enviar(get("/api/admin/ping"), jwt.generar("a@x.com", "CIUDADANO")), 403, "SIN_PERMISO", null);
    }

    @Test
    void rutaInexistenteDa404ConElFormatoUnico() throws Exception {
        esperar(enviar(get("/api/nada"), jwt.generar("a@x.com", "CIUDADANO")), 404, "NO_ENCONTRADO", null);
    }

    @Test
    void validacionDevuelveElPrimerCampoEnOrdenDelFormulario() throws Exception {
        var r = enviar(HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/auth/login"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}")), null);
        esperar(r, 400, "VALIDACION", "correo");
        assertThat(r.body()).contains("Completa el campo «Correo»");
    }

    @Test
    void cuerpoIlegibleDa400() throws Exception {
        var r = enviar(HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/auth/login"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("no es json")), null);
        esperar(r, 400, "VALIDACION", null);
    }

    @Test
    void fotoDeMasDe1MbDa413() throws Exception {
        String limite = "----notiurba";
        byte[] relleno = new byte[1_500_000];
        byte[] cabecera = ("--" + limite + "\r\nContent-Disposition: form-data; name=\"foto\"; filename=\"f.jpg\"\r\n"
                + "Content-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] cierre = ("\r\n--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] cuerpo = new byte[cabecera.length + relleno.length + cierre.length];
        System.arraycopy(cabecera, 0, cuerpo, 0, cabecera.length);
        System.arraycopy(relleno, 0, cuerpo, cabecera.length, relleno.length);
        System.arraycopy(cierre, 0, cuerpo, cabecera.length + relleno.length, cierre.length);
        var r = enviar(HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/prueba/foto"))
                .header("Content-Type", "multipart/form-data; boundary=" + limite)
                .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo)), jwt.generar("a@x.com", "CIUDADANO"));
        esperar(r, 413, "ARCHIVO_MUY_GRANDE", "foto");
    }

    @Test
    void almacenDeFotosCaidoDa502SinDetallesInternos() throws Exception {
        var r = enviar(HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/prueba/almacen"))
                .POST(HttpRequest.BodyPublishers.noBody()), jwt.generar("a@x.com", "CIUDADANO"));
        esperar(r, 502, "ALMACEN_FOTOS_NO_DISPONIBLE", "foto");
        assertThat(r.body()).doesNotContain("Cloudinary").doesNotContain("HTTP 500");
    }
}
