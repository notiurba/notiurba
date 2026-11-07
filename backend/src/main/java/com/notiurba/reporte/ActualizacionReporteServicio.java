package com.notiurba.reporte;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.notiurba.usuario.UsuarioRepository;
import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

/**
 * «Actualizar mi reporte»: el ciudadano avisa que la entidad le respondió o que el problema se resolvió, y
 * puede anotar el radicado. Su aviso NO cambia el estado oficial (eso lo hace solo el administrador): queda
 * como una nota en el historial con el mismo estado que ya tenía, y el radicado se guarda en el reporte.
 */
@Service
public class ActualizacionReporteServicio {

    static final String RESPUESTA = "RESPUESTA";
    static final String RESUELTO = "RESUELTO";
    private static final Set<String> TIPOS = Set.of(RESPUESTA, RESUELTO);

    private final ReporteRepositorio reportes;
    private final UsuarioRepository usuarios;
    private final NamedParameterJdbcTemplate jdbc;

    public ActualizacionReporteServicio(ReporteRepositorio reportes, UsuarioRepository usuarios,
            NamedParameterJdbcTemplate jdbc) {
        this.reportes = reportes;
        this.usuarios = usuarios;
        this.jdbc = jdbc;
    }

    @Transactional
    public ReporteDetalleResponse actualizar(String correoUsuario, String codigo, ActualizarReporteRequest p) {
        String normalizado = ReporteServicio.normalizarCodigo(codigo);
        long idUsuario = usuarios.findByCorreoIgnoreCase(correoUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, Codigos.NO_AUTENTICADO,
                        Codigos.MSG_NO_AUTENTICADO))
                .getIdUsuario();

        String tipo = p.tipo() == null ? "" : p.tipo().trim().toUpperCase();
        if (!TIPOS.contains(tipo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION,
                    "Elige qué pasó con tu reporte", "tipo");
        }
        String radicado = p.radicado() == null || p.radicado().isBlank() ? null : p.radicado().trim();
        if (radicado != null && radicado.length() > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION,
                    "El radicado no puede pasar de 50 caracteres", "radicado");
        }

        long idReporte = reportes.bloquearReportePropio(idUsuario, normalizado)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Codigos.NO_ENCONTRADO,
                        "No encontramos ese reporte."));

        if (radicado != null) {
            jdbc.update("UPDATE reporte SET radicado_oficial = :radicado WHERE id_reporte = :id",
                    new MapSqlParameterSource().addValue("radicado", radicado).addValue("id", idReporte));
        }
        // Nota en el historial con el estado ACTUAL: no avanza ni retrocede nada (B11) y el administrador la ve
        jdbc.update("""
                INSERT INTO historial_estado (id_reporte, id_estado_reporte, observacion, id_usuario_cambia)
                SELECT id_reporte, id_estado_reporte, :observacion, :usuario FROM reporte WHERE id_reporte = :id""",
                new MapSqlParameterSource().addValue("id", idReporte).addValue("usuario", idUsuario)
                        .addValue("observacion", observacion(tipo, radicado)));

        return reportes.buscarPropio(idUsuario, normalizado)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Codigos.NO_ENCONTRADO,
                        "No encontramos ese reporte."));
    }

    static String observacion(String tipo, String radicado) {
        String texto = RESUELTO.equals(tipo)
                ? "El ciudadano avisó que su reporte ya se resolvió. El estado oficial lo confirma el administrador."
                : "El ciudadano avisó que la entidad le respondió.";
        return radicado == null ? texto : texto + " Radicado: " + radicado + ".";
    }
}
