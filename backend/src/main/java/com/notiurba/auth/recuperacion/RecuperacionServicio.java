package com.notiurba.auth.recuperacion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.notiurba.auth.ReglaContrasena;
import com.notiurba.usuario.TipoToken;
import com.notiurba.usuario.TipoTokenRepository;
import com.notiurba.usuario.TokenVerificacion;
import com.notiurba.usuario.TokenVerificacionRepository;
import com.notiurba.usuario.Usuario;
import com.notiurba.usuario.UsuarioRepository;

/**
 * «Recuperar contraseña» con token_verificacion (tipo «Recuperación de contraseña»).
 * Nunca revela si una cuenta existe: solicitar() no devuelve nada y no falla por eso.
 */
@Service
public class RecuperacionServicio {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionServicio.class);

    static final Duration VIGENCIA = Duration.ofHours(1);
    static final int MAXIMO_POR_HORA = 3;
    static final String ENLACE_INVALIDO = "El enlace no es válido o venció";

    private final UsuarioRepository usuarioRepository;
    private final TokenVerificacionRepository tokenRepository;
    private final TipoTokenRepository tipoTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EnviadorEnlaceRecuperacion enviador;
    private final String enlaceBase;
    private final SecureRandom aleatorio = new SecureRandom();

    public RecuperacionServicio(
            UsuarioRepository usuarioRepository,
            TokenVerificacionRepository tokenRepository,
            TipoTokenRepository tipoTokenRepository,
            PasswordEncoder passwordEncoder,
            EnviadorEnlaceRecuperacion enviador,
            @Value("${notiurba.recuperacion.enlace}") String enlaceBase) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.tipoTokenRepository = tipoTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.enviador = enviador;
        this.enlaceBase = enlaceBase;
    }

    @Transactional
    public void solicitar(String correo) {
        // Cuentas inexistentes y solo-Google (sin contraseña) no reciben enlace
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo.trim())
                .filter(u -> u.getPasswordHash() != null)
                .orElse(null);
        if (usuario == null) {
            return;
        }
        TipoToken recuperacion = tipoRecuperacion();
        OffsetDateTime ahora = OffsetDateTime.now();
        if (tokenRepository.countByUsuarioAndTipoTokenAndCreadoEnAfter(
                usuario, recuperacion, ahora.minusHours(1)) >= MAXIMO_POR_HORA) {
            log.info("Límite de {} solicitudes de recuperación por hora alcanzado", MAXIMO_POR_HORA);
            return;
        }

        tokenRepository.invalidarVigentes(usuario, recuperacion, ahora);
        String token = nuevoToken();
        tokenRepository.save(new TokenVerificacion(usuario, recuperacion, hash(token), ahora, ahora.plus(VIGENCIA)));
        try {
            enviador.enviar(usuario.getCorreo(), enlaceBase + "?token=" + token);
        } catch (RuntimeException fallo) {
            // Un error de envío no puede cambiar la respuesta: delataría que la cuenta existe
            log.error("No se pudo enviar el enlace de recuperación", fallo);
        }
    }

    @Transactional
    public void restablecer(String token, String nuevaContrasena) {
        ReglaContrasena.validar(nuevaContrasena);
        OffsetDateTime ahora = OffsetDateTime.now();
        TokenVerificacion encontrado = tokenRepository.findByTokenHashAndTipoToken(hash(token), tipoRecuperacion())
                .filter(t -> t.estaVigente(ahora))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, ENLACE_INVALIDO));
        encontrado.marcarUsado(ahora);
        encontrado.getUsuario().cambiarPasswordHash(passwordEncoder.encode(nuevaContrasena));
    }

    private TipoToken tipoRecuperacion() {
        return tipoTokenRepository.findByNombre(TipoToken.RECUPERACION_CONTRASENA)
                .orElseThrow(() -> new IllegalStateException("Falta el tipo de token " + TipoToken.RECUPERACION_CONTRASENA));
    }

    /** 32 bytes aleatorios en Base64 URL (va en el enlace del correo). */
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
