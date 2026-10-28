package com.notiurba.auth.google;

import java.time.OffsetDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.notiurba.seguridad.JwtProveedor;
import com.notiurba.usuario.CatalogosRegistro;
import com.notiurba.usuario.IdentidadExterna;
import com.notiurba.usuario.IdentidadExternaRepository;
import com.notiurba.usuario.ProveedorIdentidad;
import com.notiurba.usuario.ProveedorIdentidadRepository;
import com.notiurba.usuario.Rol;
import com.notiurba.usuario.RolRepository;
import com.notiurba.usuario.Usuario;
import com.notiurba.usuario.UsuarioRepository;

import io.jsonwebtoken.Claims;

/**
 * «Continuar con Google» en dos pasos: verificar el ID token y, si la persona no tiene
 * cuenta, completar el perfil (documento, barrio, dirección) que Google no entrega.
 */
@Service
public class GoogleAuthServicio {

    static final int MAX_LONGITUD_NOMBRE = 255;

    private final GoogleTokenVerificador verificador;
    private final JwtProveedor jwtProveedor;
    private final UsuarioRepository usuarioRepository;
    private final IdentidadExternaRepository identidadRepository;
    private final ProveedorIdentidadRepository proveedorRepository;
    private final RolRepository rolRepository;
    private final CatalogosRegistro catalogos;

    public GoogleAuthServicio(
            GoogleTokenVerificador verificador,
            JwtProveedor jwtProveedor,
            UsuarioRepository usuarioRepository,
            IdentidadExternaRepository identidadRepository,
            ProveedorIdentidadRepository proveedorRepository,
            RolRepository rolRepository,
            CatalogosRegistro catalogos) {
        this.verificador = verificador;
        this.jwtProveedor = jwtProveedor;
        this.usuarioRepository = usuarioRepository;
        this.identidadRepository = identidadRepository;
        this.proveedorRepository = proveedorRepository;
        this.rolRepository = rolRepository;
        this.catalogos = catalogos;
    }

    @Transactional
    public ResultadoGoogle autenticar(String credential) {
        PerfilGoogle perfil = verificar(credential);
        ProveedorIdentidad google = proveedorGoogle();

        // Caso 1: la identidad de Google ya está vinculada → login normal
        var identidad = identidadRepository.findByProveedorAndIdentificadorExterno(google, perfil.sub());
        if (identidad.isPresent()) {
            return sesion(identidad.get().getUsuario());
        }

        // Caso 2: ya hay una cuenta con ese correo → se vincula (PROPUESTA, ver vincularPorCorreo)
        var porCorreo = usuarioRepository.findByCorreoIgnoreCase(perfil.correo());
        if (porCorreo.isPresent()) {
            return sesion(vincularPorCorreo(porCorreo.get(), google, perfil));
        }

        // Caso 3: no hay cuenta → no se crea nada; el frontend pide el resto del perfil
        return perfilIncompleto(perfil);
    }

    /**
     * PROPUESTA pendiente de confirmar con Andrés: vincular la cuenta de Google a la cuenta
     * existente con el mismo correo. Es segura porque Google garantiza email_verified = true.
     * Para quitarla, borrar este método y su llamada: la persona caería en el caso 3 y
     * /completar respondería 409 por correo repetido.
     */
    private Usuario vincularPorCorreo(Usuario usuario, ProveedorIdentidad google, PerfilGoogle perfil) {
        if (identidadRepository.existsByUsuarioAndProveedor(usuario, google)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta cuenta ya está vinculada a otra cuenta de Google");
        }
        OffsetDateTime ahora = OffsetDateTime.now();
        identidadRepository.save(new IdentidadExterna(usuario, google, perfil.sub(), ahora));
        usuario.marcarCorreoVerificado(ahora);
        return usuario;
    }

