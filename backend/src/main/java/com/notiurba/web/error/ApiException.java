package com.notiurba.web.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Error de negocio con código estable. Extiende ResponseStatusException para que getStatusCode()
 * y getReason() (el mensaje) sigan funcionando donde ya se usaban.
 */
public class ApiException extends ResponseStatusException {

    private final String codigo;
    private final String campo;

    public ApiException(HttpStatus estado, String codigo, String mensaje, String campo) {
        super(estado, mensaje);
        this.codigo = codigo;
        this.campo = campo;
    }

    public ApiException(HttpStatus estado, String codigo, String mensaje) {
        this(estado, codigo, mensaje, null);
    }

    public String getCodigo() {
        return codigo;
    }

    public String getCampo() {
        return campo;
    }
}
