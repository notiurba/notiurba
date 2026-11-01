package com.notiurba.correo;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Envía con la API HTTP de Resend (POST /emails). Sale por HTTPS, así que no le afecta el
 * bloqueo de los puertos SMTP de Render Free. Cada destinatario cuenta aparte en la cuota
 * (100 al día en el plan gratuito).
 */
public class EnviadorCorreoResend implements EnviadorCorreo {

    private static final String NOMBRE_POR_DEFECTO = "NotiUrba";

    private final RestClient cliente;
    private final String apiKey;
    private final String remitente;

    /** @param remitente solo la dirección, del dominio verificado (p. ej. reportes@reportes.notiurba.com) */
    public EnviadorCorreoResend(RestClient cliente, String apiKey, String remitente) {
        this.cliente = cliente;
        this.apiKey = apiKey;
        this.remitente = remitente;
    }

    @Override
    public String enviar(CorreoSaliente correo) {
        if (apiKey == null || apiKey.isBlank() || remitente == null || remitente.isBlank()) {
            throw new CorreoNoEnviadoException(
                    "El correo no está configurado: falta RESEND_API_KEY o RESEND_REMITENTE", false);
        }

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("from", de(correo.nombreRemitente()));
        cuerpo.put("to", correo.para());
        cuerpo.put("subject", correo.asunto());
        cuerpo.put("text", correo.texto());
        if (correo.html() != null && !correo.html().isBlank()) {
            cuerpo.put("html", correo.html());
        }
        if (correo.responderA() != null && !correo.responderA().isBlank()) {
            cuerpo.put("reply_to", correo.responderA());
        }

        try {
            var peticion = cliente.post().uri("/emails")
                    .header("Authorization", "Bearer " + apiKey);
            if (correo.claveIdempotencia() != null && !correo.claveIdempotencia().isBlank()) {
                peticion = peticion.header("Idempotency-Key", correo.claveIdempotencia());
            }
            Map<?, ?> respuesta = peticion.body(cuerpo).retrieve().body(Map.class);
            Object id = respuesta == null ? null : respuesta.get("id");
            if (id == null) {
                throw new CorreoNoEnviadoException("Resend respondió sin identificador de envío", true);
            }
            return id.toString();
        } catch (RestClientResponseException e) {
            int estado = e.getStatusCode().value();
            // 429 (límite) y 5xx pueden pasar solos; el resto (datos, clave, dominio) no
            boolean reintentable = estado == 429 || estado >= 500;
            throw new CorreoNoEnviadoException("Resend rechazó el envío (HTTP " + estado + "): "
                    + e.getResponseBodyAsString(), reintentable, e);
        } catch (ResourceAccessException e) {
            throw new CorreoNoEnviadoException("No se pudo contactar a Resend", true, e);
        }
    }

    /** «Nombre &lt;dirección&gt;»; el nombre se limpia para que no pueda romper el encabezado. */
    private String de(String nombre) {
        String limpio = nombre == null ? "" : nombre.replaceAll("[\\r\\n\"<>]", " ").trim().replaceAll("\\s+", " ");
        return (limpio.isEmpty() ? NOMBRE_POR_DEFECTO : limpio) + " <" + remitente + ">";
    }
}
