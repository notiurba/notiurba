package com.notiurba.auth.registro;

import jakarta.validation.constraints.NotBlank;

public record VerificarRequest(@NotBlank String token) {
}
