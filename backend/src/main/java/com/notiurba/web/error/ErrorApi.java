package com.notiurba.web.error;

/** Cuerpo único de todo error de la API: codigo estable, mensaje para el ciudadano y campo (o null). */
public record ErrorApi(String codigo, String mensaje, String campo) {
}
