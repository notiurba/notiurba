package com.notiurba.auth.google;

import jakarta.validation.constraints.NotBlank;

/** credential = ID token que devuelve Google Identity Services en el navegador. */
public record GoogleLoginRequest(@NotBlank String credential) {
}
