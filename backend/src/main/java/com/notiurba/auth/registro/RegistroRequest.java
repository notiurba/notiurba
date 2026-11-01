package com.notiurba.auth.registro;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Los campos van en el orden del formulario: así se elige el primer error que se devuelve. */
public record RegistroRequest(
        @NotBlank String nombres,
        @NotBlank String apellidos,
        @NotNull Short idTipoDocumento,
        @NotBlank @Size(max = 30) String numeroDocumento,
        @NotNull Integer idBarrio,
        @NotBlank @Size(max = 300) String direccionCorrespondencia,
        @NotBlank @Email @Size(max = 254) String correo,
        @Size(max = 30) String telefono,
        @NotBlank String password,
        @NotNull @AssertTrue Boolean aceptaPrivacidad) {
}
