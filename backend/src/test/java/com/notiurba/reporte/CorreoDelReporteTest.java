package com.notiurba.reporte;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.notiurba.correo.CorreoSaliente;

/** El correo que recibe la entidad (plantilla de NOT-72). */
class CorreoDelReporteTest {

    private static final OffsetDateTime CUANDO = OffsetDateTime.parse("2026-11-05T14:30:00-05:00");

    private static DatosEnvio datos(String descripcion, Double lat, Double lon, List<String> fotos) {
        return new DatosEnvio(7, "NU-000123", "Vías, huecos y andenes", "Alcaldía de Tuluá",
                "servicioalciudadano@tulua.gov.co", descripcion, lat, lon, 4, "La Floresta", "Cra 5 # 10-20",
                null, "RAD-2026-1", CUANDO, CUANDO, "Ana", "Gómez", "CC", "1234567", "Cra 1 # 2-3",
                "ana@correo.com", fotos);
    }

    @Test
    void llevaTodosLosDatosDelReporteYDelCiudadano() {
        CorreoSaliente c = CorreoDelReporte.armar(
                datos("Hay un hueco enorme frente al parque", 4.0867, -76.195, List.of("https://f.com/1.jpg")), null);

        assertThat(c.para()).containsExactly("servicioalciudadano@tulua.gov.co");
        assertThat(c.asunto()).isEqualTo("NotiUrba #NU-000123");
        assertThat(c.nombreRemitente()).isEqualTo("Ana Gómez");
        assertThat(c.responderA()).isEqualTo("ana@correo.com");
        assertThat(c.claveIdempotencia()).isEqualTo("reporte-NU-000123-7");
        assertThat(c.texto()).contains("Hay un hueco enorme frente al parque", "Vías, huecos y andenes",
                "Alcaldía de Tuluá", "4.086700, -76.195000", "openstreetmap.org", "La Floresta (comuna 4)",
                "Cra 5 # 10-20", "RAD-2026-1", "05/11/2026 14:30", "https://f.com/1.jpg",
                "CC 1234567", "Cra 1 # 2-3", "ana@correo.com", "CONSTANCIA DE AUTORIZACIÓN", "NU-000123");
        assertThat(c.texto()).doesNotContain("PRUEBA");
    }

    @Test
    void sinGpsLoDiceYNoInventaCoordenadas() {
        CorreoSaliente c = CorreoDelReporte.armar(datos("Consulta sobre el uso del suelo", null, null, List.of()), null);

        assertThat(c.texto()).contains("Sin coordenadas").doesNotContain("openstreetmap");
    }

    @Test
    void conDestinoDePruebaNoSaleALaEntidadReal() {
        CorreoSaliente c = CorreoDelReporte.armar(datos("Un hueco en la vía", 4.08, -76.19, List.of()), " prueba@correo.com ");

        assertThat(c.para()).containsExactly("prueba@correo.com");
        assertThat(c.asunto()).isEqualTo("[PRUEBA] NotiUrba #NU-000123");
        assertThat(c.texto()).contains("CORREO DE PRUEBA", "servicioalciudadano@tulua.gov.co");
    }

    @Test
    void loQueEscribeElCiudadanoSeEscapaEnElHtml() {
        CorreoSaliente c = CorreoDelReporte.armar(
                datos("<script>alert('x')</script> hueco & más", 4.08, -76.19, List.of()), null);

        assertThat(c.html()).doesNotContain("<script>").contains("&lt;script&gt;").contains("&amp;");
        assertThat(c.texto()).contains("<script>alert('x')</script>");   // el texto plano no interpreta HTML
    }
}
