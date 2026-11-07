package com.notiurba.foto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AlmacenFotosCloudinaryTest {

    private static final String SECRETO = "secreto-ficticio-de-prueba";
    private static final String BASE = "https://api.cloudinary.test/v1_1/demo";
    private static final long MARCA = 1_700_000_000L;

    private MockRestServiceServer servidor;
    private AlmacenFotosCloudinary almacen;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        servidor = MockRestServiceServer.bindTo(builder).build();
        Clock reloj = Clock.fixed(Instant.ofEpochSecond(MARCA), ZoneOffset.UTC);
        almacen = new AlmacenFotosCloudinary(builder.build(), "demo", "clave-ficticia", SECRETO,
                "notiurba/reportes", reloj);
    }

    private static String sha1(String texto) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(texto.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void subeLaFotoFirmadaYDevuelveLaUrlHttpsYElIdentificador() throws Exception {
        String firma = sha1("folder=notiurba/reportes&timestamp=" + MARCA + SECRETO);
        servidor.expect(requestTo(BASE + "/image/upload"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", containsString("multipart/form-data")))
                .andExpect(content().string(containsString("name=\"api_key\"")))
                .andExpect(content().string(containsString("clave-ficticia")))
                .andExpect(content().string(containsString("name=\"folder\"")))
                .andExpect(content().string(containsString("notiurba/reportes")))
                .andExpect(content().string(containsString("name=\"timestamp\"")))
                .andExpect(content().string(containsString(Long.toString(MARCA))))
                .andExpect(content().string(containsString("name=\"signature\"")))
                .andExpect(content().string(containsString(firma)))
                .andExpect(content().string(containsString("Content-Type: image/png")))
                // El secreto firma pero nunca viaja
                .andExpect(content().string(not(containsString(SECRETO))))
                .andRespond(withSuccess("{\"secure_url\":\"https://res.cloudinary.com/demo/image/upload/v1/notiurba/reportes/abc.png\","
                        + "\"public_id\":\"notiurba/reportes/abc\"}", MediaType.APPLICATION_JSON));

        FotoGuardada foto = almacen.guardar(new byte[] { 1, 2, 3 }, "image/png");

        assertThat(foto.url()).isEqualTo("https://res.cloudinary.com/demo/image/upload/v1/notiurba/reportes/abc.png");
        assertThat(foto.idPublico()).isEqualTo("notiurba/reportes/abc");
        servidor.verify();
    }

    @Test
    void borraConLaFirmaDelIdentificadorYAceptaOkYNotFound() throws Exception {
        String firma = sha1("public_id=notiurba/reportes/abc&timestamp=" + MARCA + SECRETO);
        servidor.expect(requestTo(BASE + "/image/destroy"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", containsString("application/x-www-form-urlencoded")))
                .andExpect(content().string(containsString("signature=" + firma)))
                .andExpect(content().string(not(containsString(SECRETO))))
                .andRespond(withSuccess("{\"result\":\"ok\"}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(BASE + "/image/destroy"))
                .andRespond(withSuccess("{\"result\":\"not found\"}", MediaType.APPLICATION_JSON));

        almacen.borrar("notiurba/reportes/abc");
        almacen.borrar("notiurba/reportes/abc");
        servidor.verify();
    }

    @Test
    void siCloudinaryRechazaOFallaSeLanzaUnErrorSinFiltrarSecretos() {
        servidor.expect(requestTo(BASE + "/image/upload")).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\":{\"message\":\"Invalid Signature: " + SECRETO + "\"}}"));
        servidor.expect(requestTo(BASE + "/image/upload")).andRespond(withServerError());

        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> almacen.guardar(new byte[] { 1 }, "image/jpeg"))
                    .isInstanceOf(AlmacenFotosNoDisponibleException.class)
                    .hasMessageContaining("HTTP")
                    .satisfies(e -> assertThat(e.getMessage()).doesNotContain(SECRETO).doesNotContain("clave-ficticia"));
        }
    }

    @Test
    void respuestaSinEnlaceOBorradoSinConfirmarSeTrataComoFallo() {
        servidor.expect(requestTo(BASE + "/image/upload")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(BASE + "/image/destroy"))
                .andRespond(withSuccess("{\"result\":\"error\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> almacen.guardar(new byte[] { 1 }, "image/png"))
                .isInstanceOf(AlmacenFotosNoDisponibleException.class);
        assertThatThrownBy(() -> almacen.borrar("x")).isInstanceOf(AlmacenFotosNoDisponibleException.class);
    }

    @Test
    void sinCredencialesFallaSinLlamarARed() {
        var cliente = mock(RestClient.class);
        var sinNada = new AlmacenFotosCloudinary(cliente, "", "", "", "carpeta", Clock.systemUTC());
        var sinSecreto = new AlmacenFotosCloudinary(cliente, "demo", "clave", null, "carpeta", Clock.systemUTC());

        for (AlmacenFotos a : new AlmacenFotos[] { sinNada, sinSecreto }) {
            assertThatThrownBy(() -> a.guardar(new byte[] { 1 }, "image/png"))
                    .isInstanceOf(AlmacenFotosNoDisponibleException.class)
                    .hasMessageContaining("no están configuradas");
            assertThatThrownBy(() -> a.borrar("x")).isInstanceOf(AlmacenFotosNoDisponibleException.class);
        }
        verifyNoInteractions(cliente);
    }
}
