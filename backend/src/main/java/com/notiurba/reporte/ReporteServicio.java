package com.notiurba.reporte;

import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.notiurba.geo.AsignadorDeComuna;
import com.notiurba.usuario.CatalogosRegistro;
import com.notiurba.usuario.UsuarioRepository;
import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

/**
 * Crea reportes. Las reglas de la base (B3, B6) se revisan antes para devolver un error con su
 * campo en vez de una violación de la base.
 *
 * Con GPS, el punto debe caer en una comuna o a menos de la distancia máxima de una (NOT-55,
 * RN-03); si no, 422 FUERA_DE_COBERTURA. La comuna no se guarda: sale de v_reporte_comuna, que usa
 * la misma función de PostGIS. Mientras ninguna comuna tenga polígono (comuna.geom NULL) no se puede
 * decidir y el reporte se guarda sin rechazo: la comuna sale del barrio.
 */
@Service
public class ReporteServicio {

    /** NU- más 6 o más dígitos (el trigger amplía el relleno si pasa de 6). */
    static final Pattern FORMATO_CODIGO = Pattern.compile("NU-\\d{6,}");
    static final String NO_ENCONTRADO = "No encontramos un reporte tuyo con ese código.";

    static final int DESCRIPCION_MINIMA = 10;
    static final int DESCRIPCION_MAXIMA = 500;

    private final ReporteRepositorio repositorio;
    private final UsuarioRepository usuarios;
    private final CatalogosRegistro catalogos;
    private final AsignadorDeComuna asignador;

    public ReporteServicio(ReporteRepositorio repositorio, UsuarioRepository usuarios, CatalogosRegistro catalogos,
            AsignadorDeComuna asignador) {
        this.repositorio = repositorio;
        this.usuarios = usuarios;
        this.catalogos = catalogos;
        this.asignador = asignador;
    }

    @Transactional
    public ReporteCreadoResponse crear(String correoUsuario, CrearReporteRequest p) {
        if (!Boolean.TRUE.equals(p.autorizaEnvio())) throw error("Debes autorizar la remisión", "autorizaEnvio");
        long idUsuario = usuarios.findByCorreoIgnoreCase(correoUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, Codigos.NO_AUTENTICADO,
                        Codigos.MSG_NO_AUTENTICADO))
                .getIdUsuario();

        var categoria = repositorio.categoriaActiva(p.idCategoria())
                .orElseThrow(() -> error("La categoría no existe o no está disponible", "idCategoria"));

        if ((p.latitud() == null) != (p.longitud() == null)) {
            throw error("Envía la latitud y la longitud juntas", p.latitud() == null ? "latitud" : "longitud");
        }
        boolean hayGps = p.latitud() != null;
        if (hayGps && asignador.asignar(p.latitud(), p.longitud()).fueraDeCobertura()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, Codigos.FUERA_DE_COBERTURA,
                    Codigos.MSG_FUERA_DE_COBERTURA, "latitud");
        }
        if (p.idBarrio() != null && !catalogos.existeBarrio(p.idBarrio())) {
            throw error("El barrio no existe", "idBarrio");
        }
        if (categoria.requiereUbicacion() && !hayGps && p.idBarrio() == null) {
            throw error("Marca la ubicación en el mapa o elige el barrio", "idBarrio");
        }

        String descripcion = p.descripcion().trim();
        if (descripcion.length() < DESCRIPCION_MINIMA || descripcion.length() > DESCRIPCION_MAXIMA) {
            throw error("La descripción debe tener entre " + DESCRIPCION_MINIMA + " y "
                    + DESCRIPCION_MAXIMA + " caracteres", "descripcion");
        }
        String matricula = vacioANulo(p.matriculaCuentaServicio());
        if (matricula != null && !categoria.esEmpresa()) {
            throw error("La matrícula solo aplica a servicios públicos", "matriculaCuentaServicio");
        }

        ReporteCreadoResponse creado = repositorio.insertar(new ReporteRepositorio.NuevoReporte(
                idUsuario, p.idCategoria(), p.idBarrio(), descripcion, p.latitud(), p.longitud(),
                vacioANulo(p.direccionIncidente()), matricula, vacioANulo(p.radicadoOficial())));
        repositorio.prepararEnrutamiento(creado.codigoSeguimiento(), Boolean.TRUE.equals(p.emergencia()));
        return creado;
    }

    /** Un reporte propio por código. No existe y «es de otra persona» dan exactamente la misma respuesta. */
    @Transactional(readOnly = true)
    public ReporteDetalleResponse consultar(String correoUsuario, String codigo) {
        String normalizado = normalizarCodigo(codigo);
        long idUsuario = usuarios.findByCorreoIgnoreCase(correoUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, Codigos.NO_AUTENTICADO,
                        Codigos.MSG_NO_AUTENTICADO))
                .getIdUsuario();
        return repositorio.buscarPropio(idUsuario, normalizado)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Codigos.NO_ENCONTRADO, NO_ENCONTRADO));
    }

    /** Quita espacios, pasa a mayúsculas y exige el formato NU-000123 (400 con campo «codigo» si no cumple). */
    public static String normalizarCodigo(String codigo) {
        String normalizado = codigo == null ? "" : codigo.trim().toUpperCase(java.util.Locale.ROOT);
        if (!FORMATO_CODIGO.matcher(normalizado).matches()) {
            throw error("El código tiene el formato NU-000123", "codigo");
        }
        return normalizado;
    }

    private static ApiException error(String mensaje, String campo) {
        return new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION, mensaje, campo);
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
