package com.notiurba.auth.recuperacion;

import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.notiurba.correo.CorreoSaliente;
import com.notiurba.correo.EnviadorCorreo;

/**
 * Envía de verdad el enlace de «Recuperar contraseña» por correo (Resend). Antes de enviar deja la
 * constancia en el log ({@link EnviadorEnlaceRecuperacionLog}), que solo escribe el enlace si
 * notiurba.recuperacion.mostrar-enlace-en-log = true (desarrollo sin Resend).
 *
 * <p>Si el envío falla lanza la excepción: {@link RecuperacionServicio} la registra y responde igual,
 * para no revelar si la cuenta existe.
 */
@Primary
@Component
public class EnviadorEnlaceRecuperacionCorreo implements EnviadorEnlaceRecuperacion {

    private final EnviadorCorreo enviadorCorreo;
    private final EnviadorEnlaceRecuperacionLog registro;

    public EnviadorEnlaceRecuperacionCorreo(EnviadorCorreo enviadorCorreo, EnviadorEnlaceRecuperacionLog registro) {
        this.enviadorCorreo = enviadorCorreo;
        this.registro = registro;
    }

    @Override
    public void enviar(String correo, String enlace) {
        registro.enviar(correo, enlace);
        enviadorCorreo.enviar(new CorreoSaliente(
                List.of(correo),
                "Restablece tu contraseña de NotiUrba",
                "Hola,\n\n"
                        + "Recibimos una solicitud para restablecer la contraseña de tu cuenta de NotiUrba. "
                        + "Usa este enlace para elegir una nueva:\n"
                        + enlace + "\n\n"
                        + "El enlace vence en 1 hora y solo se puede usar una vez. "
                        + "Si no lo pediste tú, ignora este mensaje: tu contraseña no cambia.",
                null, null, null, null));
    }
}
