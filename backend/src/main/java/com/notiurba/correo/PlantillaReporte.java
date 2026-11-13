package com.notiurba.correo;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

/**
 * Carta formal en primera persona del ciudadano (plantillas A, B y C). La vista previa y el correo
 * usan este mismo método, así que dicen exactamente lo mismo.
 */
@Component
public class PlantillaReporte {

    static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");
    private static final String[] MESES = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
            "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
    private static final String FORMATO_CORREO = "[^\\s<>@]+@[^\\s<>@]+\\.[^\\s<>@]+";
    private static final Locale ES = Locale.forLanguageTag("es-CO");

    public record Datos(String entidad, String categoria, String nombre, String tipoDocumento, String codigoDocumento,
            String numeroDocumento, String direccionCorrespondencia, String correo, String telefono,
            String descripcion, String direccionIncidente, String barrio, Integer comuna, Double latitud,
            Double longitud, OffsetDateTime creadoEn, OffsetDateTime autorizacion, String matricula, int totalFotos,
            List<String> fotos) {

        public Datos {
            fotos = fotos == null ? List.of() : List.copyOf(fotos);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String entidad, categoria, nombre, tipoDocumento, codigoDocumento, numeroDocumento,
                    direccionCorrespondencia, correo, telefono, descripcion, direccionIncidente, barrio, matricula;
            private Integer comuna;
            private Double latitud, longitud;
            private OffsetDateTime creadoEn, autorizacion;
            private int totalFotos;
            private List<String> fotos = List.of();

            public Builder entidad(String v) { entidad = v; return this; }
            public Builder categoria(String v) { categoria = v; return this; }
            public Builder nombre(String v) { nombre = v; return this; }
            public Builder tipoDocumento(String v) { tipoDocumento = v; return this; }
            public Builder codigoDocumento(String v) { codigoDocumento = v; return this; }
            public Builder numeroDocumento(String v) { numeroDocumento = v; return this; }
            public Builder direccionCorrespondencia(String v) { direccionCorrespondencia = v; return this; }
            public Builder correo(String v) { correo = v; return this; }
            public Builder telefono(String v) { telefono = v; return this; }
            public Builder descripcion(String v) { descripcion = v; return this; }
            public Builder direccionIncidente(String v) { direccionIncidente = v; return this; }
            public Builder barrio(String v) { barrio = v; return this; }
            public Builder comuna(Integer v) { comuna = v; return this; }
            public Builder latitud(Double v) { latitud = v; return this; }
            public Builder longitud(Double v) { longitud = v; return this; }
            public Builder creadoEn(OffsetDateTime v) { creadoEn = v; return this; }
            public Builder autorizacion(OffsetDateTime v) { autorizacion = v; return this; }
            public Builder matricula(String v) { matricula = v; return this; }
            public Builder totalFotos(int v) { totalFotos = v; return this; }
            public Builder fotos(List<String> v) { fotos = v; return this; }

            public Datos build() {
                return new Datos(entidad, categoria, nombre, tipoDocumento, codigoDocumento, numeroDocumento,
                        direccionCorrespondencia, correo, telefono, descripcion, direccionIncidente, barrio, comuna,
                        latitud, longitud, creadoEn, autorizacion, matricula, totalFotos, fotos);
            }
        }
    }

    /** Un dato del mensaje; paso es la pantalla a la que vuelve «Editar» (null si no se edita en el reporte). */
    public record Seccion(String id, String etiqueta, String valor, String paso) {
    }

    public record Mensaje(String asunto, String texto, String html, List<Seccion> secciones) {
    }

    public CorreoSaliente crear(Datos d, GrupoPlantilla grupo, String destino, String clave) {
        if (d.autorizacion() == null) throw new IllegalArgumentException("Falta autorización de remisión");
        if (destino == null || !destino.matches(FORMATO_CORREO) || d.correo() == null || !d.correo().matches(FORMATO_CORREO))
            throw new IllegalArgumentException("Dirección de correo no válida");
        Mensaje m = componer(d, grupo);
        return new CorreoSaliente(List.of(destino), m.asunto(), m.texto(), m.html(), d.nombre(), d.correo(), clave);
    }

