package com.notiurba.correo;

import java.util.List;

/**
 * Un correo listo para enviar.
 *
 * @param para               destinatarios (la entidad); obligatorio
 * @param asunto             obligatorio
 * @param texto              cuerpo en texto plano; obligatorio
 * @param html               cuerpo en HTML; opcional
 * @param nombreRemitente    nombre que se muestra como remitente (p. ej. el del ciudadano, ADR-0002); opcional
 * @param responderA         Reply-To (el correo del ciudadano); opcional
 * @param claveIdempotencia  si se repite la misma clave, Resend no envía dos veces; opcional
 */
public record CorreoSaliente(
        List<String> para,
        String asunto,
        String texto,
        String html,
        String nombreRemitente,
        String responderA,
        String claveIdempotencia) {

    public CorreoSaliente {
        if (para == null || para.isEmpty() || para.stream().anyMatch(p -> p == null || p.isBlank())) {
            throw new IllegalArgumentException("El correo necesita al menos un destinatario");
        }
        if (asunto == null || asunto.isBlank() || texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El correo necesita asunto y texto");
        }
        para = List.copyOf(para);
    }
}
