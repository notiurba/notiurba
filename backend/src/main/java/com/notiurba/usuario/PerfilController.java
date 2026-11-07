package com.notiurba.usuario;

import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final EliminacionDatosServicio eliminacion;

    public PerfilController(PerfilServicio servicio, EliminacionDatosServicio eliminacion) {
        this.servicio = servicio;
        this.eliminacion = eliminacion;
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

    /** «Eliminar mis datos»: borra mis reportes y fotos; la cuenta queda. 204. */
    @DeleteMapping("/datos")
    public ResponseEntity<Void> eliminarDatos(Authentication sesion) {
        eliminacion.eliminarDatos(sesion.getName());
        return ResponseEntity.noContent().build();
    }

    /** «Eliminar mi cuenta»: borra la cuenta y todo lo mío. 204. La cuenta de administración da 403. */
    @DeleteMapping
    public ResponseEntity<Void> eliminarCuenta(Authentication sesion) {
        eliminacion.eliminarCuenta(sesion.getName());
        return ResponseEntity.noContent().build();
    }
}
