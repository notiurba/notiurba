package com.notiurba.correo;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.notiurba.reporte.ReporteServicio;

/**
 * El mensaje tal como llegaría a la entidad, armado desde el borrador con las mismas reglas y la misma
 * plantilla que el correo real. No escribe en la base.
 */
@Service
public class VistaPreviaServicio {

    public record VistaPrevia(String asunto, String texto, List<PlantillaReporte.Seccion> secciones,
            boolean permiteCorreo, EnrutamientoServicio.Ruta ruta) {
    }

    private final ReporteServicio reportes;
    private final EnrutamientoServicio rutas;
    private final PlantillaReporte plantilla;
    private final JdbcTemplate jdbc;

    public VistaPreviaServicio(ReporteServicio reportes, EnrutamientoServicio rutas, PlantillaReporte plantilla,
            JdbcTemplate jdbc) {
        this.reportes = reportes;
        this.rutas = rutas;
        this.plantilla = plantilla;
        this.jdbc = jdbc;
    }

    public VistaPrevia armar(String correoUsuario, VistaPreviaRequest p) {
        var b = reportes.validar(correoUsuario, p.idCategoria(), p.latitud(), p.longitud(), p.idBarrio(),
                p.descripcion(), p.direccionIncidente(), p.matriculaCuentaServicio());
        var ruta = rutas.resolver(b.idCategoria(), Boolean.TRUE.equals(p.emergencia()));
        var u = jdbc.queryForMap("""
                SELECT u.nombres, u.apellidos, u.numero_documento, u.direccion_correspondencia, u.correo, u.telefono,
                       t.codigo AS codigo_documento, t.nombre AS tipo_documento
                  FROM usuario u JOIN tipo_documento t USING (id_tipo_documento) WHERE u.id_usuario = ?""",
                b.idUsuario());
        // Misma regla que el trigger del reporte: la comuna del punto y, si no hay, la del barrio.
        String barrio = null;
        Integer comuna = b.comunaDelPunto();
        if (b.idBarrio() != null) {
            var fila = jdbc.queryForMap("SELECT nombre, id_comuna FROM barrio WHERE id_barrio = ?", b.idBarrio());
            barrio = (String) fila.get("nombre");
            if (comuna == null && fila.get("id_comuna") != null) comuna = ((Number) fila.get("id_comuna")).intValue();
        }
        OffsetDateTime ahora = OffsetDateTime.now();
        var datos = PlantillaReporte.Datos.builder().entidad(ruta.entidad()).categoria(ruta.categoria())
                .nombre(u.get("nombres") + " " + u.get("apellidos"))
                .tipoDocumento((String) u.get("tipo_documento")).codigoDocumento((String) u.get("codigo_documento"))
                .numeroDocumento((String) u.get("numero_documento"))
                .direccionCorrespondencia((String) u.get("direccion_correspondencia"))
                .correo((String) u.get("correo")).telefono((String) u.get("telefono"))
                .descripcion(b.descripcion()).direccionIncidente(b.direccionIncidente()).barrio(barrio).comuna(comuna)
                .latitud(b.latitud()).longitud(b.longitud()).creadoEn(ahora).autorizacion(ahora)
                .matricula(b.matricula()).totalFotos(p.totalFotos() == null ? 0 : p.totalFotos()).build();
        var m = plantilla.componer(datos, GrupoPlantilla.de(b.categoria().esEmpresa(), b.categoria().requiereUbicacion()));
        return new VistaPrevia(m.asunto(), m.texto(), m.secciones(), ruta.permiteCorreo(), ruta);
    }
}
