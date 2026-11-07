package com.notiurba.usuario;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Perfil del ciudadano con sesión: «Ajustes» y «Editar mis datos» (exige sesión). */
@RestController
@RequestMapping("/api/usuarios/me")
public class PerfilController {

    private final PerfilServicio servicio;

    public PerfilController(PerfilServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public PerfilResponse obtener(Authentication sesion) {
        return servicio.obtener(sesion.getName());
    }

    /** Reemplaza los datos personales y devuelve el perfil guardado. El correo no cambia. */
    @PutMapping
    public PerfilResponse actualizar(@Valid @RequestBody ActualizarPerfilRequest peticion, Authentication sesion) {
        return servicio.actualizar(sesion.getName(), peticion);
    }
}
