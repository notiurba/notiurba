package com.notiurba.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorIdentidadRepository extends JpaRepository<ProveedorIdentidad, Short> {

    Optional<ProveedorIdentidad> findByNombre(String nombre);
}