    public Mensaje componer(Datos d, GrupoPlantilla grupo) {
        for (String foto : d.fotos()) {
            if (!enlaceSeguro(foto)) throw new IllegalArgumentException("Enlace de evidencia no seguro");
        }
        String comuna = d.comuna() == null ? null : "Comuna " + d.comuna();
        String ubicacion = unir(", ", d.direccionIncidente(), vacio(d.barrio()) ? null : "barrio " + d.barrio().trim(), comuna);
        boolean hayPunto = d.latitud() != null && d.longitud() != null;
        String coordenadas = hayPunto ? decimal(d.latitud()) + ", " + decimal(d.longitud()) : null;
        String mapa = hayPunto ? "https://www.openstreetmap.org/?mlat=" + decimal(d.latitud()) + "&mlon=" + decimal(d.longitud()) : null;
        String fecha = fecha(Objects.requireNonNull(d.creadoEn(), "creadoEn"));
        int totalFotos = Math.max(d.totalFotos(), d.fotos().size());
        String evidencia = totalFotos == 0 ? null : totalFotos + (totalFotos == 1 ? " fotografía" : " fotografías");
        String etiqueta = grupo == GrupoPlantilla.B ? "Solicitud" : "Hechos";
        String documento = unir(" ", d.codigoDocumento(), d.numeroDocumento());
        String contacto = unir(" · ", d.correo(), d.telefono(), d.direccionCorrespondencia());
        String matricula = grupo == GrupoPlantilla.C && !vacio(d.matricula()) ? d.matricula().trim() : null;
        String asunto = asunto(d, grupo, comuna);

        String intro = "Yo, " + limpio(d.nombre())
                + (vacio(d.numeroDocumento()) ? "" : ", con " + minuscula(d.tipoDocumento(), "documento") + " No. " + d.numeroDocumento().trim())
                + ", en ejercicio del derecho de petición (Constitución Política, art. 23), "
                + (grupo != GrupoPlantilla.C ? ""
                        : matricula != null
                                ? "en calidad de usuario del servicio de " + limpio(d.categoria()).toLowerCase(ES)
                                        + " (matrícula o cuenta No. " + matricula + "), "
                                : "como ciudadano, ")
                + "me dirijo a ustedes para informar la siguiente situación:";
        String solicitud = "Solicito atender esta situación y comunicarme el trámite adelantado y el número de radicado asignado.";
        String autorizo = "Autorizo el uso de mis datos para el trámite de esta petición."
                + (contacto == null ? "" : " Recibiré respuesta en " + contacto + ".");

        StringBuilder t = new StringBuilder();
        StringBuilder h = new StringBuilder("<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>" + esc(asunto) + "</title></head>"
                + "<body style=\"margin:0;padding:0;background:#ffffff;color:#1f2937;font-family:Arial,Helvetica,sans-serif;"
                + "font-size:15px;line-height:1.5\"><div style=\"max-width:600px;margin:0 auto;padding:16px;"
                + "overflow-wrap:anywhere;word-break:break-word\">");
        if (grupo == GrupoPlantilla.C) {
            t.append("Señores\n").append(limpio(d.entidad())).append("\n\n");
            h.append("<p>Señores<br>").append(esc(limpio(d.entidad()))).append("</p>");
        }
        parrafo(t, h, "Respetados señores:");
        parrafo(t, h, intro);

        String descripcion = d.descripcion() == null ? "" : d.descripcion().trim();
        t.append(etiqueta).append(": ").append(descripcion).append('\n');
        h.append("<p><strong>").append(etiqueta).append(":</strong> ").append(esc(descripcion).replace("\n", "<br>")).append("<br>");
        if (ubicacion != null || hayPunto) {
            t.append("Ubicación: ").append(unir(" · ", ubicacion, mapa, coordenadas)).append('\n');
            h.append("<strong>Ubicación:</strong> ").append(ubicacion == null ? "" : esc(ubicacion));
            if (hayPunto) {
                h.append(ubicacion == null ? "" : " · ").append("<a href=\"").append(esc(mapa))
                        .append("\" style=\"color:#0a2f6b\">Ver en el mapa</a> · ").append(esc(coordenadas));
            }
            h.append("<br>");
        }
        t.append("Fecha del registro: ").append(fecha).append('\n');
        h.append("<strong>Fecha del registro:</strong> ").append(esc(fecha));
        if (evidencia != null) {
            t.append("Evidencia: ").append(evidencia).append(".\n");
            h.append("<br><strong>Evidencia:</strong> ").append(evidencia).append('.');
        }
        h.append("</p>");

        int n = 0;
        StringBuilder miniaturas = new StringBuilder();
        StringBuilder enlaces = new StringBuilder();
        for (String foto : d.fotos()) {
            n++;
            t.append("Fotografía ").append(n).append(": ").append(foto).append('\n');
            miniaturas.append("<a href=\"").append(esc(foto)).append("\"><img src=\"").append(esc(miniatura(foto)))
                    .append("\" alt=\"Fotografía ").append(n).append(" del reporte\" width=\"160\" style=\"display:inline-block;")
                    .append("width:160px;max-width:100%;height:auto;border:0;margin:0 8px 8px 0\"></a>");
            enlaces.append(n > 1 ? " · " : "").append("<a href=\"").append(esc(foto)).append("\" style=\"color:#0a2f6b\">Fotografía ")
                    .append(n).append("</a>");
        }
        if (n > 0) h.append("<p>").append(miniaturas).append("<br>").append(enlaces).append("</p>");
        t.append('\n');

        parrafo(t, h, solicitud);
        parrafo(t, h, autorizo);
        String firma = unir(" · ", limpio(d.nombre()), documento);
        t.append("Atentamente,\n").append(firma);
        h.append("<p>Atentamente,<br>").append(esc(firma)).append("</p></div></body></html>");

        List<Seccion> secciones = new ArrayList<>();
        secciones.add(new Seccion("categoria", "Categoría", limpio(d.categoria()), "categoria"));
        secciones.add(new Seccion("descripcion", etiqueta, descripcion, "detalles"));
        if (ubicacion != null || coordenadas != null)
            secciones.add(new Seccion("ubicacion", "Ubicación", unir("\n", ubicacion, coordenadas), "ubicacion"));
        secciones.add(new Seccion("fecha", "Fecha del registro", fecha, null));
        if (evidencia != null) secciones.add(new Seccion("evidencia", "Evidencia", evidencia, "detalles"));
        if (matricula != null) secciones.add(new Seccion("matricula", "Matrícula o cuenta", matricula, "detalles"));
        secciones.add(new Seccion("remitente", "Sus datos",
                unir("\n", limpio(d.nombre()), documento, d.correo(), d.telefono(), d.direccionCorrespondencia()), null));

        return new Mensaje(asunto, t.toString(), h.toString(), List.copyOf(secciones));
    }

