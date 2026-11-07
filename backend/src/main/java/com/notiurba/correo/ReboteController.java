package com.notiurba.correo;

import java.time.Clock;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import com.notiurba.web.error.ApiException;

@RestController
public class ReboteController {
    private final String secreto;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public ReboteController(@Value("${notiurba.correo.webhook-secret:}") String secreto,JdbcTemplate jdbc,ObjectMapper json) {
        this.secreto=secreto;this.jdbc=jdbc;this.json=json;
    }
    @PostMapping("/api/webhooks/resend")
    @Transactional
    public void recibir(@RequestHeader(value="svix-id",required=false) String id,
        @RequestHeader(value="svix-timestamp",required=false) String fecha,
        @RequestHeader(value="svix-signature",required=false) String firma,@RequestBody String cuerpo) throws java.io.IOException {
        if(!FirmaWebhook.valida(secreto,id,fecha,firma,cuerpo,Clock.systemUTC()))
            throw new ApiException(HttpStatus.UNAUTHORIZED,"FIRMA_INVALIDA","Firma de correo no válida");
        var evento=json.readTree(cuerpo);
        if(!"email.bounced".equals(evento.path("type").asText()) && !"email.complained".equals(evento.path("type").asText())) return;
        String proveedor=evento.path("data").path("email_id").asText();
        if(proveedor.isBlank() || proveedor.length()>100) throw new ApiException(HttpStatus.BAD_REQUEST,"VALIDACION","Evento de correo inválido");
        int nuevo=jdbc.update("INSERT INTO evento_correo(id_evento,id_envio_proveedor) VALUES (?,?) ON CONFLICT DO NOTHING",id,proveedor);
        if(nuevo==0) return;
        jdbc.update("""
            UPDATE registro_envio_correo SET id_estado_envio=(SELECT id_estado_envio FROM estado_envio WHERE nombre='FALLIDO'),
             error='El proveedor notificó un fallo posterior',rebote=TRUE,reintentable=FALSE
            WHERE id_envio_proveedor=?
            """,proveedor);
        // El historial del reporte NO se modifica. El ciudadano consulta el fallo mediante la API existente.
    }
}
