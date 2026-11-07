package com.notiurba.usuario;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.notiurba.foto.AlmacenFotos;
import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

/**
 * Supresión de datos a pedido del ciudadano (habeas data, Ley 1581): «Eliminar mis datos» borra sus reportes y
 * fotos y conserva la cuenta; «Eliminar mi cuenta» borra además la cuenta.
 *
 * <p>Primero se borran las fotos del almacén externo. Si el almacén falla (502) no se borra nada de la base y se
 * puede reintentar: es peor dejar una foto con datos personales huérfana en el almacén que repetir la petición.
 * Los reportes ya enviados por correo a una entidad no se pueden recuperar desde aquí.
 */
@Service
public class EliminacionDatosServicio {

    private final UsuarioRepository usuarios;
    private final AlmacenFotos almacen;
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public EliminacionDatosServicio(UsuarioRepository usuarios, AlmacenFotos almacen, NamedParameterJdbcTemplate jdbc,
            PlatformTransactionManager transacciones) {
        this.usuarios = usuarios;
        this.almacen = almacen;
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(transacciones);
    }

    /** Borra los reportes del ciudadano (con sus fotos, historial y envíos) y lo que subió o apoyó. La cuenta queda. */
    public void eliminarDatos(String correoSesion) {
        long id = propio(correoSesion).getIdUsuario();
        borrarFotosDelAlmacen(id);
        tx.executeWithoutResult(s -> {
            var p = new MapSqlParameterSource("u", id);
            jdbc.update("DELETE FROM reporte WHERE id_usuario = :u", p);          // en cascada: fotos, historial, envíos
            jdbc.update("DELETE FROM evidencia_reporte WHERE id_usuario = :u", p);  // fotos que subió a reportes ajenos
            jdbc.update("DELETE FROM apoyo_reporte WHERE id_usuario = :u", p);
        });
    }

    /** Borra la cuenta y todo lo suyo. La cuenta de administración no se elimina por aquí. */
    public void eliminarCuenta(String correoSesion) {
        Usuario usuario = propio(correoSesion);
        if (usuario.getRol() != null && Rol.ADMIN.equals(usuario.getRol().getNombre())) {
            throw new ApiException(HttpStatus.FORBIDDEN, Codigos.SIN_PERMISO,
                    "La cuenta de administración no se puede eliminar desde aquí.");
        }
        long id = usuario.getIdUsuario();
        borrarFotosDelAlmacen(id);
        // Todo lo que cuelga del usuario se borra en cascada (tokens, identidad de Google, reportes...)
        tx.executeWithoutResult(s ->
                jdbc.update("DELETE FROM usuario WHERE id_usuario = :u", new MapSqlParameterSource("u", id)));
    }

    private void borrarFotosDelAlmacen(long idUsuario) {
        List<String> publicas = jdbc.queryForList("""
                SELECT DISTINCT e.id_publico_proveedor
                  FROM evidencia_reporte e JOIN reporte r ON r.id_reporte = e.id_reporte
                 WHERE (r.id_usuario = :u OR e.id_usuario = :u) AND e.id_publico_proveedor IS NOT NULL""",
                new MapSqlParameterSource("u", idUsuario), String.class);
        for (String idPublico : publicas) {
            almacen.borrar(idPublico);   // si falla sube AlmacenFotosNoDisponibleException (502) y no se borra nada más
        }
    }

    private Usuario propio(String correoSesion) {
        return usuarios.findByCorreoIgnoreCase(correoSesion)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, Codigos.NO_AUTENTICADO,
                        Codigos.MSG_NO_AUTENTICADO));
    }
}
