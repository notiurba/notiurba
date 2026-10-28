package com.notiurba.auth.recuperacion;

import jakarta.validation.constraints.NotBlank;

public record RestablecerRequest(@NotBlank String token, @NotBlank String nuevaContrasena) {
}
