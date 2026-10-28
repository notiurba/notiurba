package com.notiurba.auth.google;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth/google")
public class GoogleAuthController {

    private final GoogleAuthServicio servicio;

    public GoogleAuthController(GoogleAuthServicio servicio) {
        this.servicio = servicio;
    }

    /** 200 {token, rol} si ya hay cuenta; 200 {estado: "PERFIL_INCOMPLETO", ...} si no. */
    @PostMapping
    public ResultadoGoogle autenticar(@Valid @RequestBody GoogleLoginRequest peticion) {
        return servicio.autenticar(peticion.credential());
    }

    @PostMapping("/completar")
    public ResultadoGoogle.Sesion completar(@Valid @RequestBody CompletarPerfilRequest peticion) {
        return servicio.completar(peticion);
    }
}
