package com.notiurba.correo;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PlantillaReporte {
    public record Datos(String codigo, String entidad, String dependencia, String categoria, String ciudadano,
            String documento, String direccionCorrespondencia, String correo, String telefono,
            String descripcion, String direccionIncidente, String barrio, String comuna,
            Double latitud, Double longitud, OffsetDateTime creadoEn, OffsetDateTime autorizacion,
            String matricula, List<String> fotos) { }

    public CorreoSaliente crear(Datos d, String destino, String clave) {
        if (d.autorizacion() == null) throw new IllegalArgumentException("Falta autorización de remisión");
        if (destino == null || !destino.matches("[^\\s<>@]+@[^\\s<>@]+\\.[^\\s<>@]+")
            || d.correo() == null || !d.correo().matches("[^\\s<>@]+@[^\\s<>@]+\\.[^\\s<>@]+"))
            throw new IllegalArgumentException("Dirección de correo no válida");
        if (!d.codigo().matches("NU-[0-9]{6,}")) throw new IllegalArgumentException("Código no válido");
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("Código único de seguimiento NotiUrba", d.codigo());
        campos.put("Entidad destinataria", d.entidad()); campos.put("Dependencia competente", d.dependencia());
        campos.put("Categoría seleccionada", d.categoria()); campos.put("Nombre completo del ciudadano", d.ciudadano());
        campos.put("Tipo y número de documento", d.documento()); campos.put("Dirección de correspondencia", d.direccionCorrespondencia());
        campos.put("Correo electrónico", d.correo()); campos.put("Teléfono", d.telefono());
        campos.put("Descripción del problema", d.descripcion()); campos.put("Dirección del incidente", d.direccionIncidente());
        campos.put("Barrio", d.barrio()); campos.put("Comuna", d.comuna());
        campos.put("Coordenadas GPS", d.latitud() == null ? null : d.latitud() + ", " + d.longitud());
        campos.put("Fecha y hora (America/Bogota)", d.creadoEn().atZoneSameInstant(java.time.ZoneId.of("America/Bogota")).toString());
        campos.put("Constancia de autorización del ciudadano", "Remisión autorizada el " + d.autorizacion());
        campos.put("Matrícula del servicio", d.matricula());
        StringBuilder texto = new StringBuilder("Reporte ciudadano NotiUrba\n");
        StringBuilder html = new StringBuilder("""
            <!doctype html><html lang="es"><head><meta name="viewport" content="width=device-width, initial-scale=1"></head>
            <body style="margin:0;background:#f1f5f9;font-family:Arial,sans-serif;color:#1f2937">
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0"><tr><td align="center">
            <table role="presentation" width="100%" style="max-width:640px;background:white" cellpadding="20" cellspacing="0">
            <tr><td style="background:#0a2f6b;color:white"><h1 style="font-size:24px">Reporte ciudadano NotiUrba</h1></td></tr>
            """);
        campos.forEach((k,v) -> {
            String valor = v == null || v.isBlank() ? "No aportado" : v;
            texto.append(k).append(": ").append(valor).append('\n');
            html.append("<tr><td style=\"border-bottom:1px solid #d7e2eb;overflow-wrap:anywhere\"><strong>")
                .append(esc(k)).append("</strong><br>").append(esc(valor).replace("\n", "<br>")).append("</td></tr>");
        });
        if (d.latitud() != null && d.longitud() != null) {
            String mapa = "https://www.openstreetmap.org/?mlat=" + d.latitud() + "&mlon=" + d.longitud();
            enlace(html, texto, "Ver ubicación del incidente en el mapa", mapa);
        }
        html.append("<tr><td><h2 style=\"font-size:18px\">Relación de evidencias aportadas</h2>");
        texto.append("Relación de evidencias aportadas: ").append(d.fotos().size()).append('\n');
        if (d.fotos().isEmpty()) html.append("Sin fotografías aportadas.");
        html.append("</td></tr>");
        int n = 0;
        for (String foto : d.fotos()) enlace(html, texto, "Fotografía " + ++n, foto);
        html.append("<tr><td>Este código es de NotiUrba, no un radicado oficial. Responder al correo del ciudadano. Los enlaces de fotos pueden ser públicos: no reenviar innecesariamente.</td></tr></table></td></tr></table></body></html>");
        return new CorreoSaliente(List.of(destino), "NotiUrba #" + d.codigo(), texto.toString(), html.toString(), d.ciudadano(), d.correo(), clave);
    }
    private static void enlace(StringBuilder html, StringBuilder texto, String etiqueta, String url) {
        if (!enlaceSeguro(url)) throw new IllegalArgumentException("Enlace de evidencia no seguro");
        texto.append(etiqueta).append(": ").append(url).append('\n');
        html.append("<tr><td><a style=\"color:#0a2f6b\" href=\"").append(esc(url)).append("\">").append(etiqueta).append("</a></td></tr>");
    }
    public static boolean enlaceSeguro(String url) {
        try {
            URI u = URI.create(url);
            return "https".equalsIgnoreCase(u.getScheme()) && u.getUserInfo() == null && u.getFragment() == null
                && (u.getPort() == -1 || u.getPort() == 443)
                && ("res.cloudinary.com".equalsIgnoreCase(u.getHost()) || "www.openstreetmap.org".equalsIgnoreCase(u.getHost()));
        } catch (RuntimeException e) { return false; }
    }
    static String esc(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }
}
