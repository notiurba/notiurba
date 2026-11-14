package com.notiurba.admin;

import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.notiurba.reporte.ReporteServicio;
import com.notiurba.usuario.UsuarioRepository;
import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

@Service
public class AdminServicio {
    static final String OBSERVACION_POR_DEFECTO = "Estado actualizado por el administrador.";
    private final AdminRepositorio repositorio;
    private final UsuarioRepository usuarios;

    public AdminServicio(AdminRepositorio repositorio, UsuarioRepository usuarios) {
        this.repositorio = repositorio;
        this.usuarios = usuarios;
    }

    private long administrador(String correo) {
        var usuario = usuarios.findByCorreoIgnoreCase(correo).orElseThrow(() ->
                new ApiException(HttpStatus.UNAUTHORIZED, Codigos.NO_AUTENTICADO, Codigos.MSG_NO_AUTENTICADO));
        if (!"ADMIN".equals(usuario.getRol().getNombre())) {
            throw new ApiException(HttpStatus.FORBIDDEN, Codigos.SIN_PERMISO, Codigos.MSG_SIN_PERMISO);
        }
        return usuario.getIdUsuario();
    }

    @Transactional
    public CambioEstadoResponse cambiarEstado(String correo, String codigo, CambiarEstadoRequest peticion) {
        long actor = administrador(correo);
        String normalizado = ReporteServicio.normalizarCodigo(codigo);
        if (peticion.estado() == null || (peticion.observacion() != null && peticion.observacion().length() > 1000)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION,
                    "Indica el estado y una observación de hasta 1000 caracteres", "observacion");
        }
        var actual = repositorio.bloquear(normalizado).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, Codigos.NO_ENCONTRADO, "No encontramos ese reporte"));
        var destino = repositorio.destino(peticion.estado());
        if (EstadoReporte.RESUELTO.nombre().equals(actual.estado()) || destino.orden() < actual.orden()
                || (peticion.estado() == EstadoReporte.ENVIADO && !EstadoReporte.ENVIADO.nombre().equals(actual.estado()))) {
            throw new ApiException(HttpStatus.CONFLICT, Codigos.CONFLICTO,
                    "El estado no puede retroceder, Resuelto es terminal y Enviado se asigna al aceptar el correo");
        }
        String observacion = peticion.observacion() == null || peticion.observacion().isBlank()
                ? OBSERVACION_POR_DEFECTO : peticion.observacion().strip();
        var ultimo = repositorio.ultimo(actual.id());
        var repetido = ultimo.filter(h -> h.estado() == destino.id() && Objects.equals(h.cambiadoPor(), actor)
                && observacion.equals(h.observacion()));
        var fecha = repetido.isPresent() ? repetido.get().cambiadoEn()
                : repositorio.insertar(actual.id(), destino.id(), observacion, actor);
        return new CambioEstadoResponse(normalizado, peticion.estado(), observacion, fecha, actor, actual.vigenteHasta());
    }

    @Transactional(readOnly = true)
    public VigenciaResponse consultarVigencia() {
        return new VigenciaResponse(repositorio.vigencia());
    }

    @Transactional
    public VigenciaResponse actualizarVigencia(String correo, VigenciaRequest peticion) {
        long actor = administrador(correo);
        var valor = peticion.vigenciaDias();
        if (valor == null || !valor.isIntegralNumber() || !valor.canConvertToInt() || valor.intValue() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION,
                    "La vigencia debe ser un entero positivo", "vigenciaDias");
        }
        repositorio.guardarVigencia(valor.intValue(), actor);
        return new VigenciaResponse(valor.intValue());
    }
}
