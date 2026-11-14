package com.notiurba.reporte;

import java.util.LinkedHashMap;
import java.util.Map;
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
 * Con GPS, el punto debe caer en una comuna o a menos de la distancia máxima de una (RN-03); si no,
 * 422 FUERA_DE_COBERTURA. La base guarda la comuna en el reporte con la misma función de PostGIS.
 * Mientras ninguna comuna tenga polígono (comuna.geom NULL) no se puede decidir y el reporte se
 * guarda sin rechazo: la comuna sale del barrio.
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
    private final LimiteDiarioServicio limite;
    private final DuplicadoServicio duplicados;

    public ReporteServicio(ReporteRepositorio repositorio, UsuarioRepository usuarios, CatalogosRegistro catalogos,
            AsignadorDeComuna asignador, LimiteDiarioServicio limite, DuplicadoServicio duplicados) {
        this.repositorio = repositorio;
        this.usuarios = usuarios;
        this.catalogos = catalogos;
        this.asignador = asignador;
        this.limite = limite;
        this.duplicados = duplicados;
    }

    @Transactional
    public ReporteCreadoResponse crear(String correoUsuario, CrearReporteRequest p) {
        if (!Boolean.TRUE.equals(p.autorizaEnvio())) throw error("Debes autorizar la remisión", "autorizaEnvio");
        Borrador b = validar(correoUsuario, p);
        // Una cuenta a la vez: así dos envíos simultáneos no se saltan el tope diario
        repositorio.bloquearCiudadano(b.idUsuario());
        limite.verificar(b.idUsuario());
        if (!Boolean.TRUE.equals(p.confirmarDuplicado())) {
            duplicados.buscarSinResolver(b.idUsuario(), b.idCategoria(), b.latitud(), b.longitud()).ifPresent(parecido -> {
                throw avisoDeDuplicado(parecido);
            });
        }
        boolean requiereEvidencia = duplicados.repiteResuelto(b.idCategoria(), b.latitud(), b.longitud());
        if (requiereEvidencia && (p.totalFotos() == null || p.totalFotos() < 1)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, Codigos.EVIDENCIA_REQUERIDA,
                    Codigos.MSG_EVIDENCIA_REQUERIDA, "totalFotos");
        }
        ReporteCreadoResponse creado = repositorio.insertar(new ReporteRepositorio.NuevoReporte(
                b.idUsuario(), b.idCategoria(), b.idBarrio(), b.descripcion(), b.latitud(), b.longitud(),
                b.direccionIncidente(), b.matricula(), vacioANulo(p.radicadoOficial()), requiereEvidencia));
        repositorio.prepararEnrutamiento(creado.codigoSeguimiento(), b.emergencia());
        return creado;
    }

    /**
     * Un reporte que pasó las mismas reglas que al crearlo, sin guardar nada (lo usa también la vista
     * previa del mensaje). comunaDelPunto: la que PostGIS le da al GPS, o null.
     */
    public record Borrador(long idUsuario, int idCategoria, ReporteRepositorio.DatosCategoria categoria,
            Integer idBarrio, Double latitud, Double longitud, Integer comunaDelPunto, String descripcion,
            String direccionIncidente, String matricula, boolean emergencia) {
    }

    @Transactional(readOnly = true)
    public Borrador validar(String correoUsuario, BorradorDeReporte p) {
        Integer idCategoria = p.idCategoria();
        Double latitud = p.latitud();
        Double longitud = p.longitud();
        Integer idBarrio = p.idBarrio();
        long idUsuario = usuarios.findByCorreoIgnoreCase(correoUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, Codigos.NO_AUTENTICADO,
                        Codigos.MSG_NO_AUTENTICADO))
                .getIdUsuario();

        if (idCategoria == null) throw error("La categoría no existe o no está disponible", "idCategoria");
        var categoria = repositorio.categoriaActiva(idCategoria)
                .orElseThrow(() -> error("La categoría no existe o no está disponible", "idCategoria"));

        if ((latitud == null) != (longitud == null)) {
            throw error("Envía la latitud y la longitud juntas", latitud == null ? "latitud" : "longitud");
        }
        boolean hayGps = latitud != null;
        Integer comunaDelPunto = null;
        if (hayGps) {
            var resultado = asignador.asignar(latitud, longitud);
            if (resultado.fueraDeCobertura()) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, Codigos.FUERA_DE_COBERTURA,
                        Codigos.MSG_FUERA_DE_COBERTURA, "latitud");
            }
            comunaDelPunto = resultado.asignacion().map(AsignadorDeComuna.Asignacion::idComuna).orElse(null);
        }
        if (idBarrio != null && !catalogos.existeBarrio(idBarrio)) {
            throw error("El barrio no existe", "idBarrio");
        }
        if (categoria.requiereUbicacion() && !hayGps && idBarrio == null) {
            throw error("Marca la ubicación en el mapa o elige el barrio", "idBarrio");
        }

        String descripcion = p.descripcion() == null ? "" : p.descripcion().trim();
        if (descripcion.length() < DESCRIPCION_MINIMA || descripcion.length() > DESCRIPCION_MAXIMA) {
            throw error("La descripción debe tener entre " + DESCRIPCION_MINIMA + " y "
                    + DESCRIPCION_MAXIMA + " caracteres", "descripcion");
        }
        String matricula = vacioANulo(p.matriculaCuentaServicio());
        if (matricula != null && !categoria.esEmpresa()) {
            throw error("La matrícula solo aplica a servicios públicos", "matriculaCuentaServicio");
        }
        return new Borrador(idUsuario, idCategoria, categoria, idBarrio, latitud, longitud, comunaDelPunto, descripcion,
                vacioANulo(p.direccionIncidente()), matricula,
                repositorio.esEmergencia(idCategoria, descripcion, Boolean.TRUE.equals(p.emergencia())));
    }

    /** Solo datos base del parecido; el código únicamente si es de la misma persona (nunca la identidad de otra). */
    private static ApiException avisoDeDuplicado(DuplicadoServicio.Parecido parecido) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("esPropio", parecido.propio());
        datos.put("cantidad", parecido.cantidad());
        datos.put("estadoExistente", parecido.estado());
        datos.put("creadoEn", parecido.fecha());
        datos.put("descripcion", parecido.resumen());
        if (parecido.propio()) {
            datos.put("codigoExistente", parecido.codigo());
        }
        return new ApiException(HttpStatus.CONFLICT, Codigos.POSIBLE_DUPLICADO,
                "Ya hay un reporte parecido en este lugar. Si es otro problema, puedes enviarlo de todas maneras.", null, datos);
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
