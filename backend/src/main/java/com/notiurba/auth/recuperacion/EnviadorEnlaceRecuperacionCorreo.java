package com.notiurba.auth.recuperacion;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.notiurba.correo.CorreoSaliente;
import com.notiurba.correo.EnviadorCorreo;

/**
 * Envía el enlace por Resend. Si el envío falla lanza la excepción: {@link EnvioRecuperacionPendiente} la
 * registra y borra el token pendiente; la respuesta pública no cambia.
 */
@Component
public class EnviadorEnlaceRecuperacionCorreo implements EnviadorEnlaceRecuperacion {

    private static final Logger log = LoggerFactory.getLogger(EnviadorEnlaceRecuperacionCorreo.class);

    private final EnviadorCorreo enviadorCorreo;

    public EnviadorEnlaceRecuperacionCorreo(EnviadorCorreo enviadorCorreo) {
        this.enviadorCorreo = enviadorCorreo;
    }

    @Override
    public void enviar(String correo, String enlace) {
        String idProveedor = enviadorCorreo.enviar(new CorreoSaliente(
                List.of(correo),
                "Restablece tu contraseña de NotiUrba",
                "Hola,\n\n"
                        + "Recibimos una solicitud para restablecer la contraseña de tu cuenta de NotiUrba. "
                        + "Usa este enlace para elegir una nueva:\n"
                        + enlace + "\n\n"
                        + "El enlace vence en 1 hora y solo se puede usar una vez. "
                        + "Si no lo pediste tú, ignora este mensaje: tu contraseña no cambia.",
                null, null, null, null));
        log.info("Correo de recuperación aceptado por el proveedor: id={}", idProveedor);
    }
}
