package com.notiurba.web.error;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Error de negocio con código estable. Extiende ResponseStatusException para que getStatusCode()
 * y getReason() (el mensaje) sigan funcionando donde ya se usaban.
 */
public class ApiException extends ResponseStatusException {

    private final String codigo;
    private final String campo;
    private final Map<String, Object> datos;

    public ApiException(HttpStatus estado, String codigo, String mensaje, String campo, Map<String, Object> datos) {
        super(estado, mensaje);
        this.codigo = codigo;
        this.campo = campo;
        this.datos = datos;
    }

    public ApiException(HttpStatus estado, String codigo, String mensaje, String campo) {
        this(estado, codigo, mensaje, campo, null);
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

    /** Datos extra para el cliente (o null). */
    public Map<String, Object> getDatos() {
        return datos;
    }
}
