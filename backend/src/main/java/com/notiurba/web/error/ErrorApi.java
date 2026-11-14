package com.notiurba.web.error;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Cuerpo único de todo error de la API: codigo estable, mensaje para el ciudadano, campo (o null) y,
 * solo cuando hace falta, datos extra (segundos de espera, tope diario, etc.). Sin datos no sale en el JSON.
 */
public record ErrorApi(String codigo, String mensaje, String campo, @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, Object> datos) {

    public ErrorApi(String codigo, String mensaje, String campo) {
        this(codigo, mensaje, campo, null);
    }
}
