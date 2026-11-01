package com.notiurba.auth;

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

    public AuthController(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtProveedor jwtProveedor) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProveedor = jwtProveedor;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest peticion) {
        var usuario = usuarioRepository.findByCorreoIgnoreCase(peticion.correo())
                // Una cuenta solo-Google (sin contraseña) recibe la misma respuesta que unas credenciales malas
                .filter(u -> u.getPasswordHash() != null)
                .filter(u -> passwordEncoder.matches(peticion.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, Codigos.CREDENCIALES_INVALIDAS, Codigos.MSG_CREDENCIALES));

        String rol = usuario.getRol().getNombre();
        String token = jwtProveedor.generar(usuario.getCorreo(), rol);
        return new LoginResponse(token, rol);
    }
}
