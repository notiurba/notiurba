package com.notiurba.admin;

import java.time.LocalDate;

/**
 * Filtros comunes del mapa, del mapa de calor y de las estadísticas del panel (NOT-129). Todos son
 * opcionales y llegan ya validados; {@code estado} trae el nombre del catálogo («En trámite»).
 */
public record FiltroMapa(String estado, Integer idCategoria, Integer comuna, LocalDate desde, LocalDate hasta) {
}
