package com.notiurba.foto;

/**
 * Guarda las fotos de los reportes fuera de la base y del disco del servidor (en Render Free es
 * efímero). Hoy lo implementa Cloudinary ({@link AlmacenFotosCloudinary}, ADR-0006); el resto del
 * sistema solo conoce esta interfaz, así que pasar a Cloudflare R2 no lo toca.
 *
 * No valida el archivo: tipo real y tamaño se revisan antes, en el endpoint de subida.
 */
public interface AlmacenFotos {

    /**
     * @throws AlmacenFotosNoDisponibleException si el almacén no la guardó (sin configurar, sin red, rechazo)
     */
    FotoGuardada guardar(byte[] contenido, String tipoMime);

    /** Borra la foto (por ejemplo si el ciudadano pide suprimir sus datos). No falla si ya no existe. */
    void borrar(String idPublico);
}
