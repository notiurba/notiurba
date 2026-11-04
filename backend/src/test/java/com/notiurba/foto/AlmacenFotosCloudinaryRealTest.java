package com.notiurba.foto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.web.client.RestClient;

/**
 * Subida REAL a Cloudinary: sube una imagen de 1x1 píxel y la borra al terminar. Solo corre si existen
 * CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY y CLOUDINARY_API_SECRET; sin ellas se omite.
 */
@EnabledIfEnvironmentVariable(named = "CLOUDINARY_CLOUD_NAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "CLOUDINARY_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "CLOUDINARY_API_SECRET", matches = ".+")
class AlmacenFotosCloudinaryRealTest {

    private static final byte[] PNG_1X1 = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

    @Test
    void subeUnaFotoDeVerdadYLaBorra() {
        String cloud = System.getenv("CLOUDINARY_CLOUD_NAME");
        var tiempos = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5)).withReadTimeout(Duration.ofSeconds(20));
        RestClient cliente = RestClient.builder().baseUrl("https://api.cloudinary.com/v1_1/" + cloud)
                .requestFactory(ClientHttpRequestFactories.get(tiempos)).build();
        // Carpeta aparte para que la prueba no se mezcle con las fotos de los reportes
        var almacen = new AlmacenFotosCloudinary(cliente, cloud, System.getenv("CLOUDINARY_API_KEY"),
                System.getenv("CLOUDINARY_API_SECRET"), "notiurba/pruebas", Clock.systemUTC());

        FotoGuardada foto = almacen.guardar(PNG_1X1, "image/png");
        try {
            assertThat(foto.url()).startsWith("https://res.cloudinary.com/");
            assertThat(foto.idPublico()).startsWith("notiurba/pruebas/");
            System.out.println("CLOUDINARY_SUBIDA_OK " + foto.idPublico());
        } finally {
            almacen.borrar(foto.idPublico());
        }
    }
}
