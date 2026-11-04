package com.notiurba.correo;

/** El proveedor de correo no aceptó el envío. El mensaje nunca incluye la clave de API. */
public class CorreoNoEnviadoException extends RuntimeException {

    private final boolean reintentable;

    public CorreoNoEnviadoException(String mensaje, boolean reintentable, Throwable causa) {
        super(mensaje, causa);
        this.reintentable = reintentable;
    }

    public CorreoNoEnviadoException(String mensaje, boolean reintentable) {
        this(mensaje, reintentable, null);
    }

    /** true si el fallo pudo ser pasajero (sin red, 429, 5xx); false si repetirlo daría lo mismo (datos o clave mal). */
    public boolean reintentable() {
        return reintentable;
    }
}
