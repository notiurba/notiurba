package com.notiurba.categoria;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Categoría que elige el ciudadano (solo lectura). descripcion guarda los ejemplos
 * que muestra la tarjeta de la app; se editan en la base, no en el frontend.
 */
@Entity
@Immutable
@Table(name = "categoria")
public class Categoria {

    @Id
    @Column(name = "id_categoria")
    private Integer idCategoria;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "requiere_ubicacion", nullable = false)
    private boolean requiereUbicacion;

    @Column(name = "requiere_evidencia", nullable = false)
    private boolean requiereEvidencia;

    @Column(name = "activa", nullable = false)
    private boolean activa;

    protected Categoria() {
    }

    public Integer getIdCategoria() {
        return idCategoria;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isRequiereUbicacion() {
        return requiereUbicacion;
    }

    public boolean isRequiereEvidencia() {
        return requiereEvidencia;
    }

    public boolean isActiva() {
        return activa;
    }
}
