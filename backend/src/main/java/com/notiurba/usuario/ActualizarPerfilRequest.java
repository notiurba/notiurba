package com.notiurba.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** «Editar mis datos» (PUT /api/usuarios/me). Mismos campos y límites que el registro; sin correo ni contraseña. */
public record ActualizarPerfilRequest(
        @NotBlank String nombres,
        @NotBlank String apellidos,
        @NotNull Short idTipoDocumento,
        @NotBlank @Size(max = 30) String numeroDocumento,
        @NotNull Integer idBarrio,
        @NotBlank @Size(max = 300) String direccionCorrespondencia,
        @Size(max = 30) String telefono) {
}