    /** nombres = given_name y apellidos = family_name; sin given_name, nombres = name. Nunca bloquea. */
    private ResultadoGoogle.PerfilIncompleto perfilIncompleto(PerfilGoogle perfil) {
        String nombres;
        String apellidos;
        if (tieneTexto(perfil.givenName())) {
            nombres = perfil.givenName();
            apellidos = tieneTexto(perfil.familyName()) ? perfil.familyName() : "";
        } else {
            nombres = tieneTexto(perfil.nombreCompleto()) ? perfil.nombreCompleto() : "";
            apellidos = "";
        }
        String tokenRegistro = jwtProveedor.generarRegistroGoogle(perfil.sub(), perfil.correo());
        return new ResultadoGoogle.PerfilIncompleto(
                ResultadoGoogle.PerfilIncompleto.ESTADO, tokenRegistro, perfil.correo(), nombres, apellidos);
    }

    @Transactional
    public ResultadoGoogle.Sesion completar(CompletarPerfilRequest peticion) {
        Claims registro = validarTokenRegistro(peticion.tokenRegistro());
        String sub = registro.getSubject();
        String correo = registro.get("correo", String.class);

        if (!Boolean.TRUE.equals(peticion.aceptaPrivacidad())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes aceptar el aviso de privacidad");
        }
        String nombres = limpiarNombre(peticion.nombres(), "Los nombres");
        String apellidos = limpiarNombre(peticion.apellidos(), "Los apellidos");
        if (!catalogos.existeTipoDocumento(peticion.idTipoDocumento())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo de documento no existe");
        }
        if (!catalogos.existeBarrio(peticion.idBarrio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El barrio no existe");
        }

        ProveedorIdentidad google = proveedorGoogle();
        String numeroDocumento = peticion.numeroDocumento().trim();
        if (identidadRepository.findByProveedorAndIdentificadorExterno(google, sub).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta cuenta de Google ya está registrada: inicia sesión con Google");
        }
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con este correo");
        }
        if (usuarioRepository.existsByIdTipoDocumentoAndNumeroDocumento(peticion.idTipoDocumento(), numeroDocumento)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con este documento");
        }

        Rol ciudadano = rolRepository.findByNombre(Rol.CIUDADANO)
                .orElseThrow(() -> new IllegalStateException("Falta el rol " + Rol.CIUDADANO));
        OffsetDateTime ahora = OffsetDateTime.now();
        Usuario usuario = Usuario.nuevoConGoogle(
                nombres, apellidos, peticion.idTipoDocumento(), numeroDocumento, peticion.idBarrio(),
                peticion.direccionCorrespondencia().trim(), correo,
                tieneTexto(peticion.telefono()) ? peticion.telefono().trim() : null,
                ciudadano, catalogos.avisoPrivacidadVigente(), ahora);
        try {
            // Usuario sin contraseña + su identidad en la misma transacción (B13 se revisa al confirmar)
            usuarioRepository.saveAndFlush(usuario);
            identidadRepository.saveAndFlush(new IdentidadExterna(usuario, google, sub, ahora));
        } catch (DataIntegrityViolationException carrera) {
            // Otra petición registró el mismo documento, correo o cuenta de Google entre la revisión y el INSERT
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se pudo crear la cuenta: el documento, el correo o la cuenta de Google ya están registrados");
        }
        return sesion(usuario);
    }

    private PerfilGoogle verificar(String credential) {
        try {
            return verificador.verificar(credential);
        } catch (JwtException invalido) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo verificar la cuenta de Google");
        }
    }

    private Claims validarTokenRegistro(String tokenRegistro) {
        try {
            return jwtProveedor.validarRegistroGoogle(tokenRegistro);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException invalido) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "El registro con Google no es válido o venció: vuelve a intentarlo");
        }
    }

    private ProveedorIdentidad proveedorGoogle() {
        return proveedorRepository.findByNombre(ProveedorIdentidad.GOOGLE)
                .orElseThrow(() -> new IllegalStateException("Falta el proveedor " + ProveedorIdentidad.GOOGLE));
    }

    private ResultadoGoogle.Sesion sesion(Usuario usuario) {
        String rol = usuario.getRol().getNombre();
        return new ResultadoGoogle.Sesion(jwtProveedor.generar(usuario.getCorreo(), rol), rol);
    }

    private static String limpiarNombre(String valor, String campo) {
        String limpio = valor == null ? "" : valor.trim();
        if (limpio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " son obligatorios");
        }
        if (limpio.length() > MAX_LONGITUD_NOMBRE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    campo + " no pueden pasar de " + MAX_LONGITUD_NOMBRE + " caracteres");
        }
        return limpio;
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
