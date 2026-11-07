package com.notiurba.admin;

import static org.assertj.core.api.Assertions.assertThat;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/** El archivo de pruebas debe resolver los placeholders sin depender del YAML de producción. */
@SpringJUnitConfig(ConfiguracionIntegracionTest.Config.class)
@TestPropertySource(locations = "classpath:pruebas.properties")
class ConfiguracionIntegracionTest {
    @Configuration static class Config { }
    @Autowired Environment entorno;

    @Test void resuelveConfiguracionDeCorreoFotosYVerificacionSinSecretos() {
        for (String clave : new String[]{"notiurba.correo.resend.url", "notiurba.correo.resend.api-key",
                "notiurba.correo.resend.remitente", "notiurba.fotos.cloudinary.url",
                "notiurba.fotos.cloudinary.cloud-name", "notiurba.fotos.cloudinary.api-key",
                "notiurba.fotos.cloudinary.api-secret", "notiurba.fotos.cloudinary.carpeta",
                "notiurba.verificacion.enlace", "notiurba.jwt.secreto", "notiurba.google.client-id"}) {
            assertThat(entorno.resolveRequiredPlaceholders("${"+clave+"}")).doesNotContain("${");
        }
        assertThat(entorno.getProperty("notiurba.correo.resend.api-key")).isEmpty();
        assertThat(entorno.getProperty("notiurba.fotos.cloudinary.api-key")).isEmpty();
        assertThat(entorno.getProperty("notiurba.fotos.cloudinary.api-secret")).isEmpty();
    }

    @Test void proveedoresSonSinteticosYLaColaNoSeActivaPorElEntorno() {
        assertThat(URI.create(entorno.getRequiredProperty("notiurba.correo.resend.url")).getHost()).endsWith(".test");
        assertThat(URI.create(entorno.getRequiredProperty("notiurba.fotos.cloudinary.url")).getHost()).endsWith(".test");
        assertThat(entorno.getProperty("notiurba.correo.reportes-habilitados", Boolean.class)).isFalse();
        assertThat(entorno.getProperty("notiurba.recuperacion.mostrar-enlace-en-log", Boolean.class)).isFalse();
    }
}
