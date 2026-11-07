package com.notiurba.correo;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.time.OffsetDateTime;
import java.util.List;

class PlantillaReporteTest {
    private PlantillaReporte.Datos datos(OffsetDateTime autorizacion,List<String> fotos) {
        return new PlantillaReporte.Datos("NU-000042","Alcaldía de Tuluá","Hábitat e Infraestructura","Vías",
            "Ana <script>alert(1)</script>","CC 123","Calle de prueba 1","ana@example.test","555",
            "Hueco & daño","Calle del incidente","Barrio prueba","5",4.08,-76.19,OffsetDateTime.now(),autorizacion,"12345",fotos);
    }
    @Test void incluyeTodosLosDatosYAsuntoSinInsertarHtmlDelCiudadano() {
        var correo=new PlantillaReporte().crear(datos(OffsetDateTime.now(),List.of("https://res.cloudinary.com/demo/image/upload/evidencia.jpg")),"entidad@example.test","clave");
        assertThat(correo.asunto()).isEqualTo("NotiUrba #NU-000042");
        assertThat(correo.html()).contains("Entidad destinataria","Dependencia competente","Categoría seleccionada",
            "Nombre completo","Tipo y número","Dirección de correspondencia","Correo electrónico","Teléfono",
            "Descripción del problema","Dirección del incidente","Barrio","Comuna","Coordenadas GPS","Fecha y hora",
            "Relación de evidencias","Constancia de autorización","Fotografía 1","openstreetmap.org",
            "&lt;script&gt;","Hueco &amp; daño").doesNotContain("<script>");
        assertThat(correo.responderA()).isEqualTo("ana@example.test");
        assertThat(correo.para()).containsExactly("entidad@example.test");
    }
    @Test void exigeAutorizacionYEnlacesDeEvidenciaSeguros() {
        assertThatThrownBy(()->new PlantillaReporte().crear(datos(null,List.of()),"e@example.test","k")).isInstanceOf(IllegalArgumentException.class);
        for(String url:List.of("javascript:alert(1)","http://res.cloudinary.com/foto","https://res.cloudinary.com.evil.test/foto","https://usuario@res.cloudinary.com/foto"))
            assertThatThrownBy(()->new PlantillaReporte().crear(datos(OffsetDateTime.now(),List.of(url)),"e@example.test","k")).isInstanceOf(IllegalArgumentException.class);
    }
}
