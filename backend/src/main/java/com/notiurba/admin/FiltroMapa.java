package com.notiurba.admin;

import java.time.LocalDate;

/** Filtros comunes ya validados; {@code estado} trae el nombre del catálogo («En trámite»). */
public record FiltroMapa(String estado, Integer idCategoria, Integer comuna, LocalDate desde, LocalDate hasta) {
}
