package com.notiurba.correo;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Crea el enviador de Resend. La app arranca aunque falten la clave o el remitente: solo falla
 * (con un mensaje claro) cuando alguien intenta enviar.
 */
@Configuration
public class CorreoConfig {

    @Bean
    EnviadorCorreo enviadorCorreo(
            RestClient.Builder builder,
            @Value("${notiurba.correo.resend.url}") String url,
            @Value("${notiurba.correo.resend.api-key:}") String apiKey,
            @Value("${notiurba.correo.resend.remitente:}") String remitente) {
        var tiempos = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(15));
        RestClient cliente = builder.baseUrl(url)
                .requestFactory(ClientHttpRequestFactories.get(tiempos))
                .build();
        return new EnviadorCorreoResend(cliente, apiKey, remitente);
    }
}
