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
        var b = reportes.validar(correoUsuario, p);
        var ruta = rutas.resolver(b.idCategoria(), b.emergencia());
        // Misma regla que el trigger del reporte: la comuna del punto y, si no hay, la del barrio.
        String barrio = null;
        Integer comuna = b.comunaDelPunto();
        if (b.idBarrio() != null) {
            var fila = jdbc.queryForMap("SELECT nombre, id_comuna FROM barrio WHERE id_barrio = ?", b.idBarrio());
            barrio = (String) fila.get("nombre");
            if (comuna == null && fila.get("id_comuna") != null) comuna = ((Number) fila.get("id_comuna")).intValue();
        }
        OffsetDateTime ahora = OffsetDateTime.now();
        var datos = DatosDelCiudadano.cargar(jdbc, b.idUsuario()).entidad(ruta.entidad()).categoria(ruta.categoria())
                .descripcion(b.descripcion()).direccionIncidente(b.direccionIncidente()).barrio(barrio).comuna(comuna)
                .latitud(b.latitud()).longitud(b.longitud()).creadoEn(ahora).autorizacion(ahora)
                .matricula(b.matricula()).totalFotos(p.totalFotos() == null ? 0 : p.totalFotos()).build();
        var m = plantilla.componer(datos, GrupoPlantilla.de(ruta));
        return new VistaPrevia(m.asunto(), m.texto(), m.secciones(), ruta.permiteCorreo(), ruta);
    }
}
