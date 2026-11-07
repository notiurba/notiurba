package com.notiurba.correo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class EnviadorCorreoResendTest {

    private static final String CLAVE = "re_clave_ficticia_de_prueba";

    private MockRestServiceServer servidor;
    private EnviadorCorreoResend enviador;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.resend.test");
        servidor = MockRestServiceServer.bindTo(builder).build();
        enviador = new EnviadorCorreoResend(builder.build(), CLAVE, "reportes@reportes.notiurba.com");
    }

    private static CorreoSaliente correo() {
        return new CorreoSaliente(List.of("servicioalciudadano@tulua.gov.co"), "Reporte NU-000001",
                "Hay un hueco en la vía.", null, "Ana Gómez", "ana@correo.com", "reporte-1");
    }

    @Test
    void enviaConElFormatoDeResendYDevuelveElId() {
        servidor.expect(requestTo("https://api.resend.test/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + CLAVE))
                .andExpect(header("Idempotency-Key", "reporte-1"))
                .andExpect(jsonPath("$.from").value("Ana Gómez <reportes@reportes.notiurba.com>"))
                .andExpect(jsonPath("$.to[0]").value("servicioalciudadano@tulua.gov.co"))
                .andExpect(jsonPath("$.subject").value("Reporte NU-000001"))
                .andExpect(jsonPath("$.text").value("Hay un hueco en la vía."))
                .andExpect(jsonPath("$.reply_to").value("ana@correo.com"))
                .andExpect(jsonPath("$.html").doesNotExist())
                .andRespond(withSuccess("{\"id\":\"abc-123\"}", MediaType.APPLICATION_JSON));

        assertThat(enviador.enviar(correo())).isEqualTo("abc-123");
        servidor.verify();
    }

    @Test
    void sinNombreDeRemitenteUsaNotiUrbaYElNombreNoRompeElEncabezado() {
        servidor.expect(requestTo("https://api.resend.test/emails"))
                .andExpect(jsonPath("$.from").value("NotiUrba <reportes@reportes.notiurba.com>"))
                .andRespond(withSuccess("{\"id\":\"1\"}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo("https://api.resend.test/emails"))
                .andExpect(jsonPath("$.from").value("Ana Bcc: evil@x.com <reportes@reportes.notiurba.com>"))
                .andRespond(withSuccess("{\"id\":\"2\"}", MediaType.APPLICATION_JSON));

        enviador.enviar(new CorreoSaliente(List.of("a@b.com"), "x", "y", null, null, null, null));
        enviador.enviar(new CorreoSaliente(List.of("a@b.com"), "x", "y", null, "Ana\r\nBcc: evil@x.com\"<", null, null));
        servidor.verify();
    }

    @Test
    void errorDeDatosNoEsReintentableYNoFiltraLaClave() {
        servidor.expect(requestTo("https://api.resend.test/emails"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"name\":\"validation_error\",\"message\":\"Dominio no verificado\"}"));

        assertThatThrownBy(() -> enviador.enviar(correo()))
                .isInstanceOfSatisfying(CorreoNoEnviadoException.class, e -> {
                    assertThat(e.reintentable()).isFalse();
                    assertThat(e.getMessage()).contains("422").contains("Dominio no verificado").doesNotContain(CLAVE);
                });
    }

    @Test
    void limiteDeEnviosYErroresDelServidorSonReintentables() {
        servidor.expect(requestTo("https://api.resend.test/emails")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        servidor.expect(requestTo("https://api.resend.test/emails")).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> enviador.enviar(correo()))
                    .isInstanceOfSatisfying(CorreoNoEnviadoException.class, e -> assertThat(e.reintentable()).isTrue());
        }
    }

    @Test
    void sinClaveONoRemitenteFallaSinLlamarARed() {
        var cliente = mock(RestClient.class);
        var sinClave = new EnviadorCorreoResend(cliente, "", "pruebas@example.test");
        var sinRemitente = new EnviadorCorreoResend(cliente, CLAVE, null);

        for (EnviadorCorreo e : List.of(sinClave, sinRemitente)) {
            assertThatThrownBy(() -> e.enviar(correo()))
                    .isInstanceOfSatisfying(CorreoNoEnviadoException.class, ex -> {
                        assertThat(ex.reintentable()).isFalse();
                        assertThat(ex.getMessage()).contains("no está configurado");
                    });
        }
        verifyNoInteractions(cliente);
    }

    @Test
    void respuestaSinIdSeTrataComoFallo() {
        servidor.expect(requestTo("https://api.resend.test/emails"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> enviador.enviar(correo())).isInstanceOf(CorreoNoEnviadoException.class);
    }

    @Test
    void elCorreoExigeDestinatarioAsuntoYTexto() {
        assertThatThrownBy(() -> new CorreoSaliente(List.of(), "a", "b", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CorreoSaliente(List.of("a@b.com"), " ", "b", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
