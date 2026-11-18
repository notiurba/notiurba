package com.notiurba.auth.recuperacion;

/**
 * Envía el enlace de «Recuperar contraseña». La implementación usa {@link com.notiurba.correo.EnviadorCorreo}
 * (Resend); si el proveedor no acepta el correo, lanza la excepción.
 */
public interface EnviadorEnlaceRecuperacion {

    void enviar(String correo, String enlace);
}
