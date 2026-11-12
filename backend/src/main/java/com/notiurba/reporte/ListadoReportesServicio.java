package com.notiurba.reporte;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.notiurba.admin.EstadoReporte;
import com.notiurba.usuario.UsuarioRepository;
import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

/** «Mis reportes» (ciudadano) y el listado del panel (administrador, RF-19). */
@Service
public class ListadoReportesServicio {

    private final ListadoReportesRepositorio repositorio;
    private final UsuarioRepository usuarios;

    public ListadoReportesServicio(ListadoReportesRepositorio repositorio, UsuarioRepository usuarios) {
        this.repositorio = repositorio;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<ReporteResumenResponse> misReportes(String correoUsuario, String estado) {
        long idUsuario = usuarios.findByCorreoIgnoreCase(correoUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, Codigos.NO_AUTENTICADO,
                        Codigos.MSG_NO_AUTENTICADO))
                .getIdUsuario();
        return repositorio.listarPropios(idUsuario, nombreDeEstado(estado).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<ReporteDetalleResponse> todos(String estado, Integer idCategoria, Integer comuna,
            LocalDate desde, LocalDate hasta) {
        if (comuna != null && (comuna < 1 || comuna > 10)) {
            throw error("La comuna es un número de 1 a 10", "comuna");
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw error("La fecha «desde» no puede ser posterior a «hasta»", "desde");
        }
        return repositorio.listarParaAdmin(new ListadoReportesRepositorio.FiltroAdmin(
                nombreDeEstado(estado).orElse(null), idCategoria, comuna, desde, hasta));
    }

    /** Acepta el nombre del catálogo («En trámite») o el identificador de la API (EN_TRAMITE). */
    public static Optional<String> nombreDeEstado(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        String v = valor.trim();
        for (EstadoReporte e : EstadoReporte.values()) {
            if (e.nombre().equalsIgnoreCase(v) || e.name().equalsIgnoreCase(v)) {
                return Optional.of(e.nombre());
            }
        }
        throw error("El estado debe ser Recibido, Enviado, En trámite o Resuelto", "estado");
    }

    private static ApiException error(String mensaje, String campo) {
        return new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION, mensaje, campo);
    }
}
