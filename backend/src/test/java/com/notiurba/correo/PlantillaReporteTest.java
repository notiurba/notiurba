package com.notiurba.correo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.notiurba.correo.PlantillaReporte.Datos;
import com.notiurba.correo.PlantillaReporte.Mensaje;
import com.notiurba.correo.PlantillaReporte.Seccion;

class PlantillaReporteTest {

    private static final String FOTO_1 = "https://res.cloudinary.com/demo/image/upload/v1/notiurba/a.jpg";
    private static final String FOTO_2 = "https://res.cloudinary.com/demo/image/upload/v1/notiurba/b.jpg";
    private static final OffsetDateTime NUEVE_NOV_10_07 = OffsetDateTime.parse("2026-11-09T15:07:00Z");

    private final PlantillaReporte plantilla = new PlantillaReporte();

    private static Datos.Builder completo() {
        return Datos.builder().entidad("Alcaldía de Tuluá").categoria("Vías, huecos y andenes")
                .nombre("Ana Prueba").tipoDocumento("Cédula de ciudadanía").codigoDocumento("CC")
                .numeroDocumento("123456").direccionCorrespondencia("Calle de prueba 1").correo("ana@example.test")
                .telefono("3001234567").descripcion("Hay un hueco grande frente al colegio.")
                .direccionIncidente("Calle 26 # 30-10").barrio("El Prado").comuna(5)
                .latitud(4.086724).longitud(-76.195).creadoEn(NUEVE_NOV_10_07).autorizacion(NUEVE_NOV_10_07)
                .fotos(List.of(FOTO_1, FOTO_2));
    }

    @Test
    void grupoAEsFormalEnPrimeraPersonaYSinNotiUrbaNiCodigoNiDependencia() {
        Mensaje m = plantilla.componer(completo().build(), GrupoPlantilla.A);

        assertThat(m.asunto()).isEqualTo("Petición ciudadana – Vías, huecos y andenes – El Prado, Comuna 5");
        assertThat(m.texto()).startsWith("Respetados señores:").contains(
                "Yo, Ana Prueba, con cédula de ciudadanía No. 123456, en ejercicio del derecho de petición "
                        + "(Constitución Política, art. 23), me dirijo a ustedes para informar la siguiente situación:",
                "Hechos: Hay un hueco grande frente al colegio.",
                "Ubicación: Calle 26 # 30-10, barrio El Prado, Comuna 5 · "
                        + "https://www.openstreetmap.org/?mlat=4.08672&mlon=-76.19500 · 4.08672, -76.19500\n",
                "Fecha del registro: 9 de noviembre de 2026, 10:07 a. m.",
                "Evidencia: 2 fotografías.",
                "Fotografía 1: " + FOTO_1, "Fotografía 2: " + FOTO_2,
                "Solicito atender esta situación y comunicarme el trámite adelantado y el número de radicado asignado.",
                "Autorizo el uso de mis datos para el trámite de esta petición. "
                        + "Recibiré respuesta en ana@example.test · 3001234567 · Calle de prueba 1.")
                .endsWith("Atentamente,\nAna Prueba · CC 123456");
        for (String cuerpo : List.of(m.texto(), m.html(), m.asunto())) {
            assertThat(cuerpo).doesNotContainIgnoringCase("notiurba #").doesNotContain("NU-", "No aportado",
                    "Dependencia", "Hábitat");
        }
        assertThat(m.html()).doesNotContainIgnoringCase(">NotiUrba").doesNotContain("Reporte ciudadano NotiUrba");
    }

    @Test
    void omiteLasFilasOpcionalesVaciasYLaEvidenciaSinFotos() {
        Mensaje m = plantilla.componer(completo().telefono(" ").direccionIncidente(null).fotos(List.of()).build(),
                GrupoPlantilla.A);

        assertThat(m.texto()).contains("Ubicación: barrio El Prado, Comuna 5",
                        "Recibiré respuesta en ana@example.test · Calle de prueba 1.")
                .doesNotContain("Evidencia", "Fotografía", " ·  · ", "null");
        assertThat(m.html()).doesNotContain("<img", "Evidencia", "null");
        assertThat(m.secciones()).extracting(Seccion::id).doesNotContain("evidencia", "matricula");
    }

