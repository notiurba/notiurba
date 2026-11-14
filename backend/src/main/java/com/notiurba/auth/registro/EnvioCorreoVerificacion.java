package com.notiurba.auth.registro;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.notiurba.config.SegundoPlanoConfig;
import com.notiurba.correo.CorreoSaliente;
import com.notiurba.correo.EnviadorCorreo;

/**
 * Envía el enlace de verificación cuando la cuenta ya quedó guardada y en un hilo aparte, para que registrarse
 * no espere a Resend. Si el envío falla solo se registra: la persona puede pedir otro con reenviar-verificacion.
 */
@Component
public class EnvioCorreoVerificacion {

    private static final Logger log = LoggerFactory.getLogger(EnvioCorreoVerificacion.class);

    private final EnviadorCorreo enviadorCorreo;

    public EnvioCorreoVerificacion(EnviadorCorreo enviadorCorreo) {
        this.enviadorCorreo = enviadorCorreo;
    }

    @Async(SegundoPlanoConfig.EJECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void enviar(CorreoVerificacionPendiente pendiente) {
        try {
            enviadorCorreo.enviar(new CorreoSaliente(
                    List.of(pendiente.correo()),
                    "Confirma tu correo en NotiUrba",
                    "Hola " + pendiente.nombres() + ",\n\n"
                            + "Para activar tu cuenta de NotiUrba confirma tu correo con este enlace:\n"
                            + pendiente.enlace() + "\n\n"
                            + "El enlace vence en 24 horas. Si no creaste una cuenta, ignora este mensaje.",
                    null, null, null, null));
        } catch (RuntimeException fallo) {
            log.error("No se pudo enviar el enlace de verificación de correo", fallo);
        }
    }
}
