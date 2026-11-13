package com.notiurba.auth.registro;

/** Hay un enlace de verificación por enviar. Vive solo en memoria: el enlace trae el token sin cifrar. */
public record CorreoVerificacionPendiente(String correo, String nombres, String enlace) {

    @Override
    public String toString() {
        return "CorreoVerificacionPendiente[enlace oculto]";
    }
}
