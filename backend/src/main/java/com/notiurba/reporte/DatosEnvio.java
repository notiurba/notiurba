package com.notiurba.reporte;

import java.time.OffsetDateTime;
import java.util.List;

/** Todo lo que lleva el correo del reporte a la entidad (NOT-72): reporte, ciudadano y destino. */
public record DatosEnvio(
        long idRegistroEnvio,
        String codigoSeguimiento,
        String categoria,
        String entidad,
        String destino,
        String descripcion,
        Double latitud,
        Double longitud,
        Integer comuna,
        String barrio,
        String direccionIncidente,
        String matriculaCuentaServicio,
        String radicadoOficial,
        OffsetDateTime creadoEn,
        OffsetDateTime autorizacionEnvioEn,
        String nombres,
        String apellidos,
        String tipoDocumento,
        String numeroDocumento,
        String direccionCorrespondencia,
        String correoCiudadano,
        List<String> fotos) {
}