    private static String asunto(Datos d, GrupoPlantilla grupo, String comuna) {
        String categoria = limpio(d.categoria());
        String asunto = switch (grupo) {
            case A -> "Petición ciudadana – " + categoria
                    + Objects.toString(prefijo(" – ", unir(", ", d.barrio(), comuna)), "");
            case B -> "Solicitud ciudadana – " + categoria;
            case C -> "Petición ciudadana – " + categoria;
        };
        return asunto.replaceAll("[\\r\\n\\t]+", " ").trim();
    }

    static String fecha(OffsetDateTime instante) {
        ZonedDateTime z = instante.atZoneSameInstant(COLOMBIA);
        int hora = z.getHour() % 12 == 0 ? 12 : z.getHour() % 12;
        return String.format(Locale.ROOT, "%d de %s de %d, %d:%02d %s", z.getDayOfMonth(), MESES[z.getMonthValue() - 1],
                z.getYear(), hora, z.getMinute(), z.getHour() < 12 ? "a. m." : "p. m.");
    }

    private static String decimal(double valor) {
        return String.format(Locale.ROOT, "%.5f", valor);
    }

    /** Miniatura de Cloudinary con ancho fijo; otra URL se usa tal cual. */
    private static String miniatura(String url) {
        return url.replaceFirst("/image/upload/", "/image/upload/c_limit,w_320/");
    }

    private static void parrafo(StringBuilder texto, StringBuilder html, String contenido) {
        texto.append(contenido).append("\n\n");
        html.append("<p>").append(esc(contenido)).append("</p>");
    }

    private static String unir(String separador, String... partes) {
        String unido = Stream.of(partes).filter(p -> !vacio(p)).map(String::trim).collect(Collectors.joining(separador));
        return unido.isEmpty() ? null : unido;
    }

    private static String prefijo(String prefijo, String valor) {
        return valor == null ? null : prefijo + valor;
    }

    private static String minuscula(String valor, String siFalta) {
        if (vacio(valor)) return siFalta;
        String v = valor.trim();
        return v.substring(0, 1).toLowerCase(ES) + v.substring(1);
    }

    private static String limpio(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    public static boolean enlaceSeguro(String url) {
        try {
            URI u = URI.create(url);
            return "https".equalsIgnoreCase(u.getScheme()) && u.getUserInfo() == null && u.getFragment() == null
                    && (u.getPort() == -1 || u.getPort() == 443)
                    && ("res.cloudinary.com".equalsIgnoreCase(u.getHost()) || "www.openstreetmap.org".equalsIgnoreCase(u.getHost()));
        } catch (RuntimeException e) {
            return false;
        }
    }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
