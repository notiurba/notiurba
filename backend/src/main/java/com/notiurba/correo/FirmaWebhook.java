package com.notiurba.correo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Protocolo Svix: firma sobre los bytes originales y tolerancia temporal de cinco minutos. */
public final class FirmaWebhook {
    private FirmaWebhook() { }
    public static boolean valida(String secreto,String id,String fecha,String firma,String cuerpo,Clock reloj) {
        try {
            if(secreto==null || !secreto.startsWith("whsec_") || id==null || id.length()>200 || firma==null) return false;
            long timestamp=Long.parseLong(fecha);
            if(timestamp < reloj.instant().getEpochSecond()-300 || timestamp > reloj.instant().getEpochSecond()+300) return false;
            Mac mac=Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Base64.getDecoder().decode(secreto.substring(6)),"HmacSHA256"));
            byte[] esperado=mac.doFinal((id+"."+fecha+"."+cuerpo).getBytes(StandardCharsets.UTF_8));
            for(String candidata:firma.split(" ")) {
                String[] partes=candidata.split(",",2);
                if(partes.length==2 && partes[0].equals("v1") && MessageDigest.isEqual(esperado,Base64.getDecoder().decode(partes[1]))) return true;
            }
        } catch(Exception ignorada) { return false; }
        return false;
    }
}
