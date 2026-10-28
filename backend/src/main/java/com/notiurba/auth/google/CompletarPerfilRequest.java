package com.notiurba.auth.google;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Los nombres salen de aquí (el usuario pudo corregirlos), no del token de Google. */
public record CompletarPerfilRequest(
        @NotBlank String tokenRegistro,
        @NotBlank String nombres,
        @NotBlank String apellidos,
        @NotNull Short idTipoDocumento,
        @NotBlank @Size(max = 30) String numeroDocumento,
        @NotNull Integer idBarrio,
        @NotBlank @Size(max = 300) String direccionCorrespondencia,
        @Size(max = 30) String telefono,
        @NotNull @AssertTrue Boolean aceptaPrivacidad) {
}
