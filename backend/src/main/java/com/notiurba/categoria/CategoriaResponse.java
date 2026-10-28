package com.notiurba.categoria;

public record CategoriaResponse(
        Integer idCategoria, String nombre, String descripcion, boolean requiereUbicacion, boolean requiereEvidencia) {

    static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getIdCategoria(), categoria.getNombre(), categoria.getDescripcion(),
                categoria.isRequiereUbicacion(), categoria.isRequiereEvidencia());
    }
}
