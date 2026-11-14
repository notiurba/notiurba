package com.notiurba.reporte;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return repositorio.listarPropios(idUsuario, FiltroPanel.nombreDeEstado(estado).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<ReporteDetalleResponse> todos(String estado, Integer idCategoria, Integer comuna,
            LocalDate desde, LocalDate hasta) {
        return repositorio.listarParaAdmin(FiltroPanel.de(estado, idCategoria, comuna, desde, hasta));
    }
}
