package com.notiurba.auth.recuperacion;

/**
 * Envía el enlace de «Recuperar contraseña». Hoy solo existe la implementación de log:
 * el envío real (JavaMailSender + Resend) llega cuando el sistema tenga servidor de correo.
 */
public interface EnviadorEnlaceRecuperacion {

    void enviar(String correo, String enlace);
}
