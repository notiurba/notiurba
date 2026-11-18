package com.notiurba.correo;

/** El proveedor de correo no aceptó el envío. El mensaje nunca incluye la clave de API. */
public class CorreoNoEnviadoException extends RuntimeException {

    private final boolean reintentable;
    private final Integer estadoHttp;

    public CorreoNoEnviadoException(String mensaje, boolean reintentable, Integer estadoHttp, Throwable causa) {
        super(mensaje, causa);
        this.reintentable = reintentable;
        this.estadoHttp = estadoHttp;
    }

    public CorreoNoEnviadoException(String mensaje, boolean reintentable, Throwable causa) {
        this(mensaje, reintentable, null, causa);
    }

    public CorreoNoEnviadoException(String mensaje, boolean reintentable) {
        this(mensaje, reintentable, null, null);
    }

    /** true si el fallo pudo ser pasajero (sin red, 429, 5xx); false si repetirlo daría lo mismo (datos o clave mal). */
    public boolean reintentable() {
        return reintentable;
    }

    /** Código HTTP con el que el proveedor rechazó el envío; null si no llegó a responder o faltaba configuración. */
    public Integer estadoHttp() {
        return estadoHttp;
    }
}
