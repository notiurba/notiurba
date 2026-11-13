package com.notiurba.correo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Con RESEND_PRUEBA_DESTINO el correo del reporte no puede llegar a la entidad real. */
class DestinoDePruebaTest {

    private static CorreoSaliente original() {
        return new CorreoSaliente(List.of("servicioalciudadano@tulua.gov.co"), "Petición ciudadana – Vías, huecos y andenes",
                "Cuerpo del reporte", "<p>Cuerpo</p>", "Ana Gómez", "ana@correo.com", "reporte-1");
    }

    @Test
    void conDestinoDePruebaSaleAEsaDireccionYNoALaEntidad() {
        CorreoSaliente c = EnvioReporteServicio.aDestinoDePrueba(original(), " prueba@correo.com ");

        assertThat(c.para()).containsExactly("prueba@correo.com");
        assertThat(c.asunto()).isEqualTo("[PRUEBA] Petición ciudadana – Vías, huecos y andenes");
        assertThat(c.texto()).startsWith("CORREO DE PRUEBA").contains("servicioalciudadano@tulua.gov.co", "Cuerpo del reporte");
        assertThat(c.html()).contains("CORREO DE PRUEBA", "<p>Cuerpo</p>");
        assertThat(c.responderA()).isEqualTo("ana@correo.com");
        assertThat(c.claveIdempotencia()).isEqualTo("reporte-1");
    }

    @Test
    void sinDestinoDePruebaElCorreoQuedaIgual() {
        CorreoSaliente c = original();

        assertThat(EnvioReporteServicio.aDestinoDePrueba(c, "")).isSameAs(c);
        assertThat(EnvioReporteServicio.aDestinoDePrueba(c, null)).isSameAs(c);
        assertThat(EnvioReporteServicio.aDestinoDePrueba(c, "   ")).isSameAs(c);
    }
}
