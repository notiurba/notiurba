package com.notiurba.correo;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

class FirmaWebhookTest {
    @Test void validaBytesOriginalesYRechazaAlteracionFirmaVaciaYReplayAntiguo() throws Exception {
        String id="evt_1",ts="1793980800",body="{\"type\":\"email.bounced\"}", key="secreto-sintetico-de-prueba";
        String secret="whsec_"+Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));
        Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        String signature="v1,"+Base64.getEncoder().encodeToString(mac.doFinal((id+"."+ts+"."+body).getBytes(StandardCharsets.UTF_8)));
        Clock now=Clock.fixed(Instant.ofEpochSecond(Long.parseLong(ts)),ZoneOffset.UTC);
        assertThat(FirmaWebhook.valida(secret,id,ts,signature,body,now)).isTrue();
        assertThat(FirmaWebhook.valida(secret,id,ts,signature,body+" ",now)).isFalse();
        assertThat(FirmaWebhook.valida(secret,id,ts,"",body,now)).isFalse();
        assertThat(FirmaWebhook.valida(secret,id,ts,signature,body,Clock.offset(now,Duration.ofMinutes(6)))).isFalse();
    }
}
