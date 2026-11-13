package com.notiurba.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

/**
 * Límites contra el abuso (intentos de login, reportes por día, reportes parecidos). Viven en la base y se
 * leen en cada uso, así un cambio del administrador aplica de inmediato. Es la única fuente de estos valores.
 */
@Service
public class ParametrosServicio {

    static final String INTENTOS = "login_intentos_maximos";
    static final String VENTANA_LOGIN = "login_ventana_minutos";
    static final String BLOQUEO = "login_bloqueo_minutos";
    static final String REPORTES_POR_DIA = "reportes_maximos_por_dia";
    static final String VENTANA_DUPLICADO = "duplicado_ventana_dias";

    static final int[] RANGO_INTENTOS = {1, 10};
    static final int[] RANGO_MINUTOS = {1, 60};
    static final int[] RANGO_REPORTES = {1, 50};
    static final int[] RANGO_DIAS = {1, 30};
    static final int[] RANGO_METROS = {5, 500};

    public record Login(Integer intentosMaximos, Integer ventanaMinutos, Integer bloqueoMinutos) {
    }

    public record Distancia(Integer idCategoria, String nombre, Integer metros) {
    }

    public record Duplicados(Integer ventanaDias, List<Distancia> categorias) {
    }

    public record Limites(Login login, Integer reportesMaximosPorDia, Duplicados duplicados) {
    }

    private final JdbcTemplate jdbc;

    public ParametrosServicio(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private int entero(String clave, int respaldo) {
        return jdbc.query("SELECT valor FROM configuracion WHERE clave = ?", rs -> rs.next() ? Integer.parseInt(rs.getString(1)) : respaldo, clave);
    }

    public int intentosMaximos() {
        return entero(INTENTOS, 3);
    }

    public int ventanaLoginMinutos() {
        return entero(VENTANA_LOGIN, 5);
    }

    public int bloqueoLoginMinutos() {
        return entero(BLOQUEO, 5);
    }

    public int reportesMaximosPorDia() {
        return entero(REPORTES_POR_DIA, 2);
    }

    public int ventanaDuplicadoDias() {
        return entero(VENTANA_DUPLICADO, 30);
    }

    /** Metros de la categoría, o vacío si esa categoría no revisa duplicados. */
    public Optional<Integer> distanciaDuplicado(int idCategoria) {
        return jdbc.query("SELECT metros FROM distancia_duplicado WHERE id_categoria = ?",
                rs -> rs.next() ? Optional.of(rs.getInt(1)) : Optional.<Integer>empty(), idCategoria);
    }

    @Transactional(readOnly = true)
    public Limites consultar() {
        List<Distancia> categorias = jdbc.query("""
                SELECT d.id_categoria, c.nombre, d.metros
                  FROM distancia_duplicado d JOIN categoria c ON c.id_categoria = d.id_categoria
                 WHERE c.activa ORDER BY d.id_categoria""",
                (rs, i) -> new Distancia(rs.getInt(1), rs.getString(2), rs.getInt(3)));
        return new Limites(new Login(intentosMaximos(), ventanaLoginMinutos(), bloqueoLoginMinutos()),
                reportesMaximosPorDia(), new Duplicados(ventanaDuplicadoDias(), categorias));
    }

    /** Valida todo antes de escribir y guarda todo o nada, con quién y cuándo lo cambió. */
    @Transactional
    public Limites actualizar(String correoAdmin, Limites nuevos) {
        validar(nuevos);
        Long idAdmin = jdbc.query("SELECT id_usuario FROM usuario WHERE lower(correo) = lower(?)",
                rs -> rs.next() ? rs.getLong(1) : null, correoAdmin);
        guardar(INTENTOS, nuevos.login().intentosMaximos(), idAdmin);
        guardar(VENTANA_LOGIN, nuevos.login().ventanaMinutos(), idAdmin);
        guardar(BLOQUEO, nuevos.login().bloqueoMinutos(), idAdmin);
        guardar(REPORTES_POR_DIA, nuevos.reportesMaximosPorDia(), idAdmin);
        guardar(VENTANA_DUPLICADO, nuevos.duplicados().ventanaDias(), idAdmin);
        for (Distancia d : categoriasDe(nuevos)) {
            jdbc.update("""
                    UPDATE distancia_duplicado SET metros = ?, id_usuario_actualiza = ?, actualizado_en = now()
                     WHERE id_categoria = ?""", d.metros(), idAdmin, d.idCategoria());
        }
        return consultar();
    }

    private void guardar(String clave, int valor, Long idAdmin) {
        jdbc.update("UPDATE configuracion SET valor = ?, id_usuario_actualiza = ?, actualizado_en = now() WHERE clave = ?",
                String.valueOf(valor), idAdmin, clave);
    }

    private static List<Distancia> categoriasDe(Limites l) {
        return l.duplicados().categorias() == null ? List.of() : l.duplicados().categorias();
    }

    private void validar(Limites l) {
        if (l == null || l.login() == null) throw error("Envía los datos de ingreso.", "login");
        if (l.duplicados() == null) throw error("Envía los datos de duplicados.", "duplicados");
        rango(l.login().intentosMaximos(), RANGO_INTENTOS, "Los intentos fallidos permitidos", "intentosMaximos", "");
        rango(l.login().ventanaMinutos(), RANGO_MINUTOS, "Los minutos para contar los intentos", "ventanaMinutos", " minutos");
        rango(l.login().bloqueoMinutos(), RANGO_MINUTOS, "Los minutos de descanso tras el bloqueo", "bloqueoMinutos", " minutos");
        rango(l.reportesMaximosPorDia(), RANGO_REPORTES, "Los reportes por cuenta al día", "reportesMaximosPorDia", "");
        rango(l.duplicados().ventanaDias(), RANGO_DIAS, "Los días para considerar un reporte reciente", "ventanaDias", " días");
        List<Integer> vistas = new ArrayList<>();
        for (Distancia d : categoriasDe(l)) {
            if (d == null || d.idCategoria() == null) throw error("Falta el número de la categoría.", "categorias");
            boolean existe = Boolean.TRUE.equals(jdbc.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM distancia_duplicado WHERE id_categoria = ?)", Boolean.class, d.idCategoria()));
            if (!existe || vistas.contains(d.idCategoria())) {
                throw error("La categoría " + d.idCategoria() + " no admite distancia de duplicado.", "categorias");
            }
            vistas.add(d.idCategoria());
            rango(d.metros(), RANGO_METROS, "La distancia de la categoría " + d.idCategoria(), "categorias." + d.idCategoria() + ".metros", " metros");
        }
    }

    private static void rango(Integer valor, int[] limites, String nombre, String campo, String unidad) {
        if (valor == null || valor < limites[0] || valor > limites[1]) {
            throw error(nombre + " deben estar entre " + limites[0] + " y " + limites[1] + unidad + ".", campo);
        }
    }

    private static ApiException error(String mensaje, String campo) {
        return new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION, mensaje, campo);
    }
}
