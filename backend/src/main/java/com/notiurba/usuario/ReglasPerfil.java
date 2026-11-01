package com.notiurba.usuario;

import org.springframework.http.HttpStatus;

import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

/** Reglas de los datos del perfil que comparten el registro con contraseña y el alta con Google. */
public final class ReglasPerfil {

    public static final int MAX_LONGITUD_NOMBRE = 255;

    private ReglasPerfil() {
    }

    /**
     * @param etiqueta nombre en plural para el mensaje («Los nombres»)
     * @param campo    campo de la petición que se marca en el error
     */
    public static String limpiarNombre(String valor, String etiqueta, String campo) {
        String limpio = valor == null ? "" : valor.trim();
        if (limpio.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION, etiqueta + " son obligatorios", campo);
        }
        if (limpio.length() > MAX_LONGITUD_NOMBRE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION,
                    etiqueta + " no pueden pasar de " + MAX_LONGITUD_NOMBRE + " caracteres", campo);
        }
        return limpio;
    }
}
