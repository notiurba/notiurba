package com.notiurba.foto;

/** El almacén de fotos no guardó o no borró la foto. El mensaje nunca incluye claves. */
public class AlmacenFotosNoDisponibleException extends RuntimeException {

    public AlmacenFotosNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public AlmacenFotosNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
