package com.notiurba.catalogo;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catálogos públicos (NOT-55): llenan los selectores de «Crear cuenta» y «Editar mis datos»,
 * pantallas a las que se llega antes de tener sesión, por eso no exigen token.
 */
@RestController
public class CatalogoController {

    public record TipoDocumentoResponse(short idTipoDocumento, String nombre, String codigo) {
    }

    public record BarrioResponse(int idBarrio, short idComuna, String nombre) {
    }

    private final JdbcTemplate jdbc;

    public CatalogoController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/catalogos/tipos-documento")
    public List<TipoDocumentoResponse> tiposDocumento() {
        return jdbc.query(
                "SELECT id_tipo_documento, nombre, codigo FROM tipo_documento ORDER BY id_tipo_documento",
                (rs, n) -> new TipoDocumentoResponse(rs.getShort("id_tipo_documento"),
                        rs.getString("nombre"), rs.getString("codigo")));
    }

    /**
     * Sin los barrios internos (p. ej. el del administrador): no son para elegir. Se ordenan aquí con las reglas
     * del español (Céspedes antes que Chiminangos) y no con la configuración de idioma de la base, que cambia
     * entre bases y haría que el selector salga distinto en cada una.
     */
    @GetMapping("/api/catalogos/barrios")
    public List<BarrioResponse> barrios() {
        List<BarrioResponse> barrios = new ArrayList<>(jdbc.query(
                "SELECT id_barrio, id_comuna, nombre FROM barrio WHERE NOT interno",
                (rs, n) -> new BarrioResponse(rs.getInt("id_barrio"), rs.getShort("id_comuna"),
                        rs.getString("nombre"))));
        Collator español = Collator.getInstance(new Locale("es", "CO"));
        español.setStrength(Collator.TERTIARY);
        barrios.sort(Comparator.comparing(BarrioResponse::nombre, español).thenComparingInt(BarrioResponse::idBarrio));
        return barrios;
    }
}
