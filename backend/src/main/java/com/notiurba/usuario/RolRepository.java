package com.notiurba.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Short> {

    Optional<Rol> findByNombre(String nombre);
}
