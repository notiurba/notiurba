package com.notiurba.admin;

import java.security.Principal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** SecurityConfig restringe todas estas rutas al rol ADMIN. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminServicio servicio;
    public AdminController(AdminServicio servicio) { this.servicio = servicio; }

    @PatchMapping("/reportes/{codigo}/estado")
    public CambioEstadoResponse estado(Principal principal, @PathVariable String codigo,
            @Valid @RequestBody CambiarEstadoRequest peticion) {
        return servicio.cambiarEstado(principal.getName(), codigo, peticion);
    }

    @GetMapping("/configuracion/vigencia")
    public VigenciaResponse vigencia() { return servicio.consultarVigencia(); }

    @PutMapping("/configuracion/vigencia")
    public VigenciaResponse vigencia(Principal principal, @Valid @RequestBody VigenciaRequest peticion) {
        return servicio.actualizarVigencia(principal.getName(), peticion);
    }
}
