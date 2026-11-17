package com.notiurba.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    /** Serializa operaciones sensibles por cuenta (por ejemplo, el límite de recuperaciones). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where lower(u.correo) = lower(:correo)")
    Optional<Usuario> findByCorreoIgnoreCaseForUpdate(@Param("correo") String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByIdTipoDocumentoAndNumeroDocumento(Short idTipoDocumento, String numeroDocumento);
}
