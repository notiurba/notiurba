package com.notiurba.auth.recuperacion;

import java.time.Duration;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.notiurba.usuario.TipoToken;
import com.notiurba.usuario.TipoTokenRepository;
import com.notiurba.usuario.TokenVerificacion;
import com.notiurba.usuario.TokenVerificacionRepository;
import com.notiurba.usuario.Usuario;
import com.notiurba.usuario.UsuarioRepository;

/** Fronteras transaccionales cortas para no mantener una transacción abierta al llamar al correo. */
@Component
class PersistenciaRecuperacion {

    static final Duration MAXIMO_PENDIENTE = Duration.ofMinutes(5);

    enum Estado { LISTA, NO_ELEGIBLE, LIMITE }

    record Preparacion(Estado estado, Usuario usuario, TipoToken tipoToken, TokenVerificacion token) {
        static Preparacion noElegible() {
            return new Preparacion(Estado.NO_ELEGIBLE, null, null, null);
        }

        static Preparacion limite() {
            return new Preparacion(Estado.LIMITE, null, null, null);
        }
    }

    private final UsuarioRepository usuarioRepository;
    private final TokenVerificacionRepository tokenRepository;
    private final TipoTokenRepository tipoTokenRepository;

    PersistenciaRecuperacion(UsuarioRepository usuarioRepository,
            TokenVerificacionRepository tokenRepository,
            TipoTokenRepository tipoTokenRepository) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.tipoTokenRepository = tipoTokenRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Preparacion preparar(String correo, String tokenHash, OffsetDateTime ahora, Duration vigencia, int maximoPorHora) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCaseForUpdate(correo.trim())
                .filter(u -> u.getPasswordHash() != null)
                .orElse(null);
        if (usuario == null) {
            return Preparacion.noElegible();
        }

        TipoToken recuperacion = tipoTokenRepository.findByNombre(TipoToken.RECUPERACION_CONTRASENA)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el tipo de token " + TipoToken.RECUPERACION_CONTRASENA));
        // Un fallo persistente tras hablar con el proveedor deja un token seguro pero pendiente.
        // La siguiente solicitud lo reconcilia pronto para no consumir el cupo durante toda la hora.
        tokenRepository.eliminarPendientesAnterioresA(usuario, recuperacion, ahora.minus(MAXIMO_PENDIENTE));
        // Los pendientes reservan cupo mientras el hilo de envío decide si confirma o descarta.
        // El bloqueo de la cuenta vuelve atómicos el conteo y la inserción concurrentes.
        if (tokenRepository.countByUsuarioAndTipoTokenAndCreadoEnAfter(
                usuario, recuperacion, ahora.minusHours(1)) >= maximoPorHora) {
            return Preparacion.limite();
        }

        TokenVerificacion nuevo = new TokenVerificacion(
                usuario, recuperacion, tokenHash, ahora, ahora.plus(vigencia));
        // Un consumo real solo puede ocurrir antes de expira_en; la igualdad queda reservada
        // como marca pendiente y además cumple ck_token_fechas.
        nuevo.marcarUsado(nuevo.getExpiraEn());
        tokenRepository.saveAndFlush(nuevo);
        return new Preparacion(Estado.LISTA, usuario, recuperacion, nuevo);
    }

    /** Solo un token todavía pendiente puede activarse; un token consumido nunca vuelve a habilitarse. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    boolean confirmar(Preparacion preparacion, OffsetDateTime ahora) {
        int confirmado = tokenRepository.confirmarPendiente(
                preparacion.usuario(), preparacion.tipoToken(), preparacion.token().getTokenHash(),
                preparacion.token().getExpiraEn(), ahora);
        if (confirmado != 1) {
            return false;
        }
        tokenRepository.invalidarVigentesExcepto(
                preparacion.usuario(), preparacion.tipoToken(), preparacion.token().getTokenHash(), ahora);
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    boolean descartar(Preparacion preparacion) {
        return tokenRepository.eliminarPendiente(
                preparacion.usuario(), preparacion.tipoToken(), preparacion.token().getTokenHash(),
                preparacion.token().getExpiraEn()) == 1;
    }
}
