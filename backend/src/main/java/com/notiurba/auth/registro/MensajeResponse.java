package com.notiurba.auth.registro;

/** Respuesta del registro: dónde se envió el enlace de confirmación. */
public record MensajeResponse(String correo, String mensaje) {
}
