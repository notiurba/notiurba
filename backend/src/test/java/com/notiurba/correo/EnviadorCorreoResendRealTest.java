package com.notiurba.correo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.web.client.RestClient;

/**
 * Envío REAL a Resend. Solo corre si existen RESEND_API_KEY y RESEND_PRUEBA_DESTINO (el destinatario);
 * RESEND_REMITENTE es opcional. Sin esas variables se omite, así que no gasta cuota en el día a día.
 */
@EnabledIfEnvironmentVariable(named = "RESEND_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "RESEND_PRUEBA_DESTINO", matches = ".+")
class EnviadorCorreoResendRealTest {

    @Test
    void enviaUnCorreoDePruebaDeVerdad() {
        String remitente = System.getenv().getOrDefault("RESEND_REMITENTE", "").isBlank()
                ? "reportes@reportes.notiurba.com" : System.getenv("RESEND_REMITENTE");
        var tiempos = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5)).withReadTimeout(Duration.ofSeconds(15));
        RestClient cliente = RestClient.builder().baseUrl("https://api.resend.com")
                .requestFactory(ClientHttpRequestFactories.get(tiempos)).build();
        var enviador = new EnviadorCorreoResend(cliente, System.getenv("RESEND_API_KEY"), remitente);

        String id = enviador.enviar(new CorreoSaliente(
                List.of(System.getenv("RESEND_PRUEBA_DESTINO")),
                "Prueba de NotiUrba: envío con Resend",
                "Este correo lo envió el EnviadorCorreoResend de NotiUrba como prueba técnica.\n"
                        + "Si lo ves, el dominio reportes.notiurba.com y la clave de Resend funcionan.",
                null, "NotiUrba (prueba)", null, null));

        assertThat(id).isNotBlank();
        System.out.println("RESEND_ID " + id);
    }
}
