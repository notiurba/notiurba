package com.notiurba.auth;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.notiurba.seguridad.JwtProveedor;
import com.notiurba.usuario.UsuarioRepository;
import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProveedor jwtProveedor;
    private final IntentosLoginServicio intentos;

    public AuthController(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtProveedor jwtProveedor,
            IntentosLoginServicio intentos) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProveedor = jwtProveedor;
        this.intentos = intentos;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest peticion) {
        // Un correo bloqueado no entra ni con la contraseña correcta, exista o no la cuenta
        var reserva = intentos.reservar(peticion.correo());
        var usuario = usuarioRepository.findByCorreoIgnoreCase(peticion.correo())
                // Una cuenta solo-Google (sin contraseña) recibe la misma respuesta que unas credenciales malas
                .filter(u -> u.getPasswordHash() != null)
                .filter(u -> passwordEncoder.matches(peticion.password(), u.getPasswordHash()))
                .orElseThrow(() -> credencialesInvalidas(reserva));
        intentos.limpiar(peticion.correo());

        // Solo se dice después de acertar la contraseña, para no revelar qué cuentas existen
        if (usuario.getCorreoVerificadoEn() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, Codigos.CORREO_NO_VERIFICADO, Codigos.MSG_CORREO_NO_VERIFICADO);
        }

        String rol = usuario.getRol().getNombre();
        String token = jwtProveedor.generar(usuario.getCorreo(), rol);
        return new LoginResponse(token, rol);
    }

    /** Si con este intento se llegó al máximo es 429 con la espera; si no, 401 con los intentos que quedan (igual para todos los correos). */
    private ApiException credencialesInvalidas(IntentosLoginServicio.Reserva reserva) {
        if (reserva.bloquea()) {
            return intentos.bloqueo(reserva.bloqueadoHasta());
        }
        return new ApiException(HttpStatus.UNAUTHORIZED, Codigos.CREDENCIALES_INVALIDAS, Codigos.MSG_CREDENCIALES, null,
                Map.of("intentosRestantes", reserva.restantes()));
    }
}
