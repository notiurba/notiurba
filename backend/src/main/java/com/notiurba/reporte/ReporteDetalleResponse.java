package com.notiurba.reporte;

import java.time.OffsetDateTime;
import java.util.List;

/** Un reporte propio completo (GET /api/reportes/{codigo}). Ver docs/contexto/11-contrato-api.md, 3.6. */
public record ReporteDetalleResponse(
        String codigoSeguimiento,
        String estado,
        Categoria categoria,
        String descripcion,
        Double latitud,
        Double longitud,
        Integer comuna,
        Barrio barrio,
        String direccionIncidente,
        String matriculaCuentaServicio,
        String radicadoOficial,
        List<Foto> fotos,
        OffsetDateTime creadoEn,
        OffsetDateTime vigenteHasta,
        List<CambioDeEstado> historial,
        Envio envio) {

    public record Categoria(Integer idCategoria, String nombre) {
    }

    public record Barrio(Integer idBarrio, String nombre) {
    }

    public record Foto(Integer orden, String url) {
    }

    public record CambioDeEstado(String estado, String observacion, OffsetDateTime cambiadoEn) {
    }

    /** Estado del correo a la entidad: PENDIENTE, ENVIADO o FALLIDO; motivo solo si falló (RF-16). */
    public record Envio(String estado, String motivo) {
    }
}
