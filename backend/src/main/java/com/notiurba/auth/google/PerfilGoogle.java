package com.notiurba.auth.google;

/** Datos verificados del ID token de Google que usa NotiUrba. */
public record PerfilGoogle(String sub, String correo, String givenName, String familyName, String nombreCompleto) {
}
