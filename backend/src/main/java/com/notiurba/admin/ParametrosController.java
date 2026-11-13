package com.notiurba.admin;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Límites contra el abuso. SecurityConfig restringe /api/admin/** al rol ADMIN. */
@RestController
@RequestMapping("/api/admin/configuracion/limites")
public class ParametrosController {

    private final ParametrosServicio servicio;

    public ParametrosController(ParametrosServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public ParametrosServicio.Limites consultar() {
        return servicio.consultar();
    }

    @PutMapping
    public ParametrosServicio.Limites actualizar(Principal sesion, @RequestBody ParametrosServicio.Limites limites) {
        return servicio.actualizar(sesion.getName(), limites);
    }
}
