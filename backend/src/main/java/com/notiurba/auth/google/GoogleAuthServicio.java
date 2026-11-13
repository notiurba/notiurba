package com.notiurba.auth.google;

import java.time.OffsetDateTime;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.notiurba.seguridad.JwtProveedor;
import com.notiurba.usuario.CatalogosRegistro;
import com.notiurba.usuario.IdentidadExterna;
import com.notiurba.usuario.IdentidadExternaRepository;
import com.notiurba.usuario.ProveedorIdentidad;
import com.notiurba.usuario.ProveedorIdentidadRepository;
import com.notiurba.usuario.ReglasPerfil;
import com.notiurba.usuario.Rol;
import com.notiurba.usuario.RolRepository;
import com.notiurba.usuario.Usuario;
import com.notiurba.usuario.UsuarioRepository;

import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

import io.jsonwebtoken.Claims;

/**
 * «Continuar con Google» en dos pasos: verificar el ID token y, si la persona no tiene
 * cuenta, completar el perfil (documento, barrio, dirección) que Google no entrega.
 */
@Service
public class GoogleAuthServicio {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthServicio.class);

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
        ProveedorIdentidad google = paso("proveedor", this::proveedorGoogle);

        // Caso 1: la identidad de Google ya está vinculada → login normal
        var identidad = paso("buscar-identidad",
                () -> identidadRepository.findByProveedorAndIdentificadorExterno(google, perfil.sub()));
        if (identidad.isPresent()) {
            return sesion(identidad.get().getUsuario());
        }

        // Caso 2: ya hay una cuenta con ese correo → se vincula (PROPUESTA, ver vincularPorCorreo)
        var porCorreo = paso("buscar-cuenta", () -> usuarioRepository.findByCorreoIgnoreCase(perfil.correo()));
        if (porCorreo.isPresent()) {
            return sesion(paso("vincular", () -> vincularPorCorreo(porCorreo.get(), google, perfil)));
        }

        // Caso 3: no hay cuenta → no se crea nada; el frontend pide el resto del perfil
        return paso("perfil-incompleto", () -> perfilIncompleto(perfil));
    }

    /**
     * PROPUESTA pendiente de confirmar con Andrés: vincular la cuenta de Google a la cuenta
     * existente con el mismo correo. Es segura porque Google garantiza email_verified = true.
     * Para quitarla, borrar este método y su llamada: la persona caería en el caso 3 y
     * /completar respondería 409 por correo repetido.
     */
    private Usuario vincularPorCorreo(Usuario usuario, ProveedorIdentidad google, PerfilGoogle perfil) {
        if (identidadRepository.existsByUsuarioAndProveedor(usuario, google)) {
            throw new ApiException(HttpStatus.CONFLICT, Codigos.CONFLICTO,
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
            throw new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION,
                    "Debes aceptar el aviso de privacidad", "aceptaPrivacidad");
        }
        String nombres = ReglasPerfil.limpiarNombre(peticion.nombres(), "Los nombres", "nombres");
        String apellidos = ReglasPerfil.limpiarNombre(peticion.apellidos(), "Los apellidos", "apellidos");
        if (!catalogos.existeTipoDocumento(peticion.idTipoDocumento())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION,
                    "El tipo de documento no existe", "idTipoDocumento");
        }
        if (!catalogos.existeBarrio(peticion.idBarrio())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION, "El barrio no existe", "idBarrio");
        }

        ProveedorIdentidad google = proveedorGoogle();
        String numeroDocumento = peticion.numeroDocumento().trim();
        if (identidadRepository.findByProveedorAndIdentificadorExterno(google, sub).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, Codigos.CUENTA_GOOGLE_YA_REGISTRADA,
                    "Esta cuenta de Google ya está registrada: inicia sesión con Google");
        }
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ApiException(HttpStatus.CONFLICT, Codigos.CORREO_YA_REGISTRADO,
                    "Ya existe una cuenta con este correo", "correo");
        }
        if (usuarioRepository.existsByIdTipoDocumentoAndNumeroDocumento(peticion.idTipoDocumento(), numeroDocumento)) {
            throw new ApiException(HttpStatus.CONFLICT, Codigos.DOCUMENTO_YA_REGISTRADO,
                    "Ya existe una cuenta con este documento", "numeroDocumento");
        }

        Rol ciudadano = rolRepository.findByNombre(Rol.CIUDADANO)
                .orElseThrow(() -> new IllegalStateException("Falta el rol " + Rol.CIUDADANO));
        OffsetDateTime ahora = OffsetDateTime.now();
        Usuario usuario = Usuario.nuevoConGoogle(
                nombres, apellidos, peticion.idTipoDocumento(), numeroDocumento, peticion.idBarrio(),
                peticion.direccionCorrespondencia().trim(), correo,
                tieneTexto(peticion.telefono()) ? peticion.telefono().trim() : null,
                ciudadano, catalogos.avisoPrivacidadVigente(), ahora);
        paso("crear-cuenta", () -> {
            try {
                // Usuario sin contraseña + su identidad en la misma transacción (B13 se revisa al confirmar)
                usuarioRepository.saveAndFlush(usuario);
                identidadRepository.saveAndFlush(new IdentidadExterna(usuario, google, sub, ahora));
            } catch (DataIntegrityViolationException carrera) {
                // Otra petición registró el mismo documento, correo o cuenta de Google entre la revisión y el INSERT
                throw new ApiException(HttpStatus.CONFLICT, Codigos.CONFLICTO,
                        "No se pudo crear la cuenta: el documento, el correo o la cuenta de Google ya están registrados");
            }
            return usuario;
        });
        return sesion(usuario);
    }

    private PerfilGoogle verificar(String credential) {
        try {
            return verificador.verificar(credential);
        } catch (JwtException invalido) {
            log.warn("Google · paso «verificar»: Google rechazó el token ({})", causaDelRechazo(invalido));
            throw new ApiException(HttpStatus.UNAUTHORIZED, Codigos.NO_AUTENTICADO,
                    "No se pudo verificar la cuenta de Google");
        } catch (RuntimeException inesperado) {
            log.error("Google · paso «verificar» falló: {}", inesperado.getClass().getSimpleName());
            throw inesperado;
        }
    }

    private Claims validarTokenRegistro(String tokenRegistro) {
        try {
            return jwtProveedor.validarRegistroGoogle(tokenRegistro);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException invalido) {
            log.warn("Google · paso «token-registro»: el token de registro no es válido o venció ({})",
                    invalido.getClass().getSimpleName());
            throw new ApiException(HttpStatus.UNAUTHORIZED, Codigos.NO_AUTENTICADO,
                    "El registro con Google no es válido o venció: vuelve a intentarlo");
        }
    }

    /**
     * Qué falló en un paso, sin datos personales: solo el tipo de error (y el código si es un error de negocio).
     * Nunca el mensaje de la excepción: puede traer correo, documento o el token.
     */
    private <T> T paso(String nombre, Supplier<T> accion) {
        try {
            return accion.get();
        } catch (ApiException respuesta) {
            log.warn("Google · paso «{}» respondió {} ({})", nombre, respuesta.getStatusCode().value(), respuesta.getCodigo());
            throw respuesta;
        } catch (RuntimeException fallo) {
            log.error("Google · paso «{}» falló: {}", nombre, fallo.getClass().getSimpleName());
            throw fallo;
        }
    }

    /** Una etiqueta fija según el motivo, para separar «token mal» de «no se pudieron obtener las claves de Google». */
    static String causaDelRechazo(JwtException e) {
        String mensaje = e.getMessage() == null ? "" : e.getMessage().toLowerCase(java.util.Locale.ROOT);
        String causa;
        if (mensaje.contains("jwk") || mensaje.contains("retrieve") || mensaje.contains("connect") || mensaje.contains("timed out")) {
            causa = "no se pudieron obtener las claves de Google";
        } else if (mensaje.contains("expired")) {
            causa = "token vencido";
        } else if (mensaje.contains("aud")) {
            causa = "audiencia distinta: revisar GOOGLE_CLIENT_ID";
        } else if (mensaje.contains("iss")) {
            causa = "emisor no válido";
        } else if (mensaje.contains("signature") || mensaje.contains("firma")) {
            causa = "firma no válida";
        } else if (mensaje.contains("email")) {
            causa = "correo sin verificar en Google";
        } else {
            causa = "token mal formado";
        }
        return e.getClass().getSimpleName() + ": " + causa;
    }

    private ProveedorIdentidad proveedorGoogle() {
        return proveedorRepository.findByNombre(ProveedorIdentidad.GOOGLE)
                .orElseThrow(() -> new IllegalStateException("Falta el proveedor " + ProveedorIdentidad.GOOGLE));
    }

    private ResultadoGoogle.Sesion sesion(Usuario usuario) {
        return paso("sesion", () -> {
            String rol = usuario.getRol().getNombre();
            return new ResultadoGoogle.Sesion(jwtProveedor.generar(usuario.getCorreo(), rol), rol);
        });
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
