package com.notiurba.auth.registro;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.notiurba.auth.google.ResultadoGoogle;

import jakarta.validation.Valid;

/** Registro con correo y contraseña y confirmación del correo. Todo público (/api/auth/**). */
@RestController
@RequestMapping("/api/auth")
public class RegistroController {

    private final RegistroServicio registro;
    private final VerificacionServicio verificacion;

    public RegistroController(RegistroServicio registro, VerificacionServicio verificacion) {
        this.registro = registro;
        this.verificacion = verificacion;
    }

    /** 201 sin token: hay que confirmar el correo antes de ingresar. */
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public MensajeResponse registrar(@Valid @RequestBody RegistroRequest peticion) {
        return registro.registrar(peticion);
    }

    /** Confirma el correo con el token del enlace y devuelve la sesión. */
    @PostMapping("/verificar")
    public ResultadoGoogle.Sesion verificar(@Valid @RequestBody VerificarRequest peticion) {
        return verificacion.verificar(peticion.token());
    }

    /** Siempre el mismo 200, exista o no la cuenta. */
    @PostMapping("/reenviar-verificacion")
    public Map<String, String> reenviar(@Valid @RequestBody ReenviarRequest peticion) {
        return Map.of("mensaje", verificacion.reenviar(peticion.correo()));
    }
}
