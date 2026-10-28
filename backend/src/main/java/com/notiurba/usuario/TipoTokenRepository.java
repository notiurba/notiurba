package com.notiurba.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoTokenRepository extends JpaRepository<TipoToken, Short> {

    Optional<TipoToken> findByNombre(String nombre);
}
