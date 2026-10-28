package com.notiurba.auth.recuperacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Implementación provisional mientras no hay servidor de correo: deja constancia en el log.
 * El enlace completo (que da acceso a la cuenta) solo se escribe si
 * notiurba.recuperacion.mostrar-enlace-en-log = true, pensado para desarrollo.
 */
@Component
public class EnviadorEnlaceRecuperacionLog implements EnviadorEnlaceRecuperacion {

    private static final Logger log = LoggerFactory.getLogger(EnviadorEnlaceRecuperacionLog.class);

    private final boolean mostrarEnlace;

    public EnviadorEnlaceRecuperacionLog(
            @Value("${notiurba.recuperacion.mostrar-enlace-en-log:false}") boolean mostrarEnlace) {
        this.mostrarEnlace = mostrarEnlace;
    }

    @Override
    public void enviar(String correo, String enlace) {
        if (mostrarEnlace) {
            log.warn("[SOLO DESARROLLO] Enlace de recuperación para {}: {}", correo, enlace);
        } else {
            log.info("Se generó un enlace de recuperación de contraseña (sin servidor de correo, no se envió)");
        }
    }
}
