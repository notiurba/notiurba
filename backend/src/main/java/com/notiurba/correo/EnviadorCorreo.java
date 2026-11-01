package com.notiurba.correo;

/**
 * Envía un correo. Hoy lo implementa Resend por su API HTTP ({@link EnviadorCorreoResend});
 * el resto del sistema solo conoce esta interfaz, así que cambiar de proveedor no lo toca (ADR-0002).
 */
public interface EnviadorCorreo {

    /**
     * @return identificador que el proveedor asignó al envío
     * @throws CorreoNoEnviadoException si el proveedor no lo aceptó; {@code reintentable()} indica si vale la pena repetirlo
     */
    String enviar(CorreoSaliente correo);
}
