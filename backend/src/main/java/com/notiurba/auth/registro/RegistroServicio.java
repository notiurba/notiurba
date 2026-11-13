package com.notiurba.auth.registro;

import java.time.OffsetDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.notiurba.auth.ReglaContrasena;
import com.notiurba.usuario.CatalogosRegistro;
import com.notiurba.usuario.ReglasPerfil;
import com.notiurba.usuario.Rol;
import com.notiurba.usuario.RolRepository;
import com.notiurba.usuario.Usuario;
import com.notiurba.usuario.UsuarioRepository;
import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

/** Registro con correo y contraseña. La cuenta queda sin verificar y se envía el enlace de confirmación. */
@Service
public class RegistroServicio {

    static final String MENSAJE = "Te enviamos un enlace para confirmar tu correo. Ábrelo para activar tu cuenta.";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final CatalogosRegistro catalogos;
    private final PasswordEncoder passwordEncoder;
    private final VerificacionServicio verificacion;

    public RegistroServicio(UsuarioRepository usuarioRepository, RolRepository rolRepository,
            CatalogosRegistro catalogos, PasswordEncoder passwordEncoder, VerificacionServicio verificacion) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.catalogos = catalogos;
        this.passwordEncoder = passwordEncoder;
        this.verificacion = verificacion;
    }

    @Transactional
    public MensajeResponse registrar(RegistroRequest p) {
        String nombres = ReglasPerfil.limpiarNombre(p.nombres(), "Los nombres", "nombres");
        String apellidos = ReglasPerfil.limpiarNombre(p.apellidos(), "Los apellidos", "apellidos");
        if (!catalogos.existeTipoDocumento(p.idTipoDocumento())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION,
                    "El tipo de documento no existe", "idTipoDocumento");
        }
        String numeroDocumento = p.numeroDocumento().trim();
        if (numeroDocumento.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION,
                    "Completa el campo «Número de documento».", "numeroDocumento");
        }
        if (!catalogos.existeBarrio(p.idBarrio())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION, "El barrio no existe", "idBarrio");
        }
        String correo = p.correo().trim();
        ReglaContrasena.validar(p.password(), "password");

        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ApiException(HttpStatus.CONFLICT, Codigos.CORREO_YA_REGISTRADO,
                    "Ya existe una cuenta con este correo", "correo");
        }
        if (usuarioRepository.existsByIdTipoDocumentoAndNumeroDocumento(p.idTipoDocumento(), numeroDocumento)) {
            throw new ApiException(HttpStatus.CONFLICT, Codigos.DOCUMENTO_YA_REGISTRADO,
                    "Ya existe una cuenta con este documento", "numeroDocumento");
        }

        Rol ciudadano = rolRepository.findByNombre(Rol.CIUDADANO)
                .orElseThrow(() -> new IllegalStateException("Falta el rol " + Rol.CIUDADANO));
        Usuario usuario = Usuario.nuevoConContrasena(
                nombres, apellidos, p.idTipoDocumento(), numeroDocumento, p.idBarrio(),
                p.direccionCorrespondencia().trim(), correo,
                p.telefono() == null || p.telefono().isBlank() ? null : p.telefono().trim(),
                passwordEncoder.encode(p.password()), ciudadano, catalogos.avisoPrivacidadVigente(),
                OffsetDateTime.now());
        try {
            usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException carrera) {
            // Otra petición registró el mismo correo o documento entre la revisión y el INSERT
            throw new ApiException(HttpStatus.CONFLICT, Codigos.CONFLICTO,
                    "No se pudo crear la cuenta: el documento o el correo ya están registrados");
        }
        verificacion.emitir(usuario);
        return new MensajeResponse(correo, MENSAJE);
    }
}
