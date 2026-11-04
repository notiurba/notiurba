package com.notiurba.auth.recuperacion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RecuperarRequest(@NotBlank @Email String correo) {
}
