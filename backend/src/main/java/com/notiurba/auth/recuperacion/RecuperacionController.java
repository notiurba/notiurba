package com.notiurba.auth.recuperacion;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Pantallas «Recuperar contraseña» y «Nueva contraseña» del Figma. */
@RestController
@RequestMapping("/api/auth")
public class RecuperacionController {

    static final Map<String, String> RESPUESTA_GENERICA = Map.of("mensaje",
            "Si el correo está registrado, te enviamos un enlace para restablecer la contraseña.");

    private final RecuperacionServicio servicio;

    public RecuperacionController(RecuperacionServicio servicio) {
        this.servicio = servicio;
    }

    /** Siempre el mismo 200, exista o no la cuenta. */
    @PostMapping("/recuperar")
    public Map<String, String> recuperar(@Valid @RequestBody RecuperarRequest peticion) {
        servicio.solicitar(peticion.correo());
        return RESPUESTA_GENERICA;
    }

    @PostMapping("/restablecer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restablecer(@Valid @RequestBody RestablecerRequest peticion) {
        servicio.restablecer(peticion.token(), peticion.nuevaContrasena());
    }
}
