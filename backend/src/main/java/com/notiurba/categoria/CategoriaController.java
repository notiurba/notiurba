package com.notiurba.categoria;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Catálogo de categorías activas para la pantalla de nuevo reporte (exige sesión). */
@RestController
public class CategoriaController {

    private final CategoriaRepository categoriaRepository;

    public CategoriaController(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @GetMapping("/api/categorias")
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findByActivaTrueOrderByIdCategoria().stream()
                .map(CategoriaResponse::de)
                .toList();
    }
}
