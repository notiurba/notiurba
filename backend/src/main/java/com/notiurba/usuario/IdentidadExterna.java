package com.notiurba.usuario;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Cuenta de un proveedor externo (el «sub» de Google) vinculada a un usuario. */
@Entity
@Table(name = "identidad_externa")
public class IdentidadExterna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_identidad_externa")
    private Long idIdentidadExterna;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_proveedor_identidad", nullable = false)
    private ProveedorIdentidad proveedor;

    @Column(name = "identificador_externo", nullable = false)
    private String identificadorExterno;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected IdentidadExterna() {
    }

    public IdentidadExterna(Usuario usuario, ProveedorIdentidad proveedor, String identificadorExterno,
            OffsetDateTime ahora) {
        this.usuario = usuario;
        this.proveedor = proveedor;
        this.identificadorExterno = identificadorExterno;
        this.creadoEn = ahora;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public ProveedorIdentidad getProveedor() {
        return proveedor;
    }

    public String getIdentificadorExterno() {
        return identificadorExterno;
    }
}
