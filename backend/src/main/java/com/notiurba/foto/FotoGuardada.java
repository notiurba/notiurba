package com.notiurba.foto;

/**
 * Una foto ya guardada en el almacén.
 *
 * @param url       enlace público https (es lo que va en el correo y en {@code evidencia_reporte.url})
 * @param idPublico identificador de la foto en el almacén, necesario para borrarla
 */
public record FotoGuardada(String url, String idPublico) {
}
