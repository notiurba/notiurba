package com.notiurba.auth.recuperacion;

import java.time.OffsetDateTime;
import java.util.function.BooleanSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.notiurba.config.SegundoPlanoConfig;
import com.notiurba.correo.CorreoNoEnviadoException;

/**
 * Hace en segundo plano todo lo que depende de la cuenta: elegibilidad, límite, token pendiente, envío y
 * confirmación o limpieza. Así la respuesta pública es igual y tarda lo mismo para cualquier correo.
 */
@Component
class EnvioRecuperacionPendiente {

    private static final Logger log = LoggerFactory.getLogger(EnvioRecuperacionPendiente.class);
    private static final int INTENTOS_PERSISTENCIA = 3;

    private final EnviadorEnlaceRecuperacion enviador;
    private final PersistenciaRecuperacion persistencia;

    EnvioRecuperacionPendiente(EnviadorEnlaceRecuperacion enviador, PersistenciaRecuperacion persistencia) {
        this.enviador = enviador;
        this.persistencia = persistencia;
    }

    @Async(SegundoPlanoConfig.EJECUTOR_RECUPERACION)
    public void procesar(String correo, String tokenHash, String enlace) {
        PersistenciaRecuperacion.Preparacion preparacion;
        try {
            preparacion = persistencia.preparar(correo, tokenHash, OffsetDateTime.now(),
                    RecuperacionServicio.VIGENCIA, RecuperacionServicio.MAXIMO_POR_HORA);
        } catch (RuntimeException fallo) {
            log.warn("No se pudo preparar la recuperación ({})",
                    NestedExceptionUtils.getMostSpecificCause(fallo).getClass().getSimpleName());
            return;
        }
        switch (preparacion.estado()) {
            case NO_ELEGIBLE -> log.info("Solicitud de recuperación omitida: cuenta no elegible");
            case LIMITE -> log.info("Solicitud de recuperación omitida: límite horario alcanzado");
            case LISTA -> enviarYConfirmar(preparacion, enlace);
        }
    }

    private void enviarYConfirmar(PersistenciaRecuperacion.Preparacion preparacion, String enlace) {
        try {
            enviador.enviar(preparacion.usuario().getCorreo(), enlace);
        } catch (RuntimeException falloProveedor) {
            log.warn("Envío de recuperación fallido ({})", describir(falloProveedor));
            ejecutarConReintento("limpiar la recuperación fallida", () -> persistencia.descartar(preparacion));
            return;
        }
        if (ejecutarConReintento("confirmar el correo aceptado",
                () -> persistencia.confirmar(preparacion, OffsetDateTime.now()))) {
            log.info("Enlace de recuperación activo tras la aceptación del proveedor");
        } else {
            // El token quedó pendiente o ya se reconcilió: el enlace enviado no sirve y el anterior sigue vigente.
            log.warn("El correo de recuperación salió, pero su enlace no quedó activo");
        }
    }

    /** Solo datos seguros: el mensaje del proveedor puede traer direcciones de correo. */
    private static String describir(RuntimeException fallo) {
        String tipo = fallo.getClass().getSimpleName();
        if (fallo instanceof CorreoNoEnviadoException correo) {
            Integer estado = correo.estadoHttp();
            return tipo + ", reintentable=" + correo.reintentable()
                    + ", http=" + (estado == null ? "sin respuesta" : estado);
        }
        return tipo;
    }

    private boolean ejecutarConReintento(String operacion, BooleanSupplier operacionSegura) {
        RuntimeException ultimo = null;
        for (int intento = 1; intento <= INTENTOS_PERSISTENCIA; intento++) {
            try {
                return operacionSegura.getAsBoolean();
            } catch (RuntimeException fallo) {
                ultimo = fallo;
            }
        }
        log.warn("No se pudo {} tras {} intentos ({})", operacion, INTENTOS_PERSISTENCIA,
                ultimo.getClass().getSimpleName());
        return false;
    }
}