    @Test
    void sinUbicacionNiCoordenadasNoHayFilaDeUbicacionNiMapa() {
        Mensaje m = plantilla.componer(completo().barrio(null).comuna(null).latitud(null).longitud(null)
                .direccionIncidente(null).build(), GrupoPlantilla.B);

        assertThat(m.texto() + m.html()).doesNotContain("Ubicación", "openstreetmap", "mapa");
        assertThat(m.asunto()).isEqualTo("Solicitud ciudadana – Vías, huecos y andenes");
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "2026-11-09T15:07:00Z, '9 de noviembre de 2026, 10:07 a. m.'",
            "2026-11-09T18:30:00Z, '9 de noviembre de 2026, 1:30 p. m.'",
            "2026-11-09T17:00:00Z, '9 de noviembre de 2026, 12:00 p. m.'",
            "2026-11-10T05:05:00Z, '10 de noviembre de 2026, 12:05 a. m.'",
            "2026-01-01T04:59:00Z, '31 de diciembre de 2025, 11:59 p. m.'"})
    void laFechaVaEnHoraDeColombiaConEspaciosNormales(String instante, String esperada) {
        Mensaje m = plantilla.componer(completo().creadoEn(OffsetDateTime.parse(instante)).build(), GrupoPlantilla.A);

        assertThat(m.texto()).contains("Fecha del registro: " + esperada + "\n");
        assertThat(m.texto() + m.html()).doesNotContain("\u202F", "\u00A0");
    }

    @Test
    void soloConCoordenadasLaUbicacionEsElMapa() {
        Mensaje m = plantilla.componer(completo().barrio(null).comuna(null).direccionIncidente(null).build(), GrupoPlantilla.A);

        assertThat(m.texto()).contains("Ubicación: https://www.openstreetmap.org/?mlat=4.08672&mlon=-76.19500 · 4.08672, -76.19500\n");
        assertThat(m.html()).contains("<strong>Ubicación:</strong> <a href=");
    }

    @Test
    void unaDescripcionIgualALasCoordenadasNoDuplicaElMapa() {
        Mensaje m = plantilla.componer(completo().descripcion("4.08672, -76.19500").build(), GrupoPlantilla.A);

        assertThat(m.texto().split("openstreetmap", -1)).hasSize(2);
        assertThat(m.html().split("openstreetmap", -1)).hasSize(2);
    }

    @Test
    void lasFotosSalenComoMiniaturasConEnlaceYTextoAlternativo() {
        Mensaje m = plantilla.componer(completo().build(), GrupoPlantilla.A);

        assertThat(m.html()).contains("<a href=\"" + FOTO_1 + "\"", "<a href=\"" + FOTO_2 + "\"",
                "alt=\"Fotografía 1 del reporte\"", "alt=\"Fotografía 2 del reporte\"",
                "src=\"https://res.cloudinary.com/demo/image/upload/c_limit,w_320/v1/notiurba/a.jpg\"",
                "max-width:100%");
        assertThat(m.html().split("<img", -1)).hasSize(3);
    }

    @Test
    void laVistaPreviaCuentaLasFotosSinEnlacesTodavia() {
        Mensaje m = plantilla.componer(completo().fotos(List.of()).totalFotos(2).build(), GrupoPlantilla.A);

        assertThat(m.texto()).contains("Evidencia: 2 fotografías.").doesNotContain("Fotografía 1:");
        assertThat(m.html()).doesNotContain("<img");
    }

    @Test
    void grupoBUsaSolicitudEnLugarDeHechos() {
        Mensaje m = plantilla.componer(completo().categoria("Vivienda").descripcion("Solicito información del subsidio.")
                .fotos(List.of()).build(), GrupoPlantilla.B);

        assertThat(m.asunto()).isEqualTo("Solicitud ciudadana – Vivienda");
        assertThat(m.texto()).contains("Solicitud: Solicito información del subsidio.").doesNotContain("Hechos:");
        assertThat(m.secciones()).filteredOn(s -> s.id().equals("descripcion")).singleElement()
                .extracting(Seccion::etiqueta).isEqualTo("Solicitud");
    }

    @Test
    void grupoCConMatriculaHablaComoUsuarioDelServicio() {
        Mensaje m = plantilla.componer(completo().entidad("Centroaguas S.A. E.S.P.").categoria("Acueducto y alcantarillado")
                .matricula("998877").build(), GrupoPlantilla.C);

        assertThat(m.asunto()).isEqualTo("Petición ciudadana – Acueducto y alcantarillado");
        assertThat(m.texto()).startsWith("Señores\nCentroaguas S.A. E.S.P.\n\nRespetados señores:").contains(
                "en calidad de usuario del servicio de acueducto y alcantarillado (matrícula o cuenta No. 998877), "
                        + "me dirijo a ustedes")
                .doesNotContain("como ciudadano");
        assertThat(m.secciones()).extracting(Seccion::id).contains("matricula");
    }

    @Test
    void grupoCSinMatriculaHablaComoCiudadano() {
        Mensaje m = plantilla.componer(completo().entidad("Centroaguas S.A. E.S.P.").categoria("Acueducto y alcantarillado")
                .build(), GrupoPlantilla.C);

        assertThat(m.texto()).contains("(Constitución Política, art. 23), como ciudadano, me dirijo a ustedes")
                .doesNotContain("matrícula");
    }

    @Test
    void escapaElHtmlDelCiudadanoYElAsuntoNoTieneSaltosDeLinea() {
        Mensaje m = plantilla.componer(completo().nombre("Ana <script>alert(1)</script>")
                .descripcion("Hueco & daño <img src=x onerror=alert(1)>\nsegunda línea").categoria("Vías\r\nBcc: x@y.z")
                .build(), GrupoPlantilla.A);

        assertThat(m.html()).contains("Ana &lt;script&gt;", "Hueco &amp; daño &lt;img src=x", "<br>segunda línea")
                .doesNotContain("<script", "<img src=x");
        assertThat(m.asunto()).doesNotContain("\r", "\n");
        assertThat(m.texto()).contains("Hechos: Hueco & daño <img src=x onerror=alert(1)>\nsegunda línea");
    }

    @Test
    void elTextoPlanoTraeLosMismosDatosQueElHtml() {
        Mensaje m = plantilla.componer(completo().matricula("998877").build(), GrupoPlantilla.C);

        for (Seccion s : m.secciones()) {
            for (String linea : s.valor().split("\n")) {
                assertThat(m.asunto() + "\n" + m.texto()).as("texto: %s", s.id()).contains(linea);
                assertThat(m.html()).as("html: %s", s.id()).contains(PlantillaReporte.esc(linea));
            }
        }
    }

    @Test
    void lasSeccionesDicenAQuePasoVuelveEditar() {
        Mensaje m = plantilla.componer(completo().build(), GrupoPlantilla.A);

        assertThat(m.secciones()).extracting(Seccion::id, Seccion::paso).containsExactly(
                org.assertj.core.groups.Tuple.tuple("categoria", "categoria"),
                org.assertj.core.groups.Tuple.tuple("descripcion", "detalles"),
                org.assertj.core.groups.Tuple.tuple("ubicacion", "ubicacion"),
                org.assertj.core.groups.Tuple.tuple("fecha", null),
                org.assertj.core.groups.Tuple.tuple("evidencia", "detalles"),
                org.assertj.core.groups.Tuple.tuple("remitente", null));
    }

    @Test
    void crearArmaElCorreoConElCiudadanoComoRemitenteYRespuesta() {
        CorreoSaliente correo = plantilla.crear(completo().build(), GrupoPlantilla.A, "entidad@example.test", "clave");

        assertThat(correo.para()).containsExactly("entidad@example.test");
        assertThat(correo.responderA()).isEqualTo("ana@example.test");
        assertThat(correo.nombreRemitente()).isEqualTo("Ana Prueba");
        assertThat(correo.claveIdempotencia()).isEqualTo("clave");
        assertThat(correo.asunto()).isEqualTo(plantilla.componer(completo().build(), GrupoPlantilla.A).asunto());
    }

    @Test
    void exigeAutorizacionCorreosValidosYEnlacesDeEvidenciaSeguros() {
        assertThatThrownBy(() -> plantilla.crear(completo().autorizacion(null).build(), GrupoPlantilla.A, "e@example.test", "k"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> plantilla.crear(completo().build(), GrupoPlantilla.A, "no-es-correo", "k"))
                .isInstanceOf(IllegalArgumentException.class);
        for (String url : List.of("javascript:alert(1)", "http://res.cloudinary.com/foto",
                "https://res.cloudinary.com.evil.test/foto", "https://usuario@res.cloudinary.com/foto",
                "https://res.cloudinary.com/foto\"><script>")) {
            assertThatThrownBy(() -> plantilla.componer(completo().fotos(List.of(url)).build(), GrupoPlantilla.A))
                    .as(url).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
