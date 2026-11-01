package com.notiurba.foto;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Crea el almacén de fotos. La app arranca aunque falten las credenciales: solo falla (con un
 * mensaje claro) cuando alguien intenta guardar una foto.
 */
@Configuration
public class FotosConfig {

    @Bean
    AlmacenFotos almacenFotos(
            RestClient.Builder builder,
            @Value("${notiurba.fotos.cloudinary.url}") String url,
            @Value("${notiurba.fotos.cloudinary.cloud-name:}") String cloudName,
            @Value("${notiurba.fotos.cloudinary.api-key:}") String apiKey,
            @Value("${notiurba.fotos.cloudinary.api-secret:}") String apiSecret,
            @Value("${notiurba.fotos.cloudinary.carpeta}") String carpeta) {
        var tiempos = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(20));
        RestClient cliente = builder.baseUrl(url + "/v1_1/" + cloudName)
                .requestFactory(ClientHttpRequestFactories.get(tiempos))
                .build();
        return new AlmacenFotosCloudinary(cliente, cloudName, apiKey, apiSecret, carpeta, Clock.systemUTC());
    }
}
