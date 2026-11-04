package com.notiurba.auth.registro;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ReenviarRequest(@NotBlank @Email String correo) {
}
