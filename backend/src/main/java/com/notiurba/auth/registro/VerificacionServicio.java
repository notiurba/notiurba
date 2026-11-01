package com.notiurba.auth.registro;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.notiurba.auth.google.ResultadoGoogle;
import com.notiurba.correo.CorreoSaliente;
import com.notiurba.correo.EnviadorCorreo;
import com.notiurba.seguridad.JwtProveedor;
import com.notiurba.usuario.TipoToken;
import com.notiurba.usuario.TipoTokenRepository;
import com.notiurba.usuario.TokenVerificacion;
import com.notiurba.usuario.TokenVerificacionRepository;
import com.notiurba.usuario.Usuario;
import com.notiurba.usuario.UsuarioRepository;
import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

/**
 * Confirmación del correo con token_verificacion (tipo «Verificación de correo»). Se guarda
 * solo el hash del token. Igual que la recuperación, reenviar nunca revela si una cuenta existe.
 */
@Service
public class VerificacionServicio {

    private static final Logger log = LoggerFactory.getLogger(VerificacionServicio.class);

    static final Duration VIGENCIA = Duration.ofHours(24);
    /** Un reenvío por minuto por cuenta: cuida la cuota de Resend (100 correos al día). */
    static final Duration ESPERA_ENTRE_ENVIOS = Duration.ofMinutes(1);
    static final String ENLACE_INVALIDO = "El enlace no es válido o venció";
    static final String RESPUESTA_REENVIO =
            "Si el correo está registrado y sin confirmar, te enviamos un nuevo enlace.";

    private final UsuarioRepository usuarioRepository;
    private final TokenVerificacionRepository tokenRepository;
    private final TipoTokenRepository tipoTokenRepository;
    private final EnviadorCorreo enviadorCorreo;
    private final JwtProveedor jwtProveedor;
    private final String enlaceBase;
    private final SecureRandom aleatorio = new SecureRandom();

    public VerificacionServicio(
            UsuarioRepository usuarioRepository,
            TokenVerificacionRepository tokenRepository,
            TipoTokenRepository tipoTokenRepository,
            EnviadorCorreo enviadorCorreo,
            JwtProveedor jwtProveedor,
            @Value("${notiurba.verificacion.enlace}") String enlaceBase) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.tipoTokenRepository = tipoTokenRepository;
        this.enviadorCorreo = enviadorCorreo;
        this.jwtProveedor = jwtProveedor;
        this.enlaceBase = enlaceBase;
    }

    /** Crea un token nuevo (deja sin efecto los anteriores) y envía el enlace. Un fallo de envío solo se registra. */
    @Transactional
    public void emitirYEnviar(Usuario usuario) {
        TipoToken tipo = tipoVerificacion();
        OffsetDateTime ahora = OffsetDateTime.now();
        tokenRepository.invalidarVigentes(usuario, tipo, ahora);
        String token = nuevoToken();
        tokenRepository.save(new TokenVerificacion(usuario, tipo, hash(token), ahora, ahora.plus(VIGENCIA)));
        try {
            enviadorCorreo.enviar(new CorreoSaliente(
                    List.of(usuario.getCorreo()),
                    "Confirma tu correo en NotiUrba",
                    "Hola " + usuario.getNombres() + ",\n\n"
                            + "Para activar tu cuenta de NotiUrba confirma tu correo con este enlace:\n"
                            + enlaceBase + "?token=" + token + "\n\n"
                            + "El enlace vence en 24 horas. Si no creaste una cuenta, ignora este mensaje.",
                    null, null, null, null));
        } catch (RuntimeException fallo) {
            // La cuenta ya existe: el ciudadano puede pedir otro enlace con reenviar-verificacion
            log.error("No se pudo enviar el enlace de verificación de correo", fallo);
        }
    }

    /** Confirma el correo con el token del enlace e inicia sesión. */
    @Transactional
    public ResultadoGoogle.Sesion verificar(String token) {
        OffsetDateTime ahora = OffsetDateTime.now();
        TokenVerificacion encontrado = tokenRepository.findByTokenHashAndTipoToken(hash(token), tipoVerificacion())
                .filter(t -> t.estaVigente(ahora))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, Codigos.ENLACE_INVALIDO,
                        ENLACE_INVALIDO, "token"));
        encontrado.marcarUsado(ahora);
        Usuario usuario = encontrado.getUsuario();
        usuario.marcarCorreoVerificado(ahora);
        String rol = usuario.getRol().getNombre();
        return new ResultadoGoogle.Sesion(jwtProveedor.generar(usuario.getCorreo(), rol), rol);
    }

    /** Siempre responde lo mismo; solo envía si la cuenta existe, tiene contraseña y no está verificada. */
    @Transactional
    public String reenviar(String correo) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo.trim())
                .filter(u -> u.getPasswordHash() != null && u.getCorreoVerificadoEn() == null)
                .orElse(null);
        if (usuario != null) {
            OffsetDateTime ahora = OffsetDateTime.now();
            long recientes = tokenRepository.countByUsuarioAndTipoTokenAndCreadoEnAfter(
                    usuario, tipoVerificacion(), ahora.minus(ESPERA_ENTRE_ENVIOS));
            if (recientes == 0) {
                emitirYEnviar(usuario);
            } else {
                log.info("Reenvío de verificación omitido: ya se envió uno hace menos de un minuto");
            }
        }
        return RESPUESTA_REENVIO;
    }

    private TipoToken tipoVerificacion() {
        return tipoTokenRepository.findByNombre(TipoToken.VERIFICACION_CORREO)
                .orElseThrow(() -> new IllegalStateException("Falta el tipo de token " + TipoToken.VERIFICACION_CORREO));
    }

    private String nuevoToken() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 en hexadecimal: es lo único que se guarda en token_verificacion.token_hash. */
    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException(imposible);
        }
    }
}
