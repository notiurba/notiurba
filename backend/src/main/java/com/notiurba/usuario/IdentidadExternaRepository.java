package com.notiurba.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentidadExternaRepository extends JpaRepository<IdentidadExterna, Long> {

    Optional<IdentidadExterna> findByProveedorAndIdentificadorExterno(
            ProveedorIdentidad proveedor, String identificadorExterno);

    boolean existsByUsuarioAndProveedor(Usuario usuario, ProveedorIdentidad proveedor);
}
