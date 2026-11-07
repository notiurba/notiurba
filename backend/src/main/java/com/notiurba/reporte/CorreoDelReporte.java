package com.notiurba.reporte;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.web.util.HtmlUtils;

import com.notiurba.correo.CorreoSaliente;

/**
 * Arma el correo que recibe la entidad (plantilla de NOT-72): asunto «NotiUrba #código», datos del
 * reporte y del ciudadano, enlace a las fotos y la constancia de autorización (RN-07). El ciudadano
 * va en Reply-To y como nombre del remitente; no se le copia (decisión 6 del contrato).
 *
 * <p>Si hay un destino de prueba (RESEND_PRUEBA_DESTINO), el correo sale ahí y NO a la entidad real:
 * así las pruebas y la demo no le escriben a la Alcaldía de verdad.
 */
final class CorreoDelReporte {

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", new Locale("es", "CO")).withZone(ZoneId.of("America/Bogota"));

    private CorreoDelReporte() {
    }

    private record Linea(String etiqueta, String valor) {
    }

    static CorreoSaliente armar(DatosEnvio d, String destinoPrueba) {
        boolean prueba = destinoPrueba != null && !destinoPrueba.isBlank();
        String nombre = (d.nombres() + " " + d.apellidos()).trim();

        List<Linea> reporte = new ArrayList<>();
        reporte.add(new Linea("Código de seguimiento", d.codigoSeguimiento()));
        reporte.add(new Linea("Entidad", d.entidad()));
        reporte.add(new Linea("Categoría", d.categoria()));
        reporte.add(new Linea("Descripción", d.descripcion()));
        reporte.add(new Linea("Ubicación", ubicacion(d)));
        if (d.barrio() != null) {
            reporte.add(new Linea("Barrio", d.barrio() + (d.comuna() == null ? "" : " (comuna " + d.comuna() + ")")));
        } else if (d.comuna() != null) {
            reporte.add(new Linea("Comuna", String.valueOf(d.comuna())));
        }
        if (d.direccionIncidente() != null) {
            reporte.add(new Linea("Dirección del incidente", d.direccionIncidente()));
        }
        if (d.matriculaCuentaServicio() != null) {
            reporte.add(new Linea("Matrícula o cuenta del servicio", d.matriculaCuentaServicio()));
        }
        if (d.radicadoOficial() != null) {
            reporte.add(new Linea("Radicado oficial", d.radicadoOficial()));
        }
        reporte.add(new Linea("Fecha y hora del reporte", FECHA.format(d.creadoEn())));
        for (int i = 0; i < d.fotos().size(); i++) {
            reporte.add(new Linea("Foto " + (i + 1), d.fotos().get(i)));
        }

        List<Linea> ciudadano = List.of(
                new Linea("Nombre", nombre),
                new Linea("Documento", d.tipoDocumento() + " " + d.numeroDocumento()),
                new Linea("Dirección de correspondencia", d.direccionCorrespondencia()),
                new Linea("Correo", d.correoCiudadano()));

        String constancia = "El ciudadano autorizó el " + FECHA.format(d.autorizacionEnvioEn())
                + " que NotiUrba envíe este reporte a la entidad a su nombre y con sus datos. "
                + "Para responderle, responda a este correo.";
        String aviso = prueba
                ? "CORREO DE PRUEBA: el destino real de este reporte habría sido " + d.destino() + "."
                : null;

        String asunto = (prueba ? "[PRUEBA] " : "") + "NotiUrba #" + d.codigoSeguimiento();
        return new CorreoSaliente(
                List.of(prueba ? destinoPrueba.trim() : d.destino()),
                asunto,
                texto(aviso, reporte, ciudadano, constancia),
                html(aviso, reporte, ciudadano, constancia),
                nombre,
                d.correoCiudadano(),
                "reporte-" + d.codigoSeguimiento() + "-" + d.idRegistroEnvio());
    }

    private static String ubicacion(DatosEnvio d) {
        if (d.latitud() == null || d.longitud() == null) {
            return "Sin coordenadas (el ciudadano no compartió su ubicación)";
        }
        String lat = String.format(Locale.ROOT, "%.6f", d.latitud());
        String lon = String.format(Locale.ROOT, "%.6f", d.longitud());
        return lat + ", " + lon + " — https://www.openstreetmap.org/?mlat=" + lat + "&mlon=" + lon
                + "#map=18/" + lat + "/" + lon;
    }

    private static String texto(String aviso, List<Linea> reporte, List<Linea> ciudadano, String constancia) {
        StringBuilder t = new StringBuilder();
        if (aviso != null) {
            t.append("*** ").append(aviso).append(" ***\n\n");
        }
        t.append("REPORTE CIUDADANO\n");
        reporte.forEach(l -> t.append(l.etiqueta()).append(": ").append(l.valor()).append('\n'));
        t.append("\nDATOS DEL CIUDADANO\n");
        ciudadano.forEach(l -> t.append(l.etiqueta()).append(": ").append(l.valor()).append('\n'));
        t.append("\nCONSTANCIA DE AUTORIZACIÓN\n").append(constancia).append('\n');
        return t.toString();
    }

    /** Todo lo que escribe el ciudadano se escapa: un reporte con «&lt;script&gt;» no puede alterar el correo. */
    private static String html(String aviso, List<Linea> reporte, List<Linea> ciudadano, String constancia) {
        StringBuilder h = new StringBuilder("<div style=\"font-family:Arial,sans-serif;font-size:14px;color:#1f2933\">");
        if (aviso != null) {
            h.append("<p style=\"background:#fff4d6;padding:8px\"><strong>").append(HtmlUtils.htmlEscape(aviso)).append("</strong></p>");
        }
        bloque(h, "Reporte ciudadano", reporte);
        bloque(h, "Datos del ciudadano", ciudadano);
        h.append("<h3>Constancia de autorización</h3><p>").append(HtmlUtils.htmlEscape(constancia)).append("</p></div>");
        return h.toString();
    }

    private static void bloque(StringBuilder h, String titulo, List<Linea> lineas) {
        h.append("<h3>").append(titulo).append("</h3><table cellpadding=\"4\">");
        for (Linea l : lineas) {
            h.append("<tr><td><strong>").append(HtmlUtils.htmlEscape(l.etiqueta())).append("</strong></td><td>")
                    .append(HtmlUtils.htmlEscape(l.valor())).append("</td></tr>");
        }
        h.append("</table>");
    }
}
