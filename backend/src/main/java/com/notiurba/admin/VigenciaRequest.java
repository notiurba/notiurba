package com.notiurba.admin;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

/** JsonNode evita que Jackson convierta silenciosamente decimales o cadenas a enteros. */
public record VigenciaRequest(@NotNull JsonNode vigenciaDias) {
}
