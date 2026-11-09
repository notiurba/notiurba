package com.notiurba.usuario;

import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;

import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

/**
 * Reglas de nombres y apellidos que comparten el registro con contraseña, el alta con Google y «Editar mis datos».
 * Son las MISMAS que aplica el frontend (frontend/src/compartido/validaciones/campos.js): lo que la pantalla
 * deja escribir nunca falla aquí, y quien llame a la API directamente no puede saltarse el límite.
 */
public final class ReglasPerfil {

    public static final int MIN_LONGITUD_NOMBRE = 2;
    public static final int MAX_LONGITUD_NOMBRE = 60;

    /** Empieza con una letra y sigue con letras (con tilde o ñ), espacios, apóstrofo, punto o guion. */
    private static final Pattern FORMATO_NOMBRE = Pattern.compile("^\\p{L}[\\p{L} '’.-]*$");

    private ReglasPerfil() {
    }

    /**
     * @param etiqueta nombre en plural para el mensaje («Los nombres»)
     * @param campo    campo de la petición que se marca en el error
     * @return el valor sin espacios sobrantes (los repetidos del medio quedan en uno solo)
     */
    public static String limpiarNombre(String valor, String etiqueta, String campo) {
        String limpio = valor == null ? "" : valor.trim().replaceAll("\\s+", " ");
        if (limpio.isEmpty()) {
            throw error(etiqueta + " son obligatorios", campo);
        }
        if (limpio.length() < MIN_LONGITUD_NOMBRE) {
            throw error(etiqueta + " deben tener al menos " + MIN_LONGITUD_NOMBRE + " letras", campo);
        }
        if (limpio.length() > MAX_LONGITUD_NOMBRE) {
            throw error(etiqueta + " no pueden pasar de " + MAX_LONGITUD_NOMBRE + " caracteres", campo);
        }
        if (!FORMATO_NOMBRE.matcher(limpio).matches()) {
            throw error(etiqueta + " solo pueden llevar letras, espacios, apóstrofo, punto o guion", campo);
        }
        return limpio;
    }

    private static ApiException error(String mensaje, String campo) {
        return new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION, mensaje, campo);
    }
}
