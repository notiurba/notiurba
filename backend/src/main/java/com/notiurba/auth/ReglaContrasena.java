package com.notiurba.auth;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;

import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

/**
 * Regla de contraseña de NotiUrba: mínimo 8 caracteres, con letras y números.
 * La usan «Nueva contraseña» y, cuando exista, el registro con correo y contraseña.
 */
public final class ReglaContrasena {

    static final int MINIMO = 8;
    /** BCrypt solo usa los primeros 72 bytes: más largo daría una falsa sensación de seguridad. */
    static final int MAXIMO_BYTES = 72;
    static final String MENSAJE = "La contraseña debe tener al menos 8 caracteres, con letras y números";

    private static final Pattern LETRA = Pattern.compile("\\p{L}");
    private static final Pattern NUMERO = Pattern.compile("\\p{Nd}");

    private ReglaContrasena() {
    }

    public static boolean cumple(String contrasena) {
        return contrasena != null
                && contrasena.length() >= MINIMO
                && contrasena.getBytes(StandardCharsets.UTF_8).length <= MAXIMO_BYTES
                && LETRA.matcher(contrasena).find()
                && NUMERO.matcher(contrasena).find();
    }

    /** @throws ApiException 400 VALIDACION (campo = {@code campo}) si no cumple */
    public static void validar(String contrasena, String campo) {
        if (!cumple(contrasena)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION, MENSAJE, campo);
        }
    }
}
